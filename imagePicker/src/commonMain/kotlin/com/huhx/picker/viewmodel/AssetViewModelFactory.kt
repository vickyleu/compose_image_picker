package com.huhx.picker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import com.huhx.picker.provider.AssetPickerRepository
import kotlin.reflect.KClass

internal class AssetViewModelFactory(
    private val assetPickerRepository: AssetPickerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return AssetViewModel(assetPickerRepository) as T
    }
}