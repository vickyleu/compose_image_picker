package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.Navigator
import com.github.jing332.filepicker.base.absolutePath
import com.github.jing332.filepicker.base.inputStream
import com.github.jing332.filepicker.base.useImpl
import com.huhx.app.ImagePicker
import com.huhx.app.data.MomentModelFactory
import com.huhx.app.data.MomentRepository
import com.huhx.app.data.MomentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DisplayViewModel : BasicViewModel() {
}

class DisplayScreen : BasicScreen<DisplayViewModel>(
    create = { DisplayViewModel() }
) {
    @Composable
    override fun modelContent(model: DisplayViewModel, navigator: Navigator, tabbarHeight: Dp) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(top = tabbarHeight)) {
            val viewModel: MomentViewModel = viewModel(
                factory = MomentModelFactory(momentRepository = MomentRepository())
            )
            val scope = rememberCoroutineScope()
            ImagePicker(
                onPicked = {
                    val copy = listOf(*it.toTypedArray())
                    viewModel.selectedList.clear()
                    println("ImagePicker onPicked:${it.size}")
                    viewModel.selectedList.addAll(it)

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            delay(500)
                            viewModel.selectedList.clear()
                        }
                    }
                },
                onClose = {
                    val copy = listOf(*it.toTypedArray())
                    viewModel.selectedList.clear()
                }
            )
            LaunchedEffect(key1 = viewModel) {
                snapshotFlow { viewModel.selectedList.size }
                    .distinctUntilChanged()
                    .collect {
                        if (it == 1) {
                            viewModel.selectedList
                                .map { file ->
                                    val buffer = ByteArray(file.length().toInt())
                                    file.inputStream().useImpl {
                                        it.read(buffer)
                                    }
                                    println("bytesRead:${buffer.size}  ${file.absolutePath} ")
                                    buffer
                                }.toList().lastOrNull()?.apply {

                                }
                        }
                    }
            }
        }
    }


}