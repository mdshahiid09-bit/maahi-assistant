package com.maahiassistant.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.data.local.LiveLogEntry
import com.maahiassistant.android.data.local.LiveLogManager
import com.maahiassistant.android.data.local.LogLevel
import com.maahiassistant.android.ui.theme.ClayBackground
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClaySurfaceCard
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbCrimson
import com.maahiassistant.android.ui.theme.OrbGold
import com.maahiassistant.android.ui.theme.StatusGreen

@Composable
fun LiveLogsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLogs by LiveLogManager.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(allLogs, selectedFilter) {
        when (selectedFilter) {
            "ERRORS" -> allLogs.filter { it.level == LogLevel.ERROR || it.level == LogLevel.WARN }
            "WEBSOCKET" -> allLogs.filter { it.tag.contains("WebSocket", ignoreCase = true) || it.tag.contains("Setup", ignoreCase = true) }
            "AUDIO" -> allLogs.filter { it.tag.contains("Audio", ignoreCase = true) || it.tag.contains("Mic", ignoreCase = true) }
            "GEMINI" -> allLogs.filter { it.tag.contains("Gemini", ignoreCase = true) || it.tag.contains("Turn", ignoreCase = true) }
            else -> allLogs
        }
    }

    // Auto-scroll on new log entry
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClayBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ClaySurface)
                        .border(1.dp, ClayBorder, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = ClayTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Live Diagnostics & Logs",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayTextPrimary
                    )
                    Text(
                        text = "${filteredLogs.size} events logged",
                        fontSize = 11.5.sp,
                        color = ClayTextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Copy Button
                OutlinedButton(
                    onClick = {
                        val text = LiveLogManager.getAllLogsAsFormattedText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Maahi Live Logs", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "All logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClayBlueAccent),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = "Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Clear Button
                ElevatedButton(
                    onClick = { LiveLogManager.clear() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = ClaySurfaceCard,
                        contentColor = OrbCrimson
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = "Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        val filters = listOf("ALL", "ERRORS", "WEBSOCKET", "AUDIO", "GEMINI")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ClayBlueAccent else ClaySurface)
                        .border(1.dp, if (isSelected) ClayBlueAccent else ClayBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else ClayTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Output List
        if (filteredLogs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_terminal),
                        contentDescription = null,
                        tint = ClayBorder,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No logs recorded yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start a voice session or test your key in Settings to see real-time WebSocket, network, audio, and error frames.",
                        fontSize = 12.5.sp,
                        color = ClayTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        LogEntryCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(log: LiveLogEntry) {
    val (badgeColor, textColor) = when (log.level) {
        LogLevel.ERROR -> Color(0xFFFDE8E8) to OrbCrimson
        LogLevel.WARN -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        LogLevel.SUCCESS -> Color(0xFFDEF7EC) to StatusGreen
        LogLevel.FRAME -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        LogLevel.INFO -> ClayBlueContainer to ClayBlueAccent
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ClaySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClayBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.level.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tag
                    Text(
                        text = "[${log.tag}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayTextSecondary
                    )
                }

                // Timestamp
                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClayTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Log Message
            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (log.level == LogLevel.ERROR) OrbCrimson else ClayTextPrimary,
                lineHeight = 16.sp
            )
        }
    }
}
