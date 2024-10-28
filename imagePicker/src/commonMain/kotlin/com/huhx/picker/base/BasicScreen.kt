package com.huhx.picker.base

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.ScreenTransition
import kotlin.jvm.Transient

internal expect interface BasicScreenSerializer

internal expect interface TabImpl : Tab

@OptIn(ExperimentalVoyagerApi::class)
internal abstract class BasicScreen<T : BasicViewModel>(
    @Transient private val create: () -> T,
) : TabImpl, BasicScreenSerializer, ScreenTransition {

    @delegate:Transient
    internal val model: T by lazy {
        create()
    }
    override val key: ScreenKey = uniqueScreenKey


    open val rootColor: Color
        @Composable
        get() = Color.White//MaterialTheme.colorScheme.background


    @OptIn(InternalVoyagerApi::class)
    @Suppress("UNCHECKED_CAST")
    @Composable
    final override fun Content() {
        val scope = rememberCoroutineScope()
        val screenModel: BasicViewModel =
            rememberScreenModel<BasicViewModel>(
                tag = try {
                    "${model.hashCode()}:${model::class.qualifiedName}"
                } catch (ignored: Exception) {
                    "ignored"
                }
            ) {
                model
            }

        val navigator = navigator
        DisposableEffect(Unit) {
            if (model.shouldRePrepare()) {
                prepare(screenModel as T, navigator = navigator)
            }

            onDispose {
//                println("navigator dispose ${this@BasicScreen::class.simpleName}")

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
                    modelTopBar(screenModel as T, navigator, topAppBarHeightAssign)
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
                        modelContent(screenModel as T, navigator, tabbarHeight)
                    }
                },
                bottomBar = {
                    modelBottomBar(screenModel as T, navigator,bottomBarHeightAssign)
                }
            )
        }
    }

    open fun prepare(model: T, navigator: Navigator) {
        model.prepare()
    }


    override val options: TabOptions
        @Composable
        get() = TabOptions(index = 0u, title = "${this::class.simpleName}")


    @Composable
    private fun actionsRowScope(model: T, scope: RowScope) {
        scope.actions(model, navigator)
    }

    @Composable
    open fun RowScope.actions(model: T, navigator: Navigator) {

    }

    @Composable
    open fun modelTopBar(
        model: T,
        navigator: Navigator,
        topAppBarHeightAssign: MutableState<Dp>
    ) {

    }

    internal open fun forceShowingBack(): Boolean {
        return false
    }

    @Composable
    abstract fun modelContent(model: T, navigator: Navigator, tabbarHeight: Dp)

    @Composable
    open fun modelBottomBar(model: T, navigator: Navigator, bottomBarHeightAssign: MutableState<Dp>) {
    }

    @Composable
    open fun canPop(): Boolean {
        return navigator.canPop
    }

    open fun recycle() {
        model.recycle()
    }


    open suspend fun onBackPressed(model: T, navigator: Navigator): Boolean {
        return model.onBackPressed()
    }

    @delegate:Transient
    private var _isTransitionOpen by mutableStateOf(transitionAnimationEnable())

    open fun transitionAnimationEnable(): Boolean {
        return true
    }

    override fun enter(lastEvent: StackEvent): EnterTransition? {
       return if (try {
                _isTransitionOpen
            } catch (e: Exception) {
                false
            }
        ) slideInHorizontally(
            animationSpec = tween(500),
            initialOffsetX = {
                it
            }
        ) + fadeIn(
            animationSpec = tween(500)
        ) else null
    }



    @Transient
    private val isInTransition = mutableStateOf(false)

    override fun exit(lastEvent: StackEvent): ExitTransition? {
        return (if (try {
                _isTransitionOpen
            } catch (e: Exception) {
                false
            }
        ) {
            (slideOutHorizontally(
                animationSpec = tween(500),
                targetOffsetX = {
                    it
                }
            ) + fadeOut(
                animationSpec = tween(500)
            ))
        } else null)
    }

    final fun transitionAnimation(enable: Boolean) {
        _isTransitionOpen = enable
    }
}

expect annotation class ParcelizeImpl()

private val BasicScreen<*>.navigator: Navigator
    @Composable
    get() = LocalNavigatorController.current

@OptIn(InternalVoyagerApi::class)
val LocalNavigatorController = compositionLocalOf(structuralEqualityPolicy<Navigator>()) {
    Navigator(
        screens = listOf(), key = "fakeKey", stateHolder = object : SaveableStateHolder {
            @Composable
            override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
            }

            override fun removeState(key: Any) {
            }
        },
        disposeBehavior = NavigatorDisposeBehavior()
    )
}
