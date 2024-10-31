package com.huhx.picker.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import coil3.ImageLoader
import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.pxOrElse
import coil3.toBitmap
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.provider.AssetLoader.Companion.uriString
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ImageObserver.ImageObserverProtocol
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSURL
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHCachingImageManager
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun videoPreview(
    modifier: Modifier,
    uriString: String,
    isPlaying: MutableState<Boolean>,
    isLoaded: MutableState<Boolean>,
    position: MutableState<Long>,
    duration: MutableState<Long>,
    isCurrentPage: Boolean,
): PlayCallback {
    val player = remember {
        AVPlayer()
    }
    val item = remember {
        mutableStateOf<AVPlayerItem?>(null)
    }
    val playerLayer by remember { mutableStateOf(AVPlayerLayer()) }
    val avPlayerViewController = remember {
        AVPlayerViewController().apply {
            this.player = player
            this.showsPlaybackControls = true
            this.videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val job = scope.launch {
            withContext(Dispatchers.IO) {
                val identifier = uriString.replace("phasset://", "")
                val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(identifier), null)
                val asset = (fetchResult.firstObject() as? PHAsset)
                if (asset != null) {
                    PHCachingImageManager.defaultManager().requestAVAssetForVideo(
                        asset,
                        options = null,
                        resultHandler = { avAsset, _, _ ->
                            val avUrlAsset = avAsset as AVURLAsset
                            val playerItem = AVPlayerItem(uRL = avUrlAsset.URL)
                            item.value = playerItem
                        })
                }
            }
        }
        onDispose {
            job.cancel()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { item.value }
            .filterNotNull()
            .distinctUntilChanged()
            .collect {
                player.replaceCurrentItemWithPlayerItem(it)
            }
    }
    val playerContainer = remember {
        UIView().apply {
            layer.addSublayer(playerLayer)
        }
    }

    DisposableEffect(isCurrentPage) {
        if (!isCurrentPage) {
            player.pause()
        }
        onDispose {
            player.pause()
        }
    }

    var isSliding by remember { mutableStateOf(false) }

    DisposableEffect(item.value) {
        if (item.value == null) return@DisposableEffect onDispose { }
        val observer = Observer(object : PlayerEventListener {
            override fun onPlayerItemDidPlayToEndTime() {
                isPlaying.value = false
//
//                player.replaceCurrentItemWithPlayerItem(item.value)
                player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
                position.value = 0
            }

            override fun onPlayerFailedToPlay() {
                isPlaying.value = false
            }

            override fun onPlayerBuffering() {
                isLoaded.value = true
            }

            override fun onPlayerBufferingCompleted() {
                isLoaded.value = false

            }

            override fun onPlayerPlaying() {
                isPlaying.value = true
            }

            override fun onPlayerPaused() {
                isPlaying.value = false
                isLoaded.value = false
            }

            override fun onPlayerStopped() {
                isPlaying.value = false
                isLoaded.value = false
            }

            override fun onPlaying(pos: Long, dur: Long) {
                if (isSliding) return
                position.value = pos
            }
        })
        observer.addObserver(player)
        onDispose {
            observer.removeObserver(player)
        }
    }

    val frame = remember { mutableStateOf(Size.Zero) }
    LaunchedEffect(Unit) {
        snapshotFlow { frame.value }
            .distinctUntilChanged()
            .collect {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        CATransaction.begin()
                        CATransaction.setValue(true, kCATransactionDisableActions)
                        val rect = CGRectMake(0.0, 0.0, it.width.toDouble(), it.height.toDouble())
                        playerLayer.setFrame(rect)
                        playerContainer.setFrame(rect)
                        avPlayerViewController.view.layer.frame = rect
                        CATransaction.commit()
                    }
                }
            }
    }
    with(LocalDensity.current) {
        BoxWithConstraints(
            modifier = modifier.then(Modifier.onGloballyPositioned {
                frame.value = it.size.toSize().let {
                    Size(it.width.toDp().value, it.height.toDp().value)
                }
            })
        ) {
            UIKitView(
                factory = {
                    playerContainer.addSubview(avPlayerViewController.view)
                    playerContainer.setFrame(
                        CGRectMake(
                            0.0,
                            0.0,
                            frame.value.width.toDouble(),
                            frame.value.height.toDouble()
                        )
                    )
                    // 禁用 autoresizing mask
                    avPlayerViewController.view.translatesAutoresizingMaskIntoConstraints = false
                    // 设置 Auto Layout 约束
                    NSLayoutConstraint.activateConstraints(
                        listOf(
                            avPlayerViewController.view.leadingAnchor.constraintEqualToAnchor(
                                playerContainer.leadingAnchor
                            ),
                            avPlayerViewController.view.trailingAnchor.constraintEqualToAnchor(
                                playerContainer.trailingAnchor
                            ),
                            avPlayerViewController.view.topAnchor.constraintEqualToAnchor(
                                playerContainer.topAnchor
                            ),
                            avPlayerViewController.view.bottomAnchor.constraintEqualToAnchor(
                                playerContainer.bottomAnchor
                            )
                        )
                    )
                    playerContainer
                },
                modifier = modifier.then(Modifier.onGloballyPositioned {
                    /*val rect = it.boundsInParent().let {
                        CGRectMake(
                            it.topLeft.x.toDp().value.toDouble(),
                            it.topLeft.y.toDp().value.toDouble(),
                            it.width.toDp().value.toDouble(),
                            it.height.toDp().value.toDouble()
                        )
                    }
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            CATransaction.begin()
                            CATransaction.setValue(true, kCATransactionDisableActions)
                            playerContainer.layer.setFrame(rect)
                            playerLayer.setFrame(rect)
                            playerContainer.setFrame(rect)
                            avPlayerViewController.view.layer.frame = rect
                            CATransaction.commit()
                        }
                    }*/
                }),
                update = { view ->
                },
                properties = UIKitInteropProperties(
                    isInteractive = false,
                    isNativeAccessibilityEnabled = true
                )
            )
        }
    }

    val callback = remember {
        object : PlayCallback {
            override fun play() {
                if (item.value != null) {
                    player.playImmediatelyAtRate(1.0f)
                }
            }

            override fun pause() {
                player.pause()
            }

            override fun seekTo(value: Long) {
                val time = CMTimeMake(value = value, timescale = 1000)
                player.seekToTime(time)
            }

            override fun onChangeSliding(sliding: Boolean) {
                isSliding = sliding
            }
        }
    }

    return callback
}


internal class Observer(private val eventListener: PlayerEventListener) : NSObject(),
    ImageObserverProtocol {

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?
    ) {
        when (keyPath) {
            "timeControlStatus" -> {
                val status = (ofObject as? AVPlayer)?.timeControlStatus
                when (status) {
                    AVPlayerTimeControlStatusPaused -> eventListener.onPlayerPaused()
                    AVPlayerTimeControlStatusPlaying -> eventListener.onPlayerPlaying()
                    AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> eventListener.onPlayerBufferingCompleted()
                    else -> Unit
                }
            }

            "status" -> {
                val itemStatus = (ofObject as? AVPlayerItem)?.status
                when (itemStatus) {
                    AVPlayerItemStatusFailed -> eventListener.onPlayerFailedToPlay()
                    else -> Unit
                }
            }

            "playbackBufferEmpty" -> eventListener.onPlayerBuffering()
            "playbackLikelyToKeepUp", "playbackBufferFull" -> eventListener.onPlayerBufferingCompleted()
            else -> Unit
        }
    }

    private var isRegisterEvent = false

    fun addObserver(player: AVPlayer) {
        if (isRegisterEvent) return

        player.addObserver(this, "timeControlStatus", NSKeyValueObservingOptionNew, null)
        player.currentItem?.apply {
            addObserver(this@Observer, "status", NSKeyValueObservingOptionNew, null) // 添加status监听
            addObserver(this@Observer, "playbackBufferEmpty", NSKeyValueObservingOptionNew, null)
            addObserver(this@Observer, "playbackLikelyToKeepUp", NSKeyValueObservingOptionNew, null)
            addObserver(this@Observer, "playbackBufferFull", NSKeyValueObservingOptionNew, null)

            NSNotificationCenter.defaultCenter().addObserver(
                this@Observer,
                NSSelectorFromString(this@Observer::onPlayerItemDidPlayToEndTime.name),
                AVPlayerItemDidPlayToEndTimeNotification,
                this
            )

            // 播放失败通知监听
            NSNotificationCenter.defaultCenter().addObserver(
                this@Observer,
                NSSelectorFromString(this@Observer::onPlayerFailedToPlay.name),
                AVPlayerItemFailedToPlayToEndTimeNotification,
                this
            )
            addTimer(player)
        }

        isRegisterEvent = true
    }

    private var timeObserver: Any? = null
    fun addTimer(player: AVPlayer) {
        if (timeObserver != null) return
        timeObserver = player.addPeriodicTimeObserverForInterval(
            CMTimeMake(value = 50, timescale = 1000),//300毫秒回调一次
            dispatch_get_main_queue()
        ) { time ->
            if (player.status == AVPlayerItemStatusFailed) {
                eventListener.onPlayerFailedToPlay()
                return@addPeriodicTimeObserverForInterval
            }
            eventListener.onPlaying(
                CMTimeGetMilliseconds(player.currentTime()).coerceAtLeast(0.0).toLong(),
                (player.currentItem?.duration?.let {
                    CMTimeGetMilliseconds(it)
                } ?: 0.0).coerceAtLeast(0.0).toLong()
            )
        }
    }

    @Suppress("FunctionName")
    private fun CMTimeGetMilliseconds(time: CValue<CMTime>): Double {
        time.useContents {
            return (value.toDouble() * 1000.0) / timescale.toDouble()
        }
    }

    fun removeTimer(player: AVPlayer) {
        timeObserver?.let {
            player.removeTimeObserver(it)
            timeObserver = null
        }
    }

    @Suppress("unused")
    @ObjCAction
    fun onPlayerItemDidPlayToEndTime() {
        eventListener.onPlayerItemDidPlayToEndTime()
    }

    @Suppress("unused")
    @ObjCAction
    fun onPlayerFailedToPlay() {
        eventListener.onPlayerFailedToPlay()
    }

    fun stopPlayer(player: AVPlayer) {
        // 手动停止播放时调用
        player.pause()
        eventListener.onPlayerStopped()
    }

    fun removeObserver(player: AVPlayer) {
        removeTimer(player)
        if (isRegisterEvent) {
            player.removeObserver(this, "timeControlStatus")
            player.currentItem?.apply {
                removeObserver(this@Observer, "status")
                removeObserver(this@Observer, "playbackBufferEmpty")
                removeObserver(this@Observer, "playbackLikelyToKeepUp")
                removeObserver(this@Observer, "playbackBufferFull")
                NSNotificationCenter.defaultCenter().removeObserver(
                    this@Observer,
                    AVPlayerItemDidPlayToEndTimeNotification,
                    this
                )
                NSNotificationCenter.defaultCenter().removeObserver(
                    this@Observer,
                    AVPlayerItemFailedToPlayToEndTimeNotification,
                    this
                )
            }
            isRegisterEvent = false
        }
    }
}


private val fetcherFactory = PHAssetFetcherFactory()

@OptIn(ExperimentalCoilApi::class)
actual fun ImageRequest.Builder.decoderFactoryPlatform(progress: (Int) -> Unit): ImageRequest.Builder {

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

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun fetch(): FetchResult {
        val uriStr = uri.toString()
        val localIdentifier = uriStr.substring("phasset://".length)
        return imageLoader.memoryCache?.let {
            val memoryCache = it
            val key = memoryCache.keys.filter { it.key == uriStr }
            key.mapNotNull {
                val cache = memoryCache[it] ?: return@mapNotNull null
                val image = cache.image
                if (image.width == options.size.width.pxOrElse { 0 } && image.height == options.size.height.pxOrElse { 0 }) {
                    val bitmap = image.toBitmap()
                    if (bitmap.isNull.not() && bitmap.rowBytes > 0) {
                        return@mapNotNull ImageFetchResult(
                            image = image,
                            isSampled = false,
                            dataSource = DataSource.MEMORY,
                        )
                    } else return@mapNotNull null
                } else return@mapNotNull null
            }.firstOrNull()
        } ?: run {
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
            SourceFetchResult(
                source = ImageSource(
                    source = bufferedSource,
                    fileSystem = FileSystem.SYSTEM,
                    metadata = null,
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.MEMORY,
            )
        }

    }
}

@OptIn(ExperimentalForeignApi::class)
internal suspend fun getImageDataFromPHAsset(
    localIdentifier: String?, optionsCoil: Options,
    cancelToken: Any? = null,
    progressCallback: (Int) -> Unit
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
        options.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
        options.resizeMode = PHImageRequestOptionsResizeModeFast
        options.progressHandler = { progress, error, stop, info ->
            val p = (progress * 100f).toInt().coerceIn(0, 100)
            progressCallback.invoke(p)
            if (error != null) {
                completer.complete(null)
            } else if (p == 100) {
            }
        }
        val size = optionsCoil.size.let {
            androidx.compose.ui.geometry.Size(
                (it.width as Dimension.Pixels).px.toFloat(),
                (it.height as Dimension.Pixels).px.toFloat()
            )
        }
        PHImageManager.defaultManager().requestImageForAsset(asset,
            targetSize = CGSizeMake(size.width.toDouble(), size.height.toDouble()),
            contentMode = PHImageContentModeAspectFill,
            options = options,
            resultHandler = { image, _ ->
                imageData = image?.toByteArray()
                completer.complete(imageData)
            }
        )
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


@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toByteArray(): ByteArray {
    val nsData = UIImageJPEGRepresentation(this, 1.0)
    return nsData?.toByteArray() ?: ByteArray(0)
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
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
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
