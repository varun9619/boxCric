# Product Requirements Document (PRD)
## AI-Assisted Box Cricket Scoring System

### 1. Product Vision & Scope
To build a constrained-environment AI scoring system for box cricket that reduces the cognitive load on human umpires/scorers. The system automatically infers runs, boundaries, and wickets based on player movement using a dual-camera Android architecture, but relies on a human operator for final confirmation.

### 2. Core Philosophy
- **Event Inference over Object Tracking**: The system infers runs based on batsman crease-crossing rather than tracking a fast-moving cricket ball.
- **Human-in-the-Loop**: AI suggests events (e.g., 2 runs); a human operator confirms them. The AI does not autonomously alter the match state.
- **Local Network Priority**: The system relies on a local Wi-Fi router or hotspot. No cloud streaming is required, drastically reducing latency and bandwidth costs.

### 3. Key Features
1. **Match Setup & Player Registration**: UI to preload teams, players, and their face embeddings.
2. **Dual-Camera Feed Processing**:
   - *Phone 1 (Behind Keeper)*: Striker/bowler recognition, crease crossing, wicket visibility.
   - *Phone 2 (Side Angle)*: Run confirmation, boundary validation, occlusion handling.
3. **Automatic Run Inference**: Uses MediaPipe Pose and ByteTrack to detect when batters swap crease positions.
4. **Boundary Detection**: Uses predefined polygon zones mapped to the physical arena.
5. **Operator UI**: A confirmation queue where AI suggestions (e.g., "AI detected 1 Run -> [Confirm] / [Edit]") are presented.
6. **Live Scoreboard**: Real-time WebSocket-powered UI showing score, run rate, target, and current batters.
7. **Post-Match Summaries**: Automatically generates batting/bowling cards and MVP stats.

### 4. Technical Stack
- **Mobile (Android/Kotlin)**: CameraX (feed), TFLite (inference), MediaPipe (pose), YOLOv8 (detection), ByteTrack/DeepSORT (tracking), Ktor/WebSockets (networking).
- **Backend (Python)**: FastAPI, WebSockets (for real-time event sync and broadcast).
- **Database**: PostgreSQL (relational storage for players, matches, events, summaries).

### 5. Development Phases
- **Phase 1**: Manual scoring Android app (base architecture and UI).
- **Phase 2**: Realtime phone synchronization over local network via FastAPI.
- **Phase 3**: Player face recognition module.
- **Phase 4**: Automatic run inference via crease transition tracking.
- **Phase 5**: Boundary detection using geometric zones.
- **Phase 6**: AI-assisted wicket detection.

### 6. Out of Scope
- Full, precise cricket ball tracking.
- Cloud video recording/playback.
- 100% Autonomous umpiring without operator intervention.
