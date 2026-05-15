from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class MatchCreate(BaseModel):
    team_a: str
    team_b: str
    total_overs: int = 6

class MatchResponse(BaseModel):
    id: int
    team_a: str
    team_b: str
    total_overs: int
    status: str
    
    class Config:
        from_attributes = True

class EventCreate(BaseModel):
    event_type: str
    runs: int
    camera_id: str
    confidence: float = 1.0

class EventResponse(BaseModel):
    id: int
    match_id: int
    event_type: str
    runs: int
    camera_id: str
    confidence: float
    timestamp: datetime
    
    class Config:
        from_attributes = True

class PlayerCreate(BaseModel):
    name: str
    team_id: int

class PlayerResponse(BaseModel):
    id: int
    name: str
    team_id: int

    class Config:
        from_attributes = True

class PlayerRegistration(BaseModel):
    face_embedding: List[float]

class FaceMatchRequest(BaseModel):
    face_embedding: List[float]

class FaceMatchResponse(BaseModel):
    player_id: Optional[int] = None
    name: Optional[str] = None
    confidence: float
