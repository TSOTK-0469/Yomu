package cn.yomu.reader.ui

import android.content.ContentResolver
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cn.yomu.reader.BuildConfig
import cn.yomu.reader.CoverPickerState
import cn.yomu.reader.model.ALL_BOOKSHELF_ID
import cn.yomu.reader.model.AlbumSummary
import cn.yomu.reader.model.AlbumOpenFeedback
import cn.yomu.reader.model.Bookshelf
import cn.yomu.reader.model.DirectoryChoice
import cn.yomu.reader.model.GridDensity
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountBrowserState
import cn.yomu.reader.model.MountMode
import cn.yomu.reader.model.MountedFolder
import cn.yomu.reader.model.ScanProgress
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class LibraryDestination { LIBRARY, MOUNTS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    library: LibrarySnapshot,
    initializing: Boolean,
    resolver: ContentResolver,
    mountBrowser: MountBrowserState?,
    scanProgress: ScanProgress?,
    scanTitle: String?,
    openingAlbum: AlbumSummary?,
    openingFeedback: AlbumOpenFeedback,
    gridDensity: GridDensity,
    diskCacheBytes: Long,
    coverPicker: CoverPickerState?,
    onPickFolder: () -> Unit,
    onSelectBookshelf: (String) -> Unit,
    onCreateBookshelf: (String) -> Unit,
    onRenameBookshelf: (String, String) -> Unit,
    onDeleteBookshelf: (String) -> Unit,
    onSetBookshelfCover: (String, String?) -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onRenameAlbum: (String, String?) -> Unit,
    onSetAlbumBookshelves: (String, Set<String>) -> Unit,
    onRemoveFromCurrentBookshelf: (String) -> Unit,
    onHideAlbum: (String) -> Unit,
    onShowAlbumCoverPicker: (AlbumSummary) -> Unit,
    onSetAlbumCover: (String, String?) -> Unit,
    onRepairAlbumCover: (String) -> Unit,
    onDismissCoverPicker: () -> Unit,
    onEnterMountDirectory: (DirectoryChoice) -> Unit,
    onLeaveMountDirectory: () -> Unit,
    onSetMountMode: (MountMode) -> Unit,
    onConfirmMount: () -> Unit,
    onCancelMount: () -> Unit,
    onCancelScan: () -> Unit,
    onRefreshMount: (String) -> Unit,
    onRequestReauthorize: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onRemoveMount: (String) -> Unit,
    onRestoreAlbum: (String) -> Unit,
    onClearCache: () -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
    onCancelOpen: () -> Unit,
) {
    val activity = LocalActivity.current
    val darkTheme = isSystemInDarkTheme()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var destinationName by rememberSaveable { mutableStateOf(LibraryDestination.LIBRARY.name) }
    val destination = LibraryDestination.valueOf(destinationName)
    var query by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<AlbumSummary?>(null) }
    var renamingAlbum by remember { mutableStateOf<AlbumSummary?>(null) }
    var membershipAlbum by remember { mutableStateOf<AlbumSummary?>(null) }
    var selectedMemberships by remember { mutableStateOf<Set<String>>(emptySet()) }
    var shelfMenu by remember { mutableStateOf<Bookshelf?>(null) }
    var shelfCoverPicker by remember { mutableStateOf<Bookshelf?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var editingShelf by remember { mutableStateOf<Bookshelf?>(null) }
    var shelfName by remember { mutableStateOf("") }
    var deleteShelf by remember { mutableStateOf<Bookshelf?>(null) }
    var removeMount by remember { mutableStateOf<MountedFolder?>(null) }
    var confirmRefreshAll by remember { mutableStateOf(false) }
    var pathDialog by remember { mutableStateOf<MountedFolder?>(null) }

    BackHandler(destination != LibraryDestination.LIBRARY) {
        onCancelOpen()
        destinationName = LibraryDestination.LIBRARY.name
    }

    val currentShelf = library.bookshelves.firstOrNull { it.id == library.currentBookshelfId }
        ?: Bookshelf(ALL_BOOKSHELF_ID, "全部画册", null, 0L, library.allAlbums.size, true)
    val albums = remember(library.albums, query) {
        if (query.isBlank()) library.albums else library.albums.filter {
            it.name.contains(query, true) || it.sourceName.contains(query, true) || it.path.contains(query, true)
        }
    }

    DisposableEffect(darkTheme) {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        onDispose { }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = scanProgress == null && destination == LibraryDestination.LIBRARY,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Yomu",
                        modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 18.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("书架", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = {
                            editingShelf = null
                            shelfName = ""
                            showNameDialog = true
                        }) { Icon(Icons.Outlined.Add, "新建书架") }
                    }
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(library.bookshelves, key = Bookshelf::id) { shelf ->
                            val selected = shelf.id == library.currentBookshelfId
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                                    .combinedClickable(
                                        onClick = {
                                            onSelectBookshelf(shelf.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = { if (!shelf.builtIn) shelfMenu = shelf },
                                    ),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (shelf.coverUri == null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(40.dp),
                                        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bookmarks, null) } }
                                    } else MiniCover(resolver, shelf.coverUri, shelf.name, shelf.coverVersion)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(shelf.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                        Text("${shelf.albumCount} 个画册", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!shelf.builtIn) {
                                        IconButton(onClick = { shelfMenu = shelf }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Outlined.MoreVert, "管理 ${shelf.name}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("挂载管理") }, selected = false,
                        onClick = {
                            onCancelOpen()
                            destinationName = LibraryDestination.MOUNTS.name
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.Storage, null) }, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("设置") }, selected = false,
                        onClick = {
                            onCancelOpen()
                            destinationName = LibraryDestination.SETTINGS.name
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.Settings, null) }, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp))
                }
            }
        },
    ) {
        when (destination) {
            LibraryDestination.LIBRARY -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LibraryHeader(
                    shelf = currentShelf,
                    mountCount = library.mounts.size,
                    onMenu = { scope.launch { drawerState.open() } },
                )
            },
            floatingActionButton = {
                FilledIconButton(onClick = { onCancelOpen(); onPickFolder() }, modifier = Modifier.size(58.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "挂载文件夹")
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (library.allAlbums.isNotEmpty()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            AnimatedVisibility(query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "清空搜索") }
                            }
                        },
                        placeholder = { Text("搜索当前书架") },
                    )
                }
                when {
                    initializing -> CenterLoading("正在读取本地索引")
                    library.mounts.isEmpty() -> EmptyLibrary(onPickFolder)
                    albums.isEmpty() -> EmptyShelf(
                        name = currentShelf.name,
                        filtering = query.isNotBlank(),
                        allBookshelf = currentShelf.id == ALL_BOOKSHELF_ID,
                        onAction = {
                            when {
                                query.isNotBlank() -> query = ""
                                currentShelf.id != ALL_BOOKSHELF_ID -> onSelectBookshelf(ALL_BOOKSHELF_ID)
                                else -> destinationName = LibraryDestination.MOUNTS.name
                            }
                        },
                    )
                    else -> AlbumGrid(
                        albums = albums,
                        resolver = resolver,
                        density = gridDensity,
                        openingAlbumId = openingAlbum?.id,
                        openingFeedback = openingFeedback,
                        onOpenAlbum = onOpenAlbum,
                        onLongPress = { selectedAlbum = it },
                        onCoverFailed = onRepairAlbumCover,
                    )
                }
            }
            }
            LibraryDestination.MOUNTS -> MountManagerScreen(
                library = library,
                onBack = {
                    onCancelOpen()
                    destinationName = LibraryDestination.LIBRARY.name
                },
                onAdd = onPickFolder,
                onRefresh = onRefreshMount,
                onReauthorize = onRequestReauthorize,
                onRefreshAll = { confirmRefreshAll = true },
                onRemove = { removeMount = it },
                onRestore = onRestoreAlbum,
                onShowPath = { pathDialog = it },
            )
            LibraryDestination.SETTINGS -> SettingsScreen(
                density = gridDensity,
                diskCacheBytes = diskCacheBytes,
                onDensityChange = onGridDensityChange,
                onClearCache = onClearCache,
                onBack = {
                    onCancelOpen()
                    destinationName = LibraryDestination.LIBRARY.name
                },
            )
        }
    }

    selectedAlbum?.let { album ->
        ModalBottomSheet(onDismissRequest = { selectedAlbum = null }) {
            Text(album.name, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            ListItem(
                headlineContent = { Text("重命名") },
                supportingContent = { Text("仅修改 Yomu 中的显示名称") },
                leadingContent = { Icon(Icons.Outlined.Edit, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    renamingAlbum = album
                    selectedAlbum = null
                }),
            )
            ListItem(
                headlineContent = { Text("管理所属书架") },
                leadingContent = { Icon(Icons.Outlined.Bookmarks, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    selectedMemberships = album.bookshelfIds
                    membershipAlbum = album
                    selectedAlbum = null
                }),
            )
            if (library.currentBookshelfId != ALL_BOOKSHELF_ID) {
                ListItem(
                    headlineContent = { Text("移出当前书架") },
                    leadingContent = { Icon(Icons.Outlined.Close, null) },
                    modifier = Modifier.combinedClickable(onClick = {
                        onRemoveFromCurrentBookshelf(album.id)
                        selectedAlbum = null
                    }),
                )
            }
            ListItem(
                headlineContent = { Text("设置画册封面") },
                leadingContent = { Icon(Icons.Outlined.Image, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    onShowAlbumCoverPicker(album)
                    selectedAlbum = null
                }),
            )
            ListItem(
                headlineContent = { Text("隐藏画册") },
                supportingContent = { Text("不会删除设备上的图片") },
                leadingContent = { Icon(Icons.Outlined.HideImage, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    onHideAlbum(album.id)
                    selectedAlbum = null
                }),
            )
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }

    renamingAlbum?.let { album ->
        AlbumRenameDialog(
            album = album,
            onDismiss = { renamingAlbum = null },
            onSave = { name ->
                onRenameAlbum(album.id, name)
                renamingAlbum = null
            },
            onRestore = {
                onRenameAlbum(album.id, null)
                renamingAlbum = null
            },
        )
    }

    membershipAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { membershipAlbum = null },
            title = { Text("管理所属书架") },
            text = {
                Column {
                    if (library.bookshelves.none { !it.builtIn }) Text("还没有自建书架")
                    library.bookshelves.filterNot(Bookshelf::builtIn).forEach { shelf ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick = {
                                selectedMemberships = if (shelf.id in selectedMemberships) {
                                    selectedMemberships - shelf.id
                                } else selectedMemberships + shelf.id
                            }).padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = shelf.id in selectedMemberships,
                                onCheckedChange = { checked ->
                                    selectedMemberships = if (checked) selectedMemberships + shelf.id
                                    else selectedMemberships - shelf.id
                                },
                            )
                            Text(shelf.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetAlbumBookshelves(album.id, selectedMemberships)
                    membershipAlbum = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { membershipAlbum = null }) { Text("取消") } },
        )
    }
    shelfMenu?.let { shelf ->
        ModalBottomSheet(onDismissRequest = { shelfMenu = null }) {
            Text(shelf.name, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            ListItem(
                headlineContent = { Text("重命名") },
                leadingContent = { Icon(Icons.Outlined.Edit, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    editingShelf = shelf
                    shelfName = shelf.name
                    showNameDialog = true
                    shelfMenu = null
                }),
            )
            ListItem(
                headlineContent = { Text("设置书架封面") },
                leadingContent = { Icon(Icons.Outlined.Image, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    shelfCoverPicker = shelf
                    shelfMenu = null
                }),
            )
            ListItem(
                headlineContent = { Text("删除书架") },
                supportingContent = { Text("画册和原始图片都会保留") },
                leadingContent = { Icon(Icons.Outlined.Delete, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    deleteShelf = shelf
                    shelfMenu = null
                }),
            )
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(if (editingShelf == null) "新建书架" else "重命名书架") },
            text = {
                OutlinedTextField(
                    value = shelfName,
                    onValueChange = { if (it.length <= 40) shelfName = it },
                    label = { Text("书架名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = shelfName.isNotBlank(),
                    onClick = {
                        editingShelf?.let { onRenameBookshelf(it.id, shelfName) }
                            ?: onCreateBookshelf(shelfName)
                        showNameDialog = false
                    },
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("取消") } },
        )
    }

    deleteShelf?.let { shelf ->
        AlertDialog(
            onDismissRequest = { deleteShelf = null },
            title = { Text("删除“${shelf.name}”？") },
            text = { Text("只会删除书架和分类关系，画册仍保留在“全部画册”中。") },
            confirmButton = {
                TextButton(onClick = { onDeleteBookshelf(shelf.id); deleteShelf = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteShelf = null }) { Text("取消") } },
        )
    }

    shelfCoverPicker?.let { shelf ->
        val candidates = library.allAlbums.filter { shelf.id in it.bookshelfIds }
        ModalBottomSheet(onDismissRequest = { shelfCoverPicker = null }) {
            Text("设置“${shelf.name}”封面", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { onSetBookshelfCover(shelf.id, null); shelfCoverPicker = null }) {
                Text("恢复默认封面")
            }
            LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
                items(candidates, key = AlbumSummary::id) { album ->
                    ListItem(
                        headlineContent = { Text(album.name) },
                        leadingContent = { MiniCover(resolver, album.coverUri, album.name, album.updatedAt) },
                        modifier = Modifier.combinedClickable(onClick = {
                            onSetBookshelfCover(shelf.id, album.id)
                            shelfCoverPicker = null
                        }),
                    )
                }
            }
        }
    }

    coverPicker?.let { picker ->
        ModalBottomSheet(onDismissRequest = onDismissCoverPicker) {
            Text("设置画册封面", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { onSetAlbumCover(picker.album.id, null) }) { Text("恢复首张图片") }
            if (picker.loading) CenterLoading("正在读取图片")
            else LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.fillMaxWidth().height(470.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(picker.images, key = { it.uri }) { image ->
                    Surface(
                        onClick = { onSetAlbumCover(picker.album.id, image.uri) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(132.dp)) {
                                Thumbnail(resolver, image.uri, image.name, 0L)
                            }
                            Text(image.name, Modifier.padding(6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    removeMount?.let { mount ->
        AlertDialog(
            onDismissRequest = { removeMount = null },
            title = { Text("取消挂载“${mount.name}”？") },
            text = { Text("将移除 ${mount.albumCount} 个画册的分类、封面和进度，但不会删除设备图片。") },
            confirmButton = {
                TextButton(onClick = { onRemoveMount(mount.id); removeMount = null }) {
                    Text("取消挂载")
                }
            },
            dismissButton = { TextButton(onClick = { removeMount = null }) { Text("保留") } },
        )
    }

    if (confirmRefreshAll) {
        AlertDialog(
            onDismissRequest = { confirmRefreshAll = false },
            title = { Text("刷新全部挂载源？") },
            text = { Text("大型目录可能需要较长时间。每个挂载源只有完整扫描成功后才会更新。") },
            confirmButton = {
                TextButton(onClick = { confirmRefreshAll = false; onRefreshAll() }) { Text("刷新") }
            },
            dismissButton = { TextButton(onClick = { confirmRefreshAll = false }) { Text("取消") } },
        )
    }

    pathDialog?.let { mount ->
        AlertDialog(
            onDismissRequest = { pathDialog = null },
            title = { Text(mount.name) },
            text = { Text(mount.path.ifBlank { "尚未记录路径" }) },
            confirmButton = {
                TextButton(
                    enabled = mount.path.isNotBlank(),
                    onClick = {
                        clipboard.setText(AnnotatedString(mount.path))
                        pathDialog = null
                    },
                ) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("复制路径") }
            },
            dismissButton = { TextButton(onClick = { pathDialog = null }) { Text("关闭") } },
        )
    }

    mountBrowser?.let { browser ->
        ModalBottomSheet(onDismissRequest = onCancelMount) {
            MountBrowser(
                browser = browser,
                onEnter = onEnterMountDirectory,
                onBack = onLeaveMountDirectory,
                onMode = onSetMountMode,
                onConfirm = onConfirmMount,
                onCancel = onCancelMount,
            )
        }
    }

    scanProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(scanTitle ?: "正在扫描") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(progress.currentPath, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "已发现 ${progress.albumsFound} 个画册 · ${progress.imagesFound} 张图片",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onCancelScan) { Text("取消") } },
        )
    }

    if (openingFeedback == AlbumOpenFeedback.INDEX_BACKFILL) openingAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在打开 ${album.name}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("正在首次建立画册索引…")
                }
            },
            confirmButton = { TextButton(onClick = onCancelOpen) { Text("取消") } },
        )
    }
}

@Composable
private fun LibraryHeader(shelf: Bookshelf, mountCount: Int, onMenu: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().padding(WindowInsets.statusBars.asPaddingValues())
                .padding(start = 6.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Outlined.Menu, "打开书架侧栏") }
            Column(Modifier.weight(1f)) {
                Text(shelf.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    if (mountCount == 0) "你的本地图片阅读器" else "${shelf.albumCount} 个画册 · $mountCount 个挂载源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGrid(
    albums: List<AlbumSummary>,
    resolver: ContentResolver,
    density: GridDensity,
    openingAlbumId: String?,
    openingFeedback: AlbumOpenFeedback,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onLongPress: (AlbumSummary) -> Unit,
    onCoverFailed: (String) -> Unit,
) {
    val targetWidth = when (density) {
        GridDensity.COMFORTABLE -> 154.dp
        GridDensity.STANDARD -> 100.dp
        GridDensity.COMPACT -> 74.dp
    }
    val spacing = when (density) {
        GridDensity.COMFORTABLE -> 14.dp
        GridDensity.STANDARD -> 10.dp
        GridDensity.COMPACT -> 8.dp
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(targetWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 8.dp,
            end = 18.dp,
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(if (density == GridDensity.COMPACT) 12.dp else 18.dp),
    ) {
        items(albums, key = AlbumSummary::id) { album ->
            Column(
                Modifier.combinedClickable(
                    onClick = { onOpenAlbum(album) },
                    onLongClick = { onLongPress(album) },
                ),
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(154f / 214f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Thumbnail(
                        resolver,
                        album.coverUri,
                        album.name,
                        album.updatedAt,
                        onFailure = { onCoverFailed(album.id) },
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.68f),
                        contentColor = Color.White,
                    ) {
                        val current = if (album.lastReadIndex < 0) 0 else album.lastReadIndex + 1
                        Text(
                            "$current/${album.pageCount}",
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { album.progressFraction },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                    )
                    if (openingAlbumId == album.id && openingFeedback == AlbumOpenFeedback.CARD) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                        }
                    }
                }
                Spacer(Modifier.height(if (density == GridDensity.COMPACT) 6.dp else 9.dp))
                Text(album.name, Modifier.padding(horizontal = 2.dp), fontWeight = FontWeight.SemiBold,
                    style = if (density == GridDensity.COMPACT) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (density != GridDensity.COMPACT) {
                    Text(
                        if (density == GridDensity.COMFORTABLE) "${album.pageCount} 页 · ${album.path}" else "${album.pageCount} 页",
                        Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(
    resolver: ContentResolver,
    uri: String,
    description: String,
    version: Long,
    onFailure: (() -> Unit)? = null,
) {
    val state by rememberBitmap(resolver, uri, 640, diskCache = true, version = version)
    var failureReported by remember(uri, version) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == BitmapLoadState.Failed && !failureReported) {
            failureReported = true
            onFailure?.invoke()
        }
    }
    when (val value = state) {
        BitmapLoadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        BitmapLoadState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is BitmapLoadState.Ready -> Image(
            value.bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MiniCover(resolver: ContentResolver, uri: String, description: String, version: Long = 0L) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))) { Thumbnail(resolver, uri, description, version) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MountManagerScreen(
    library: LibrarySnapshot,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onRefresh: (String) -> Unit,
    onReauthorize: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onRemove: (MountedFolder) -> Unit,
    onRestore: (String) -> Unit,
    onShowPath: (MountedFolder) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("挂载管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = onRefreshAll, enabled = library.mounts.isNotEmpty()) {
                        Icon(Icons.Outlined.Cached, "刷新全部")
                    }
                    IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "挂载新目录") }
                },
            )
        },
    ) { padding ->
        if (library.mounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Storage, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("还没有挂载源", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("挂载后只在刷新时更新索引。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("挂载目录") }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, top = 12.dp, end = 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("每个来源独立刷新；取消挂载不会删除设备图片。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                items(library.mounts, key = MountedFolder::id) { mount ->
                    MountSourceCard(
                        mount = mount,
                        hiddenAlbums = library.hiddenAlbums.filter { it.mountId == mount.id },
                        onRefresh = { onRefresh(mount.id) },
                        onReauthorize = { onReauthorize(mount.id) },
                        onRemove = { onRemove(mount) },
                        onRestore = onRestore,
                        onShowPath = { onShowPath(mount) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MountSourceCard(
    mount: MountedFolder,
    hiddenAlbums: List<AlbumSummary>,
    onRefresh: () -> Unit,
    onReauthorize: () -> Unit,
    onRemove: () -> Unit,
    onRestore: (String) -> Unit,
    onShowPath: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var hiddenExpanded by rememberSaveable(mount.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Folder, null) }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f).combinedClickable(onClick = onShowPath),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(mount.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(middleEllipsis(mount.path), maxLines = 2, overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "更多操作") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("刷新") }, leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                            onClick = { menuOpen = false; onRefresh() },
                        )
                        DropdownMenuItem(
                            text = { Text("取消挂载") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = { menuOpen = false; onRemove() },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(if (mount.mode == MountMode.RECURSIVE) "递归" else "非递归") })
                AssistChip(onClick = {}, label = { Text(if (mount.available) "可访问" else "访问失败") })
            }
            Text("${mount.albumCount - mount.hiddenCount} 个可见画册 · ${mount.hiddenCount} 个已隐藏",
                style = MaterialTheme.typography.bodyMedium)
            Text(
                "上次成功刷新：${formatRefreshTime(mount.lastSuccessfulRefreshAt)}" +
                    if (mount.lastRefreshFailed) " · 最近一次刷新失败" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (mount.lastRefreshFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!mount.available) {
                Button(onClick = onReauthorize, modifier = Modifier.fillMaxWidth()) { Text("重新授权此来源") }
            }
            if (hiddenAlbums.isNotEmpty()) {
                HorizontalDivider()
                TextButton(onClick = { hiddenExpanded = !hiddenExpanded }) {
                    Icon(Icons.Outlined.HideImage, null)
                    Spacer(Modifier.width(8.dp))
                    Text("隐藏画册 ${hiddenAlbums.size}")
                    Spacer(Modifier.weight(1f))
                    Text(if (hiddenExpanded) "收起" else "展开")
                }
                if (hiddenExpanded) hiddenAlbums.forEach { album ->
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(album.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { onRestore(album.id) }) {
                            Icon(Icons.Outlined.Restore, null); Spacer(Modifier.width(4.dp)); Text("恢复")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    density: GridDensity,
    diskCacheBytes: Long,
    onDensityChange: (GridDensity) -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 18.dp, top = 12.dp, end = 18.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("主页布局", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Text("更改后立即应用；横屏和平板会自动增加列数。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(GridDensity.entries) { option ->
                DensityChoice(option, selected = option == density, onClick = { onDensityChange(option) })
            }
            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("图片缓存", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${formatBytes(diskCacheBytes)} / 256 MiB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { (diskCacheBytes.toFloat() / (256f * 1024f * 1024f)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) { Text("清除磁盘缓存") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Yomu") },
                        supportingContent = { Text("版本 ${BuildConfig.VERSION_NAME}") },
                        leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DensityChoice(option: GridDensity, selected: Boolean, onClick: () -> Unit) {
    val label = when (option) {
        GridDensity.COMFORTABLE -> "舒适"
        GridDensity.STANDARD -> "标准"
        GridDensity.COMPACT -> "紧凑"
    }
    val columns = when (option) {
        GridDensity.COMFORTABLE -> 2
        GridDensity.STANDARD -> 3
        GridDensity.COMPACT -> 4
    }
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.width(104.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(columns) {
                    Surface(
                        Modifier.weight(1f).aspectRatio(154f / 214f),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(5.dp),
                    ) {}
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text("典型手机约 $columns 列", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Outlined.Check, "已选择", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatRefreshTime(value: Long?): String = value?.let {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
} ?: "尚未记录"

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MiB".format(bytes / 1024f / 1024f)
    bytes >= 1024L -> "%.1f KiB".format(bytes / 1024f)
    else -> "$bytes B"
}

private fun middleEllipsis(value: String, maxLength: Int = 60): String {
    if (value.length <= maxLength) return value
    val side = (maxLength - 1) / 2
    return value.take(side) + "…" + value.takeLast(side)
}

@Composable
private fun AlbumRenameDialog(
    album: AlbumSummary,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRestore: () -> Unit,
) {
    val nameState = remember(album.id) { albumRenameFieldState(album.name) }
    val horizontalScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名画册") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "来源名称：${album.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    state = nameState,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("显示名称") },
                    supportingText = { Text("留空会恢复来源名称，不会改动设备文件夹。") },
                    inputTransformation = InputTransformation.maxLength(100),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    scrollState = horizontalScrollState,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nameState.text.toString()) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestore) { Text("恢复来源名称") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

internal fun albumRenameFieldState(name: String) = TextFieldState(
    initialText = name,
    initialSelection = TextRange(name.length),
)

@Composable
private fun MountBrowser(
    browser: MountBrowserState,
    onEnter: (DirectoryChoice) -> Unit,
    onBack: () -> Unit,
    onMode: (MountMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = browser.parents.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "上级目录")
            }
            Column(Modifier.weight(1f)) {
                Text("选择实际挂载目录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(browser.current.path, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            FilterChip(
                selected = browser.mode == MountMode.NON_RECURSIVE,
                onClick = { onMode(MountMode.NON_RECURSIVE) },
                label = { Text("非递归") },
                leadingIcon = if (browser.mode == MountMode.NON_RECURSIVE) {{ Icon(Icons.Outlined.Check, null) }} else null,
            )
            FilterChip(
                selected = browser.mode == MountMode.RECURSIVE,
                onClick = { onMode(MountMode.RECURSIVE) },
                label = { Text("递归") },
                leadingIcon = if (browser.mode == MountMode.RECURSIVE) {{ Icon(Icons.Outlined.Check, null) }} else null,
            )
        }
        Text("Yomu 会显示系统选择器可能隐藏的点号子目录。", style = MaterialTheme.typography.bodySmall)
        if (browser.loading) CenterLoading("正在读取子目录")
        else LazyColumn(Modifier.fillMaxWidth().height(330.dp)) {
            items(browser.children, key = DirectoryChoice::uri) { child ->
                ListItem(
                    headlineContent = { Text(child.name) },
                    leadingContent = { Icon(Icons.Outlined.Folder, null) },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                    modifier = Modifier.combinedClickable(onClick = { onEnter(child) }),
                )
            }
            if (browser.children.isEmpty()) item { Text("没有子目录", Modifier.padding(20.dp)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(onClick = onConfirm, enabled = !browser.loading, modifier = Modifier.weight(1f)) { Text("挂载此目录") }
        }
        Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 14.dp))
    }
}

@Composable
private fun CenterLoading(text: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(text)
        }
    }
}

@Composable
private fun EmptyLibrary(onPickFolder: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(112.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderOpen, null, Modifier.size(52.dp)) }
            }
            Spacer(Modifier.height(24.dp))
            Text("把图片目录放上书架", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("启动和打开画册不会重新扫描；挂载或刷新时更新索引。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPickFolder) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("挂载目录") }
        }
    }
}

@Composable
private fun EmptyShelf(name: String, filtering: Boolean, allBookshelf: Boolean, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (filtering) "没有匹配的画册" else "“$name”中还没有画册", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) {
                Text(when {
                    filtering -> "清除搜索"
                    allBookshelf -> "打开挂载管理"
                    else -> "查看全部画册"
                })
            }
        }
    }
}
