package com.huhx.picker.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.Transient

internal expect interface BasicScreenSerializer

internal abstract class BasicScreen<T : BasicViewModel>(
    @Transient private val create: () -> T,
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
        val screenModel: T = model

        DisposableEffect(Unit) {
            if (model.shouldRePrepare()) {
                prepare(screenModel, onNavigateUp, onNavigate)
            }

            onDispose {
                screenModel.onDispose()
            }
        }
        
        with(LocalDensity.current) {
            val topAppBarHeightAssign = remember { mutableStateOf(0.dp) }
            val bottomBarHeightAssign = remember { mutableStateOf(0.dp) }
            Scaffold(
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                contentColor = Color.Transparent,
                containerColor = rootColor,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    modelTopBar(screenModel, onNavigateUp, onNavigate, topAppBarHeightAssign)
                },
                content = {
                    val tabbarHeight = topAppBarHeightAssign.value
                    val bottomBarHeight = bottomBarHeightAssign.value
                    BoxWithConstraints(
                        Modifier
                            .padding(bottom = bottomBarHeight)
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        modelContent(screenModel, onNavigateUp, onNavigate, tabbarHeight)
                    }
                },
                bottomBar = {
                    modelBottomBar(screenModel, onNavigateUp, onNavigate, bottomBarHeightAssign)
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
    private fun actionsRowScope(model: T, scope: RowScope) {
        scope.actions(model, onNavigateUp = null, onNavigate = null)
    }

    @Composable
    open fun RowScope.actions(
        model: T, 
        onNavigateUp: (() -> Unit)?, 
        onNavigate: ((String) -> Unit)?
    ) {

    }

    @Composable
    open fun modelTopBar(
        model: T,
        onNavigateUp: (() -> Unit)?,
        onNavigate: ((String) -> Unit)?,
        topAppBarHeightAssign: MutableState<Dp>
    ) {

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
        onNavigate: ((String) -> Unit)?, 
        bottomBarHeightAssign: MutableState<Dp>
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
        return model.onBackPressed()
    }
}

expect annotation class ParcelizeImpl()
