package com.unipocket.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.feature.focus.FocusScreen
import com.unipocket.feature.focus.FocusViewModel

/** 番茄钟页：计时逻辑全在框架里，这里只接状态与按钮 */
@Composable
fun FocusTab(modifier: Modifier = Modifier) {
    val viewModel: FocusViewModel = viewModel(factory = FocusViewModel.factory())
    val state by viewModel.state.collectAsStateWithLifecycle()

    FocusScreen(
        state = state,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}
