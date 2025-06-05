package com.huhx.picker.model.page

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.dokar.sonner.ToasterState
import com.huhx.picker.base.BasicViewModel
import com.huhx.picker.model.DateTimeFormatterKMP
import com.huhx.picker.model.RequestType
import com.huhx.picker.viewmodel.AssetViewModel

internal class AssetPreviewViewModel(
    val viewModel: AssetViewModel,
    val toasterState: ToasterState?,
    val index: Int,
    val time: String,
    val requestType: RequestType
):BasicViewModel() {
    val pageIndex = mutableIntStateOf(index)
    val dtf = DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日 HH:mm:ss")
    val assets = viewModel.getGroupedAssets(requestType).toList().sortedByDescending {
        dtf.parse(it.first)
    }.flatMap { (time, list) ->
        list.map {
            time to it
        }.sortedByDescending {
            dtf.parse(it.first)
        }
    }
    val assetInfo =  mutableStateOf(assets[pageIndex.value].second)

    override fun prepare() {
        super.prepare()
        println("navigator作用域:prepare")
    }
}