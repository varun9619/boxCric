from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

# Note: Using SQLite for local development
SQLALCHEMY_DATABASE_URL = "sqlite:///./boxcricket.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
