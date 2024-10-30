package com.huhx.app

import androidx.compose.runtime.Composable
import com.github.jing332.filepicker.base.FileImpl
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.view.AssetPicker
import com.huhx.picker.view.permissionHandle


@Composable
fun ImagePicker(
    onPicked: (List<FileImpl>) -> Unit,
    onClose: (List<FileImpl>) -> Unit,
) {
    permissionHandle {
        AssetPicker(
            assetPickerConfig = AssetPickerConfig(
                gridCount = 3,
                requestType = com.huhx.picker.model.RequestType.COMMON
            ),
            onPicked = onPicked,
            onClose = onClose
        )
    }

}
