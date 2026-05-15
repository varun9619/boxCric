from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Depends, HTTPException
from sqlalchemy.orm import Session
from .database import engine, Base, get_db
from .websocket_manager import manager
from . import models, schemas
import json

# Auto-create tables for development
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Box Cricket AI Scoring API")

@app.get("/")
def read_root():
    return {"message": "Box Cricket Scoring API is running!"}

@app.post("/matches", response_model=schemas.MatchResponse)
def create_match(match: schemas.MatchCreate, db: Session = Depends(get_db)):
    db_match = models.Match(team_a=match.team_a, team_b=match.team_b, status="live")
    db.add(db_match)
    db.commit()
    db.refresh(db_match)
    
    # Initialize match state
    db_state = models.MatchState(match_id=db_match.id)
    db.add(db_state)
    db.commit()
    
    return db_match

@app.get("/matches/{match_id}", response_model=schemas.MatchResponse)
def get_match(match_id: int, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")
    return db_match

@app.post("/matches/{match_id}/events", response_model=schemas.EventResponse)
async def create_event(match_id: int, event: schemas.EventCreate, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")
        
    db_state = db.query(models.MatchState).filter(models.MatchState.match_id == match_id).first()
    
    # Simple over/ball calculation for backend tracking
    current_balls = db.query(models.BallEvent).filter(
        models.BallEvent.match_id == match_id,
        models.BallEvent.event_type.notin_(["wide", "noball"]) # Rough approximation
    ).count()
    
    db_event = models.BallEvent(
        match_id=match_id,
        event_type=event.event_type,
        runs=event.runs,
        camera_id=event.camera_id,
        confidence=event.confidence,
        over_number=current_balls // 6,
        ball_number=current_balls % 6
    )
    db.add(db_event)
    
    # Update match state
    if db_state:
        db_state.score += event.runs
        if event.event_type == "wicket":
            db_state.wickets += 1
            
    db.commit()
    db.refresh(db_event)
    
    # Broadcast to websocket clients
    await manager.broadcast_match_state(match_id, {
        "type": "state_update", 
        "last_event": {
            "type": event.event_type,
            "runs": event.runs
        },
        "current_score": db_state.score if db_state else 0,
        "current_wickets": db_state.wickets if db_state else 0
    })
    
    return db_event

@app.get("/matches/{match_id}/summary")
def get_match_summary(match_id: int, db: Session = Depends(get_db)):
    db_match = db.query(models.Match).filter(models.Match.id == match_id).first()
    if db_match is None:
        raise HTTPException(status_code=404, detail="Match not found")
        
    db_state = db.query(models.MatchState).filter(models.MatchState.match_id == match_id).first()
    events = db.query(models.BallEvent).filter(models.BallEvent.match_id == match_id).all()
    
    return {
        "match": db_match.id,
        "team_a": db_match.team_a,
        "team_b": db_match.team_b,
        "score": db_state.score if db_state else 0,
        "wickets": db_state.wickets if db_state else 0,
        "total_events": len(events)
    }

@app.websocket("/ws/match/{match_id}")
async def websocket_endpoint(websocket: WebSocket, match_id: int):
    """
    WebSocket endpoint for Phones to stream events and receive match state updates.
    """
    await manager.connect(websocket, match_id)
    # Dictionary to keep track of pending events for reconciliation
    pending_events = {}
    
    try:
        while True:
            # 1. Receive event suggestions from phones
            data = await websocket.receive_text()
            event = json.loads(data)
            
            print(f"Received event from phone: {event}")
            
            # Simple reconciliation logic
            # If camera 1 detects something, we can broadcast it as an AI suggestion
            if "camera" in event and event.get("type") in ["BALL", "WICKET"]:
                # In a real scenario, we'd wait for both cameras or use confidence threshold
                # For now, just forward the suggestion back to the clients to confirm
                await manager.broadcast_match_state(match_id, {
                    "type": "ai_suggestion",
                    "runs": event.get("runs", 0),
                    "confidence": event.get("confidence", 0.9),
                    "description": f"AI detected event from {event['camera']}",
                    "camera": event["camera"]
                })
            
    except WebSocketDisconnect:
        manager.disconnect(websocket, match_id)
        print(f"Client disconnected from match {match_id}")
