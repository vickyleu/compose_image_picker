package com.huhx.picker.view

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.LocalPlatformContext
import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPreview(
    modifier: Modifier,
    uriString: String,
    loading: @Composable (() -> Unit)?
) {
    val context = LocalPlatformContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)
            val source = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.fromUri(uriString)
            )
            setMediaSource(source)

            prepare()
        }
    }

    if (loading != null && player.isLoading) {
        loading()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        factory = {
            PlayerView(it).apply {
                this.player = player
                setShowPreviousButton(false)
                setShowNextButton(false)
                setShowFastForwardButton(false)
                setShowRewindButton(false)
                setShowSubtitleButton(false)
            }
        })
}
actual fun ImageRequest. Builder.decoderFactoryPlatform(): ImageRequest.Builder{
    return if (Build.VERSION.SDK_INT >= 28) decoderFactory(AnimatedImageDecoder.Factory()) else decoderFactory(GifDecoder.Factory())
}