package com.maahiassistant.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.R
import com.maahiassistant.android.ui.MainViewModel
import com.maahiassistant.android.ui.theme.ClayBackground
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClayBlueContainer
import com.maahiassistant.android.ui.theme.ClayBorder
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClaySurfaceCard
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onRequestMicPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // 0, 1, 2
    var keyInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClayBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    text = "Skip",
                    color = ClayTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Animated Content for 3 steps
        AnimatedContent(
            targetState = currentStep,
            label = "onboarding_step_transition",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                0 -> StepOneWhatIsMaahi()
                1 -> StepTwoPermissions(onRequestMicPermission = onRequestMicPermission)
                2 -> StepThreeAddKey(
                    keyInput = keyInput,
                    onKeyChange = { keyInput = it },
                    onOpenAIStudio = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://aistudio.google.com/app/apikey")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Bottom Controls: Page Indicator & Next / Start Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (currentStep == index) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (currentStep == index) ClayBlueAccent else ClayBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        if (keyInput.isNotBlank()) {
                            viewModel.testAndSaveApiKey(keyInput)
                        }
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClayBlueAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (currentStep == 2) "Get Started" else "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StepOneWhatIsMaahi() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(ClayBlueContainer)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Maahi Orb",
                tint = ClayBlueAccent,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to Maahi",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ClayTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Experience conversational voice AI in real time. Powered by the Gemini Multimodal Live API, Maahi listens continuously, speaks naturally, and lets you interrupt at any moment.",
            fontSize = 14.5.sp,
            lineHeight = 22.sp,
            color = ClayTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun StepTwoPermissions(onRequestMicPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ClayBlueContainer)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = "Microphone",
                tint = ClayBlueAccent,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Essential Permissions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ClayTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Maahi captures 16kHz audio directly to stream with Gemini and uses a low-priority notification so your conversation stays alive when you minimize the app.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ClayTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onRequestMicPermission,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = ClayBlueAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Grant Microphone Permission", color = ClayBlueAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StepThreeAddKey(
    keyInput: String,
    onKeyChange: (String) -> Unit,
    onOpenAIStudio: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ClayBlueContainer)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_key),
                contentDescription = "Gemini Key",
                tint = ClayBlueAccent,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Add Your Gemini Key",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ClayTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Enter your Google AI Studio API key. You can also add or change it later in Settings.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ClayTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = onKeyChange,
            placeholder = { Text("AIzaSy...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ClayBlueAccent,
                unfocusedBorderColor = ClayBorder,
                focusedContainerColor = ClaySurfaceCard,
                unfocusedContainerColor = ClaySurfaceCard
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenAIStudio,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_open_browser),
                contentDescription = null,
                tint = ClayBlueAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Get Free Key at Google AI Studio", fontSize = 12.5.sp, color = ClayBlueAccent)
        }
    }
}
