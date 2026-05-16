from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Depends, HTTPException, Query, UploadFile, File
from sqlalchemy.orm import Session
from .database import engine, Base, get_db
from .websocket_manager import manager
from . import models, schemas
import json
import io
import numpy as np
from PIL import Image

try:
    import torch
    import torchvision.models as models
    import torchvision.transforms as transforms

    # Initialize models globally
    device = torch.device('cuda:0' if torch.cuda.is_available() else 'cpu')
    # Use a standard resnet18 as a lightweight embedding extractor for the prototype
    resnet = models.resnet18(weights=models.ResNet18_Weights.DEFAULT)
    resnet.fc = torch.nn.Identity() # Remove classification head to get embeddings
    resnet = resnet.eval().to(device)

    preprocess = transforms.Compose([
        transforms.Resize((160, 160)),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
    ])

    def get_face_embedding(image_bytes: bytes):
        img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
        tensor = preprocess(img).unsqueeze(0).to(device)
        with torch.no_grad():
            embedding = resnet(tensor).cpu().numpy()[0]
        return embedding.tolist()
except ImportError as e:
    print(f"Warning: PyTorch could not be initialized ({e}). Using mock embeddings.")
    def get_face_embedding(image_bytes: bytes):
        return [0.5] * 512


# Auto-create tables for development
# NOTE: Delete boxcricket.db if you add new columns and need a fresh schema
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Box Cricket AI Scoring API")


# ── Health Check ──────────────────────────────────────────────────────────────

@app.get("/")
def read_root():
    return {"message": "Box Cricket Scoring API is running!", "status": "ok"}


# ── Players ───────────────────────────────────────────────────────────────────

@app.post("/players", response_model=schemas.PlayerResponse)
def create_player(player: schemas.PlayerCreate, db: Session = Depends(get_db)):
    db_player = models.Player(
        name=player.name,
        team_id=player.team_id
    )
    db.add(db_player)
    db.commit()
    db.refresh(db_player)
    return db_player

@app.get("/players/{player_id}", response_model=schemas.PlayerResponse)
def get_player(player_id: int, db: Session = Depends(get_db)):
    db_player = db.query(models.Player).filter(models.Player.id == player_id).first()
    if db_player is None:
        raise HTTPException(status_code=404, detail="Player not found")
    return db_player

@app.post("/players/{player_id}/register_face")
async def register_face(player_id: int, file: UploadFile = File(...), db: Session = Depends(get_db)):
    db_player = db.query(models.Player).filter(models.Player.id == player_id).first()
    if not db_player:
        raise HTTPException(status_code=404, detail="Player not found")
    
    image_bytes = await file.read()
    try:
        embedding = get_face_embedding(image_bytes)
        if embedding is None:
            raise HTTPException(status_code=400, detail="No face detected in image")
            
        db_player.face_embedding = json.dumps(embedding)
        db.commit()
        return {"message": "Face registered successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Matches ───────────────────────────────────────────────────────────────────

@app.post("/matches", response_model=schemas.MatchResponse)
def create_match(match: schemas.MatchCreate, db: Session = Depends(get_db)):
    db_match = models.Match(
        team_a=match.team_a,
        team_b=match.team_b,
        total_overs=match.total_overs,
        status="live"
    )
    db.add(db_match)
    db.commit()
    db.refresh(db_match)

    # Initialize match state for the first innings
    db_state = models.MatchState(match_id=db_match.id, inning=1)
    db.add(db_state)
    db.commit()

    return db_match


@app.get("/matches", response_model=list[schemas.MatchResponse])
def list_matches(status: str = "live", db: Session = Depends(get_db)):
    """List matches by status (default: live). Used by joining devices to discover active matches."""
    return db.query(models.Match).filter(models.Match.status == status).all()


@app.get("/matches/{match_id}", response_model=schemas.MatchResponse)
def get_match(match_id: int, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")
    return db_match

@app.post("/matches/{match_id}/recognize_batter", response_model=schemas.FaceMatchResponse)
async def recognize_batter(match_id: int, file: UploadFile = File(...), db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if not db_match:
        raise HTTPException(status_code=404, detail="Match not found")
        
    image_bytes = await file.read()
    try:
        query_embedding = get_face_embedding(image_bytes)
        if query_embedding is None:
            raise HTTPException(status_code=400, detail="No face detected in image")
            
        query_emb_np = np.array(query_embedding)
        
        # Get all players with face embeddings
        players = db.query(models.Player).filter(models.Player.face_embedding.isnot(None)).all()
        
        best_match = None
        best_sim = -1.0
        
        for player in players:
            emb = np.array(json.loads(player.face_embedding))
            # Cosine similarity
            sim = np.dot(query_emb_np, emb) / (np.linalg.norm(query_emb_np) * np.linalg.norm(emb))
            if sim > best_sim:
                best_sim = float(sim)
                best_match = player
                
        # threshold for vggface2 is usually around 0.6-0.7
        if best_match and best_sim > 0.65:
            return schemas.FaceMatchResponse(
                player_id=best_match.id,
                name=best_match.name,
                confidence=best_sim
            )
        else:
            return schemas.FaceMatchResponse(
                player_id=None,
                name=None,
                confidence=max(best_sim, 0.0)
            )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Events ────────────────────────────────────────────────────────────────────

@app.post("/matches/{match_id}/events", response_model=schemas.EventResponse)
async def create_event(match_id: int, event: schemas.EventCreate, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")

    db_state = db.query(models.MatchState).filter(models.MatchState.match_id == match_id).first()

    # Count legal deliveries (exclude extras) for accurate over/ball tracking
    legal_ball_count = db.query(models.BallEvent).filter(
        models.BallEvent.match_id == match_id,
        models.BallEvent.event_type.notin_(["wide", "noball"])
    ).count()

    db_event = models.BallEvent(
        match_id=match_id,
        event_type=event.event_type,
        runs=event.runs,
        camera_id=event.camera_id,
        confidence=event.confidence,
        over_number=legal_ball_count // 6,
        ball_number=legal_ball_count % 6
    )
    db.add(db_event)

    # Update match state
    is_legal = event.event_type not in ["wide", "noball"]
    if db_state:
        db_state.score += event.runs
        if event.event_type == "wicket":
            db_state.wickets += 1
        if is_legal:
            new_legal = legal_ball_count + 1
            db_state.overs = new_legal / 6.0

    db.commit()
    db.refresh(db_event)

    # Broadcast authoritative state_update to all connected clients
    await manager.broadcast_match_state(match_id, {
        "type": "state_update",
        "last_event": {
            "id": db_event.id,
            "type": db_event.event_type,
            "runs": db_event.runs,
            "camera_id": db_event.camera_id,
            "over": db_event.over_number,
            "ball": db_event.ball_number,
            "description": f"{db_event.runs} run(s)" if db_event.event_type == "boundary" else db_event.event_type.capitalize()
        },
        "current_score": db_state.score if db_state else 0,
        "current_wickets": db_state.wickets if db_state else 0,
        "current_overs": legal_ball_count // 6,
        "current_balls": legal_ball_count % 6,
        "inning": db_state.inning if db_state else 1,
        "target": db_state.target if db_state else None
    })

    return db_event

@app.get("/matches/{match_id}/events", response_model=list[schemas.EventResponse])
def get_match_events(match_id: int, db: Session = Depends(get_db)):
    return db.query(models.BallEvent).filter(models.BallEvent.match_id == match_id).all()


# ── Summary ───────────────────────────────────────────────────────────────────

@app.get("/matches/{match_id}/summary")
def get_match_summary(match_id: int, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")

    db_state = db.query(models.MatchState).filter(models.MatchState.match_id == match_id).first()
    events = db.query(models.BallEvent).filter(models.BallEvent.match_id == match_id).all()

    boundaries = [e for e in events if e.event_type == "boundary"]
    wickets = [e for e in events if e.event_type == "wicket"]

    return {
        "match_id": db_match.id,
        "team_a": db_match.team_a,
        "team_b": db_match.team_b,
        "total_overs": db_match.total_overs,
        "score": db_state.score if db_state else 0,
        "wickets": db_state.wickets if db_state else 0,
        "overs_bowled": round(db_state.overs, 2) if db_state else 0.0,
        "total_events": len(events),
        "boundaries": len(boundaries),
        "total_wickets": len(wickets),
    }


# ── WebSocket ─────────────────────────────────────────────────────────────────

@app.websocket("/ws/match/{match_id}")
async def websocket_endpoint(
    websocket: WebSocket,
    match_id: int,
    camera: str = Query(default="CAM1", description="Camera role: CAM1 or CAM2")
):
    """
    WebSocket endpoint for phones to stream events and receive match state updates.

    Query param `camera` identifies the role of this phone (CAM1 or CAM2).
    Example: ws://192.168.1.100:8000/ws/match/1?camera=CAM2
    """
    await manager.connect(websocket, match_id, camera_role=camera.upper())

    try:
        while True:
            data = await websocket.receive_text()
            try:
                event = json.loads(data)
            except json.JSONDecodeError:
                await websocket.send_json({"type": "error", "message": "Invalid JSON payload"})
                continue

            event_type = event.get("type", "")
            print(f"[WS] match={match_id} cam={camera} event={event_type}")

            if event_type in ("BALL", "WICKET", "EXTRA"):
                runs = event.get("runs", 0)
                cam_id = event.get("camera", camera)
                confidence = float(event.get("confidence", 0.9))
                mapped_type = {
                    "BALL": "run",
                    "WICKET": "wicket",
                    "EXTRA": event.get("extra_type", "wide"),
                }.get(event_type, "run")

                # Run through dual-camera reconciliation
                payload = await manager.reconcile_event(
                    match_id=match_id,
                    event_type=mapped_type,
                    runs=runs,
                    camera_id=cam_id,
                    confidence=confidence,
                )
                if payload:
                    await manager.broadcast_match_state(match_id, payload)

            elif event_type == "START_SECOND_INNINGS":
                # Notify all clients to switch to second innings
                await manager.broadcast_match_state(match_id, {
                    "type": "innings_change",
                    "inning": 2,
                    "message": "Second innings started"
                })

    except WebSocketDisconnect:
        manager.disconnect(websocket, match_id)
        print(f"[WS] {camera} disconnected from match {match_id}. "
              f"Remaining: {manager.get_camera_count(match_id)}")
