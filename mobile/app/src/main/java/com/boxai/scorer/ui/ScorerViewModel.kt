package com.boxai.scorer.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boxai.scorer.data.*
import com.boxai.scorer.network.ApiClient
import com.boxai.scorer.network.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ScorerViewModel(application: Application) : AndroidViewModel(application) {

    // ── SharedPreferences (persists across app restarts) ─────────────────────
    private val prefs: SharedPreferences =
        application.getSharedPreferences("boxcric_setup", Context.MODE_PRIVATE)

    // ── Network ───────────────────────────────────────────────────────────────

    private val wsManager = WebSocketManager()
    private var apiClient: ApiClient? = null

    val connectionStatus: StateFlow<Boolean> = wsManager.connectionStatus

    // ── Snackbar / error messages ─────────────────────────────────────────────

    private val _snackBarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackBarMessage: SharedFlow<String> = _snackBarMessage

    // ── Match Setup ───────────────────────────────────────────────────────────

    private val _matchSetup = MutableStateFlow(loadSetup())
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

    // ── First-innings snapshot (score at end of inning 1) ──────────────────
    // Saved when startSecondInnings() is called. Used to:
    //  (a) offset backend cumulative scores during inning 2
    //  (b) fill teamA rows in the match summary accurately
    private var firstInningsRuns: Int = 0
    private var firstInningsWickets: Int = 0
    private var firstInningsOvers: Int = 0
    private var firstInningsBalls: Int = 0

    // ── Navigation events ─────────────────────────────────────────────────────

    private val _navEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<String> = _navEvent

    // ─────────────────────────────────────────────────────────────────────────
    // Match Setup
    // ─────────────────────────────────────────────────────────────────────────

    fun updateSetup(setup: MatchSetup) {
        _matchSetup.value = setup
        saveSetup(setup)  // persist immediately so app-kill doesn't lose data
    }

    fun startMatch(registeredFaces: Map<Pair<Int, Int>, android.graphics.Bitmap> = emptyMap()) {
        val setup = _matchSetup.value
        apiClient = ApiClient(ip = setup.serverIp)
        
        viewModelScope.launch {
            var actualMatchId = setup.matchId
            try {
                val resp = apiClient?.createMatch(setup.teamA.name, setup.teamB.name, setup.totalOvers)
                if (resp != null) {
                    val json = org.json.JSONObject(resp)
                    actualMatchId = json.optInt("id", setup.matchId)
                }
            } catch (e: Exception) { }

            val finalSetup = setup.copy(matchId = actualMatchId)
            _matchSetup.value = finalSetup
            saveSetup(finalSetup) // Save the final setup with the assigned matchId

            val isTeamAWinner = finalSetup.tossWinnerTeamId == 1
            val winnerBats = finalSetup.tossDecision == "BAT"
            
            val teamABats = if (isTeamAWinner) winnerBats else !winnerBats
            
            val (batting, bowling) = if (teamABats) {
                finalSetup.teamA to finalSetup.teamB
            } else {
                finalSetup.teamB to finalSetup.teamA
            }

            _matchState.value = MatchState(
                matchId = finalSetup.matchId,
                battingTeam = batting,
                bowlingTeam = bowling,
                target = null,
                inning = 1,
                maxOvers = finalSetup.totalOvers
            )
            _ballHistory.value = emptyList()
            _pendingSuggestion.value = null

            // Create players and upload faces
            listOf(finalSetup.teamA, finalSetup.teamB).forEach { team ->
                team.players.forEachIndexed { idx, player ->
                    if (player.name.isNotBlank()) {
                        try {
                            val resp = apiClient?.createPlayer(player.name, team.id)
                            val json = org.json.JSONObject(resp ?: "")
                            val playerId = json.optInt("id", -1)
                            if (playerId != -1) {
                                val bitmap = registeredFaces[team.id to idx]
                                if (bitmap != null) {
                                    apiClient?.uploadFace(playerId, bitmap)
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
            }

            wsManager.connect(this, finalSetup.serverIp, finalSetup.matchId, finalSetup.cameraRole.name)
            
            // Listen for inbound WS messages (AI suggestions, state sync from Cam2)
            launch {
                wsManager.incomingMessages.collect { raw -> handleIncomingMessage(raw) }
            }

            _navEvent.tryEmit("scoreboard")
        }
    }

    /**
     * Phase 4: Join an existing match started by another device.
     * Fetches match config from server, restores state, and connects WebSocket.
     */
    fun joinMatch(matchId: Int, role: CameraRole, serverIp: String) {
        val setup = _matchSetup.value.copy(matchId = matchId, cameraRole = role, serverIp = serverIp)
        _matchSetup.value = setup
        apiClient = ApiClient(ip = serverIp)

        viewModelScope.launch {
            try {
                // Fetch match info from server
                val resp = apiClient?.getMatch(matchId)
                if (resp != null) {
                    val json = org.json.JSONObject(resp)
                    val teamAName = json.optString("team_a", "Team A")
                    val teamBName = json.optString("team_b", "Team B")
                    val totalOvers = json.optInt("total_overs", 6)

                    val teamA = com.boxai.scorer.data.Team(1, teamAName)
                    val teamB = com.boxai.scorer.data.Team(2, teamBName)

                    _matchState.value = MatchState(
                        matchId = matchId,
                        battingTeam = teamA,
                        bowlingTeam = teamB,
                        maxOvers = totalOvers
                    )
                    _matchSetup.value = setup.copy(
                        teamA = teamA,
                        teamB = teamB,
                        totalOvers = totalOvers
                    )
                }
            } catch (e: Exception) {
                _snackBarMessage.tryEmit("⚠️ Could not fetch match #$matchId from server")
            }

            _ballHistory.value = emptyList()
            _pendingSuggestion.value = null

            wsManager.connect(this, serverIp, matchId, role.name)
            launch {
                wsManager.incomingMessages.collect { raw -> handleIncomingMessage(raw) }
            }

            _navEvent.tryEmit("scoreboard")
        }
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

    // ── Batter Recognition ────────────────────────────────────────────────────
    
    fun recognizeBatter(bitmap: android.graphics.Bitmap) {
        val state = _matchState.value
        if (state.matchId == 0) return
        
        viewModelScope.launch {
            val resp = apiClient?.recognizeBatter(state.matchId, bitmap)
            try {
                val json = org.json.JSONObject(resp ?: "")
                val playerName = json.optString("name", "")
                val confidence = json.optDouble("confidence", 0.0)
                if (playerName.isNotEmpty() && confidence > 0.65) {
                    _snackBarMessage.tryEmit("Batter Identified: $playerName (${(confidence * 100).toInt()}%)")
                }
            } catch(e: Exception) { }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Innings Management
    // ─────────────────────────────────────────────────────────────────────────

    fun startSecondInnings() {
        val current = _matchState.value
        val setup = _matchSetup.value

        // ── Snapshot inning 1 result before resetting state ────────────────────
        firstInningsRuns    = current.totalRuns
        firstInningsWickets = current.wickets
        firstInningsOvers   = current.overs
        firstInningsBalls   = current.balls

        _matchState.value = MatchState(
            matchId    = current.matchId,
            battingTeam = setup.teamB,
            bowlingTeam = setup.teamA,
            target  = current.totalRuns + 1,
            inning  = 2,
            maxOvers = setup.totalOvers
        )
        _ballHistory.value = emptyList()
    }

    fun endCurrentPhase() {
        val state = _matchState.value
        if (state.inning == 1) {
            _navEvent.tryEmit("innings_break")
        } else {
            _matchSummary.value = buildSummary()
            _matchState.value = state.copy(isMatchComplete = true)
            _navEvent.tryEmit("summary")
        }
    }

    /**
     * Restart immediately with the exact same teams, players and total overs.
     * Accepts fresh toss result from the user (picked in the dialog).
     */
    fun rematch(
        tossWinnerTeamId: Int = 1,
        tossDecision: String = "BAT",
        registeredFaces: Map<Pair<Int, Int>, android.graphics.Bitmap> = emptyMap()
    ) {
        _matchSummary.value = null
        // Reset first-innings snapshot so a clean match starts
        firstInningsRuns = 0; firstInningsWickets = 0
        firstInningsOvers = 0; firstInningsBalls = 0
        // Apply the new toss to the existing setup
        _matchSetup.value = _matchSetup.value.copy(
            tossWinnerTeamId = tossWinnerTeamId,
            tossDecision = tossDecision
        )
        startMatch(registeredFaces)
    }

    /** Clear summary without resetting setup — used when going back to setup screen. */
    fun clearSummary() { _matchSummary.value = null }

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

        val isLegal = event.isLegalDelivery

        // Only advance over/ball counter for legal deliveries
        val newBalls = if (isLegal) {
            if (current.balls == 5) 0 else current.balls + 1
        } else current.balls

        val newOvers = if (isLegal && current.balls == 5) {
            current.overs + 1
        } else current.overs

        _matchState.value = current.copy(
            totalRuns = current.totalRuns + event.runs,
            wickets = current.wickets + if (event.isWicket) 1 else 0,
            balls = newBalls,
            overs = newOvers,
            legalBallsDelivered = current.legalBallsDelivered + if (isLegal) 1 else 0,
            totalBallsDelivered = current.totalBallsDelivered + 1
        )
    }

    private fun checkMatchComplete() {
        val state = _matchState.value
        val setup = _matchSetup.value
        val oversComplete = state.overs >= setup.totalOvers
        
        val teamSize = state.battingTeam?.players?.size ?: 0
        val maxWickets = if (teamSize > 1) teamSize - 1 else 9
        val allOut = state.wickets >= maxWickets
        
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
        // Determine winner
        val winner = when {
            state.target != null && state.totalRuns >= state.target -> setup.teamB.name
            else -> setup.teamA.name
        }
        // Team A score comes from the snapshot taken at the inning break, NOT from target-1
        val teamAScore   = if (state.inning == 2) firstInningsRuns    else state.totalRuns
        val teamAWickets = if (state.inning == 2) firstInningsWickets else state.wickets
        val teamAOvers   = if (state.inning == 2)
            "${firstInningsOvers}.${firstInningsBalls}"
        else
            "${state.overs}.${state.balls}"
        return MatchSummary(
            teamA = setup.teamA,
            teamB = setup.teamB,
            teamAScore   = teamAScore,
            teamAWickets = teamAWickets,
            teamAOvers   = teamAOvers,
            teamBScore   = state.totalRuns,
            teamBWickets = state.wickets,
            teamBOvers   = "${state.overs}.${state.balls}",
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
                    // Apply authoritative state broadcast from backend (e.g. Cam2 confirmed event)
                    val rawBackendScore   = json.optInt("current_score", -1)
                    val rawBackendWickets = json.optInt("current_wickets", -1)
                    if (rawBackendScore >= 0) {
                        val current = _matchState.value
                        // The backend keeps a CUMULATIVE score across innings.
                        // In inning 2 we subtract the inning-1 offset so the
                        // local state always reflects the current-innings runs only.
                        val inningOffset  = if (current.inning == 2) firstInningsRuns else 0
                        val adjustedScore = rawBackendScore - inningOffset
                        // Only sync if there's a meaningful difference (avoids self-echo loops)
                        if (adjustedScore != current.totalRuns || rawBackendWickets != current.wickets) {
                            _matchState.value = current.copy(
                                totalRuns = adjustedScore.coerceAtLeast(0),
                                wickets = if (rawBackendWickets >= 0) rawBackendWickets else current.wickets
                            )
                        }
                    }
                }
                "error" -> {
                    val msg = json.optString("message", "Unknown server error")
                    _snackBarMessage.tryEmit("⚠️ Server: $msg")
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

    // ── Persistence Helpers ──────────────────────────────────────────────────

    private fun saveSetup(setup: MatchSetup) {
        try {
            val json = JSONObject().apply {
                put("serverIp", setup.serverIp)
                put("matchId", setup.matchId)
                put("totalOvers", setup.totalOvers)
                put("cameraRole", setup.cameraRole.name)
                put("tossWinnerTeamId", setup.tossWinnerTeamId)
                put("tossDecision", setup.tossDecision)
                
                put("teamA", JSONObject().apply {
                    put("id", setup.teamA.id)
                    put("name", setup.teamA.name)
                    put("players", JSONArray().apply {
                        setup.teamA.players.forEach { p ->
                            put(JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                            })
                        }
                    })
                })
                
                put("teamB", JSONObject().apply {
                    put("id", setup.teamB.id)
                    put("name", setup.teamB.name)
                    put("players", JSONArray().apply {
                        setup.teamB.players.forEach { p ->
                            put(JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                            })
                        }
                    })
                })
            }
            prefs.edit().putString("last_setup", json.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("ScorerViewModel", "Failed to save setup", e)
        }
    }

    private fun loadSetup(): MatchSetup {
        val jsonStr = prefs.getString("last_setup", null) ?: return MatchSetup()
        return try {
            val json = JSONObject(jsonStr)
            
            fun parseTeam(teamObj: JSONObject): Team {
                val pArray = teamObj.getJSONArray("players")
                val players = mutableListOf<Player>()
                for (i in 0 until pArray.length()) {
                    val pObj = pArray.getJSONObject(i)
                    players.add(Player(pObj.getInt("id"), pObj.getString("name")))
                }
                return Team(teamObj.getInt("id"), teamObj.getString("name"), players)
            }

            MatchSetup(
                serverIp = json.optString("serverIp", ""),
                matchId = json.optInt("matchId", 1),
                teamA = parseTeam(json.getJSONObject("teamA")),
                teamB = parseTeam(json.getJSONObject("teamB")),
                totalOvers = json.optInt("totalOvers", 6),
                cameraRole = CameraRole.valueOf(json.optString("cameraRole", CameraRole.DEVICE_1.name)),
                tossWinnerTeamId = json.optInt("tossWinnerTeamId", 1),
                tossDecision = json.optString("tossDecision", "BAT")
            )
        } catch (e: Exception) {
            android.util.Log.e("ScorerViewModel", "Failed to load setup", e)
            MatchSetup()
        }
    }
}
