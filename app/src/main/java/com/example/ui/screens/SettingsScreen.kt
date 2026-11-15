package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.StudioSettingsState
import com.example.data.repository.StorageStats
import com.example.domain.model.AspectRatio
import com.example.domain.model.ProviderConfig
import com.example.domain.model.VideoResolution
import com.example.ui.components.GlassCard
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: StudioSettingsState,
    providers: List<ProviderConfig>,
    storageStats: StorageStats,
    onOpenApiManager: () -> Unit,
    onUpdateCreatorName: (String) -> Unit,
    onAutoFallbackChange: (Boolean) -> Unit,
    onDefaultAspectChange: (String) -> Unit,
    onDefaultResolutionChange: (VideoResolution) -> Unit,
    onJobNotifChange: (Boolean) -> Unit,
    onApiNotifChange: (Boolean) -> Unit,
    onThemeAccentChange: (String) -> Unit,
    onClearCache: () -> Unit
) {
    val configuredCount = providers.count { it.hasApiKey }
    var nameDraft by remember(settings.creatorName) { mutableStateOf(settings.creatorName) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Creator Account / Studio Profile
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Creator Profile", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            label = { Text("Studio Display Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { onUpdateCreatorName(nameDraft) }) {
                            Text("Save")
                        }
                    }
                }
            }

            // API Manager Entry
            item {
                GlassCard(
                    highlighted = true,
                    onClick = onOpenApiManager,
                    modifier = Modifier.testTag("settings_api_manager_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Api, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Multi-AI API Manager", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "$configuredCount of ${providers.size} providers configured • Runway, Luma, Veo, Pika",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextPrimary)
                    }
                }
            }

            // Generation Preferences
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = StudioVioletLight, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generation Preferences", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Fallback Provider", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Try next compatible configured API if primary fails", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(checked = settings.autoFallbackEnabled, onCheckedChange = onAutoFallbackChange)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Default Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AspectRatio.PORTRAIT_9_16, AspectRatio.LANDSCAPE_16_9, AspectRatio.SQUARE_1_1).forEach { r ->
                            FilterChip(
                                selected = settings.defaultAspectRatioCode == r.code,
                                onClick = { onDefaultAspectChange(r.code) },
                                label = { Text(r.code) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Default Export Quality", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(VideoResolution.HD_720, VideoResolution.FHD_1080).forEach { res ->
                            FilterChip(
                                selected = settings.defaultResolution == res.name,
                                onClick = { onDefaultResolutionChange(res) },
                                label = { Text(res.label) }
                            )
                        }
                    }
                }
            }

            // Storage Management
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Storage Management", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Saved Projects: ${storageStats.projectsCount} • Videos/Exports: ${formatBytes(storageStats.videosBytes)} • Temp Cache: ${formatBytes(storageStats.cacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Temporary Thumbnails & Cache")
                    }
                }
            }

            // Notifications & Theme Accent
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = StudioVioletLight, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Video generation job alerts", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(checked = settings.jobNotificationsEnabled, onCheckedChange = onJobNotifChange)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("API failure & quota alerts", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(checked = settings.apiAlertNotificationsEnabled, onCheckedChange = onApiNotifChange)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Studio Accent Theme", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("violet_blue" to "Violet", "electric_cyan" to "Cyan", "neon_magenta" to "Magenta").forEach { (id, label) ->
                            FilterChip(
                                selected = settings.themeAccent == id,
                                onClick = { onThemeAccentChange(id) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Help & Support
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Help & Architecture", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "• All API keys are encrypted locally using Android Keystore AES-256-GCM.\n" +
                            "• On-device editing and MP4 rendering run natively via Jetpack Media3 Transformer.\n" +
                            "• Built-in cloud Gemini tools require enabling Firebase AI Logic; external video generation requires your Runway, Luma AI, Veo, Pika, or custom API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
