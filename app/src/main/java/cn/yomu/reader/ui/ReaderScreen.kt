package cn.yomu.reader.ui

import android.content.ContentResolver
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.ImageRef
import cn.yomu.reader.model.ReaderPreferences
import cn.yomu.reader.model.ReadingDirection
import cn.yomu.reader.model.ReadingMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    album: Album,
    preferences: ReaderPreferences,
    resolver: ContentResolver,
    onBack: () -> Unit,
    onProgress: (Int) -> Unit,
    onImageLoadFailed: (ImageRef) -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
) {
    val activity = LocalActivity.current ?: return
    var chromeVisible by remember { mutableStateOf(true) }
    var currentPage by remember(album.id) { mutableIntStateOf(album.progress.coerceIn(0, album.images.lastIndex)) }
    var currentImageUri by remember(album.id) { mutableStateOf(album.images[currentPage].uri) }
    var seekValue by remember { mutableFloatStateOf(currentPage.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = currentPage) { album.images.size }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
    val scope = rememberCoroutineScope()

    LaunchedEffect(album.images, preferences.mode) {
        val target = album.images.indexOfFirst { it.uri == currentImageUri }
            .takeIf { it >= 0 }
            ?: currentPage.coerceIn(0, album.images.lastIndex)
        if (target != currentPage) {
            currentPage = target
            seekValue = target.toFloat()
            onProgress(target)
            if (preferences.mode == ReadingMode.PAGER) pagerState.scrollToPage(target)
            else listState.scrollToItem(target)
        }
        currentImageUri = album.images[target].uri
    }

    BackHandler(onBack = onBack)

    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(chromeVisible) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (chromeVisible) controller.show(WindowInsetsCompat.Type.systemBars())
        else controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }

    LaunchedEffect(preferences.mode, album.id) {
        if (preferences.mode == ReadingMode.PAGER) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    currentPage = page
                    album.images.getOrNull(page)?.let { currentImageUri = it.uri }
                    if (!isSeeking) seekValue = page.toFloat()
                    onProgress(page)
                }
        } else {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { page ->
                    currentPage = page
                    album.images.getOrNull(page)?.let { currentImageUri = it.uri }
                    if (!isSeeking) seekValue = page.toFloat()
                    onProgress(page)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (preferences.mode) {
            ReadingMode.PAGER -> PagerReader(
                album = album,
                resolver = resolver,
                pagerState = pagerState,
                direction = preferences.direction,
                onToggleChrome = { chromeVisible = !chromeVisible },
                onImageLoadFailed = onImageLoadFailed,
            )
            ReadingMode.WEBTOON -> WebtoonReader(
                album = album,
                resolver = resolver,
                listState = listState,
                onToggleChrome = { chromeVisible = !chromeVisible },
                onImageLoadFailed = onImageLoadFailed,
            )
        }

        ReaderChrome(
            visible = chromeVisible,
            album = album,
            currentPage = currentPage,
            seekValue = seekValue,
            preferences = preferences,
            onBack = onBack,
            onSeekChange = {
                isSeeking = true
                seekValue = it
            },
            onSeekFinished = {
                val target = seekValue.roundToInt().coerceIn(0, album.images.lastIndex)
                currentPage = target
                onProgress(target)
                scope.launch {
                    if (preferences.mode == ReadingMode.PAGER) pagerState.scrollToPage(target)
                    else listState.scrollToItem(target)
                    isSeeking = false
                }
            },
            onModeChange = { mode ->
                onPreferencesChange(preferences.copy(mode = mode))
                scope.launch {
                    if (mode == ReadingMode.PAGER) pagerState.scrollToPage(currentPage)
                    else listState.scrollToItem(currentPage)
                }
            },
            onDirectionChange = {
                onPreferencesChange(
                    preferences.copy(
                        direction = if (preferences.direction == ReadingDirection.LEFT_TO_RIGHT) {
                            ReadingDirection.RIGHT_TO_LEFT
                        } else {
                            ReadingDirection.LEFT_TO_RIGHT
                        },
                    ),
                )
            },
        )
    }
}

@Composable
private fun PagerReader(
    album: Album,
    resolver: ContentResolver,
    pagerState: androidx.compose.foundation.pager.PagerState,
    direction: ReadingDirection,
    onToggleChrome: () -> Unit,
    onImageLoadFailed: (ImageRef) -> Unit,
) {
    var zoomed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = direction == ReadingDirection.RIGHT_TO_LEFT,
        userScrollEnabled = !zoomed,
        beyondViewportPageCount = 1,
        key = { album.images[it].uri },
    ) { page ->
        ZoomablePage(
            resolver = resolver,
            uri = album.images[page].uri,
            contentDescription = album.images[page].name,
            onZoomChanged = { if (page == pagerState.currentPage) zoomed = it },
            onImageLoadFailed = { onImageLoadFailed(album.images[page]) },
            onTap = { fraction ->
                when {
                    fraction in 0.34f..0.66f -> onToggleChrome()
                    fraction < 0.34f -> {
                        val delta = if (direction == ReadingDirection.LEFT_TO_RIGHT) -1 else 1
                        scope.launch { pagerState.animateScrollToPage((page + delta).coerceIn(0, album.images.lastIndex)) }
                    }
                    else -> {
                        val delta = if (direction == ReadingDirection.LEFT_TO_RIGHT) 1 else -1
                        scope.launch { pagerState.animateScrollToPage((page + delta).coerceIn(0, album.images.lastIndex)) }
                    }
                }
            },
        )
    }
}

@Composable
private fun ZoomablePage(
    resolver: ContentResolver,
    uri: String,
    contentDescription: String,
    onZoomChanged: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onTap: (Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val targetWidth = with(density) { maxWidth.roundToPx() * 2 }
        val state by rememberBitmap(resolver, uri, targetWidth)
        var scale by remember(uri) { mutableFloatStateOf(1f) }
        var offset by remember(uri) { mutableStateOf(Offset.Zero) }
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val transformState = rememberTransformableState { _, zoom, pan, _ ->
            val newScale = (scale * zoom).coerceIn(1f, 5f)
            scale = newScale
            if (newScale == 1f) {
                offset = Offset.Zero
            } else {
                val maxX = (viewWidth * (newScale - 1f) / 2f).coerceAtLeast(0f)
                val maxY = (viewHeight * (newScale - 1f) / 2f).coerceAtLeast(0f)
                offset = Offset(
                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                )
            }
            onZoomChanged(newScale > 1.02f)
        }

        when (val value = state) {
            BitmapLoadState.Loading -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            BitmapLoadState.Failed -> Text("图片无法解码", color = Color.White.copy(alpha = 0.72f))
            is BitmapLoadState.Ready -> Image(
                bitmap = value.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(
                        state = transformState,
                        lockRotationOnZoomPan = true,
                        canPan = { scale > 1.02f },
                    )
                    .pointerInput(uri) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2.5f
                                if (scale == 1f) offset = Offset.Zero
                                onZoomChanged(scale > 1f)
                            },
                            onTap = { onTap(it.x / size.width.coerceAtLeast(1)) },
                        )
                    },
            )
        }
        LaunchedEffect(state, uri) {
            if (state == BitmapLoadState.Failed) onImageLoadFailed()
        }
    }
}

@Composable
private fun WebtoonReader(
    album: Album,
    resolver: ContentResolver,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToggleChrome: () -> Unit,
    onImageLoadFailed: (ImageRef) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val targetWidth = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(album.images, key = { _, image -> image.uri }) { index, image ->
                WebtoonPage(
                    resolver = resolver,
                    uri = image.uri,
                    name = "${index + 1}. ${image.name}",
                    targetWidth = targetWidth,
                    onTap = onToggleChrome,
                    onImageLoadFailed = { onImageLoadFailed(image) },
                )
            }
        }
    }
}

@Composable
private fun WebtoonPage(
    resolver: ContentResolver,
    uri: String,
    name: String,
    targetWidth: Int,
    onTap: () -> Unit,
    onImageLoadFailed: () -> Unit,
) {
    val state by rememberBitmap(resolver, uri, targetWidth)
    when (val value = state) {
        BitmapLoadState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().height(520.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }
        BitmapLoadState.Failed -> Box(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            contentAlignment = Alignment.Center,
        ) { Text("$name 无法解码", color = Color.White.copy(alpha = 0.72f)) }
        is BitmapLoadState.Ready -> Image(
            bitmap = value.bitmap.asImageBitmap(),
            contentDescription = name,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(uri) { detectTapGestures(onTap = { onTap() }) },
        )
    }
    LaunchedEffect(state, uri) {
        if (state == BitmapLoadState.Failed) onImageLoadFailed()
    }
}

@Composable
private fun ReaderChrome(
    visible: Boolean,
    album: Album,
    currentPage: Int,
    seekValue: Float,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onModeChange: (ReadingMode) -> Unit,
    onDirectionChange: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.76f),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(top = 10.dp, start = 6.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回书架")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        album.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.66f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("${currentPage + 1} / ${album.images.size}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Slider(
                        value = seekValue.coerceIn(0f, album.images.lastIndex.toFloat()),
                        onValueChange = onSeekChange,
                        onValueChangeFinished = onSeekFinished,
                        valueRange = 0f..album.images.lastIndex.coerceAtLeast(1).toFloat(),
                        steps = (album.images.size - 2).coerceIn(0, 100),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderAction(
                            label = "分页",
                            selected = preferences.mode == ReadingMode.PAGER,
                            onClick = { onModeChange(ReadingMode.PAGER) },
                        ) { Icon(Icons.Outlined.ViewCarousel, null) }
                        ReaderAction(
                            label = "长图",
                            selected = preferences.mode == ReadingMode.WEBTOON,
                            onClick = { onModeChange(ReadingMode.WEBTOON) },
                        ) { Icon(Icons.Outlined.ViewAgenda, null) }
                        ReaderAction(
                            label = if (preferences.direction == ReadingDirection.LEFT_TO_RIGHT) "左 → 右" else "右 → 左",
                            selected = false,
                            onClick = onDirectionChange,
                        ) { Icon(Icons.Outlined.SwapHoriz, null) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ReaderAction(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(7.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
