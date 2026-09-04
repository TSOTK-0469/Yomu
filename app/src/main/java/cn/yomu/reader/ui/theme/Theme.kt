package cn.yomu.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Paper = Color(0xFFF7F2E9)
private val Ink = Color(0xFF202923)
private val Moss = Color(0xFF334B3D)
private val Tangerine = Color(0xFFD96D45)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8DC),
    onPrimaryContainer = Color(0xFF12251A),
    secondary = Tangerine,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCD),
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFFFBF5),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E2D8),
    onSurfaceVariant = Color(0xFF504A43),
    outline = Color(0xFF7A756E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFCFB8),
    onPrimary = Color(0xFF173424),
    primaryContainer = Color(0xFF304C3A),
    secondary = Color(0xFFFFB598),
    onSecondary = Color(0xFF57200D),
    background = Color(0xFF151A17),
    onBackground = Color(0xFFE5EAE4),
    surface = Color(0xFF1B211D),
    onSurface = Color(0xFFE5EAE4),
    surfaceVariant = Color(0xFF3F4841),
    onSurfaceVariant = Color(0xFFBFC9C0),
)

@Composable
fun YomuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
