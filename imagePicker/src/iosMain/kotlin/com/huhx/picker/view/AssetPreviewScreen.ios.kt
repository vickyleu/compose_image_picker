package com.huhx.picker.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import coil3.DrawableImage
import coil3.ImageLoader
import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.provider.AssetLoader.Companion.uriString
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun VideoPreview(
    modifier: Modifier,
    uriString: String,
    loading: @Composable (() -> Unit)?
) {
}

@OptIn(ExperimentalCoilApi::class)
class ProgressDrawable : DrawableImage() {
    private val paint = Paint().apply {
        isAntiAlias = true
    }
    var progress: Float = 0f
        set(value) {
            field = value
        }
    override val height: Int
        get() = TODO("Not yet implemented")
    override val shareable: Boolean
        get() = TODO("Not yet implemented")
    override val size: Long
        get() = TODO("Not yet implemented")
    override val width: Int
        get() = TODO("Not yet implemented")

    override fun Canvas.onDraw() {
        TODO("Not yet implemented")
    }


}

private val fetcherFactory = PHAssetFetcherFactory()

@OptIn(ExperimentalCoilApi::class)
actual fun ImageRequest.Builder.decoderFactoryPlatform(progress:(Int)->Unit): ImageRequest.Builder {

    this.listener(onSuccess = { imageRequest, result ->
        progress.invoke(100)
        imageRequest.target?.onSuccess(result.image)
    }, onError = { imageRequest, result ->
        progress.invoke(0)
    })
    return this
        .fetcherFactory(fetcherFactory)
        .placeholder {
            (it.fetcherFactory?.first as? PHAssetFetcherFactory)?.let {
//                val p = it.progress.value
            }
            null
        }
}

class PHAssetFetcher(
    private val uri: Uri,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {
    internal val progress = mutableStateOf(0)
    override suspend fun fetch(): FetchResult {
        val uriStr = uri.toString()
        val localIdentifier = uriStr.substring("phasset://".length)
        val bufferedSource: BufferedSource = Buffer()
        withContext(Dispatchers.IO) {
            var attempt = 0
            val maxRetries = 3
            var imageData: ByteArray? = null
            while (attempt < maxRetries) {
                attempt++
                imageData = withContext(Dispatchers.IO) {
                    getImageDataFromPHAsset(localIdentifier, options) {
                        progress.value = it
                    }
                }
                if (imageData != null) {
                    bufferedSource.buffer.write(imageData)
                    bufferedSource.buffer.flush()
                    break
                }
                if (attempt < maxRetries) {
                    delay(1000L)  // 等待一秒后重试
                }
            }
            if (imageData == null) {
                throw Exception("Failed to get image data after $maxRetries attempts")
            }
        }
        return SourceFetchResult(
            source = ImageSource(
                source = bufferedSource,
                fileSystem = FileSystem.SYSTEM,
                metadata = null,
            ),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK,
        )

    }

}

@OptIn(ExperimentalForeignApi::class)
internal suspend fun getImageDataFromPHAsset(
    localIdentifier: String?, optionsCoil: Options,progressCallback: (Int)->Unit
): ByteArray? {
    val completer = CompletableDeferred<ByteArray?>()
    withContext(Dispatchers.IO) {
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), null)
        val asset = fetchResult.firstObject() as? PHAsset ?: return@withContext run {
            completer.complete(null)
        }
        var imageData: ByteArray?
        val options = PHImageRequestOptions()
        options.networkAccessAllowed = true
        options.synchronous = false
        options.progressHandler = { progress, error, stop, info ->
            val p = (progress * 100f).toInt().coerceIn(0, 100)
            progressCallback.invoke(p)
            println("下载进度: ${p}")
            if (error != null) {
                println("下载进度: ${p} error:${error}")
                completer.complete(null)
            } else if (p == 100) {
            }
        }
        PHImageManager.defaultManager()
            .requestImageDataForAsset(asset, options) { data, _, _, _ ->
                imageData = data?.toByteArray()
                completer.complete(imageData)
            }
    }
    return completer.await()
}
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(length.toInt())
    byteArray.usePinned {
        memcpy(it.addressOf(0), bytes, length)
    }
    return byteArray
}
class PHAssetFetcherFactory : Fetcher.Factory<Uri> {

    override fun create(
        data: Uri,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher? {
        // 判断是否是我们需要处理的URI
        if (data.scheme == "phasset") {
            return PHAssetFetcher(data, options, imageLoader)
        }
        println("PHAssetFetcherFactory create are you sure?")
        return null
    }
}

class IOSCameraController(
    private val scope: CoroutineScope,
    private val viewController: UIViewController
) : NSObject(),
    UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    private var onResult: ((AssetInfo?) -> Unit)? = null

    fun startCamera(onResult: (AssetInfo?) -> Unit) {
        this.onResult = {
            onResult(it)
            this.onResult = null
        }
        scope.launch {
            withContext(Dispatchers.Main) {
                val imagePickerController = UIImagePickerController().apply {
                    sourceType =
                        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    delegate = this@IOSCameraController
                }
                viewController.presentViewController(
                    imagePickerController,
                    animated = true,
                    completion = null
                )
            }
        }
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val image =
                        didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                    image?.let {
                        saveImageToAlbum(it)
                    } ?: onResult?.invoke(null)
                }
            }
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult?.invoke(null)
    }


    @OptIn(ExperimentalForeignApi::class)
    private fun saveImageToAlbum(image: UIImage) {
        UIImageWriteToSavedPhotosAlbum(image, null,null, null)
        scope.launch {
            withContext(Dispatchers.IO) {
                delay(300)
                // 获取保存后的照片URL
                fetchLastImageUri { info ->
                    onResult?.invoke(info)
                }
            }
        }
       /*
       TODO 目前的版本不支持超过三个参数的方法调用
       UIImageWriteToSavedPhotosAlbum(
            image, this,
            NSSelectorFromString("image:didFinishSavingWithError:contextInfo:"), null
        )*/
    }

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
//    @ObjCAction  TODO 目前的版本不支持超过三个参数的方法调用
    @Suppress("UNUSED_PARAMETER", "unused")
    fun image(image: UIImage, didFinishSavingWithError: NSError?, contextInfo: Any?) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (didFinishSavingWithError != null) {
                    onResult?.invoke(null)
                } else {
                    // 获取保存后的照片URL
                    fetchLastImageUri { uri ->
                        onResult?.invoke(uri)
                    }
                }
            }
        }
    }

    private fun fetchLastImageUri(onComplete: (AssetInfo?) -> Unit) {
        val fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors =
            listOf(NSSortDescriptor(key = "creationDate", ascending = false))
        fetchOptions.fetchLimit = 1u
        val fetchResult =
            PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options = fetchOptions)

        val asset = fetchResult.firstObject as? PHAsset
        asset?.let {
            val fileName = asset.valueForKey("filename") as? String ?: ""
            val mimeType = "image/jpeg" // 根据需要设置MIME类型
            val info = AssetLoader.AssetInfoImpl(
                    id = asset.localIdentifier,
                    uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                    filename = fileName,
                    date = asset.creationDate?.timeIntervalSince1970?.toLong()
                        ?.times(1000) ?: 0L,
                    mediaType = asset.mediaType.toInt(),
                    mimeType = mimeType,
                    duration = asset.duration.toLong(),
                    directory = "Photo" // iOS上没有直接的文件目录
                )
            onComplete(info)
//            val options = PHContentEditingInputRequestOptions()
//            it.requestContentEditingInputWithOptions(options) { input, _ ->
//                val uri = input?.fullSizeImageURL?.absoluteString?.toUri()
//
//            }
        } ?: run {
            onComplete(null)
        }
    }

}