package com.huhx.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.DateTimeFormatterKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.view.AssetDisplayScreen
import com.huhx.picker.view.AssetPreviewScreen
import com.huhx.picker.view.AssetSelectorScreen
import com.huhx.picker.viewmodel.AssetViewModel
import kotlinx.datetime.LocalDateTime

@OptIn(InternalVoyagerApi::class)
@Composable
internal fun AssetPickerRoute(
    navController: NavHostController,
    viewModel: AssetViewModel,
    onPicked: (List<AssetInfo>) -> Unit,
    onClose: (List<AssetInfo>) -> Unit,
) {

    BackHandler(enabled = true,onBack = {
        if (viewModel.selectedList.isNotEmpty()) {
            onPicked(viewModel.selectedList)
        } else {
            onClose(viewModel.selectedList)
        }
    })
    NavHost(navController = navController, startDestination = AssetRoute.display) {
        composable(route = AssetRoute.display) {
            AssetDisplayScreen(
                viewModel = viewModel,
                navigateToDropDown = { navController.navigate(AssetRoute.selector(it)) },
                onPicked = onPicked,
                onClose = onClose,
            )
        }

        composable(
            route = AssetRoute.selector,
            arguments = listOf(navArgument("directory") { type = NavType.StringType })
        ) {
            val directory = it.arguments!!.getString("directory")!!

            AssetSelectorScreen(
                directory = directory,
                assetDirectories = viewModel.directoryGroup,
                navigateUp = { navController.navigateUp() },
                onSelected = { name ->
                    navController.navigateUp()
                    viewModel.directory = formatDirectoryName(name) to name
                },
            )
        }

        composable(
            route = AssetRoute.preview,
            arguments = listOf(
                navArgument("index") { type = NavType.IntType },
                navArgument("dateString") { type = NavType.StringType },
                navArgument("requestType") { type = NavType.StringType },
            )
        ) {
            val arguments = it.arguments!!
            val requestType = arguments.getString("requestType")
            val assets = viewModel.getGroupedAssets(RequestType.valueOf(requestType!!))
            var index by remember {
                mutableIntStateOf(arguments.getInt("index"))
            }
            val dtf = remember{ DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日 HH:mm:ss") }
            val flattenedList = remember {
                assets.toList().sortedByDescending {
                    dtf.parse(it.first)
                }.flatMap { (time, list) ->
                    list.map {
                        time to it
                    }.sortedByDescending {
                        dtf.parse(it.first)
                    }
                }
            }
            AssetPreviewScreen(
                index = index,
                assets = flattenedList,
                selectedList = viewModel.selectedList,
                navigateUp = { navController.navigateUp() },
                onSelectChanged = { assetInfo ->
                    viewModel.selectedList.sortBy { asset ->
                        flattenedList.indexOfFirst { it.second == asset }
                    }
                    index = viewModel.selectedList.indexOf(assetInfo)
                }
            )
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
            .replaceFirst("{requestType}", requestType.name)
    }

    fun selector(directory: String): String {
        return selector.replaceFirst("{directory}", directory)
    }
}