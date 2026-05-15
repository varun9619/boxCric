package com.boxai.scorer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxai.scorer.data.BallEvent
import com.boxai.scorer.data.MatchSummary
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    summary: MatchSummary,
    onRematch: (tossWinnerTeamId: Int, tossDecision: String) -> Unit,
    onChangeTeams: () -> Unit
) {
    var showTossDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Winner banner ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1B4332), Color(0xFF0D1B2A))
                        )
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        summary.winner,
                        color = AccentGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text("wins the match!", color = TextSecondary, fontSize = 15.sp)
                }
            }

            // ── Score comparison ──────────────────────────────────────────────
            SummaryCard(title = "Final Scores") {
                ScoreCompareRow(
                    teamName = summary.teamA.name,
                    score = summary.teamAScore,
                    wickets = summary.teamAWickets,
                    overs = summary.teamAOvers,
                    isWinner = summary.winner == summary.teamA.name
                )
                Divider(color = TextSecondary.copy(alpha = 0.15f), thickness = 1.dp)
                ScoreCompareRow(
                    teamName = summary.teamB.name,
                    score = summary.teamBScore,
                    wickets = summary.teamBWickets,
                    overs = summary.teamBOvers,
                    isWinner = summary.winner == summary.teamB.name
                )
            }

            // ── Match Stats ───────────────────────────────────────────────────
            val totalRuns = summary.teamAScore + summary.teamBScore
            val boundaries = summary.totalBalls.count { it.isBoundary && it.runs == 4 }
            val sixes     = summary.totalBalls.count { it.isBoundary && it.runs == 6 }
            val wickets   = summary.totalBalls.count { it.isWicket }
            val extras    = summary.totalBalls.count { it.isWide || it.isNoBall }
            val aiConfirmed = summary.totalBalls.count { it.aiSuggested && it.confirmedByOperator }

            SummaryCard(title = "Match Stats") {
                StatGridRow(listOf(
                    "Total Runs" to "$totalRuns",
                    "Fours" to "$boundaries",
                    "Sixes" to "$sixes",
                    "Wickets" to "$wickets",
                    "Extras" to "$extras",
                    "AI Confirmed" to "$aiConfirmed"
                ))
            }

            // ── Ball-by-ball timeline ─────────────────────────────────────────
            if (summary.totalBalls.isNotEmpty()) {
                SummaryCard(title = "Ball by Ball") {
                    BallTimeline(balls = summary.totalBalls)
                }
            }

            // ── MVP ───────────────────────────────────────────────────────────
            summary.mvp?.let {
                SummaryCard(title = "Player of the Match") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(it.name, color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────────────────
            // "Same Teams" quick-restart card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B2B1B))
                    .border(1.dp, AccentGreen, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Play again with same teams?",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "${summary.teamA.name}  vs  ${summary.teamB.name}  •  ${summary.teamAOvers.substringBefore(".").toIntOrNull() ?: "?"} overs",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showTossDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("▶  Yes, Same Teams & Overs!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = onChangeTeams,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TextSecondary)
            ) {
                Text("⚙️  Change Teams / Setup", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Toss Dialog Overlay ─────────────────────────────────────────────
    if (showTossDialog) {
        TossDialog(
            teamAName = summary.teamA.name,
            teamBName = summary.teamB.name,
            onConfirm = { winnerId, decision ->
                showTossDialog = false
                onRematch(winnerId, decision)
            },
            onDismiss = { showTossDialog = false }
        )
    }
}

@Composable
fun TossDialog(
    teamAName: String,
    teamBName: String,
    onConfirm: (tossWinnerTeamId: Int, tossDecision: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tossWinner by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1) }
    var tossDecision by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("BAT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text("🪙 New Toss", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Text("Who won the toss?", color = TextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tossWinner == 1,
                            onClick = { tossWinner = 1 },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                        )
                        Text(teamAName, color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tossWinner == 2,
                            onClick = { tossWinner = 2 },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                        )
                        Text(teamBName, color = TextPrimary, fontSize = 14.sp)
                    }
                }
                Divider(color = TextSecondary.copy(alpha = 0.15f))
                Text("Decision", color = TextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tossDecision == "BAT",
                            onClick = { tossDecision = "BAT" },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                        )
                        Text("Bat", color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tossDecision == "BOWL",
                            onClick = { tossDecision = "BOWL" },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                        )
                        Text("Bowl", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tossWinner, tossDecision) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("▶  Start Match!", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ── Innings Break ─────────────────────────────────────────────────────────────

@Composable
fun InningsBreakScreen(
    firstInningsScore: Int,
    firstInningsWickets: Int,
    firstInningsOvers: String,
    battingTeamName: String,
    onStartSecondInnings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🏏", fontSize = 48.sp)
            Text("Innings Break", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Divider(color = TextSecondary.copy(alpha = 0.2f))
            Text("First Innings Complete", color = TextSecondary, fontSize = 14.sp)
            Text(
                "$firstInningsScore/$firstInningsWickets",
                color = AccentGreen,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black
            )
            Text("in $firstInningsOvers overs", color = TextSecondary, fontSize = 14.sp)
            Divider(color = TextSecondary.copy(alpha = 0.2f))
            Text(
                "$battingTeamName needs ${firstInningsScore + 1} to win",
                color = AccentOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onStartSecondInnings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Start 2nd Innings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        content()
    }
}

@Composable
private fun ScoreCompareRow(
    teamName: String,
    score: Int,
    wickets: Int,
    overs: String,
    isWinner: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWinner) Text("🏆 ", fontSize = 14.sp)
                Text(
                    teamName,
                    color = if (isWinner) AccentGreen else TextPrimary,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
            Text("$overs overs", color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            "$score/$wickets",
            color = if (isWinner) AccentGreen else TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun StatGridRow(items: List<Pair<String, String>>) {
    for (row in items.chunked(3)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(value, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(label, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun BallTimeline(balls: List<BallEvent>) {
    val grouped = balls.groupBy { it.over }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (over, overBalls) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Ov ${over + 1}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.width(36.dp)
                )
                overBalls.forEach { ball ->
                    val (bg, label) = when {
                        ball.isWicket -> AccentRed to "W"
                        ball.isBoundary && ball.runs == 6 -> AccentPurple to "6"
                        ball.isBoundary && ball.runs == 4 -> AccentBlue to "4"
                        ball.isWide -> AccentOrange to "Wd"
                        ball.isNoBall -> AccentOrange to "Nb"
                        else -> CardBg to "${ball.runs}"
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
