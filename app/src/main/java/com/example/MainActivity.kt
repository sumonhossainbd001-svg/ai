package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.NotificationEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.StudioTopBar
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.AiToolsScreen
import com.example.ui.screens.ApiManagerScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AiVideoStudioTheme
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGradients
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudioViewModel

const val ROUTE_HOME = "home"
const val ROUTE_GENERATE = "generate"
const val ROUTE_EDITOR = "editor"
const val ROUTE_PROJECTS = "projects"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_API_MANAGER = "api_manager"
const val ROUTE_AI_TOOLS = "ai_tools"

private data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudioViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            AiVideoStudioTheme(accentStyle = settings.themeAccent) {
                AiVideoStudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AiVideoStudioApp(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val generatorState by viewModel.generatorState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val aiToolsState by viewModel.aiToolsState.collectAsStateWithLifecycle()
    val ttsReady by viewModel.ttsReady.collectAsStateWithLifecycle()
    val ttsSpeaking by viewModel.ttsSpeaking.collectAsStateWithLifecycle()
    val activePlayerMedia by viewModel.activePlayerMedia.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage!!)
            viewModel.clearSnackbar()
        }
    }

    val bottomTabs = listOf(
        BottomTabItem(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomTabItem(ROUTE_GENERATE, "Generate", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        BottomTabItem(ROUTE_EDITOR, "Editor", Icons.Filled.ContentCut, Icons.Outlined.ContentCut),
        BottomTabItem(ROUTE_PROJECTS, "Projects", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
        BottomTabItem(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ROUTE_HOME

    val topBarTitle = when (currentRoute) {
        ROUTE_HOME -> "AI Video Studio"
        ROUTE_GENERATE -> "AI Generator"
        ROUTE_EDITOR -> "Mobile Editor"
        ROUTE_PROJECTS -> "My Projects"
        ROUTE_SETTINGS -> "Studio Settings"
        ROUTE_API_MANAGER -> "API Manager"
        ROUTE_AI_TOOLS -> "AI Smart Tools"
        else -> "AI Video Studio"
    }
    val isSubScreen = currentRoute == ROUTE_API_MANAGER || currentRoute == ROUTE_AI_TOOLS

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioGradients.BackgroundGlow)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                StudioTopBar(
                    title = topBarTitle,
                    credits = settings.localCredits,
                    unreadNotifications = unreadCount,
                    onNotificationsClick = {
                        showNotificationsDialog = true
                        viewModel.markAllNotificationsRead()
                    },
                    onCreditsClick = { navController.navigate(ROUTE_API_MANAGER) },
                    showBackButton = isSubScreen,
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = {
                if (!isSubScreen) {
                    NavigationBar(
                        containerColor = NavyDeep.copy(alpha = 0.96f),
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StudioCyan,
                                    selectedTextColor = StudioCyan,
                                    indicatorColor = StudioViolet.copy(alpha = 0.25f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            settings = settings,
                            providers = providers,
                            projects = projects,
                            jobs = jobs,
                            templates = viewModel.templates,
                            onNavigateToGenerate = { mode ->
                                viewModel.setGeneratorMode(mode)
                                navigateTopTab(ROUTE_GENERATE)
                            },
                            onNavigateToEditor = { navigateTopTab(ROUTE_EDITOR) },
                            onNavigateToProjects = { navigateTopTab(ROUTE_PROJECTS) },
                            onNavigateToApiManager = { navController.navigate(ROUTE_API_MANAGER) },
                            onNavigateToAiTool = { tool ->
                                viewModel.selectAiTool(tool)
                                navController.navigate(ROUTE_AI_TOOLS)
                            },
                            onApplyTemplate = { tpl ->
                                viewModel.applyTemplateToGenerator(tpl)
                                navigateTopTab(ROUTE_GENERATE)
                            },
                            onOpenProjectInEditor = { proj ->
                                viewModel.openProjectInEditor(proj)
                                navigateTopTab(ROUTE_EDITOR)
                            },
                            onPlayVideo = { uri, title, path ->
                                viewModel.openVideoPlayer(uri, title, path)
                            }
                        )
                    }

                    composable(ROUTE_GENERATE) {
                        GenerateScreen(
                            formState = generatorState,
                            providers = providers,
                            jobs = jobs,
                            onModeChange = viewModel::setGeneratorMode,
                            onPromptChange = viewModel::updatePrompt,
                            onNegativePromptChange = viewModel::updateNegativePrompt,
                            onEnhancePrompt = viewModel::enhancePromptWithAi,
                            onAutoModeChange = viewModel::setAutoMode,
                            onProviderSelect = viewModel::selectProvider,
                            onModelSelect = viewModel::selectModel,
                            onDurationSelect = viewModel::setDurationSec,
                            onAspectRatioSelect = viewModel::setAspectRatio,
                            onResolutionSelect = viewModel::setResolution,
                            onMotionSelect = viewModel::setMotionIntensity,
                            onMediaUriSelected = viewModel::setInputMediaUri,
                            onSubmitGeneration = viewModel::submitGeneration,
                            onRetryJob = viewModel::retryJob,
                            onDeleteJob = viewModel::deleteJob,
                            onPlayVideo = { uri, title, path ->
                                viewModel.openVideoPlayer(uri, title, path)
                            },
                            onOpenApiManager = { navController.navigate(ROUTE_API_MANAGER) }
                        )
                    }

                    composable(ROUTE_EDITOR) {
                        EditorScreen(
                            state = editorState,
                            exportState = exportState,
                            onUpdateTitle = viewModel::updateEditorProjectTitle,
                            onAddMediaClip = viewModel::addMediaToTimeline,
                            onAddAudioTrack = viewModel::addAudioTrack,
                            onSelectClip = viewModel::selectEditorClip,
                            onPlayheadChange = viewModel::updatePlayhead,
                            onTogglePlaying = viewModel::setEditorPlaying,
                            onTrimClip = viewModel::trimSelectedClip,
                            onSplitClip = viewModel::splitSelectedClipAtPlayhead,
                            onDeleteClip = viewModel::deleteSelectedClip,
                            onMoveClip = viewModel::moveSelectedClip,
                            onRotateClip = viewModel::rotateSelectedClip,
                            onSpeedChange = viewModel::setSelectedClipSpeed,
                            onFilterChange = viewModel::setSelectedClipFilter,
                            onTransitionChange = viewModel::setSelectedClipTransition,
                            onAspectRatioChange = viewModel::setEditorAspectRatio,
                            onClipVolumeChange = viewModel::setSelectedClipVolume,
                            onAudioTrackVolumeChange = viewModel::setAudioTrackVolume,
                            onRemoveAudioTrack = viewModel::removeAudioTrack,
                            onAddTextOverlay = viewModel::addTextOverlay,
                            onRemoveTextOverlay = viewModel::removeTextOverlay,
                            onAddSubtitle = viewModel::addSubtitleCue,
                            onRemoveSubtitle = viewModel::removeSubtitleCue,
                            onUndo = viewModel::undoEdit,
                            onRedo = viewModel::redoEdit,
                            onSaveProject = viewModel::saveEditorProject,
                            onStartExport = viewModel::startExport,
                            onCancelExport = viewModel::cancelExport,
                            onResetExport = viewModel::resetExportState,
                            onSaveToGallery = viewModel::saveVideoToGallery,
                            onShareExport = { path, title ->
                                viewModel.buildShareIntent(path, title)?.let { context.startActivity(it) }
                            }
                        )
                    }

                    composable(ROUTE_PROJECTS) {
                        ProjectsScreen(
                            projects = projects,
                            onOpenInEditor = { proj ->
                                viewModel.openProjectInEditor(proj)
                                navigateTopTab(ROUTE_EDITOR)
                            },
                            onPlayVideo = { uri, title, path ->
                                viewModel.openVideoPlayer(uri, title, path)
                            },
                            onRenameProject = viewModel::renameProject,
                            onDuplicateProject = viewModel::duplicateProject,
                            onDeleteProject = viewModel::deleteProject,
                            onSaveToGallery = viewModel::saveVideoToGallery,
                            onShareVideo = { path, title ->
                                viewModel.buildShareIntent(path, title)?.let { context.startActivity(it) }
                            }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            providers = providers,
                            storageStats = storageStats,
                            onOpenApiManager = { navController.navigate(ROUTE_API_MANAGER) },
                            onUpdateCreatorName = viewModel::updateCreatorName,
                            onAutoFallbackChange = viewModel::setAutoFallback,
                            onDefaultAspectChange = viewModel::setDefaultAspectRatio,
                            onDefaultResolutionChange = viewModel::setDefaultResolution,
                            onJobNotifChange = viewModel::setJobNotifications,
                            onApiNotifChange = viewModel::setApiNotifications,
                            onThemeAccentChange = viewModel::setThemeAccent,
                            onClearCache = viewModel::clearTempCache
                        )
                    }

                    composable(ROUTE_API_MANAGER) {
                        ApiManagerScreen(
                            providers = providers,
                            onSaveProvider = viewModel::saveProviderConfig,
                            onTestConnection = { provider -> viewModel.testProviderConnection(provider) },
                            onRemoveKey = viewModel::removeProviderKey,
                            onSetDefault = viewModel::setDefaultProvider
                        )
                    }

                    composable(ROUTE_AI_TOOLS) {
                        AiToolsScreen(
                            state = aiToolsState,
                            ttsReady = ttsReady,
                            ttsSpeaking = ttsSpeaking,
                            onSelectTool = viewModel::selectAiTool,
                            onInputPromptChange = viewModel::updateAiToolPrompt,
                            onRunTool = viewModel::runSelectedAiTool,
                            onTtsParamsChange = viewModel::updateTtsParams,
                            onSpeakPreview = { viewModel.speakTtsPreview() },
                            onStopTts = viewModel::stopTts,
                            onSynthesizeWavToEditor = viewModel::synthesizeTtsToEditorAudio
                        )
                    }
                }
            }
        }
    }

    activePlayerMedia?.let { media ->
        VideoPlayerModal(
            media = media,
            onDismiss = viewModel::closeVideoPlayer,
            onSaveToGallery = viewModel::saveVideoToGallery,
            onShare = { path, title ->
                viewModel.buildShareIntent(path, title)?.let { context.startActivity(it) }
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false },
            onClearAll = {
                viewModel.clearNotifications()
                showNotificationsDialog = false
            }
        )
    }
}

@Composable
private fun NotificationsDialog(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text("Studio Notifications", color = TextPrimary) },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    "No notifications yet. Job completions, API connection tests, and MP4 exports appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        GlassCard(cornerRadius = 12.dp) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                            Spacer(Modifier.height(2.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("Clear All") }
            }
        }
    )
}
