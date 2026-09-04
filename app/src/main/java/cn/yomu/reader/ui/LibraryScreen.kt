package cn.yomu.reader.ui

import android.content.ContentResolver
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cn.yomu.reader.CoverPickerState
import cn.yomu.reader.model.ALL_BOOKSHELF_ID
import cn.yomu.reader.model.AlbumSummary
import cn.yomu.reader.model.Bookshelf
import cn.yomu.reader.model.DirectoryChoice
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountBrowserState
import cn.yomu.reader.model.MountMode
import cn.yomu.reader.model.MountedFolder
import cn.yomu.reader.model.ScanProgress
import kotlinx.coroutines.launch

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
    coverPicker: CoverPickerState?,
    onPickFolder: () -> Unit,
    onSelectBookshelf: (String) -> Unit,
    onCreateBookshelf: (String) -> Unit,
    onRenameBookshelf: (String, String) -> Unit,
    onDeleteBookshelf: (String) -> Unit,
    onSetBookshelfCover: (String, String?) -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
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
    onCancelOpen: () -> Unit,
) {
    val activity = LocalActivity.current
    val darkTheme = isSystemInDarkTheme()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var showMounts by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<AlbumSummary?>(null) }
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

    val currentShelf = library.bookshelves.firstOrNull { it.id == library.currentBookshelfId }
        ?: Bookshelf(ALL_BOOKSHELF_ID, "全部画册", null, 0L, library.allAlbums.size, true)
    val albums = remember(library.albums, query) {
        if (query.isBlank()) library.albums else library.albums.filter {
            it.name.contains(query, true) || it.path.contains(query, true)
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
        gesturesEnabled = scanProgress == null,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Yomu 书架",
                    modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                library.bookshelves.forEach { shelf ->
                    val selected = shelf.id == library.currentBookshelfId
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .combinedClickable(
                                onClick = {
                                    onSelectBookshelf(shelf.id)
                                    scope.launch { drawerState.close() }
                                },
                                onLongClick = { if (!shelf.builtIn) shelfMenu = shelf },
                            ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (shelf.coverUri == null) Icon(Icons.Outlined.Bookmarks, null)
                            else MiniCover(resolver, shelf.coverUri, shelf.name, shelf.coverVersion)
                            Spacer(Modifier.width(12.dp))
                            Text(shelf.name, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${shelf.albumCount}", style = MaterialTheme.typography.labelMedium)
                            if (!shelf.builtIn) {
                                IconButton(onClick = { shelfMenu = shelf }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.MoreVert, "管理 ${shelf.name}")
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                NavigationDrawerItem(
                    label = { Text("新建书架") },
                    selected = false,
                    onClick = {
                        editingShelf = null
                        shelfName = ""
                        showNameDialog = true
                    },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("挂载管理") },
                    selected = false,
                    onClick = {
                        showMounts = true
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Storage, null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LibraryHeader(
                    shelf = currentShelf,
                    mountCount = library.mounts.size,
                    onMenu = { scope.launch { drawerState.open() } },
                    onShowMounts = { showMounts = true },
                )
            },
            floatingActionButton = {
                FilledIconButton(onClick = onPickFolder, modifier = Modifier.size(58.dp)) {
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
                    albums.isEmpty() -> EmptyShelf(currentShelf.name, query.isNotBlank())
                    else -> AlbumGrid(
                        albums = albums,
                        resolver = resolver,
                        onOpenAlbum = onOpenAlbum,
                        onLongPress = { selectedAlbum = it },
                        onCoverFailed = onRepairAlbumCover,
                    )
                }
            }
        }
    }

    selectedAlbum?.let { album ->
        ModalBottomSheet(onDismissRequest = { selectedAlbum = null }) {
            Text(album.name, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
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

    if (showMounts) {
        ModalBottomSheet(onDismissRequest = { showMounts = false }) {
            MountManager(
                library = library,
                onAdd = { showMounts = false; onPickFolder() },
                onRefresh = { showMounts = false; onRefreshMount(it) },
                onReauthorize = { showMounts = false; onRequestReauthorize(it) },
                onRefreshAll = { confirmRefreshAll = true },
                onRemove = { removeMount = it },
                onRestore = onRestoreAlbum,
                onClearCache = onClearCache,
            )
        }
    }

    removeMount?.let { mount ->
        AlertDialog(
            onDismissRequest = { removeMount = null },
            title = { Text("取消挂载“${mount.name}”？") },
            text = { Text("将移除 ${mount.albumCount} 个画册的分类、封面和进度，但不会删除设备图片。") },
            confirmButton = {
                TextButton(onClick = { onRemoveMount(mount.id); removeMount = null; showMounts = false }) {
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
                TextButton(onClick = { confirmRefreshAll = false; showMounts = false; onRefreshAll() }) { Text("刷新") }
            },
            dismissButton = { TextButton(onClick = { confirmRefreshAll = false }) { Text("取消") } },
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

    openingAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在打开 ${album.name}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("正在读取并排序图片…")
                }
            },
            confirmButton = { TextButton(onClick = onCancelOpen) { Text("取消") } },
        )
    }
}

@Composable
private fun LibraryHeader(shelf: Bookshelf, mountCount: Int, onMenu: () -> Unit, onShowMounts: () -> Unit) {
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
            IconButton(onClick = onShowMounts) { Icon(Icons.Outlined.FolderOpen, "挂载管理") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGrid(
    albums: List<AlbumSummary>,
    resolver: ContentResolver,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onLongPress: (AlbumSummary) -> Unit,
    onCoverFailed: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(154.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 8.dp,
            end = 18.dp,
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(albums, key = AlbumSummary::id) { album ->
            Column(
                Modifier.clip(RoundedCornerShape(18.dp)).combinedClickable(
                    onClick = { onOpenAlbum(album) },
                    onLongClick = { onLongPress(album) },
                ),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(18.dp))
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
                }
                Spacer(Modifier.height(10.dp))
                Text(album.name, Modifier.padding(horizontal = 2.dp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${album.pageCount} 页 · ${album.path}",
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
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))) { Thumbnail(resolver, uri, description, version) }
}

@Composable
private fun MountManager(
    library: LibrarySnapshot,
    onAdd: () -> Unit,
    onRefresh: (String) -> Unit,
    onReauthorize: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onRemove: (MountedFolder) -> Unit,
    onRestore: (String) -> Unit,
    onClearCache: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("挂载管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("刷新按挂载源执行；取消挂载不会删除设备图片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
            items(library.mounts, key = MountedFolder::id) { mount ->
                Column {
                    ListItem(
                        headlineContent = { Text(mount.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                "${if (mount.mode == MountMode.RECURSIVE) "递归" else "非递归"} · ${mount.albumCount} 个画册" +
                                    if (mount.available) "" else " · 无法访问",
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Folder, null) },
                        trailingContent = {
                            Row {
                                if (!mount.available) {
                                    TextButton(onClick = { onReauthorize(mount.id) }) { Text("重新授权") }
                                }
                                IconButton(onClick = { onRefresh(mount.id) }) { Icon(Icons.Outlined.Refresh, "刷新") }
                                IconButton(onClick = { onRemove(mount) }) { Icon(Icons.Outlined.Delete, "取消挂载") }
                            }
                        },
                    )
                    library.hiddenAlbums.filter { it.mountId == mount.id }.forEach { album ->
                        ListItem(
                            headlineContent = { Text(album.name) },
                            supportingContent = { Text("已隐藏") },
                            leadingContent = { Icon(Icons.Outlined.HideImage, null) },
                            trailingContent = {
                                IconButton(onClick = { onRestore(album.id) }) { Icon(Icons.Outlined.Restore, "恢复") }
                            },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefreshAll, enabled = library.mounts.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Cached, null); Spacer(Modifier.width(6.dp)); Text("刷新全部")
            }
            OutlinedButton(onClick = onClearCache, modifier = Modifier.weight(1f)) { Text("清除缓存") }
        }
        Button(onClick = onAdd, Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("挂载新目录")
        }
        Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
    }
}

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
            Text("启动不扫描，只有挂载、刷新和打开画册时读取目录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPickFolder) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("挂载目录") }
        }
    }
}

@Composable
private fun EmptyShelf(name: String, filtering: Boolean) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(if (filtering) "没有匹配的画册" else "“$name”中还没有画册", style = MaterialTheme.typography.titleLarge)
    }
}
