package com.unipocket.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.feature.room.RoomScreen
import com.unipocket.feature.room.RoomViewModel

/**
 * 空教室页：数据经由官方 [RoomViewModel] 加载。
 * 提供一个 `RoomSource` 实现即可，筛选与分组都由框架处理。
 */
@Composable
fun RoomTab(modifier: Modifier = Modifier) {
    val config = FakeData.config
    val source = remember { FakeRoomSource() }
    val viewModel: RoomViewModel = viewModel(
        factory = RoomViewModel.factory(config, source),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    RoomScreen(
        config = config,
        state = state,
        modifier = modifier.fillMaxSize(),
        onDaySelected = viewModel::setDayOfWeek,
        onBuildingSelected = viewModel::setBuilding,
        onRetry = viewModel::retry,
    )
}
