package cn.yomu.reader.data

import cn.yomu.reader.model.ImageRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRulesTest {
    @Test
    fun `album display name is trimmed and source name restores default`() {
        assertEquals("自定义名称", LibraryRules.normalizeAlbumDisplayName("  自定义名称  ", "来源名称"))
        assertEquals(null, LibraryRules.normalizeAlbumDisplayName("来源名称", "来源名称"))
        assertEquals(null, LibraryRules.normalizeAlbumDisplayName("   ", "来源名称"))
    }

    @Test
    fun `album display name is limited to one hundred characters`() {
        val error = runCatching {
            LibraryRules.normalizeAlbumDisplayName("x".repeat(101), "来源名称")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `reading position follows the image identity`() {
        val images = listOf(
            ImageRef("content://page-1", "1.jpg"),
            ImageRef("content://page-2", "2.jpg"),
            ImageRef("content://page-3", "3.jpg"),
        )

        assertEquals(1, LibraryRules.resolveReadingIndex("content://page-2", images))
    }

    @Test
    fun `missing reading image falls back to first page`() {
        val images = listOf(ImageRef("content://page-1", "1.jpg"))

        assertEquals(0, LibraryRules.resolveReadingIndex("content://deleted", images))
    }

    @Test
    fun `bookshelf names are trimmed and unique ignoring case`() {
        assertEquals("漫画", LibraryRules.validateBookshelfName("  漫画  ", listOf("收藏")))
        val error = runCatching {
            LibraryRules.validateBookshelfName("COMICS", listOf("Comics"))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `all albums is a reserved bookshelf name`() {
        val error = runCatching {
            LibraryRules.validateBookshelfName("全部画册", emptyList())
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `nested document ids overlap but siblings do not`() {
        assertTrue(LibraryRules.documentIdsOverlap("primary:Pictures", "primary:Pictures/Manga"))
        assertFalse(LibraryRules.documentIdsOverlap("primary:Pictures/A", "primary:Pictures/B"))
    }
}
