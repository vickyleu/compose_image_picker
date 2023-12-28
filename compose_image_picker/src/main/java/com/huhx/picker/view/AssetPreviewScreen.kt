package com.huhx.picker.view

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.huhx.picker.R
import com.huhx.picker.model.AssetInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AssetPreviewScreen(
    index: Int,
    assets: List<AssetInfo>,
    navigateUp: () -> Unit,
    selectedList: SnapshotStateList<AssetInfo>,
) {
    val pageState = rememberPagerState(initialPage = index, pageCount = assets::size)

    Scaffold(
        topBar = { PreviewTopAppBar(navigateUp = navigateUp) },
        bottomBar = {
            SelectorBottomBar(selectedList = selectedList, assetInfo = assets[pageState.currentPage]) {
                navigateUp()
                if (selectedList.isEmpty()) selectedList.add(it)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .background(Color.Black)
        ) {
            AssetPreview(assets = assets, pagerState = pageState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewTopAppBar(navigateUp: () -> Unit) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {},
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black),
        navigationIcon = {
            IconButton(onClick = navigateUp) {
                Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "")
            }
        }
    )
}

@Composable
private fun SelectorBottomBar(
    assetInfo: AssetInfo,
    selectedList: SnapshotStateList<AssetInfo>,
    onClick: (AssetInfo) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Black.copy(alpha = 0.9F))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImageIndicator(
                assetInfo = assetInfo,
                size = 20.dp,
                fontSize = 14.sp,
                selected = selectedList.any { it == assetInfo },
                assetSelected = selectedList,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.text_asset_select), color = Color.White, fontSize = 14.sp)
        }
        Button(
            modifier = Modifier.defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            shape = RoundedCornerShape(5.dp),
            enabled = true,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            onClick = { onClick(assetInfo) }
        ) {
            Text(stringResource(R.string.text_done), color = Color.White, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetPreview(assets: List<AssetInfo>, pagerState: PagerState) {
    Box {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val assetInfo = assets[page]
            if (assetInfo.isImage()) {
                ImagePreview(uriString = assetInfo.uriString)
            } else {
                VideoPreview(uriString = assetInfo.uriString)
            }
        }
    }
}

@Composable
fun ImagePreview(uriString: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uriString)
            .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
            .build(),
        modifier = Modifier.fillMaxSize(),
        filterQuality = FilterQuality.None,
        contentScale = ContentScale.Fit,
        contentDescription = ""
    )
}

@Composable
fun VideoPreview(uriString: String) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)
            val source = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uriString))
            setMediaSource(source)

            prepare()
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                StyledPlayerView(context).apply {
                    player = exoPlayer
                }
            }
        )
    ) {
        onDispose { exoPlayer.release() }
    }
}
