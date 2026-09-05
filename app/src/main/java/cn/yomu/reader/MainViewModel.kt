package cn.yomu.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.yomu.reader.data.LibraryRepository
import cn.yomu.reader.data.MissingImageCheck
import cn.yomu.reader.data.AlbumOpenFeedbackPolicy
import cn.yomu.reader.model.ALL_BOOKSHELF_ID
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.AlbumOpenFeedback
import cn.yomu.reader.model.AlbumSummary
import cn.yomu.reader.model.GridDensity
import cn.yomu.reader.model.ImageRef
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountBrowserState
import cn.yomu.reader.model.MountMode
import cn.yomu.reader.model.ReaderPreferences
import cn.yomu.reader.model.ScanProgress
import cn.yomu.reader.ui.LocalImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoverPickerState(
    val album: AlbumSummary,
    val images: List<ImageRef> = emptyList(),
    val loading: Boolean = true,
)

data class MainUiState(
    val library: LibrarySnapshot = LibrarySnapshot(),
    val initializing: Boolean = true,
    val scanProgress: ScanProgress? = null,
    val scanTitle: String? = null,
    val mountBrowser: MountBrowserState? = null,
    val openAlbum: Album? = null,
    val openingAlbum: AlbumSummary? = null,
    val openingFeedback: AlbumOpenFeedback = AlbumOpenFeedback.NONE,
    val coverPicker: CoverPickerState? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val gridDensity: GridDensity = GridDensity.STANDARD,
    val diskCacheBytes: Long = 0L,
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private var workJob: Job? = null
    private var latestPosition: Triple<String, ImageRef, Int>? = null
    private val checkedImageFailures = mutableSetOf<String>()
    private val _state = MutableStateFlow(
        MainUiState(
            readerPreferences = repository.readerPreferences(),
            gridDensity = repository.gridDensity(),
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
            reload()
            refreshCacheUsage()
            _state.update { it.copy(initializing = false) }
        }
    }

    fun beginMountSetup(uri: Uri) {
        workJob?.cancel()
        workJob = viewModelScope.launch {
            runCatching {
                val root = repository.rootDirectory(uri.toString())
                val browser = MountBrowserState(uri.toString(), root, loading = true)
                _state.update { it.copy(mountBrowser = browser) }
                val children = repository.childDirectories(uri.toString(), root)
                _state.update { state ->
                    state.copy(mountBrowser = state.mountBrowser?.copy(children = children, loading = false))
                }
            }.onFailure { error ->
                _state.update { it.copy(mountBrowser = null) }
                showError(error, "无法读取所选目录")
            }
        }
    }

    fun enterMountDirectory(child: cn.yomu.reader.model.DirectoryChoice) {
        val browser = _state.value.mountBrowser ?: return
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update { it.copy(mountBrowser = browser.copy(loading = true)) }
            runCatching { repository.childDirectories(browser.treeUri, child) }
                .onSuccess { children ->
                    _state.update {
                        it.copy(
                            mountBrowser = browser.copy(
                                current = child,
                                parents = browser.parents + browser.current,
                                children = children,
                                loading = false,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(mountBrowser = browser.copy(loading = false)) }
                    showError(error, "无法打开子目录")
                }
        }
    }

    fun leaveMountDirectory() {
        val browser = _state.value.mountBrowser ?: return
        val parent = browser.parents.lastOrNull() ?: return
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update { it.copy(mountBrowser = browser.copy(loading = true)) }
            runCatching { repository.childDirectories(browser.treeUri, parent) }
                .onSuccess { children ->
                    _state.update {
                        it.copy(
                            mountBrowser = browser.copy(
                                current = parent,
                                parents = browser.parents.dropLast(1),
                                children = children,
                                loading = false,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(mountBrowser = browser.copy(loading = false)) }
                    showError(error, "无法返回上级目录")
                }
        }
    }

    fun setMountMode(mode: MountMode) = _state.update { state ->
        state.copy(mountBrowser = state.mountBrowser?.copy(mode = mode))
    }

    fun cancelMountSetup() {
        workJob?.cancel()
        _state.update { it.copy(mountBrowser = null, scanProgress = null, scanTitle = null) }
    }

    fun confirmMount() {
        val browser = _state.value.mountBrowser ?: return
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    mountBrowser = null,
                    scanTitle = "正在挂载 ${browser.current.name}",
                    scanProgress = ScanProgress(browser.current.path, 0, 0),
                )
            }
            try {
                repository.addMount(browser.treeUri, browser.current, browser.mode) { progress ->
                    _state.update { it.copy(scanProgress = progress) }
                }
                reload()
                _state.update { it.copy(message = "${browser.current.name} 已挂载") }
            } catch (_: CancellationException) {
                _state.update { it.copy(message = "已取消挂载") }
            } catch (error: Throwable) {
                showError(error, "挂载失败，未保存任何扫描结果")
            } finally {
                _state.update { it.copy(scanProgress = null, scanTitle = null) }
            }
        }
    }

    fun cancelScan() {
        workJob?.cancel()
        _state.update { it.copy(scanProgress = null, scanTitle = null) }
    }

    fun selectBookshelf(id: String) = viewModelScope.launch {
        cancelOpenAlbum()
        repository.saveCurrentShelf(id)
        reload(id)
    }.let { Unit }

    fun createBookshelf(name: String) = mutate("书架已创建") { repository.createBookshelf(name) }

    fun renameBookshelf(id: String, name: String) = mutate("书架已重命名") {
        repository.renameBookshelf(id, name)
    }

    fun deleteBookshelf(id: String) = mutate("书架已删除") { repository.deleteBookshelf(id) }

    fun setBookshelfCover(bookshelfId: String, albumId: String?) = mutate("书架封面已更新") {
        repository.setBookshelfCover(bookshelfId, albumId)
    }

    fun setAlbumBookshelves(albumId: String, shelfIds: Set<String>) = mutate("所属书架已更新") {
        repository.setAlbumBookshelves(albumId, shelfIds)
    }

    fun renameAlbum(albumId: String, name: String?) = mutate("画册名称已更新") {
        repository.renameAlbum(albumId, name)
    }

    fun removeFromCurrentBookshelf(albumId: String) {
        val shelfId = _state.value.library.currentBookshelfId
        if (shelfId == ALL_BOOKSHELF_ID) return
        mutate("已移出当前书架") { repository.removeAlbumFromBookshelf(albumId, shelfId) }
    }

    fun hideAlbum(albumId: String) = mutate(null) {
        val removedMount = repository.hideAlbum(albumId)
        _state.update {
            it.copy(message = if (removedMount) "挂载源内已无可见画册，已自动取消挂载" else "画册已隐藏")
        }
    }

    fun restoreAlbum(albumId: String) = mutate("画册已恢复") { repository.restoreAlbum(albumId) }

    fun removeMount(mountId: String) = mutate("已取消挂载，设备图片未被删除") {
        repository.removeMount(mountId)
    }

    fun refreshMount(mountId: String) = runScan("正在刷新挂载源") {
        repository.refreshMount(mountId, it)
    }

    fun reauthorizeMount(mountId: String, uri: Uri) = runScan("正在重新授权挂载源") {
        repository.reauthorizeMount(mountId, uri.toString(), it)
    }

    fun refreshAll() {
        val mounts = _state.value.library.mounts
        runScan("正在刷新全部挂载源") { progress ->
            val failures = mutableListOf<String>()
            mounts.forEach { mount ->
                try {
                    repository.refreshMount(mount.id, progress)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    failures += mount.name
                }
            }
            if (failures.isNotEmpty()) error("以下挂载源刷新失败：${failures.joinToString("、")}")
        }
    }

    fun open(album: AlbumSummary) {
        if (_state.value.openingAlbum != null) return
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update { it.copy(openingAlbum = album, openingFeedback = AlbumOpenFeedback.NONE) }
            val feedbackJob = launch {
                delay(AlbumOpenFeedbackPolicy.DELAY_MILLIS)
                _state.update { state ->
                    if (state.openingAlbum?.id == album.id && state.openingFeedback == AlbumOpenFeedback.NONE) {
                        state.copy(openingFeedback = AlbumOpenFeedbackPolicy.feedback(AlbumOpenFeedbackPolicy.DELAY_MILLIS))
                    } else state
                }
            }
            try {
                val detail = repository.openAlbum(album.id) {
                    feedbackJob.cancel()
                    _state.update { state ->
                        if (state.openingAlbum?.id == album.id) {
                            state.copy(openingFeedback = AlbumOpenFeedbackPolicy.feedback(0, indexBackfill = true))
                        } else state
                    }
                }
                _state.update {
                    it.copy(
                        openAlbum = detail,
                        openingAlbum = null,
                        openingFeedback = AlbumOpenFeedback.NONE,
                        message = if (detail.indexBackfilled) "画册索引已补全，之后打开将直接加载" else it.message,
                    )
                }
            } catch (_: CancellationException) {
                _state.update { it.copy(openingAlbum = null, openingFeedback = AlbumOpenFeedback.NONE) }
            } catch (error: Throwable) {
                reload()
                _state.update { it.copy(openingAlbum = null, openingFeedback = AlbumOpenFeedback.NONE) }
                showError(error, "无法打开画册")
            } finally {
                feedbackJob.cancel()
            }
        }
    }

    fun cancelOpenAlbum() {
        workJob?.cancel()
        _state.update { it.copy(openingAlbum = null, openingFeedback = AlbumOpenFeedback.NONE) }
    }

    fun closeReader() {
        _state.update { it.copy(openAlbum = null) }
        checkedImageFailures.clear()
        val pending = latestPosition
        latestPosition = null
        viewModelScope.launch {
            pending?.let { (albumId, image, page) -> repository.saveProgress(albumId, image, page) }
            reload()
        }
    }

    fun saveProgress(albumId: String, page: Int) {
        val album = _state.value.openAlbum ?: return
        val image = album.images.getOrNull(page) ?: return
        latestPosition = Triple(albumId, image, page)
        viewModelScope.launch { repository.saveProgress(albumId, image, page) }
    }

    fun imageLoadFailed(albumId: String, image: ImageRef) {
        val checkKey = "$albumId|${image.uri}"
        if (!checkedImageFailures.add(checkKey)) return
        viewModelScope.launch {
            when (repository.removeImageIfMissing(albumId, image.uri)) {
                MissingImageCheck.PRESENT_OR_UNREADABLE -> Unit
                MissingImageCheck.REMOVED -> {
                    if (latestPosition?.second?.uri == image.uri) latestPosition = null
                    _state.update { state ->
                        val current = state.openAlbum?.takeIf { it.id == albumId }
                        val remaining = current?.images?.filterNot { it.uri == image.uri }.orEmpty()
                        state.copy(
                            openAlbum = current?.copy(
                                images = remaining,
                                progress = current.progress.coerceAtMost(remaining.lastIndex),
                            ) ?: state.openAlbum,
                            message = "图片已从设备移除，已跳过；刷新挂载源可同步其他变更",
                        )
                    }
                    reload()
                }
                MissingImageCheck.ALBUM_REMOVED -> {
                    latestPosition = null
                    _state.update {
                        it.copy(openAlbum = null, message = "画册已无可用图片，已从书库移除")
                    }
                    reload()
                }
                MissingImageCheck.SOURCE_UNAVAILABLE -> {
                    _state.update { it.copy(message = "挂载源暂时无法访问，请在挂载管理中重新授权") }
                    reload()
                }
            }
        }
    }

    fun showAlbumCoverPicker(album: AlbumSummary) {
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update { it.copy(coverPicker = CoverPickerState(album)) }
            runCatching { repository.albumImages(album.id) }
                .onSuccess { images ->
                    _state.update { it.copy(coverPicker = CoverPickerState(album, images, false)) }
                }
                .onFailure {
                    _state.update { it.copy(coverPicker = null) }
                    showError(it, "无法读取封面候选图片")
                }
        }
    }

    fun dismissCoverPicker() = _state.update { it.copy(coverPicker = null) }

    fun setAlbumCover(albumId: String, uri: String?) {
        _state.update { it.copy(coverPicker = null) }
        mutate("画册封面已更新") { repository.setAlbumCover(albumId, uri) }
    }

    fun repairAlbumCover(albumId: String) {
        viewModelScope.launch {
            val images = runCatching { repository.albumImages(albumId) }.getOrNull() ?: return@launch
            val resolver = getApplication<Application>().contentResolver
            var candidate: ImageRef? = null
            for (image in images) {
                val readable = LocalImageLoader.load(
                    getApplication(),
                    resolver,
                    image.uri,
                    640,
                    diskCache = true,
                    version = System.currentTimeMillis(),
                ) != null
                if (readable) {
                    candidate = image
                    break
                }
            }
            val selected = candidate ?: return@launch
            repository.repairAlbumCover(albumId, selected.uri)
            reload()
        }
    }

    fun setReaderPreferences(value: ReaderPreferences) {
        repository.saveReaderPreferences(value)
        _state.update { it.copy(readerPreferences = value) }
    }

    fun setGridDensity(value: GridDensity) {
        repository.saveGridDensity(value)
        _state.update { it.copy(gridDensity = value) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun clearImageCache() {
        viewModelScope.launch {
            LocalImageLoader.clear(getApplication())
            _state.update { it.copy(message = "图片缓存已清除", diskCacheBytes = 0L) }
        }
    }

    private fun refreshCacheUsage() {
        viewModelScope.launch {
            val bytes = LocalImageLoader.diskCacheBytes(getApplication())
            _state.update { it.copy(diskCacheBytes = bytes) }
        }
    }

    private fun runScan(title: String, block: suspend ((ScanProgress) -> Unit) -> Unit) {
        workJob?.cancel()
        workJob = viewModelScope.launch {
            _state.update { it.copy(scanTitle = title, scanProgress = ScanProgress("准备中", 0, 0)) }
            try {
                block { progress -> _state.update { it.copy(scanProgress = progress) } }
                reload()
                _state.update { it.copy(message = "刷新完成") }
            } catch (_: CancellationException) {
                _state.update { it.copy(message = "已取消刷新，原索引保持不变") }
            } catch (error: Throwable) {
                reload()
                showError(error, "刷新失败，原索引保持不变")
            } finally {
                _state.update { it.copy(scanTitle = null, scanProgress = null) }
            }
        }
    }

    private fun mutate(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    reload()
                    if (successMessage != null) _state.update { it.copy(message = successMessage) }
                }
                .onFailure { showError(it, "操作失败") }
        }
    }

    private suspend fun reload(shelfId: String? = null) {
        val snapshot = repository.loadLibrary(shelfId)
        _state.update { it.copy(library = snapshot) }
    }

    private fun showError(error: Throwable, fallback: String) {
        if (error is CancellationException) return
        val detail = error.message?.takeIf(String::isNotBlank)
        _state.update { it.copy(message = if (detail == null) fallback else "$fallback：$detail") }
    }
}
