package org.chimeramc.client.launcher.core.mods;

import org.chimeramc.client.util.AppLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The real contents of a mods folder, for the in-app browser.
 *
 * <p>"Open mods folder" handed the directory to whatever file manager the device had, which on a
 * bare device is nothing at all — the press looked dead. It also only ever surfaced the mods the
 * client recognises (a folder with a manifest, or a loose {@code .so}), so the user's other files
 * were invisible. This lists everything the folder actually holds, so the browser shows the user's
 * real files, not the subset the loader happens to understand.
 */
public final class ModsFolderListing {

    private static final String TAG = "ModsFolderListing";

    private ModsFolderListing() {
    }

    /** One entry in a folder: a file or a sub-directory, with enough to draw a row. */
    public static final class Entry {
        public final String name;
        public final String path;
        public final boolean directory;
        public final long sizeBytes;

        public Entry(String name, String path, boolean directory, long sizeBytes) {
            this.name = name;
            this.path = path;
            this.directory = directory;
            this.sizeBytes = sizeBytes;
        }
    }

    /**
     * Lists {@code directory}: directories first, then files, each sorted case-insensitively.
     *
     * <p>Never throws: an unreadable or missing folder is an empty list, because the browser then
     * draws its empty state rather than a crash.
     */
    public static List<Entry> list(File directory) {
        List<Entry> out = new ArrayList<>();
        if (directory == null || !directory.isDirectory()) return out;
        File[] children;
        try {
            children = directory.listFiles();
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "list:" + directory.getName(), "Could not list the folder", t);
            return out;
        }
        if (children == null) return out;
        Arrays.sort(children, Comparator
                .comparing(File::isDirectory).reversed()
                .thenComparing(f -> f.getName().toLowerCase(Locale.ROOT)));
        for (File child : children) {
            out.add(new Entry(child.getName(), child.getAbsolutePath(),
                    child.isDirectory(), child.isDirectory() ? 0L : child.length()));
        }
        return out;
    }

    /** A short, human size: {@code 812 B}, {@code 4.2 KB}, {@code 1.7 MB}. Empty for a directory. */
    public static String formatSize(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb);
        return String.format(Locale.ROOT, "%.1f GB", mb / 1024.0);
    }

    /**
     * The path shown in the browser's breadcrumb: {@code root} itself reads as "/", a child reads as
     * its name relative to the root, and anything outside the root reads as its absolute path.
     */
    public static String relativePath(File root, File current) {
        if (current == null) return "/";
        if (root == null) return current.getName();
        String rootPath = root.getAbsolutePath();
        String currentPath = current.getAbsolutePath();
        if (currentPath.equals(rootPath)) return "/";
        if (currentPath.startsWith(rootPath + File.separator)) {
            return currentPath.substring(rootPath.length() + 1);
        }
        return currentPath;
    }
}
