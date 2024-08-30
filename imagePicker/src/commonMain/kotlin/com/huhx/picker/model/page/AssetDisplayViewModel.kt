package com.huhx.picker.model.page

import com.huhx.picker.base.BasicViewModel
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.viewmodel.AssetViewModel

internal class AssetDisplayViewModel internal constructor(
    val viewModel: AssetViewModel,
    val onPicked: (List<AssetInfo>) -> Unit,
    val onClose: (List<AssetInfo>) -> Unit
) : BasicViewModel() {

}