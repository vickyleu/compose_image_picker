package com.huhx.picker.view

//import compose_image_picker.imagepicker.generated.resources.message_selected_exceed
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Density
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
import com.huhx.picker.model.DateTimeFormatterKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.getNavigationBarHeight
import com.huhx.picker.util.goToAppSetting
import com.huhx.picker.viewmodel.AssetViewModel
import compose_image_picker.imagepicker.generated.resources.Res
import compose_image_picker.imagepicker.generated.resources.icon_back
import compose_image_picker.imagepicker.generated.resources.icon_camera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource


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

    with(LocalDensity.current) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                DisplayTopAppBar(
                    selectedList = viewModel.selectedList,
                    navigateUp = onClose, onPicked = onPicked,
                    initialTopBarHeight = initialTopBarHeight,
                )
            },
            bottomBar = {
                DisplayBottomBar(
                    viewModel,
                    initialBottomBarHeight = initialBottomBarHeight,
                    navigateToDropDown = navigateToDropDown
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Gray)
                    .padding(top = initialTopBarHeight.value)
                    .fillMaxSize()
            ) {
                AssetContent(viewModel, RequestType.IMAGE, initialBottomBarHeight)
            }
        }
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Density.DisplayTopAppBar(
    selectedList: List<AssetInfo>,
    navigateUp: (List<AssetInfo>) -> Unit,
    onPicked: (List<AssetInfo>) -> Unit,
    initialTopBarHeight: MutableState<Dp>,
) {
    with(LocalDensity.current) {

        Box(modifier = Modifier.fillMaxWidth().height(48.dp)
            .background(Color.Black)
            .onGloballyPositioned {
                val height = it.size.height.toDp()
                if (initialTopBarHeight.value < height) {
                    initialTopBarHeight.value = height
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .padding(8.dp)
                        .clickable {
                            navigateUp(selectedList)
                        }.padding(horizontal = 3.dp, vertical = 3.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.icon_back),
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillHeight,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .padding(end = 15.dp)
                        .background(ButtonDefaults.buttonColors().let {
                            if (selectedList.isEmpty()) Color.Gray else it.containerColor
                        }, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .clickable(enabled = selectedList.isNotEmpty()) {
                            onPicked(selectedList)
                        }
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
        }

        // todo iOS中的TopAppBar不能正常显示,修改为普通的Box
        /*CenterAlignedTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .onGloballyPositioned {
                    val height = it.size.height.toDp()
                    if (initialTopBarHeight.value < height) {
                        initialTopBarHeight.value = height
                    }
                },
            navigationIcon = {

            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black
            ),
            title = {
                *//*Row(modifier = Modifier.clickable { navigateToDropDown(directory) }) {
                    Text(text = directory, fontSize = 18.sp, color = Color.White)
                    Icon(imageVector = Icons.Default.KeyboardArrowDown,
                        tint = Color.White,
                        contentDescription = "")
                }*//*
            },
            actions = {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(ButtonDefaults.buttonColors().let {
                            if (selectedList.isEmpty()) Color.Gray else it.containerColor
                        }, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .clickable(enabled = selectedList.isNotEmpty()) {
                            onPicked(selectedList)
                        }
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
        )*/
    }
}


@Composable
private fun Density.DisplayBottomBar(
    viewModel: AssetViewModel,
    initialBottomBarHeight: MutableState<Dp>,
    navigateToDropDown: (String) -> Unit
) {
    with(LocalDensity.current) {
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
                .padding(bottom = getNavigationBarHeight())
                .pointerInput(Unit) {
                    detectTapGestures {
                        //
                    }
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                contentPadding = PaddingValues(0.dp),
                onClick = { navigateToDropDown(viewModel.directory.second) },
                content = {
                    Text(viewModel.directory.first, fontSize = 16.sp, color = Color.White)
                }
            )
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
    var assets by remember { mutableStateOf(viewModel.getGroupedAssets(requestType)) }
    val context = LocalPlatformContext.current
    val gridCount = LocalAssetConfig.current.gridCount
    val maxAssets = LocalAssetConfig.current.maxAssets
    val errorMessage =
        "你最多只能选择${maxAssets}个图片" //stringResource(Res.string.message_selected_exceed, maxAssets)
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
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberCameraLauncher(scope) { info ->
        if (info != null) {
            scope.launch { viewModel.initDirectories() }
        } else {
            scope.launch { viewModel.deleteImage(this@rememberCameraLauncher.uri) }
        }
    }


    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.assets.size }
            .distinctUntilChanged()
            .filter { it > 0 }
            .distinctUntilChanged()
            .collect {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val a = viewModel.getGroupedAssets(requestType)
                        if (a.isNotEmpty()) {
                            cameraLauncher.fetchCameraUri(a)?.let {
                                viewModel.selectedList.add(it)
                            }
                        }
                        assets = a
                    }
                }
            }
    }


    // 需要将assets 排序,最后将所有list flatten 到一个list里面去
    val dtf = remember { DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日 HH:mm:ss") }
    val flattenedList = remember(assets) {
        listOf("-" to AssetInfo.Camera, *(assets.toList().sortedByDescending {
            dtf.parse(it.first)
        }.flatMap { (time, list) ->
            list.map {
                time to it
            }.sortedByDescending {
                dtf.parse(it.first)
            }
        }.toTypedArray()))
    }

    val impl = LocalStoragePermission.current
        ?: throw IllegalStateException("LocalStoragePermission not found")
    var cameraPermission by remember { mutableStateOf(false) }
    var cameraPermissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cameraPermission = impl.checkCameraPermission()
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
                                index - 1,
                                time,
                                requestType
                            )
                        },
                        selectedList = viewModel.selectedList,
                        onCameraClicks = {
                            scope.launch {
                                cameraPermission = impl.checkCameraPermission()
                                if (cameraPermission.not()) {
                                    withContext(Dispatchers.IO) {
                                        if (cameraPermission) {
                                            cameraLauncher.launch(context, viewModel.getUri())
                                            return@withContext
                                        }
                                        if (cameraPermissionRequested) {
                                            context.goToAppSetting()
                                            return@withContext
                                        }
                                        cameraPermissionRequested = true
                                        impl.requestCameraPermission(
                                            onGranted = {
                                                cameraPermission = true
                                                scope.launch {
                                                    cameraLauncher.launch(
                                                        context,
                                                        viewModel.getUri()
                                                    )
                                                }
                                            },
                                            onDenied = {
                                                showToast(context, "请授予相机权限")
                                                context.goToAppSetting()
                                            }
                                        )
                                    }
                                    return@launch
                                } else {
                                    cameraLauncher.launch(context, viewModel.getUri())
                                }
                            }
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

    if (assetInfo is AssetInfo.Camera) {
        val errorMessage =
            "你最多只能选择${maxAssets}个图片"//stringResource(Res.string.message_selected_exceed, maxAssets)
        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
                .background(Color.Black)
                .clickable {
                    if (selectedList.size < maxAssets) {
                        onCameraClicks()
                    } else {
                        showToast(context, errorMessage)
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon_camera),
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(fraction = 0.4f)
                        .aspectRatio(1f),
                )
                Text("拍照", fontSize = 12.sp, color = Color.White)
            }
        }

        return
    } else {
        val errorMessage =
            "你最多只能选择${maxAssets}个图片"//stringResource(Res.string.message_selected_exceed, maxAssets)
        val selected =
            remember(assetInfo.id) { mutableStateOf(selectedList.any { it.id == assetInfo.id }) }
        LaunchedEffect(Unit) {
            snapshotFlow { selectedList.size }
                .distinctUntilChanged()
                .collect {
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
    fun launch(context: PlatformContext, uri: Uri?)
    fun fetchCameraUri(assets: Map<String, List<AssetInfo>>): AssetInfo?
    val uri: Uri?
}

@Composable
expect fun rememberCameraLauncher(
    scope: CoroutineScope,
    onCreate: (CameraLauncher) -> Unit = {},
    callback: CameraLauncher.(AssetInfo?) -> Unit
): CameraLauncher