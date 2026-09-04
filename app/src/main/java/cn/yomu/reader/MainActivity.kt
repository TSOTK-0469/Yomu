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
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.addMount(uri)
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
                onPreferencesChange = viewModel::setReaderPreferences,
            )
        } ?: LibraryScreen(
            library = state.library,
            isScanning = state.isScanning,
            resolver = resolver,
            onPickFolder = { folderPicker.launch(null) },
            onRefresh = viewModel::refresh,
            onOpenAlbum = viewModel::open,
            onRemoveMount = viewModel::removeMount,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
