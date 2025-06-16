package com.huhx.app.view

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.jvm.Transient
import kotlinx.coroutines.CoroutineScope


data class TitleInfo(
    val titleCenterPosition: Offset,
    val titleStyle: TextStyle,
)

abstract class BasicViewModel {
    private val isFirstLoading = mutableStateOf(true)


    open fun shouldRePrepare(): Boolean {
        if (isFirstLoading.value) {
            isFirstLoading.value = false
            return true
        }
        return false
    }

    @Transient
    var scope: CoroutineScope? = null

    @Transient
    var depth: Int = 0

    fun injectScreen(screen: BasicScreen<*>, scope: CoroutineScope, depth: Int) {
        this.scope = scope
        this.depth = depth
    }

    fun dejectScreen(screen: BasicScreen<*>, scope: CoroutineScope, depth: Int) {
        this.scope = null
        this.depth = 0
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
}

