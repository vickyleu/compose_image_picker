@file:OptIn(ExperimentalTime::class)

package com.huhx.picker.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.dokar.sonner.ToasterState
import com.huhx.picker.base.BasicScreen
import com.huhx.picker.component.SelectedAssetImageItem
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.DateTimeFormatterKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.model.page.AssetPreviewViewModel
import com.huhx.picker.util.getNavigationBarHeight
import com.huhx.picker.util.vibration
import com.huhx.picker.view.MaterialSliderDefaults.toBrush
import com.huhx.picker.viewmodel.AssetViewModel
import com.huhx.picker.viewmodel.LocalAssetViewModelProvider
import compose_image_picker.imagepicker.generated.resources.Res
import compose_image_picker.imagepicker.generated.resources.icon_back
import compose_image_picker.imagepicker.generated.resources.preview_title_image
import compose_image_picker.imagepicker.generated.resources.preview_title_video
import compose_image_picker.imagepicker.generated.resources.text_asset_select
import compose_image_picker.imagepicker.generated.resources.text_done
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime


internal class AssetPreviewScreen(
    viewModel: AssetViewModel,
    toasterState: ToasterState?,
    index: Int,
    time: String,
    requestType: RequestType
) : BasicScreen<AssetPreviewViewModel>(
    create = {
        AssetPreviewViewModel(
            viewModel = viewModel,
            toasterState = toasterState,
            index = index,
            time = time,
            requestType = requestType
        )
    }
) {
    @Composable
    override fun modelContent(
        model: AssetPreviewViewModel,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        tabbarHeight: Dp
    ) {
        var index by remember { mutableIntStateOf(model.index) }
        val assets = remember(model.index) { model.assets }
        val pageState = rememberPagerState(pageCount = assets::size)

        LaunchedEffect(assets.size) {
            pageState.scrollToPage(index.coerceIn(0, assets.size - 1))
        }
        var pageIndex by remember { model.pageIndex }
        val scope = rememberCoroutineScope()
        val assetInfo = remember { model.assetInfo }
        LaunchedEffect(Unit) {
            snapshotFlow { pageIndex }
                .distinctUntilChanged()
                .collect {
                    assetInfo.value = assets[it].second
                }
        }
        val dateStringState = remember {
            mutableStateOf(
                DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日 HH:mm:ss")
                    .format(
                        Instant.fromEpochMilliseconds(assetInfo.value.date)
                            .toLocalDateTime(TimeZone.UTC)
                    )
            )
        }

        LaunchedEffect(Unit) {
            snapshotFlow { assetInfo.value }
                .distinctUntilChanged()
                .collect {
                    dateStringState.value =
                        DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日 HH:mm:ss")
                            .format(
                                Instant.fromEpochMilliseconds(it.date).toLocalDateTime(TimeZone.UTC)
                            )
                }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { pageState.currentPage }
                .flowOn(Dispatchers.IO)
                .map {
                    delay(50)
                    pageState.currentPage
                }
                .distinctUntilChanged()
                .collect {
                    pageIndex = it
                }
        }

        val context = LocalPlatformContext.current
        val viewModel = LocalAssetViewModelProvider.current
        Box(
            modifier = Modifier
                .background(Color.Black),
            contentAlignment = Alignment.BottomCenter
        ) {
            AssetPreview(assets = assets, pagerState = pageState)

            if (viewModel.selectedList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8F))
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(viewModel.selectedList) { _, resource ->
                        SelectedAssetImageItem(
                            assetInfo = resource,
                            isSelected = resource.id == assetInfo.value.id,
                            resourceType = resource.resourceType,
                            durationString = resource.formatDuration(),
                            modifier = Modifier.size(64.dp),
                            onClick = { asset ->
                                val selectedIndex =
                                    assets.indexOfFirst { (_, item) -> item.id == asset.id }
                                // 如果index == -1，说明该asset 是存在于别的时间
                                if (selectedIndex == -1) {
                                    viewModel.selectedList.sortBy { asset ->
                                        model.assets.indexOfFirst { it.second == asset }
                                    }
                                    println("AssetPreviewScreen indexOf ==selectedList=======>> ${viewModel.selectedList}")
                                    index = viewModel.selectedList.indexOf(asset)
                                    context.vibration(50L)
                                    return@SelectedAssetImageItem
                                }

                                scope.launch {
                                    println("AssetPreviewScreen animateScrollToPage=====>> $selectedIndex")
                                    pageState.animateScrollToPage(selectedIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun modelTopBar(
        model: AssetPreviewViewModel,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        topAppBarHeightAssign: MutableState<Dp>
    ) {
        var leftWidth by remember { mutableStateOf(0.dp) }
        var rightWidth by remember { mutableStateOf(0.dp) }
        with(LocalDensity.current) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color.Black)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().wrapContentWidth()
                            .requiredWidthIn(
                                min = androidx.compose.ui.unit.max(leftWidth, rightWidth),
                            )
                            .onGloballyPositioned {
                                val width = it.size.width.toDp()
                                leftWidth = width
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp)
                                .padding(8.dp)
                                .clickable {
                                    onNavigateUp?.invoke()
                                }.padding(horizontal = 3.dp, vertical = 3.dp)
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.icon_back),
                                contentDescription = "",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillHeight,
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (model.assetInfo.value.isImage()) {
                                stringResource(Res.string.preview_title_image)
                            } else {
                                stringResource(Res.string.preview_title_video)
                            },
                            style = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )

                        val dateStringState = remember(model.assetInfo.value.date) {
                            val localDateTime =
                                Instant.fromEpochMilliseconds(model.assetInfo.value.date)
                                    .toLocalDateTime(TimeZone.UTC)
                            val ds = model.dtf.format(localDateTime)
                            mutableStateOf(ds)
                        }
                        Text(
                            text = dateStringState.value,
                            style = LocalTextStyle.current.copy(color = Color.Gray)
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().fillMaxHeight()
                            .requiredWidthIn(
                                min = androidx.compose.ui.unit.max(leftWidth, rightWidth),
                            )
                            .onGloballyPositioned {
                                val width = it.size.width.toDp()
                                rightWidth = width
                            },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            modifier = Modifier.wrapContentHeight().wrapContentWidth()
                                .padding(horizontal = 4.dp),
                            text = "${model.pageIndex.value + 1}/${model.assets.size}",
                            style = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        )
                    }

                }
            }
        }
    }

    @Composable
    override fun modelBottomBar(
        model: AssetPreviewViewModel,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        bottomBarHeightAssign: MutableState<Dp>
    ) {
        val viewModel = LocalAssetViewModelProvider.current
        val maxFileSize = LocalAssetConfig.current.maxFileSize
        with(LocalDensity.current) {
            SelectorBottomBar(
                Modifier.onGloballyPositioned {
                    bottomBarHeightAssign.value = it.size.height.toDp()
                },
                toasterState = model.toasterState,
                selectedList = viewModel.selectedList,
                maxFileSize = maxFileSize,
                assetInfo = model.assetInfo.value
            ) {
                onNavigateUp?.invoke()
                if (viewModel.selectedList.isEmpty()) viewModel.selectedList.add(it)
            }
        }
    }
}

@Composable
fun SelectorBottomBar(
    modifier: Modifier = Modifier,
    assetInfo: AssetInfo,
    toasterState: ToasterState?,
    maxFileSize: Long,
    selectedList: SnapshotStateList<AssetInfo>,
    onClick: (AssetInfo) -> Unit,
) {
    Row(
        modifier = modifier.then(
            Modifier.fillMaxWidth()
                .background(color = Color.Black.copy(alpha = 0.9F))
                .padding(bottom = getNavigationBarHeight())
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentSize()) {
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                AssetImageIndicator(
                    assetInfo = assetInfo,
                    toasterState = toasterState,
                    selected = selectedList.any { it == assetInfo },
                    assetSelected = selectedList,
                    maxFileSize=maxFileSize,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.text_asset_select),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Button(
            modifier = Modifier.defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            shape = RoundedCornerShape(5.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            onClick = { onClick(assetInfo) }
        ) {
            Text(text = stringResource(Res.string.text_done), color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AssetPreview(assets: List<Pair<String, AssetInfo>>, pagerState: PagerState) {
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 0.dp),
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = {
            assets[it].second.uriString
        }
    ) { page ->
        val asset = remember(page) {
            val (_, assetInfo) = assets[page]
            assetInfo
        }
        if (asset.isImage()) {
            ImagePreview(uriString = asset.uriString, index = page) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载中...",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        } else {

            VideoItem(asset, page, pagerState)
        }
    }
}

@Composable
private fun VideoItem(
    asset: AssetInfo,
    page: Int,
    pagerState: PagerState
) {
    val isCurrentPage = page == pagerState.currentPage
    val isToolbarVisible = remember { mutableStateOf(true) }
    val isLoaded = remember { mutableStateOf(true) }
    val isPlaying = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 启动倒计时
    val tempJob = remember { mutableStateOf<Job?>(null) }
    fun startHideToolbarCountdown() {
        tempJob.value?.cancel()
        tempJob.value = scope.launch {
            delay(3000L) // 3秒倒计时
            if (isPlaying.value) {
                isToolbarVisible.value = false
            }
        }
    }
    // 自动隐藏逻辑：播放、缓冲状态时启动倒计时，停止时始终显示
    DisposableEffect(isPlaying.value, isLoaded.value) {
        if (isPlaying.value || isLoaded.value) {
            startHideToolbarCountdown()
        } else {
            isToolbarVisible.value = true // 停止状态时始终显示
        }
        onDispose { }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val interactionSource = remember { MutableInteractionSource() }
        val position = remember { mutableStateOf(0L) }
        val duration = remember { mutableStateOf(asset.duration ?: 0L) }

        val playCallback = videoPreview(
            uriString = asset.uriString,
            modifier = Modifier.fillMaxSize().clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                if (!isPlaying.value && !isLoaded.value) {
                    // 停止状态下保持显示
                    isToolbarVisible.value = true
                } else {
                    isToolbarVisible.value = !isToolbarVisible.value
                    if (isToolbarVisible.value) startHideToolbarCountdown() // 重新启动倒计时
                }
            },
            isPlaying = isPlaying,
            isLoaded = isLoaded,
            position = position,
            duration = duration,
            isCurrentPage = isCurrentPage // 判断当前页面
        )
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
        if (isCurrentPage && asset.isVideo()) {
            // 显示加载状态
            if (isLoaded.value) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    CircularProgressIndicator()
                }
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                AnimatedVisibility(
                    visible = isToolbarVisible.value,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying.value) {
                                playCallback.pause()
                            } else {
                                playCallback.play()
                            }
                            isToolbarVisible.value = true // 重置倒计时
                            startHideToolbarCountdown()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .padding(8.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.8f),
                        ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying.value) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying.value) "Pause" else "Play",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                }
            }


            val listener = remember {
                object : DefaultLifecycleObserver {
                    override fun onPause(owner: LifecycleOwner) {
                        playCallback.pause()
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        playCallback.pause()
                    }
                }
            }
            DisposableEffect(Unit) {
                lifecycle.lifecycleScope.launch {
                    lifecycle.lifecycle.addObserver(listener)
                }
                onDispose {
                    lifecycle.lifecycle.removeObserver(listener)
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AnimatedVisibility(
                    visible = isToolbarVisible.value,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { it },
                    ),
                    exit = fadeOut() + slideOutVertically(
                        targetOffsetY = { it },
                    )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(min = 20.dp)
                            .background(Color.Black.copy(alpha = 0.8F))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // 此处留空，不做任何处理，只为了拦截点击事件，避免透传
                            }
                            .padding(horizontal = 10.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val tempPosFlow = remember { mutableStateOf(-1L) }

                        Box(
                            modifier = Modifier.weight(0.2f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = ((if (tempPosFlow.value < 0) position.value else tempPosFlow.value).coerceAtLeast(
                                    0
                                )).formatDurationSec(),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f).wrapContentHeight()
                                .heightIn(min = 20.dp)
                        ) {

                            ColorfulSlider(
                                value = ((if (tempPosFlow.value < 0) position.value else tempPosFlow.value).coerceAtLeast(
                                    0
                                )).toFloat(), // Current value of the slider
                                onValueChange = { second, offset ->
                                    playCallback.onChangeSliding(true)
                                    tempPosFlow.value = second.toLong()
                                    isToolbarVisible.value = true // 重置倒计时
                                    startHideToolbarCountdown()
                                },
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                enabled = true, // Enable the slider
                                valueRange = 0f..duration.value.toFloat(), // Range of the slider
                                onValueChangeFinished = {
                                    val old = tempPosFlow.value
                                    playCallback.seekTo(old)
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            playCallback.onChangeSliding(false)
                                            delay(100L)
                                            tempPosFlow.value = -1L
                                        }
                                    }
                                },
                                colors = MaterialSliderDefaults.defaultColors(
                                    disabledThumbColor = Color.Red.toBrush(),
                                    disabledActiveTrackColor = Color.White.toBrush(),
                                    disabledInactiveTrackColor = Color.DarkGray.copy(alpha = 0.7f)
                                        .toBrush(),
                                    thumbColor = Color.Red.toBrush(),
                                    inactiveTrackColor = Color.DarkGray.copy(alpha = 0.7f)
                                        .toBrush(),
                                    activeTrackColor = Color.White.toBrush()
                                )
                            )
                        }
                        Box(
                            modifier = Modifier.weight(0.2f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = (duration.value - ((if (tempPosFlow.value < 0) position.value else tempPosFlow.value).coerceAtLeast(
                                    0
                                ))).coerceAtLeast(0L)
                                    .formatDurationSec(),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                    }

                }
            }
        }
    }
}


private fun Long.prefixZero(): String {
    return if (this < 10) "0$this" else "$this"
}

fun Long.formatDurationSec(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60
    return "${minutes.prefixZero()}:${seconds.prefixZero()}"
}

@Composable
fun ImagePreview(
    modifier: Modifier = Modifier,
    uriString: String,
    index: Int,
    loading: (@Composable () -> Unit)? = null,
) {
    val isLoading = remember { mutableStateOf(true) }
    if (isLoading.value) {
        loading?.invoke()
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(uriString)
            .crossfade(true)
            .memoryCacheKey("${uriString}_preview_")
            .decoderFactoryPlatform {
//                println("progress: $it")
            }
            .build(),
        filterQuality = FilterQuality.Low,
        contentDescription = null,
        onState = {
            when (it) {
                AsyncImagePainter.State.Empty -> isLoading.value = true
                is AsyncImagePainter.State.Error -> isLoading.value = true
                is AsyncImagePainter.State.Loading -> isLoading.value = true
                is AsyncImagePainter.State.Success -> isLoading.value = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    )
}


interface PlayCallback {
    fun play()
    fun pause()
    fun seekTo(value: Long)
    fun onChangeSliding(sliding: Boolean)
}

interface PlayerEventListener {
    fun onPlayerItemDidPlayToEndTime()
    fun onPlayerFailedToPlay()
    fun onPlayerBuffering()
    fun onPlayerBufferingCompleted()
    fun onPlayerPlaying()
    fun onPlayerPaused()
    fun onPlayerStopped() // 新增：停止播放的回调

    fun onPlaying(pos: Long, dur: Long) // 新增：播放进度回调
}

@Composable
expect fun videoPreview(
    modifier: Modifier = Modifier,
    uriString: String,
    isPlaying: MutableState<Boolean>,
    isLoaded: MutableState<Boolean>,
    position: MutableState<Long>,
    duration: MutableState<Long>,
    isCurrentPage: Boolean,
): PlayCallback

expect fun ImageRequest.Builder.decoderFactoryPlatform(progress: (Int) -> Unit): ImageRequest.Builder
