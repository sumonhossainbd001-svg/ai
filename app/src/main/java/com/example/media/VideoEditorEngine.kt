package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportProgressState
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineState
import com.example.domain.model.VideoFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MediaMetadataInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumbnailPath: String?
)

/**
 * Real Media3 Transformer engine for trimming, speed adjustment, rotation,
 * color filters, text overlays, aspect ratio presentation, MP4 export, and
 * scoped MediaStore gallery saving.
 */
@OptIn(UnstableApi::class)
class VideoEditorEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null

    private val _exportState = MutableStateFlow<ExportProgressState>(ExportProgressState.Idle)
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    suspend fun extractMediaInfo(uri: Uri, isImage: Boolean): MediaMetadataInfo =
        withContext(Dispatchers.IO) {
            if (isImage) {
                return@withContext MediaMetadataInfo(
                    durationMs = 5000L,
                    width = 1080,
                    height = 1920,
                    thumbnailPath = uri.toString()
                )
            }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(appContext, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 5000L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 1080
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 1920

                val thumbDir = File(appContext.filesDir, "thumbnails").apply { mkdirs() }
                val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
                val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                val thumbPath = if (frame != null) {
                    FileOutputStream(thumbFile).use { out ->
                        frame.compress(Bitmap.CompressFormat.JPEG, 82, out)
                    }
                    frame.recycle()
                    thumbFile.absolutePath
                } else null

                MediaMetadataInfo(duration, width, height, thumbPath)
            } catch (_: Exception) {
                MediaMetadataInfo(5000L, 1080, 1920, null)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

    fun startExport(
        timeline: TimelineState,
        config: ExportConfig,
        outputFileName: String
    ) {
        if (timeline.clips.isEmpty()) {
            _exportState.value = ExportProgressState.Error("Add at least one video clip or image to export.")
            return
        }

        cancelExport()
        val exportDir = File(appContext.filesDir, "exports").apply { mkdirs() }
        val safeName = outputFileName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "studio_export" }
        val outputFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.${config.format.extension}")

        val editedVideoItems = timeline.clips.mapIndexed { index, clip ->
            buildEditedItem(clip, timeline, config, includeOverlays = (index == 0))
        }

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences.add(EditedMediaItemSequence.Builder(editedVideoItems).build())

        val activeAudio = timeline.audioTracks.firstOrNull { !it.muted }
        if (activeAudio != null) {
            val audioMediaItem = MediaItem.fromUri(Uri.parse(activeAudio.uriString))
            val editedAudio = EditedMediaItem.Builder(audioMediaItem)
                .setRemoveVideo(true)
                .build()
            sequences.add(EditedMediaItemSequence.Builder(listOf(editedAudio)).build())
        }

        val composition = Composition.Builder(sequences).build()
        val mimeType = when (config.format) {
            ExportFormat.MP4 -> MimeTypes.VIDEO_H264
            ExportFormat.MP4_HEVC -> MimeTypes.VIDEO_H265
            ExportFormat.WEBM -> MimeTypes.VIDEO_VP9
        }

        _exportState.value = ExportProgressState.Exporting(2, "Initializing Media3 pipeline...")

        mainHandler.post {
            try {
                val transformer = Transformer.Builder(appContext)
                    .setVideoMimeType(mimeType)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            stopProgressPolling()
                            val size = outputFile.length()
                            _exportState.value = ExportProgressState.Completed(
                                outputPath = outputFile.absolutePath,
                                fileSizeBytes = size
                            )
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            stopProgressPolling()
                            _exportState.value = ExportProgressState.Error(
                                exportException.localizedMessage ?: "Export failed (${exportException.errorCodeName})"
                            )
                        }
                    })
                    .build()

                activeTransformer = transformer
                transformer.start(composition, outputFile.absolutePath)
                startProgressPolling(transformer)
            } catch (e: Exception) {
                _exportState.value = ExportProgressState.Error(
                    e.localizedMessage ?: "Failed to start Media3 export"
                )
            }
        }
    }

    private fun buildEditedItem(
        clip: TimelineClip,
        timeline: TimelineState,
        config: ExportConfig,
        includeOverlays: Boolean
    ): EditedMediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(clip.uriString))

        if (clip.isImage) {
            mediaItemBuilder.setImageDurationMs(clip.effectiveDurationMs.coerceAtLeast(1000L))
        } else {
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 500L))
                    .build()
            )
        }

        val videoEffects = mutableListOf<Effect>()
        if (clip.rotationDegrees != 0f) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(clip.rotationDegrees)
                    .build()
            )
        }

        val targetHeight = config.resolution.shortHeight
        val targetWidth = ((targetHeight * config.aspectRatio.ratio).toInt() / 2) * 2
        val evenHeight = (targetHeight / 2) * 2
        videoEffects.add(
            Presentation.createForWidthAndHeight(
                targetWidth.coerceAtLeast(360),
                evenHeight.coerceAtLeast(360),
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        )

        when (clip.filter) {
            VideoFilterType.NONE -> Unit
            VideoFilterType.NOIR_BW -> videoEffects.add(RgbFilter.createGrayscaleFilter())
            VideoFilterType.NEON_CYBER -> {
                videoEffects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.9f)
                        .setGreenScale(1.05f)
                        .setBlueScale(1.3f)
                        .build()
                )
                videoEffects.add(Contrast(0.18f))
            }
            VideoFilterType.CINEMA_TEAL -> {
                videoEffects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.95f)
                        .setGreenScale(1.1f)
                        .setBlueScale(1.15f)
                        .build()
                )
                videoEffects.add(Contrast(0.12f))
            }
            VideoFilterType.WARM_SUNSET -> {
                videoEffects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.22f)
                        .setGreenScale(1.04f)
                        .setBlueScale(0.84f)
                        .build()
                )
            }
            VideoFilterType.VIVID_BOOST -> {
                videoEffects.add(Contrast(0.24f))
            }
        }

        if (includeOverlays && (timeline.textOverlays.isNotEmpty() || timeline.subtitles.isNotEmpty())) {
            val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()
            timeline.textOverlays.firstOrNull()?.let { item ->
                val spannable = SpannableString(item.text).apply {
                    setSpan(ForegroundColorSpan(item.colorHex.toInt()), 0, length, 0)
                    setSpan(RelativeSizeSpan(1.4f), 0, length, 0)
                }
                overlays.add(TextOverlay.createStaticTextOverlay(spannable))
            }
            timeline.subtitles.firstOrNull()?.let { cue ->
                val spannable = SpannableString(cue.text).apply {
                    setSpan(ForegroundColorSpan(0xFFFFFFFF.toInt()), 0, length, 0)
                }
                overlays.add(TextOverlay.createStaticTextOverlay(spannable))
            }
            if (overlays.isNotEmpty()) {
                videoEffects.add(OverlayEffect(overlays))
            }
        }

        val audioProcessors = mutableListOf<AudioProcessor>()
        if (clip.speed != 1.0f && !clip.isImage) {
            val sonic = SonicAudioProcessor().apply {
                setSpeed(clip.speed.coerceIn(0.25f, 4.0f))
            }
            audioProcessors.add(sonic)
        }

        val builder = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setEffects(androidx.media3.transformer.Effects(audioProcessors, videoEffects))
            .setRemoveAudio(clip.muted || clip.isImage)

        if (clip.isImage) {
            builder.setFrameRate(30)
        }
        return builder.build()
    }

    private fun startProgressPolling(transformer: Transformer) {
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _exportState.value = ExportProgressState.Exporting(
                        progressPercent = holder.progress.coerceIn(1, 99),
                        stageLabel = "Rendering video (${holder.progress}%)"
                    )
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 350L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 350L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun cancelExport() {
        stopProgressPolling()
        mainHandler.post {
            activeTransformer?.cancel()
            activeTransformer = null
        }
        if (_exportState.value is ExportProgressState.Exporting) {
            _exportState.value = ExportProgressState.Idle
        }
    }

    fun resetExportState() {
        _exportState.value = ExportProgressState.Idle
    }

    /**
     * Saves a local video file to the smartphone's Movies/AI Video Studio collection
     * using scoped MediaStore API (zero broad storage permissions required).
     */
    suspend fun saveVideoToGallery(localFilePath: String, displayName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            val source = File(localFilePath)
            if (!source.exists()) {
                return@withContext Result.failure(IllegalStateException("Video file not found on device."))
            }
            try {
                val resolver = appContext.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AI Video Studio")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Unable to create MediaStore entry."))

                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun createShareIntent(localFilePath: String, title: String): Intent? {
        val file = File(localFilePath)
        if (!file.exists()) return null
        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share Video") }
    }
}
