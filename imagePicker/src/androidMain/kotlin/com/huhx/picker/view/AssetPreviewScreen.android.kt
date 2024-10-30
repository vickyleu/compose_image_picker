package com.huhx.picker.view

import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import coil3.compose.LocalPlatformContext
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
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
    var isSliding by remember { mutableStateOf(false) }

    val eventListener = remember {
        object : PlayerEventListener {
            override fun onPlayerItemDidPlayToEndTime() {
                isPlaying.value = false
                isLoaded.value = false
                position.value = 0
            }

            override fun onPlayerFailedToPlay() {
                isLoaded.value = false
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
            }

            override fun onPlayerStopped() {
                isPlaying.value = false
                isLoaded.value = false
            }

            override fun onPlaying(pos: Long, dur: Long) {
                if (!isSliding) {
                    position.value = pos
                }
            }
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
    DisposableEffect(Unit) {
        player.addListener(object : Player.Listener {

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                eventListener.onPlayerBufferingCompleted()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_READY -> {
                        eventListener.onPlayerBufferingCompleted()
                    }

                    Player.STATE_ENDED -> {
                        eventListener.onPlayerItemDidPlayToEndTime()
                    }

                    Player.STATE_BUFFERING -> {
                        eventListener.onPlayerBuffering()
                    }

                    Player.STATE_IDLE -> {
                        eventListener.onPlayerStopped()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                eventListener.onPlayerFailedToPlay()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    eventListener.onPlayerPlaying()
                } else {
                    eventListener.onPlayerPaused()
                }
            }
        })
        onDispose {
            player.release()
        }
    }
    LaunchedEffect(player) {
        withContext(Dispatchers.IO) abc@{
            while (isActive) {
                withContext(Dispatchers.Main) {
                    if(player.isPlaying||player.isLoading){
                        eventListener.onPlaying(
                            player.currentPosition.coerceAtLeast(0),
                            player.duration.coerceAtLeast(0)
                        )
                    }
                }
                delay(50L)
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        factory = {
            PlayerView(it).apply {
                this.player = player
                this.useController = false
                this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.setShowBuffering(SHOW_BUFFERING_ALWAYS)
                this.setShutterBackgroundColor(Color.TRANSPARENT)
                this.setKeepContentOnPlayerReset(true)
                this.setShowPreviousButton(false)
                this.setShowNextButton(false)
                this.setShowFastForwardButton(false)
                this.setShowRewindButton(false)
                this.setShowSubtitleButton(false)
                this.setShowVrButton(false)
                this.setShowShuffleButton(false)
                this.findViewById<View>(androidx.media3.ui.R.id.exo_settings).visibility = View.GONE
            }
        })

    val callback = remember {
        object : PlayCallback {
            override fun play() {
                player.play()
            }

            override fun pause() {
                player.pause()
            }

            override fun seekTo(value: Long) {
                player.pause()
                player.seekTo(value)
                player.play()
            }

            override fun onChangeSliding(sliding: Boolean) {
                isSliding = sliding
            }
        }
    }

    return callback
}

actual fun ImageRequest.Builder.decoderFactoryPlatform(progress: (Int) -> Unit): ImageRequest.Builder {
    return if (Build.VERSION.SDK_INT >= 28) decoderFactory(AnimatedImageDecoder.Factory()) else decoderFactory(
        GifDecoder.Factory()
    )
}
