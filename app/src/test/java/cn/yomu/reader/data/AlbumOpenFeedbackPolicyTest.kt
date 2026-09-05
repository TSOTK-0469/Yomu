package cn.yomu.reader.data

import cn.yomu.reader.model.AlbumOpenFeedback
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumOpenFeedbackPolicyTest {
    @Test
    fun `fast indexed opens remain silent`() {
        assertEquals(AlbumOpenFeedback.NONE, AlbumOpenFeedbackPolicy.feedback(299))
    }

    @Test
    fun `slow indexed opens use card feedback`() {
        assertEquals(AlbumOpenFeedback.CARD, AlbumOpenFeedbackPolicy.feedback(300))
    }

    @Test
    fun `missing index immediately uses explicit backfill feedback`() {
        assertEquals(
            AlbumOpenFeedback.INDEX_BACKFILL,
            AlbumOpenFeedbackPolicy.feedback(elapsedMillis = 0, indexBackfill = true),
        )
    }
}
