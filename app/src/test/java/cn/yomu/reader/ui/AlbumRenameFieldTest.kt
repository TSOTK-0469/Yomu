package cn.yomu.reader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumRenameFieldTest {
    @Test
    fun `rename field opens with cursor at end of a long album name`() {
        val name = "Pictures 2026 Summer Collection Volume 123456789"

        val state = albumRenameFieldState(name)

        assertEquals(name, state.text.toString())
        assertEquals(name.length, state.selection.start)
        assertEquals(name.length, state.selection.end)
    }
}
