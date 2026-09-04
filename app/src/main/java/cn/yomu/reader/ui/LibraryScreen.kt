package cn.yomu.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountedFolder
import android.content.ContentResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    library: LibrarySnapshot,
    isScanning: Boolean,
    resolver: ContentResolver,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onRemoveMount: (String) -> Unit,
) {
    val activity = LocalActivity.current
    val darkTheme = isSystemInDarkTheme()
    var query by remember { mutableStateOf("") }
    var showFolders by remember { mutableStateOf(false) }
    var removeCandidate by remember { mutableStateOf<MountedFolder?>(null) }
    val albums = remember(library.albums, query) {
        if (query.isBlank()) library.albums else library.albums.filter {
            it.name.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LibraryHeader(
                folderCount = library.mounts.size,
                albumCount = library.albums.size,
                isScanning = isScanning,
                onShowFolders = { showFolders = true },
                onRefresh = onRefresh,
            )
        },
        floatingActionButton = {
            FilledIconButton(
                onClick = onPickFolder,
                modifier = Modifier.size(58.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "挂载文件夹")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (library.mounts.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        AnimatedVisibility(query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
                            }
                        }
                    },
                    placeholder = { Text("搜索书册或路径") },
                )
            }

            when {
                library.mounts.isEmpty() && !isScanning -> EmptyLibrary(onPickFolder)
                albums.isEmpty() && !isScanning -> NoAlbums(query.isNotBlank(), onRefresh)
                else -> AlbumGrid(
                    albums = albums,
                    resolver = resolver,
                    onOpenAlbum = onOpenAlbum,
                )
            }
        }
    }

    if (showFolders) {
        ModalBottomSheet(onDismissRequest = { showFolders = false }) {
            FolderSheet(
                mounts = library.mounts,
                issues = library.issues,
                onAdd = {
                    showFolders = false
                    onPickFolder()
                },
                onRemove = { removeCandidate = it },
            )
        }
    }

    removeCandidate?.let { folder ->
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            title = { Text("取消挂载？") },
            text = { Text("“${folder.name}”中的原始图片不会被删除，只会从 Yomu 书架移除。") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveMount(folder.uri)
                    removeCandidate = null
                }) { Text("取消挂载") }
            },
            dismissButton = {
                TextButton(onClick = { removeCandidate = null }) { Text("保留") }
            },
        )
    }
}

@Composable
private fun LibraryHeader(
    folderCount: Int,
    albumCount: Int,
    isScanning: Boolean,
    onShowFolders: () -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Yomu",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (folderCount == 0) "你的本地图片书架" else "$folderCount 个目录 · $albumCount 册",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShowFolders) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = "管理挂载目录")
                }
                IconButton(onClick = onRefresh, enabled = !isScanning) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "重新扫描")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    resolver: ContentResolver,
    onOpenAlbum: (Album) -> Unit,
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
        items(albums, key = Album::id) { album ->
            AlbumCard(album, resolver, onOpenAlbum)
        }
    }
}

@Composable
private fun AlbumCard(album: Album, resolver: ContentResolver, onOpenAlbum: (Album) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenAlbum(album) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Thumbnail(
                resolver = resolver,
                uri = album.cover.uri,
                contentDescription = album.name,
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
            ) {
                Text(
                    text = "${album.progress + 1}/${album.images.size}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            LinearProgressIndicator(
                progress = { album.progressFraction },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = Color.Black.copy(alpha = 0.22f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = album.name,
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${album.images.size} 页 · ${album.path}",
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Thumbnail(resolver: ContentResolver, uri: String, contentDescription: String) {
    val state by rememberBitmap(resolver, uri, 640)
    when (val value = state) {
        BitmapLoadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        BitmapLoadState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is BitmapLoadState.Ready -> Image(
            bitmap = value.bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun EmptyLibrary(onPickFolder: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(112.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("把图片文件夹放上书架", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "每个含图片的子目录会成为一册。\n以“.”开头的隐藏目录也会扫描。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPickFolder) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("挂载文件夹")
            }
        }
    }
}

@Composable
private fun NoAlbums(filtering: Boolean, onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (filtering) "没有匹配的书册" else "这个目录里还没有图片", style = MaterialTheme.typography.titleLarge)
            if (!filtering) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRefresh) { Text("重新扫描") }
            }
        }
    }
}

@Composable
private fun FolderSheet(
    mounts: List<MountedFolder>,
    issues: List<String>,
    onAdd: () -> Unit,
    onRemove: (MountedFolder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 22.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {
        Text("挂载目录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "目录授权保存在设备上；取消挂载不会删除任何图片。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        mounts.forEachIndexed { index, folder ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.name, fontWeight = FontWeight.SemiBold)
                    Text("包含隐藏子目录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onRemove(folder) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "取消挂载 ${folder.name}")
                }
            }
            if (index != mounts.lastIndex) HorizontalDivider()
        }
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            issues.forEach { issue ->
                Text(issue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("再挂载一个目录")
        }
    }
}
