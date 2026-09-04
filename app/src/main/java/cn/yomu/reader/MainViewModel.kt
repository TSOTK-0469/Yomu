package cn.yomu.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.yomu.reader.data.LibraryRepository
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.ReaderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class MainUiState(
    val library: LibrarySnapshot = LibrarySnapshot(),
    val isScanning: Boolean = true,
    val openAlbum: Album? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private var scanJob: Job? = null
    private val _state = MutableStateFlow(
        MainUiState(readerPreferences = repository.readerPreferences()),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun addMount(uri: Uri) {
        val permissionPersisted = runCatching {
            repository.persistFolderPermission(uri)
        }.isSuccess
        repository.addMount(uri)
        if (!permissionPersisted) {
            _state.update { state ->
                state.copy(message = "目录已挂载，但此文件提供方可能要求重启后重新授权")
            }
        }
        refresh()
    }

    fun removeMount(uri: String) {
        repository.removeMount(uri)
        refresh()
    }

    fun refresh() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            val snapshot = repository.scanLibrary()
            _state.update { current ->
                current.copy(
                    library = snapshot,
                    isScanning = false,
                    openAlbum = current.openAlbum?.let { open ->
                        snapshot.albums.firstOrNull { it.id == open.id } ?: open
                    },
                )
            }
        }
    }

    fun open(album: Album) {
        _state.update { it.copy(openAlbum = album) }
    }

    fun closeReader() {
        _state.update { current ->
            val open = current.openAlbum
            val updatedAlbums = if (open == null) current.library.albums else {
                current.library.albums.map { album ->
                    if (album.id == open.id) album.copy(progress = repository.progressFor(open.id)) else album
                }
            }
            current.copy(
                openAlbum = null,
                library = current.library.copy(albums = updatedAlbums),
            )
        }
    }

    fun saveProgress(albumId: String, page: Int) = repository.saveProgress(albumId, page)

    fun setReaderPreferences(value: ReaderPreferences) {
        repository.saveReaderPreferences(value)
        _state.update { it.copy(readerPreferences = value) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
