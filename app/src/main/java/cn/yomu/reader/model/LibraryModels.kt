package cn.yomu.reader.model

enum class ReadingMode { PAGER, WEBTOON }

enum class ReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }

data class MountedFolder(
    val uri: String,
    val name: String,
)

data class ImageRef(
    val uri: String,
    val name: String,
)

data class Album(
    val id: String,
    val mountUri: String,
    val name: String,
    val path: String,
    val images: List<ImageRef>,
    val progress: Int,
) {
    val cover: ImageRef get() = images.first()
    val progressFraction: Float
        get() = if (images.isEmpty()) 0f else (progress + 1f) / images.size
}

data class LibrarySnapshot(
    val mounts: List<MountedFolder> = emptyList(),
    val albums: List<Album> = emptyList(),
    val issues: List<String> = emptyList(),
)

data class ReaderPreferences(
    val mode: ReadingMode = ReadingMode.PAGER,
    val direction: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
)
