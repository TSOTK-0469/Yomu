package cn.yomu.reader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.yomu.reader.model.GridDensity
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
class ReadingSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longImageDisablesDirectionWithoutDiscardingChoice() {
        var preferences by mutableStateOf(ReaderPreferences(direction = ReadingDirection.RIGHT_TO_LEFT))
        compose.setContent {
            MaterialTheme {
                SettingsScreen(preferences, { preferences = it }, GridDensity.STANDARD, 0L, {}, {}, {})
            }
        }
        compose.onNodeWithText("从右到左").assertIsSelected()
        compose.onNodeWithText("长图").performClick()
        compose.onNodeWithText("从右到左").assertIsNotEnabled().assertIsSelected()
        compose.onNodeWithText("从左到右").assertIsNotEnabled()
        assertEquals(ReadingMode.WEBTOON, preferences.mode)
        assertEquals(ReadingDirection.RIGHT_TO_LEFT, preferences.direction)
        compose.onNodeWithText("分页").performClick()
        compose.onNodeWithText("从右到左").assertIsSelected()
        compose.onNodeWithText("从左到右").performClick()
        assertEquals(ReadingDirection.LEFT_TO_RIGHT, preferences.direction)
    }
}
