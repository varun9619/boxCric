package com.boxai.scorer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxai.scorer.camera.CameraPreview

@Composable
fun PlayerRegistrationScreen(
    playerName: String,
    onFaceCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var detectedFace by remember { mutableStateOf<Bitmap?>(null) }
    var latestFace by remember { mutableStateOf<Bitmap?>(null) }
    var isCameraActive by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📷 Register Face",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = AccentRed)
                }
            }

            Text(
                "Detecting face for: $playerName",
                color = AccentGreen,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Camera View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                CameraPreview(
                    active = isCameraActive,
                    onFaceDetected = { bitmap ->
                        latestFace = bitmap
                    }
                )

                // Manual Capture Button
                if (isCameraActive) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Button(
                            onClick = {
                                if (latestFace != null) {
                                    detectedFace = latestFace
                                    isCameraActive = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (latestFace != null) AccentGreen else Color.Gray
                            ),
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                if (latestFace != null) "📸 Capture" else "Looking for face...",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Overlay the detected face if available
                detectedFace?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Detected Face",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        detectedFace = null
                                        latestFace = null
                                        isCameraActive = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text("Retake")
                                }
                                Button(
                                    onClick = { onFaceCaptured(bitmap) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                                ) {
                                    Text("Confirm", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
            
            Text(
                "Please position the player's face in the camera frame.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
