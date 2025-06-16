package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.Transient

expect interface BasicScreenSerializer

abstract class BasicScreen<T : BasicViewModel>(
    @Transient private val create: () -> T,
    @Transient private val screenDepth: T.(screen: BasicScreen<T>) -> Int = { 0 },
) : BasicScreenSerializer {

    @delegate:Transient
    internal val model: T by lazy {
        create()
    }

    open val rootColor: Color
        @Composable
        get() = Color.White

    @Composable
    fun Content(
        onNavigateUp: (() -> Unit)? = null,
        onNavigate: ((String) -> Unit)? = null
    ) {
        val scope = rememberCoroutineScope()
        val screenModel: T = model.apply {
            val depth = screenDepth(this@BasicScreen)
            injectScreen(this@BasicScreen, scope, depth)
        }

        DisposableEffect(Unit) {
            if (model.shouldRePrepare()) {
                prepare(screenModel, onNavigateUp, onNavigate)
            }

            onDispose {
                val depth = with(screenModel) { screenDepth(this@BasicScreen) }
                screenModel.dejectScreen(this@BasicScreen, scope, depth)
            }
        }
        
        with(LocalDensity.current) {
            val topAppBarHeightAssign = remember { mutableStateOf(0.dp) }
            Scaffold(
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                contentColor = Color.Transparent,
                containerColor = rootColor,
                content = {
                    val tabbarHeight = topAppBarHeightAssign.value
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        modelContent(screenModel, onNavigateUp, onNavigate, tabbarHeight)
                    }
                },
                bottomBar = {
                    modelBottomBar(screenModel, onNavigateUp, onNavigate)
                }
            )
        }
    }

    open fun prepare(
        model: T, 
        onNavigateUp: (() -> Unit)?, 
        onNavigate: ((String) -> Unit)?
    ) {
        model.prepare()
    }

    @Composable
    abstract fun modelContent(
        model: T, 
        onNavigateUp: (() -> Unit)?, 
        onNavigate: ((String) -> Unit)?, 
        tabbarHeight: Dp
    )

    @Composable
    open fun modelBottomBar(
        model: T, 
        onNavigateUp: (() -> Unit)?, 
        onNavigate: ((String) -> Unit)?
    ) {
    }

    open fun recycle() {
        model.recycle()
    }

    open suspend fun onBackPressed(
        model: T, 
        onNavigateUp: (() -> Unit)?, 
        onNavigate: ((String) -> Unit)?
    ): Boolean {
        val result = model.onBackPressed()
        if (result) {
            onNavigateUp?.invoke()
        }
        return result
    }
}

expect annotation class ParcelizeImpl()
