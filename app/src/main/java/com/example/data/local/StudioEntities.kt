package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceType: String, // "generated" | "edited"
    val aspectRatioCode: String,
    val durationMs: Long,
    val videoPath: String?,
    val thumbnailPath: String?,
    val timelineJson: String,
    val promptUsed: String,
    val providerId: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "generation_jobs")
data class GenerationJobEntity(
    @PrimaryKey val id: String,
    val remoteTaskId: String?,
    val providerId: String,
    val model: String,
    val mode: String,
    val prompt: String,
    val negativePrompt: String,
    val aspectRatioCode: String,
    val durationSec: Int,
    val resolution: String,
    val inputMediaUri: String?,
    val status: String,
    val progressPercent: Int,
    val outputVideoPath: String?,
    val remoteVideoUrl: String?,
    val errorMessage: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "studio_notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String, // "job" | "api" | "export" | "info"
    val isRead: Boolean = false,
    val createdAtMs: Long
)
