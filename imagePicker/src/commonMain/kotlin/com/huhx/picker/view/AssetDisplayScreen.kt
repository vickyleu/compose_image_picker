package com.huhx.picker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import coil3.PlatformContext
import coil3.Uri
import coil3.compose.LocalPlatformContext
import com.huhx.picker.component.AssetImageItem
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType
import com.huhx.picker.viewmodel.AssetViewModel
import compose_image_picker.imagepicker.generated.resources.Res
import compose_image_picker.imagepicker.generated.resources.icon_back
import compose_image_picker.imagepicker.generated.resources.icon_camera
import compose_image_picker.imagepicker.generated.resources.label_album
import compose_image_picker.imagepicker.generated.resources.label_camera
import compose_image_picker.imagepicker.generated.resources.message_selected_exceed
import compose_image_picker.imagepicker.generated.resources.text_select_tip
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
private fun rememberLazyGridModelState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialCheck: () -> LazyGridState?,
    initialCallback: (LazyGridState) -> Unit
): LazyGridState {
    return rememberSaveable(saver = LazyGridState.Saver) {
        initialCheck.invoke() ?: LazyGridState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset
        ).apply(initialCallback)
    }
}

@OptIn(InternalVoyagerApi::class)
@Composable
internal fun AssetDisplayScreen(
    viewModel: AssetViewModel,
    navigateToDropDown: (String) -> Unit,
    onPicked: (List<AssetInfo>) -> Unit,
    onClose: (List<AssetInfo>) -> Unit,
) {
    BackHandler(enabled = true) {
        if (viewModel.selectedList.isNotEmpty()) {
            viewModel.clear()
        } else {
            onClose(viewModel.selectedList)
        }
    }

    val initialTopBarHeight = remember { viewModel.initialTopBarHeight }
    val initialBottomBarHeight = remember { viewModel.initialBottomBarHeight }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            DisplayTopAppBar(
                selectedList = viewModel.selectedList,
                navigateUp = onClose,
                initialTopBarHeight = initialTopBarHeight,
            )
        },
        bottomBar = {
            DisplayBottomBar(
                viewModel, onPicked,
                initialBottomBarHeight = initialBottomBarHeight,
                navigateToDropDown = navigateToDropDown
            )
        }
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = initialTopBarHeight.value)
                .fillMaxSize()
        ) {
            AssetContent(viewModel, RequestType.IMAGE, initialBottomBarHeight)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DisplayTopAppBar(
    selectedList: List<AssetInfo>,
    navigateUp: (List<AssetInfo>) -> Unit,
    initialTopBarHeight: MutableState<Dp>,
) {
    with(LocalDensity.current) {
        CenterAlignedTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .statusBarsPadding()
                .onGloballyPositioned {
                    val height = it.size.height.toDp()
                    if (initialTopBarHeight.value < height) {
                        initialTopBarHeight.value = height
                    }
                },
            navigationIcon = {
                Box(
                    modifier = Modifier.size(48.dp)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.icon_back),
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize().clickable {
                            navigateUp(selectedList)
                        },
                        contentScale = ContentScale.FillBounds,
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black
            ),
            title = {
                /*Row(modifier = Modifier.clickable { navigateToDropDown(directory) }) {
                    Text(text = directory, fontSize = 18.sp, color = Color.White)
                    Icon(imageVector = Icons.Default.KeyboardArrowDown,
                        tint = Color.White,
                        contentDescription = "")
                }*/
            },
            actions = {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(Color.Green, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    val maxAssets = LocalAssetConfig.current.maxAssets
                    Text(
                        "完成${selectedList.size}/${maxAssets}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = Color.White
                    )
                }
            }
        )
    }
}


@Composable
private fun DisplayBottomBar(
    viewModel: AssetViewModel,
    onPicked: (List<AssetInfo>) -> Unit,
    initialBottomBarHeight: MutableState<Dp>,
    navigateToDropDown: (String) -> Unit
) {


    with(LocalDensity.current) {
        if (viewModel.selectedList.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .onGloballyPositioned {
                        val height = it.size.height.toDp()
                        if (initialBottomBarHeight.value < height) {
                            initialBottomBarHeight.value = height
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            //
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(viewModel.directory, fontSize = 16.sp, color = Color.Gray)
                TextButton(
                    onClick = {


                    },
                    content = {
                        Text(
                            text = stringResource(Res.string.label_camera),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                )
                TextButton(
                    onClick = {},
                    content = {
                        Text(
                            text = stringResource(Res.string.label_album),
                            fontSize = 16.sp
                        )
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .onGloballyPositioned {
                        val height = it.size.height.toDp()
                        if (initialBottomBarHeight.value < height) {
                            initialBottomBarHeight.value = height
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.text_select_tip),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                AppBarButton(
                    size = viewModel.selectedList.size,
                    onPicked = { onPicked(viewModel.selectedList) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssetContent(
    viewModel: AssetViewModel,
    requestType: RequestType,
    padding: MutableState<Dp>
) {
    val assets = remember{viewModel.getGroupedAssets(requestType)}
    val context = LocalPlatformContext.current
    val gridCount = LocalAssetConfig.current.gridCount
    val maxAssets = LocalAssetConfig.current.maxAssets
    val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)
    val gridState = rememberSaveable(assets, saver = LazyGridState.Saver) {
        LazyGridState()
    }
    if (assets.isEmpty()) {
        return Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "对应的资源为空",
                textAlign = TextAlign.Center
            )
        }
    }
    var cameraUri: Uri? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberCameraLauncher { success ->
        if (success) {
            cameraUri?.let { scope.launch { viewModel.initDirectories() } }
        } else {
            viewModel.deleteImage(cameraUri)
        }
    }

    // 需要将assets 排序,最后将所有list flatten 到一个list里面去
    val flattenedList = remember {
        listOf("-" to AssetInfo.Camera, *(assets.toList().sortedByDescending {
            LocalDateTime.parse(it.first)
        }.flatMap { (time, list) ->
            list.map {
                time to it
            }.sortedByDescending {
                LocalDateTime.parse(it.first)
            }
        }.toTypedArray()))


    }

    LazyVerticalGrid(
        state = gridState,//viewModel.lazyState.value ?: rememberLazyGridState(),
        columns = GridCells.Fixed(3),
        content = {
            itemsIndexed(flattenedList, key = { index, (time, assetInfo) ->
                assetInfo.id
            }) { index, (time, assetInfo) ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    AssetImage(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight)
                            .padding(horizontal = 1.dp, vertical = 1.dp),
                        assetInfo = assetInfo,
                        navigateToPreview = {
                            viewModel.navigateToPreview(
                                index-1,
                                time,
                                requestType
                            )
                        },
                        selectedList = viewModel.selectedList,
                        onCameraClicks = {
                            cameraUri = viewModel.getUri()
                            cameraLauncher.launch(cameraUri)
                        },
                        onLongClick = { selected ->
                            viewModel.toggleSelect(
                                selected,
                                assetInfo
                            )
                        }
                    )
                }
            }
            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(padding.value))
            }
        }
    )
}

@Composable
expect fun getScreenSize(current: PlatformContext): Dp

@Composable
private fun AssetImage(
    modifier: Modifier = Modifier,
    assetInfo: AssetInfo,
    selectedList: SnapshotStateList<AssetInfo>,
    navigateToPreview: () -> Unit,
    onCameraClicks: () -> Unit = {},
    onLongClick: (Boolean) -> Unit,
) {
    val context = LocalPlatformContext.current
    val maxAssets = LocalAssetConfig.current.maxAssets

    if(assetInfo is AssetInfo.Camera) {
        val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)
        Box(
            modifier = modifier.fillMaxSize().clickable {
                if (selectedList.size < maxAssets) {
                    onCameraClicks()
                } else {
                    showToast(context, errorMessage)
                }
            },
        ){
            Column(
                modifier=Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon_camera),
                    contentDescription = "",
                    modifier=Modifier.wrapContentSize()
                )
                Text("拍照", fontSize = 12.sp, color = Color.White)
            }
        }

      return
    }else{
        val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)
        val selected = remember(assetInfo.id) { mutableStateOf(selectedList.any { it.id == assetInfo.id }) }
        LaunchedEffect(Unit){
            snapshotFlow { selectedList.size }
                .distinctUntilChanged()
                .collect{
                    selected.value = selectedList.any { it.id == assetInfo.id }
                }
        }
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd,
        ) {
            AssetImageItem(
                filterQuality = FilterQuality.Low,
                urlString = assetInfo.uriString,
                isSelected = selected.value,
                resourceType = assetInfo.resourceType,
                durationString = assetInfo.formatDuration(),
                navigateToPreview = navigateToPreview,
                onLongClick = {
                    val selectResult = !selected.value
                    if (selected.value || selectedList.size < maxAssets) {
                        onLongClick(selectResult)
                    } else {
                        showToast(context, errorMessage)
                    }
                }
            )
            AssetImageIndicator(
                assetInfo = assetInfo,
                selected = selected.value,
                assetSelected = selectedList
            )
        }
    }
}



expect class CameraLauncher {
    fun launch(cameraUri: Uri?)
}

@Composable
expect fun rememberCameraLauncher(callback: (Boolean) -> Unit): CameraLauncher