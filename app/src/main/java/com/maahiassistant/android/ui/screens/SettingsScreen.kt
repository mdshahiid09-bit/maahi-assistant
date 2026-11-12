package com.maahiassistant.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.ui.KeyTestState
import com.maahiassistant.android.ui.MainViewModel
import com.maahiassistant.android.ui.components.ConfirmDialog
import com.maahiassistant.android.ui.components.DrawerDestination
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClaySurfaceCard
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbCrimson
import com.maahiassistant.android.ui.theme.StatusGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val modelId by viewModel.modelId.collectAsState()
    val voice by viewModel.voice.collectAsState()
    val useSpeaker by viewModel.useSpeakerPreference.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val keyTestState by viewModel.keyTestState.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showHowToGuide by remember { mutableStateOf(false) }

    var modelIdInput by remember { mutableStateOf(modelId) }
    var voiceExpanded by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val storedKey = viewModel.getStoredApiKey()
        if (!storedKey.isNullOrEmpty()) {
            apiKeyInput = storedKey
        }
        modelIdInput = modelId
    }

    LaunchedEffect(modelId) {
        modelIdInput = modelId
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset Settings",
            message = "Restore model, voice, and audio preferences to factory defaults? Your chat history will be kept.",
            confirmText = "Reset",
            onConfirm = { viewModel.resetSettings() },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showDeleteAllDialog) {
        ConfirmDialog(
            title = "Delete All Data",
            message = "This will erase your Gemini API key, reset all preferences, and permanently delete all conversation history.",
            confirmText = "Delete Everything",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAllData()
                apiKeyInput = ""
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Column {
            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ClayTextPrimary
            )
            Text(
                text = "Configure API connection, voice synthesis, and storage",
                fontSize = 13.sp,
                color = ClayTextSecondary
            )
        }

        // ==========================================
        // CARD 1: Gemini API Key & Guide
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ClayBlueContainer)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_key),
                            contentDescription = "API Key",
                            tint = ClayBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gemini API Key",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ClayTextPrimary
                        )
                        Text(
                            text = "Stored securely with Android Keystore encryption",
                            fontSize = 11.5.sp,
                            color = ClayTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key (starts with AIza...)") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                painter = painterResource(id = if (isKeyVisible) R.drawable.ic_close else R.drawable.ic_info),
                                contentDescription = "Toggle Visibility",
                                tint = ClayTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClayBlueAccent,
                        unfocusedBorderColor = ClayBorder
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.testAndSaveApiKey(apiKeyInput) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test / Save Button
                Button(
                    onClick = { viewModel.testAndSaveApiKey(apiKeyInput) },
                    enabled = keyTestState !is KeyTestState.Testing && apiKeyInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClayBlueAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (keyTestState is KeyTestState.Testing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Testing Key against Google AI Studio...", fontSize = 13.5.sp)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Save & Test Key", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Test Result Feedback
                when (keyTestState) {
                    is KeyTestState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = "Success",
                                tint = StatusGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (keyTestState as KeyTestState.Success).message,
                                fontSize = 12.5.sp,
                                color = StatusGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is KeyTestState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Error",
                                tint = OrbCrimson,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (keyTestState as KeyTestState.Error).error,
                                fontSize = 12.5.sp,
                                color = OrbCrimson,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Guide toggler and AI Studio launch button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showHowToGuide) "Hide Guide" else "How to get your key?",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayBlueAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showHowToGuide = !showHowToGuide }
                            .padding(4.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://aistudio.google.com/app/apikey")
                            )
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_browser),
                            contentDescription = "Open Browser",
                            tint = ClayBlueAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open AI Studio", fontSize = 11.5.sp, color = ClayBlueAccent)
                    }
                }

                // Collapsible English Guide
                AnimatedVisibility(visible = showHowToGuide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ClaySurfaceCard)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Steps to obtain a free Gemini API key:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ClayTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Tap 'Open AI Studio' above or go to aistudio.google.com\n" +
                                    "2. Sign in with your Google account.\n" +
                                    "3. Click 'Get API key' in the left menu.\n" +
                                    "4. Click 'Create API key' and copy the generated key.\n" +
                                    "5. Paste it in the field above and tap 'Save & Test Key'.",
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                            color = ClayTextSecondary
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 2: Live Model ID
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Live Voice Model",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "Gemini Live audio-to-audio model (default gemini-3.8-live)",
                    fontSize = 11.5.sp,
                    color = ClayTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = modelIdInput,
                        onValueChange = {
                            modelIdInput = it
                            viewModel.updateModelId(it)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClayBlueAccent,
                            unfocusedBorderColor = ClayBorder
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val quickModels = listOf("gemini-3.8-live", "gemini-3.8-live-extended-thinking", "gemini-2.0-flash-exp")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickModels.forEach { modelOption ->
                        val isSelected = modelIdInput.trim() == modelOption
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ClayBlueContainer else ClayBackground)
                                .border(1.dp, if (isSelected) ClayBlueAccent else ClayBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    modelIdInput = modelOption
                                    viewModel.updateModelId(modelOption)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = modelOption,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ClayBlueAccent else ClayTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // CARD 3: Voice Selection
        // ==========================================
        val voiceOptions = listOf("Kore", "Puck", "Charon", "Fenrir", "Aoede")
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Voice Persona",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "Select neural prebuilt voice for Maahi's spoken output",
                    fontSize = 11.5.sp,
                    color = ClayTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = voiceExpanded,
                    onExpandedChange = { voiceExpanded = !voiceExpanded }
                ) {
                    OutlinedTextField(
                        value = voice,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClayBlueAccent,
                            unfocusedBorderColor = ClayBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = voiceExpanded,
                        onDismissRequest = { voiceExpanded = false },
                        containerColor = ClaySurface
                    ) {
                        voiceOptions.forEach { voiceItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (voiceItem == "Kore") "$voiceItem (Default)" else voiceItem,
                                        fontWeight = if (voiceItem == voice) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.updateVoice(voiceItem)
                                    voiceExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // CARD 4: Speaker / Earpiece Output
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Speakerphone Output",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayTextPrimary
                    )
                    Text(
                        text = if (useSpeaker) "Audio plays via loud speaker" else "Audio routes to phone earpiece",
                        fontSize = 12.sp,
                        color = ClayTextSecondary
                    )
                }
                Switch(
                    checked = useSpeaker,
                    onCheckedChange = { viewModel.updateUseSpeaker(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClayBlueAccent
                    )
                )
            }
        }

        // ==========================================
        // CARD 5: Keep Screen On
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keep Screen Awake",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayTextPrimary
                    )
                    Text(
                        text = "Prevent screen from sleeping during active voice conversation",
                        fontSize = 12.sp,
                        color = ClayTextSecondary
                    )
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClayBlueAccent
                    )
                )
            }
        }

        // ==========================================
        // CARD 6: Live Diagnostics & Logs
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Live Diagnostics & Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "View real-time WebSocket connection frames, errors, microphone events, and audio streaming status",
                    fontSize = 12.sp,
                    color = ClayTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.selectDrawerDestination(DrawerDestination.LIVE_LOGS) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClayBlueAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_terminal),
                        contentDescription = "Logs",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "View Live Logs", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ==========================================
        // CARD 7: Reset Settings & Delete All Data
        // ==========================================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ClaySurface),
            border = BorderStroke(1.dp, ClayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Data Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "Manage application preferences and local chat history",
                    fontSize = 12.sp,
                    color = ClayTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Reset Settings",
                        tint = ClayBlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Reset Settings (keeps chat)", color = ClayBlueAccent)
                }

                Button(
                    onClick = { showDeleteAllDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrbCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Delete All Data",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete All Data", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
