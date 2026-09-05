package cn.yomu.reader.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import cn.yomu.reader.data.db.AlbumBookshelfEntity
import cn.yomu.reader.data.db.AlbumEntity
import cn.yomu.reader.data.db.AlbumImageEntity
import cn.yomu.reader.data.db.BookshelfEntity
import cn.yomu.reader.data.db.MountEntity
import cn.yomu.reader.data.db.YomuDatabase
import cn.yomu.reader.model.ALL_BOOKSHELF_ID
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.AlbumSummary
import cn.yomu.reader.model.Bookshelf
import cn.yomu.reader.model.DirectoryChoice
import cn.yomu.reader.model.GridDensity
import cn.yomu.reader.model.ImageRef
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountMode
import cn.yomu.reader.model.MountedFolder
import cn.yomu.reader.model.ReaderPreferences
import cn.yomu.reader.model.ReadingDirection
import cn.yomu.reader.model.ReadingMode
import cn.yomu.reader.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class MissingImageCheck { PRESENT_OR_UNREADABLE, REMOVED, ALBUM_REMOVED, SOURCE_UNAVAILABLE }

class LibraryRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val database = YomuDatabase.get(context)
    private val dao = database.libraryDao()
    private val resolver: ContentResolver = context.contentResolver

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (!preferences.getBoolean(KEY_ROOM_INITIALIZED, false)) {
            val legacyUris = preferences.getString(KEY_LEGACY_MOUNTS, "")
                .orEmpty()
                .lineSequence()
                .filter(String::isNotBlank)
                .distinct()
                .toList()
            legacyUris.forEach(::releasePermission)
            val editor = preferences.edit().remove(KEY_LEGACY_MOUNTS)
            preferences.all.keys.filter { it.startsWith("progress_") }.forEach(editor::remove)
            editor.putBoolean(KEY_ROOM_INITIALIZED, true).commit()
        }
    }

    suspend fun loadLibrary(requestedShelfId: String? = null): LibrarySnapshot = withContext(Dispatchers.IO) {
        val mounts = dao.mounts()
        val entities = dao.albums()
        val shelves = dao.bookshelves()
        val memberships = dao.memberships()
        val shelfIdsByAlbum = memberships.groupBy({ it.albumId }, { it.bookshelfId })
        val summaries = entities.map { it.toSummary(shelfIdsByAlbum[it.id].orEmpty().toSet()) }
        val visible = summaries.filterNot(AlbumSummary::hidden).sortedWith(albumComparator)
        val hidden = summaries.filter(AlbumSummary::hidden).sortedWith(albumComparator)
        val validShelfIds = shelves.mapTo(mutableSetOf()) { it.id }
        val preferred = requestedShelfId
            ?: preferences.getString(KEY_CURRENT_SHELF, ALL_BOOKSHELF_ID)
            ?: ALL_BOOKSHELF_ID
        val currentShelfId = preferred.takeIf { it == ALL_BOOKSHELF_ID || it in validShelfIds }
            ?: ALL_BOOKSHELF_ID
        if (currentShelfId != preferred) saveCurrentShelf(currentShelfId)

        val currentAlbums = if (currentShelfId == ALL_BOOKSHELF_ID) visible else {
            visible.filter { currentShelfId in it.bookshelfIds }
        }
        val bookshelfModels = buildList {
            add(
                Bookshelf(
                    id = ALL_BOOKSHELF_ID,
                    name = "全部画册",
                    coverUri = visible.firstOrNull()?.coverUri,
                    coverVersion = visible.firstOrNull()?.updatedAt ?: 0L,
                    albumCount = visible.size,
                    builtIn = true,
                ),
            )
            shelves.sortedWith { first, second -> NaturalOrder.compare(first.name, second.name) }
                .forEach { shelf ->
                    val members = visible.filter { shelf.id in it.bookshelfIds }
                    val cover = members.firstOrNull { it.id == shelf.coverAlbumId }?.coverUri
                        ?: members.firstOrNull()?.coverUri
                    val coverVersion = members.firstOrNull { it.id == shelf.coverAlbumId }?.updatedAt
                        ?: members.firstOrNull()?.updatedAt
                        ?: 0L
                    add(Bookshelf(shelf.id, shelf.name, cover, coverVersion, members.size))
                }
        }
        val mountModels = mounts.map { mount ->
            val owned = summaries.filter { it.mountId == mount.id }
            MountedFolder(
                id = mount.id,
                treeUri = mount.treeUri,
                uri = mount.directoryUri,
                name = mount.name,
                path = mount.path.ifBlank { mount.name },
                mode = runCatching { MountMode.valueOf(mount.mode) }.getOrDefault(MountMode.RECURSIVE),
                available = mount.available,
                albumCount = owned.size,
                hiddenCount = owned.count(AlbumSummary::hidden),
                lastSuccessfulRefreshAt = mount.lastSuccessfulRefreshAt,
                lastRefreshFailed = mount.lastRefreshFailed,
            )
        }
        LibrarySnapshot(
            mounts = mountModels,
            albums = currentAlbums,
            allAlbums = visible,
            hiddenAlbums = hidden,
            bookshelves = bookshelfModels,
            currentBookshelfId = currentShelfId,
            issues = mountModels.filterNot(MountedFolder::available)
                .map { "${it.name} 暂时无法访问，可在挂载管理中重新授权或刷新。" },
        )
    }

    fun saveCurrentShelf(id: String) {
        preferences.edit().putString(KEY_CURRENT_SHELF, id).apply()
    }

    suspend fun rootDirectory(treeUri: String): DirectoryChoice = withContext(Dispatchers.IO) {
        val tree = Uri.parse(treeUri)
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val name = queryDocumentName(documentUri)
            ?: tree.lastPathSegment?.substringAfterLast(':')?.takeIf(String::isNotBlank)
            ?: "已选目录"
        DirectoryChoice(documentUri.toString(), name, name)
    }

    suspend fun childDirectories(treeUri: String, current: DirectoryChoice): List<DirectoryChoice> =
        withContext(Dispatchers.IO) {
            listChildren(Uri.parse(treeUri), Uri.parse(current.uri))
                .filter(SafEntry::isDirectory)
                .sortedWith { first, second -> NaturalOrder.compare(first.name, second.name) }
                .map { DirectoryChoice(it.uri, it.name, "${current.path}/${it.name}") }
        }

    suspend fun addMount(
        treeUri: String,
        target: DirectoryChoice,
        mode: MountMode,
        onProgress: (ScanProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        ensureNotOverlapping(target.uri)
        val mountId = stableId(target.uri)
        val scanned = scan(
            mountId = mountId,
            treeUri = treeUri,
            rootUri = target.uri,
            rootName = target.name,
            mode = mode,
            onProgress = onProgress,
        )
        persistFolderPermission(Uri.parse(treeUri))
        try {
            val now = System.currentTimeMillis()
            val albums = scanned.map { it.toEntity(now) }
            database.withTransaction {
                dao.upsertMount(
                    MountEntity(
                        mountId,
                        treeUri,
                        target.uri,
                        target.name,
                        target.path,
                        mode.name,
                        true,
                        now,
                        now,
                        false,
                    ),
                )
                dao.upsertAlbums(albums)
                replaceAlbumImages(scanned)
            }
        } catch (error: Throwable) {
            releasePermission(treeUri)
            throw error
        }
    }

    suspend fun refreshMount(mountId: String, onProgress: (ScanProgress) -> Unit) =
        withContext(Dispatchers.IO) {
            val mount = dao.mount(mountId) ?: error("挂载源不存在")
            try {
                val mode = runCatching { MountMode.valueOf(mount.mode) }.getOrDefault(MountMode.RECURSIVE)
                val scanned = scan(mount.id, mount.treeUri, mount.directoryUri, mount.name, mode, onProgress)
                val existing = dao.albumsForMount(mount.id).associateBy(AlbumEntity::id)
                val now = System.currentTimeMillis()
                val replacements = scanned.map { item ->
                    val old = existing[item.id]
                    val imageUris = item.images.mapTo(hashSetOf(), ImageRef::uri)
                    val custom = old?.customCoverUri?.takeIf { it in imageUris }
                    val oldRead = old?.lastReadUri
                    val readStillExists = oldRead != null && oldRead in imageUris
                    item.toEntity(now).copy(
                        customName = old?.customName,
                        customCoverUri = custom,
                        lastReadUri = when {
                            oldRead == null -> null
                            readStillExists -> oldRead
                            else -> item.images.first().uri
                        },
                        lastReadIndex = when {
                            oldRead == null -> -1
                            readStillExists -> item.images.indexOfFirst { it.uri == oldRead }
                            else -> 0
                        },
                        hidden = old?.hidden ?: false,
                    )
                }
                val replacementIds = replacements.mapTo(hashSetOf(), AlbumEntity::id)
                val removed = existing.values.filter { it.id !in replacementIds }
                database.withTransaction {
                    if (removed.isNotEmpty()) dao.deleteAlbums(removed)
                    if (replacements.isNotEmpty()) dao.upsertAlbums(replacements)
                    replaceAlbumImages(scanned)
                    dao.markMountRefreshSuccess(mount.id, now)
                }
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                dao.markMountRefreshFailed(mount.id)
                throw error
            }
        }

    suspend fun reauthorizeMount(
        mountId: String,
        newTreeUri: String,
        onProgress: (ScanProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val oldMount = dao.mount(mountId) ?: error("挂载源不存在")
        val newTree = Uri.parse(newTreeUri)
        val newRootId = DocumentsContract.getTreeDocumentId(newTree)
        val oldTargetId = runCatching { DocumentsContract.getDocumentId(Uri.parse(oldMount.directoryUri)) }.getOrNull()
        val targetId = oldTargetId?.takeIf { it == newRootId || it.startsWith("$newRootId/") } ?: newRootId
        val newTargetUri = DocumentsContract.buildDocumentUriUsingTree(newTree, targetId)
        val newName = queryDocumentName(newTargetUri) ?: oldMount.name
        val scanned = try {
            scan(
                mountId,
                newTreeUri,
                newTargetUri.toString(),
                newName,
                runCatching { MountMode.valueOf(oldMount.mode) }.getOrDefault(MountMode.RECURSIVE),
                onProgress,
            )
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            dao.markMountRefreshFailed(mountId)
            throw error
        }
        persistFolderPermission(newTree)
        try {
            val existing = dao.albumsForMount(mountId).associateBy(AlbumEntity::id)
            val now = System.currentTimeMillis()
            val replacements = scanned.map { item ->
                val old = existing[item.id]
                val imageUris = item.images.mapTo(hashSetOf(), ImageRef::uri)
                item.toEntity(now).copy(
                    customName = old?.customName,
                    customCoverUri = old?.customCoverUri?.takeIf { it in imageUris },
                    lastReadUri = old?.lastReadUri?.takeIf { it in imageUris }
                        ?: old?.lastReadUri?.let { item.images.first().uri },
                    lastReadIndex = old?.lastReadUri?.takeIf { it in imageUris }
                        ?.let { target -> item.images.indexOfFirst { it.uri == target } }
                        ?: if (old?.lastReadUri == null) -1 else 0,
                    hidden = old?.hidden ?: false,
                )
            }
            val replacementIds = replacements.mapTo(hashSetOf(), AlbumEntity::id)
            val removed = existing.values.filter { it.id !in replacementIds }
            database.withTransaction {
                dao.upsertMount(
                    oldMount.copy(
                        treeUri = newTreeUri,
                        directoryUri = newTargetUri.toString(),
                        name = newName,
                        path = oldMount.path.ifBlank { newName },
                        available = true,
                        lastSuccessfulRefreshAt = now,
                        lastRefreshFailed = false,
                    ),
                )
                if (removed.isNotEmpty()) dao.deleteAlbums(removed)
                if (replacements.isNotEmpty()) dao.upsertAlbums(replacements)
                replaceAlbumImages(scanned)
            }
            if (oldMount.treeUri != newTreeUri && dao.mounts().none { it.id != mountId && it.treeUri == oldMount.treeUri }) {
                releasePermission(oldMount.treeUri)
            }
        } catch (error: Throwable) {
            if (newTreeUri != oldMount.treeUri) releasePermission(newTreeUri)
            if (error !is kotlinx.coroutines.CancellationException) dao.markMountRefreshFailed(mountId)
            throw error
        }
    }

    suspend fun openAlbum(
        albumId: String,
        onIndexBackfill: () -> Unit = {},
    ): Album = withContext(Dispatchers.IO) {
        val entity = dao.album(albumId) ?: error("画册不存在")
        val indexed = ensureImageIndex(entity, onIndexBackfill)
        val resolvedProgress = LibraryRules.resolveReadingIndex(entity.lastReadUri, indexed.images)
        Album(
            entity.id,
            entity.mountId,
            entity.customName ?: entity.name,
            entity.path,
            indexed.images,
            resolvedProgress,
            indexed.backfilled,
        )
    }

    suspend fun albumImages(albumId: String): List<ImageRef> = withContext(Dispatchers.IO) {
        val album = dao.album(albumId) ?: return@withContext emptyList()
        ensureImageIndex(album).images
    }

    suspend fun saveProgress(albumId: String, image: ImageRef, index: Int) = withContext(Dispatchers.IO) {
        dao.saveReadingPosition(albumId, image.uri, index.coerceAtLeast(0))
    }

    suspend fun removeImageIfMissing(albumId: String, imageUri: String): MissingImageCheck =
        withContext(Dispatchers.IO) {
            val album = dao.album(albumId) ?: return@withContext MissingImageCheck.ALBUM_REMOVED
            val exists = try {
                resolver.query(
                    Uri.parse(imageUri),
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                    null,
                    null,
                    null,
                )?.use { it.moveToFirst() } ?: false
            } catch (_: SecurityException) {
                dao.setMountAvailable(album.mountId, false)
                return@withContext MissingImageCheck.SOURCE_UNAVAILABLE
            } catch (_: FileNotFoundException) {
                false
            } catch (_: Throwable) {
                return@withContext MissingImageCheck.PRESENT_OR_UNREADABLE
            }
            if (exists) return@withContext MissingImageCheck.PRESENT_OR_UNREADABLE

            database.withTransaction {
                dao.deleteAlbumImage(albumId, imageUri)
                val remainingEntities = dao.imagesForAlbum(albumId)
                if (remainingEntities.isEmpty()) {
                    dao.deleteAlbums(listOf(album))
                    return@withTransaction MissingImageCheck.ALBUM_REMOVED
                }
                val remaining = remainingEntities.map { ImageRef(it.uri, it.name) }
                val remainingUris = remaining.mapTo(hashSetOf(), ImageRef::uri)
                val nextReadUri = album.lastReadUri?.takeIf { it in remainingUris }
                    ?: remaining.first().uri
                dao.upsertAlbums(
                    listOf(
                        album.copy(
                            defaultCoverUri = album.defaultCoverUri.takeIf { it in remainingUris }
                                ?: remaining.first().uri,
                            customCoverUri = album.customCoverUri?.takeIf { it in remainingUris },
                            pageCount = remaining.size,
                            lastReadUri = nextReadUri,
                            lastReadIndex = remaining.indexOfFirst { it.uri == nextReadUri },
                            updatedAt = System.currentTimeMillis(),
                        ),
                    ),
                )
                MissingImageCheck.REMOVED
            }
        }

    suspend fun setAlbumCover(albumId: String, uri: String?) = withContext(Dispatchers.IO) {
        val validUri = uri?.takeIf { candidate -> albumImages(albumId).any { it.uri == candidate } }
        dao.setAlbumCover(albumId, validUri, System.currentTimeMillis())
    }

    suspend fun renameAlbum(albumId: String, rawName: String?) = withContext(Dispatchers.IO) {
        val album = dao.album(albumId) ?: error("画册不存在")
        val customName = LibraryRules.normalizeAlbumDisplayName(rawName, album.name)
        dao.setAlbumCustomName(albumId, customName, System.currentTimeMillis())
    }

    suspend fun repairAlbumCover(albumId: String, uri: String) = withContext(Dispatchers.IO) {
        if (albumImages(albumId).any { it.uri == uri }) {
            dao.repairAlbumCover(albumId, uri, System.currentTimeMillis())
        }
    }

    suspend fun hideAlbum(albumId: String): Boolean = withContext(Dispatchers.IO) {
        val album = dao.album(albumId) ?: return@withContext false
        dao.setAlbumHidden(albumId, true)
        if (dao.visibleAlbumCount(album.mountId) == 0) {
            removeMount(album.mountId)
            true
        } else false
    }

    suspend fun restoreAlbum(albumId: String) = withContext(Dispatchers.IO) {
        dao.setAlbumHidden(albumId, false)
    }

    suspend fun removeMount(mountId: String) = withContext(Dispatchers.IO) {
        val mount = dao.mount(mountId) ?: return@withContext
        val sharedGrant = dao.mounts().any { it.id != mountId && it.treeUri == mount.treeUri }
        dao.deleteMount(mountId)
        if (!sharedGrant) releasePermission(mount.treeUri)
    }

    suspend fun createBookshelf(rawName: String): String = withContext(Dispatchers.IO) {
        val name = validateBookshelfName(rawName, null)
        val id = UUID.randomUUID().toString()
        dao.upsertBookshelf(BookshelfEntity(id, name, null, System.currentTimeMillis()))
        id
    }

    suspend fun renameBookshelf(id: String, rawName: String) = withContext(Dispatchers.IO) {
        val current = dao.bookshelf(id) ?: error("书架不存在")
        val name = validateBookshelfName(rawName, id)
        dao.upsertBookshelf(current.copy(name = name))
    }

    suspend fun deleteBookshelf(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBookshelf(id)
        if (preferences.getString(KEY_CURRENT_SHELF, null) == id) saveCurrentShelf(ALL_BOOKSHELF_ID)
    }

    suspend fun setBookshelfCover(bookshelfId: String, albumId: String?) = withContext(Dispatchers.IO) {
        val shelf = dao.bookshelf(bookshelfId) ?: error("书架不存在")
        if (albumId != null) {
            val album = dao.album(albumId) ?: error("画册不存在")
            require(!album.hidden && bookshelfId in dao.bookshelfIdsForAlbum(albumId)) { "画册不在这个书架中" }
        }
        dao.upsertBookshelf(shelf.copy(coverAlbumId = albumId))
    }

    suspend fun setAlbumBookshelves(albumId: String, shelfIds: Set<String>) = withContext(Dispatchers.IO) {
        val valid = dao.bookshelves().mapTo(hashSetOf(), BookshelfEntity::id)
        val selected = shelfIds.intersect(valid)
        database.withTransaction {
            dao.deleteMembershipsForAlbum(albumId)
            if (selected.isNotEmpty()) {
                dao.insertMemberships(selected.map { AlbumBookshelfEntity(albumId, it) })
            }
        }
    }

    suspend fun removeAlbumFromBookshelf(albumId: String, bookshelfId: String) = withContext(Dispatchers.IO) {
        if (bookshelfId != ALL_BOOKSHELF_ID) dao.deleteMembership(albumId, bookshelfId)
    }

    fun readerPreferences(): ReaderPreferences = ReaderPreferences(
        mode = runCatching { ReadingMode.valueOf(preferences.getString(KEY_READING_MODE, null).orEmpty()) }
            .getOrDefault(ReadingMode.PAGER),
        direction = runCatching { ReadingDirection.valueOf(preferences.getString(KEY_READING_DIRECTION, null).orEmpty()) }
            .getOrDefault(ReadingDirection.LEFT_TO_RIGHT),
    )

    fun saveReaderPreferences(value: ReaderPreferences) {
        preferences.edit()
            .putString(KEY_READING_MODE, value.mode.name)
            .putString(KEY_READING_DIRECTION, value.direction.name)
            .apply()
    }

    fun gridDensity(): GridDensity = runCatching {
        GridDensity.valueOf(preferences.getString(KEY_GRID_DENSITY, null).orEmpty())
    }.getOrDefault(GridDensity.STANDARD)

    fun saveGridDensity(value: GridDensity) {
        preferences.edit().putString(KEY_GRID_DENSITY, value.name).apply()
    }

    private suspend fun validateBookshelfName(rawName: String, currentId: String?): String {
        val existingNames = dao.bookshelves().filter { it.id != currentId }.map(BookshelfEntity::name)
        return LibraryRules.validateBookshelfName(rawName, existingNames)
    }

    private suspend fun ensureNotOverlapping(targetUri: String) {
        val target = Uri.parse(targetUri)
        val targetId = runCatching { DocumentsContract.getDocumentId(target) }.getOrNull()
        dao.mounts().forEach { existing ->
            if (existing.directoryUri == targetUri) error("这个目录已经挂载")
            val other = Uri.parse(existing.directoryUri)
            val otherId = runCatching { DocumentsContract.getDocumentId(other) }.getOrNull()
            if (target.authority == other.authority && targetId != null && otherId != null) {
                val overlaps = LibraryRules.documentIdsOverlap(targetId, otherId)
                require(!overlaps) { "这个目录与“${existing.name}”存在父子重叠" }
            }
        }
    }

    private suspend fun ensureImageIndex(
        album: AlbumEntity,
        onIndexBackfill: () -> Unit = {},
    ): IndexedImages {
        val stored = dao.imagesForAlbum(album.id)
            .map { ImageRef(it.uri, it.name) }
        if (stored.isNotEmpty()) return IndexedImages(stored, backfilled = false)

        onIndexBackfill()
        val mount = dao.mount(album.mountId) ?: error("挂载源不存在")
        try {
            val images = listImages(Uri.parse(mount.treeUri), Uri.parse(album.directoryUri))
            if (images.isEmpty()) {
                dao.deleteAlbums(listOf(album))
                throw EmptyAlbumException()
            }
            val uriSet = images.mapTo(hashSetOf(), ImageRef::uri)
            val resolvedProgress = LibraryRules.resolveReadingIndex(album.lastReadUri, images)
            val updated = album.copy(
                defaultCoverUri = images.first().uri,
                customCoverUri = album.customCoverUri?.takeIf { it in uriSet },
                pageCount = images.size,
                lastReadUri = when {
                    album.lastReadUri == null -> null
                    album.lastReadUri in uriSet -> album.lastReadUri
                    else -> images.first().uri
                },
                lastReadIndex = if (album.lastReadUri == null) -1 else resolvedProgress,
                updatedAt = System.currentTimeMillis(),
            )
            database.withTransaction {
                dao.upsertAlbums(listOf(updated))
                dao.upsertAlbumImages(images.mapIndexed { index, image ->
                    AlbumImageEntity(album.id, image.uri, image.name, index)
                })
                dao.setMountAvailable(mount.id, true)
            }
            return IndexedImages(images, backfilled = true)
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException || error is EmptyAlbumException) throw error
            dao.setMountAvailable(mount.id, false)
            throw error
        }
    }

    private suspend fun replaceAlbumImages(scanned: List<ScannedAlbum>) {
        if (scanned.isEmpty()) return
        val albumIds = scanned.map(ScannedAlbum::id)
        dao.deleteImagesForAlbums(albumIds)
        dao.upsertAlbumImages(scanned.flatMap(ScannedAlbum::toImageEntities))
    }

    private suspend fun scan(
        mountId: String,
        treeUri: String,
        rootUri: String,
        rootName: String,
        mode: MountMode,
        onProgress: (ScanProgress) -> Unit,
    ): List<ScannedAlbum> {
        val output = mutableListOf<ScannedAlbum>()
        var imagesFound = 0
        val visited = hashSetOf<String>()

        suspend fun visit(directoryUri: String, relativePath: String) {
            coroutineContext.ensureActive()
            if (!visited.add(directoryUri)) return
            val children = listChildren(Uri.parse(treeUri), Uri.parse(directoryUri))
            val images = children.filter { !it.isDirectory && it.isImage }
                .sortedWith { first, second -> NaturalOrder.compare(first.name, second.name) }
                .map { ImageRef(it.uri, it.name) }
            imagesFound += images.size
            val displayPath = if (relativePath.isBlank()) rootName else "$rootName/$relativePath"
            if (images.isNotEmpty()) {
                output += ScannedAlbum(
                    id = stableId("$mountId|${relativePath.ifBlank { "." }}"),
                    mountId = mountId,
                    directoryUri = directoryUri,
                    name = displayPath.substringAfterLast('/'),
                    path = displayPath,
                    images = images,
                )
            }
            onProgress(ScanProgress(displayPath, output.size, imagesFound))
            if (mode == MountMode.RECURSIVE) {
                children.filter(SafEntry::isDirectory)
                    .sortedWith { first, second -> NaturalOrder.compare(first.name, second.name) }
                    .forEach { child ->
                        val childPath = if (relativePath.isBlank()) child.name else "$relativePath/${child.name}"
                        visit(child.uri, childPath)
                    }
            }
        }

        visit(rootUri, "")
        return output.sortedWith { first, second -> NaturalOrder.compare(first.path, second.path) }
    }

    private fun listImages(treeUri: Uri, directoryUri: Uri): List<ImageRef> =
        listChildren(treeUri, directoryUri)
            .filter { !it.isDirectory && it.isImage }
            .sortedWith { first, second -> NaturalOrder.compare(first.name, second.name) }
            .map { ImageRef(it.uri, it.name) }

    private fun listChildren(treeUri: Uri, directoryUri: Uri): List<SafEntry> {
        val parentId = DocumentsContract.getDocumentId(directoryUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        return buildList {
            resolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty().ifBlank { "未命名" }
                    val mime = cursor.getString(mimeColumn).orEmpty()
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString()
                    val directory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    add(SafEntry(uri, name, directory, !directory && isImage(name, mime)))
                }
            } ?: error("目录无法读取")
        }
    }

    private fun queryDocumentName(documentUri: Uri): String? {
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return resolver.query(documentUri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getString(0)
        }
    }

    private fun persistFolderPermission(uri: Uri) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun releasePermission(uri: String) {
        runCatching {
            resolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun isImage(name: String, mime: String): Boolean =
        mime.lowercase(Locale.ROOT).startsWith("image/") ||
            name.substringAfterLast('.', "").lowercase(Locale.ROOT) in IMAGE_EXTENSIONS

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(12)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun AlbumEntity.toSummary(shelfIds: Set<String>) = AlbumSummary(
        id = id,
        mountId = mountId,
        name = customName ?: name,
        sourceName = name,
        path = path,
        directoryUri = directoryUri,
        coverUri = customCoverUri ?: defaultCoverUri,
        defaultCoverUri = defaultCoverUri,
        pageCount = pageCount,
        lastReadIndex = lastReadIndex,
        hidden = hidden,
        updatedAt = updatedAt,
        bookshelfIds = shelfIds,
    )

    private data class SafEntry(val uri: String, val name: String, val isDirectory: Boolean, val isImage: Boolean)

    private data class IndexedImages(val images: List<ImageRef>, val backfilled: Boolean)

    private class EmptyAlbumException : IllegalStateException("这个画册已经没有图片")

    private data class ScannedAlbum(
        val id: String,
        val mountId: String,
        val directoryUri: String,
        val name: String,
        val path: String,
        val images: List<ImageRef>,
    ) {
        fun toEntity(now: Long) = AlbumEntity(
            id = id,
            mountId = mountId,
            directoryUri = directoryUri,
            name = name,
            customName = null,
            path = path,
            defaultCoverUri = images.first().uri,
            customCoverUri = null,
            pageCount = images.size,
            lastReadUri = null,
            lastReadIndex = -1,
            hidden = false,
            updatedAt = now,
        )

        fun toImageEntities(): List<AlbumImageEntity> = images.mapIndexed { index, image ->
            AlbumImageEntity(id, image.uri, image.name, index)
        }
    }

    private companion object {
        const val PREFERENCES = "yomu_library"
        const val KEY_ROOM_INITIALIZED = "room_index_initialized_v1"
        const val KEY_LEGACY_MOUNTS = "mounted_folders"
        const val KEY_CURRENT_SHELF = "current_bookshelf"
        const val KEY_READING_MODE = "reading_mode"
        const val KEY_READING_DIRECTION = "reading_direction"
        const val KEY_GRID_DENSITY = "grid_density"
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
        val albumComparator = Comparator<AlbumSummary> { first, second ->
            NaturalOrder.compare(first.name, second.name).takeIf { it != 0 }
                ?: NaturalOrder.compare(first.path, second.path)
        }
    }
}
