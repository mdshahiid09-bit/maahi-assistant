package com.maahiassistant.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.ui.theme.ClayBackground
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClayOnBlueContainer
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary

enum class DrawerDestination(val title: String, val iconRes: Int) {
    HOME("Home", R.drawable.ic_home),
    CHAT("Chat", R.drawable.ic_chat),
    CHAT_HISTORY("Chat history", R.drawable.ic_history),
    HELP_GUIDE("Help and guide", R.drawable.ic_help),
    SETTINGS("Settings", R.drawable.ic_settings)
}

@Composable
fun MaahiDrawerSheet(
    currentDestination: String,
    onDestinationSelect: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = ClaySurface,
        modifier = modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClayBackground)
                    .padding(16.dp)
            ) {
                Column {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ClayBlueAccent)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Maahi Logo",
                            tint = ClaySurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Maahi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayTextPrimary
                    )
                    Text(
                        text = "Real-time Voice AI Assistant",
                        fontSize = 12.sp,
                        color = ClayTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ClayBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Items
            DrawerDestination.entries.forEach { dest ->
                val selected = currentDestination == dest.name
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = dest.title,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = dest.iconRes),
                            contentDescription = dest.title,
                            tint = if (selected) ClayBlueAccent else ClayTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    selected = selected,
                    onClick = { onDestinationSelect(dest) },
                    shape = RoundedCornerShape(16.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = ClayBlueContainer,
                        selectedTextColor = ClayOnBlueContainer,
                        unselectedContainerColor = ClaySurface,
                        unselectedTextColor = ClayTextPrimary
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Maahi v1.0.2 (Phase 1)",
                fontSize = 11.sp,
                color = ClayTextSecondary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}
