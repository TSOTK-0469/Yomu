package cn.yomu.reader.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomPanBoundsTest {
    @Test
    fun `portrait image only pans vertically until zoom fills the width`() {
        val bounds = zoomPanBounds(
            viewWidth = 1_000f,
            viewHeight = 1_000f,
            imageWidth = 1_000f,
            imageHeight = 2_000f,
            scale = 2f,
        )

        assertEquals(Offset(0f, 500f), bounds)
    }

    @Test
    fun `landscape image only pans horizontally until zoom fills the height`() {
        val bounds = zoomPanBounds(
            viewWidth = 1_000f,
            viewHeight = 1_000f,
            imageWidth = 2_000f,
            imageHeight = 1_000f,
            scale = 2f,
        )

        assertEquals(Offset(500f, 0f), bounds)
    }

    @Test
    fun `pan offset clamps independently at real image edges`() {
        val clamped = clampPanOffset(
            value = Offset(800f, -900f),
            bounds = Offset(500f, 300f),
        )

        assertEquals(Offset(500f, -300f), clamped)
    }

    @Test
    fun `unzoomed image cannot pan`() {
        val bounds = zoomPanBounds(1_000f, 1_000f, 2_000f, 1_000f, scale = 1f)

        assertEquals(Offset.Zero, bounds)
    }
}
