package com.huhx.app.view

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlin.jvm.Transient


data class TitleInfo(
    val titleCenterPosition: Offset,
    val titleStyle: TextStyle,
)

abstract class BasicViewModel : ScreenModel{

    open val topBarColor: Color
        get() = Color.White

    open val showingTitle: Boolean
        get() = true

    open val naviBarColor: Color
        get() = Color.White

    open val hasTopBar: Boolean
        get() = true

    private val isFirstLoading = mutableStateOf(true)


    open fun shouldRePrepare(): Boolean {
        if(isFirstLoading.value){
            isFirstLoading.value = false
           return true
        }
        return false
    }

    val titleInfo = mutableStateOf(
        TitleInfo(
            Offset.Unspecified,
            TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
    )

    open suspend fun backCallBack(): Boolean {
        return true
    }

    @Transient
    private val backPressed = derivedStateOf { ::backCallBack }


    suspend fun onBackPressed(): Boolean {
        return backPressed.value.invoke()
    }

    open fun prepare() {}
    open fun recycle() {

    }

    final override fun onDispose() {
        recycle()
    }


    open fun <T : BasicViewModel> injectScreen(screen: BasicScreen<T>, scope: CoroutineScope,depth:Int) {

    }

    open fun <T : BasicViewModel> dejectScreen(screen: BasicScreen<T>, scope: CoroutineScope,depth:Int) {

    }

}


abstract class BasicTabViewModel : BasicViewModel() {
    override val hasTopBar: Boolean
        get() = false
    override val topBarColor: Color
        get() = Color.Transparent
    override val showingTitle: Boolean
        get() = false

}

abstract class BasicStatelessViewModel : BasicViewModel() {
    override val hasTopBar: Boolean
        get() = false
    override val topBarColor: Color
        get() = Color.Transparent
    override val showingTitle: Boolean
        get() = false
}