package com.boxai.scorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import com.boxai.scorer.ui.*
import kotlinx.coroutines.flow.collectLatest
import android.graphics.Bitmap

class MainActivity : ComponentActivity() {

    private val viewModel: ScorerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handle result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(background = Color(0xFF0D1117))
            ) {
                Surface {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: ScorerViewModel) {
    var screen by remember { mutableStateOf("setup") }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collectLatest { event ->
            screen = event
        }
    }

    val matchSetup by viewModel.matchSetup.collectAsState()
    val matchState by viewModel.matchState.collectAsState()
    val pendingSuggestion by viewModel.pendingSuggestion.collectAsState()
    val ballHistory by viewModel.ballHistory.collectAsState()
    val matchSummary by viewModel.matchSummary.collectAsState()
    val isConnected by viewModel.connectionStatus.collectAsState()

    val registeredFaces = remember { mutableStateMapOf<Pair<Int, Int>, Bitmap>() }
    var registeringPlayer by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            "setup" -> MatchSetupScreen(
                serverIp = matchSetup.serverIp,
                onIpChange = { viewModel.updateSetup(matchSetup.copy(serverIp = it)) },
                onStartMatch = { setup ->
                    viewModel.updateSetup(setup)
                    viewModel.startMatch(registeredFaces)
                },
                onJoinMatch = { matchId, role, ip ->
                    viewModel.joinMatch(matchId = matchId, role = role, serverIp = ip)
                },
                onRegisterFace = { tIdx, pIdx, name ->
                    registeringPlayer = Triple(tIdx, pIdx, name)
                },
                registeredFaces = registeredFaces.mapValues { true },
                initialSetup = matchSetup   // pre-fills team names & player list from last match
            )
            "scoreboard" -> ScoreboardScreen(
                matchState = matchState,
                pendingSuggestion = pendingSuggestion,
                ballHistory = ballHistory,
                isConnected = isConnected,
                isSpectator = matchSetup.cameraRole == com.boxai.scorer.data.CameraRole.SPECTATOR,
                onAddRuns = { viewModel.addRuns(it) },
                onWicket = { viewModel.addWicket() },
                onExtra = { viewModel.addExtra(it) },
                onConfirmAi = { viewModel.confirmAiSuggestion() },
                onRejectAi = { viewModel.rejectAiSuggestion() },
                onFourClicked = { viewModel.addRuns(4, isBoundary = true) },
                onSixClicked = { viewModel.addRuns(6, isBoundary = true) },
                onFaceDetected = { bitmap -> viewModel.recognizeBatter(bitmap) },
                onEndInnings = { viewModel.endCurrentPhase() }
            )
            "innings_break" -> InningsBreakScreen(
                firstInningsScore = matchState.totalRuns,
                firstInningsWickets = matchState.wickets,
                firstInningsOvers = "${matchState.overs}.${matchState.balls}",
                battingTeamName = matchSetup.teamB.name,
                onStartSecondInnings = {
                    viewModel.startSecondInnings()
                    screen = "scoreboard"
                }
            )
            "summary" -> matchSummary?.let { summary ->
                SummaryScreen(
                    summary = summary,
                    onRematch = { tossWinnerId, tossDecision ->
                        // Apply new toss then restart with same teams & overs
                        viewModel.rematch(
                            tossWinnerTeamId = tossWinnerId,
                            tossDecision = tossDecision,
                            registeredFaces = registeredFaces
                        )
                    },
                    onChangeTeams = {
                        viewModel.clearSummary()
                        screen = "setup"
                    }
                )
            }
        }

        if (registeringPlayer != null) {
            val (teamId, playerIdx, pName) = registeringPlayer!!
            PlayerRegistrationScreen(
                playerName = pName,
                onFaceCaptured = { bitmap ->
                    registeredFaces[teamId to playerIdx] = bitmap
                    registeringPlayer = null
                },
                onCancel = { registeringPlayer = null }
            )
        }
    }
}
