package com.maahiassistant.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun HelpGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = ClayBlueAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Help and Guide",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayTextPrimary
                )
                Text(
                    text = "Everything you need to know about Maahi",
                    fontSize = 12.sp,
                    color = ClayTextSecondary
                )
            }
        }

        // Section 1: Real-time Live Voice
        GuideCard(
            title = "1. Real-Time Live Voice",
            iconRes = R.drawable.ic_mic,
            description = "Unlike traditional assistants that wait for complete sentences or require voice typing, Maahi uses the Gemini Multimodal Live API (BidiGenerateContent). Your voice is streamed as raw 16kHz PCM audio over WebSockets and answered immediately with 24kHz synthesized speech."
        )

        // Section 2: Barge-in (Interruption)
        GuideCard(
            title = "2. Natural Interruption (Barge-In)",
            iconRes = R.drawable.ic_stop,
            description = "You don't need to wait for Maahi to finish speaking. Simply start talking whenever you want, and Maahi will instantly halt its audio output, clear pending speech buffers, and listen to your new thought."
        )

        // Section 3: Google Search Grounding
        GuideCard(
            title = "3. Live Google Search Grounding",
            iconRes = R.drawable.ic_search,
            description = "Google Search is natively integrated into every session. Whenever you ask about current events, sports scores, weather, or real-time data, Gemini autonomously searches the web and speaks the fresh facts naturally. A small 'Searching…' indicator appears in the Chat tab."
        )

        // Section 4: Background Sessions
        GuideCard(
            title = "4. Background Calls & Notifications",
            iconRes = R.drawable.ic_info,
            description = "Maahi runs as an Android foreground service. You can minimize the app, switch to other apps, or turn off the screen; the session remains active. Use the ongoing notification to quickly Mute/Unmute or End the call at any time."
        )

        // Section 5: Speaker vs Earpiece
        GuideCard(
            title = "5. Private Earpiece Mode",
            iconRes = R.drawable.ic_earpiece,
            description = "Switch between the phone's loud speaker and the private earpiece speaker on the Home screen or in Settings. In earpiece mode, you can hold the phone to your ear like a private phone call."
        )

        // Section 6: Troubleshooting
        GuideCard(
            title = "6. Troubleshooting & Resumption",
            iconRes = R.drawable.ic_help,
            description = "If your internet connection drops momentarily, Maahi automatically holds conversation state using session resumption tokens and reconnects silently once the network returns. If mic permission is denied, tap the banner on Home to grant it."
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GuideCard(
    title: String,
    iconRes: Int,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ClaySurface),
        border = BorderStroke(1.dp, ClayBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ClayBlueAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = ClayTextSecondary
            )
        }
    }
}
