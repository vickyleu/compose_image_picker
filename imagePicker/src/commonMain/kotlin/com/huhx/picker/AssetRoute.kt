package com.huhx.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import com.github.jing332.filepicker.base.FileImpl
import com.huhx.picker.base.LocalNavigatorController
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.model.RequestType
import com.huhx.picker.model.toUri
import com.huhx.picker.view.AssetDisplayScreen
import com.huhx.picker.viewmodel.AssetViewModel
import com.huhx.picker.viewmodel.LocalAssetViewModelProvider
import kotlinx.coroutines.launch

@OptIn(InternalVoyagerApi::class)
@Composable
internal fun AssetPickerRoute(
    viewModel: AssetViewModel,
    onPicked: (List<FileImpl>) -> Unit,
    onClose: (List<FileImpl>) -> Unit,
    assetPickerConfig: AssetPickerConfig
) {
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalAssetViewModelProvider provides viewModel) {
        val startScreen = remember {
            AssetDisplayScreen(viewModel = viewModel, onPicked = {
                scope.launch {
                    val list = mutableListOf<FileImpl>()
                    list.addAll(viewModel.selectedList.mapNotNull {
                        val path = it.toUri().path
                        if (path != null && it.size > 0) {
                            FileImpl(path)
                        } else null
                    })
                    viewModel.selectedList.clear()
                    onPicked(list)
                }
            }, onClose = {
                scope.launch {
                    val list = mutableListOf<FileImpl>()
                    list.addAll(viewModel.selectedList.mapNotNull {
                        val path = it.toUri().path
                        if (path != null && it.size > 0) {
                            FileImpl(path)
                        } else null
                    })
                    viewModel.selectedList.clear()
                    onClose(list)
                }
            }, assetPickerConfig = assetPickerConfig)
        }
        Navigator(
            screen = startScreen,
            disposeBehavior = NavigatorDisposeBehavior(
                disposeNestedNavigators = false,
                disposeSteps = false
            ),
            onBackPressed = { currentScreen ->
                false
            }
        ) {
            CompositionLocalProvider(LocalNavigatorController provides it) {
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