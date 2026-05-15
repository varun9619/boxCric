from fastapi import WebSocket
from typing import Dict, List, Optional, Tuple
import json
import asyncio
import time

# ── Pending event for dual-camera reconciliation ──────────────────────────────

class PendingEvent:
    """Holds an event received from one camera, waiting for confirmation from the other."""
    def __init__(self, event_type: str, runs: int, camera_id: str, confidence: float):
        self.event_type = event_type
        self.runs = runs
        self.camera_id = camera_id
        self.confidence = confidence
        self.timestamp = time.monotonic()

    def is_expired(self, timeout_seconds: float = 2.0) -> bool:
        return (time.monotonic() - self.timestamp) > timeout_seconds


# ── Connection Manager ────────────────────────────────────────────────────────

class ConnectionManager:
    def __init__(self):
        # match_id -> list of (websocket, camera_role) tuples
        self.active_connections: Dict[int, List[Tuple[WebSocket, str]]] = {}
        # match_id -> PendingEvent (waiting for 2nd camera confirmation)
        self.pending_events: Dict[int, PendingEvent] = {}

    async def connect(self, websocket: WebSocket, match_id: int, camera_role: str = "CAM1"):
        await websocket.accept()
        if match_id not in self.active_connections:
            self.active_connections[match_id] = []
        self.active_connections[match_id].append((websocket, camera_role))
        print(f"[WS] {camera_role} connected to match {match_id}. "
              f"Total clients: {len(self.active_connections[match_id])}")

    def disconnect(self, websocket: WebSocket, match_id: int):
        if match_id in self.active_connections:
            self.active_connections[match_id] = [
                (ws, role) for ws, role in self.active_connections[match_id]
                if ws != websocket
            ]

    def get_camera_count(self, match_id: int) -> int:
        return len(self.active_connections.get(match_id, []))

    async def broadcast_match_state(self, match_id: int, state: dict):
        """Send a JSON payload to all connected clients for a match."""
        if match_id not in self.active_connections:
            return
        dead = []
        for ws, role in self.active_connections[match_id]:
            try:
                await ws.send_json(state)
            except Exception:
                dead.append((ws, role))
        # Prune dead connections
        for item in dead:
            self.active_connections[match_id].remove(item)

    # ── Dual-Camera Reconciliation ────────────────────────────────────────────

    async def reconcile_event(
        self,
        match_id: int,
        event_type: str,
        runs: int,
        camera_id: str,
        confidence: float,
    ) -> Optional[dict]:
        """
        Two-camera confidence merge:
        - If a pending event exists from the OTHER camera with the same type within 2s →
          merge confidences, broadcast as high-confidence ai_suggestion.
        - If no pending event exists → store as pending, schedule a timeout fallback.
        - If pending event is expired → replace it and restart the timer.

        Returns the broadcast payload if a decision was made, else None.
        """
        pending = self.pending_events.get(match_id)

        if pending and not pending.is_expired():
            if pending.camera_id != camera_id and pending.event_type == event_type:
                # Both cameras agree — high confidence merge
                merged_confidence = min(1.0, (pending.confidence + confidence) / 2 + 0.1)
                del self.pending_events[match_id]
                return {
                    "type": "ai_suggestion",
                    "runs": runs,
                    "confidence": round(merged_confidence, 2),
                    "description": f"Dual-camera confirmed: {event_type} ({runs} run(s))",
                    "camera": "dual",
                    "source": "reconciled"
                }
            else:
                # Same camera sent again, or different event — replace pending
                pass

        # Store as pending, fire a delayed fallback broadcast after 2 seconds
        self.pending_events[match_id] = PendingEvent(event_type, runs, camera_id, confidence)
        asyncio.ensure_future(
            self._timeout_fallback(match_id, event_type, runs, camera_id, confidence)
        )
        return None  # Don't broadcast yet — waiting for 2nd camera

    async def _timeout_fallback(
        self,
        match_id: int,
        event_type: str,
        runs: int,
        camera_id: str,
        confidence: float,
    ):
        """
        If no 2nd camera confirms within 2s, broadcast the single-camera event
        as a medium-confidence suggestion for the human operator to confirm.
        """
        await asyncio.sleep(2.0)
        pending = self.pending_events.get(match_id)
        if pending and pending.camera_id == camera_id and pending.event_type == event_type:
            # Still unconfirmed — send as single-camera suggestion
            del self.pending_events[match_id]
            await self.broadcast_match_state(match_id, {
                "type": "ai_suggestion",
                "runs": runs,
                "confidence": round(min(confidence, 0.75), 2),  # cap at 75% for single-cam
                "description": f"Single-camera ({camera_id}): {event_type} ({runs} run(s)) — please confirm",
                "camera": camera_id,
                "source": "single_camera_timeout"
            })


manager = ConnectionManager()
