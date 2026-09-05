package cn.yomu.reader.data

import cn.yomu.reader.model.ImageRef
import java.util.Locale

object LibraryRules {
    fun normalizeAlbumDisplayName(rawName: String?, sourceName: String): String? {
        val name = rawName?.trim().orEmpty()
        require(name.length <= 100) { "画册名称不能超过 100 个字符" }
        return name.takeIf { it.isNotEmpty() && it != sourceName }
    }

    fun validateBookshelfName(rawName: String, existingNames: Collection<String>): String {
        val name = rawName.trim()
        require(name.isNotEmpty()) { "书架名称不能为空" }
        require(name.length <= 40) { "书架名称不能超过 40 个字符" }
        require(!name.equals("全部画册", ignoreCase = true)) { "“全部画册”是保留名称" }
        val normalized = name.lowercase(Locale.ROOT)
        require(existingNames.none { it.lowercase(Locale.ROOT) == normalized }) { "已经存在同名书架" }
        return name
    }

    fun resolveReadingIndex(lastReadUri: String?, images: List<ImageRef>): Int {
        if (images.isEmpty() || lastReadUri == null) return 0
        return images.indexOfFirst { it.uri == lastReadUri }.takeIf { it >= 0 } ?: 0
    }

    fun documentIdsOverlap(first: String, second: String): Boolean =
        first == second || first.startsWith("$second/") || second.startsWith("$first/")
}
