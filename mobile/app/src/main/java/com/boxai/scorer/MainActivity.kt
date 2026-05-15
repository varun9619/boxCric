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
import androidx.core.content.ContextCompat
import com.boxai.scorer.ui.*
import kotlinx.coroutines.flow.collectLatest

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

    when (screen) {
        "setup" -> MatchSetupScreen(
            serverIp = matchSetup.serverIp,
            onIpChange = { viewModel.updateSetup(matchSetup.copy(serverIp = it)) },
            onStartMatch = { setup ->
                viewModel.updateSetup(setup)
                viewModel.startMatch()
            }
        )
        "scoreboard" -> ScoreboardScreen(
            matchState = matchState,
            pendingSuggestion = pendingSuggestion,
            ballHistory = ballHistory,
            isConnected = isConnected,
            onAddRuns = { viewModel.addRuns(it) },
            onWicket = { viewModel.addWicket() },
            onExtra = { viewModel.addExtra(it) },
            onConfirmAi = { viewModel.confirmAiSuggestion() },
            onRejectAi = { viewModel.rejectAiSuggestion() },
            onFourClicked = { viewModel.addRuns(4, isBoundary = true) },
            onSixClicked = { viewModel.addRuns(6, isBoundary = true) },
            onEndInnings = {
                if (matchState.inning == 1) {
                    screen = "innings_break"
                } else {
                    screen = "summary"
                }
            }
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
                onNewMatch = {
                    screen = "setup"
                }
            )
        }
    }
}
