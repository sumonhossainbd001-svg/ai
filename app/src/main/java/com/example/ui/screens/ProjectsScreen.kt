package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ProjectEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.StatusError
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    onOpenInEditor: (ProjectEntity) -> Unit,
    onPlayVideo: (String, String, String?) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSaveToGallery: (String, String) -> Unit,
    onShareVideo: (String, String) -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    var renamingProject by remember { mutableStateOf<ProjectEntity?>(null) }

    val filtered = remember(projects, filter) {
        when (filter) {
            "generated" -> projects.filter { it.sourceType == "generated" }
            "edited" -> projects.filter { it.sourceType == "edited" }
            else -> projects
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All (${projects.size})", "generated" to "AI Generated", "edited" to "Edited").forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { filter = key },
                            label = { Text(label) },
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

            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = StudioVioletLight, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No projects in this view", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Generated videos and saved Editor timelines will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { proj ->
                    var menuOpen by remember { mutableStateOf(false) }
                    GlassCard(
                        modifier = Modifier.testTag("project_card_${proj.id}"),
                        onClick = { onOpenInEditor(proj) },
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 11f)
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
                                Icon(Icons.Filled.Movie, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(34.dp))
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = proj.aspectRatioCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (!proj.videoPath.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = StudioViolet.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .clickable {
                                            onPlayVideo(File(proj.videoPath).toURI().toString(), proj.title, proj.videoPath)
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = TextPrimary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${proj.sourceType.uppercase()} • ${formatDurationMs(proj.durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioCyan
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { menuOpen = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Project options", tint = TextSecondary)
                                }
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open in Editor") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            onOpenInEditor(proj)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            renamingProject = proj
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            onDuplicateProject(proj.id)
                                        }
                                    )
                                    if (!proj.videoPath.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text("Save MP4 to Gallery") },
                                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                            onClick = {
                                                menuOpen = false
                                                onSaveToGallery(proj.videoPath, proj.title)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                            onClick = {
                                                menuOpen = false
                                                onShareVideo(proj.videoPath, proj.title)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = StatusError) },
                                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = StatusError) },
                                        onClick = {
                                            menuOpen = false
                                            onDeleteProject(proj.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProject?.let { proj ->
        var newName by remember(proj.id) { mutableStateOf(proj.title) }
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            containerColor = NavySurface,
            title = { Text("Rename Project", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameProject(proj.id, newName)
                    renamingProject = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("Cancel") }
            }
        )
    }
}
