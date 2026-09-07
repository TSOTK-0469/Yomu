package cn.yomu.reader.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.ImageRef
import cn.yomu.reader.model.ReaderPreferences
import cn.yomu.reader.model.ReadingDirection
import cn.yomu.reader.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderPositionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changingModeAndDirectionPreservesCurrentImage() {
        val album = Album("album", "mount", "Album", "/pictures", List(10) {
            ImageRef("content://test/$it", "$it.png")
        }, progress = 3)
        var preferences by mutableStateOf(ReaderPreferences())
        var savedPage = -1
        compose.setContent {
            ReaderScreen(
                album, preferences, LocalContext.current.contentResolver,
                onBack = {}, onProgress = { savedPage = it }, onImageLoadFailed = {},
                settingsOpen = false, onOpenSettings = {},
            )
        }
        compose.onNodeWithText("4 / 10").assertExists()
        compose.runOnIdle { preferences = preferences.copy(mode = ReadingMode.WEBTOON) }
        compose.onNodeWithText("4 / 10").assertExists()
        assertEquals(3, savedPage)
        compose.runOnIdle { preferences = preferences.copy(mode = ReadingMode.PAGER) }
        compose.onNodeWithText("4 / 10").assertExists()
        assertEquals(3, savedPage)
        compose.runOnIdle { preferences = preferences.copy(direction = ReadingDirection.RIGHT_TO_LEFT) }
        compose.onNodeWithText("4 / 10").assertExists()
        assertEquals(3, savedPage)
    }
}
