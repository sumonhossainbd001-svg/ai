package com.example

import com.example.domain.model.AspectRatio
import com.example.domain.model.GenerationMode
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineState
import com.example.domain.model.VideoProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelTest {

    @Test
    fun `timeline clip effective duration scales with trim and speed`() {
        val clip = TimelineClip(
            id = "c1",
            uriString = "file:///tmp/sample.mp4",
            label = "Sample",
            sourceDurationMs = 10_000L,
            trimStartMs = 2_000L,
            trimEndMs = 8_000L,
            speed = 2.0f
        )
        assertEquals(3_000L, clip.effectiveDurationMs)

        val state = TimelineState(clips = listOf(clip))
        assertEquals(3_000L, state.totalDurationMs)
        assertEquals(AspectRatio.PORTRAIT_9_16.code, state.aspectRatioCode)
    }

    @Test
    fun `provider capabilities restrict unsupported modes`() {
        assertTrue(VideoProviderType.RUNWAY.supportedModes.contains(GenerationMode.VIDEO_TO_VIDEO))
        assertFalse(VideoProviderType.LUMA.supportedModes.contains(GenerationMode.VIDEO_TO_VIDEO))
        assertFalse(VideoProviderType.VEO.supportedModes.contains(GenerationMode.VIDEO_TO_VIDEO))
        assertTrue(VideoProviderType.VEO.supportsNegativePrompt)
    }
}
