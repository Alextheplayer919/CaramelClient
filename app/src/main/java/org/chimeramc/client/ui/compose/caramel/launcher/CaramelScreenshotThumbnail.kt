package org.chimeramc.client.ui.compose.caramel.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.T
import java.io.File

/**
 * A real screenshot thumbnail for the Screenshots modal.
 *
 * <p>The modal drew a tinted gradient with the file name, so a player could not tell one shot from
 * another without opening each. This decodes the captured file at a bounded size off the main thread
 * and draws it. A file that has been moved or deleted since the scan falls back to the name, rather
 * than a broken frame.
 */
@Composable
fun CaramelScreenshotThumbnail(path: String, name: String, modifier: Modifier) {
    val bitmap = produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodeBounded(path) }
    }.value
    DisposableEffect(bitmap) {
        onDispose { bitmap?.recycle() }
    }
    Box(modifier.background(C.Navy), Alignment.Center) {
        val image = bitmap
        if (image == null) {
            Text(name, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize())
        } else {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Decodes a screenshot at roughly a thumbnail's resolution, never allocating the full image. */
private fun decodeBounded(path: String, target: Int = 512): Bitmap? {
    val file = File(path)
    if (!file.isFile) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target)
        }
        BitmapFactory.decodeFile(path, options)
    } catch (t: Throwable) {
        null
    }
}

/** The power-of-two downsample that keeps the longest edge at or above {@code target}. */
fun sampleSize(width: Int, height: Int, target: Int): Int {
    if (width <= 0 || height <= 0 || target <= 0) return 1
    var sample = 1
    var longest = maxOf(width, height)
    while (longest / 2 >= target) {
        longest /= 2
        sample *= 2
    }
    return sample
}
