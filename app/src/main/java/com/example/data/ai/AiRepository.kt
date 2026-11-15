package com.example.data.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.model.AiToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class AiToolResult(
    val tool: AiToolType,
    val title: String,
    val content: String,
    val isSamplePreview: Boolean,
    val generatedAudioPath: String? = null
)

/**
 * Unified interface for built-in Gemini AI features (prompt enhancement, script writing,
 * captions, subtitles, summarization, auto-edit planning).
 * Because Firebase setup was declined, this is backed by PlaceholderAiRepository with
 * clearly labeled sample responses rather than a direct Gemini REST client or embedded key.
 */
interface AiRepository {
    val isCloudAiEnabled: Boolean
    suspend fun enhanceVideoPrompt(rawPrompt: String, aspectRatioCode: String): Result<String>
    suspend fun runAiTool(tool: AiToolType, inputPrompt: String, contextNote: String = ""): Result<AiToolResult>
}

class PlaceholderAiRepository : AiRepository {
    override val isCloudAiEnabled: Boolean = false

    override suspend fun enhanceVideoPrompt(
        rawPrompt: String,
        aspectRatioCode: String
    ): Result<String> {
        val seed = rawPrompt.trim().ifEmpty { "Futuristic neon street scene in rain" }
        val enhanced = "[Sample Preview — Enable Firebase AI Logic for live Gemini enhancement] " +
            "Cinematic $aspectRatioCode shot: $seed. Volumetric neon rim lighting, " +
            "smooth gimbal push-in, shallow depth of field, 24fps film grain, crisp reflections."
        return Result.success(enhanced)
    }

    override suspend fun runAiTool(
        tool: AiToolType,
        inputPrompt: String,
        contextNote: String
    ): Result<AiToolResult> {
        val topic = inputPrompt.trim().ifEmpty { "AI Video Studio Launch Reel" }
        val body = when (tool) {
            AiToolType.AUTO_EDIT -> """
                [Sample Preview — Firebase AI Logic disabled]
                Smart Cut Plan for "$topic":
                • 00:00–00:01.8 — Hook shot at 1.25x speed with Neon Cyber grade
                • 00:01.8–00:04.5 — Main subject reveal (Crossfade 0.4s)
                • 00:04.5–00:08.0 — Beat-synced montage with Pop Scale title overlay
                Tip: Apply these cuts directly in the Mobile Video Editor tab.
            """.trimIndent()

            AiToolType.CAPTION_GENERATOR -> """
                [Sample Preview — Firebase AI Logic disabled]
                Hook Caption:
                "POV: Turning a single prompt into a 9:16 cinema reel in seconds."
                
                Topic: $topic
                Hashtags: #AIVideoStudio #MobileCreator #ReelsEditing #ShortFormVideo
            """.trimIndent()

            AiToolType.AUTO_SUBTITLES -> """
                [Sample Preview — Firebase AI Logic disabled]
                1
                00:00:00,200 --> 00:00:02,400
                Meet the next wave of mobile filmmaking.

                2
                00:00:02,500 --> 00:00:05,200
                Create, trim, and grade "$topic" right from your phone.
            """.trimIndent()

            AiToolType.TEXT_TO_SPEECH -> """
                On-device Android Text-to-Speech is ready for:
                "$topic"
                
                Tap "Speak Preview" or "Export WAV Track" below to synthesize real audio using your phone's TTS engine.
            """.trimIndent()

            AiToolType.AI_VOICEOVER -> """
                [Sample Preview Narration Script — Firebase AI Logic disabled]
                "Every frame tells a story. With $topic, bring studio lighting, crisp pacing, and effortless sound design straight to your pocket."
                
                Use the on-device voice preview below to audition timing.
            """.trimIndent()

            AiToolType.BACKGROUND_REMOVAL -> """
                [Sample Preview — Firebase AI Logic disabled]
                Subject Segmentation Plan for "$topic":
                • Foreground mask confidence: Sample preview mode
                • Recommended matte feather: 4px
                • Suggested backdrop: Deep Navy Volumetric Gradient (#06091A -> #3B82F6)
            """.trimIndent()

            AiToolType.VIDEO_ENHANCEMENT -> """
                [Sample Preview — Firebase AI Logic disabled]
                Recommended Media3 Color & Clarity Recipe for "$topic":
                • Contrast boost: +12%
                • Highlight recovery: +8%
                • Target export: 1080p MP4 H.264 @ 30fps
                Apply a color filter in the Editor tab and export via Media3 Transformer.
            """.trimIndent()

            AiToolType.SCRIPT_WRITER -> """
                [Sample Preview — Firebase AI Logic disabled]
                9:16 Reel Script: "$topic"
                [0:00-0:02] HOOK: Close-up dynamic reveal + bold kinetic headline
                [0:02-0:06] STORY: 3 fast cuts showing transformation in action
                [0:06-0:09] PAYOFF: Hero shot + clear call-to-action
            """.trimIndent()

            AiToolType.SCENE_GENERATOR -> """
                [Sample Preview — Firebase AI Logic disabled]
                Shot List for "$topic":
                1. Opening Drone Orbit — 9:16 vertical, blue hour haze, glowing highlights
                2. Macro Detail Cut — 50mm f/1.4 shallow focus, subtle camera push-in
                3. Finale Hero Frame — Electric violet & cyan rim light, clean negative space
            """.trimIndent()

            AiToolType.VIDEO_SUMMARIZER -> """
                [Sample Preview — Firebase AI Logic disabled]
                Summary for "$topic":
                • Core theme: Mobile-first visual storytelling
                • Pacing highlight: Strongest visual beat in first 3 seconds
                • Recommended aspect: 9:16 Portrait for mobile feeds
            """.trimIndent()
        }

        return Result.success(
            AiToolResult(
                tool = tool,
                title = tool.title,
                content = body,
                isSamplePreview = tool.requiresCloudAi
            )
        )
    }
}

/**
 * Real on-device Android TextToSpeech engine for live voice preview and WAV file synthesis.
 */
class OnDeviceTtsManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
                _isReady.value = true
            }
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f): Boolean {
        val engine = tts ?: return false
        if (!_isReady.value || text.isBlank()) return false
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        val params = Bundle()
        val id = UUID.randomUUID().toString()
        return engine.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, params, id) == TextToSpeech.SUCCESS
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    suspend fun synthesizeToWav(text: String, pitch: Float = 1.0f, rate: Float = 1.0f): Result<File> =
        withContext(Dispatchers.IO) {
            val engine = tts
            if (engine == null || !_isReady.value) {
                return@withContext Result.failure(IllegalStateException("On-device TTS engine is not ready."))
            }
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Enter text before synthesizing speech."))
            }
            val dir = File(appContext.filesDir, "audio").apply { mkdirs() }
            val outFile = File(dir, "tts_${System.currentTimeMillis()}.wav")
            engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
            engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

            suspendCancellableCoroutine { cont ->
                val utteranceId = "wav_${UUID.randomUUID()}"
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(id: String?) {
                        _isSpeaking.value = false
                        if (id == utteranceId && cont.isActive) {
                            cont.resume(Result.success(outFile))
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        _isSpeaking.value = false
                        if (id == utteranceId && cont.isActive) {
                            cont.resume(Result.failure(IllegalStateException("TTS synthesis failed")))
                        }
                    }
                })
                val res = engine.synthesizeToFile(text.trim(), Bundle(), outFile, utteranceId)
                if (res != TextToSpeech.SUCCESS && cont.isActive) {
                    cont.resume(Result.failure(IllegalStateException("Unable to start TTS file synthesis")))
                }
            }
        }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
