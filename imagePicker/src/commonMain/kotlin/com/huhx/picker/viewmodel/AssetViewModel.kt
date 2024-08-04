package com.huhx.picker.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import coil3.Uri
import com.huhx.picker.AssetRoute
import com.huhx.picker.formatDirectoryName
import com.huhx.picker.model.AssetDirectory
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType
import com.huhx.picker.provider.AssetPickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

val init_directory = "所有图片" to "所有图片"

internal class AssetViewModel(
    private val assetPickerRepository: AssetPickerRepository,
    private val navController: NavController,
) : ViewModel() {

    val initialTopBarHeight = mutableStateOf(40.dp)
    val initialBottomBarHeight = mutableStateOf(30.dp)

    val assets = mutableStateListOf<AssetInfo>()
    private val _directoryGroup = mutableStateListOf<AssetDirectory>()

    val directoryGroup: List<AssetDirectory>
        get() = _directoryGroup

    val selectedList = mutableStateListOf<AssetInfo>()
    var directory by mutableStateOf(init_directory)

    suspend fun initDirectories() {
        withContext(Dispatchers.IO){
            initAssets(RequestType.COMMON)
            withContext(Dispatchers.Default){
                val directoryList = assets.groupBy {
                    it.directory
                }.map {
                    AssetDirectory(directory = formatDirectoryName(it.key) to it.key, assets = it.value)
                }.apply {
                    println("initDirectories: ${this.size} ${assets.size}")
                }
                _directoryGroup.clear()

                println("assets: ${assets.size}")
                _directoryGroup.add(AssetDirectory(directory = init_directory, assets = assets))
                _directoryGroup.addAll(directoryList)
            }
        }
    }

    fun clear() {
        selectedList.clear()
    }

    fun toggleSelect(selected: Boolean, assetInfo: AssetInfo) {
        if (selected) {
            selectedList += assetInfo
        } else {
            selectedList -= assetInfo
        }
    }

    fun getGroupedAssets(requestType: RequestType): Map<String, List<AssetInfo>> {
        val assetList = _directoryGroup.first {
            it.directory == directory
        }.assets
        return assetList.filter {
            when (requestType) {
                RequestType.COMMON -> true
                RequestType.IMAGE -> it.isImage()
                RequestType.VIDEO -> it.isVideo()
            }
        }
            .sortedByDescending { it.date }
            .groupBy { it.dateString }
    }

    fun isAllSelected(assets: List<AssetInfo>): Boolean {
        val selectedIds = selectedList.map { it.id }
        val ids = assets.map { it.id }
        return selectedIds.containsAll(ids)
    }

    fun hasSelected(assets: List<AssetInfo>): Boolean {
        val selectedIds = selectedList.map { it.id }
        val ids = assets.map { it.id }

        return selectedIds.any { ids.contains(it) }
    }

    fun navigateToPreview(index: Int, dateString: String, requestType: RequestType) {
        navController.navigate(AssetRoute.preview(index, dateString, requestType))
    }

    suspend fun deleteImage(cameraUri: Uri?) {
        assetPickerRepository.deleteByUri(cameraUri)
    }

    suspend fun getUri(): Uri? {
        return assetPickerRepository.insertImage()
    }

    fun unSelectAll(resources: List<AssetInfo>) {
        selectedList -= resources.toSet()
    }

    fun selectAll(resources: List<AssetInfo>, maxAssets: Int): Boolean {
        val selectedIds = selectedList.map { it.id }
        val newSelectedList = resources.filterNot { selectedIds.contains(it.id) }

        selectedList += newSelectedList.subList(
            0,
            minOf(maxAssets - selectedIds.size, newSelectedList.size)
        )
        return maxAssets - selectedIds.size < newSelectedList.size
    }

    private suspend fun initAssets(requestType: RequestType) {
        assets.clear()
        assets.addAll(assetPickerRepository.getAssets(requestType))
    }
}