package com.huhx.picker.base

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import cafe.adriel.voyager.core.model.ScreenModel
import kotlin.jvm.Transient


internal abstract class BasicViewModel : ScreenModel {
    private val isFirstLoading = mutableStateOf(true)


    open fun shouldRePrepare(): Boolean {
        if (isFirstLoading.value) {
            isFirstLoading.value = false
            return true
        }
        return false
    }


    open suspend fun backCallBack(): Boolean {
        return true
    }

    @Transient
    private val backPressed = derivedStateOf { ::backCallBack }


    suspend fun onBackPressed(): Boolean {
        return backPressed.value.invoke()
    }

    open fun prepare() {

    }

    open fun recycle() {

    }

    final override fun onDispose() {
        recycle()
//        println("navigator model this::${this::class.simpleName}")
    }

}


