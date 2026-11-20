package com.unipocket.demo

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unipocket.widget.WeekWidgetRenderer
import com.unipocket.widget.WidgetMetrics
import com.unipocket.widget.WidgetStyle

/**
 * 组件预览页：把 RemoteViews 在普通 Activity 里渲染出来，方便在没有桌面入口的设备上验证。
 *
 * 用法（可在运行时改尺寸）：
 * `adb shell am start -n com.unipocket.demo/.WidgetPreviewActivity --ei width 342 --ei height 380`
 */
class WidgetPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widthDp = intent.getIntExtra("width", 342)
        val reportedHeightDp = intent.getIntExtra("height", 380)
        val dark = intent.getBooleanExtra("dark", false)
        // 桌面实际画布 = 上报高度 - 30dp（本机实测虚高量）
        val canvasHeightDp = WidgetMetrics.correctedHeight(reportedHeightDp.toFloat())

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    AndroidView(
                        modifier = Modifier.size(widthDp.dp, canvasHeightDp.dp),
                        factory = { context ->
                            FrameLayout(context).apply {
                                setBackgroundColor(0xFFFFFFFF.toInt())
                                val config = FakeData.config
                                val views = WeekWidgetRenderer.render(
                                    context = context,
                                    config = config,
                                    courses = FakeData.courses,
                                    week = config.semester.currentWeek() ?: 1,
                                    reportedWidthDp = widthDp.toFloat(),
                                    reportedHeightDp = reportedHeightDp.toFloat(),
                                    style = WidgetStyle(dark = dark),
                                )
                                addView(views.apply(context, this))
                            }
                        },
                    )
                }
            }
        }
    }
}
