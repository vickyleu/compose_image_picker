package com.huhx.picker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.huhx.picker.base.BasicScreen
import com.huhx.picker.component.SelectedAssetImageItem
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.DateTimeFormatterKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.model.page.AssetPreviewViewModel
import com.huhx.picker.util.getNavigationBarHeight
import com.huhx.picker.util.vibration
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


internal class AssetPreviewScreen(
    viewModel: AssetViewModel,
    index: Int,
    time: String,
    requestType: RequestType
) : BasicScreen<AssetPreviewViewModel>(
    create = {
        AssetPreviewViewModel(
            viewModel = viewModel,
            index = index,
            time = time,
            requestType = requestType
        )
    }
) {
    @Composable
    override fun modelContent(
        model: AssetPreviewViewModel,
        navigator: Navigator,
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
        navigator: Navigator,
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
                                    navigator.pop()
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
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
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
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
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
                            style = MaterialTheme.typography.bodyLarge.copy(
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
        navigator: Navigator,
        bottomBarHeightAssign: MutableState<Dp>
    ) {
        val viewModel = LocalAssetViewModelProvider.current
        with(LocalDensity.current){
            SelectorBottomBar(
                Modifier.onGloballyPositioned {
                    bottomBarHeightAssign.value = it.size.height.toDp()
                },
                selectedList = viewModel.selectedList,
                assetInfo = model.assetInfo.value
            ) {
                navigator.pop()
                if (viewModel.selectedList.isEmpty()) viewModel.selectedList.add(it)
            }
        }

    }
}

@Composable
fun SelectorBottomBar(
    modifier: Modifier = Modifier,
    assetInfo: AssetInfo,
    selectedList: SnapshotStateList<AssetInfo>,
    onClick: (AssetInfo) -> Unit,
) {
    Row(
        modifier = modifier.then(Modifier.fillMaxWidth()
            .background(color = Color.Black.copy(alpha = 0.9F))
            .padding(bottom =  getNavigationBarHeight())
            .padding(horizontal = 10.dp, vertical = 8.dp)),
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
                    selected = selectedList.any { it == assetInfo },
                    assetSelected = selectedList,
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
            ImagePreview(uriString = asset.uriString,index=page){
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
            VideoPreview(uriString = asset.uriString, modifier = Modifier)
        }
    }
}

@Composable
fun ImagePreview(
    modifier: Modifier = Modifier,
    uriString: String,
    index: Int,
    loading: (@Composable () -> Unit)? = null,
) {
   /* val painter = rememberAsyncImagePainter(
        filterQuality = FilterQuality.Low,
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(uriString)
            .decoderFactoryPlatform {
                println("progress: $it")
            }
            .build()
    )*/

   /* if (loading != null && painter.state is AsyncImagePainter.State.Loading) {
        loading()
    }*/


    val isLoading = remember { mutableStateOf(true) }
    if(isLoading.value){
        loading?.invoke()
    }

    println("在加载哪个? index:$index uriString: $uriString")
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(uriString)
            .crossfade(true)
            .memoryCacheKey("${uriString}_preview_")
            .decoderFactoryPlatform {
                println("progress: $it")
            }
            .build(),
        filterQuality = FilterQuality.Low,
        contentDescription = null,
        onState = {
            when(it){
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


    /*Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    )*/
}

@Composable
expect fun VideoPreview(
    modifier: Modifier = Modifier,
    uriString: String,
    loading: (@Composable () -> Unit)? = null,
)

expect fun ImageRequest.Builder.decoderFactoryPlatform(progress: (Int) -> Unit): ImageRequest.Builder
