package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class GenerationMode(val label: String, val shortLabel: String) {
    TEXT_TO_VIDEO("Text to Video", "Text"),
    IMAGE_TO_VIDEO("Image to Video", "Image"),
    VIDEO_TO_VIDEO("Video to Video", "Video")
}

enum class AspectRatio(
    val code: String,
    val label: String,
    val ratio: Float,
    val widthPx: Int,
    val heightPx: Int,
    val isRecommendedMobile: Boolean = false
) {
    PORTRAIT_9_16("9:16", "9:16 Portrait", 9f / 16f, 1080, 1920, isRecommendedMobile = true),
    LANDSCAPE_16_9("16:9", "16:9 Landscape", 16f / 9f, 1920, 1080),
    SQUARE_1_1("1:1", "1:1 Square", 1f, 1080, 1080),
    PORTRAIT_4_5("4:5", "4:5 Portrait", 4f / 5f, 1080, 1350)
}

enum class VideoResolution(val label: String, val shortHeight: Int) {
    HD_720("720p HD", 720),
    FHD_1080("1080p Full HD", 1080),
    UHD_4K("4K Ultra HD", 2160)
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    MP4("MP4 (H.264)", "mp4", "video/mp4"),
    MP4_HEVC("MP4 (H.265 HEVC)", "mp4", "video/mp4"),
    WEBM("WebM (VP9 where supported)", "webm", "video/webm")
}

enum class MotionIntensity(val label: String, val value: Float) {
    SUBTLE("Subtle", 0.25f),
    BALANCED("Balanced", 0.5f),
    DYNAMIC("Dynamic", 0.8f),
    CINEMATIC("Cinematic", 1.0f)
}

enum class VideoProviderType(
    val id: String,
    val displayName: String,
    val tagline: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val availableModels: List<String>,
    val supportedModes: Set<GenerationMode>,
    val supportsNegativePrompt: Boolean,
    val setupSteps: List<String>
) {
    RUNWAY(
        id = "runway",
        displayName = "Runway API",
        tagline = "Gen-4 Turbo & Gen-3 Alpha",
        defaultBaseUrl = "https://api.dev.runwayml.com/v1",
        defaultModel = "gen4_turbo",
        availableModels = listOf("gen4_turbo", "gen3a_turbo", "gen4_aleph"),
        supportedModes = setOf(
            GenerationMode.TEXT_TO_VIDEO,
            GenerationMode.IMAGE_TO_VIDEO,
            GenerationMode.VIDEO_TO_VIDEO
        ),
        supportsNegativePrompt = false,
        setupSteps = listOf(
            "Sign in at dev.runwayml.com and open API Keys.",
            "Create a new secret key (starts with key_...) and ensure billing credits are active.",
            "Paste the key below and tap Test Connection to verify Runway task API access."
        )
    ),
    LUMA(
        id = "luma",
        displayName = "Luma AI API",
        tagline = "Dream Machine Ray 2 & Ray 2 Flash",
        defaultBaseUrl = "https://api.lumalabs.ai/dream-machine/v1",
        defaultModel = "ray-2",
        availableModels = listOf("ray-2", "ray-flash-2", "ray-1-6"),
        supportedModes = setOf(
            GenerationMode.TEXT_TO_VIDEO,
            GenerationMode.IMAGE_TO_VIDEO
        ),
        supportsNegativePrompt = false,
        setupSteps = listOf(
            "Log in to lumalabs.ai/dream-machine/api and generate a Dream Machine API key.",
            "Paste the Bearer API key below (never shared outside encrypted device keystore).",
            "Tap Test Connection to validate /generations endpoint availability."
        )
    ),
    VEO(
        id = "veo",
        displayName = "Google Veo API",
        tagline = "Veo 2 / Veo 3 via Vertex AI Endpoint",
        defaultBaseUrl = "https://us-central1-aiplatform.googleapis.com/v1",
        defaultModel = "veo-2.0-generate-001",
        availableModels = listOf("veo-2.0-generate-001", "veo-3.0-generate-preview"),
        supportedModes = setOf(
            GenerationMode.TEXT_TO_VIDEO,
            GenerationMode.IMAGE_TO_VIDEO
        ),
        supportsNegativePrompt = true,
        setupSteps = listOf(
            "Note: A standard Gemini text API key does NOT automatically grant Veo video generation access.",
            "Enable Vertex AI API in Google Cloud and obtain an OAuth2 access token or Veo-enabled proxy endpoint.",
            "Enter your Vertex AI endpoint URL (e.g. .../projects/ID/locations/us-central1/publishers/google/models/veo-2.0-generate-001:predictLongRunning) and token."
        )
    ),
    PIKA(
        id = "pika",
        displayName = "Pika API",
        tagline = "Pika 2.1 Video Generation (Partner/Official)",
        defaultBaseUrl = "https://api.pika.art/v1",
        defaultModel = "pika-2.1",
        availableModels = listOf("pika-2.1", "pika-2.0"),
        supportedModes = setOf(
            GenerationMode.TEXT_TO_VIDEO,
            GenerationMode.IMAGE_TO_VIDEO
        ),
        supportsNegativePrompt = true,
        setupSteps = listOf(
            "Request official or partner API access from pika.art / fal.ai Pika endpoint.",
            "Configure the base endpoint URL if using a partner gateway, then enter your API key.",
            "Tap Test Connection to verify live reachability."
        )
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom Video API",
        tagline = "Any REST-compatible video generation backend",
        defaultBaseUrl = "",
        defaultModel = "custom-video-v1",
        availableModels = listOf("custom-video-v1"),
        supportedModes = setOf(
            GenerationMode.TEXT_TO_VIDEO,
            GenerationMode.IMAGE_TO_VIDEO,
            GenerationMode.VIDEO_TO_VIDEO
        ),
        supportsNegativePrompt = true,
        setupSteps = listOf(
            "Deploy your own Node.js/FastAPI video gateway that accepts POST /generations and GET /generations/{id}.",
            "Enter your HTTPS Base URL and Bearer API Key below.",
            "Test the connection before running jobs in Generate."
        )
    )
}

enum class ConnectionStatus(val label: String) {
    NOT_CONFIGURED("Not Configured"),
    TESTING("Testing..."),
    CONNECTED("Connected"),
    AUTH_ERROR("Auth Failed"),
    UNREACHABLE("Unreachable")
}

data class ProviderConfig(
    val type: VideoProviderType,
    val hasApiKey: Boolean = false,
    val maskedKeyPreview: String = "",
    val baseUrl: String = type.defaultBaseUrl,
    val selectedModel: String = type.defaultModel,
    val status: ConnectionStatus = ConnectionStatus.NOT_CONFIGURED,
    val statusDetail: String = "No API key saved",
    val lastLatencyMs: Long? = null,
    val isDefault: Boolean = false
)

enum class JobStatus(val label: String, val isTerminal: Boolean) {
    QUEUED("Queued", false),
    SUBMITTING("Submitting", false),
    PROCESSING("Generating", false),
    DOWNLOADING("Saving MP4", false),
    COMPLETED("Completed", true),
    FAILED("Failed", true),
    CANCELLED("Cancelled", true)
}

enum class VideoFilterType(val label: String) {
    NONE("Original"),
    NEON_CYBER("Neon Cyber"),
    CINEMA_TEAL("Cinema Teal"),
    WARM_SUNSET("Warm Sunset"),
    NOIR_BW("Noir B&W"),
    VIVID_BOOST("Vivid Boost")
}

enum class TransitionType(val label: String) {
    CUT("Hard Cut"),
    CROSSFADE("Crossfade"),
    FADE_BLACK("Fade Black"),
    SLIDE_LEFT("Slide")
}

enum class TextAnimationType(val label: String) {
    NONE("Static"),
    FADE_IN("Fade In"),
    SLIDE_UP("Slide Up"),
    POP_SCALE("Pop Scale"),
    TYPEWRITER("Typewriter")
}

@JsonClass(generateAdapter = true)
data class TimelineClip(
    val id: String,
    val uriString: String,
    val label: String,
    val isImage: Boolean = false,
    val sourceDurationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val filter: VideoFilterType = VideoFilterType.NONE,
    val transitionToNext: TransitionType = TransitionType.CUT
) {
    val effectiveDurationMs: Long
        get() = (((trimEndMs - trimStartMs).coerceAtLeast(500L)) / speed.coerceIn(0.25f, 4.0f)).toLong()
}

@JsonClass(generateAdapter = true)
data class AudioTrackItem(
    val id: String,
    val uriString: String,
    val title: String,
    val durationMs: Long = 10000L,
    val volume: Float = 0.7f,
    val muted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TextOverlayItem(
    val id: String,
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val animation: TextAnimationType = TextAnimationType.FADE_IN,
    val colorHex: Long = 0xFFFFFFFF,
    val fontSizeSp: Float = 22f
)

@JsonClass(generateAdapter = true)
data class SubtitleCue(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long
)

@JsonClass(generateAdapter = true)
data class TimelineState(
    val clips: List<TimelineClip> = emptyList(),
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val subtitles: List<SubtitleCue> = emptyList(),
    val aspectRatioCode: String = AspectRatio.PORTRAIT_9_16.code
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }
}

enum class AiToolType(
    val title: String,
    val subtitle: String,
    val requiresCloudAi: Boolean,
    val supportsOnDevicePreview: Boolean = false
) {
    AUTO_EDIT("AI Auto Edit", "Smart pacing, cut points & filter plan", true),
    CAPTION_GENERATOR("AI Caption Generator", "Hook captions & hashtags for Reels", true),
    AUTO_SUBTITLES("Automatic Subtitles", "Generate timed .SRT subtitle cues", true),
    TEXT_TO_SPEECH("Text to Speech", "Speak & export WAV voice track", false, true),
    AI_VOICEOVER("AI Voiceover", "Narration script + voice synthesis", true, true),
    BACKGROUND_REMOVAL("Background Removal", "Subject segmentation & matte pass", true),
    VIDEO_ENHANCEMENT("AI Video Enhancement", "Upscale, denoise & color balance plan", true),
    SCRIPT_WRITER("AI Script Writer", "Scene-by-scene mobile video script", true),
    SCENE_GENERATOR("AI Scene Generator", "Detailed visual prompts for shots", true),
    VIDEO_SUMMARIZER("AI Video Summarizer", "Key moments & short-form summary", true)
}

data class StudioTemplate(
    val id: String,
    val title: String,
    val category: String,
    val aspectRatio: AspectRatio,
    val durationSec: Int,
    val prompt: String,
    val drawableResName: String
)

data class ExportConfig(
    val format: ExportFormat = ExportFormat.MP4,
    val resolution: VideoResolution = VideoResolution.FHD_1080,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16
)

sealed interface ExportProgressState {
    data object Idle : ExportProgressState
    data class Exporting(val progressPercent: Int, val stageLabel: String) : ExportProgressState
    data class Completed(val outputPath: String, val fileSizeBytes: Long, val galleryUri: String? = null) : ExportProgressState
    data class Error(val message: String) : ExportProgressState
}
