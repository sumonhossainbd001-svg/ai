package com.example.data.repository

import android.content.Context
import com.example.data.local.GenerationJobEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioPreferences
import com.example.data.local.StudioSettingsState
import com.example.data.network.ConnectionTestResult
import com.example.data.network.GenerationSubmitRequest
import com.example.data.network.ProviderAdapters
import com.example.data.security.SecureKeyVault
import com.example.domain.model.AspectRatio
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.GenerationMode
import com.example.domain.model.JobStatus
import com.example.domain.model.MotionIntensity
import com.example.domain.model.ProviderConfig
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineState
import com.example.domain.model.VideoProviderType
import com.example.domain.model.VideoResolution
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class StorageStats(
    val projectsCount: Int = 0,
    val videosBytes: Long = 0L,
    val cacheBytes: Long = 0L
)

class StudioRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = StudioDatabase.getInstance(appContext)
    private val projectDao = db.projectDao()
    private val jobDao = db.generationJobDao()
    private val notificationDao = db.notificationDao()

    val preferences = StudioPreferences(appContext)
    private val keyVault = SecureKeyVault(appContext)
    private val adapters = ProviderAdapters(appContext)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val timelineAdapter = moshi.adapter(TimelineState::class.java)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activePollingJobs = mutableMapOf<String, Job>()

    private val _providerConfigs = MutableStateFlow<List<ProviderConfig>>(emptyList())
    val providerConfigs: StateFlow<List<ProviderConfig>> = _providerConfigs.asStateFlow()

    val projectsFlow: Flow<List<ProjectEntity>> = projectDao.observeProjects()
    val jobsFlow: Flow<List<GenerationJobEntity>> = jobDao.observeJobs()
    val notificationsFlow: Flow<List<NotificationEntity>> = notificationDao.observeNotifications()
    val settingsFlow: Flow<StudioSettingsState> = preferences.settingsFlow

    init {
        repoScope.launch {
            refreshProviderConfigs()
            resumePendingJobs()
        }
    }

    suspend fun refreshProviderConfigs() {
        val settings = preferences.settingsFlow.first()
        val existingStatuses = _providerConfigs.value.associateBy { it.type }
        val updated = VideoProviderType.entries.map { type ->
            val hasKey = keyVault.hasApiKey(type)
            val prev = existingStatuses[type]
            val status = when {
                !hasKey -> ConnectionStatus.NOT_CONFIGURED
                prev != null && prev.status != ConnectionStatus.NOT_CONFIGURED -> prev.status
                else -> ConnectionStatus.NOT_CONFIGURED
            }
            val detail = when {
                !hasKey -> "No API key saved"
                prev != null && prev.status != ConnectionStatus.NOT_CONFIGURED -> prev.statusDetail
                else -> "Key saved securely — tap Test Connection to verify"
            }
            ProviderConfig(
                type = type,
                hasApiKey = hasKey,
                maskedKeyPreview = keyVault.getMaskedPreview(type),
                baseUrl = keyVault.getProviderBaseUrl(type),
                selectedModel = keyVault.getProviderModel(type),
                status = status,
                statusDetail = detail,
                lastLatencyMs = prev?.lastLatencyMs,
                isDefault = settings.defaultProviderId == type.id
            )
        }
        _providerConfigs.value = updated
    }

    suspend fun saveProviderConfiguration(
        provider: VideoProviderType,
        apiKey: String?,
        baseUrl: String,
        model: String
    ) {
        keyVault.saveProviderEndpoint(provider, baseUrl, model)
        if (!apiKey.isNullOrBlank()) {
            keyVault.saveApiKey(provider, apiKey)
        }
        refreshProviderConfigs()
        addNotification(
            title = "${provider.displayName} Updated",
            message = "Configuration saved in Android Keystore. Run Test Connection to verify.",
            category = "api"
        )
    }

    suspend fun removeProviderKey(provider: VideoProviderType) {
        keyVault.removeApiKey(provider)
        refreshProviderConfigs()
        addNotification(
            title = "${provider.displayName} Key Removed",
            message = "API key deleted from device Keystore.",
            category = "api"
        )
    }

    suspend fun setDefaultProvider(provider: VideoProviderType) {
        preferences.setDefaultProvider(provider.id)
        refreshProviderConfigs()
    }

    suspend fun testProviderConnection(provider: VideoProviderType): ConnectionTestResult {
        updateProviderStatus(provider, ConnectionStatus.TESTING, "Testing endpoint...", null)
        val key = keyVault.getApiKey(provider).orEmpty()
        val url = keyVault.getProviderBaseUrl(provider)
        val result = adapters.testConnection(provider, key, url)
        updateProviderStatus(provider, result.status, result.detail, result.latencyMs)
        if (result.status != ConnectionStatus.CONNECTED) {
            addNotification(
                title = "${provider.displayName} Check: ${result.status.label}",
                message = result.detail,
                category = "api"
            )
        }
        return result
    }

    private fun updateProviderStatus(
        provider: VideoProviderType,
        status: ConnectionStatus,
        detail: String,
        latencyMs: Long?
    ) {
        _providerConfigs.value = _providerConfigs.value.map { cfg ->
            if (cfg.type == provider) {
                cfg.copy(status = status, statusDetail = detail, lastLatencyMs = latencyMs)
            } else cfg
        }
    }

    fun selectAutoProvider(mode: GenerationMode): VideoProviderType? {
        val configs = _providerConfigs.value
        val compatible = configs.filter { it.hasApiKey && it.type.supportedModes.contains(mode) }
        return compatible.firstOrNull { it.isDefault && it.status == ConnectionStatus.CONNECTED }?.type
            ?: compatible.firstOrNull { it.status == ConnectionStatus.CONNECTED }?.type
            ?: compatible.firstOrNull { it.isDefault }?.type
            ?: compatible.firstOrNull()?.type
    }

    suspend fun submitVideoGeneration(
        requestedProvider: VideoProviderType?,
        autoMode: Boolean,
        mode: GenerationMode,
        prompt: String,
        negativePrompt: String,
        aspectRatio: AspectRatio,
        durationSec: Int,
        resolution: VideoResolution,
        motionIntensity: MotionIntensity,
        selectedModel: String?,
        inputMediaUri: String?
    ): Result<String> {
        refreshProviderConfigs()
        val settings = preferences.settingsFlow.first()

        val candidates = if (autoMode || requestedProvider == null) {
            _providerConfigs.value
                .filter { it.hasApiKey && it.type.supportedModes.contains(mode) }
                .sortedWith(
                    compareByDescending<ProviderConfig> { it.status == ConnectionStatus.CONNECTED }
                        .thenByDescending { it.isDefault }
                )
                .map { it.type }
        } else {
            val primary = listOf(requestedProvider)
            val fallback = if (settings.autoFallbackEnabled) {
                _providerConfigs.value
                    .filter { it.hasApiKey && it.type != requestedProvider && it.type.supportedModes.contains(mode) }
                    .map { it.type }
            } else emptyList()
            primary + fallback
        }

        if (candidates.isEmpty()) {
            return Result.failure(
                IllegalStateException("No configured provider supports ${mode.label}. Open API Manager to connect Runway, Luma AI, Veo, or a custom endpoint.")
            )
        }

        val jobId = "job_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        var chosenProvider = candidates.first()
        var chosenModel = selectedModel?.takeIf { it.isNotBlank() } ?: keyVault.getProviderModel(chosenProvider)

        var lastError: Throwable? = null
        var remoteTaskId: String? = null

        for (provider in candidates) {
            val apiKey = keyVault.getApiKey(provider)
            if (apiKey.isNullOrBlank()) {
                lastError = IllegalStateException("${provider.displayName} has no saved API key.")
                continue
            }
            chosenProvider = provider
            chosenModel = if (provider == requestedProvider && !selectedModel.isNullOrBlank()) {
                selectedModel
            } else {
                keyVault.getProviderModel(provider)
            }

            val draftJob = GenerationJobEntity(
                id = jobId,
                remoteTaskId = null,
                providerId = provider.id,
                model = chosenModel,
                mode = mode.name,
                prompt = prompt,
                negativePrompt = negativePrompt,
                aspectRatioCode = aspectRatio.code,
                durationSec = durationSec,
                resolution = resolution.label,
                inputMediaUri = inputMediaUri,
                status = JobStatus.SUBMITTING.name,
                progressPercent = 10,
                outputVideoPath = null,
                remoteVideoUrl = null,
                errorMessage = null,
                createdAtMs = now,
                updatedAtMs = now
            )
            jobDao.upsert(draftJob)

            val res = adapters.submitGeneration(
                provider = provider,
                apiKey = apiKey,
                baseUrl = keyVault.getProviderBaseUrl(provider),
                request = GenerationSubmitRequest(
                    mode = mode,
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    aspectRatio = aspectRatio,
                    durationSec = durationSec,
                    resolution = resolution,
                    motionIntensity = motionIntensity,
                    model = chosenModel,
                    inputMediaUri = inputMediaUri
                )
            )
            if (res.isSuccess) {
                remoteTaskId = res.getOrThrow().remoteTaskId
                break
            } else {
                lastError = res.exceptionOrNull()
            }
        }

        if (remoteTaskId == null) {
            val errMsg = lastError?.localizedMessage ?: "Generation request rejected by provider."
            jobDao.upsert(
                GenerationJobEntity(
                    id = jobId,
                    remoteTaskId = null,
                    providerId = chosenProvider.id,
                    model = chosenModel,
                    mode = mode.name,
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    aspectRatioCode = aspectRatio.code,
                    durationSec = durationSec,
                    resolution = resolution.label,
                    inputMediaUri = inputMediaUri,
                    status = JobStatus.FAILED.name,
                    progressPercent = 0,
                    outputVideoPath = null,
                    remoteVideoUrl = null,
                    errorMessage = errMsg,
                    createdAtMs = now,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            addNotification("Generation Failed (${chosenProvider.displayName})", errMsg, "job")
            return Result.failure(IllegalStateException(errMsg))
        }

        val activeJob = GenerationJobEntity(
            id = jobId,
            remoteTaskId = remoteTaskId,
            providerId = chosenProvider.id,
            model = chosenModel,
            mode = mode.name,
            prompt = prompt,
            negativePrompt = negativePrompt,
            aspectRatioCode = aspectRatio.code,
            durationSec = durationSec,
            resolution = resolution.label,
            inputMediaUri = inputMediaUri,
            status = JobStatus.PROCESSING.name,
            progressPercent = 20,
            outputVideoPath = null,
            remoteVideoUrl = null,
            errorMessage = null,
            createdAtMs = now,
            updatedAtMs = System.currentTimeMillis()
        )
        jobDao.upsert(activeJob)
        startPollingJob(activeJob)
        return Result.success(jobId)
    }

    suspend fun retryJob(jobId: String): Result<String> {
        val job = jobDao.getById(jobId) ?: return Result.failure(IllegalStateException("Job not found"))
        val provider = VideoProviderType.entries.find { it.id == job.providerId }
        val mode = runCatching { GenerationMode.valueOf(job.mode) }.getOrDefault(GenerationMode.TEXT_TO_VIDEO)
        val aspect = AspectRatio.entries.find { it.code == job.aspectRatioCode } ?: AspectRatio.PORTRAIT_9_16
        val res = VideoResolution.entries.find { it.label == job.resolution } ?: VideoResolution.FHD_1080
        return submitVideoGeneration(
            requestedProvider = provider,
            autoMode = false,
            mode = mode,
            prompt = job.prompt,
            negativePrompt = job.negativePrompt,
            aspectRatio = aspect,
            durationSec = job.durationSec,
            resolution = res,
            motionIntensity = MotionIntensity.BALANCED,
            selectedModel = job.model,
            inputMediaUri = job.inputMediaUri
        )
    }

    private suspend fun resumePendingJobs() {
        val pending = jobDao.getActiveJobs()
        pending.forEach { job ->
            if (!job.remoteTaskId.isNullOrBlank()) {
                startPollingJob(job)
            }
        }
    }

    private fun startPollingJob(initialJob: GenerationJobEntity) {
        if (activePollingJobs.containsKey(initialJob.id)) return
        val pollingJob = repoScope.launch {
            var current = initialJob
            val provider = VideoProviderType.entries.find { it.id == current.providerId } ?: return@launch
            val apiKey = keyVault.getApiKey(provider) ?: return@launch
            val baseUrl = keyVault.getProviderBaseUrl(provider)
            val remoteId = current.remoteTaskId ?: return@launch

            repeat(60) {
                delay(4000L)
                val pollRes = adapters.pollStatus(provider, apiKey, baseUrl, remoteId)
                if (pollRes.isFailure) {
                    val err = pollRes.exceptionOrNull()?.localizedMessage ?: "Polling failed"
                    current = current.copy(
                        status = JobStatus.FAILED.name,
                        errorMessage = err,
                        updatedAtMs = System.currentTimeMillis()
                    )
                    jobDao.upsert(current)
                    addNotification("Job Polling Error", err, "job")
                    activePollingJobs.remove(current.id)
                    return@launch
                }

                val poll = pollRes.getOrThrow()
                when (poll.status) {
                    JobStatus.COMPLETED -> {
                        val url = poll.videoUrl
                        if (url.isNullOrBlank()) {
                            current = current.copy(
                                status = JobStatus.FAILED.name,
                                errorMessage = "Provider completed without returning a video URL.",
                                updatedAtMs = System.currentTimeMillis()
                            )
                            jobDao.upsert(current)
                        } else {
                            current = current.copy(
                                status = JobStatus.DOWNLOADING.name,
                                progressPercent = 90,
                                remoteVideoUrl = url,
                                updatedAtMs = System.currentTimeMillis()
                            )
                            jobDao.upsert(current)

                            val outDir = File(appContext.filesDir, "videos").apply { mkdirs() }
                            val outFile = File(outDir, "${current.id}.mp4")
                            val dl = adapters.downloadVideo(url, outFile)
                            if (dl.isSuccess) {
                                current = current.copy(
                                    status = JobStatus.COMPLETED.name,
                                    progressPercent = 100,
                                    outputVideoPath = outFile.absolutePath,
                                    updatedAtMs = System.currentTimeMillis()
                                )
                                jobDao.upsert(current)
                                saveGeneratedVideoAsProject(current, outFile.absolutePath)
                                preferences.adjustCredits(-5)
                                addNotification(
                                    title = "Video Ready (${provider.displayName})",
                                    message = current.prompt.take(70),
                                    category = "job"
                                )
                            } else {
                                current = current.copy(
                                    status = JobStatus.FAILED.name,
                                    errorMessage = dl.exceptionOrNull()?.localizedMessage ?: "Failed to download MP4",
                                    updatedAtMs = System.currentTimeMillis()
                                )
                                jobDao.upsert(current)
                            }
                        }
                        activePollingJobs.remove(current.id)
                        return@launch
                    }

                    JobStatus.FAILED, JobStatus.CANCELLED -> {
                        val err = poll.errorMessage ?: "Provider job failed"
                        current = current.copy(
                            status = JobStatus.FAILED.name,
                            errorMessage = err,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        jobDao.upsert(current)
                        addNotification("Generation Failed", err, "job")
                        activePollingJobs.remove(current.id)
                        return@launch
                    }

                    else -> {
                        current = current.copy(
                            status = JobStatus.PROCESSING.name,
                            progressPercent = poll.progressPercent,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        jobDao.upsert(current)
                    }
                }
            }
        }
        activePollingJobs[initialJob.id] = pollingJob
    }

    private suspend fun saveGeneratedVideoAsProject(job: GenerationJobEntity, videoPath: String) {
        val projectId = "proj_${UUID.randomUUID().toString().take(8)}"
        val timeline = TimelineState(
            clips = listOf(
                TimelineClip(
                    id = "clip_${UUID.randomUUID().toString().take(6)}",
                    uriString = File(videoPath).toURI().toString(),
                    label = job.prompt.take(24).ifBlank { "AI Clip" },
                    isImage = false,
                    sourceDurationMs = job.durationSec * 1000L,
                    trimStartMs = 0L,
                    trimEndMs = job.durationSec * 1000L
                )
            ),
            aspectRatioCode = job.aspectRatioCode
        )
        val now = System.currentTimeMillis()
        projectDao.upsert(
            ProjectEntity(
                id = projectId,
                title = job.prompt.take(32).ifBlank { "AI Reel" },
                sourceType = "generated",
                aspectRatioCode = job.aspectRatioCode,
                durationMs = job.durationSec * 1000L,
                videoPath = videoPath,
                thumbnailPath = null,
                timelineJson = encodeTimeline(timeline),
                promptUsed = job.prompt,
                providerId = job.providerId,
                createdAtMs = now,
                updatedAtMs = now
            )
        )
    }

    suspend fun saveProjectFromTimeline(
        existingProjectId: String?,
        title: String,
        timeline: TimelineState,
        exportedVideoPath: String?,
        thumbnailPath: String?
    ): String {
        val id = existingProjectId ?: "proj_${UUID.randomUUID().toString().take(8)}"
        val existing = existingProjectId?.let { projectDao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = id,
            title = title.trim().ifEmpty { "Untitled Studio Edit" },
            sourceType = existing?.sourceType ?: "edited",
            aspectRatioCode = timeline.aspectRatioCode,
            durationMs = timeline.totalDurationMs,
            videoPath = exportedVideoPath ?: existing?.videoPath,
            thumbnailPath = thumbnailPath ?: existing?.thumbnailPath,
            timelineJson = encodeTimeline(timeline),
            promptUsed = existing?.promptUsed ?: "",
            providerId = existing?.providerId,
            createdAtMs = existing?.createdAtMs ?: now,
            updatedAtMs = now
        )
        projectDao.upsert(entity)
        return id
    }

    suspend fun renameProject(id: String, newTitle: String) {
        if (newTitle.isBlank()) return
        projectDao.rename(id, newTitle.trim(), System.currentTimeMillis())
    }

    suspend fun duplicateProject(id: String) {
        val orig = projectDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        projectDao.upsert(
            orig.copy(
                id = "proj_${UUID.randomUUID().toString().take(8)}",
                title = "${orig.title} (Copy)",
                createdAtMs = now,
                updatedAtMs = now
            )
        )
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
    }

    suspend fun deleteJob(id: String) {
        activePollingJobs.remove(id)?.cancel()
        jobDao.deleteById(id)
    }

    suspend fun getProjectById(id: String): ProjectEntity? = projectDao.getById(id)

    fun encodeTimeline(timeline: TimelineState): String = timelineAdapter.toJson(timeline)

    fun decodeTimeline(json: String): TimelineState {
        if (json.isBlank()) return TimelineState()
        return runCatching { timelineAdapter.fromJson(json) }.getOrNull() ?: TimelineState()
    }

    suspend fun addNotification(title: String, message: String, category: String) {
        notificationDao.insert(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                title = title,
                message = message,
                category = category,
                isRead = false,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun markAllNotificationsRead() {
        notificationDao.markAllRead()
    }

    suspend fun clearNotifications() {
        notificationDao.clearAll()
    }

    suspend fun calculateStorageStats(): StorageStats {
        val projects = projectDao.observeProjects().first()
        val videoSize = dirSize(File(appContext.filesDir, "videos")) +
            dirSize(File(appContext.filesDir, "exports")) +
            dirSize(File(appContext.filesDir, "audio"))
        val cacheSize = dirSize(appContext.cacheDir) + dirSize(File(appContext.filesDir, "thumbnails"))
        return StorageStats(
            projectsCount = projects.size,
            videosBytes = videoSize,
            cacheBytes = cacheSize
        )
    }

    suspend fun clearTempCache() {
        appContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        File(appContext.filesDir, "thumbnails").listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
