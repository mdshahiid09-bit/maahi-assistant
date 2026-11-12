package com.maahiassistant.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.maahiassistant.android.MaahiApplication
import com.maahiassistant.android.ui.components.BottomTab
import com.maahiassistant.android.ui.components.DrawerDestination
import com.maahiassistant.android.ui.components.EdgeGlow
import com.maahiassistant.android.ui.components.MaahiBottomBar
import com.maahiassistant.android.ui.components.MaahiDrawerSheet
import com.maahiassistant.android.ui.components.MaahiTopBar
import com.maahiassistant.android.ui.screens.ChatHistoryScreen
import com.maahiassistant.android.ui.screens.ChatScreen
import com.maahiassistant.android.ui.screens.HelpGuideScreen
import com.maahiassistant.android.ui.screens.HomeScreen
import com.maahiassistant.android.ui.screens.LiveLogsScreen
import com.maahiassistant.android.ui.screens.OnboardingScreen
import com.maahiassistant.android.ui.screens.SettingsScreen
import com.maahiassistant.android.ui.screens.YaadeinScreen
import com.maahiassistant.android.ui.theme.ClayBackground
import com.maahiassistant.android.ui.theme.MaahiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MaahiApplication
        viewModel = MainViewModel(app.chatRepository, app.settingsRepository)

        setContent {
            MaahiTheme {
                MainAppContent(
                    viewModel = viewModel,
                    onKeepScreenOnChanged = { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshKeyStatus()
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentTab by viewModel.currentTab.collectAsState()
    val currentDrawerDestination by viewModel.currentDrawerDestination.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val keepScreenOnSetting by viewModel.keepScreenOn.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    // Screen On handling
    LaunchedEffect(isSessionActive, keepScreenOnSetting) {
        onKeepScreenOnChanged(isSessionActive && keepScreenOnSetting)
    }

    // Permissions check & launcher
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            requestPermissions()
        }
    }

    // Back handling
    BackHandler(enabled = drawerState.isOpen || currentDrawerDestination != DrawerDestination.HOME) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.selectDrawerDestination(DrawerDestination.HOME)
        }
    }

    if (!onboardingCompleted) {
        OnboardingScreen(
            viewModel = viewModel,
            onRequestMicPermission = { requestPermissions() },
            onFinish = { viewModel.completeOnboarding() }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MaahiDrawerSheet(
                currentDestination = currentDrawerDestination.name,
                onDestinationSelect = { dest ->
                    viewModel.selectDrawerDestination(dest)
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClayBackground)
        ) {
            Scaffold(
                topBar = {
                    val title = when (currentDrawerDestination) {
                        DrawerDestination.HOME -> "Maahi"
                        DrawerDestination.CHAT -> "Chat"
                        DrawerDestination.CHAT_HISTORY -> "History"
                        DrawerDestination.LIVE_LOGS -> "Live Logs"
                        DrawerDestination.HELP_GUIDE -> "Guide"
                        DrawerDestination.SETTINGS -> "Settings"
                    }
                    MaahiTopBar(
                        title = title,
                        onDrawerClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        actions = {
                            if (currentDrawerDestination != DrawerDestination.LIVE_LOGS) {
                                IconButton(
                                    onClick = { viewModel.selectDrawerDestination(DrawerDestination.LIVE_LOGS) }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_terminal),
                                        contentDescription = "Live Logs",
                                        tint = ClayBlueAccent
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (currentDrawerDestination != DrawerDestination.CHAT_HISTORY &&
                        currentDrawerDestination != DrawerDestination.HELP_GUIDE &&
                        currentDrawerDestination != DrawerDestination.LIVE_LOGS
                    ) {
                        MaahiBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { tab ->
                                viewModel.selectTab(tab)
                                when (tab) {
                                    BottomTab.HOME -> viewModel.selectDrawerDestination(DrawerDestination.HOME)
                                    BottomTab.CHAT -> viewModel.selectDrawerDestination(DrawerDestination.CHAT)
                                    BottomTab.SETTINGS -> viewModel.selectDrawerDestination(DrawerDestination.SETTINGS)
                                    BottomTab.YAADEIN -> { /* Handled in tab state */ }
                                }
                            },
                            isSessionActive = isSessionActive,
                            onMicFabClick = {
                                if (!hasMicPermission) {
                                    requestPermissions()
                                } else {
                                    viewModel.toggleSession(
                                        context = context,
                                        onMissingKey = {
                                            viewModel.selectTab(BottomTab.SETTINGS)
                                            viewModel.selectDrawerDestination(DrawerDestination.SETTINGS)
                                        },
                                        onMicPermissionNeeded = { requestPermissions() }
                                    )
                                }
                            }
                        )
                    }
                },
                containerColor = ClayBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        currentDrawerDestination == DrawerDestination.LIVE_LOGS -> {
                            LiveLogsScreen(
                                onBack = { viewModel.selectDrawerDestination(DrawerDestination.HOME) }
                            )
                        }
                        currentDrawerDestination == DrawerDestination.CHAT_HISTORY -> {
                            ChatHistoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectDrawerDestination(DrawerDestination.HOME) }
                            )
                        }
                        currentDrawerDestination == DrawerDestination.HELP_GUIDE -> {
                            HelpGuideScreen(
                                onBack = { viewModel.selectDrawerDestination(DrawerDestination.HOME) }
                            )
                        }
                        currentTab == BottomTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                hasMicPermission = hasMicPermission,
                                onRequestMicPermission = { requestPermissions() },
                                onNavigateToSettings = {
                                    viewModel.selectTab(BottomTab.SETTINGS)
                                    viewModel.selectDrawerDestination(DrawerDestination.SETTINGS)
                                }
                            )
                        }
                        currentTab == BottomTab.CHAT -> {
                            ChatScreen(viewModel = viewModel)
                        }
                        currentTab == BottomTab.SETTINGS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                        currentTab == BottomTab.YAADEIN -> {
                            YaadeinScreen()
                        }
                    }
                }
            }

            // Edge Glow: soft coloured frame at screen edges only while session is live
            EdgeGlow(
                isSessionActive = isSessionActive,
                orbState = orbState
            )
        }
    }
}
