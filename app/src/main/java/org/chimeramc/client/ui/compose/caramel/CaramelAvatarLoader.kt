package org.chimeramc.client.ui.compose.caramel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import org.chimeramc.client.util.AppLog
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads and caches a remote profile picture (an Xbox avatar, a GitHub contributor's picture).
 *
 * <p>Avatars are small HTTPS images, so a bounded in-memory LRU plus a copy under the app cache is
 * enough — no image library is pulled in. A failed or non-http URL returns null and the caller draws
 * its placeholder, so a network problem never blanks the screen. The download runs on the caller's
 * dispatcher (the composable moves it off the main thread).
 */
object CaramelAvatarLoader {

    private const val TIMEOUT_MS = 10_000
    private const val MAX_BYTES = 4 * 1024 * 1024

    private val memory = object : LruCache<String, Bitmap>(64) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /** Whether [url] is an http(s) URL worth attempting; anything else draws the fallback. */
    fun isLoadable(url: String?): Boolean =
        !url.isNullOrEmpty() && (url.startsWith("https://") || url.startsWith("http://"))

    /** A filesystem-safe cache file name for [url]. Pure, so the mapping is unit-tested. */
    fun cacheName(url: String): String = Integer.toHexString(url.hashCode()) + ".png"

    /** Returns the avatar for [url], from memory, the disk cache, or the network, or null. */
    fun load(context: Context, url: String?): Bitmap? {
        if (!isLoadable(url)) return null
        val key = url as String
        memory.get(key)?.let { return it }
        val file = File(cacheDir(context), cacheName(key))
        val cached = decode(file)
        if (cached != null) {
            memory.put(key, cached)
            return cached
        }
        val downloaded = download(key) ?: return null
        memory.put(key, downloaded)
        write(file, downloaded)
        return downloaded
    }

    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "avatars")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun decode(file: File): Bitmap? {
        if (!file.isFile) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (t: Throwable) {
            null
        }
    }

    private fun write(file: File, bitmap: Bitmap) {
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (t: Throwable) {
            AppLog.warnOnce(TAG, "CaramelAvatarLoader:write", "Could not cache an avatar", t)
        }
    }

    private fun download(url: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (t: Throwable) {
            AppLog.warnOnce(TAG, "CaramelAvatarLoader:download",
                    "Could not load a profile picture", t)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private const val TAG = "CaramelAvatarLoader"
}
