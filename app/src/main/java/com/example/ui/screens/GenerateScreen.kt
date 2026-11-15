package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.GenerationJobEntity
import com.example.domain.model.AspectRatio
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.GenerationMode
import com.example.domain.model.MotionIntensity
import com.example.domain.model.ProviderConfig
import com.example.domain.model.VideoProviderType
import com.example.domain.model.VideoResolution
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.ProviderStatusChip
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GeneratorFormState
import java.io.File

@Composable
fun GenerateScreen(
    formState: GeneratorFormState,
    providers: List<ProviderConfig>,
    jobs: List<GenerationJobEntity>,
    onModeChange: (GenerationMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onNegativePromptChange: (String) -> Unit,
    onEnhancePrompt: () -> Unit,
    onAutoModeChange: (Boolean) -> Unit,
    onProviderSelect: (VideoProviderType) -> Unit,
    onModelSelect: (String) -> Unit,
    onDurationSelect: (Int) -> Unit,
    onAspectRatioSelect: (AspectRatio) -> Unit,
    onResolutionSelect: (VideoResolution) -> Unit,
    onMotionSelect: (MotionIntensity) -> Unit,
    onMediaUriSelected: (String?) -> Unit,
    onSubmitGeneration: () -> Unit,
    onRetryJob: (String) -> Unit,
    onDeleteJob: (String) -> Unit,
    onPlayVideo: (String, String, String?) -> Unit,
    onOpenApiManager: () -> Unit
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onMediaUriSelected(uri.toString())
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onMediaUriSelected(uri.toString())
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            onMediaUriSelected(pendingCameraUri.toString())
        }
    }

    fun launchCameraCapture() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val photoFile = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermissionMessage = null
            launchCameraCapture()
        } else {
            cameraPermissionMessage = "Camera permission is required to capture a reference photo."
        }
    }

    val activeProviderConfig = if (formState.autoMode) {
        providers.firstOrNull { it.hasApiKey && it.type.supportedModes.contains(formState.mode) }
    } else {
        providers.find { it.type == formState.selectedProvider }
    }
    val hasAnyConfiguredProvider = providers.any { it.hasApiKey }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Tabs (Text / Image / Video to Video)
            item {
                GlassCard(contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GenerationMode.entries.forEach { mode ->
                            val isSupported = formState.autoMode || formState.selectedProvider.supportedModes.contains(mode)
                            val isSelected = formState.mode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = isSupported) { onModeChange(mode) }
                                    .testTag("gen_mode_${mode.name.lowercase()}"),
                                color = when {
                                    isSelected -> StudioViolet
                                    !isSupported -> NavySurface.copy(alpha = 0.4f)
                                    else -> NavyElevated
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSupported) TextPrimary else TextMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!isSupported) {
                                        Text("N/A", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Provider & Auto Mode Selector
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Provider Routing", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = if (formState.autoMode) {
                                    activeProviderConfig?.let { "Auto -> ${it.type.displayName}" }
                                        ?: "No video provider connected yet"
                                } else {
                                    formState.selectedProvider.displayName
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasAnyConfiguredProvider) StudioCyan else StatusError
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = formState.autoMode,
                                onCheckedChange = onAutoModeChange,
                                modifier = Modifier.testTag("gen_auto_mode_switch")
                            )
                        }
                    }

                    if (!formState.autoMode) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            providers.forEach { cfg ->
                                FilterChip(
                                    selected = formState.selectedProvider == cfg.type,
                                    onClick = { onProviderSelect(cfg.type) },
                                    label = { Text(cfg.type.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioViolet,
                                        selectedLabelColor = TextPrimary,
                                        containerColor = NavyElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProviderStatusChip(status = activeProviderConfig?.status ?: ConnectionStatus.NOT_CONFIGURED)
                            TextButton(onClick = onOpenApiManager) {
                                Icon(Icons.Filled.Api, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Configure Key", color = StudioCyan)
                            }
                        }
                    } else if (!hasAnyConfiguredProvider) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenApiManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gen_connect_api_cta"),
                            border = BorderStroke(1.dp, StudioCyan)
                        ) {
                            Icon(Icons.Filled.Api, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open API Manager to Connect Video Provider", color = StudioCyan)
                        }
                    }
                }
            }

            // Image or Video Input Card (when in Image-to-Video or Video-to-Video)
            if (formState.mode != GenerationMode.TEXT_TO_VIDEO) {
                item {
                    GlassCard {
                        Text(
                            text = if (formState.mode == GenerationMode.IMAGE_TO_VIDEO) "Reference Image" else "Source Video Clip",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        if (!formState.inputMediaUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NavyElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (formState.mode == GenerationMode.IMAGE_TO_VIDEO) {
                                    AsyncImage(
                                        model = Uri.parse(formState.inputMediaUri),
                                        contentDescription = "Selected reference image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(40.dp))
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Video selected: ${Uri.parse(formState.inputMediaUri).lastPathSegment ?: "clip"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (formState.mode == GenerationMode.IMAGE_TO_VIDEO) {
                                OutlinedButton(
                                    onClick = {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("gen_pick_image_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Gallery")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                            PackageManager.PERMISSION_GRANTED
                                        if (granted) launchCameraCapture() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("gen_camera_button")
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Camera")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("gen_pick_video_button")
                                ) {
                                    Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pick Video from Gallery")
                                }
                            }
                        }

                        if (!cameraPermissionMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(cameraPermissionMessage!!, style = MaterialTheme.typography.bodySmall, color = StatusError)
                        }
                    }
                }
            }

            // Prompt & Enhancement Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (formState.mode) {
                                GenerationMode.TEXT_TO_VIDEO -> "Video Prompt"
                                GenerationMode.IMAGE_TO_VIDEO -> "Animation Prompt"
                                GenerationMode.VIDEO_TO_VIDEO -> "Transformation Instructions"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = onEnhancePrompt,
                            enabled = !formState.isEnhancingPrompt,
                            modifier = Modifier.testTag("gen_enhance_prompt_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (formState.isEnhancingPrompt) "Enhancing..." else "AI Enhance",
                                color = StudioCyan
                            )
                        }
                    }

                    OutlinedTextField(
                        value = formState.prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("gen_prompt_input"),
                        placeholder = {
                            Text(
                                "Describe camera movement, lighting, subject, and 9:16 framing...",
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (formState.prompt.startsWith("[Sample Preview")) {
                        Spacer(Modifier.height(8.dp))
                        FirebaseNoticeBanner()
                    }

                    val showNegative = activeProviderConfig?.type?.supportsNegativePrompt ?: true
                    if (showNegative && formState.mode == GenerationMode.TEXT_TO_VIDEO) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = formState.negativePrompt,
                            onValueChange = onNegativePromptChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gen_negative_prompt_input"),
                            label = { Text("Negative Prompt (where supported)") },
                            placeholder = { Text("blurry, watermark, distorted hands", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioViolet,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Mobile Format Controls (Aspect Ratio 9:16 prioritized, Duration, Resolution, Model, Motion)
            item {
                GlassCard {
                    Text("Aspect Ratio (Mobile First)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = formState.aspectRatio == ratio,
                                onClick = { onAspectRatioSelect(ratio) },
                                label = {
                                    Text(if (ratio.isRecommendedMobile) "${ratio.code} ★" else ratio.code)
                                },
                                modifier = Modifier.testTag("gen_aspect_${ratio.code}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioViolet,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = NavyElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Duration & Resolution", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 8, 10).forEach { sec ->
                            FilterChip(
                                selected = formState.durationSec == sec,
                                onClick = { onDurationSelect(sec) },
                                label = { Text("${sec}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioViolet,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = NavyElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                        listOf(VideoResolution.HD_720, VideoResolution.FHD_1080).forEach { res ->
                            FilterChip(
                                selected = formState.resolution == res,
                                onClick = { onResolutionSelect(res) },
                                label = { Text(res.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = StudioCyan,
                                    containerColor = NavyElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    if (formState.mode == GenerationMode.IMAGE_TO_VIDEO) {
                        Spacer(Modifier.height(12.dp))
                        Text("Camera Motion Intensity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MotionIntensity.entries.forEach { motion ->
                                FilterChip(
                                    selected = formState.motionIntensity == motion,
                                    onClick = { onMotionSelect(motion) },
                                    label = { Text(motion.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioViolet,
                                        selectedLabelColor = TextPrimary,
                                        containerColor = NavyElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    val models = activeProviderConfig?.type?.availableModels ?: formState.selectedProvider.availableModels
                    Spacer(Modifier.height(12.dp))
                    Text("Model", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        models.forEach { model ->
                            FilterChip(
                                selected = formState.selectedModel == model,
                                onClick = { onModelSelect(model) },
                                label = { Text(model, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioViolet,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = NavyElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Error / Status Banner & Generate CTA
            item {
                if (!formState.errorBanner.isNullOrBlank()) {
                    Surface(
                        color = StatusError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(formState.errorBanner, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            if (!hasAnyConfiguredProvider) {
                                TextButton(
                                    onClick = onOpenApiManager,
                                    colors = ButtonDefaults.textButtonColors(contentColor = StudioCyan)
                                ) {
                                    Text("Open API Manager ->")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (!formState.statusMessage.isNullOrBlank()) {
                    Text(
                        text = formState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSuccess,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                GradientPrimaryButton(
                    text = if (formState.isSubmitting) "Submitting Job..." else "Generate Video",
                    icon = Icons.Filled.MovieCreation,
                    onClick = onSubmitGeneration,
                    isLoading = formState.isSubmitting,
                    testTag = "gen_submit_button"
                )
            }

            // Tracked Jobs List
            if (jobs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    SectionHeader(title = "Generation Jobs (${jobs.size})")
                }
                items(jobs, key = { it.id }) { job ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${job.providerId.uppercase()} • ${job.model}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyan
                            )
                            Text(
                                text = job.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (job.status) {
                                    "COMPLETED" -> StatusSuccess
                                    "FAILED" -> StatusError
                                    else -> StatusInfo
                                }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = job.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (job.status !in listOf("COMPLETED", "FAILED", "CANCELLED")) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (job.progressPercent / 100f).coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = StudioCyan,
                                trackColor = NavyElevated
                            )
                        }
                        if (!job.errorMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(job.errorMessage, style = MaterialTheme.typography.bodySmall, color = StatusError)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (job.status == "FAILED") {
                                TextButton(onClick = { onRetryJob(job.id) }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Retry", color = StudioCyan)
                                }
                            }
                            if (!job.outputVideoPath.isNullOrBlank()) {
                                TextButton(onClick = {
                                    onPlayVideo(File(job.outputVideoPath).toURI().toString(), job.prompt.take(28), job.outputVideoPath)
                                }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Watch MP4", color = StudioVioletLight)
                                }
                            }
                            IconButton(onClick = { onDeleteJob(job.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove job", tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
