package com.huhx.picker.model.page

import com.dokar.sonner.ToasterState
import com.huhx.picker.base.BasicViewModel
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.AssetPickerConfig
import com.huhx.picker.viewmodel.AssetViewModel

internal class AssetDisplayViewModel internal constructor(
    val viewModel: AssetViewModel,
    val toasterState: ToasterState?=null,
    val onPicked: (List<AssetInfo>) -> Unit,
    val assetPickerConfig: AssetPickerConfig
) : BasicViewModel() {

}