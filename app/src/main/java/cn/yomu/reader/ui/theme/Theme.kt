package cn.yomu.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Paper = Color(0xFFF6F0D8)
private val Ink = Color(0xFF2D2920)
private val Amber = Color(0xFF8A5A24)

private val LightColors = lightColorScheme(
    primary = Amber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2D9A6),
    onPrimaryContainer = Color(0xFF34230D),
    secondary = Color(0xFF755B2C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E4BD),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE6D2),
    onSurfaceVariant = Color(0xFF5D5646),
    outline = Color(0xFF817866),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE4BC78),
    onPrimary = Color(0xFF442B05),
    primaryContainer = Color(0xFF5C421B),
    onPrimaryContainer = Color(0xFFFFDEAA),
    secondary = Color(0xFFD7C18E),
    onSecondary = Color(0xFF3B2F13),
    background = Color(0xFF1B1915),
    onBackground = Color(0xFFECE5D3),
    surface = Color(0xFF25221C),
    onSurface = Color(0xFFECE5D3),
    surfaceVariant = Color(0xFF454037),
    onSurfaceVariant = Color(0xFFCFC6B4),
)

@Composable
fun YomuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
