package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiToolType
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiToolsUiState

@Composable
fun AiToolsScreen(
    state: AiToolsUiState,
    ttsReady: Boolean,
    ttsSpeaking: Boolean,
    onSelectTool: (AiToolType) -> Unit,
    onInputPromptChange: (String) -> Unit,
    onRunTool: () -> Unit,
    onTtsParamsChange: (Float, Float) -> Unit,
    onSpeakPreview: () -> Unit,
    onStopTts: () -> Unit,
    onSynthesizeWavToEditor: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FirebaseNoticeBanner()
            }

            item {
                Text("Select AI Smart Tool", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiToolType.entries.forEach { tool ->
                        FilterChip(
                            selected = state.selectedTool == tool,
                            onClick = { onSelectTool(tool) },
                            label = { Text(tool.title) },
                            modifier = Modifier.testTag("ai_tool_chip_${tool.name.lowercase()}"),
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

            item {
                GlassCard {
                    Text(state.selectedTool.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(state.selectedTool.subtitle, style = MaterialTheme.typography.bodySmall, color = StudioCyan)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.inputPrompt,
                        onValueChange = onInputPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("ai_tool_prompt_input"),
                        placeholder = {
                            Text(
                                text = if (state.selectedTool == AiToolType.TEXT_TO_SPEECH) {
                                    "Enter narration script to speak or export as WAV..."
                                } else {
                                    "Describe your video topic, product, or scene idea..."
                                },
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

                    Spacer(Modifier.height(12.dp))
                    GradientPrimaryButton(
                        text = if (state.isRunning) "Processing..." else "Run ${state.selectedTool.title}",
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onRunTool,
                        isLoading = state.isRunning,
                        testTag = "ai_tool_run_button"
                    )
                }
            }

            if (state.selectedTool.supportsOnDevicePreview) {
                item {
                    GlassCard {
                        Text("On-Device Voice Studio (Android TTS)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (ttsReady) "Android Speech Engine ready" else "Initializing Android TTS...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ttsReady) StatusSuccess else TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Pitch: ${"%.1f".format(state.ttsPitch)}x", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = state.ttsPitch,
                            onValueChange = { onTtsParamsChange(it, state.ttsRate) },
                            valueRange = 0.6f..1.6f
                        )
                        Text("Speed: ${"%.1f".format(state.ttsRate)}x", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = state.ttsRate,
                            onValueChange = { onTtsParamsChange(state.ttsPitch, it) },
                            valueRange = 0.6f..1.6f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = if (ttsSpeaking) onStopTts else onSpeakPreview,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (ttsSpeaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (ttsSpeaking) "Stop" else "Speak Preview")
                            }
                            Button(
                                onClick = onSynthesizeWavToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioViolet),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add WAV")
                            }
                        }
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Text(err, style = MaterialTheme.typography.bodySmall, color = StatusError)
                }
            }

            state.result?.let { result ->
                item {
                    GlassCard(highlighted = true) {
                        Text(result.title, style = MaterialTheme.typography.titleMedium, color = StudioCyan)
                        Spacer(Modifier.height(8.dp))
                        Text(result.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
