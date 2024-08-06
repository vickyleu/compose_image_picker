package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import coil3.toUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSPredicate
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumUserLibrary
import platform.Photos.PHAssetCollectionTypeSmartAlbum
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetResource
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

 actual abstract class AssetLoader {

    actual companion object {
        private fun Uri.toNativeUri(): NSURL {
            return NSURL(string = this.path!!)
        }

        private val NSURL.uriString: String
            get() = (this.absoluteString ?: "")

        private fun generateTemporaryUri(): Uri {
            val uuid = NSUUID.UUID().UUIDString
            return uuid.toUri()
        }

        private fun saveImage(): Uri? {
            var insertedUri: Uri? = null
            val semaphore = dispatch_semaphore_create(0)
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                /* val creationRequest = PHAssetChangeRequest.creationRequestForAssetFromImage()
                 val placeholder = creationRequest.placeholderForCreatedAsset()
                 insertedUri = placeholder?.localIdentifier?.toUri()*/
            }) { success, error ->
                if (!success) {
                    println("Error inserting image: ${error?.localizedDescription}")
                }
                dispatch_semaphore_signal(semaphore)
            }
            dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
            return insertedUri
        }


        actual suspend fun insertImage(context: PlatformContext): Uri? {
            return generateTemporaryUri()
        }

        actual suspend fun deleteByUri(context: PlatformContext, uri: Uri) {
            val assetUri = uri.toNativeUri()
            val fetchOptions = PHFetchOptions()
            val assets = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetUri), fetchOptions)
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                PHAssetChangeRequest.deleteAssets(assets)
            }) { success, error ->
                if (!success) {
                    println("Error deleting image: ${error?.localizedDescription}")
                }
            }
        }

        actual suspend fun findByUri(context: PlatformContext, uri: Uri): AssetInfo? {
            val assetUri = uri.toNativeUri()
            val fetchOptions = PHFetchOptions()
            val assets = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetUri), fetchOptions)
            val asset = assets.firstObject() as? PHAsset ?: return null

            val fileName = (PHAssetResource.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource)?.originalFilename ?: ""
            val mimeType = "image/jpeg" // 根据需要设置MIME类型
            return AssetInfo(
                id = asset.localIdentifier,
                uriString = assetUri.uriString,
                filepath = "", // iOS上没有直接的文件路径
                filename = fileName,
                date = asset.creationDate?.timeIntervalSince1970?.toLong() ?: 0L,
                mediaType = asset.mediaType.toInt(),
                mimeType = mimeType,
                size = 0L, // 需要进一步实现获取文件大小
                duration = asset.duration.toLong(),
                directory = "" // iOS上没有直接的文件目录
            )
        }

        @OptIn(ExperimentalForeignApi::class)
        actual suspend fun load(
            context: PlatformContext,
            requestType: RequestType
        ): List<AssetInfo> {
            val assets = mutableListOf<AssetInfo>()
            val fetchOptions = PHFetchOptions()
            val completer = CompletableDeferred<MutableList<AssetInfo>>()
            withContext(Dispatchers.IO) {
                when (requestType) {
                    RequestType.COMMON -> fetchOptions.predicate = NSPredicate.predicateWithFormat(
                        "mediaType == %d OR mediaType == %d",
                        PHAssetMediaTypeImage,
                        PHAssetMediaTypeVideo
                    )

                    RequestType.IMAGE -> fetchOptions.predicate =
                        NSPredicate.predicateWithFormat("mediaType == %d", PHAssetMediaTypeImage)

                    RequestType.VIDEO -> fetchOptions.predicate =
                        NSPredicate.predicateWithFormat("mediaType == %d", PHAssetMediaTypeVideo)
                }
                // 获取相册集合
                val collections = PHAssetCollection.fetchAssetCollectionsWithType(
                    PHAssetCollectionTypeSmartAlbum, PHAssetCollectionSubtypeSmartAlbumUserLibrary, null)
                println("查到新的照片 ${collections.count}")
                collections.enumerateObjectsUsingBlock { collection, _, _ ->
                    val phac = collection as PHAssetCollection
                    val collectionName = phac.localizedTitle ?: "Unknown"
                    // 根据相册集合获取资产
                    val fetchedAssets = PHAsset.fetchAssetsInAssetCollection(phac,fetchOptions)
                    fetchedAssets.enumerateObjectsUsingBlock { asset, _, _->
                        if (asset !is PHAsset) return@enumerateObjectsUsingBlock
                        val fileName = asset.valueForKey("filename") as? String ?: ""
                        val mimeType = "image/jpeg" // 根据需要设置MIME类型
                        assets.add(
                            AssetInfo(
                                id = asset.localIdentifier,
                                uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                                filepath = "", // iOS上没有直接的文件路径
                                filename = fileName,
                                date = asset.creationDate?.timeIntervalSince1970?.toLong()?.times(1000) ?: 0L,
                                mediaType = asset.mediaType.toInt(),
                                mimeType = mimeType,
                                size = 0L, // 需要进一步实现获取文件大小
                                duration = asset.duration.toLong(),
                                directory = collectionName // iOS上没有直接的文件目录
                            )
                        )
                    }
                }
                completer.complete(assets)
            }
            completer.await()
            return assets
        }
    }
}