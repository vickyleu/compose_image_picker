package com.huhx.picker.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import coil3.compose.LocalPlatformContext
import com.dokar.sonner.ToasterState
import com.github.jing332.filepicker.base.FileImpl
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
    toasterState: ToasterState?=null,
    onPicked: (List<FileImpl>) -> Unit,
    onClose: (List<FileImpl>) -> Unit,
    onLoading: @Composable (() -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val viewModel: AssetViewModel = viewModel(
        factory = AssetViewModelFactory(
            assetPickerRepository = AssetPickerRepository(context),
        )
    )
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            viewModel.initDirectories()
            isLoading.value = false
        }
    }

    when {
        isLoading.value -> {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black),
                contentAlignment = Alignment.Center){
                onLoading?.invoke() ?: CircularProgressIndicator()
            }

        }
        else -> {
            CompositionLocalProvider(LocalAssetConfig provides assetPickerConfig) {
                AssetPickerRoute(
                    viewModel = viewModel,
                    toasterState=toasterState,
                    onPicked = onPicked,
                    onClose = onClose,
                    assetPickerConfig = assetPickerConfig
                )
            }
        }
    }
}

@Composable
expect fun permissionHandle(content: @Composable (() -> Unit))