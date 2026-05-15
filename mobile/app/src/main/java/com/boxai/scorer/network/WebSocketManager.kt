package com.boxai.scorer.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.seconds

class WebSocketManager {

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000L // keep-alive ping every 20s
        }
    }

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    // Inbound messages broadcast to ViewModel
    private val _incomingMessages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 32)
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private var session: DefaultClientWebSocketSession? = null
    private var reconnectJob: Job? = null
    private var currentIp: String = ""
    private var currentMatchId: Int = 1
    private val port = 8000

    // ── Connect with auto-reconnect ──────────────────────────────────────────

    fun connect(scope: CoroutineScope, ip: String, matchId: Int = 1, cameraRole: String = "CAM1") {
        currentIp = ip
        currentMatchId = matchId
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (isActive) {
                try {
                    client.webSocket(
                        host = currentIp,
                        port = port,
                        path = "/ws/match/$currentMatchId?camera=$cameraRole"
                    ) {
                        session = this
                        _connectionStatus.value = true
                        // Pump incoming frames
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                _incomingMessages.tryEmit(frame.readText())
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    // Connection dropped — wait 3 s then retry
                }
                session = null
                _connectionStatus.value = false
                delay(3.seconds)
            }
        }
    }

    // ── Send JSON payload ─────────────────────────────────────────────────────

    suspend fun sendEvent(json: String) {
        try {
            session?.send(Frame.Text(json))
        } catch (_: Exception) {
            /* Silently drop if not connected; reconnect loop will restore session */
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    fun disconnect() {
        reconnectJob?.cancel()
        session?.cancel()
        session = null
        _connectionStatus.value = false
    }
}
