package com.huhx.picker.view

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import coil3.compose.LocalPlatformContext
import com.huhx.picker.AssetPickerRoute
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.provider.AssetPickerRepository
import com.huhx.picker.viewmodel.AssetViewModel
import com.huhx.picker.viewmodel.AssetViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
fun AssetPicker(
    assetPickerConfig: AssetPickerConfig,
    onPicked: (List<AssetInfo>) -> Unit,
    onClose: (List<AssetInfo>) -> Unit,
    onLoading: @Composable (() -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val navController = rememberNavController()
    val viewModel: AssetViewModel = viewModel(
        factory = AssetViewModelFactory(
            assetPickerRepository = AssetPickerRepository(context),
            navController = navController
        )
    )

    println("viewModel:::${viewModel.hashCode()}")


    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            viewModel.initDirectories()
            isLoading.value = false
        }
    }

    when {
        isLoading.value -> {
            onLoading?.invoke() ?: CircularProgressIndicator()
        }
        else -> {
            CompositionLocalProvider(LocalAssetConfig provides assetPickerConfig) {
                AssetPickerRoute(
                    navController = navController,
                    viewModel = viewModel,
                    onPicked = onPicked,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
expect fun permissionHandle(content: @Composable (() -> Unit))