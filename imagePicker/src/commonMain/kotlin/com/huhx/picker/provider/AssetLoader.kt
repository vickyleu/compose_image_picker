package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType

internal expect abstract class AssetLoader {
     fun insertImage(context: PlatformContext): Uri?
     fun deleteByUri(context: PlatformContext, uri: Uri)
     fun findByUri(context: PlatformContext, uri: Uri): AssetInfo?
     fun load(context: PlatformContext, requestType: RequestType): List<AssetInfo>
}