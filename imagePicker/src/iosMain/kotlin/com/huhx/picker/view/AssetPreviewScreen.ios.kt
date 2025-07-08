package com.huhx.picker.view

import ImageObserver.ImageObserverProtocol
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
import coil3.compose.LocalPlatformContext
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
import com.huhx.picker.provider.AssetLoader.AssetInfoImpl
import com.huhx.picker.provider.AssetLoader.Companion.convertMediaType
import com.huhx.picker.provider.AssetLoader.Companion.uriString
import com.huhx.picker.provider.toPHAsset
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
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import com.huhx.picker.provider.FileImplFetcherFactory
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
import platform.CoreGraphics.CGSize
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
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeFastFormat
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.Photos.PHPhotoLibrary
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
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
    val context = LocalPlatformContext.current
    DisposableEffect(Unit) {
        val job = scope.launch {
            withContext(Dispatchers.IO) {
                if (uriString.startsWith("phasset://")) {
                    val identifier = uriString.replace("phasset://", "")
                    val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(identifier), null)
                    val asset = (fetchResult.firstObject() as? PHAsset)
                    if (asset != null) {
                        val fileName = asset.valueForKey("filename") as? String ?: ""
                        val mimeType = "image/jpeg" // 根据需要设置MIME类型
                        val assetImpl = AssetInfoImpl(
                            id = asset.localIdentifier,
                            uriString = NSURL(string = "phasset://${asset.localIdentifier}").uriString,
                            filename = fileName,
                            date = asset.creationDate?.timeIntervalSince1970?.toLong()
                                ?.times(1000) ?: 0L,
                            mediaType = asset.mediaType.convertMediaType,
                            mimeType = mimeType,
                            duration = (asset.duration * 1000F).toLong(),
                            directory = "Photo",// iOS上没有直接的文件目录
                        )
                        assetImpl.checkIfVideoNotDownload(context) {
                            val playerItem = AVPlayerItem(uRL = NSURL.fileURLWithPath(path=it.path ?: ""))
                            item.value = playerItem
                        }
                    }
                }else{
                    println("videoPreview fetch uriString ${uriString}")
                    val playerItem = AVPlayerItem(uRL = NSURL.fileURLWithPath(path=uriString))
                    item.value = playerItem
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
                if(dur>0L && duration.value!=dur){
                    duration.value = dur
                }
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
private val fetcherImplFactory = FileImplFetcherFactory()

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
        .fetcherFactory(fetcherImplFactory)
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
                var lastError: Exception? = null
                
                while (attempt < maxRetries) {
                    attempt++
                    try {
                        imageData = withContext(Dispatchers.IO) {
                            getImageDataFromPHAsset(localIdentifier, options) { progressValue ->
                                progress.value = progressValue
                                
                                // 如果是从iCloud下载，更新UI进度
                                if (progressValue > 0 && progressValue < 100) {
                                    println("iCloud下载进度: $progressValue%")
                                }
                            }
                        }
                        if (imageData != null) {
                            bufferedSource.buffer.write(imageData)
                            bufferedSource.buffer.flush()
                            break
                        }
                    } catch (e: Exception) {
                        lastError = e
                        println("第${attempt}次尝试失败: ${e.message}")
                    }
                    
                    if (attempt < maxRetries) {
                        // 智能重试延迟：第一次1秒，第二次2秒
                        val delayMs = attempt * 1000L
                        println("等待${delayMs}ms后进行第${attempt + 1}次重试")
                        delay(delayMs)
                    }
                }
                
                if (imageData == null) {
                    throw Exception("Failed to get image data after $maxRetries attempts. Last error: ${lastError?.message}")
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
        
        var imageData: ByteArray? = null
        var hasReceivedHighQuality = false
        var fallbackData: ByteArray? = null
        
        val options = PHImageRequestOptions()
        options.networkAccessAllowed = true
        options.synchronous = false
        options.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
        options.resizeMode = PHImageRequestOptionsResizeModeFast
        
        options.progressHandler = { progress, error, stop, info ->
            val p = (progress * 100f).toInt().coerceIn(0, 100)
            progressCallback.invoke(p)
            
            if (error != null) {
                println("iCloud下载错误: ${error.localizedDescription}")
                // 如果有降级图片，使用它
                if (fallbackData != null && !hasReceivedHighQuality) {
                    completer.complete(fallbackData)
                } else {
                    completer.complete(null)
                }
            }
        }
        
        val size = optionsCoil.size.let {
            androidx.compose.ui.geometry.Size(
                (it.width as Dimension.Pixels).px.toFloat(),
                (it.height as Dimension.Pixels).px.toFloat()
            )
        }
        
        PHImageManager.defaultManager().requestImageForAsset(
            asset,
            targetSize = CGSizeMake(size.width.toDouble(), size.height.toDouble()),
            contentMode = PHImageContentModeAspectFill,
            options = options
        ) { image, info ->
            val isDegraded = info?.get("PHImageResultIsDegradedKey") as? Boolean ?: false
            val isInCloud = info?.get("PHImageResultIsInCloudKey") as? Boolean ?: false
            val imageBytes = image?.toByteArray()
            
            if (imageBytes != null) {
                if (isDegraded) {
                    // 这是降级图片，保存作为后备，但继续等待高质量版本
                    fallbackData = imageBytes
                    println("收到降级图片，大小: ${imageBytes.size} bytes")
                    
                    // 如果图片在iCloud中，设置超时
                    if (isInCloud) {
                        kotlinx.coroutines.MainScope().launch {
                            delay(5000) // 5秒超时
                            if (!hasReceivedHighQuality && !completer.isCompleted) {
                                println("超时，使用降级图片")
                                completer.complete(fallbackData)
                            }
                        }
                    }
                } else {
                    // 这是高质量图片
                    hasReceivedHighQuality = true
                    imageData = imageBytes
                    println("收到高质量图片，大小: ${imageBytes.size} bytes")
                    completer.complete(imageData)
                }
            } else if (!isDegraded) {
                // 没有图片且不是降级版本，说明失败了
                completer.complete(fallbackData) // 尝试使用降级版本
            }
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
    
    // 添加保存状态管理
    private val _isSaving = mutableStateOf(false)
    val isSaving: Boolean get() = _isSaving.value
    
    private val _savingProgress = mutableStateOf(0)
    val savingProgress: Int get() = _savingProgress.value

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
        // 开始保存，更新状态
        _isSaving.value = true
        _savingProgress.value = 0
        
        scope.launch {
            withContext(Dispatchers.Main) {
                _savingProgress.value = 10
            }
        }
        
        // 使用更精确的保存方法
        saveImageWithPreciseTracking(image) { success, assetIdentifier ->
            scope.launch {
                withContext(Dispatchers.Main) {
                    if (success && assetIdentifier != null) {
                        _savingProgress.value = 100
                        delay(200)
                        _isSaving.value = false
                        _savingProgress.value = 0
                        
                        // 直接使用精确的asset identifier创建AssetInfo
                        createAssetInfoFromIdentifier(assetIdentifier) { info ->
                            onResult?.invoke(info)
                        }
                    } else {
                        _isSaving.value = false
                        _savingProgress.value = 0
                        onResult?.invoke(null)
                    }
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
        asset?.let { phAsset ->
            val fileName = phAsset.valueForKey("filename") as? String ?: ""
            val mimeType = "image/jpeg" // 根据需要设置MIME类型
            val info = AssetLoader.AssetInfoImpl(
                id = phAsset.localIdentifier,
                uriString = NSURL(string = "phasset://${phAsset.localIdentifier}").uriString,
                filename = fileName,
                date = phAsset.creationDate?.timeIntervalSince1970?.toLong()
                    ?.times(1000) ?: 0L,
                mediaType = phAsset.mediaType.toInt(),
                mimeType = mimeType,
                duration = phAsset.duration.toLong(),
                directory = "Photo" // iOS上没有直接的文件目录
            )
            
            // 预先检查和下载图片以确保可用性
            preloadImageFromAsset(phAsset) { success ->
                if (success) {
                    onComplete(info)
                } else {
                    // 如果预加载失败，仍然返回AssetInfo，但日志记录问题
                    println("警告: 图片可能在iCloud中，Coil加载时可能需要网络下载")
                    onComplete(info)
                }
            }
        } ?: run {
            onComplete(null)
        }
    }
    
    // 精确保存图片并获取asset identifier
    @OptIn(ExperimentalForeignApi::class)
    private fun saveImageWithPreciseTracking(image: UIImage, onComplete: (Boolean, String?) -> Unit) {
        scope.launch {
            withContext(Dispatchers.Main) {
                _savingProgress.value = 30
            }
        }
        
        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            // 创建图片asset请求
            val creationRequest = PHAssetChangeRequest.creationRequestForAssetFromImage(image)
            creationRequest?.placeholderForCreatedAsset?.localIdentifier
        }) { success, error ->
            scope.launch {
                withContext(Dispatchers.Main) {
                    _savingProgress.value = 80
                }
            }
            
            if (success) {
                // 保存成功，获取最新创建的asset
                fetchMostRecentAsset { identifier ->
                    onComplete(true, identifier)
                }
            } else {
                println("保存图片失败: ${error?.localizedDescription}")
                onComplete(false, null)
            }
        }
    }
    
    // 获取最新创建的asset (作为备选方案)
    private fun fetchMostRecentAsset(onComplete: (String?) -> Unit) {
        val fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = listOf(NSSortDescriptor(key = "creationDate", ascending = false))
        fetchOptions.fetchLimit = 1u
        
        val fetchResult = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options = fetchOptions)
        val asset = fetchResult.firstObject as? PHAsset
        onComplete(asset?.localIdentifier)
    }
    
    // 从identifier创建AssetInfo
    private fun createAssetInfoFromIdentifier(identifier: String, onComplete: (AssetInfo?) -> Unit) {
        val asset = identifier.toPHAsset()
        asset?.let { phAsset ->
            val fileName = phAsset.valueForKey("filename") as? String ?: ""
            val mimeType = "image/jpeg"
            val info = AssetLoader.AssetInfoImpl(
                id = phAsset.localIdentifier,
                uriString = NSURL(string = "phasset://${phAsset.localIdentifier}").uriString,
                filename = fileName,
                date = phAsset.creationDate?.timeIntervalSince1970?.toLong()?.times(1000) ?: 0L,
                mediaType = phAsset.mediaType.toInt(),
                mimeType = mimeType,
                duration = phAsset.duration.toLong(),
                directory = "Photo"
            )
            
            // 智能预载 - 预载多个尺寸
            intelligentPreload(phAsset) { 
                onComplete(info)
            }
        } ?: run {
            onComplete(null)
        }
    }

    // 智能预载 - 预载多个尺寸以优化用户体验
    @OptIn(ExperimentalForeignApi::class)
    private fun intelligentPreload(asset: PHAsset, onComplete: () -> Unit) {
        val sizes = listOf(
            CGSizeMake(200.0, 200.0),   // 缩略图
            CGSizeMake(800.0, 600.0),   // 中等尺寸
            CGSizeMake(1200.0, 1200.0)  // 高质量预览
        )
        
        var completedCount = 0
        val totalCount = sizes.size
        
        sizes.forEach { size ->
            preloadSpecificSize(asset, size) { success ->
                completedCount++
                if (completedCount == 1) {
                    // 第一个(缩略图)完成就可以继续，其他在后台继续
                    onComplete()
                }
            }
        }
    }
    
    // 预载特定尺寸
    @OptIn(ExperimentalForeignApi::class)
    private fun preloadSpecificSize(asset: PHAsset, targetSize: CValue<CGSize>, onComplete: (Boolean) -> Unit) {
        val options = PHImageRequestOptions().apply {
            networkAccessAllowed = true
            synchronous = false
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            resizeMode = PHImageRequestOptionsResizeModeFast
            
            progressHandler = { progress, error, stop, info ->
                if (error != null && progress == 0.0) {
                    println("预载失败: ${error.localizedDescription}")
                    onComplete(false)
                }
            }
        }
        
        PHImageManager.defaultManager().requestImageForAsset(
            asset,
            targetSize = targetSize,
            contentMode = PHImageContentModeAspectFill,
            options = options
        ) { image, info ->
            val success = image != null
            onComplete(success)
            
            // 监控iCloud状态
            val isInCloud = info?.get("PHImageResultIsInCloudKey") as? Boolean ?: false
            val isDegraded = info?.get("PHImageResultIsDegradedKey") as? Boolean ?: false
            
            if (isInCloud) {
                println("图片位于iCloud中，尺寸${targetSize}已触发下载")
            }
        }
    }

    // 预加载图片以确保可用性 (保留原方法作为备用)
    @OptIn(ExperimentalForeignApi::class)
    private fun preloadImageFromAsset(asset: PHAsset, onComplete: (Boolean) -> Unit) {
        preloadSpecificSize(asset, CGSizeMake(200.0, 200.0)) { success ->
            onComplete(success)
        }
    }
    
    // 检查网络连接状态
    private fun isNetworkAvailable(): Boolean {
        return try {
            // 简单的网络可达性检查
            // 实际应用中可以实现更精确的检查
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // 智能下载策略：根据网络状态调整下载行为
    private fun getOptimalImageRequestOptions(isNetworkPreferred: Boolean = true): PHImageRequestOptions {
        return PHImageRequestOptions().apply {
            networkAccessAllowed = isNetworkPreferred && isNetworkAvailable()
            synchronous = false
            deliveryMode = if (networkAccessAllowed) {
                PHImageRequestOptionsDeliveryModeHighQualityFormat
            } else {
                PHImageRequestOptionsDeliveryModeFastFormat // 如果没有网络，使用快速格式
            }
            resizeMode = PHImageRequestOptionsResizeModeFast
        }
    }

}
