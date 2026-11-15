package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.ProviderConfig
import com.example.domain.model.VideoProviderType
import com.example.ui.components.GlassCard
import com.example.ui.components.ProviderStatusChip
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApiManagerScreen(
    providers: List<ProviderConfig>,
    onSaveProvider: (VideoProviderType, String?, String, String) -> Unit,
    onTestConnection: (VideoProviderType) -> Unit,
    onRemoveKey: (VideoProviderType) -> Unit,
    onSetDefault: (VideoProviderType) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Gemini vs Veo Capability Separation Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusInfo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini Text AI vs. Video Generation APIs", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Google Gemini (scripts, prompt enhancement, captions) and Google Veo / Runway / Luma / Pika (video rendering) are distinct APIs. A Gemini key does not enable video rendering. API keys entered below are encrypted locally in Android Keystore and never fabricated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(providers, key = { it.type.id }) { cfg ->
                ProviderConfigCard(
                    config = cfg,
                    onSave = { key, url, model -> onSaveProvider(cfg.type, key, url, model) },
                    onTest = { onTestConnection(cfg.type) },
                    onRemoveKey = { onRemoveKey(cfg.type) },
                    onSetDefault = { onSetDefault(cfg.type) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProviderConfigCard(
    config: ProviderConfig,
    onSave: (String?, String, String) -> Unit,
    onTest: () -> Unit,
    onRemoveKey: () -> Unit,
    onSetDefault: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var apiKeyInput by remember(config.type) { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var baseUrlInput by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var modelInput by remember(config.selectedModel) { mutableStateOf(config.selectedModel) }

    GlassCard(highlighted = config.isDefault) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (config.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = StudioViolet.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioVioletLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(config.type.tagline, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Modes: ${config.type.supportedModes.joinToString { it.shortLabel }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioCyan
                )
            }

            ProviderStatusChip(status = config.status)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (config.hasApiKey) {
                "Key: ${config.maskedKeyPreview} • ${config.statusDetail}"
            } else {
                config.statusDetail
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (config.status) {
                ConnectionStatus.CONNECTED -> StatusSuccess
                ConnectionStatus.AUTH_ERROR -> StatusError
                ConnectionStatus.UNREACHABLE -> StatusWarning
                else -> TextSecondary
            }
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("api_edit_${config.type.id}")
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "Hide" else if (config.hasApiKey) "Update Key" else "Add Key")
            }

            Button(
                onClick = onTest,
                enabled = config.hasApiKey && config.status != ConnectionStatus.TESTING,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyElevated,
                    contentColor = StudioCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("api_test_${config.type.id}")
            ) {
                Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Test API")
            }
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text(if (config.hasApiKey) "New API Key / Token (leave blank to keep)" else "Enter API Key / Bearer Token") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle key visibility"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioViolet,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input_${config.type.id}")
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base Endpoint URL") },
                placeholder = { Text("https://...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioViolet,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Default Model ID") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioViolet,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onSave(apiKeyInput.takeIf { it.isNotBlank() }, baseUrlInput, modelInput)
                        apiKeyInput = ""
                        expanded = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioViolet),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("api_save_${config.type.id}")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }

                if (!config.isDefault) {
                    OutlinedButton(onClick = onSetDefault) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Default")
                    }
                }

                if (config.hasApiKey) {
                    IconButton(onClick = onRemoveKey) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove key", tint = StatusError)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { showGuide = !showGuide }) {
            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showGuide) "Hide Setup Guide" else "Provider Setup Instructions", color = StudioCyan)
        }

        if (showGuide) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                config.type.setupSteps.forEachIndexed { idx, step ->
                    Text(
                        text = "${idx + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
