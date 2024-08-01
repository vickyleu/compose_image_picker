package com.huhx.picker.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
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
import compose_image_picker.imagepicker.generated.resources.label_album
import compose_image_picker.imagepicker.generated.resources.label_camera
import compose_image_picker.imagepicker.generated.resources.message_selected_exceed
import compose_image_picker.imagepicker.generated.resources.tab_item_all
import compose_image_picker.imagepicker.generated.resources.tab_item_image
import compose_image_picker.imagepicker.generated.resources.tab_item_video
import compose_image_picker.imagepicker.generated.resources.text_deselect_all
import compose_image_picker.imagepicker.generated.resources.text_select_all
import compose_image_picker.imagepicker.generated.resources.text_select_tip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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

    Scaffold(
        topBar = {
            DisplayTopAppBar(
                directory = viewModel.directory,
                selectedList = viewModel.selectedList,
                navigateUp = onClose,
                navigateToDropDown = navigateToDropDown
            )
        },
        bottomBar = {
            DisplayBottomBar(viewModel, onPicked)
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            val tabs = listOf(TabItem.All, TabItem.Video, TabItem.Image)
            val pagerState = rememberPagerState(pageCount = tabs::size)

            Column {
                AssetTab(tabs = tabs, pagerState = pagerState)
                HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
                    tabs[page].screen(viewModel)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DisplayTopAppBar(
    directory: String,
    selectedList: List<AssetInfo>,
    navigateUp: (List<AssetInfo>) -> Unit,
    navigateToDropDown: (String) -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {
            IconButton(onClick = { navigateUp(selectedList) }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "")
            }
        },
        title = {
            Row(modifier = Modifier.clickable { navigateToDropDown(directory) }) {
                Text(text = directory, fontSize = 18.sp)
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "")
            }
        },
    )
}


@Composable
private fun DisplayBottomBar(viewModel: AssetViewModel, onPicked: (List<AssetInfo>) -> Unit) {
    var cameraUri: Uri? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberCameraLauncher { success ->
        if (success) {
            cameraUri?.let { scope.launch { viewModel.initDirectories() } }
        } else {
            viewModel.deleteImage(cameraUri)
        }
    }


    if (viewModel.selectedList.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TextButton(
                onClick = {
                    cameraUri = viewModel.getUri()
                    cameraLauncher.launch(cameraUri)
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
                content = { Text(text = stringResource(Res.string.label_album), fontSize = 16.sp) }
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

@Composable
private fun AssetTab(tabs: List<TabItem>, pagerState: PagerState) {
    val scope = rememberCoroutineScope()

    TabRow(selectedTabIndex = pagerState.currentPage, indicator = {}) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = pagerState.currentPage == index,
                text = { Text(text = stringResource(tab.resourceId)) },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = Color.Gray,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssetContent(viewModel: AssetViewModel, requestType: RequestType) {
    val assets = viewModel.getGroupedAssets(requestType)
    val context = LocalPlatformContext.current
    val gridCount = LocalAssetConfig.current.gridCount
    val maxAssets = LocalAssetConfig.current.maxAssets
    val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)

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

    LazyColumn {
        assets.forEach { (dateString, resources) ->
            val allSelected = viewModel.isAllSelected(resources)
            val isAlreadyFull = viewModel.selectedList.size == maxAssets
            val hasSelected = viewModel.hasSelected(resources)

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )

                    TextButton(onClick = {
                        if (allSelected || (isAlreadyFull && hasSelected)) {
                            viewModel.unSelectAll(resources)
                        } else {
                            if (viewModel.selectAll(resources, maxAssets)) {
                                showToast(context, errorMessage)
                            }
                        }
                    }) {
                        Text(
                            text = if (allSelected || (isAlreadyFull && hasSelected)) {
                                stringResource(Res.string.text_deselect_all)
                            } else {
                                stringResource(Res.string.text_select_all)
                            }
                        )
                    }
                }
            }

            item {
                val itemSize: Dp = (getScreenSize(LocalPlatformContext.current) / gridCount)
                FlowRow(maxItemsInEachRow = gridCount) {
                    resources.forEachIndexed { index, assetInfo ->
                        AssetImage(
                            modifier = Modifier
                                .size(itemSize)
                                .padding(horizontal = 1.dp, vertical = 1.dp),
                            assetInfo = assetInfo,
                            navigateToPreview = {
                                viewModel.navigateToPreview(
                                    index,
                                    dateString,
                                    requestType
                                )
                            },
                            selectedList = viewModel.selectedList,
                            onLongClick = { selected ->
                                viewModel.toggleSelect(
                                    selected,
                                    assetInfo
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
expect fun getScreenSize(current: PlatformContext): Dp

@Composable
private fun AssetImage(
    modifier: Modifier = Modifier,
    assetInfo: AssetInfo,
    selectedList: SnapshotStateList<AssetInfo>,
    navigateToPreview: () -> Unit,
    onLongClick: (Boolean) -> Unit,
) {
    val selected = selectedList.any { it.id == assetInfo.id }
    val context = LocalPlatformContext.current
    val maxAssets = LocalAssetConfig.current.maxAssets
    val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd,
    ) {
        AssetImageItem(
            filterQuality = FilterQuality.None,
            urlString = assetInfo.uriString,
            isSelected = selected,
            resourceType = assetInfo.resourceType,
            durationString = assetInfo.formatDuration(),
            navigateToPreview = navigateToPreview,
            onLongClick = {
                val selectResult = !selected
                if (selected || selectedList.size < maxAssets) {
                    onLongClick(selectResult)
                } else {
                    showToast(context, errorMessage)
                }
            }
        )
        AssetImageIndicator(
            assetInfo = assetInfo,
            selected = selected,
            assetSelected = selectedList
        )
    }
}

private sealed class TabItem(
    val resourceId: StringResource,
    val screen: @Composable (AssetViewModel) -> Unit,
) {
    data object All :
        TabItem(
            Res.string.tab_item_all,
            { viewModel -> AssetContent(viewModel, RequestType.COMMON) })

    data object Video : TabItem(
        Res.string.tab_item_video,
        { viewModel -> AssetContent(viewModel, RequestType.VIDEO) })

    data object Image : TabItem(
        Res.string.tab_item_image,
        { viewModel -> AssetContent(viewModel, RequestType.IMAGE) })
}

expect class CameraLauncher {
    fun launch(cameraUri: Uri?)
}

@Composable
expect fun rememberCameraLauncher(callback: (Boolean) -> Unit): CameraLauncher