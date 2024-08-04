package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType

internal expect abstract class AssetLoader {
    companion object {
        suspend fun insertImage(context: PlatformContext): Uri?
        suspend fun deleteByUri(context: PlatformContext, uri: Uri)
        suspend fun findByUri(context: PlatformContext, uri: Uri): AssetInfo?
        suspend fun load(context: PlatformContext, requestType: RequestType): List<AssetInfo>
    }
}