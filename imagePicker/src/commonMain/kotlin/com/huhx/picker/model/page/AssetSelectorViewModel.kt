package com.huhx.picker.model.page

import com.huhx.picker.base.BasicViewModel
import com.huhx.picker.model.AssetDirectory

internal class AssetSelectorViewModel(
    val directory: String,
    val assetDirectories: List<AssetDirectory>,
    val onSelected: (String) -> Unit
): BasicViewModel() {
}