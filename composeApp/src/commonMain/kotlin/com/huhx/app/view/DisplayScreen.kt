package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cafe.adriel.voyager.navigator.Navigator
import com.github.jing332.filepicker.base.FileImpl
import com.github.jing332.filepicker.base.inputStream
import com.github.jing332.filepicker.base.useImpl
import com.huhx.app.AppRoute
import com.huhx.app.data.MomentModelFactory
import com.huhx.app.data.MomentRepository
import com.huhx.app.data.MomentViewModel
import com.huhx.picker.model.toUri
import kotlinx.coroutines.flow.distinctUntilChanged


class DisplayViewModel : BasicViewModel() {
}

class DisplayScreen : BasicScreen<DisplayViewModel>(
    create = { DisplayViewModel() }
) {
    @Composable
    override fun modelContent(model: DisplayViewModel, navigator: Navigator, tabbarHeight: Dp) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(top = tabbarHeight)) {
            val navController = rememberNavController()
            val viewModel: MomentViewModel = viewModel(
                factory = MomentModelFactory(momentRepository = MomentRepository())
            )
            AppRoute(navController = navController, viewModel = viewModel)
            LaunchedEffect(key1 = viewModel) {
                snapshotFlow { viewModel.selectedList.size }
                    .distinctUntilChanged()
                    .collect {
                        if (it == 1) {
                            viewModel.selectedList
                                .mapNotNull {
                                    val path = it.toUri().path
                                    println("selectedList.last()======>> size=${it.size} filepath=${it.filepath} filename=${it.filename}  path=${path}")
                                    if (path != null && it.size > 0) {
                                        path to it.size
                                    } else null
                                }
                                .map { (path, size) ->
                                    val buffer = ByteArray(size.toInt())
                                    FileImpl(path).inputStream().useImpl {
                                        it.read(buffer)
                                    }
                                    println("bytesRead:${buffer.size}  ${path} ")
                                    buffer
                                }.toList().lastOrNull()?.apply {

                                }
                        }
                    }
            }
        }
    }


}