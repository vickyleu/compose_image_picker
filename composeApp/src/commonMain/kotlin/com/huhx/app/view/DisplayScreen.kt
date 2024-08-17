package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cafe.adriel.voyager.navigator.Navigator
import com.huhx.app.AppRoute
import com.huhx.app.data.MomentModelFactory
import com.huhx.app.data.MomentRepository
import com.huhx.app.data.MomentViewModel


class DisplayViewModel : BasicViewModel() {
}

class DisplayScreen : BasicScreen<DisplayViewModel>(
    create = { DisplayViewModel() }
) {
    @Composable
    override fun modelContent(model: DisplayViewModel, navigator: Navigator, tabbarHeight: Dp) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)){
            val navController = rememberNavController()
            val viewModel: MomentViewModel = viewModel(
                factory = MomentModelFactory(momentRepository = MomentRepository())
            )
            AppRoute(navController = navController, viewModel = viewModel)
        }
    }


}