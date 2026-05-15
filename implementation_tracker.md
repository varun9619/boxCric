# 🏏 AI Box Cricket Scorer — Implementation Tracker

> **Last Updated:** 2026-05-15
> **Stack:** Android (Kotlin / Jetpack Compose) + Python (FastAPI / SQLAlchemy)
> **Repo Root:** `d:\boxai`

---

## 📊 Overall Progress

| Phase | Title | Status | Progress |
|-------|-------|--------|----------|
| 1 | Manual Scoring Android App | 🟡 In Progress | ████████░░ 75% |
| 2 | Real-Time Phone Sync (FastAPI + WebSockets) | 🟡 In Progress | ██████░░░░ 55% |
| 3 | Player Face Recognition | 🔴 Not Started | ░░░░░░░░░░ 0% |
| 4 | Automatic Run Inference (Crease Tracking) | 🔴 Not Started | ░░░░░░░░░░ 0% |
| 5 | Boundary Detection (Geometric Zones) | 🔴 Not Started | ░░░░░░░░░░ 0% |
| 6 | AI-Assisted Wicket Detection | 🔴 Not Started | ░░░░░░░░░░ 0% |

**Total Project Completion: ~22%**

---

## Phase 1 — Manual Scoring Android App
> Goal: Base architecture, UI, and manual human-operated scoring.

### 🏗️ Architecture & Setup

| # | Task | Status | File |
|---|------|--------|------|
| 1.1 | Android project scaffolding (namespace, SDK config) | ✅ Done | [build.gradle.kts](file:///d:/boxai/mobile/app/build.gradle.kts) |
| 1.2 | Jetpack Compose + Material3 setup | ✅ Done | [build.gradle.kts](file:///d:/boxai/mobile/app/build.gradle.kts) |
| 1.3 | CameraX dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxai/mobile/app/build.gradle.kts) |
| 1.4 | TFLite dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxai/mobile/app/build.gradle.kts) |
| 1.5 | Ktor WebSocket client dependency added | ✅ Done | [build.gradle.kts](file:///d:/boxai/mobile/app/build.gradle.kts) |
| 1.6 | Camera permission request flow | ✅ Done | [MainActivity.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) |

### 📱 UI Screens

| # | Task | Status | File |
|---|------|--------|------|
| 1.7 | `ConnectScreen` — IP input & connect button | ✅ Done | [ConnectScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ConnectScreen.kt) |
| 1.8 | `ScoreboardScreen` — live score display | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.9 | Manual run buttons (1, 2, 3, 4, 6) | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.10 | Wicket button | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.11 | Extra (wide/no-ball) button | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.12 | AI suggestion confirm/reject UI card | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.13 | Ball history timeline display | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.14 | Connection status indicator | ✅ Done | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) |
| 1.15 | Match Setup screen (team/player entry) | 🔴 Not Started | — |
| 1.16 | Post-match summary screen | 🔴 Not Started | — |
| 1.17 | Navigation between Setup → Live → Summary | 🟡 In Progress | [MainActivity.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) |

### 🧠 ViewModel & State

| # | Task | Status | File |
|---|------|--------|------|
| 1.18 | `ScorerViewModel` with match state flows | ✅ Done | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.19 | `addRuns()`, `addWicket()`, `addExtra()` actions | ✅ Done | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.20 | `confirmAiSuggestion()` / `rejectAiSuggestion()` | ✅ Done | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.21 | Ball history accumulation | ✅ Done | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.22 | Over/ball counter logic | 🟡 In Progress | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 1.23 | Data models (MatchState, BallEvent, AiSuggestion) | ✅ Done | [Models.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/data/Models.kt) |

---

## Phase 2 — Real-Time Phone Sync (FastAPI + WebSockets)
> Goal: Dual-phone synchronization over local Wi-Fi via a Python backend.

### 🐍 Backend — FastAPI

| # | Task | Status | File |
|---|------|--------|------|
| 2.1 | FastAPI app bootstrap | ✅ Done | [main.py](file:///d:/boxai/backend/app/main.py) |
| 2.2 | SQLAlchemy ORM setup + SQLite/PostgreSQL engine | ✅ Done | [database.py](file:///d:/boxai/backend/app/database.py) |
| 2.3 | `Player`, `Match`, `BallEvent`, `MatchState` models | ✅ Done | [models.py](file:///d:/boxai/backend/app/models.py) |
| 2.4 | WebSocket endpoint `/ws/match/{match_id}` | ✅ Done | [main.py](file:///d:/boxai/backend/app/main.py) |
| 2.5 | `WebSocketManager` — connect/disconnect/broadcast | ✅ Done | [websocket_manager.py](file:///d:/boxai/backend/app/websocket_manager.py) |
| 2.6 | REST API: `POST /matches` (create match) | 🔴 Not Started | — |
| 2.7 | REST API: `GET /matches/{id}` (fetch match state) | 🔴 Not Started | — |
| 2.8 | REST API: `POST /matches/{id}/events` (record event) | 🔴 Not Started | — |
| 2.9 | REST API: `GET /matches/{id}/summary` | 🔴 Not Started | — |
| 2.10 | Multi-camera event reconciliation logic | 🔴 Not Started | [main.py](file:///d:/boxai/backend/app/main.py) (`TODO` present) |
| 2.11 | Confidence-based event merging (Cam1 + Cam2) | 🔴 Not Started | — |
| 2.12 | Backend auth/session management (basic) | 🔴 Not Started | — |

### 📱 Android — Networking

| # | Task | Status | File |
|---|------|--------|------|
| 2.13 | Ktor WebSocket client setup | ✅ Done | [WebSocketManager.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) |
| 2.14 | `connectToServer(ip)` in ViewModel | ✅ Done | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.15 | Emit ball events to backend over WebSocket | 🟡 In Progress | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.16 | Receive & apply state updates from backend | 🟡 In Progress | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) |
| 2.17 | Reconnection / error handling on disconnect | 🔴 Not Started | — |
| 2.18 | Camera role assignment (Phone 1 vs Phone 2) | 🔴 Not Started | — |

---

## Phase 3 — Player Face Recognition
> Goal: Identify batters at the crease using face embeddings.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 3.1 | Face embedding generation pipeline (Python) | 🔴 Not Started | Use `deepface` or `facenet-pytorch` |
| 3.2 | Store face embeddings in DB (Player model ready) | 🔴 Not Started | Column exists in model, logic missing |
| 3.3 | TFLite face detection model on Android | 🔴 Not Started | Add ML Kit or custom model |
| 3.4 | Real-time batter identification from CameraX frame | 🔴 Not Started | — |
| 3.5 | Match recognized face to player DB record | 🔴 Not Started | — |
| 3.6 | Player registration UI (photo capture + save) | 🔴 Not Started | — |
| 3.7 | Confidence threshold / fallback to manual | 🔴 Not Started | — |

---

## Phase 4 — Automatic Run Inference (Crease Tracking)
> Goal: Detect crease crossings using MediaPipe Pose + ByteTrack.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 4.1 | Add MediaPipe Tasks for Android dependency | 🔴 Not Started | `com.google.mediapipe:tasks-vision` |
| 4.2 | CameraX frame → MediaPipe Pose inference pipeline | 🔴 Not Started | — |
| 4.3 | Define crease zone coordinates (pixel or physical) | 🔴 Not Started | — |
| 4.4 | Crease-crossing detection algorithm | 🔴 Not Started | Track foot landmark crossing threshold |
| 4.5 | ByteTrack / DeepSORT player tracking integration | 🔴 Not Started | May need JNI or Python-side processing |
| 4.6 | Run count inference from crease swap count | 🔴 Not Started | — |
| 4.7 | Emit inferred run event as AI suggestion (not confirmed) | 🔴 Not Started | Hook into existing AI suggestion confirm/reject UI |
| 4.8 | Dual-camera run confirmation reconciliation | 🔴 Not Started | Backend task 2.10–2.11 |

---

## Phase 5 — Boundary Detection (Geometric Zones)
> Goal: Detect 4s and 6s using predefined polygon boundary zones.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 5.1 | Boundary polygon calibration tool (one-time setup UI) | 🔴 Not Started | Map physical arena to pixel coords |
| 5.2 | Ball / player position extraction from frame | 🔴 Not Started | Depends on Phase 4 tracking |
| 5.3 | Point-in-polygon detection logic | 🔴 Not Started | Computational geometry — ray casting |
| 5.4 | 4-boundary zone (rope crossed, not over head height) | 🔴 Not Started | — |
| 5.5 | 6-boundary zone (ball over head height or out of arena) | 🔴 Not Started | — |
| 5.6 | Emit boundary event as AI suggestion | 🔴 Not Started | — |
| 5.7 | Visual overlay on CameraX preview showing zones | 🔴 Not Started | Nice-to-have for operator confidence |

---

## Phase 6 — AI-Assisted Wicket Detection
> Goal: Detect wickets (bowled, run-out, catch) from video frames.

| # | Task | Status | Notes |
|---|------|--------|-------|
| 6.1 | Wicket geometry detection (stumps disturbed) | 🔴 Not Started | YOLOv8 model or rule-based |
| 6.2 | Run-out detection (batter out of crease + ball in) | 🔴 Not Started | Combine crease tracking + ball position |
| 6.3 | Catch detection (fielder catches airborne ball) | 🔴 Not Started | High complexity — lower priority |
| 6.4 | YOLOv8 model integration on Android (TFLite export) | 🔴 Not Started | Convert `.pt` → `.tflite` |
| 6.5 | Confidence-gated wicket suggestion (human confirms) | 🔴 Not Started | Use existing confirm/reject UI |
| 6.6 | False-positive suppression (missed deliveries) | 🔴 Not Started | — |

---

## 🧰 Cross-Cutting Concerns

| # | Task | Status | Notes |
|---|------|--------|-------|
| X.1 | Unit tests — ViewModel logic | 🔴 Not Started | JUnit4 setup present |
| X.2 | Unit tests — Backend endpoints | 🔴 Not Started | `pytest` + `httpx` |
| X.3 | `requirements.txt` complete & pinned | 🟡 In Progress | [requirements.txt](file:///d:/boxai/backend/requirements.txt) |
| X.4 | `README.md` — local dev setup instructions | 🟡 In Progress | [README.md](file:///d:/boxai/README.md) |
| X.5 | Backend: switch SQLite → PostgreSQL for production | 🔴 Not Started | Currently using SQLite (`boxcricket.db`) |
| X.6 | App dark mode polish / branding | 🟡 In Progress | Dark bg `#0D1117` set in `MainActivity` |
| X.7 | Error state handling (network, permissions) | 🔴 Not Started | — |
| X.8 | Logging / debug overlay for AI events | 🔴 Not Started | — |

---

## 🗂️ Key Files Reference

| Layer | File | Purpose |
|-------|------|---------|
| Android | [MainActivity.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/MainActivity.kt) | Entry point, nav graph, permissions |
| Android | [ScorerViewModel.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScorerViewModel.kt) | State + actions |
| Android | [ScoreboardScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ScoreboardScreen.kt) | Main scoring UI |
| Android | [ConnectScreen.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/ui/ConnectScreen.kt) | Server IP entry |
| Android | [WebSocketManager.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/network/WebSocketManager.kt) | Ktor WS client |
| Android | [Models.kt](file:///d:/boxai/mobile/app/src/main/java/com/boxai/scorer/data/Models.kt) | Data classes |
| Backend | [main.py](file:///d:/boxai/backend/app/main.py) | FastAPI app + WS endpoint |
| Backend | [models.py](file:///d:/boxai/backend/app/models.py) | SQLAlchemy ORM models |
| Backend | [database.py](file:///d:/boxai/backend/app/database.py) | DB engine + session |
| Backend | [websocket_manager.py](file:///d:/boxai/backend/app/websocket_manager.py) | WS connection registry |
| Docs | [PRD.md](file:///d:/boxai/PRD.md) | Product Requirements |

---

## ✅ Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Done | Implemented and committed |
| 🟡 In Progress | Partially implemented or stubbed |
| 🔴 Not Started | Planned, no code yet |
