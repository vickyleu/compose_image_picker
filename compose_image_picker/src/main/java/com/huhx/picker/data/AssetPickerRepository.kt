package com.huhx.picker.data

import android.content.Context
import android.net.Uri
import com.huhx.picker.constant.RequestType

class AssetPickerRepository(
    private val context: Context
) {
    fun getAssets(requestType: RequestType): List<AssetInfo> {
        return AssetLoader.load(context, requestType)
    }

    fun insertImage(): Uri? {
        return AssetLoader.insertImage(context)
    }

    fun findByUri(uri: Uri?): AssetInfo? {
        return uri?.let {
            AssetLoader.findByUri(context, it)
        }
    }
}