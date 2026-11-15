package org.chimeramc.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * The one place a local file or directory is handed to another app.
 *
 * <p>Both calls build an {@code ACTION_VIEW} intent with a {@code content:} uri from this app's
 * {@link FileProvider}, because a {@code file:} uri has not been readable by another app since
 * API 24 — the plain form opens a chooser that then fails. The intent is implicit (the user picks
 * the app), which lint flags as {@code UnsafeImplicitIntentLaunch}; that is intended here, so the
 * exception lives in one audited place rather than at each call site, the same way {@link UrlOpener}
 * holds the one for https links.
 */
public final class LocalFileOpener {

    private LocalFileOpener() {
    }

    /**
     * Opens {@code directory} in whatever app handles a folder.
     *
     * <p>Android has no "open this directory" action, so this is the only route. Best-effort: no
     * file manager is a false return, not a crash.
     */
    public static boolean openFolder(Context context, File directory) {
        if (directory == null) return false;
        return view(context, directory, "resource/folder");
    }

    /** Opens {@code file} as an image. Best-effort, like {@link #openFolder}. */
    public static boolean openImage(Context context, File file) {
        if (file == null) return false;
        return view(context, file, "image/*");
    }

    /**
     * {@code UnsafeImplicitIntentLaunch} is suppressed here on purpose and only here: the target is
     * whichever app the user has chosen for the type, so naming a component would defeat the point.
     * The uri is a {@code content:} uri scoped to this app's provider, which is what makes the
     * implicit launch safe.
     */
    @SuppressLint("UnsafeImplicitIntentLaunch")
    private static boolean view(Context context, File file, String mimeType) {
        if (context == null || !file.exists()) return false;
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            AppLog.warnOnce("LocalFileOpener", "view:" + file.getName(),
                    "Could not open the file", t);
            return false;
        }
    }
}
