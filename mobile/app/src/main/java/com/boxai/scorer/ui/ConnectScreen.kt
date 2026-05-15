package com.boxai.scorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectScreen(
    serverIp: String,
    onIpChange: (String) -> Unit,
    onConnect: (String) -> Unit
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🏏", fontSize = 48.sp)
            Text(
                "Box Cricket AI Scorer",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter the IP address of the PC running the FastAPI backend",
                color = TextSecondary,
                fontSize = 13.sp
            )
            OutlinedTextField(
                value = serverIp,
                onValueChange = onIpChange,
                label = { Text("Server IP", color = TextSecondary) },
                placeholder = { Text("e.g. 192.168.0.125", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConnect(serverIp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Connect to Backend", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = { onConnect("") }) {
                Text("Continue Offline (Manual Scoring)", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
