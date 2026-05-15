# 🏏 AI Box Cricket Scorer — Implementation Tracker

> **Last Updated:** 2026-05-15
> **Stack:** Android (Kotlin / Jetpack Compose) + Python (FastAPI / SQLAlchemy)
> **Repo Root:** `d:\boxCric\boxCric`

---

## 📊 Overall Progress

| Phase | Title | Status | Progress |
|-------|-------|--------|----------|
| 1 | Manual Scoring Android App | ✅ Complete | ██████████ 100% |
| 2 | Real-Time Phone Sync (FastAPI + WebSockets) | ✅ Complete | ██████████ 100% |
| 3 | Player Face Recognition | ✅ Complete | ██████████ 100% |
| 4 | Multi-Device Sync (Join Match) | ✅ Complete | ██████████ 100% |
| 5 | Automatic Run Inference (Crease Tracking) | 🔴 Not Started | ░░░░░░░░░░ 0% |
| 6 | Boundary Detection (Geometric Zones) | 🔴 Not Started | ░░░░░░░░░░ 0% |
| 7 | AI-Assisted Wicket Detection | 🔴 Not Started | ░░░░░░░░░░ 0% |

**Total Project Completion: ~57%**

---

## Phase 1 — Manual Scoring Android App
> Goal: Base architecture, UI, and manual human-operated scoring.

### 🏗️ Architecture & Setup

| # | Task | Status | File |
|---|------|--------|------|
| 1.1 | Android project scaffolding (namespace, SDK config) | ✅ Done | [build.gradle.kts](file:///d:/boxCric/boxCric/mobile/app/build.gradle.kts) |
| 1.2 | Jetpack Compose + Material3 setup | ✅ Done | [build.gradle.kts](file:///d:/boxCric/boxCric/mobile/app/build.gradle.kts) |
| 1.3 | CameraX dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxCric/boxCric/mobile/app/build.gradle.kts) |
| 1.4 | TFLite dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxCric/boxCric/mobile/app/build.gradle.kts) |
| 1.5 | Ktor WebSocket client dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxCric/boxCric/mobile/app/build.gradle.kts) |
| 1.6 | Camera permission request flow | ✅ Done | [MainActivity.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) |

### 📱 UI Screens

| # | Task | Status | File |
|---|------|--------|------|
| 1.7 | `ConnectScreen` — IP input & connect button | ✅ Done | [ConnectScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ConnectScreen.kt) |
| 1.8 | `ScoreboardScreen` — live score display | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.9 | Manual run buttons (0, 1, 2, 3, FOUR, SIX) | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.10 | Wicket button | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.11 | Extra (wide/no-ball) button | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.12 | AI suggestion confirm/reject UI card | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.13 | Ball history timeline display | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.14 | Connection status indicator | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.15 | Match Setup screen (team/player entry) | ✅ Done | [MatchSetupScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/MatchSetupScreen.kt) |
| 1.16 | Post-match summary screen | ✅ Done | [SummaryScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/SummaryScreen.kt) |
| 1.17 | Navigation: Setup → Live → InningsBreak → Summary | ✅ Done | [MainActivity.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) |

### 🧠 ViewModel & State

| # | Task | Status | File |
|---|------|--------|------|
| 1.18 | `ScorerViewModel` with match state flows | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.19 | `addRuns()`, `addWicket()`, `addExtra()` actions | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.20 | `confirmAiSuggestion()` / `rejectAiSuggestion()` | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.21 | Ball history accumulation | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.22 | Over/ball counter logic — legal vs extra deliveries | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.23 | Data models (MatchState, BallEvent, AiSuggestion) | ✅ Done | [Models.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/data/Models.kt) |

---

## Phase 2 — Real-Time Phone Sync (FastAPI + WebSockets)
> Goal: Dual-phone synchronization over local Wi-Fi via a Python backend.

### 🐍 Backend — FastAPI

| # | Task | Status | File |
|---|------|--------|------|
| 2.1 | FastAPI app bootstrap | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.2 | SQLAlchemy ORM setup + SQLite engine | ✅ Done | [database.py](file:///d:/boxCric/boxCric/backend/app/database.py) |
| 2.3 | `Player`, `Match`, `BallEvent`, `MatchState` models | ✅ Done | [models.py](file:///d:/boxCric/boxCric/backend/app/models.py) |
| 2.4 | WebSocket endpoint `/ws/match/{match_id}?camera=CAM1\|CAM2` | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.5 | `WebSocketManager` — connect/disconnect/broadcast | ✅ Done | [websocket_manager.py](file:///d:/boxCric/boxCric/backend/app/websocket_manager.py) |
| 2.6 | REST API: `POST /matches` (create match) | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.7 | REST API: `GET /matches/{id}` (fetch match state) | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.8 | REST API: `POST /matches/{id}/events` (record event) | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.9 | REST API: `GET /matches/{id}/summary` | ✅ Done | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) |
| 2.10 | Multi-camera event reconciliation logic | ✅ Done | [websocket_manager.py](file:///d:/boxCric/boxCric/backend/app/websocket_manager.py) |
| 2.11 | Confidence-based event merging (Cam1 + Cam2, 2s TTL) | ✅ Done | [websocket_manager.py](file:///d:/boxCric/boxCric/backend/app/websocket_manager.py) |
| 2.12 | Backend auth/session management | 🔴 Skipped | Local Wi-Fi — not needed |

### 📱 Android — Networking

| # | Task | Status | File |
|---|------|--------|------|
| 2.13 | Ktor WebSocket client setup | ✅ Done | [WebSocketManager.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) |
| 2.14 | `connectToServer(ip)` in ViewModel | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.15 | Emit ball events to backend over WebSocket | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.16 | Receive & apply state updates from backend | ✅ Done | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.17 | Reconnection / error handling on disconnect | ✅ Done | [WebSocketManager.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) |
| 2.18 | Camera role assignment (Phone 1 vs Phone 2) | ✅ Done | [WebSocketManager.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) |

---

## Phase 3 — Player Face Recognition
> Goal: Identify batters at the crease using face embeddings.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 3.1 | Face embedding generation pipeline (Python) | ✅ Done | Using `torchvision.models.resnet18` |
| 3.2 | Store face embeddings in DB (Player model ready) | ✅ Done | Added JSON parsing logic in endpoints |
| 3.3 | TFLite face detection model on Android | ✅ Done | Used ML Kit Face Detection |
| 3.4 | Real-time batter identification from CameraX frame | ✅ Done | Hidden preview runs 1 FPS analysis |
| 3.5 | Match recognized face to player DB record | ✅ Done | Backend matches using Cosine Similarity |
| 3.6 | Player registration UI (photo capture + save) | ✅ Done | Built overlay with manual capture |
| 3.7 | Confidence threshold / fallback to manual | ✅ Done | >65% confidence required |

---

## Phase 4 — Multi-Device Sync (Join Match)
> Goal: Allow secondary devices and spectators to join an active match. Persist player data across matches for a smooth tournament day.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 4.1 | UI: "Join Match" tab on Setup Screen | ✅ Done | Two-tab switcher (New / Join) |
| 4.2 | Dynamic player count (add/remove) per team | ✅ Done | No fixed limit, ➕/✕ buttons |
| 4.3 | Backend `GET /matches/{id}` for join fetch | ✅ Done | Already existed; ViewModel now uses it |
| 4.4 | Backend `GET /matches?status=live` list endpoint | ✅ Done | For future match discovery UI |
| 4.5 | Android `joinMatch()` in ViewModel | ✅ Done | Bypasses match creation, syncs from server |
| 4.6 | Match ID badge shown on scoreboard header | ✅ Done | Orange `Match #N` chip — Device 2/3 read and type it |
| 4.7 | Remove camera role picker from New Match tab | ✅ Done | Starter is always Device 1 automatically |
| 4.8 | "Same Teams" quick-rematch button on summary screen | ✅ Done | Restarts match instantly with same teams & overs |
| 4.9 | Persistent player history across matches | ✅ Done | Setup screen pre-fills from last match |
| 4.10 | Fix: 2nd innings score corruption | ✅ Done | Subtracts 1st innings offset from backend state |
| 4.11 | Rematch Toss Dialog | ✅ Done | Ask who won toss & decision before quick-restart |
| 4.12 | Local Persistence (SharedPreferences) | ✅ Done | Data survives app-kill/restarts |

---

## Phase 5 — Automatic Run Inference (Crease Tracking)
> Goal: Detect crease crossings using MediaPipe Pose + ByteTrack.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 5.1 | Add MediaPipe Tasks for Android dependency | 🔴 Not Started | `com.google.mediapipe:tasks-vision` |
| 5.2 | CameraX frame → MediaPipe Pose inference pipeline | 🔴 Not Started | — |
| 5.3 | Define crease zone coordinates (pixel or physical) | 🔴 Not Started | — |
| 5.4 | Crease-crossing detection algorithm | 🔴 Not Started | Track foot landmark crossing threshold |
| 5.5 | ByteTrack / DeepSORT player tracking integration | 🔴 Not Started | May need JNI or Python-side processing |
| 5.6 | Run count inference from crease swap count | 🔴 Not Started | — |
| 5.7 | Emit inferred run event as AI suggestion (not confirmed) | 🔴 Not Started | Hook into existing AI suggestion confirm/reject UI |
| 5.8 | Dual-camera run confirmation reconciliation | 🔴 Not Started | Uses Phase 2 WS reconciliation |

---

## Phase 6 — Boundary Detection (Geometric Zones)
> Goal: Detect 4s and 6s using predefined polygon boundary zones.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 6.1 | Boundary polygon calibration tool (one-time setup UI) | 🔴 Not Started | Map physical arena to pixel coords |
| 6.2 | Ball / player position extraction from frame | 🔴 Not Started | Depends on Phase 5 tracking |
| 6.3 | Point-in-polygon detection logic | 🔴 Not Started | Computational geometry — ray casting |
| 6.4 | 4-boundary zone (rope crossed, not over head height) | 🔴 Not Started | — |
| 6.5 | 6-boundary zone (ball over head height or out of arena) | 🔴 Not Started | — |
| 6.6 | Emit boundary event as AI suggestion | 🔴 Not Started | — |
| 6.7 | Visual overlay on CameraX preview showing zones | 🔴 Not Started | Nice-to-have for operator confidence |

---

## Phase 7 — AI-Assisted Wicket Detection
> Goal: Detect wickets (bowled, run-out, catch) from video frames.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 7.1 | Wicket geometry detection (stumps disturbed) | 🔴 Not Started | YOLOv8 model or rule-based |
| 7.2 | Run-out detection (batter out of crease + ball in) | 🔴 Not Started | Combine crease tracking + ball position |
| 7.3 | Catch detection (fielder catches airborne ball) | 🔴 Not Started | High complexity — lower priority |
| 7.4 | YOLOv8 model integration on Android (TFLite export) | 🔴 Not Started | Convert `.pt` → `.tflite` |
| 7.5 | Confidence-gated wicket suggestion (human confirms) | 🔴 Not Started | Use existing confirm/reject UI |
| 7.6 | False-positive suppression (missed deliveries) | 🔴 Not Started | — |

---

## 🧰 Cross-Cutting Concerns

| # | Task | Status | Notes |
|---|------|--------|-------|
| X.1 | Unit tests — ViewModel logic | 🔴 Not Started | JUnit4 setup present |
| X.2 | Unit tests — Backend endpoints | 🔴 Not Started | `pytest` + `httpx` |
| X.3 | `requirements.txt` complete & pinned | 🟡 In Progress | [requirements.txt](file:///d:/boxCric/boxCric/backend/requirements.txt) |
| X.4 | `README.md` — local dev setup instructions | 🟡 In Progress | [README.md](file:///d:/boxCric/boxCric/README.md) |
| X.5 | Backend: switch SQLite → PostgreSQL for production | 🔴 Not Started | Currently using SQLite (`boxcricket.db`) |
| X.6 | App dark mode polish / branding | ✅ Done | Dark bg `#0D1117`, full Material3 theme |
| X.7 | Error state handling (network, permissions) | ✅ Done | `snackBarMessage` flow + WS auto-reconnect |
| X.8 | Logging / debug overlay for AI events | 🔴 Not Started | — |

---

## 🗂️ Key Files Reference

| Layer | File | Purpose |
|-------|------|---------|
| Android | [MainActivity.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) | Entry point, nav graph, permissions |
| Android | [ScorerViewModel.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) | State + actions |
| Android | [ScoreboardScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) | Main scoring UI |
| Android | [MatchSetupScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/MatchSetupScreen.kt) | Match & team setup |
| Android | [SummaryScreen.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/ui/SummaryScreen.kt) | Post-match summary |
| Android | [WebSocketManager.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) | Ktor WS client (with auto-reconnect + camera role) |
| Android | [ApiClient.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/network/ApiClient.kt) | HTTP REST client |
| Android | [Models.kt](file:///d:/boxCric/boxCric/mobile/app/src/main/java/com/boxai/scorer/data/Models.kt) | Data classes |
| Backend | [main.py](file:///d:/boxCric/boxCric/backend/app/main.py) | FastAPI app + WS endpoint + reconciliation |
| Backend | [models.py](file:///d:/boxCric/boxCric/backend/app/models.py) | SQLAlchemy ORM models |
| Backend | [database.py](file:///d:/boxCric/boxCric/backend/app/database.py) | DB engine + session |
| Backend | [websocket_manager.py](file:///d:/boxCric/boxCric/backend/app/websocket_manager.py) | WS connection registry + dual-camera merge |
| Backend | [schemas.py](file:///d:/boxCric/boxCric/backend/app/schemas.py) | Pydantic request/response schemas |
| Docs | [PRD.md](file:///d:/boxCric/boxCric/PRD.md) | Product Requirements |

---

## ✅ Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Done | Implemented and committed |
| 🟡 In Progress | Partially implemented or stubbed |
| 🔴 Not Started | Planned, no code yet |
