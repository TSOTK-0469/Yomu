package cn.yomu.reader.ui

import android.content.ContentResolver
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    album: Album,
    preferences: ReaderPreferences,
    resolver: ContentResolver,
    onBack: () -> Unit,
    onProgress: (Int) -> Unit,
    onImageLoadFailed: (ImageRef) -> Unit,
    settingsOpen: Boolean,
    onOpenSettings: () -> Unit,
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

    BackHandler(onBack = onBack)

    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(chromeVisible, settingsOpen, darkTheme) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = settingsOpen && !darkTheme
        controller.isAppearanceLightNavigationBars = settingsOpen && !darkTheme
        if (chromeVisible || settingsOpen) controller.show(WindowInsetsCompat.Type.systemBars())
        else controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }

    LaunchedEffect(preferences.mode, preferences.direction, album.id, album.images) {
        // Restore the image before observing the newly active reader's position.
        val target = album.images.indexOfFirst { it.uri == currentImageUri }
            .takeIf { it >= 0 } ?: currentPage.coerceIn(0, album.images.lastIndex)
        currentPage = target
        currentImageUri = album.images[target].uri
        seekValue = target.toFloat()
        if (preferences.mode == ReadingMode.PAGER) pagerState.scrollToPage(target)
        else listState.scrollToItem(target)

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
            ReadingMode.PAGER -> key(preferences.direction) {
                PagerReader(
                    album = album,
                    resolver = resolver,
                    pagerState = pagerState,
                    direction = preferences.direction,
                    onToggleChrome = { chromeVisible = !chromeVisible },
                    onImageLoadFailed = onImageLoadFailed,
                )
            }
            ReadingMode.WEBTOON -> WebtoonReader(
                album = album,
                resolver = resolver,
                listState = listState,
                onToggleChrome = { chromeVisible = !chromeVisible },
                onImageLoadFailed = onImageLoadFailed,
            )
        }

        ReaderChrome(
            visible = chromeVisible && !settingsOpen,
            album = album,
            currentPage = currentPage,
            seekValue = seekValue,
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
            onOpenSettings = onOpenSettings,
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
        val targetWidth = with(LocalDensity.current) { maxWidth.roundToPx() * 2 }
        val state by rememberBitmap(resolver, uri, targetWidth)
        when (val value = state) {
            BitmapLoadState.Loading -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            BitmapLoadState.Failed -> Text("图片无法解码", color = Color.White.copy(alpha = 0.72f))
            is BitmapLoadState.Ready -> ZoomableImage(value.bitmap, uri, contentDescription, onZoomChanged, onTap)
        }
        LaunchedEffect(state, uri) {
            if (state == BitmapLoadState.Failed) onImageLoadFailed()
        }
    }
}

internal val ImageScaleKey = androidx.compose.ui.semantics.SemanticsPropertyKey<Float>("ImageScale")
internal val ImageOffsetKey = androidx.compose.ui.semantics.SemanticsPropertyKey<Offset>("ImageOffset")

@Composable
internal fun ZoomableImage(
    bitmap: android.graphics.Bitmap,
    uri: String,
    contentDescription: String,
    onZoomChanged: (Boolean) -> Unit,
    onTap: (Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val flingDecay = rememberSplineBasedDecay<Offset>()
        var scale by remember(uri) { mutableFloatStateOf(1f) }
        var offset by remember(uri) { mutableStateOf(Offset.Zero) }
        var flingJob by remember(uri) { mutableStateOf<Job?>(null) }
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val transformState = rememberTransformableState { centroid, zoom, pan, _ ->
            flingJob?.cancel()
            val newScale = (scale * zoom).coerceIn(1f, 5f)
            val bounds = zoomPanBounds(viewWidth, viewHeight, bitmap.width.toFloat(), bitmap.height.toFloat(), newScale)
            offset = clampPanOffset(
                anchoredZoomOffset(offset, centroid, Offset(viewWidth / 2f, viewHeight / 2f), newScale / scale, pan),
                bounds,
            )
            scale = newScale
            onZoomChanged(newScale > 1.02f)
        }
        val currentScale = rememberUpdatedState(scale)
        val currentOffset = rememberUpdatedState(offset)
        val currentBitmap = rememberUpdatedState(bitmap)
        val currentViewWidth = rememberUpdatedState(viewWidth)
        val currentViewHeight = rememberUpdatedState(viewHeight)

        LaunchedEffect(bitmap, viewWidth, viewHeight) {
            flingJob?.cancel()
            val bounds = zoomPanBounds(
                viewWidth, viewHeight, bitmap.width.toFloat(), bitmap.height.toFloat(), scale,
            )
            offset = clampPanOffset(offset, bounds)
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .transformable(
                    state = transformState,
                    lockRotationOnZoomPan = true,
                    canPan = { scale > 1.02f },
                )
                .pointerInput(uri, viewWidth, viewHeight, bitmap) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        flingJob?.cancel()
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var previousPosition = down.position
                        var travelDistance = 0f
                        var usedMultiplePointers = false
                        var released = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } > 1) usedMultiplePointers = true
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            travelDistance += (change.position - previousPosition).getDistance()
                            previousPosition = change.position
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            if (!change.pressed) {
                                released = true
                                break
                            }
                        }

                        val bitmap = currentBitmap.value
                        if (
                            released &&
                            !usedMultiplePointers &&
                            travelDistance >= viewConfiguration.touchSlop &&
                            currentScale.value > 1.02f
                        ) {
                            val velocity = velocityTracker.calculateVelocity()
                            val speed = hypot(velocity.x, velocity.y)
                            val minimumFlingSpeed = with(density) { 80.dp.toPx() }
                            if (speed >= minimumFlingSpeed) {
                                val bounds = zoomPanBounds(
                                    currentViewWidth.value,
                                    currentViewHeight.value,
                                    bitmap.width.toFloat(),
                                    bitmap.height.toFloat(),
                                    currentScale.value,
                                )
                                val startOffset = clampPanOffset(currentOffset.value, bounds)
                                flingJob = scope.launch {
                                    val animation = Animatable(startOffset, Offset.VectorConverter)
                                    animation.updateBounds(
                                        lowerBound = Offset(-bounds.x, -bounds.y),
                                        upperBound = bounds,
                                    )
                                    animation.animateDecay(
                                        initialVelocity = Offset(velocity.x, velocity.y),
                                        animationSpec = flingDecay,
                                    ) {
                                        offset = this.value
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(uri, viewWidth, viewHeight, bitmap) {
                    detectTapGestures(
                        onDoubleTap = { position ->
                            flingJob?.cancel()
                            val newScale = if (scale > 1f) 1f else 2.5f
                            val bounds = zoomPanBounds(viewWidth, viewHeight, bitmap.width.toFloat(), bitmap.height.toFloat(), newScale)
                            offset = clampPanOffset(
                                anchoredZoomOffset(offset, position, Offset(viewWidth / 2f, viewHeight / 2f), newScale / scale),
                                bounds,
                            )
                            scale = newScale
                            onZoomChanged(scale > 1f)
                        },
                        onTap = { onTap(it.x / size.width.coerceAtLeast(1)) },
                    )
                }
                // Keep gesture receivers outside this layer so deltas and velocity stay in screen pixels.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
        Box(Modifier.semantics { this[ImageScaleKey] = scale; this[ImageOffsetKey] = offset })
    }
}

// The focal point is in viewport coordinates, as are all pan and fling distances.
internal fun anchoredZoomOffset(
    offset: Offset,
    focalPoint: Offset,
    viewportCenter: Offset,
    ratio: Float,
    pan: Offset = Offset.Zero,
): Offset = (offset - (focalPoint - viewportCenter)) * ratio + (focalPoint - viewportCenter) + pan

internal fun zoomPanBounds(
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
    scale: Float,
): Offset {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f || scale <= 1f) {
        return Offset.Zero
    }
    val fitScale = min(viewWidth / imageWidth, viewHeight / imageHeight)
    val scaledWidth = imageWidth * fitScale * scale
    val scaledHeight = imageHeight * fitScale * scale
    return Offset(
        x = ((scaledWidth - viewWidth) / 2f).coerceAtLeast(0f),
        y = ((scaledHeight - viewHeight) / 2f).coerceAtLeast(0f),
    )
}

internal fun clampPanOffset(value: Offset, bounds: Offset) = Offset(
    x = value.x.coerceIn(-bounds.x, bounds.x),
    y = value.y.coerceIn(-bounds.y, bounds.y),
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderChrome(
    visible: Boolean,
    album: Album,
    currentPage: Int,
    seekValue: Float,
    onBack: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sliderColors = SliderDefaults.colors()
    val sliderInteractionSource = remember { MutableInteractionSource() }

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
                        colors = sliderColors,
                        interactionSource = sliderInteractionSource,
                        thumb = {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                            )
                        },
                        track = { state -> ReaderProgressTrack(state) },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ReaderAction(label = "设置", selected = false, onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, null)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderProgressTrack(state: SliderState) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = activeColor.copy(alpha = 0.24f)
    val range = state.valueRange
    val fraction = if (range.endInclusive > range.start) {
        ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Canvas(Modifier.fillMaxWidth().height(3.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
            color = inactiveColor,
            cornerRadius = CornerRadius(radius, radius),
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = activeColor,
                size = size.copy(width = size.width * fraction),
                cornerRadius = CornerRadius(radius, radius),
            )
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
