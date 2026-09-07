@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package cn.yomu.reader.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
// API 27 avoids Robolectric's unsupported platform Magnifier window; handles and scrolling remain real Compose.
@Config(sdk = [27])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextFieldEdgeScrollTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun cursorHeldAtLeftEdgeContinuesRevealingTextAndStopsOnRelease() {
        val text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(3)
        val state = TextFieldState(text, TextRange(text.length))
        compose.setContent { MaterialTheme { AutoScrollingTextField(state, Modifier.width(220.dp)) } }
        compose.runOnIdle { compose.activity.window.decorView.dispatchWindowFocusChanged(true) }
        val field = compose.onNode(hasSetTextAction())
        field.performTouchInput { click(Offset(right - 25f, centerY)) }
        compose.runOnIdle { state.edit { selection = TextRange(text.length) } }
        field.performTouchInput { advanceEventTime(600); click(Offset(right - 25f, centerY)) }
        val handle = compose.onNode(SemanticsMatcher("cursor handle") {
            it.config.getOrNull(SelectionHandleInfoKey)?.handle == Handle.Cursor
        }, useUnmergedTree = true)
        compose.mainClock.autoAdvance = false
        handle.performTouchInput {
            down(center)
            moveBy(Offset(-180f, 0f), 200)
        }
        val atEdge = state.selection.start
        assertTrue("Fixture must leave undisplayed text to the left: $atEdge", atEdge > 10)
        compose.mainClock.advanceTimeBy(800)
        val afterHold = state.selection.start
        handle.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(300)
        assertTrue("Holding the edge must keep moving: $atEdge -> $afterHold", afterHold < atEdge - 3)
        assertEquals("Release must stop selection movement", afterHold, state.selection.start)
    }

    @Test fun cursorHeldAtRightEdgeContinuesAndStopsWhenMovedInward() {
        val text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(6)
        val state = TextFieldState(text)
        compose.setContent { MaterialTheme { AutoScrollingTextField(state, Modifier.width(220.dp)) } }
        compose.runOnIdle { compose.activity.window.decorView.dispatchWindowFocusChanged(true) }
        val field = compose.onNode(hasSetTextAction())
        field.performTouchInput { click(center) }
        field.performTouchInput { advanceEventTime(600); click(Offset(25f, centerY)) }
        val handle = compose.onNode(SemanticsMatcher("cursor handle") {
            it.config.getOrNull(SelectionHandleInfoKey)?.handle == Handle.Cursor
        }, useUnmergedTree = true)
        compose.mainClock.autoAdvance = false
        handle.performTouchInput { down(center); moveBy(Offset(180f, 0f), 200) }
        val atEdge = state.selection.start
        assertTrue(atEdge < text.length - 10)
        compose.mainClock.advanceTimeBy(500)
        assertTrue("Right edge must keep advancing: $atEdge -> ${state.selection.start}", state.selection.start > atEdge + 3)
        val fieldNode = field.fetchSemanticsNode()
        val centerOnScreen = fieldNode.positionOnScreen.x + fieldNode.size.width / 2f
        val handleOnScreen = handle.fetchSemanticsNode().positionOnScreen.x
        handle.performTouchInput { moveTo(Offset(centerOnScreen - handleOnScreen, centerY), 100) }
        compose.mainClock.advanceTimeByFrame()
        val inside = state.selection.start
        compose.mainClock.advanceTimeBy(300)
        assertEquals("Moving back inside must stop auto-scroll", inside, state.selection.start)
        handle.performTouchInput { up() }
    }

    @Test fun selectionStartKeepsExtendingAtLeftEdge() = checkSelectionHandle(Handle.SelectionStart)

    @Test fun selectionEndKeepsExtendingAtRightEdge() = checkSelectionHandle(Handle.SelectionEnd)

    private fun checkSelectionHandle(which: Handle) {
        val text = "abcdefghij ".repeat(20)
        val state = TextFieldState(text)
        compose.setContent { MaterialTheme { AutoScrollingTextField(state, Modifier.width(220.dp)) } }
        compose.runOnIdle { compose.activity.window.decorView.dispatchWindowFocusChanged(true) }
        compose.onNode(hasSetTextAction()).performTouchInput { click() }
        compose.runOnIdle { state.edit { selection = TextRange(90) } }
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTouchInput { advanceEventTime(600); longClick(center) }
        compose.runOnIdle { compose.activity.window.decorView.dispatchWindowFocusChanged(true) }
        compose.mainClock.advanceTimeBy(100)
        val handle = compose.onNode(SemanticsMatcher("$which handle") {
            it.config.getOrNull(SelectionHandleInfoKey)?.handle == which
        }, useUnmergedTree = true)
        compose.mainClock.autoAdvance = false
        handle.performTouchInput {
            down(center)
            moveBy(Offset(if (which == Handle.SelectionStart) -100f else 90f, 0f), 200)
        }
        val before = state.selection
        compose.mainClock.advanceTimeBy(500)
        val after = state.selection
        handle.performTouchInput { up() }
        if (which == Handle.SelectionStart) {
            assertTrue("Start must extend: $before -> $after", after.start < before.start - 3)
            assertEquals(before.end, after.end)
        } else {
            assertTrue("End must extend: $before -> $after", after.end > before.end + 3)
            assertEquals(before.start, after.start)
        }
        compose.mainClock.advanceTimeBy(300)
        assertEquals(after, state.selection)
    }
}
