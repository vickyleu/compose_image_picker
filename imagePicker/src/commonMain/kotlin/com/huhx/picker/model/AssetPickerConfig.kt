package com.huhx.picker.model

data class AssetPickerConfig(
    val maxAssets: Int = 9,
    val gridCount: Int = 3,
    val requestType: RequestType = RequestType.IMAGE,
    val maxFileSize: Long = 50 * 1024 * 1024,
)