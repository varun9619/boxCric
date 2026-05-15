# AI-Assisted Box Cricket Scoring System

An AI-powered, dual-camera Android application designed specifically for box cricket. It tracks player movement and infers scoring events (runs, boundaries, wickets) to assist a human operator in maintaining the live scoreboard, without requiring a full cloud architecture.

## Tech Stack
- **Android App**: Kotlin, CameraX, Ktor Client
- **On-Device AI**: TensorFlow Lite, MediaPipe Pose, YOLOv8, ByteTrack / DeepSORT
- **Backend Server**: FastAPI (Python), WebSockets
- **Database**: PostgreSQL

## Architecture Diagram
```text
Phone 1 (Behind Keeper)
        ↓
Phone 2 (Side Angle)
        ↓
Local Wi-Fi / Hotspot
        ↓
FastAPI Backend Server
        ↓
PostgreSQL Database
        ↓
Realtime Match State
        ↓
Android Scoreboard UI
```

## Core Philosophy
This system provides **constrained-environment event inference**, rather than full generalized cricket AI. 
- It infers runs from batsman crease transitions rather than ball tracking.
- The AI **suggests** events; a human operator **confirms** them.
- All real-time synchronization happens over a local network to minimize latency.

## Setup Instructions
*(Coming Soon - Pending initial scaffolding of FastAPI backend and Kotlin Android projects)*
