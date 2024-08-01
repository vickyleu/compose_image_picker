package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType

internal actual abstract class AssetLoader {
    actual fun insertImage(context: PlatformContext): Uri? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun deleteByUri(context: PlatformContext, uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun findByUri(context: PlatformContext, uri: Uri): AssetInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun load(context: PlatformContext, requestType: RequestType): List<AssetInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}