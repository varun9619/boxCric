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
    onStartMatch: (MatchSetup) -> Unit
) {
    // ── Local form state ──────────────────────────────────────────────────────
    var teamAName by remember { mutableStateOf("Team A") }
    var teamBName by remember { mutableStateOf("Team B") }
    var totalOversStr by remember { mutableStateOf("6") }
    var selectedCamera by remember { mutableStateOf(CameraRole.PHONE_1) }

    // Player name rows (up to 6 per team)
    val teamAPlayers = remember { mutableStateListOf("", "", "", "", "", "") }
    val teamBPlayers = remember { mutableStateListOf("", "", "", "", "", "") }

    var showErrors by remember { mutableStateOf(false) }

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
            // Header
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "🏏 Match Setup",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Text("Configure teams and camera before starting", color = TextSecondary, fontSize = 13.sp)

            // ── Server IP ─────────────────────────────────────────────────────
            SetupCard(title = "Backend Server") {
                SetupTextField(
                    label = "Server IP",
                    value = serverIp,
                    onValueChange = onIpChange,
                    placeholder = "192.168.1.100",
                    keyboardType = KeyboardType.Decimal
                )
            }

            // ── Match Config ──────────────────────────────────────────────────
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

            // ── Team A Players ────────────────────────────────────────────────
            SetupCard(title = "Team A Players") {
                teamAPlayers.forEachIndexed { i, name ->
                    SetupTextField(
                        label = "Player ${i + 1}",
                        value = name,
                        onValueChange = { teamAPlayers[i] = it },
                        placeholder = "Player ${i + 1} name (optional)"
                    )
                }
            }

            // ── Team B Players ────────────────────────────────────────────────
            SetupCard(title = "Team B Players") {
                teamBPlayers.forEachIndexed { i, name ->
                    SetupTextField(
                        label = "Player ${i + 1}",
                        value = name,
                        onValueChange = { teamBPlayers[i] = it },
                        placeholder = "Player ${i + 1} name (optional)"
                    )
                }
            }

            // ── Camera Role ───────────────────────────────────────────────────
            SetupCard(title = "This Phone's Camera Role") {
                CameraRole.entries.forEach { role ->
                    CameraRoleOption(
                        role = role,
                        selected = selectedCamera == role,
                        onSelect = { selectedCamera = role }
                    )
                }
            }

            // ── Start Button ──────────────────────────────────────────────────
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
                            cameraRole = selectedCamera,
                            serverIp = serverIp,
                            matchId = 1
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(
                    "▶  Start Match",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
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
                if (role == CameraRole.PHONE_1) "Primary — striker/bowler angle"
                else "Secondary — side angle for run confirmation",
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
