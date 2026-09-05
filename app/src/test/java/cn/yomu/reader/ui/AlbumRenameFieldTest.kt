package cn.yomu.reader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumRenameFieldTest {
    @Test
    fun `rename field opens with cursor at end of a long album name`() {
        val name = "Pictures 2026 Summer Collection Volume 123456789"

        val value = albumRenameFieldValue(name)

        assertEquals(name, value.text)
        assertEquals(name.length, value.selection.start)
        assertEquals(name.length, value.selection.end)
    }
}
