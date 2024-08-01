package com.huhx.app.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.huhx.picker.viewmodel.isAssignableFromKMP
import kotlin.reflect.KClass

class MomentModelFactory(
    private val momentRepository: MomentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return if (modelClass.isAssignableFromKMP(MomentViewModel::class)) {
            MomentViewModel(momentRepository) as T
        } else {
            throw IllegalArgumentException("ViewModel is Missing")
        }
    }
}