package org.chimeramc.client.core.minecraft;

import android.content.Context;

import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.launcher.settings.OptionsText;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.AppLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The instance's own {@code minecraftpe/options.txt}, as the performance tab reads and writes it.
 *
 * <p>Every performance feature is a key in this one file. The path is resolved exactly as
 * {@code ContentManagementActivity} and {@code ScreenshotStore} resolve it — the selected version's
 * storage profile through {@link LauncherStorage#getActiveGameDataDir} — so a write can never land
 * in a different root than the one the game actually loads.
 *
 * <p>Reading returns the raw text so the panel can show a value the user set in the game rather than
 * a default; the line surgery itself is {@link OptionsText}, which is pure and unit-tested. A write
 * rewrites only the keys it was given, so the game's own keys survive untouched.
 */
public final class InstanceOptionsStore {

    private static final String TAG = "InstanceOptionsStore";

    private InstanceOptionsStore() {
    }

    /** The instance's {@code options.txt}, or null when no instance or storage root is resolvable. */
    public static File optionsFile(Context context, GameVersion version) {
        if (context == null || version == null) return null;
        FeatureSettings.StorageType type = LauncherStorage.readSavedContentStorageType(context);
        File gameDataDir = LauncherStorage.getActiveGameDataDir(
                context, version.getStorageProfileId(), version.versionIsolation, type);
        if (gameDataDir == null) return null;
        return new File(gameDataDir, "minecraftpe/options.txt");
    }

    /** The file's text, or an empty string when it does not exist or cannot be read. */
    public static String read(Context context, GameVersion version) {
        File file = optionsFile(context, version);
        if (file == null || !file.isFile()) return "";
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 4L * 1024L * 1024L)];
            int read = in.read(buffer);
            if (read <= 0) return "";
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            AppLog.warnOnce(TAG, "read", "Could not read the instance options file", e);
            return "";
        }
    }

    /** The stored value for [key], or null when the file has no such key. */
    public static String get(Context context, GameVersion version, String key) {
        return OptionsText.get(read(context, version), key);
    }

    /**
     * Writes [values] into the instance's options file.
     *
     * <p>The whole file is read, the given keys are replaced in place, and it is written back, so a
     * key the launcher does not know survives. Returns true when the file was written.
     */
    public static boolean write(Context context, GameVersion version, Map<String, String> values) {
        File file = optionsFile(context, version);
        if (file == null || values == null || values.isEmpty()) return false;
        String text = read(context, version);
        String updated = text;
        for (Map.Entry<String, String> entry : new LinkedHashMap<>(values).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            updated = OptionsText.set(updated, entry.getKey(), entry.getValue());
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            AppLog.warnOnce(TAG, "mkdir", "Could not create the options directory", null);
            return false;
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(updated.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            AppLog.warnOnce(TAG, "write", "Could not write the instance options file", e);
            return false;
        }
    }
}
