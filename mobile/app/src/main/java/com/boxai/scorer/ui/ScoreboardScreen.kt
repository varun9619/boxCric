package com.boxai.scorer.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.boxai.scorer.data.AiSuggestion
import com.boxai.scorer.data.BallEvent
import com.boxai.scorer.data.MatchState
import kotlin.math.roundToInt

// ─── Color Palette ────────────────────────────────────────────────────────────
val DarkBg        = Color(0xFF0D1117)
val CardBg        = Color(0xFF161B22)
val AccentGreen   = Color(0xFF39D98A)
val AccentBlue    = Color(0xFF4F9CF9)
val AccentOrange  = Color(0xFFFF8C42)
val AccentRed     = Color(0xFFFF5757)
val AccentPurple  = Color(0xFF7B2FBE)
val TextPrimary   = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)

// ─── Scoreboard Screen ────────────────────────────────────────────────────────
@Composable
fun ScoreboardScreen(
    matchState: MatchState,
    pendingSuggestion: AiSuggestion?,
    ballHistory: List<BallEvent>,
    isConnected: Boolean,
    isSpectator: Boolean = false,
    onAddRuns: (Int) -> Unit,
    onWicket: () -> Unit,
    onExtra: (String) -> Unit,
    onConfirmAi: () -> Unit,
    onRejectAi: () -> Unit,
    onFourClicked: () -> Unit,
    onSixClicked: () -> Unit,
    onFaceDetected: (android.graphics.Bitmap) -> Unit = {},
    onEndInnings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HeaderBar(isConnected = isConnected, inning = matchState.inning, matchId = matchState.matchId)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isSpectator) {
                // Visible camera preview for aiming at batters
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    com.boxai.scorer.camera.CameraPreview(
                        onFaceDetected = onFaceDetected,
                        modifier = Modifier.fillMaxSize(),
                        active = true
                    )
                    Text(
                        "Live AI Camera",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            MainScoreCard(matchState = matchState)
            Spacer(modifier = Modifier.height(16.dp))

            // AI Suggestion Banner
            AnimatedVisibility(
                visible = pendingSuggestion != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                pendingSuggestion?.let {
                    AiSuggestionBanner(
                        suggestion = it,
                        onConfirm = onConfirmAi,
                        onReject = onRejectAi
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Run rate stats row
            StatsRow(matchState = matchState)
            Spacer(modifier = Modifier.height(16.dp))

            // Ball History
            if (ballHistory.isNotEmpty()) {
                BallHistoryRow(balls = ballHistory)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scoring Buttons
            ScoringPad(
                onAddRuns = onAddRuns,
                onWicket = onWicket,
                onExtra = onExtra,
                onFour = onFourClicked,
                onSix = onSixClicked
            )

            Spacer(modifier = Modifier.height(16.dp))

            // End Innings button
            OutlinedButton(
                onClick = onEndInnings,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, TextSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (matchState.inning == 1) "End Innings →" else "End Match →",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HeaderBar(isConnected: Boolean, inning: Int = 1, matchId: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "🏏 Box Cricket AI",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${if (inning == 1) "1st" else "2nd"} Innings",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            // Match ID badge — prominently shown so other devices can join
            if (matchId > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrange.copy(alpha = 0.15f))
                        .border(1.dp, AccentOrange, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Match #$matchId",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isConnected) AccentGreen else AccentRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isConnected) "Live" else "Offline",
                    color = if (isConnected) AccentGreen else AccentRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun MainScoreCard(matchState: MatchState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1))
                )
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${matchState.battingTeam?.name ?: "Team A"} vs ${matchState.bowlingTeam?.name ?: "Team B"}",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "${matchState.totalRuns}/${matchState.wickets}",
                color = TextPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${matchState.overs}.${matchState.balls} overs",
                color = AccentGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            matchState.target?.let {
                Spacer(modifier = Modifier.height(8.dp))
                val needed = it - matchState.totalRuns
                val colour = if (needed <= 0) AccentGreen else AccentOrange
                Text(
                    if (needed <= 0) "🎉 Target Reached!" else "Need $needed more to win",
                    color = colour,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StatsRow(matchState: MatchState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            label = "Run Rate",
            value = "%.2f".format(matchState.runRate),
            modifier = Modifier.weight(1f)
        )
        matchState.requiredRunRate?.let { rrr ->
            StatChip(
                label = "Req. Rate",
                value = "%.2f".format(rrr),
                valueColor = if (rrr > 12f) AccentRed else AccentOrange,
                modifier = Modifier.weight(1f)
            )
        }
        StatChip(
            label = "Balls Left",
            value = "${matchState.ballsRemaining}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    valueColor: Color = AccentBlue,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun AiSuggestionBanner(
    suggestion: AiSuggestion,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F2B1F))
            .border(1.dp, AccentGreen, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("AI Suggestion", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(suggestion.description, color = TextSecondary, fontSize = 12.sp)
                    Text(
                        "Source: ${suggestion.cameraSource}",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${(suggestion.confidence * 100).roundToInt()}%",
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text(
                        "✓ Confirm ${suggestion.suggestedRuns} Run(s)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AccentRed)
                ) {
                    Text("✗ Reject", color = AccentRed)
                }
            }
        }
    }
}

@Composable
fun BallHistoryRow(balls: List<BallEvent>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ball History", color = TextSecondary, fontSize = 13.sp)
            Text("Last ${minOf(balls.size, 12)} balls", color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(balls.takeLast(12).reversed()) { ball ->
                val (bg, label) = when {
                    ball.isWicket -> AccentRed to "W"
                    ball.isBoundary && ball.runs == 6 -> AccentPurple to "6"
                    ball.isBoundary && ball.runs == 4 -> AccentBlue to "4"
                    ball.isWide -> AccentOrange to "Wd"
                    ball.isNoBall -> AccentOrange to "Nb"
                    else -> CardBg to "${ball.runs}"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (ball.aiSuggested) {
                        Text("AI", color = AccentGreen, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoringPad(
    onAddRuns: (Int) -> Unit,
    onWicket: () -> Unit,
    onExtra: (String) -> Unit,
    onFour: () -> Unit,
    onSix: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Scoring Pad", color = TextSecondary, fontSize = 13.sp)

        // Runs Row: 0–3
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (r in 0..3) {
                ScoringButton(
                    label = "$r",
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onAddRuns(r) }
                )
            }
        }

        // Boundaries
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ScoringButton("FOUR", AccentBlue, Modifier.weight(1f)) { onFour() }
            ScoringButton("SIX", AccentPurple, Modifier.weight(1f)) { onSix() }
        }

        // Extras + Wicket
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ScoringButton("Wide", AccentOrange, Modifier.weight(1f)) { onExtra("wide") }
            ScoringButton("No Ball", AccentOrange, Modifier.weight(1f)) { onExtra("noball") }
            ScoringButton("WICKET", AccentRed, Modifier.weight(1f)) { onWicket() }
        }
    }
}

@Composable
fun ScoringButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
