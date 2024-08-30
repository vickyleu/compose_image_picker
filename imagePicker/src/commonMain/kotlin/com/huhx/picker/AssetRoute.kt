package com.huhx.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import com.huhx.picker.base.LocalNavigatorController
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType
import com.huhx.picker.view.AssetDisplayScreen
import com.huhx.picker.viewmodel.AssetViewModel
import com.huhx.picker.viewmodel.LocalAssetViewModelProvider

@OptIn(InternalVoyagerApi::class)
@Composable
internal fun AssetPickerRoute(
    viewModel: AssetViewModel,
    onPicked: (List<AssetInfo>) -> Unit,
    onClose: (List<AssetInfo>) -> Unit,
) {
    CompositionLocalProvider(LocalAssetViewModelProvider provides viewModel) {
        val startScreen = remember{  AssetDisplayScreen(viewModel = viewModel, onPicked = {
            val list = mutableListOf<AssetInfo>()
            list.addAll(viewModel.selectedList)
            viewModel.selectedList.clear()
            onPicked(list)
        }, onClose = {
            val list = mutableListOf<AssetInfo>()
            list.addAll(viewModel.selectedList)
            viewModel.selectedList.clear()
            onClose(list)
        }) }
        Navigator(
            screen = startScreen,
            disposeBehavior = NavigatorDisposeBehavior(
                disposeNestedNavigators = false,
                disposeSteps = false
            ),
            onBackPressed = { currentScreen ->
                false
            }
        ){
            CompositionLocalProvider(LocalNavigatorController provides it){
                CurrentScreen()
            }
        }
    }
}

//相册名称格式化成中文名字
fun formatDirectoryName(name: String): String {
    return when (name) {
        "Camera" -> "相机"
        "Screenshots" -> "截图"
        "Download" -> "下载"
        "Pictures" -> "图片"
        "Movies" -> "视频"
        else -> name
    }
}

object AssetRoute {
    const val display = "asset_display"
    const val preview =
        "asset_preview?index={index}&dateString={dateString}&requestType={requestType}"
    const val selector = "asset_selector?directory={directory}"

    fun preview(index: Int, dateString: String, requestType: RequestType): String {
        return preview.replaceFirst("{index}", index.toString())
            .replaceFirst("{dateString}", dateString)
            .replaceFirst("{requestType}", requestType.name).apply {
                println("preview======>> $this")
            }
    }

    fun selector(directory: String): String {
        return selector.replaceFirst("{directory}", directory)
    }
}