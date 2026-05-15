from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class MatchCreate(BaseModel):
    team_a: str
    team_b: str
    total_overs: int

class MatchResponse(BaseModel):
    id: int
    team_a: str
    team_b: str
    status: str
    
    class Config:
        from_attributes = True

class EventCreate(BaseModel):
    event_type: str
    runs: int
    camera_id: str
    confidence: float

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
