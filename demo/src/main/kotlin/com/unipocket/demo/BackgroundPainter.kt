package com.unipocket.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * 把偏好里的背景图 URI 解码成 Compose 的 [Painter]。
 *
 * 框架只管画（[com.unipocket.ui.AppBackground]），解码与权限归 App——
 * 这样框架不必引入图片库。这里给接入方一份可直接抄的参考实现：
 * - 后台线程解码，不卡首帧；
 * - 先读尺寸再按需降采样，避免把 5000px 的照片整张读进内存。
 */
@Composable
fun rememberBackgroundPainter(uri: String?): Painter? {
    val context = LocalContext.current
    val painter by produceState<Painter?>(initialValue = null, uri) {
        value = if (uri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadScaledBitmap(context, Uri.parse(uri))?.let { BitmapPainter(it.asImageBitmap()) }
            }
        }
    }
    return painter
}

private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sample = max(1, max(bounds.outWidth / 1080, bounds.outHeight / 2400))
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

/**
 * 支持 `content://`、`file://` 与 `file:///android_asset/`。
 * assets 里的示例背景图不需要任何存储权限。
 */
private fun openStream(context: Context, uri: Uri): java.io.InputStream? {
    val path = uri.path
    if (path != null && path.startsWith("/android_asset/")) {
        return context.assets.open(path.removePrefix("/android_asset/"))
    }
    return context.contentResolver.openInputStream(uri)
}
