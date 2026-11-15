package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.AspectRatio
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportProgressState
import com.example.domain.model.TextAnimationType
import com.example.domain.model.TransitionType
import com.example.domain.model.VideoFilterType
import com.example.domain.model.VideoResolution
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.NavyAbyss
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackAudioColor
import com.example.ui.theme.TrackImageColor
import com.example.ui.theme.TrackTextColor
import com.example.ui.theme.TrackVideoColor
import com.example.ui.viewmodel.EditorUiState
import kotlinx.coroutines.delay

private enum class EditorToolPanel(val label: String, val icon: ImageVector) {
    TRIM("Trim", Icons.Filled.Tune),
    SPEED("Speed", Icons.Filled.Speed),
    FILTERS("Filters", Icons.Filled.AutoFixHigh),
    TRANSITIONS("Transitions", Icons.Filled.Movie),
    ASPECT("Crop/Ratio", Icons.Filled.AspectRatio),
    VOLUME("Audio", Icons.Filled.VolumeUp),
    TEXT("Text", Icons.Filled.TextFields),
    SUBTITLES("Subtitles", Icons.Filled.Subtitles)
}

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    exportState: ExportProgressState,
    onUpdateTitle: (String) -> Unit,
    onAddMediaClip: (Uri, Boolean, String) -> Unit,
    onAddAudioTrack: (Uri, String) -> Unit,
    onSelectClip: (String) -> Unit,
    onPlayheadChange: (Long) -> Unit,
    onTogglePlaying: (Boolean) -> Unit,
    onTrimClip: (Long, Long) -> Unit,
    onSplitClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onRotateClip: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onFilterChange: (VideoFilterType) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onClipVolumeChange: (Float, Boolean) -> Unit,
    onAudioTrackVolumeChange: (String, Float, Boolean) -> Unit,
    onRemoveAudioTrack: (String) -> Unit,
    onAddTextOverlay: (String, TextAnimationType) -> Unit,
    onRemoveTextOverlay: (String) -> Unit,
    onAddSubtitle: (String, Long, Long) -> Unit,
    onRemoveSubtitle: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveProject: () -> Unit,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onResetExport: () -> Unit,
    onSaveToGallery: (String, String) -> Unit,
    onShareExport: (String, String) -> Unit
) {
    val context = LocalContext.current
    var activePanel by remember { mutableStateOf(EditorToolPanel.TRIM) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAddMediaClip(uri, false, "Clip")
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAddMediaClip(uri, true, "Photo")
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAddAudioTrack(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "Music")
        }
    }

    val selectedClip = state.selectedClip
    val targetAspect = AspectRatio.entries.find { it.code == state.timeline.aspectRatioCode } ?: AspectRatio.PORTRAIT_9_16

    // Live ExoPlayer preview bound to the currently selected video clip
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(selectedClip?.uriString, selectedClip?.isImage, selectedClip?.trimStartMs, selectedClip?.trimEndMs) {
        if (selectedClip != null && !selectedClip.isImage) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(selectedClip.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(selectedClip.trimStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(selectedClip.trimEndMs.coerceAtLeast(selectedClip.trimStartMs + 500L))
                        .build()
                )
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(selectedClip?.speed, selectedClip?.volume, selectedClip?.muted) {
        selectedClip?.let { clip ->
            exoPlayer.setPlaybackSpeed(clip.speed.coerceIn(0.25f, 4f))
            exoPlayer.volume = if (clip.muted) 0f else clip.volume
        }
    }

    LaunchedEffect(state.isPlaying) {
        if (selectedClip != null && !selectedClip.isImage) {
            if (state.isPlaying) exoPlayer.play() else exoPlayer.pause()
        }
    }

    LaunchedEffect(state.isPlaying, selectedClip?.id) {
        while (state.isPlaying) {
            if (selectedClip != null && !selectedClip.isImage) {
                onPlayheadChange(exoPlayer.currentPosition.coerceAtLeast(0L))
            } else if (selectedClip != null && selectedClip.isImage) {
                val next = state.playheadMs + 200L
                val total = state.timeline.totalDurationMs.coerceAtLeast(1000L)
                onPlayheadChange(if (next >= total) 0L else next)
            }
            delay(200L)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Project Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showRenameDialog = true }
                        .testTag("editor_project_title")
                ) {
                    Text(
                        text = state.projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.timeline.aspectRatioCode} • ${state.timeline.clips.size} clips • ${formatDurationMs(state.timeline.totalDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUndo, enabled = state.canUndo, modifier = Modifier.testTag("editor_undo")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) TextPrimary else TextMuted
                        )
                    }
                    IconButton(onClick = onRedo, enabled = state.canRedo, modifier = Modifier.testTag("editor_redo")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) TextPrimary else TextMuted
                        )
                    }
                    IconButton(onClick = onSaveProject, modifier = Modifier.testTag("editor_save_button")) {
                        Icon(Icons.Filled.Save, contentDescription = "Save project", tint = StudioCyan)
                    }
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_export_button")
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Upper Video Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedClip == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(Icons.Filled.VideoCall, contentDescription = null, tint = StudioVioletLight, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Add videos or photos to start editing", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier.testTag("editor_empty_add_video")
                            ) {
                                Text("+ Video Clip")
                            }
                            OutlinedButton(
                                onClick = {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.testTag("editor_empty_add_image")
                            ) {
                                Text("+ Photo")
                            }
                        }
                    }
                } else {
                    val filterTint = when (selectedClip.filter) {
                        VideoFilterType.NONE -> Color.Transparent
                        VideoFilterType.NEON_CYBER -> StudioViolet.copy(alpha = 0.22f)
                        VideoFilterType.CINEMA_TEAL -> StudioCyan.copy(alpha = 0.18f)
                        VideoFilterType.WARM_SUNSET -> Color(0xFFFF6B35).copy(alpha = 0.20f)
                        VideoFilterType.NOIR_BW -> Color(0xFF111827).copy(alpha = 0.45f)
                        VideoFilterType.VIVID_BOOST -> Color(0xFF3B82F6).copy(alpha = 0.12f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(targetAspect.ratio.coerceIn(0.45f, 1.8f))
                            .graphicsLayer { rotationZ = selectedClip.rotationDegrees },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedClip.isImage) {
                            AsyncImage(
                                model = Uri.parse(selectedClip.uriString),
                                contentDescription = selectedClip.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (filterTint != Color.Transparent) {
                            Box(modifier = Modifier.fillMaxSize().background(filterTint))
                        }
                    }

                    // Active Text & Subtitle Overlay Preview
                    val activeText = state.timeline.textOverlays.firstOrNull {
                        state.playheadMs in it.startMs..it.endMs
                    } ?: state.timeline.textOverlays.firstOrNull()
                    if (activeText != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = activeText.text,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(activeText.colorHex),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val activeSub = state.timeline.subtitles.firstOrNull {
                        state.playheadMs in it.startMs..it.endMs
                    } ?: state.timeline.subtitles.firstOrNull()
                    if (activeSub != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Text(
                                text = activeSub.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Center Play/Pause pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StudioViolet.copy(alpha = 0.85f))
                            .clickable { onTogglePlaying(!state.isPlaying) }
                            .testTag("editor_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Timeline Playhead & Scrubber
            GlassCard(contentPadding = PaddingValues(12.dp)) {
                val totalMs = state.timeline.totalDurationMs.coerceAtLeast(1000L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDurationMs(state.playheadMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioCyan
                    )
                    Text(
                        text = "TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatDurationMs(state.timeline.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Slider(
                    value = state.playheadMs.toFloat().coerceIn(0f, totalMs.toFloat()),
                    onValueChange = { pos ->
                        onPlayheadChange(pos.toLong())
                        if (selectedClip != null && !selectedClip.isImage) {
                            exoPlayer.seekTo(pos.toLong())
                        }
                    },
                    valueRange = 0f..totalMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioViolet)
                )

                // Multi-Track Clips Strip (Video/Image Track)
                Text("Video & Photo Track (drag clip right-edge to trim)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.timeline.clips.forEach { clip ->
                        val isSelected = clip.id == selectedClip?.id
                        val widthDp = (clip.effectiveDurationMs / 60L).coerceIn(96L, 220L).toInt().dp
                        val trackColor = if (clip.isImage) TrackImageColor else TrackVideoColor
                        Surface(
                            modifier = Modifier
                                .width(widthDp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectClip(clip.id) }
                                .pointerInput(clip.id, clip.trimStartMs, clip.trimEndMs) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        if (isSelected) {
                                            val deltaMs = (dragAmount * 25f).toLong()
                                            onTrimClip(clip.trimStartMs, clip.trimEndMs + deltaMs)
                                        }
                                    }
                                },
                            color = trackColor.copy(alpha = if (isSelected) 0.42f else 0.22f),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) StudioCyan else trackColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = clip.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatDurationMs(clip.effectiveDurationMs)} • ${clip.speed}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioCyan
                                )
                            }
                        }
                    }

                    // Quick + Clip buttons directly on timeline
                    Surface(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                            .testTag("editor_timeline_add_video"),
                        color = NavyElevated,
                        border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VideoCall, contentDescription = "Add video", tint = StudioVioletLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Video", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .testTag("editor_timeline_add_image"),
                        color = NavyElevated,
                        border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add image", tint = StudioCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Photo", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }

                // Audio & Text Summary Tracks
                if (state.timeline.audioTracks.isNotEmpty() || state.timeline.textOverlays.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.timeline.audioTracks.forEach { aud ->
                            Surface(
                                color = TrackAudioColor.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, TrackAudioColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TrackAudioColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(aud.title, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
                                }
                            }
                        }
                        state.timeline.textOverlays.forEach { txt ->
                            Surface(
                                color = TrackTextColor.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, TrackTextColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.TextFields, contentDescription = null, tint = TrackTextColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(txt.text, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Horizontally Scrollable Mobile Editing Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolbarActionChip("Split", Icons.Filled.ContentCut, enabled = selectedClip != null, onClick = onSplitClip)
                ToolbarActionChip("Rotate", Icons.Filled.RotateRight, enabled = selectedClip != null, onClick = onRotateClip)
                ToolbarActionChip("Move <-", Icons.Filled.ChevronLeft, enabled = selectedClip != null) { onMoveClip(-1) }
                ToolbarActionChip("Move ->", Icons.Filled.ChevronRight, enabled = selectedClip != null) { onMoveClip(1) }
                ToolbarActionChip("+ Music", Icons.Filled.AudioFile, enabled = true) {
                    audioPicker.launch(arrayOf("audio/*"))
                }
                ToolbarActionChip("Delete", Icons.Filled.Delete, enabled = selectedClip != null, tint = StatusError, onClick = onDeleteClip)
            }

            // Tool Inspector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorToolPanel.entries.forEach { panel ->
                    FilterChip(
                        selected = activePanel == panel,
                        onClick = { activePanel = panel },
                        leadingIcon = {
                            Icon(panel.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(panel.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioViolet,
                            selectedLabelColor = TextPrimary,
                            containerColor = NavyElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Active Tool Panel Controls
            GlassCard {
                when (activePanel) {
                    EditorToolPanel.TRIM -> {
                        if (selectedClip == null) {
                            Text("Select a clip on the timeline to trim.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            val maxDur = selectedClip.sourceDurationMs.coerceAtLeast(1000L).toFloat()
                            Text(
                                text = "Trim \"${selectedClip.label}\": ${formatDurationMs(selectedClip.trimStartMs)} -> ${formatDurationMs(selectedClip.trimEndMs)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            RangeSlider(
                                value = selectedClip.trimStartMs.toFloat().coerceIn(0f, maxDur)..selectedClip.trimEndMs.toFloat().coerceIn(0f, maxDur),
                                onValueChange = { range ->
                                    onTrimClip(range.start.toLong(), range.endInclusive.toLong())
                                },
                                valueRange = 0f..maxDur
                            )
                        }
                    }

                    EditorToolPanel.SPEED -> {
                        Text("Clip Playback Speed", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                FilterChip(
                                    selected = selectedClip?.speed == spd,
                                    onClick = { onSpeedChange(spd) },
                                    label = { Text("${spd}x") },
                                    enabled = selectedClip != null
                                )
                            }
                        }
                    }

                    EditorToolPanel.FILTERS -> {
                        Text("Color Grade Filters", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoFilterType.entries.forEach { filter ->
                                FilterChip(
                                    selected = selectedClip?.filter == filter,
                                    onClick = { onFilterChange(filter) },
                                    label = { Text(filter.label) },
                                    enabled = selectedClip != null
                                )
                            }
                        }
                    }

                    EditorToolPanel.TRANSITIONS -> {
                        Text("Clip Transition Out", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransitionType.entries.forEach { tr ->
                                FilterChip(
                                    selected = selectedClip?.transitionToNext == tr,
                                    onClick = { onTransitionChange(tr) },
                                    label = { Text(tr.label) },
                                    enabled = selectedClip != null
                                )
                            }
                        }
                    }

                    EditorToolPanel.ASPECT -> {
                        Text("Canvas Aspect Ratio & Crop", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = state.timeline.aspectRatioCode == ratio.code,
                                    onClick = { onAspectRatioChange(ratio) },
                                    label = { Text(ratio.label) }
                                )
                            }
                        }
                    }

                    EditorToolPanel.VOLUME -> {
                        if (selectedClip != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Clip Audio (${(selectedClip.volume * 100).toInt()}%)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                IconButton(onClick = { onClipVolumeChange(selectedClip.volume, !selectedClip.muted) }) {
                                    Icon(
                                        imageVector = if (selectedClip.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        contentDescription = "Mute clip",
                                        tint = StudioCyan
                                    )
                                }
                            }
                            Slider(
                                value = selectedClip.volume,
                                onValueChange = { onClipVolumeChange(it, false) }
                            )
                        }
                        state.timeline.audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Music: ${track.title}", style = MaterialTheme.typography.bodySmall, color = StudioCyan, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onAudioTrackVolumeChange(track.id, track.volume, !track.muted) }) {
                                    Icon(
                                        imageVector = if (track.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        contentDescription = "Mute music",
                                        tint = TextPrimary
                                    )
                                }
                                IconButton(onClick = { onRemoveAudioTrack(track.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove audio", tint = StatusError)
                                }
                            }
                        }
                        if (state.timeline.audioTracks.isEmpty()) {
                            OutlinedButton(
                                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Background Music Track")
                            }
                        }
                    }

                    EditorToolPanel.TEXT -> {
                        var overlayText by remember { mutableStateOf("") }
                        var anim by remember { mutableStateOf(TextAnimationType.FADE_IN) }
                        Text("Add Animated Text Overlay", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = overlayText,
                            onValueChange = { overlayText = it },
                            placeholder = { Text("Enter headline or hook text", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextAnimationType.entries.forEach { type ->
                                FilterChip(
                                    selected = anim == type,
                                    onClick = { anim = type },
                                    label = { Text(type.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                onAddTextOverlay(overlayText, anim)
                                overlayText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Text at Playhead")
                        }
                        state.timeline.textOverlays.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("\"${item.text}\" (${item.animation.label})", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                IconButton(onClick = { onRemoveTextOverlay(item.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove text", tint = StatusError)
                                }
                            }
                        }
                    }

                    EditorToolPanel.SUBTITLES -> {
                        var subText by remember { mutableStateOf("") }
                        Text("Subtitle Cues", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = subText,
                            onValueChange = { subText = it },
                            placeholder = { Text("Enter subtitle line at current playhead", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                onAddSubtitle(subText, state.playheadMs, state.playheadMs + 2500L)
                                subText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Insert Subtitle Cue")
                        }
                        state.timeline.subtitles.forEach { cue ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatDurationMs(cue.startMs)}: ${cue.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemoveSubtitle(cue.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete cue", tint = StatusError)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showRenameDialog) {
        var draftTitle by remember(state.projectTitle) { mutableStateOf(state.projectTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = NavySurface,
            title = { Text("Project Title", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTitle(draftTitle)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportDialog) {
        ExportVideoDialog(
            defaultAspect = targetAspect,
            exportState = exportState,
            projectTitle = state.projectTitle,
            onDismiss = {
                onResetExport()
                showExportDialog = false
            },
            onStartExport = onStartExport,
            onCancelExport = onCancelExport,
            onSaveToGallery = onSaveToGallery,
            onShare = onShareExport
        )
    }
}

@Composable
private fun ToolbarActionChip(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    tint: Color = StudioCyan,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = NavyElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) TextPrimary else TextMuted
            )
        }
    }
}

@Composable
private fun ExportVideoDialog(
    defaultAspect: AspectRatio,
    exportState: ExportProgressState,
    projectTitle: String,
    onDismiss: () -> Unit,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onSaveToGallery: (String, String) -> Unit,
    onShare: (String, String) -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.MP4) }
    var resolution by remember { mutableStateOf(VideoResolution.FHD_1080) }
    var aspect by remember { mutableStateOf(defaultAspect) }

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportProgressState.Exporting) onDismiss()
        },
        containerColor = NavySurface,
        title = { Text("Export Video", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (exportState) {
                    is ExportProgressState.Idle -> {
                        Text("Format", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportFormat.entries.forEach { fmt ->
                                FilterChip(
                                    selected = format == fmt,
                                    onClick = { format = fmt },
                                    label = { Text(fmt.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Text("Resolution", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoResolution.entries.forEach { res ->
                                FilterChip(
                                    selected = resolution == res,
                                    onClick = { resolution = res },
                                    label = { Text(res.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(AspectRatio.PORTRAIT_9_16, AspectRatio.LANDSCAPE_16_9, AspectRatio.SQUARE_1_1).forEach { r ->
                                FilterChip(
                                    selected = aspect == r,
                                    onClick = { aspect = r },
                                    label = { Text(r.code) }
                                )
                            }
                        }
                    }

                    is ExportProgressState.Exporting -> {
                        Text(exportState.stageLabel, style = MaterialTheme.typography.bodyMedium, color = StudioCyan)
                        LinearProgressIndicator(
                            progress = { (exportState.progressPercent / 100f).coerceIn(0.05f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = StudioCyan
                        )
                    }

                    is ExportProgressState.Completed -> {
                        Text(
                            text = "Rendered successfully (${exportState.fileSizeBytes / 1024} KB)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusSuccess
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSaveToGallery(exportState.outputPath, projectTitle) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                            Button(
                                onClick = { onShare(exportState.outputPath, projectTitle) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }

                    is ExportProgressState.Error -> {
                        Text(exportState.message, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                is ExportProgressState.Idle -> {
                    GradientPrimaryButton(
                        text = "Start Export",
                        onClick = { onStartExport(ExportConfig(format, resolution, aspect)) },
                        testTag = "export_confirm_button"
                    )
                }
                is ExportProgressState.Exporting -> {
                    TextButton(onClick = onCancelExport) { Text("Cancel Export", color = StatusError) }
                }
                else -> {
                    Button(onClick = onDismiss) { Text("Done") }
                }
            }
        },
        dismissButton = {
            if (exportState is ExportProgressState.Idle) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
