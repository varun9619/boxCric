package com.boxai.scorer.data

// ── Team & Player ─────────────────────────────────────────────────────────────

data class Player(
    val id: Int,
    val name: String,
    val role: String = "batsman" // "batsman", "bowler", "all-rounder"
)

data class Team(
    val id: Int,
    val name: String,
    val players: List<Player> = emptyList()
)

// ── Match Setup (filled on Setup Screen) ──────────────────────────────────────

data class MatchSetup(
    val teamA: Team = Team(1, ""),
    val teamB: Team = Team(2, ""),
    val totalOvers: Int = 6,
    val cameraRole: CameraRole = CameraRole.DEVICE_1,
    val serverIp: String = "192.168.0.125",
    val matchId: Int = 1,
    val tossWinnerTeamId: Int = 1, // 1 for Team A, 2 for Team B
    val tossDecision: String = "BAT" // "BAT" or "BOWL"
)

enum class CameraRole(val label: String) {
    DEVICE_1("Device 1 – Behind Keeper"),
    DEVICE_2("Device 2 – Side Angle"),
    SPECTATOR("Device 3 – Spectator / Umpire")
}

// ── Live Match State ───────────────────────────────────────────────────────────

data class MatchState(
    val matchId: Int = 0,
    val battingTeam: Team? = null,
    val bowlingTeam: Team? = null,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    // Legal ball count within the current over (0–5)
    val balls: Int = 0,
    // Completed overs count
    val overs: Int = 0,
    // Legal deliveries only (extras excluded)
    val legalBallsDelivered: Int = 0,
    // Total deliveries including extras
    val totalBallsDelivered: Int = 0,
    val target: Int? = null,
    val striker: Player? = null,
    val nonStriker: Player? = null,
    val currentBowler: Player? = null,
    val isMatchComplete: Boolean = false,
    val inning: Int = 1,  // 1 = first innings, 2 = second innings
    // Configured total overs for this match
    val maxOvers: Int = 6
) {
    val runRate: Float
        get() {
            val totalOversFloat = overs + balls / 6f
            return if (totalOversFloat == 0f) 0f else totalRuns / totalOversFloat
        }

    val ballsRemaining: Int
        get() = (maxOvers * 6) - legalBallsDelivered

    val requiredRunRate: Float?
        get() {
            val t = target ?: return null
            val needed = t - totalRuns
            val remaining = ballsRemaining
            return if (remaining <= 0) null else (needed * 6f) / remaining
        }
}

// ── Ball Event ────────────────────────────────────────────────────────────────

data class BallEvent(
    val eventId: Int = 0,
    val over: Int = 0,
    val ball: Int = 0,
    val runs: Int = 0,
    val isWicket: Boolean = false,
    val isBoundary: Boolean = false,
    val isWide: Boolean = false,
    val isNoBall: Boolean = false,
    val description: String = "",
    val confirmedByOperator: Boolean = false,
    val aiSuggested: Boolean = false,
    val cameraSource: String = "manual"
) {
    /** Extras (wide / no-ball) do not count as a legal delivery */
    val isLegalDelivery: Boolean get() = !isWide && !isNoBall

    fun toBallLabel(): String = when {
        isWicket -> "W"
        isBoundary && runs == 6 -> "6"
        isBoundary && runs == 4 -> "4"
        isWide -> "Wd"
        isNoBall -> "Nb"
        else -> "$runs"
    }
}

// ── AI Suggestion ─────────────────────────────────────────────────────────────

data class AiSuggestion(
    val suggestedRuns: Int,
    val confidence: Float,
    val description: String,
    val eventType: String = "run", // "run", "boundary", "wicket"
    val cameraSource: String = "cam1"
)

// ── Post-Match Stats ──────────────────────────────────────────────────────────

data class BattingCard(
    val player: Player,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val isOut: Boolean
) {
    val strikeRate: Float get() = if (ballsFaced == 0) 0f else (runs * 100f) / ballsFaced
}

data class BowlingCard(
    val player: Player,
    val overs: Float,
    val runsConceded: Int,
    val wickets: Int
) {
    val economy: Float get() = if (overs == 0f) 0f else runsConceded / overs
}

data class MatchSummary(
    val teamA: Team,
    val teamB: Team,
    val teamAScore: Int,
    val teamAWickets: Int,
    val teamAOvers: String,
    val teamBScore: Int,
    val teamBWickets: Int,
    val teamBOvers: String,
    val winner: String,
    val mvp: Player? = null,
    val totalBalls: List<BallEvent> = emptyList()
)
