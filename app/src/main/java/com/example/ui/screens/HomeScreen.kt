package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.GenerationJobEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StudioSettingsState
import com.example.domain.model.AiToolType
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.GenerationMode
import com.example.domain.model.ProviderConfig
import com.example.domain.model.StudioTemplate
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.NavyAbyss
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioElectricBlue
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

private data class ShortcutItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    settings: StudioSettingsState,
    providers: List<ProviderConfig>,
    projects: List<ProjectEntity>,
    jobs: List<GenerationJobEntity>,
    templates: List<StudioTemplate>,
    onNavigateToGenerate: (GenerationMode) -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToApiManager: () -> Unit,
    onNavigateToAiTool: (AiToolType) -> Unit,
    onApplyTemplate: (StudioTemplate) -> Unit,
    onOpenProjectInEditor: (ProjectEntity) -> Unit,
    onPlayVideo: (String, String, String?) -> Unit
) {
    val connectedCount = providers.count { it.status == ConnectionStatus.CONNECTED || it.hasApiKey }
    val shortcuts = listOf(
        ShortcutItem("Text to Video", "Prompt to 9:16 reel", Icons.Filled.TextFields, StudioVioletLight, "shortcut_text_to_video") {
            onNavigateToGenerate(GenerationMode.TEXT_TO_VIDEO)
        },
        ShortcutItem("Image to Video", "Animate photo/camera", Icons.Filled.Image, StudioCyan, "shortcut_image_to_video") {
            onNavigateToGenerate(GenerationMode.IMAGE_TO_VIDEO)
        },
        ShortcutItem("AI Video Editor", "Trim, split & export", Icons.Filled.ContentCut, StudioElectricBlue, "shortcut_ai_editor") {
            onNavigateToEditor()
        },
        ShortcutItem("AI Script Generator", "Hooks & shot lists", Icons.Filled.Description, StudioViolet, "shortcut_script_gen") {
            onNavigateToAiTool(AiToolType.SCRIPT_WRITER)
        },
        ShortcutItem("AI Voice Generator", "On-device TTS & WAV", Icons.Filled.RecordVoiceOver, StudioCyan, "shortcut_voice_gen") {
            onNavigateToAiTool(AiToolType.TEXT_TO_SPEECH)
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Hero Card
            item {
                GlassCard(
                    highlighted = true,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(206.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero),
                            contentDescription = "AI Video Studio atmosphere",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            NavyAbyss.copy(alpha = 0.25f),
                                            NavyAbyss.copy(alpha = 0.78f),
                                            NavyAbyss.copy(alpha = 0.96f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = StudioViolet.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Welcome, ${settings.creatorName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onNavigateToApiManager)
                                        .testTag("home_api_status_pill"),
                                    color = NavyElevated.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Api,
                                            contentDescription = null,
                                            tint = if (connectedCount > 0) StatusSuccess else StudioCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (connectedCount > 0) "$connectedCount API Ready" else "Connect API",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "Create Mobile Reels with AI",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Generate 9:16 clips via your connected APIs & cut on touch timeline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                GradientPrimaryButton(
                                    text = "Quick Video Generate",
                                    icon = Icons.Filled.AutoAwesome,
                                    onClick = { onNavigateToGenerate(GenerationMode.TEXT_TO_VIDEO) },
                                    testTag = "home_quick_generate_button"
                                )
                            }
                        }
                    }
                }
            }

            // 5 Mobile Studio Shortcut Cards
            item {
                SectionHeader(title = "Studio Shortcuts")
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shortcuts) { item ->
                        GlassCard(
                            modifier = Modifier
                                .width(148.dp)
                                .testTag(item.tag),
                            onClick = item.onClick,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Popular AI Tools Strip
            item {
                SectionHeader(
                    title = "Popular AI Tools",
                    actionLabel = "All 10 Tools",
                    onAction = { onNavigateToAiTool(AiToolType.AUTO_EDIT) }
                )
                Spacer(Modifier.height(8.dp))
                val featuredTools = listOf(
                    AiToolType.AUTO_EDIT to Icons.Filled.AutoAwesome,
                    AiToolType.CAPTION_GENERATOR to Icons.Filled.TextFields,
                    AiToolType.AUTO_SUBTITLES to Icons.Filled.Subtitles,
                    AiToolType.AI_VOICEOVER to Icons.Filled.GraphicEq,
                    AiToolType.SCENE_GENERATOR to Icons.Filled.VideoCameraBack
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(featuredTools) { (tool, icon) ->
                        GlassCard(
                            modifier = Modifier
                                .width(172.dp)
                                .testTag("home_tool_${tool.name.lowercase()}"),
                            onClick = { onNavigateToAiTool(tool) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = tool.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 9:16 Video Templates
            item {
                SectionHeader(title = "9:16 Creator Templates")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    templates.forEach { tpl ->
                        val drawableId = if (tpl.drawableResName == "img_template_cyberpunk") {
                            R.drawable.img_template_cyberpunk
                        } else {
                            R.drawable.img_template_product
                        }
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("template_${tpl.id}"),
                            onClick = { onApplyTemplate(tpl) },
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = drawableId),
                                    contentDescription = tpl.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, NavyAbyss.copy(alpha = 0.85f))
                                            )
                                        )
                                )
                                Surface(
                                    color = StudioViolet.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${tpl.aspectRatio.code} • ${tpl.durationSec}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = tpl.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Use prompt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Projects
            item {
                SectionHeader(
                    title = "Recent Projects",
                    actionLabel = if (projects.isNotEmpty()) "View All" else null,
                    onAction = onNavigateToProjects
                )
                Spacer(Modifier.height(8.dp))
                if (projects.isEmpty()) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = StudioVioletLight, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No saved projects yet", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    "Generate a video or import clips in the Editor to start building reels.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(projects.take(6), key = { it.id }) { proj ->
                            GlassCard(
                                modifier = Modifier.width(180.dp),
                                onClick = { onOpenProjectInEditor(proj) },
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NavyElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!proj.thumbnailPath.isNullOrBlank()) {
                                        AsyncImage(
                                            model = File(proj.thumbnailPath),
                                            contentDescription = proj.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Filled.Movie, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(32.dp))
                                    }
                                    if (!proj.videoPath.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .clickable {
                                                    onPlayVideo(File(proj.videoPath).toURI().toString(), proj.title, proj.videoPath)
                                                }
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = TextPrimary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = proj.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${proj.aspectRatioCode} • ${formatDurationMs(proj.durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Recent Generation History
            item {
                SectionHeader(title = "Generation History")
                Spacer(Modifier.height(8.dp))
                if (jobs.isEmpty()) {
                    GlassCard {
                        Text(
                            text = "No generation jobs submitted yet. Connect a provider in API Manager and tap Generate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        jobs.take(4).forEach { job ->
                            val statusColor = when (job.status) {
                                "COMPLETED" -> StatusSuccess
                                "FAILED" -> StatusError
                                else -> StatusInfo
                            }
                            GlassCard(contentPadding = PaddingValues(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = job.prompt.ifBlank { "Video job" },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${job.providerId.uppercase()} • ${job.aspectRatioCode} • ${job.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                    }
                                    if (!job.outputVideoPath.isNullOrBlank()) {
                                        IconButton(onClick = {
                                            onPlayVideo(File(job.outputVideoPath).toURI().toString(), job.prompt.take(24), job.outputVideoPath)
                                        }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play generated video", tint = StudioCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = StudioCyan)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}
