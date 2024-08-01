package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType

internal class AssetPickerRepository(
    private val context: PlatformContext
) {
    suspend fun getAssets(requestType: RequestType): List<AssetInfo> {
        return AssetLoader.load(context, requestType)
    }

    fun insertImage(): Uri? {
        return AssetLoader.insertImage(context)
    }

    fun findByUri(uri: Uri?): AssetInfo? {
        return uri?.let { AssetLoader.findByUri(context, it) }
    }

    fun deleteByUri(uri: Uri?) {
        uri?.let { AssetLoader.deleteByUri(context, it) }
    }
}