@file:OptIn(ExperimentalComposeUiApi::class)

package com.huhx.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import com.github.jing332.filepicker.base.FileImpl
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.model.RequestType
import com.huhx.picker.model.toUri
import com.huhx.picker.view.AssetDisplayScreen
import com.huhx.picker.view.AssetPreviewScreen
import com.huhx.picker.view.AssetSelectorScreen
import com.huhx.picker.viewmodel.AssetViewModel
import com.huhx.picker.viewmodel.LocalAssetViewModelProvider
import kotlinx.coroutines.launch

@Composable
internal fun AssetPickerRoute(
    viewModel: AssetViewModel,
    toasterState: ToasterState? = null,
    onPicked: (List<FileImpl>) -> Unit,
    onClose: (List<FileImpl>) -> Unit,
    assetPickerConfig: AssetPickerConfig
) {
    val scope = rememberCoroutineScope()
    
    // 导航状态管理
    var currentScreen by remember { mutableStateOf("display") }
    var previewIndex by remember { mutableStateOf(0) }
    var previewTime by remember { mutableStateOf("") }
    var previewRequestType by remember { mutableStateOf(RequestType.COMMON) }
    var selectorDirectory by remember { mutableStateOf("") }
    
    // 统一的返回处理逻辑
    BackHandler(enabled = true) {
        when (currentScreen) {
            "display" -> {
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
            }
            "preview" -> {
                // 从预览返回到主界面
                currentScreen = "display"
            }
            "selector" -> {
                // 从选择器返回到主界面
                currentScreen = "display"
            }
        }
    }
    
    val displayScreen = remember {
        AssetDisplayScreen(
            viewModel = viewModel,
            toasterState = toasterState,
            onPicked = {
                scope.launch {
                    val list = mutableListOf<FileImpl>()
                    println("viewModel.selectedList::${viewModel.selectedList.size}")
                    viewModel.selectedList.mapNotNull {
                        val path = it.toUri().path
                        println("path::${path}  size::${it.size}  name::${it.filename}  ${it.filepath}")
                        if (path != null && it.size > 0) {
                            FileImpl(path)
                        } else null
                    }.let {
                        if (it.size == viewModel.selectedList.size) {
                            list.addAll(it)
                            viewModel.selectedList.clear()
                            onPicked(list)
                        } else {
                            toasterState?.show("文件下载中，请稍后再试", type = ToastType.Toast)
                                ?: kotlin.run {
                                    list.addAll(it)
                                    viewModel.selectedList.clear()
                                    onPicked(list)
                                }
                        }
                    }
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
            }, assetPickerConfig = assetPickerConfig
        )
    }
    
    CompositionLocalProvider(LocalAssetViewModelProvider provides viewModel) {
        when (currentScreen) {
            "display" -> {
                displayScreen.Content(
                    onNavigateUp = { onClose(emptyList()) },
                    onNavigate = { route ->
                        when {
                            route.startsWith("preview/") -> {
                                val parts = route.substringAfter("preview/").split("/")
                                if (parts.size >= 3) {
                                    previewIndex = parts[0].toIntOrNull() ?: 0
                                    previewTime = parts[1]
                                    previewRequestType = try {
                                        RequestType.valueOf(parts[2])
                                    } catch (e: Exception) {
                                        RequestType.COMMON
                                    }
                                    currentScreen = "preview"
                                }
                            }
                            route.startsWith("selector/") -> {
                                selectorDirectory = route.substringAfter("selector/")
                                currentScreen = "selector"
                            }
                        }
                    }
                )
            }
            "preview" -> {
                val previewScreen = remember(previewIndex, previewTime, previewRequestType) {
                    AssetPreviewScreen(
                        viewModel = viewModel,
                        toasterState = toasterState,
                        index = previewIndex,
                        time = previewTime,
                        requestType = previewRequestType
                    )
                }
                previewScreen.Content(
                    onNavigateUp = { currentScreen = "display" },
                    onNavigate = { /* Handle nested navigation if needed */ }
                )
            }
            "selector" -> {
                val selectorScreen = remember(selectorDirectory) {
                    AssetSelectorScreen(
                        directory = selectorDirectory,
                        assetDirectories = viewModel.directoryGroup,
                        onSelected = { name ->
                            viewModel.directory = formatDirectoryName(name) to name
                            currentScreen = "display"
                        }
                    )
                }
                selectorScreen.Content(
                    onNavigateUp = { currentScreen = "display" },
                    onNavigate = { /* Handle nested navigation if needed */ }
                )
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