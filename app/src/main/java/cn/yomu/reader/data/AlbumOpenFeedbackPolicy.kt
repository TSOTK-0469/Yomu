package cn.yomu.reader.data

import cn.yomu.reader.model.AlbumOpenFeedback

object AlbumOpenFeedbackPolicy {
    const val DELAY_MILLIS = 300L

    fun feedback(elapsedMillis: Long, indexBackfill: Boolean = false): AlbumOpenFeedback = when {
        indexBackfill -> AlbumOpenFeedback.INDEX_BACKFILL
        elapsedMillis >= DELAY_MILLIS -> AlbumOpenFeedback.CARD
        else -> AlbumOpenFeedback.NONE
    }
}
