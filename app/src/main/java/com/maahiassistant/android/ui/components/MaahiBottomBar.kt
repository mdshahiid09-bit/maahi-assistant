package com.maahiassistant.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbCrimson

enum class BottomTab(val title: String, val iconRes: Int) {
    HOME("Home", R.drawable.ic_home),
    CHAT("Chat", R.drawable.ic_chat),
    SETTINGS("Settings", R.drawable.ic_settings),
    YAADEIN("Yaadein", R.drawable.ic_yaadein)
}

@Composable
fun MaahiBottomBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    isSessionActive: Boolean,
    onMicFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Soft rounded Clay bar
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ClaySurface,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ClayBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Tab 1: Home
                BottomBarItem(
                    tab = BottomTab.HOME,
                    isSelected = currentTab == BottomTab.HOME,
                    onClick = { onTabSelected(BottomTab.HOME) }
                )

                // Tab 2: Chat
                BottomBarItem(
                    tab = BottomTab.CHAT,
                    isSelected = currentTab == BottomTab.CHAT,
                    onClick = { onTabSelected(BottomTab.CHAT) }
                )

                // Spacer for center floating button
                Box(modifier = Modifier.size(56.dp))

                // Tab 3: Settings
                BottomBarItem(
                    tab = BottomTab.SETTINGS,
                    isSelected = currentTab == BottomTab.SETTINGS,
                    onClick = { onTabSelected(BottomTab.SETTINGS) }
                )

                // Tab 4: Yaadein
                BottomBarItem(
                    tab = BottomTab.YAADEIN,
                    isSelected = currentTab == BottomTab.YAADEIN,
                    onClick = { onTabSelected(BottomTab.YAADEIN) }
                )
            }
        }

        // Floating centre mic button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(60.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (isSessionActive) OrbCrimson else ClayBlueAccent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMicFabClick
                )
        ) {
            AnimatedContent(
                targetState = isSessionActive,
                label = "fab_icon_transition"
            ) { active ->
                Icon(
                    painter = painterResource(id = if (active) R.drawable.ic_stop else R.drawable.ic_mic),
                    contentDescription = if (active) "Stop Session" else "Start Session",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = tab.iconRes),
            contentDescription = tab.title,
            tint = if (isSelected) ClayBlueAccent else ClayTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) ClayBlueAccent else ClayTextSecondary
        )
    }
}
