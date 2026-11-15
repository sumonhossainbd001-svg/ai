package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE projects SET title = :title, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun rename(id: String, title: String, updatedAtMs: Long)
}

@Dao
interface GenerationJobDao {
    @Query("SELECT * FROM generation_jobs ORDER BY createdAtMs DESC")
    fun observeJobs(): Flow<List<GenerationJobEntity>>

    @Query("SELECT * FROM generation_jobs WHERE status IN ('QUEUED', 'SUBMITTING', 'PROCESSING', 'DOWNLOADING')")
    suspend fun getActiveJobs(): List<GenerationJobEntity>

    @Query("SELECT * FROM generation_jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GenerationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: GenerationJobEntity)

    @Query("DELETE FROM generation_jobs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM studio_notifications ORDER BY createdAtMs DESC LIMIT 50")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE studio_notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM studio_notifications")
    suspend fun clearAll()
}
