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
