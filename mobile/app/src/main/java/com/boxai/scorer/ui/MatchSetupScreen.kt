package com.boxai.scorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxai.scorer.data.CameraRole
import com.boxai.scorer.data.MatchSetup
import com.boxai.scorer.data.Player
import com.boxai.scorer.data.Team

@Composable
fun MatchSetupScreen(
    serverIp: String,
    onIpChange: (String) -> Unit,
    onStartMatch: (MatchSetup) -> Unit,
    onJoinMatch: (matchId: Int, role: CameraRole, serverIp: String) -> Unit,
    onRegisterFace: (teamIndex: Int, playerIndex: Int, playerName: String) -> Unit,
    registeredFaces: Map<Pair<Int, Int>, Boolean>,
    initialSetup: com.boxai.scorer.data.MatchSetup = com.boxai.scorer.data.MatchSetup()
) {
    // ── Tab state ─────────────────────────────────────────────────────────
    var activeTab by remember { mutableStateOf(0) } // 0 = Start, 1 = Join

    // ── Start-match form state (pre-filled from last match if available) ──────
    var teamAName by remember { mutableStateOf(initialSetup.teamA.name.ifBlank { "Team A" }) }
    var teamBName by remember { mutableStateOf(initialSetup.teamB.name.ifBlank { "Team B" }) }
    var totalOversStr by remember { mutableStateOf(if (initialSetup.totalOvers > 0) initialSetup.totalOvers.toString() else "6") }
    var selectedCamera by remember { mutableStateOf(CameraRole.DEVICE_1) }
    var tossWinnerTeamId by remember { mutableStateOf(1) }
    var tossDecision by remember { mutableStateOf("BAT") }
    var showErrors by remember { mutableStateOf(false) }

    // Dynamic player lists — pre-filled from last match if available
    val teamAPlayers = remember {
        val saved = initialSetup.teamA.players.map { it.name }
        mutableStateListOf<String>().also { list ->
            list.addAll(if (saved.isEmpty()) listOf("", "") else saved)
        }
    }
    val teamBPlayers = remember {
        val saved = initialSetup.teamB.players.map { it.name }
        mutableStateListOf<String>().also { list ->
            list.addAll(if (saved.isEmpty()) listOf("", "") else saved)
        }
    }

    // ── Join-match form state ──────────────────────────────────────────────
    var joinMatchIdStr by remember { mutableStateOf("") }
    var joinCamera by remember { mutableStateOf(CameraRole.DEVICE_2) }
    var showJoinError by remember { mutableStateOf(false) }

    val isValid = teamAName.isNotBlank() &&
            teamBName.isNotBlank() &&
            (totalOversStr.toIntOrNull() ?: 0) in 1..20

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
            Text(
                "🏏 Match Setup",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )

            // ── Tab Switcher ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("🆕  New Match", "🔗  Join Match").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeTab == idx) AccentGreen else Color.Transparent)
                            .clickable { activeTab = idx }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (activeTab == idx) Color.Black else TextSecondary,
                            fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── Server IP (shared) ─────────────────────────────────────────
            SetupCard(title = "Backend Server") {
                SetupTextField(
                    label = "Server IP",
                    value = serverIp,
                    onValueChange = onIpChange,
                    placeholder = "192.168.0.125",
                    keyboardType = KeyboardType.Decimal
                )
            }

            if (activeTab == 0) {
                // ══════════════════════════════════════════════════════════
                // NEW MATCH TAB
                // ══════════════════════════════════════════════════════════

                // ── Match Config ───────────────────────────────────────────
                SetupCard(title = "Match Config") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SetupTextField(
                            label = "Team A Name",
                            value = teamAName,
                            onValueChange = { teamAName = it },
                            placeholder = "Team A",
                            isError = showErrors && teamAName.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        SetupTextField(
                            label = "Team B Name",
                            value = teamBName,
                            onValueChange = { teamBName = it },
                            placeholder = "Team B",
                            isError = showErrors && teamBName.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SetupTextField(
                        label = "Total Overs",
                        value = totalOversStr,
                        onValueChange = { totalOversStr = it.filter(Char::isDigit).take(2) },
                        placeholder = "6",
                        keyboardType = KeyboardType.Number,
                        isError = showErrors && (totalOversStr.toIntOrNull() ?: 0) !in 1..20
                    )
                }

                // ── Team A Players ─────────────────────────────────────────
                SetupCard(title = "Team A Players  (${teamAPlayers.count { it.isNotBlank() }})") {
                    teamAPlayers.forEachIndexed { i, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SetupTextField(
                                label = "Player ${i + 1}",
                                value = name,
                                onValueChange = { teamAPlayers[i] = it },
                                placeholder = "Player ${i + 1} name",
                                modifier = Modifier.weight(1f)
                            )
                            if (name.isNotBlank()) {
                                IconButton(onClick = { onRegisterFace(1, i, name) }) {
                                    Text(if (registeredFaces[1 to i] == true) "✅" else "📷")
                                }
                            }
                            // Remove button (only if more than 1 player)
                            if (teamAPlayers.size > 1) {
                                IconButton(onClick = { teamAPlayers.removeAt(i) }) {
                                    Text("✕", color = AccentRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    // Add player button
                    TextButton(
                        onClick = { teamAPlayers.add("") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Player", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Team B Players ─────────────────────────────────────────
                SetupCard(title = "Team B Players  (${teamBPlayers.count { it.isNotBlank() }})") {
                    teamBPlayers.forEachIndexed { i, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SetupTextField(
                                label = "Player ${i + 1}",
                                value = name,
                                onValueChange = { teamBPlayers[i] = it },
                                placeholder = "Player ${i + 1} name",
                                modifier = Modifier.weight(1f)
                            )
                            if (name.isNotBlank()) {
                                IconButton(onClick = { onRegisterFace(2, i, name) }) {
                                    Text(if (registeredFaces[2 to i] == true) "✅" else "📷")
                                }
                            }
                            if (teamBPlayers.size > 1) {
                                IconButton(onClick = { teamBPlayers.removeAt(i) }) {
                                    Text("✕", color = AccentRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { teamBPlayers.add("") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Player", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                    }
                }


                // ── Toss ───────────────────────────────────────────────────
                SetupCard(title = "Toss Result") {
                    Text("Who won the toss?", color = TextPrimary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tossWinnerTeamId == 1,
                                onClick = { tossWinnerTeamId = 1 },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                            )
                            Text(if (teamAName.isNotBlank()) teamAName else "Team A", color = TextPrimary, fontSize = 14.sp,
                                modifier = Modifier.clickable { tossWinnerTeamId = 1 })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tossWinnerTeamId == 2,
                                onClick = { tossWinnerTeamId = 2 },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                            )
                            Text(if (teamBName.isNotBlank()) teamBName else "Team B", color = TextPrimary, fontSize = 14.sp,
                                modifier = Modifier.clickable { tossWinnerTeamId = 2 })
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Decision", color = TextPrimary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tossDecision == "BAT",
                                onClick = { tossDecision = "BAT" },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                            )
                            Text("Bat", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.clickable { tossDecision = "BAT" })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tossDecision == "BOWL",
                                onClick = { tossDecision = "BOWL" },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
                            )
                            Text("Bowl", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.clickable { tossDecision = "BOWL" })
                        }
                    }
                }

                // ── Start Button ───────────────────────────────────────────
                Button(
                    onClick = {
                        if (!isValid) { showErrors = true; return@Button }
                        val toPlayers: (List<String>, Int) -> List<Player> = { names, teamId ->
                            names.filter(String::isNotBlank).mapIndexed { i, n ->
                                Player(id = teamId * 100 + i, name = n)
                            }
                        }
                        onStartMatch(
                            MatchSetup(
                                teamA = Team(1, teamAName, toPlayers(teamAPlayers, 1)),
                                teamB = Team(2, teamBName, toPlayers(teamBPlayers, 2)),
                                totalOvers = totalOversStr.toIntOrNull() ?: 6,
                                cameraRole = CameraRole.DEVICE_1,
                                serverIp = serverIp,
                                matchId = 1,
                                tossWinnerTeamId = tossWinnerTeamId,
                                tossDecision = tossDecision
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("▶  Start Match", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }

            } else {
                // ══════════════════════════════════════════════════════════
                // JOIN MATCH TAB
                // ══════════════════════════════════════════════════════════

                SetupCard(title = "Join Existing Match") {
                    Text(
                        "Enter the Match ID shown on the primary device after it starts the match.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    SetupTextField(
                        label = "Match ID",
                        value = joinMatchIdStr,
                        onValueChange = { joinMatchIdStr = it.filter(Char::isDigit).take(6) },
                        placeholder = "e.g. 3",
                        keyboardType = KeyboardType.Number,
                        isError = showJoinError && (joinMatchIdStr.toIntOrNull() ?: 0) <= 0
                    )
                    if (showJoinError && (joinMatchIdStr.toIntOrNull() ?: 0) <= 0) {
                        Text("Please enter a valid Match ID", color = AccentRed, fontSize = 11.sp)
                    }
                }

                SetupCard(title = "This Device's Role") {
                    CameraRole.entries.forEach { role ->
                        CameraRoleOption(
                            role = role,
                            selected = joinCamera == role,
                            onSelect = { joinCamera = role }
                        )
                    }
                }

                Button(
                    onClick = {
                        val id = joinMatchIdStr.toIntOrNull() ?: 0
                        if (id <= 0) { showJoinError = true; return@Button }
                        onJoinMatch(id, joinCamera, serverIp)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("🔗  Join Match", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
fun SetupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        content()
    }
}

@Composable
fun SetupTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (isError) AccentRed else TextSecondary, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGreen,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
            errorBorderColor = AccentRed,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentGreen
        ),
        modifier = modifier
    )
}

@Composable
fun CameraRoleOption(role: CameraRole, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentGreen.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) AccentGreen else TextSecondary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                role.label,
                color = if (selected) AccentGreen else TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Text(
                when (role) {
                    CameraRole.DEVICE_1 -> "Primary — striker/bowler angle"
                    CameraRole.DEVICE_2 -> "Secondary — side angle for run confirmation"
                    CameraRole.SPECTATOR -> "No camera tracking — score & AI approval only"
                },
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextSecondary)
        )
    }
}
