package com.huhx.picker.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.decode.Decoder

@Composable
actual fun VideoPreview(
    modifier: Modifier,
    uriString: String,
    loading: @Composable (() -> Unit)?
) {
}

actual fun DecoderFactory(): Decoder.Factory {
    TODO("Not yet implemented")
}