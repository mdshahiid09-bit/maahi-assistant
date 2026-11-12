package com.maahiassistant.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.data.db.ChatMessageEntity
import com.maahiassistant.android.ui.MainViewModel
import com.maahiassistant.android.ui.components.ConfirmDialog
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClayOnBlueContainer
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClaySurfaceCard
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbGold

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val liveUserText by viewModel.currentUserText.collectAsState()
    val liveAssistantText by viewModel.currentAssistantText.collectAsState()
    val isToolSearching by viewModel.isToolSearching.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, liveUserText, liveAssistantText) {
        if (messages.isNotEmpty() || liveUserText.isNotEmpty() || liveAssistantText.isNotEmpty()) {
            val target = messages.size +
                    (if (liveUserText.isNotEmpty()) 1 else 0) +
                    (if (liveAssistantText.isNotEmpty()) 1 else 0)
            if (target > 0) {
                listState.animateScrollToItem(target - 1)
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear Chat History",
            message = "Are you sure you want to delete all messages? This action cannot be undone.",
            confirmText = "Clear",
            isDestructive = true,
            onConfirm = { viewModel.clearChat() },
            onDismiss = { showClearDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Chat Header with Clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Live Conversation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "Transcribed live from voice and typed inputs",
                    fontSize = 12.sp,
                    color = ClayTextSecondary
                )
            }

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = ClayTextSecondary)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Clear Chat",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && liveAssistantText.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet.\nStart speaking with Maahi or type below!",
                            fontSize = 14.sp,
                            color = ClayTextSecondary,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.isSystemNote) {
                    SystemNoteChip(note = msg.text)
                } else {
                    ChatBubble(
                        role = msg.role,
                        text = msg.text,
                        isSearching = msg.isSearching
                    )
                }
            }

            // Live User Stream Bubble
            if (liveUserText.isNotEmpty()) {
                item {
                    ChatBubble(
                        role = "user",
                        text = liveUserText,
                        isSearching = false,
                        isStreaming = true
                    )
                }
            }

            // Live Assistant Stream Bubble
            if (liveAssistantText.isNotEmpty() || isToolSearching) {
                item {
                    ChatBubble(
                        role = "maahi",
                        text = liveAssistantText,
                        isSearching = isToolSearching,
                        isStreaming = true
                    )
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClaySurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(text = "Message Maahi...", fontSize = 14.sp, color = ClayTextSecondary)
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClayBlueAccent,
                    unfocusedBorderColor = ClayBorder,
                    focusedContainerColor = ClaySurfaceCard,
                    unfocusedContainerColor = ClaySurfaceCard,
                    focusedTextColor = ClayTextPrimary,
                    unfocusedTextColor = ClayTextPrimary
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendTextMessage(inputText)
                            inputText = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = ClayBlueAccent,
                    contentColor = Color.White,
                    disabledContainerColor = ClayBorder,
                    disabledContentColor = ClayTextSecondary
                ),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    role: String,
    text: String,
    isSearching: Boolean,
    isStreaming: Boolean = false
) {
    val isUser = role.lowercase() == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // "Searching…" chip
        if (isSearching) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ClaySurface)
                    .border(1.dp, ClayBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Searching",
                    tint = OrbGold,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Searching…",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ClayTextPrimary
                )
            }
        }

        if (text.isNotEmpty() || isStreaming) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) ClayBlueAccent else ClaySurface
                ),
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, ClayBorder),
                modifier = Modifier.padding(
                    start = if (isUser) 48.dp else 0.dp,
                    end = if (isUser) 0.dp else 48.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (text.isEmpty() && isStreaming) "..." else text,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                        color = if (isUser) Color.White else ClayTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SystemNoteChip(note: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ClayBlueContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = note,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = ClayOnBlueContainer
            )
        }
    }
}
