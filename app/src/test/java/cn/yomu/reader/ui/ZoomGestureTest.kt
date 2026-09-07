package cn.yomu.reader.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ZoomGestureTest {
    @get:Rule val compose = createComposeRule()

    private fun openImage() {
        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        compose.setContent {
            Box(Modifier.size(300.dp)) {
                ZoomableImage(bitmap, "test-image", "test image", {}, {})
            }
        }
        compose.onRoot().performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        assertEquals(2.5f, scale(), 0.01f)
    }

    private fun state() = compose.onNode(SemanticsMatcher.keyIsDefined(ImageScaleKey), useUnmergedTree = true)
        .fetchSemanticsNode().config
    private fun scale() = state()[ImageScaleKey]
    private fun offset() = state()[ImageOffsetKey]

    @Test fun zoomedDragTracksFingerInScreenPixels() {
        openImage()
        compose.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(30f, 0f), 32)
        }
        val before = offset()
        compose.onRoot().performTouchInput { moveBy(Offset(60f, 0f), 64) }
        val after = offset()
        compose.onRoot().performTouchInput { up() }
        assertEquals("60 px finger movement must move the image 60 px", 60f, after.x - before.x, 3f)
    }

    @Test fun releasingFastDragContinuesMoving() {
        openImage()
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            down(center - Offset(60f, 0f))
            repeat(8) { moveBy(Offset(10f, 0f), 16) }
        }
        compose.mainClock.advanceTimeByFrame()
        val beforeRelease = offset().x
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.advanceTimeBy(96)
        val afterRelease = offset().x
        assertTrue("Image must continue after release: $beforeRelease -> $afterRelease", afterRelease > beforeRelease + 8f)
    }

    @Test fun doubleTapKeepsTappedImagePointUnderFinger() {
        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        compose.setContent { Box(Modifier.size(300.dp)) { ZoomableImage(bitmap, "anchor", "image", {}, {}) } }
        var tap = Offset.Zero
        var midpoint = Offset.Zero
        compose.onRoot().performTouchInput {
            midpoint = center
            tap = center + Offset(30f, 20f)
            doubleClick(tap)
        }
        val expected = (tap - midpoint) * (1f - scale())
        assertEquals(expected.x, offset().x, 2f)
        assertEquals(expected.y, offset().y, 2f)
    }

    @Test fun pinchKeepsOffCenterFocalPointAndAllowsTwoFingerPan() {
        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        compose.setContent { Box(Modifier.size(300.dp)) { ZoomableImage(bitmap, "pinch", "image", {}, {}) } }
        var focal = Offset.Zero
        var midpoint = Offset.Zero
        compose.onRoot().performTouchInput {
            midpoint = center
            focal = center + Offset(25f, 15f)
            down(0, focal - Offset(35f, 0f))
            down(1, focal + Offset(35f, 0f))
            repeat(8) { step ->
                val radius = 35f + (step + 1) * 5f
                updatePointerTo(0, focal - Offset(radius, 0f))
                updatePointerTo(1, focal + Offset(radius, 0f))
                move(16)
            }
        }
        val expected = (focal - midpoint) * (1f - scale())
        assertTrue(scale() > 1.5f)
        assertEquals(expected.x, offset().x, 2f)
        assertEquals(expected.y, offset().y, 2f)
        val beforePan = offset()
        compose.onRoot().performTouchInput {
            updatePointerTo(0, focal - Offset(75f, 0f) + Offset(20f, 10f))
            updatePointerTo(1, focal + Offset(75f, 0f) + Offset(20f, 10f))
            move(16)
            up(0)
            up(1)
        }
        assertEquals(beforePan.x + 20f, offset().x, 2f)
        assertEquals(beforePan.y + 10f, offset().y, 2f)
    }

    @Test fun touchingAgainStopsInertiaImmediately() {
        openImage()
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            down(center - Offset(70f, 0f))
            repeat(8) { moveBy(Offset(10f, 0f), 16) }
            up()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onRoot().performTouchInput { down(center) }
        val stopped = offset()
        compose.mainClock.advanceTimeBy(128)
        assertEquals(stopped.x, offset().x, 0.1f)
        compose.onRoot().performTouchInput { up() }
    }
}
