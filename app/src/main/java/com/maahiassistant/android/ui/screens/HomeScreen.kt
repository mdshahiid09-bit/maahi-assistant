package com.maahiassistant.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.data.api.models.ConnectionStatus
import com.maahiassistant.android.data.api.models.SessionOrbState
import com.maahiassistant.android.ui.MainViewModel
import com.maahiassistant.android.ui.components.BottomTab
import com.maahiassistant.android.ui.components.NeuralOrb
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClayOnBlueContainer
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClaySurfaceCard
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbCrimson
import com.maahiassistant.android.ui.theme.OrbGold
import com.maahiassistant.android.ui.theme.OrbGoldBright

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val inputLevel by viewModel.audioInputLevel.collectAsState()
    val outputLevel by viewModel.audioOutputLevel.collectAsState()
    val errorMessage by viewModel.serviceErrorMessage.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Missing API Key Banner
            AnimatedVisibility(
                visible = !hasApiKey,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ClaySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClayBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ClayBlueContainer)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_key),
                                contentDescription = "Key required",
                                tint = ClayBlueAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add your Gemini API key",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ClayTextPrimary
                            )
                            Text(
                                text = "Required to connect to live voice session",
                                fontSize = 12.sp,
                                color = ClayTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClayBlueAccent)
                        ) {
                            Text(text = "Settings", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Mic Permission Denied Banner
            AnimatedVisibility(
                visible = !hasMicPermission,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF8B4B4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info),
                                contentDescription = "Permission needed",
                                tint = OrbCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Microphone access required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrbCrimson
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Maahi streams live audio to converse in real time. Please grant permission in settings.",
                            fontSize = 12.sp,
                            color = ClayTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onRequestMicPermission,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "Grant Mic", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "Open App Settings", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Neural Orb
            NeuralOrb(
                state = orbState,
                audioInputLevel = inputLevel,
                audioOutputLevel = outputLevel,
                onClick = {
                    if (!hasMicPermission) {
                        onRequestMicPermission()
                    } else {
                        viewModel.toggleSession(
                            context = context,
                            onMissingKey = onNavigateToSettings,
                            onMicPermissionNeeded = onRequestMicPermission
                        )
                    }
                },
                size = 250.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Status Line under Orb
            val displayStatus = when {
                errorMessage != null -> errorMessage!!
                connectionStatus == ConnectionStatus.DISCONNECTED -> "Tap orb to start"
                connectionStatus == ConnectionStatus.CONNECTING -> "Connecting…"
                connectionStatus == ConnectionStatus.LISTENING -> "Listening"
                connectionStatus == ConnectionStatus.SPEAKING -> "Speaking"
                connectionStatus == ConnectionStatus.MUTED -> "Muted"
                connectionStatus == ConnectionStatus.RECONNECTING -> "Reconnecting…"
                connectionStatus == ConnectionStatus.NO_NETWORK -> "No connection, retrying…"
                else -> connectionStatus.displayText
            }

            val statusColor = when (orbState) {
                SessionOrbState.IDLE -> ClayTextSecondary
                SessionOrbState.CONNECTING -> OrbGold
                SessionOrbState.LISTENING -> ClayBlueAccent
                SessionOrbState.SPEAKING -> OrbGoldBright
                SessionOrbState.MUTED -> ClayTextSecondary
                SessionOrbState.ERROR -> OrbCrimson
            }

            Text(
                text = displayStatus,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Session controls (Mute and Speaker/Earpiece)
            AnimatedVisibility(
                visible = isSessionActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleMute(context) },
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isMuted) Color(0xFFFDE8E8) else ClaySurface,
                            contentColor = if (isMuted) OrbCrimson else ClayTextPrimary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Speaker / Earpiece Toggle
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleSpeaker(context) },
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = ClaySurface,
                            contentColor = ClayBlueAccent
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_earpiece),
                            contentDescription = if (isSpeakerOn) "Switch to Earpiece" else "Switch to Speaker",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
