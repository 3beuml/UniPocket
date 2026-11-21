package com.unipocket.demo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.feature.focus.FocusScreen
import com.unipocket.feature.focus.FocusViewModel

/** 番茄钟页：计时逻辑在框架里；传入记录存储即可获得统计区 */
@Composable
fun FocusTab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val recordStore = remember {
        SharedPrefsFocusRecordStore(
            context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE),
        )
    }
    val viewModel: FocusViewModel = viewModel(
        key = "focus",
        factory = FocusViewModel.factory(recordStore = recordStore),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    FocusScreen(
        state = state,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onReset = viewModel::reset,
        modifier = modifier,
        stats = stats,
    )
}
