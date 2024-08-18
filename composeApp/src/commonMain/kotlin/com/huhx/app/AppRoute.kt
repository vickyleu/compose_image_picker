package com.huhx.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.huhx.app.data.MomentViewModel
import com.huhx.app.view.MomentAddScreen
import com.huhx.app.view.MomentListScreen
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.model.toUri
import com.huhx.picker.view.AssetPicker
import com.huhx.picker.view.permissionHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppRoute(
    navController: NavHostController,
    viewModel: MomentViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = "asset_picker",
    ) {
        composable("moment_list") {
            MomentListScreen(viewModel) { navController.navigate("moment_add") }
        }

        composable("moment_add") {
            MomentAddScreen(viewModel, navController, navController::navigateUp)
        }

        composable("asset_picker") {
            val scope = rememberCoroutineScope()
            ImagePicker(
                onPicked = {
                    viewModel.selectedList.clear()
                    viewModel.selectedList.addAll(it)
                    navController.navigateUp()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            delay(500)
                            viewModel.selectedList.clear()
                        }
                    }
                },
                onClose = {
                    viewModel.selectedList.clear()
                    navController.navigateUp()
                }
            )
        }
    }
}

@Composable
fun ImagePicker(
    onPicked: (List<AssetInfo>) -> Unit,
    onClose: (List<AssetInfo>) -> Unit,
) {
    permissionHandle {
        AssetPicker(
            assetPickerConfig = AssetPickerConfig(gridCount = 3),
            onPicked = onPicked,
            onClose = onClose
        )
    }

}
