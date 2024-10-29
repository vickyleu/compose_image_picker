package com.huhx.picker.provider

import coil3.PlatformContext
import coil3.Uri
import coil3.toUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.MediaStoreKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.model.toUri
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import platform.Foundation.NSPredicate
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
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
import platform.Photos.PHContentEditingInputRequestOptions
import platform.Photos.PHFetchOptions
import platform.Photos.PHFetchResult
import platform.Photos.PHPhotoLibrary
import platform.Photos.requestContentEditingInputWithOptions

actual abstract class AssetLoader {

    actual companion object {
        private val platform.Photos.PHAssetMediaType.convertMediaType: Int
            get() {
                return when (this) {
                    PHAssetMediaTypeImage -> MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_IMAGE
                    PHAssetMediaTypeVideo -> MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_VIDEO
                    else -> MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_IMAGE
                }
            }

        private fun Uri.toNativeUri(): NSURL {
            return NSURL(string = this.path!!)
        }

        internal val NSURL.uriString: String
            get() = (this.absoluteString ?: "")

        private fun generateTemporaryUri(): Uri {
            val uuid = NSUUID.UUID().UUIDString
            return uuid.toUri()
        }


        actual suspend fun insertImage(context: PlatformContext): Uri? {
            return generateTemporaryUri()
        }

        actual suspend fun deleteByUri(context: PlatformContext, uri: Uri) {
            //uriString: phasset://A4D3D3D3-3D3D-3D3D-3D3D-3D3D3D3D3D3D/L0/001
            val uriStr = uri.path ?: return
            val localIdentifier = if (uriStr.startsWith("phasset://")) {
                 uriStr.substring("phasset://".length)
            }else{
                uriStr
            }
            val assetsFetch = localIdentifier.toPHAssetFetch()
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                PHAssetChangeRequest.deleteAssets(assetsFetch)
            }) { success, error ->
                if (!success) {
                    println("Error deleting image: ${error?.localizedDescription}")
                }
            }
        }

        actual suspend fun findByUri(context: PlatformContext, uri: Uri): AssetInfo? {
            val uriStr = uri.path ?: return null
            val localIdentifier = if (uriStr.startsWith("phasset://")) {
                uriStr.substring("phasset://".length)
            }else{
                uriStr
            }
            val asset = localIdentifier.toPHAsset()?:return null
            val fileName = asset.valueForKey("filename") as? String ?: ""
            val mimeType = "image/jpeg" // 根据需要设置MIME类型
            return AssetInfoImpl(
                id = asset.localIdentifier,
                uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                filename = fileName,
                date = asset.creationDate?.timeIntervalSince1970?.toLong() ?: 0L,
                mediaType = asset.mediaType.convertMediaType,
                mimeType = mimeType,
                duration = (asset.duration*1000F).toLong(),
                directory = "Photo", // iOS上没有直接的文件目录
            )
        }

        @OptIn(ExperimentalForeignApi::class)
        actual suspend fun load(
            context: PlatformContext, requestType: RequestType, onlyLast: Boolean
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
                fetchOptions.sortDescriptors = listOf(
                    NSSortDescriptor("creationDate", false)
                )

                if (onlyLast) {
                    fetchOptions.fetchLimit = 1u
                    // 获取相册
                    val fetchedAssets = PHAsset.fetchAssetsWithOptions(fetchOptions)
                    fetchedAssets.enumerateObjectsUsingBlock { asset, _, _ ->
                        if (asset !is PHAsset) return@enumerateObjectsUsingBlock
                        val fileName = asset.valueForKey("filename") as? String ?: ""
                        val mimeType = when(asset.mediaType){
                            PHAssetMediaTypeImage -> "image/jpeg"
                            PHAssetMediaTypeVideo -> "video/mp4"
                            else -> "image/jpeg"
                        }// 根据需要设置MIME类型
//                            val mimeType = "image/jpeg"
                        assets.add(
                            AssetInfoImpl(
                                id = asset.localIdentifier,
                                uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                                filename = fileName,
                                date = asset.creationDate?.timeIntervalSince1970?.toLong()
                                    ?.times(1000) ?: 0L,
                                mediaType = asset.mediaType.convertMediaType,
                                mimeType = mimeType,
                                duration = (asset.duration*1000F).toLong(),
                                directory = "Photo",// iOS上没有直接的文件目录
                            )
                        )
                    }
                }
                else {
                    // 获取相册集合
                    val collections = PHAssetCollection.fetchAssetCollectionsWithType(
                        PHAssetCollectionTypeSmartAlbum,
                        PHAssetCollectionSubtypeSmartAlbumUserLibrary,
                        null
                    )
                    collections.enumerateObjectsUsingBlock { collection, _, _ ->
                        val phac = collection as PHAssetCollection
                        val collectionName = phac.localizedTitle ?: "Unknown"
                        // 根据相册集合获取资产
                        val fetchedAssets = PHAsset.fetchAssetsInAssetCollection(phac, fetchOptions)
                        fetchedAssets.enumerateObjectsUsingBlock { asset, _, _ ->
                            if (asset !is PHAsset) return@enumerateObjectsUsingBlock
                            val fileName = asset.valueForKey("filename") as? String ?: ""

                            val mimeType = when(asset.mediaType){
                                PHAssetMediaTypeImage -> "image/jpeg"
                                PHAssetMediaTypeVideo -> "video/mp4"
                                else -> "image/jpeg"
                            }// 根据需要设置MIME类型
//                            val mimeType = "image/jpeg"
                            if(mimeType=="video/mp4"){
                                println("asset.duration::fileName:$fileName   ${asset.duration}")
                            }

                            assets.add(
                                AssetInfoImpl(
                                    id = asset.localIdentifier,
                                    uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                                    filename = fileName,
                                    date = asset.creationDate?.timeIntervalSince1970?.toLong()
                                        ?.times(1000) ?: 0L,
                                    mediaType = asset.mediaType.convertMediaType,
                                    mimeType = mimeType,
                                    duration = (asset.duration*1000F).toLong(),
                                    directory = collectionName, // iOS上没有直接的文件目录
                                )
                            )
                        }
                    }
                }
                completer.complete(assets)
            }
            completer.await()
            return assets
        }
    }

    internal class AssetInfoImpl(
        id: String,
        uriString: String,
        filename: String,
        directory: String,
        mediaType: Int,
        mimeType: String,
        duration: Long? = null,
        date: Long,
    ) : AssetInfo(
        id,
        uriString,
        "",
        filename,
        directory,
        0L,
        mediaType,
        mimeType,
        duration,
        date
    ) {
        private var _size: Long = 0
        internal var _filePath: String = ""

        override val size: Long
            get() {
                if (_size == 0L) {
                    queryJob2?.cancel()
                    queryJob2=innerScope.launch {
                        withContext(Dispatchers.IO){
                            val assets = id.toPHAsset()
                            val fileSize = assets?.let {
                                val resource = PHAssetResource.assetResourcesForAsset(it)
                                    .firstOrNull() as? PHAssetResource
                                val fileSize = (resource?.valueForKey("fileSize") as? Long) ?: 0L
                                fileSize
                            } ?: 0L
                            _size = fileSize
                        }
                    }
                }
                return _size
            }

        private val innerScope = MainScope()

        private var queryJob2:Job? = null
        private var queryJob:Job? = null
        override val filepath: String
            get(){
                if (_filePath.isEmpty()) {
                    queryJob?.cancel()
                    queryJob=innerScope.launch {
                        withContext(Dispatchers.IO){
                            val assets = id.toPHAsset()
                            val filePath = assets?.let {
                                val completer = CompletableDeferred<String>()
                                it.requestContentEditingInputWithOptions(options = PHContentEditingInputRequestOptions.new()) {
                                        contentEditingInput, info ->
                                    val imageURL = contentEditingInput?.fullSizeImageURL?.absoluteString ?: ""
                                    println("imageURL: $imageURL")
                                    completer.complete(imageURL)
                                }
                                completer.await()
                            } ?: ""
                            _filePath = filePath
                        }
                    }
                }
                return _filePath
            }

        internal fun setFilePath(filePath: String) {
            _filePath = filePath
        }

    }
}

internal fun String.toPHAsset(): PHAsset? {
    return toPHAssetFetch().firstObject() as? PHAsset
}
internal fun String.toPHAssetFetch(): PHFetchResult{
    val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(NSString.create(string = this)), null)
    return fetchResult
}