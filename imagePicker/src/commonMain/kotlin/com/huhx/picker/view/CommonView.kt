package com.huhx.picker.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.AssetPickerConfig
import compose_image_picker.imagepicker.generated.resources.Res
//import compose_image_picker.imagepicker.generated.resources.message_selected_exceed
import compose_image_picker.imagepicker.generated.resources.text_select_button
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

internal val LocalAssetConfig = compositionLocalOf { AssetPickerConfig() }

@Composable
internal fun AppBarButton(size: Int, onPicked: () -> Unit) {
    val maxAssets = LocalAssetConfig.current.maxAssets
    val buttonText = stringResource(Res.string.text_select_button, size, maxAssets)
    Button(
        modifier = Modifier.defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
        shape = RoundedCornerShape(5.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        onClick = onPicked,
    ) {
        Text(buttonText, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
fun AssetImageIndicator(
    assetInfo: AssetInfo,
    selected: Boolean,
    fontSize: TextUnit = 14.sp,
    assetSelected: SnapshotStateList<AssetInfo>,
    onClicks: ((Boolean) -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val maxAssets = LocalAssetConfig.current.maxAssets
    val errorMessage = "你最多只能选择${maxAssets}个图片"
//    val errorMessage = stringResource(Res.string.message_selected_exceed, maxAssets)

    val (border, color) = if (selected) {
        Pair(null, Color(64, 151, 246))
    } else {
        Pair(BorderStroke(width = 1.dp, color = Color.White), Color.Black.copy(alpha = 0.3F))
    }

    with(LocalDensity.current){
        BoxWithConstraints ( modifier = Modifier
            .padding(6.dp)
            .wrapContentHeight()
        ){
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .then(if (border == null) Modifier else Modifier.border(border, shape = CircleShape))
                    .background(color = color, shape = CircleShape)
                    .clickable {
                        val isSelected = !selected
                        if (onClicks != null) {
                            onClicks(isSelected)
                            return@clickable
                        }
                        if (assetSelected.size == maxAssets && isSelected) {
                            showToast(context, errorMessage)

                            return@clickable
                        }
                        if (isSelected) assetSelected.add(assetInfo) else assetSelected.remove(assetInfo)
                    }
                    .clip(CircleShape)
                ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    textAlign = TextAlign.Center,
                    text = if (selected)"${assetSelected.indexOf(assetInfo) + 1}" else "",
                    color = Color.White,
                    fontSize = fontSize,
                    lineHeight = (fontSize.roundToPx()*1.5f).toDp().value.sp
                )
            }
        }

    }
}

expect fun showToast(context: PlatformContext, errorMessage: String)
