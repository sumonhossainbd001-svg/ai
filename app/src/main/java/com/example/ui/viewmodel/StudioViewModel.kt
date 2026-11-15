package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepository
import com.example.data.ai.AiToolResult
import com.example.data.ai.OnDeviceTtsManager
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.GenerationJobEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StudioSettingsState
import com.example.data.network.ConnectionTestResult
import com.example.data.repository.StorageStats
import com.example.data.repository.StudioRepository
import com.example.domain.model.AiToolType
import com.example.domain.model.AspectRatio
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportProgressState
import com.example.domain.model.GenerationMode
import com.example.domain.model.MotionIntensity
import com.example.domain.model.ProviderConfig
import com.example.domain.model.StudioTemplate
import com.example.domain.model.SubtitleCue
import com.example.domain.model.TextAnimationType
import com.example.domain.model.TextOverlayItem
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineState
import com.example.domain.model.TransitionType
import com.example.domain.model.VideoFilterType
import com.example.domain.model.VideoProviderType
import com.example.domain.model.VideoResolution
import com.example.media.VideoEditorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class GeneratorFormState(
    val mode: GenerationMode = GenerationMode.TEXT_TO_VIDEO,
    val prompt: String = "",
    val negativePrompt: String = "",
    val autoMode: Boolean = true,
    val selectedProvider: VideoProviderType = VideoProviderType.RUNWAY,
    val selectedModel: String = VideoProviderType.RUNWAY.defaultModel,
    val durationSec: Int = 5,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val resolution: VideoResolution = VideoResolution.FHD_1080,
    val motionIntensity: MotionIntensity = MotionIntensity.BALANCED,
    val inputMediaUri: String? = null,
    val isEnhancingPrompt: Boolean = false,
    val isSubmitting: Boolean = false,
    val statusMessage: String? = null,
    val errorBanner: String? = null
)

data class EditorUiState(
    val projectId: String? = null,
    val projectTitle: String = "New Mobile Reel",
    val timeline: TimelineState = TimelineState(),
    val selectedClipId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusBanner: String? = null
) {
    val selectedClip: TimelineClip?
        get() = timeline.clips.find { it.id == selectedClipId } ?: timeline.clips.firstOrNull()
}

data class AiToolsUiState(
    val selectedTool: AiToolType = AiToolType.SCRIPT_WRITER,
    val inputPrompt: String = "",
    val isRunning: Boolean = false,
    val result: AiToolResult? = null,
    val ttsPitch: Float = 1.0f,
    val ttsRate: Float = 1.0f,
    val synthesizedAudioPath: String? = null,
    val error: String? = null
)

data class ActivePlayerMedia(
    val uriString: String,
    val title: String,
    val localFilePath: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioRepository(application)
    private val aiRepository: AiRepository = PlaceholderAiRepository()
    private val ttsManager = OnDeviceTtsManager(application)
    val editorEngine = VideoEditorEngine(application)

    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val jobs: StateFlow<List<GenerationJobEntity>> = repository.jobsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val settings: StateFlow<StudioSettingsState> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StudioSettingsState())

    val providers: StateFlow<List<ProviderConfig>> = repository.providerConfigs

    val exportState: StateFlow<ExportProgressState> = editorEngine.exportState

    val ttsReady: StateFlow<Boolean> = ttsManager.isReady
    val ttsSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _generatorState = MutableStateFlow(GeneratorFormState())
    val generatorState: StateFlow<GeneratorFormState> = _generatorState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _aiToolsState = MutableStateFlow(AiToolsUiState())
    val aiToolsState: StateFlow<AiToolsUiState> = _aiToolsState.asStateFlow()

    private val _activePlayerMedia = MutableStateFlow<ActivePlayerMedia?>(null)
    val activePlayerMedia: StateFlow<ActivePlayerMedia?> = _activePlayerMedia.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val undoStack = ArrayDeque<TimelineState>()
    private val redoStack = ArrayDeque<TimelineState>()

    val templates: List<StudioTemplate> = listOf(
        StudioTemplate(
            id = "tpl_cyberpunk",
            title = "Cyberpunk Neon Reel",
            category = "Sci-Fi Reel",
            aspectRatio = AspectRatio.PORTRAIT_9_16,
            durationSec = 5,
            prompt = "Rain-slicked futuristic Tokyo alley at midnight, glowing electric cyan and violet neon reflections, slow cinematic gimbal push-in, 9:16 vertical reel",
            drawableResName = "img_template_cyberpunk"
        ),
        StudioTemplate(
            id = "tpl_product",
            title = "Luxury Tech Reveal",
            category = "Commercial",
            aspectRatio = AspectRatio.PORTRAIT_9_16,
            durationSec = 5,
            prompt = "Sleek matte-black smartwatch levitating above glowing electric blue and purple glass waves, studio rim lighting, smooth 360 orbit camera, 9:16",
            drawableResName = "img_template_product"
        )
    )

    init {
        refreshStorageStats()
        viewModelScope.launch {
            editorEngine.exportState.collect { state ->
                if (state is ExportProgressState.Completed) {
                    val ed = _editorState.value
                    val thumb = editorEngine.extractMediaInfo(Uri.fromFile(File(state.outputPath)), false).thumbnailPath
                    val savedId = repository.saveProjectFromTimeline(
                        existingProjectId = ed.projectId,
                        title = ed.projectTitle,
                        timeline = ed.timeline,
                        exportedVideoPath = state.outputPath,
                        thumbnailPath = thumb
                    )
                    _editorState.update { it.copy(projectId = savedId) }
                    repository.addNotification(
                        title = "Export Complete",
                        message = "${ed.projectTitle} rendered to MP4 (${state.fileSizeBytes / 1024} KB).",
                        category = "export"
                    )
                    refreshStorageStats()
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Generator Controls ---
    fun setGeneratorMode(mode: GenerationMode) {
        _generatorState.update { curr ->
            val fallbackProvider = if (!curr.autoMode && !curr.selectedProvider.supportedModes.contains(mode)) {
                VideoProviderType.entries.first { it.supportedModes.contains(mode) }
            } else curr.selectedProvider
            curr.copy(
                mode = mode,
                selectedProvider = fallbackProvider,
                selectedModel = fallbackProvider.defaultModel,
                inputMediaUri = if (mode == GenerationMode.TEXT_TO_VIDEO) null else curr.inputMediaUri,
                errorBanner = null
            )
        }
    }

    fun updatePrompt(prompt: String) {
        _generatorState.update { it.copy(prompt = prompt, errorBanner = null) }
    }

    fun updateNegativePrompt(neg: String) {
        _generatorState.update { it.copy(negativePrompt = neg) }
    }

    fun setAutoMode(auto: Boolean) {
        _generatorState.update { it.copy(autoMode = auto, errorBanner = null) }
    }

    fun selectProvider(provider: VideoProviderType) {
        _generatorState.update { curr ->
            val adjustedMode = if (provider.supportedModes.contains(curr.mode)) curr.mode else GenerationMode.TEXT_TO_VIDEO
            curr.copy(
                autoMode = false,
                selectedProvider = provider,
                selectedModel = provider.defaultModel,
                mode = adjustedMode,
                errorBanner = null
            )
        }
    }

    fun selectModel(model: String) {
        _generatorState.update { it.copy(selectedModel = model) }
    }

    fun setDurationSec(sec: Int) {
        _generatorState.update { it.copy(durationSec = sec) }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        _generatorState.update { it.copy(aspectRatio = ratio) }
    }

    fun setResolution(res: VideoResolution) {
        _generatorState.update { it.copy(resolution = res) }
    }

    fun setMotionIntensity(motion: MotionIntensity) {
        _generatorState.update { it.copy(motionIntensity = motion) }
    }

    fun setInputMediaUri(uri: String?) {
        _generatorState.update { it.copy(inputMediaUri = uri, errorBanner = null) }
    }

    fun applyTemplateToGenerator(template: StudioTemplate) {
        _generatorState.update {
            it.copy(
                mode = GenerationMode.TEXT_TO_VIDEO,
                prompt = template.prompt,
                aspectRatio = template.aspectRatio,
                durationSec = template.durationSec,
                errorBanner = null
            )
        }
    }

    fun enhancePromptWithAi() {
        val state = _generatorState.value
        viewModelScope.launch {
            _generatorState.update { it.copy(isEnhancingPrompt = true, errorBanner = null) }
            val res = aiRepository.enhanceVideoPrompt(state.prompt, state.aspectRatio.code)
            res.onSuccess { enhanced ->
                _generatorState.update { it.copy(prompt = enhanced, isEnhancingPrompt = false) }
            }.onFailure { err ->
                _generatorState.update {
                    it.copy(isEnhancingPrompt = false, errorBanner = err.localizedMessage)
                }
            }
        }
    }

    fun submitGeneration() {
        val state = _generatorState.value
        if (state.prompt.isBlank()) {
            _generatorState.update { it.copy(errorBanner = "Write a video prompt before generating.") }
            return
        }
        if ((state.mode == GenerationMode.IMAGE_TO_VIDEO || state.mode == GenerationMode.VIDEO_TO_VIDEO) &&
            state.inputMediaUri.isNullOrBlank()
        ) {
            _generatorState.update {
                it.copy(
                    errorBanner = if (state.mode == GenerationMode.IMAGE_TO_VIDEO) {
                        "Select or capture a reference image first."
                    } else {
                        "Select an existing video clip to transform."
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            _generatorState.update { it.copy(isSubmitting = true, errorBanner = null, statusMessage = null) }
            val result = repository.submitVideoGeneration(
                requestedProvider = if (state.autoMode) null else state.selectedProvider,
                autoMode = state.autoMode,
                mode = state.mode,
                prompt = state.prompt,
                negativePrompt = state.negativePrompt,
                aspectRatio = state.aspectRatio,
                durationSec = state.durationSec,
                resolution = state.resolution,
                motionIntensity = state.motionIntensity,
                selectedModel = state.selectedModel,
                inputMediaUri = state.inputMediaUri
            )
            result.onSuccess { jobId ->
                _generatorState.update {
                    it.copy(
                        isSubmitting = false,
                        statusMessage = "Job $jobId submitted. Tracking progress below."
                    )
                }
            }.onFailure { err ->
                _generatorState.update {
                    it.copy(
                        isSubmitting = false,
                        errorBanner = err.localizedMessage ?: "Failed to submit generation."
                    )
                }
            }
        }
    }

    fun retryJob(jobId: String) {
        viewModelScope.launch {
            val res = repository.retryJob(jobId)
            res.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Retry failed"
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch { repository.deleteJob(jobId) }
    }

    // --- API Manager ---
    fun saveProviderConfig(
        provider: VideoProviderType,
        apiKey: String?,
        baseUrl: String,
        model: String
    ) {
        viewModelScope.launch {
            repository.saveProviderConfiguration(provider, apiKey, baseUrl, model)
            _snackbarMessage.value = "${provider.displayName} configuration saved."
        }
    }

    fun removeProviderKey(provider: VideoProviderType) {
        viewModelScope.launch {
            repository.removeProviderKey(provider)
            _snackbarMessage.value = "${provider.displayName} key removed."
        }
    }

    fun setDefaultProvider(provider: VideoProviderType) {
        viewModelScope.launch {
            repository.setDefaultProvider(provider)
            _snackbarMessage.value = "${provider.displayName} set as default."
        }
    }

    fun testProviderConnection(provider: VideoProviderType, onDone: (ConnectionTestResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.testProviderConnection(provider)
            onDone(res)
        }
    }

    // --- Mobile Video Editor ---
    private fun mutateTimeline(transform: (TimelineState) -> TimelineState) {
        val current = _editorState.value.timeline
        val updated = transform(current)
        if (updated == current) return
        undoStack.addLast(current)
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        _editorState.update { state ->
            val validSelectedId = updated.clips.find { it.id == state.selectedClipId }?.id
                ?: updated.clips.firstOrNull()?.id
            state.copy(
                timeline = updated,
                selectedClipId = validSelectedId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun undoEdit() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_editorState.value.timeline)
        _editorState.update {
            it.copy(
                timeline = prev,
                selectedClipId = prev.clips.firstOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun redoEdit() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_editorState.value.timeline)
        _editorState.update {
            it.copy(
                timeline = next,
                selectedClipId = next.clips.firstOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun updateEditorProjectTitle(title: String) {
        _editorState.update { it.copy(projectTitle = title) }
    }

    fun selectEditorClip(clipId: String) {
        _editorState.update { it.copy(selectedClipId = clipId) }
    }

    fun updatePlayhead(ms: Long) {
        val maxMs = _editorState.value.timeline.totalDurationMs.coerceAtLeast(1L)
        _editorState.update { it.copy(playheadMs = ms.coerceIn(0L, maxMs)) }
    }

    fun setEditorPlaying(playing: Boolean) {
        _editorState.update { it.copy(isPlaying = playing) }
    }

    fun addMediaToTimeline(uri: Uri, isImage: Boolean, labelPrefix: String) {
        viewModelScope.launch {
            val info = editorEngine.extractMediaInfo(uri, isImage)
            val newClip = TimelineClip(
                id = "clip_${UUID.randomUUID().toString().take(6)}",
                uriString = uri.toString(),
                label = "$labelPrefix ${_editorState.value.timeline.clips.size + 1}",
                isImage = isImage,
                sourceDurationMs = info.durationMs,
                trimStartMs = 0L,
                trimEndMs = info.durationMs
            )
            mutateTimeline { it.copy(clips = it.clips + newClip) }
            _editorState.update { it.copy(selectedClipId = newClip.id) }
        }
    }

    fun addAudioTrack(uri: Uri, title: String) {
        viewModelScope.launch {
            val info = editorEngine.extractMediaInfo(uri, false)
            val track = com.example.domain.model.AudioTrackItem(
                id = "aud_${UUID.randomUUID().toString().take(6)}",
                uriString = uri.toString(),
                title = title.ifBlank { "Music Track" },
                durationMs = info.durationMs
            )
            mutateTimeline { it.copy(audioTracks = it.audioTracks + track) }
        }
    }

    fun trimSelectedClip(trimStartMs: Long, trimEndMs: Long) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map { clip ->
                    if (clip.id == targetId) {
                        val safeStart = trimStartMs.coerceIn(0L, (clip.sourceDurationMs - 500L).coerceAtLeast(0L))
                        val safeEnd = trimEndMs.coerceIn(safeStart + 500L, clip.sourceDurationMs.coerceAtLeast(safeStart + 500L))
                        clip.copy(trimStartMs = safeStart, trimEndMs = safeEnd)
                    } else clip
                }
            )
        }
    }

    fun splitSelectedClipAtPlayhead() {
        val state = _editorState.value
        val target = state.selectedClip ?: return
        val tl = state.timeline
        var cursor = 0L
        var clipStartInTimeline = 0L
        for (c in tl.clips) {
            if (c.id == target.id) {
                clipStartInTimeline = cursor
                break
            }
            cursor += c.effectiveDurationMs
        }
        val offsetInClip = ((state.playheadMs - clipStartInTimeline) * target.speed).toLong()
        val splitPoint = target.trimStartMs + offsetInClip
        val safeSplit = if (splitPoint <= target.trimStartMs + 400L || splitPoint >= target.trimEndMs - 400L) {
            (target.trimStartMs + target.trimEndMs) / 2L
        } else splitPoint

        if (safeSplit - target.trimStartMs < 350L || target.trimEndMs - safeSplit < 350L) {
            _snackbarMessage.value = "Clip is too short to split further."
            return
        }

        val firstPart = target.copy(
            trimEndMs = safeSplit,
            label = "${target.label} A"
        )
        val secondPart = target.copy(
            id = "clip_${UUID.randomUUID().toString().take(6)}",
            trimStartMs = safeSplit,
            label = "${target.label} B"
        )
        mutateTimeline { current ->
            val newClips = current.clips.flatMap { c ->
                if (c.id == target.id) listOf(firstPart, secondPart) else listOf(c)
            }
            current.copy(clips = newClips)
        }
        _editorState.update { it.copy(selectedClipId = secondPart.id) }
    }

    fun deleteSelectedClip() {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(clips = tl.clips.filterNot { it.id == targetId })
        }
    }

    fun moveSelectedClip(delta: Int) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            val idx = tl.clips.indexOfFirst { it.id == targetId }
            val newIdx = (idx + delta).coerceIn(0, tl.clips.lastIndex)
            if (idx == -1 || idx == newIdx) return@mutateTimeline tl
            val mutable = tl.clips.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(newIdx, item)
            tl.copy(clips = mutable)
        }
    }

    fun rotateSelectedClip() {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map {
                    if (it.id == targetId) it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) else it
                }
            )
        }
    }

    fun setSelectedClipSpeed(speed: Float) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map {
                    if (it.id == targetId) it.copy(speed = speed.coerceIn(0.25f, 4.0f)) else it
                }
            )
        }
    }

    fun setSelectedClipFilter(filter: VideoFilterType) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map {
                    if (it.id == targetId) it.copy(filter = filter) else it
                }
            )
        }
    }

    fun setSelectedClipTransition(transition: TransitionType) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map {
                    if (it.id == targetId) it.copy(transitionToNext = transition) else it
                }
            )
        }
    }

    fun setSelectedClipVolume(volume: Float, muted: Boolean) {
        val targetId = _editorState.value.selectedClip?.id ?: return
        mutateTimeline { tl ->
            tl.copy(
                clips = tl.clips.map {
                    if (it.id == targetId) it.copy(volume = volume.coerceIn(0f, 1f), muted = muted) else it
                }
            )
        }
    }

    fun setAudioTrackVolume(trackId: String, volume: Float, muted: Boolean) {
        mutateTimeline { tl ->
            tl.copy(
                audioTracks = tl.audioTracks.map {
                    if (it.id == trackId) it.copy(volume = volume.coerceIn(0f, 1f), muted = muted) else it
                }
            )
        }
    }

    fun removeAudioTrack(trackId: String) {
        mutateTimeline { tl ->
            tl.copy(audioTracks = tl.audioTracks.filterNot { it.id == trackId })
        }
    }

    fun setEditorAspectRatio(ratio: AspectRatio) {
        mutateTimeline { it.copy(aspectRatioCode = ratio.code) }
    }

    fun addTextOverlay(text: String, animation: TextAnimationType) {
        if (text.isBlank()) return
        val start = _editorState.value.playheadMs
        val overlay = TextOverlayItem(
            id = "txt_${UUID.randomUUID().toString().take(6)}",
            text = text.trim(),
            startMs = start,
            endMs = start + 3500L,
            animation = animation
        )
        mutateTimeline { it.copy(textOverlays = it.textOverlays + overlay) }
    }

    fun removeTextOverlay(id: String) {
        mutateTimeline { tl -> tl.copy(textOverlays = tl.textOverlays.filterNot { it.id == id }) }
    }

    fun addSubtitleCue(text: String, startMs: Long, endMs: Long) {
        if (text.isBlank()) return
        val cue = SubtitleCue(
            id = "sub_${UUID.randomUUID().toString().take(6)}",
            text = text.trim(),
            startMs = startMs.coerceAtLeast(0L),
            endMs = endMs.coerceAtLeast(startMs + 800L)
        )
        mutateTimeline { it.copy(subtitles = it.subtitles + cue) }
    }

    fun removeSubtitleCue(id: String) {
        mutateTimeline { tl -> tl.copy(subtitles = tl.subtitles.filterNot { it.id == id }) }
    }

    fun saveEditorProject() {
        val state = _editorState.value
        if (state.timeline.clips.isEmpty()) {
            _snackbarMessage.value = "Add at least one clip before saving project."
            return
        }
        viewModelScope.launch {
            val firstClip = state.timeline.clips.first()
            val info = editorEngine.extractMediaInfo(Uri.parse(firstClip.uriString), firstClip.isImage)
            val savedId = repository.saveProjectFromTimeline(
                existingProjectId = state.projectId,
                title = state.projectTitle,
                timeline = state.timeline,
                exportedVideoPath = null,
                thumbnailPath = info.thumbnailPath
            )
            _editorState.update { it.copy(projectId = savedId) }
            refreshStorageStats()
            _snackbarMessage.value = "Project \"${state.projectTitle}\" saved."
        }
    }

    fun openProjectInEditor(project: ProjectEntity) {
        undoStack.clear()
        redoStack.clear()
        val decoded = repository.decodeTimeline(project.timelineJson)
        val timeline = if (decoded.clips.isEmpty() && !project.videoPath.isNullOrBlank()) {
            val uriStr = File(project.videoPath).toURI().toString()
            decoded.copy(
                clips = listOf(
                    TimelineClip(
                        id = "clip_${UUID.randomUUID().toString().take(6)}",
                        uriString = uriStr,
                        label = project.title.take(20),
                        sourceDurationMs = project.durationMs.coerceAtLeast(3000L),
                        trimStartMs = 0L,
                        trimEndMs = project.durationMs.coerceAtLeast(3000L)
                    )
                )
            )
        } else decoded
        _editorState.value = EditorUiState(
            projectId = project.id,
            projectTitle = project.title,
            timeline = timeline,
            selectedClipId = timeline.clips.firstOrNull()?.id,
            playheadMs = 0L,
            canUndo = false,
            canRedo = false
        )
    }

    fun startExport(config: ExportConfig) {
        val state = _editorState.value
        editorEngine.startExport(state.timeline, config, state.projectTitle)
    }

    fun cancelExport() {
        editorEngine.cancelExport()
    }

    fun resetExportState() {
        editorEngine.resetExportState()
    }

    fun saveVideoToGallery(filePath: String, title: String) {
        viewModelScope.launch {
            val res = editorEngine.saveVideoToGallery(filePath, title)
            res.onSuccess {
                _snackbarMessage.value = "Saved to Movies/AI Video Studio in Gallery!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save to gallery"
            }
        }
    }

    fun buildShareIntent(filePath: String, title: String): Intent? =
        editorEngine.createShareIntent(filePath, title)

    // --- Projects ---
    fun renameProject(id: String, newTitle: String) {
        viewModelScope.launch { repository.renameProject(id, newTitle) }
    }

    fun duplicateProject(id: String) {
        viewModelScope.launch {
            repository.duplicateProject(id)
            refreshStorageStats()
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            refreshStorageStats()
        }
    }

    // --- Player Modal ---
    fun openVideoPlayer(uriString: String, title: String, localFilePath: String? = null) {
        _activePlayerMedia.value = ActivePlayerMedia(uriString, title, localFilePath)
    }

    fun closeVideoPlayer() {
        _activePlayerMedia.value = null
    }

    // --- AI Smart Tools ---
    fun selectAiTool(tool: AiToolType) {
        _aiToolsState.update { it.copy(selectedTool = tool, error = null) }
    }

    fun updateAiToolPrompt(prompt: String) {
        _aiToolsState.update { it.copy(inputPrompt = prompt, error = null) }
    }

    fun updateTtsParams(pitch: Float, rate: Float) {
        _aiToolsState.update { it.copy(ttsPitch = pitch, ttsRate = rate) }
    }

    fun runSelectedAiTool() {
        val state = _aiToolsState.value
        viewModelScope.launch {
            _aiToolsState.update { it.copy(isRunning = true, error = null) }
            val res = aiRepository.runAiTool(state.selectedTool, state.inputPrompt)
            res.onSuccess { out ->
                _aiToolsState.update { it.copy(isRunning = false, result = out) }
            }.onFailure { err ->
                _aiToolsState.update { it.copy(isRunning = false, error = err.localizedMessage) }
            }
        }
    }

    fun speakTtsPreview(textOverride: String? = null) {
        val state = _aiToolsState.value
        val text = (textOverride ?: state.inputPrompt).ifBlank {
            state.result?.content?.take(200).orEmpty()
        }
        if (text.isBlank()) {
            _snackbarMessage.value = "Enter script or narration text first."
            return
        }
        val ok = ttsManager.speak(text, state.ttsPitch, state.ttsRate)
        if (!ok) {
            _snackbarMessage.value = "Android Text-to-Speech engine is initializing or unavailable."
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun synthesizeTtsToEditorAudio() {
        val state = _aiToolsState.value
        val text = state.inputPrompt.ifBlank { state.result?.content?.take(300).orEmpty() }
        if (text.isBlank()) {
            _snackbarMessage.value = "Enter voiceover text first."
            return
        }
        viewModelScope.launch {
            val res = ttsManager.synthesizeToWav(text, state.ttsPitch, state.ttsRate)
            res.onSuccess { file ->
                _aiToolsState.update { it.copy(synthesizedAudioPath = file.absolutePath) }
                addAudioTrack(Uri.fromFile(file), "AI Voiceover WAV")
                _snackbarMessage.value = "Voice WAV synthesized and added to Editor audio track!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "TTS synthesis failed"
            }
        }
    }

    // --- Settings & Notifications ---
    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun clearNotifications() {
        viewModelScope.launch { repository.clearNotifications() }
    }

    fun updateCreatorName(name: String) {
        viewModelScope.launch { repository.preferences.updateCreatorName(name) }
    }

    fun setAutoFallback(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setAutoFallback(enabled) }
    }

    fun setDefaultAspectRatio(code: String) {
        viewModelScope.launch { repository.preferences.setDefaultAspectRatio(code) }
    }

    fun setDefaultResolution(res: VideoResolution) {
        viewModelScope.launch { repository.preferences.setDefaultResolution(res) }
    }

    fun setJobNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setJobNotifications(enabled) }
    }

    fun setApiNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setApiNotifications(enabled) }
    }

    fun setThemeAccent(accent: String) {
        viewModelScope.launch { repository.preferences.setThemeAccent(accent) }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    fun clearTempCache() {
        viewModelScope.launch {
            repository.clearTempCache()
            refreshStorageStats()
            _snackbarMessage.value = "Temporary thumbnails & cache cleared."
        }
    }

    override fun onCleared() {
        super.onCleared()
        editorEngine.cancelExport()
        ttsManager.shutdown()
    }
}
