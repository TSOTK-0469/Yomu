package cn.yomu.reader.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Shared single-line editing behavior for search and both naming dialogs. */
@Composable
internal fun AutoScrollingTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    maxLength: Int? = null,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    val scrollState = rememberScrollState()
    OutlinedTextField(
        state = state,
        modifier = modifier.textFieldEdgeAutoScroll(state, scrollState),
        lineLimits = TextFieldLineLimits.SingleLine,
        scrollState = scrollState,
        inputTransformation = maxLength?.let { InputTransformation.maxLength(it) },
        label = label?.let { content -> { content() } },
        placeholder = placeholder,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
    )
}
