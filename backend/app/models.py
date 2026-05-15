from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Float
from sqlalchemy.sql import func
from .database import Base

class Player(Base):
    __tablename__ = "players"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    team_id = Column(Integer)
    face_embedding = Column(String, nullable=True) # Stored as embedding array or JSON in production

class Match(Base):
    __tablename__ = "matches"

    id = Column(Integer, primary_key=True, index=True)
    team_a = Column(String)
    team_b = Column(String)
    total_overs = Column(Integer, default=6)
    date = Column(DateTime(timezone=True), server_default=func.now())
    status = Column(String, default="scheduled")  # scheduled, live, completed

class BallEvent(Base):
    __tablename__ = "ball_events"

    id = Column(Integer, primary_key=True, index=True)
    match_id = Column(Integer, ForeignKey("matches.id"))
    over_number = Column(Integer)
    ball_number = Column(Integer)
    event_type = Column(String)  # run, boundary, wicket, wide, noball
    runs = Column(Integer, default=0)
    camera_id = Column(String)
    confidence = Column(Float)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())

class MatchState(Base):
    __tablename__ = "match_state"

    match_id = Column(Integer, ForeignKey("matches.id"), primary_key=True)
    score = Column(Integer, default=0)
    wickets = Column(Integer, default=0)
    overs = Column(Float, default=0.0)
    inning = Column(Integer, default=1)  # 1 = first innings, 2 = second innings
    striker_id = Column(Integer, ForeignKey("players.id"), nullable=True)
    non_striker_id = Column(Integer, ForeignKey("players.id"), nullable=True)
