package com.huhx.picker.base

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.jvm.Transient


internal abstract class BasicViewModel {
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

    open fun onDispose() {
        recycle()
    }

}


