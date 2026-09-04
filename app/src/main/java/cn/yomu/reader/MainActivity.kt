package cn.yomu.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.yomu.reader.ui.LibraryScreen
import cn.yomu.reader.ui.ReaderScreen
import cn.yomu.reader.ui.theme.YomuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YomuTheme {
                YomuApp(viewModel)
            }
        }
    }
}

@Composable
private fun YomuApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }
    var reauthorizeMountId by rememberSaveable { mutableStateOf<String?>(null) }
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val mountId = reauthorizeMountId
        reauthorizeMountId = null
        if (uri != null) {
            if (mountId == null) viewModel.beginMountSetup(uri)
            else viewModel.reauthorizeMount(mountId, uri)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        state.openAlbum?.let { album ->
            ReaderScreen(
                album = album,
                preferences = state.readerPreferences,
                resolver = resolver,
                onBack = viewModel::closeReader,
                onProgress = { viewModel.saveProgress(album.id, it) },
                onImageLoadFailed = { viewModel.imageLoadFailed(album.id, it) },
                onPreferencesChange = viewModel::setReaderPreferences,
            )
        } ?: LibraryScreen(
            library = state.library,
            initializing = state.initializing,
            resolver = resolver,
            mountBrowser = state.mountBrowser,
            scanProgress = state.scanProgress,
            scanTitle = state.scanTitle,
            openingAlbum = state.openingAlbum,
            coverPicker = state.coverPicker,
            onPickFolder = {
                reauthorizeMountId = null
                folderPicker.launch(null)
            },
            onSelectBookshelf = viewModel::selectBookshelf,
            onCreateBookshelf = viewModel::createBookshelf,
            onRenameBookshelf = viewModel::renameBookshelf,
            onDeleteBookshelf = viewModel::deleteBookshelf,
            onSetBookshelfCover = viewModel::setBookshelfCover,
            onOpenAlbum = viewModel::open,
            onSetAlbumBookshelves = viewModel::setAlbumBookshelves,
            onRemoveFromCurrentBookshelf = viewModel::removeFromCurrentBookshelf,
            onHideAlbum = viewModel::hideAlbum,
            onShowAlbumCoverPicker = viewModel::showAlbumCoverPicker,
            onSetAlbumCover = viewModel::setAlbumCover,
            onRepairAlbumCover = viewModel::repairAlbumCover,
            onDismissCoverPicker = viewModel::dismissCoverPicker,
            onEnterMountDirectory = viewModel::enterMountDirectory,
            onLeaveMountDirectory = viewModel::leaveMountDirectory,
            onSetMountMode = viewModel::setMountMode,
            onConfirmMount = viewModel::confirmMount,
            onCancelMount = viewModel::cancelMountSetup,
            onCancelScan = viewModel::cancelScan,
            onRefreshMount = viewModel::refreshMount,
            onRequestReauthorize = { mountId ->
                reauthorizeMountId = mountId
                folderPicker.launch(null)
            },
            onRefreshAll = viewModel::refreshAll,
            onRemoveMount = viewModel::removeMount,
            onRestoreAlbum = viewModel::restoreAlbum,
            onClearCache = viewModel::clearImageCache,
            onCancelOpen = viewModel::cancelOpenAlbum,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
