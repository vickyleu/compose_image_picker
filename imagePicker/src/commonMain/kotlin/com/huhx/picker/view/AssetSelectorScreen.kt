package com.huhx.picker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.huhx.picker.base.BasicScreen
import com.huhx.picker.model.AssetDirectory
import com.huhx.picker.model.AssetInfo.Camera.directory
import com.huhx.picker.model.page.AssetSelectorViewModel
import compose_image_picker.imagepicker.generated.resources.Res
import compose_image_picker.imagepicker.generated.resources.icon_back
import org.jetbrains.compose.resources.painterResource

internal class AssetSelectorScreen(
    directory: String,
    assetDirectories: List<AssetDirectory>,
    onSelected: (String) -> Unit
) : BasicScreen<AssetSelectorViewModel>(create = {
    AssetSelectorViewModel(
        directory,
        assetDirectories,
        onSelected
    )
}) {
    @Composable
    override fun modelContent(
        model: AssetSelectorViewModel,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        tabbarHeight: Dp
    ) {
        LazyColumn(modifier = Modifier) {
            items(items = model.assetDirectories) {
                val itemDirectory = it.directory
                ListItem(
                    modifier = Modifier.clickable { 
                        model.onSelected(itemDirectory.second)
                        onNavigateUp?.invoke()
                    },
                    leadingContent = {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(it.cover ?: Icons.Default.Place)
                                .decoderFactoryPlatform {}
                                .build(),
                            modifier = Modifier
                                .size(32.dp)
                                .aspectRatio(1.0F),
                            filterQuality = FilterQuality.Low,
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Row {
                            Text(
                                text = itemDirectory.first,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(text = "(${it.counts})", color = Color.Gray)
                        }
                    },
                    trailingContent = {
                        if (directory == itemDirectory.second) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "",
                                tint = Color.Blue
                            )
                        }
                    }
                )
            }
        }
    }

    @Composable
    override fun modelTopBar(
        model: AssetSelectorViewModel,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        topAppBarHeightAssign: MutableState<Dp>
    ) {
        with(LocalDensity.current) {
            var leftWidth by remember { mutableStateOf(0.dp) }
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color.Black)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .onGloballyPositioned {
                                val width = it.size.width.toDp()
                                leftWidth = width
                            }
                            .padding(8.dp)
                            .clickable {
                                onNavigateUp?.invoke()
                            }.padding(horizontal = 3.dp, vertical = 3.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.icon_back),
                            contentDescription = "",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillHeight,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = {
                                onNavigateUp?.invoke()
                            })
                    ) {
                        Text(text = directory, fontSize = 18.sp)
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "")
                    }
                    Spacer(modifier = Modifier.width(leftWidth))
                }
            }
        }
    }
}


