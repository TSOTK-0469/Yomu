package cn.yomu.reader.model

const val ALL_BOOKSHELF_ID = "__all_albums__"

enum class ReadingMode { PAGER, WEBTOON }
enum class ReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }
enum class MountMode { NON_RECURSIVE, RECURSIVE }

data class MountedFolder(
    val id: String,
    val treeUri: String,
    val uri: String,
    val name: String,
    val mode: MountMode,
    val available: Boolean,
    val albumCount: Int,
    val hiddenCount: Int,
)

data class ImageRef(val uri: String, val name: String)

data class AlbumSummary(
    val id: String,
    val mountId: String,
    val name: String,
    val path: String,
    val directoryUri: String,
    val coverUri: String,
    val defaultCoverUri: String,
    val pageCount: Int,
    val lastReadIndex: Int,
    val hidden: Boolean,
    val updatedAt: Long,
    val bookshelfIds: Set<String> = emptySet(),
) {
    val progressFraction: Float
        get() = if (pageCount <= 0 || lastReadIndex < 0) 0f
        else (lastReadIndex + 1f) / pageCount
}

data class Album(
    val id: String,
    val mountId: String,
    val name: String,
    val path: String,
    val images: List<ImageRef>,
    val progress: Int,
)

data class Bookshelf(
    val id: String,
    val name: String,
    val coverUri: String?,
    val coverVersion: Long,
    val albumCount: Int,
    val builtIn: Boolean = false,
)

data class LibrarySnapshot(
    val mounts: List<MountedFolder> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val allAlbums: List<AlbumSummary> = emptyList(),
    val hiddenAlbums: List<AlbumSummary> = emptyList(),
    val bookshelves: List<Bookshelf> = emptyList(),
    val currentBookshelfId: String = ALL_BOOKSHELF_ID,
    val issues: List<String> = emptyList(),
)

data class ReaderPreferences(
    val mode: ReadingMode = ReadingMode.PAGER,
    val direction: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
)

data class ScanProgress(val currentPath: String, val albumsFound: Int, val imagesFound: Int)

data class DirectoryChoice(val uri: String, val name: String, val path: String)

data class MountBrowserState(
    val treeUri: String,
    val current: DirectoryChoice,
    val parents: List<DirectoryChoice> = emptyList(),
    val children: List<DirectoryChoice> = emptyList(),
    val mode: MountMode = MountMode.RECURSIVE,
    val loading: Boolean = false,
)
