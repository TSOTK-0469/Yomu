@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package cn.yomu.reader.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.TextFieldDecoratorModifierNode
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.node.visitSubtree
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
 * Compose 1.11.4 exposes the horizontal scroll state, but not the active handle drag.
 * This small, version-bound adapter observes the real cursor/selection handles (including
 * their popup windows). It keeps the normal IME, clipboard, accessibility and handle gestures.
 * Keep the touch regression tests when updating Compose; do not replace this with reflection.
 */
internal fun Modifier.textFieldEdgeAutoScroll(state: TextFieldState, scroll: ScrollState): Modifier =
    this.then(EdgeScrollElement(state, scroll))

private data class EdgeScrollElement(val state: TextFieldState, val scroll: ScrollState) :
    ModifierNodeElement<EdgeScrollNode>() {
    override fun create() = EdgeScrollNode(state, scroll)
    override fun update(node: EdgeScrollNode) { node.state = state; node.scroll = scroll }
    override fun InspectorInfo.inspectableProperties() { name = "textFieldEdgeAutoScroll" }
}

private class EdgeScrollNode(var state: TextFieldState, var scroll: ScrollState) :
    Modifier.Node(), GlobalPositionAwareModifierNode, ObserverModifierNode {
    private var field: TextFieldDecoratorModifierNode? = null
    private var animation: Job? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (field?.isAttached != true) {
            visitSubtree(Nodes.Semantics) { node ->
                if (node is TextFieldDecoratorModifierNode) field = node
            }
        }
        observeDrag()
    }

    override fun onObservedReadsChanged() = observeDrag()

    private fun observeDrag() {
        observeReads {
            val selection = field?.textFieldSelectionState
            val dragging = selection?.draggingHandle != null
            // Observe movement as well as release, so re-entering the edge restarts the loop.
            selection?.handleDragPosition
            if (!dragging) {
                animation?.cancel()
                animation = null
            } else if (animation?.isActive != true) {
                animation = coroutineScope.launch {
                    var previousFrame = withFrameNanos { it }
                    while (isActive) {
                        val frame = withFrameNanos { it }
                        val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f)
                        previousFrame = frame
                        scrollOneFrame(seconds)
                    }
                }
            }
        }
    }

    private fun scrollOneFrame(seconds: Float) {
        val field = field?.takeIf { it.isAttached } ?: return
        val selectionState = field.textFieldSelectionState
        val handle = selectionState.draggingHandle ?: return
        val position = selectionState.handleDragPosition.takeIf { it.isSpecified } ?: return
        val layoutState = field.textLayoutState
        val text = layoutState.textLayoutNodeCoordinates?.takeIf { it.isAttached } ?: return
        val viewport = layoutState.coreNodeCoordinates?.takeIf { it.isAttached } ?: return
        val layout = layoutState.layoutResult ?: return
        val local = viewport.localPositionOf(text, position)
        val edge = with(requireDensity()) { 32.dp.toPx() }
        val speed = edgeScrollSpeed(local.x, viewport.size.width.toFloat(), edge)
        if (speed == 0f) return
        val direction = if (requireLayoutDirection() == LayoutDirection.Rtl) -1f else 1f
        val moved = scroll.dispatchRawDelta(speed * seconds * direction) * direction
        if (moved == 0f) return

        // Clamp to the visible edge before moving; an out-of-bounds finger must not cause a jump.
        val visiblePoint = Offset(local.x.coerceIn(0f, viewport.size.width.toFloat()), local.y)
        val target = text.localPositionOf(viewport, visiblePoint) + Offset(moved, 0f)
        val index = layout.getOffsetForPosition(target).coerceIn(0, state.text.length)
        state.edit {
            selection = when (handle) {
                Handle.Cursor -> TextRange(index)
                Handle.SelectionStart -> if (index == selection.end) selection else TextRange(index, selection.end)
                Handle.SelectionEnd -> if (index == selection.start) selection else TextRange(selection.start, index)
            }
        }
    }

    override fun onDetach() {
        animation?.cancel()
        animation = null
        field = null
    }
}

/** Linear acceleration in the edge band, capped at twelve band widths per second. */
internal fun edgeScrollSpeed(x: Float, width: Float, edge: Float): Float {
    if (width <= 0f || edge <= 0f) return 0f
    val band = edge.coerceAtMost(width / 2f)
    return when {
        x < band -> -((band - x) / band).coerceIn(0f, 1f) * edge * 12f
        x > width - band -> ((x - (width - band)) / band).coerceIn(0f, 1f) * edge * 12f
        else -> 0f
    }
}
