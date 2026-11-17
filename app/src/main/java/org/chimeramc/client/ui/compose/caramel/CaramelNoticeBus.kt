package org.chimeramc.client.ui.compose.caramel

import android.content.Context
import android.widget.Toast

/**
 * The single entry point for transient messages, so every screen can raise the launcher's own toast.
 *
 * <p>The launcher's toast slides in and out and honours the reduce-motion setting; the square system
 * bubble does neither. Screens that live outside the launcher tree (the in-game overlays, the logcat
 * dialog, the screenshot share sheet) could not reach the Compose toast and drew a raw
 * [Toast] instead, so the same app showed two different notice styles depending on where you were.
 *
 * <p>A screen posts here. When the launcher is mounted it registers a [Sink] and the message is
 * drawn on the Caramel toast; when nothing is mounted (a cold splash, a game-only process) the call
 * degrades to a system toast so the message is never lost. The sink is held weakly and cleared on
 * detach, so a finished activity cannot be kept alive by a stale message.
 */
object CaramelNoticeBus {

    /** What the mounted launcher installs to receive notices. */
    fun interface Sink {
        fun post(text: String, warning: Boolean)
    }

    @Volatile
    private var sink: Sink? = null

    /** Installs the launcher's sink. The launcher calls this from `onStart`. */
    @JvmStatic
    fun attach(sink: Sink) {
        this.sink = sink
    }

    /** Removes the sink, but only when [sink] is the one still installed. */
    @JvmStatic
    fun detach(sink: Sink) {
        if (this.sink === sink) this.sink = null
    }

    /**
     * Raises [text] on the launcher's toast when one is mounted, otherwise on a system toast.
     *
     * <p>[context] is only used for the fallback, so a caller may pass the application context. The
     * message is never dropped: an empty string is the only thing that does nothing.
     */
    @JvmStatic
    @JvmOverloads
    fun post(context: Context?, text: String?, warning: Boolean = false) {
        if (text.isNullOrEmpty()) return
        val current = sink
        if (current != null) {
            current.post(text, warning)
            return
        }
        val ctx = context ?: return
        val app = ctx.applicationContext ?: ctx
        org.chimeramc.client.util.MainThread.post {
            Toast.makeText(app, text, if (warning) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    /** Raises the string resource [resId] on the launcher's toast, or a system toast as a fallback. */
    @JvmStatic
    fun post(context: Context?, resId: Int) {
        post(context, context?.getString(resId), false)
    }

    /** Raises the string resource [resId] as a failure, with the toast's warning rail. */
    @JvmStatic
    fun postWarning(context: Context?, resId: Int) {
        post(context, context?.getString(resId), true)
    }

    /** Raises an already-formatted failure [text] with the toast's warning rail. */
    @JvmStatic
    fun postWarning(context: Context?, text: String?) {
        post(context, text, true)
    }
}
