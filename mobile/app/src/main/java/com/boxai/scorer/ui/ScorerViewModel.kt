package com.boxai.scorer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxai.scorer.data.*
import com.boxai.scorer.network.ApiClient
import com.boxai.scorer.network.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class ScorerViewModel : ViewModel() {

    // ── Network ───────────────────────────────────────────────────────────────

    private val wsManager = WebSocketManager()
    private var apiClient: ApiClient? = null

    val connectionStatus: StateFlow<Boolean> = wsManager.connectionStatus

    // ── Match Setup ───────────────────────────────────────────────────────────

    private val _matchSetup = MutableStateFlow(MatchSetup())
    val matchSetup: StateFlow<MatchSetup> = _matchSetup

    // ── Live Match State ──────────────────────────────────────────────────────

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState

    // ── AI Suggestion ─────────────────────────────────────────────────────────

    private val _pendingSuggestion = MutableStateFlow<AiSuggestion?>(null)
    val pendingSuggestion: StateFlow<AiSuggestion?> = _pendingSuggestion

    // ── Ball History ──────────────────────────────────────────────────────────

    private val _ballHistory = MutableStateFlow<List<BallEvent>>(emptyList())
    val ballHistory: StateFlow<List<BallEvent>> = _ballHistory

    // ── Match Summary (populated at end of match) ─────────────────────────────

    private val _matchSummary = MutableStateFlow<MatchSummary?>(null)
    val matchSummary: StateFlow<MatchSummary?> = _matchSummary

    // ── Navigation events ─────────────────────────────────────────────────────

    private val _navEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<String> = _navEvent

    // ─────────────────────────────────────────────────────────────────────────
    // Match Setup
    // ─────────────────────────────────────────────────────────────────────────

    fun updateSetup(setup: MatchSetup) { _matchSetup.value = setup }

    fun startMatch() {
        val setup = _matchSetup.value
        _matchState.value = MatchState(
            matchId = setup.matchId,
            battingTeam = setup.teamA,
            bowlingTeam = setup.teamB,
            target = null,
            inning = 1
        )
        _ballHistory.value = emptyList()
        _pendingSuggestion.value = null

        // Initialise network
        apiClient = ApiClient(ip = setup.serverIp)
        viewModelScope.launch {
            apiClient?.createMatch(setup.teamA.name, setup.teamB.name, setup.totalOvers)
        }
        wsManager.connect(viewModelScope, setup.serverIp, setup.matchId)

        // Listen for inbound WS messages (AI suggestions, state sync from Cam2)
        viewModelScope.launch {
            wsManager.incomingMessages.collect { raw -> handleIncomingMessage(raw) }
        }

        _navEvent.tryEmit("scoreboard")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ball Recording
    // ─────────────────────────────────────────────────────────────────────────

    fun addRuns(runs: Int, isBoundary: Boolean = false) {
        val current = _matchState.value
        val event = buildEvent(
            runs = runs,
            isBoundary = isBoundary,
            description = if (isBoundary) (if (runs == 6) "SIX! 🎉" else "FOUR! 🏏") else "$runs run(s)",
            isLegal = true
        )
        commitEvent(event)
        sendWsEvent("""{"type":"BALL","runs":$runs,"is_boundary":$isBoundary,"camera":"${_matchSetup.value.cameraRole.name}"}""")
        postEventToApi("boundary", runs)
        checkMatchComplete()
    }

    fun addWicket() {
        val event = buildEvent(
            isWicket = true,
            description = "WICKET! 🔴",
            isLegal = true
        )
        commitEvent(event)
        sendWsEvent("""{"type":"WICKET","camera":"${_matchSetup.value.cameraRole.name}"}""")
        postEventToApi("wicket", 0)
        checkMatchComplete()
    }

    fun addExtra(type: String) {
        val isWide = type == "wide"
        val event = buildEvent(
            runs = 1,
            isWide = isWide,
            isNoBall = !isWide,
            description = if (isWide) "Wide" else "No Ball",
            isLegal = false   // extras don't advance the ball count
        )
        commitEvent(event)
        sendWsEvent("""{"type":"EXTRA","extra_type":"$type","camera":"${_matchSetup.value.cameraRole.name}"}""")
        postEventToApi(type, 1)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI Suggestion Flow
    // ─────────────────────────────────────────────────────────────────────────

    fun simulateAiSuggestion(runs: Int, confidence: Float = 0.87f) {
        _pendingSuggestion.value = AiSuggestion(
            suggestedRuns = runs,
            confidence = confidence,
            description = "AI detected batsmen crossed crease $runs time(s)",
            eventType = "run",
            cameraSource = _matchSetup.value.cameraRole.name
        )
    }

    fun confirmAiSuggestion() {
        _pendingSuggestion.value?.let { addRuns(it.suggestedRuns) }
        _pendingSuggestion.value = null
    }

    fun rejectAiSuggestion() { _pendingSuggestion.value = null }

    // ─────────────────────────────────────────────────────────────────────────
    // Innings Management
    // ─────────────────────────────────────────────────────────────────────────

    fun startSecondInnings() {
        val current = _matchState.value
        val setup = _matchSetup.value
        _matchState.value = MatchState(
            matchId = current.matchId,
            battingTeam = setup.teamB,
            bowlingTeam = setup.teamA,
            target = current.totalRuns + 1,
            inning = 2
        )
        _ballHistory.value = emptyList()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildEvent(
        runs: Int = 0,
        isWicket: Boolean = false,
        isBoundary: Boolean = false,
        isWide: Boolean = false,
        isNoBall: Boolean = false,
        description: String = "",
        isLegal: Boolean = true,
        aiSuggested: Boolean = false
    ): BallEvent {
        val current = _matchState.value
        return BallEvent(
            eventId = _ballHistory.value.size + 1,
            over = current.overs,
            ball = current.balls,
            runs = runs,
            isWicket = isWicket,
            isBoundary = isBoundary,
            isWide = isWide,
            isNoBall = isNoBall,
            description = description,
            confirmedByOperator = true,
            aiSuggested = aiSuggested,
            cameraSource = _matchSetup.value.cameraRole.name
        )
    }

    private fun commitEvent(event: BallEvent) {
        _ballHistory.value = _ballHistory.value + event
        val current = _matchState.value

        // Advance over/ball counter only for legal deliveries
        val newBalls = if (event.isLegalDelivery) {
            if (current.balls == 5) 0 else current.balls + 1
        } else current.balls

        val newOvers = if (event.isLegalDelivery && current.balls == 5) {
            current.overs + 1
        } else current.overs

        _matchState.value = current.copy(
            totalRuns = current.totalRuns + event.runs,
            wickets = current.wickets + if (event.isWicket) 1 else 0,
            balls = newBalls,
            overs = newOvers,
            totalBallsDelivered = current.totalBallsDelivered + 1
        )
    }

    private fun checkMatchComplete() {
        val state = _matchState.value
        val setup = _matchSetup.value
        val oversComplete = state.overs >= setup.totalOvers
        val allOut = state.wickets >= (state.battingTeam?.players?.size?.minus(1) ?: 9)
        val targetMet = state.target?.let { state.totalRuns >= it } ?: false

        if (oversComplete || allOut || targetMet) {
            if (state.inning == 1) {
                _navEvent.tryEmit("innings_break")
            } else {
                _matchSummary.value = buildSummary()
                _matchState.value = state.copy(isMatchComplete = true)
                _navEvent.tryEmit("summary")
            }
        }
    }

    private fun buildSummary(): MatchSummary {
        val state = _matchState.value
        val setup = _matchSetup.value
        val winner = when {
            state.target != null && state.totalRuns >= state.target -> setup.teamB.name
            else -> setup.teamA.name
        }
        return MatchSummary(
            teamA = setup.teamA,
            teamB = setup.teamB,
            teamAScore = if (state.inning == 2) state.target?.minus(1) ?: 0 else state.totalRuns,
            teamAWickets = 0,
            teamAOvers = "${setup.totalOvers}.0",
            teamBScore = state.totalRuns,
            teamBWickets = state.wickets,
            teamBOvers = "${state.overs}.${state.balls}",
            winner = winner,
            totalBalls = _ballHistory.value
        )
    }

    private fun handleIncomingMessage(raw: String) {
        try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "ai_suggestion" -> {
                    val runs = json.optInt("runs", 0)
                    val confidence = json.optDouble("confidence", 0.8).toFloat()
                    val desc = json.optString("description", "AI detected $runs run(s)")
                    _pendingSuggestion.value = AiSuggestion(
                        suggestedRuns = runs,
                        confidence = confidence,
                        description = desc,
                        cameraSource = json.optString("camera", "cam1")
                    )
                }
                "state_update" -> {
                    // Optionally merge state from Cam2 here
                }
            }
        } catch (_: Exception) { /* ignore malformed JSON */ }
    }

    private fun sendWsEvent(json: String) {
        viewModelScope.launch { wsManager.sendEvent(json) }
    }

    private fun postEventToApi(eventType: String, runs: Int) {
        val state = _matchState.value
        val setup = _matchSetup.value
        viewModelScope.launch {
            apiClient?.postEvent(
                matchId = state.matchId,
                eventType = eventType,
                runs = runs,
                cameraId = setup.cameraRole.name
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
        apiClient?.close()
    }
}
