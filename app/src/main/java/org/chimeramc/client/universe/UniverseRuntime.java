package org.chimeramc.client.universe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.ModPackageFormat;
import org.chimeramc.client.util.AppLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Drives the {@code .universe} load/tick/shutdown lifecycle from the launcher.
 *
 * <p>This is the missing link the audit named: the Rust loader's {@code load_mods} / {@code tick_mods}
 * / {@code shutdown_mods} existed and were tested, but nothing called them, so an enabled engine
 * package could be discovered, listed and signed yet never run. This class is that caller.
 *
 * <p><b>It is a thin orchestration shell.</b> It extracts each enabled package into the engine
 * sandbox (the Rust {@code extract}, Zip-Slip-safe), reads the package's own manifest for the
 * library to load, the dependencies and the declared permissions, renders the loader's spec array,
 * and hands it to the native loader. Every decision about <em>what runs</em> — load order, cycle
 * refusal, per-mod permission reduction, fault isolation — stays in Rust; this class only decides
 * <em>which installed packages are eligible</em>.
 *
 * <p><b>Fail-closed.</b> A package whose engine library is absent, whose extraction fails, or whose
 * manifest is unreadable is skipped with a recorded reason; the other packages still load. Nothing
 * here throws to a caller and nothing claims a mod is live that the loader did not confirm.
 *
 * <p>The native methods are implemented in the sweetloader C++ (which owns the host table the loader
 * is handed); the Rust cdylib is loaded first, so the C++ bridge can resolve it by name.
 */
public final class UniverseRuntime {

    private static final String TAG = "UniverseRuntime";
    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final int MAX_MANIFEST_BYTES = 1 << 20;

    private static boolean sweetloaderAttempted;
    private static boolean sweetloaderLoaded;
    private static boolean initialised;

    /**
     * True once a load reported at least one live mod.
     *
     * <p>The frame tick is on the hot path, and for the common case (no {@code .universe} package
     * enabled) there is nothing to tick. This flag lets {@link #tick} return immediately rather than
     * crossing into native code every frame, while still ticking the instant a mod is live.
     */
    private static volatile boolean liveModsPresent;

    private UniverseRuntime() {
    }

    /**
     * Runs {@link UniverseLoader#init} once, stamping the loader with this build's real version.
     *
     * <p>Without this the loader keeps the crate's {@code 1.0.0} and compares each package's
     * {@code min_client_version} against it (audit 6.6), so a package could never require a newer
     * client than {@code 1.0.0} and the gate said nothing about the client actually installed. The
     * BerryAPI pointer is passed as 0: the C++ bridge does not expose the raw API table, and the
     * loader's BerryAPI-backed features resolve their own symbols by name and report unavailable
     * when absent, so 0 is honest rather than a guessed address.
     */
    private static synchronized void ensureInitialised() {
        if (initialised) return;
        initialised = true;
        String refusal = UniverseLoader.init(0L, org.chimeramc.client.BuildConfig.VERSION_NAME);
        if (refusal != null && !refusal.isEmpty()) {
            AppLog.warn(TAG, "The .universe loader refused init: " + refusal);
        }
    }

    /** Loads {@code libsweetloader.so} once; returns whether its JNI surface is available. */
    public static synchronized boolean ensureLoaded() {
        if (sweetloaderLoaded) return true;
        if (sweetloaderAttempted) return false;
        sweetloaderAttempted = true;
        try {
            System.loadLibrary("sweetloader");
            sweetloaderLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "libsweetloader.so is not available; engine packages cannot run", e);
        }
        return sweetloaderLoaded;
    }

    /**
     * Extracts and loads every enabled {@code .universe} package in {@code mods}.
     *
     * <p>Runs off the main thread (it does file IO and {@code dlopen}s mod code). Each package is
     * extracted into {@code sandboxRoot} under its own id, so two packages never collide. A package
     * the loader refuses is reported, not fatal.
     *
     * @param sandboxRoot the engine sandbox root (must be inside the app's files dir); each package
     *                    is extracted as {@code sandboxRoot/<id>}
     * @param mods        the instance's discovered mods; only enabled {@code .universe} packages are
     *                    considered
     * @return the loader's JSON report, or an empty string when nothing loaded or the runtime is
     *         unavailable
     */
    public static String load(File sandboxRoot, List<Mod> mods) {
        if (!ensureLoaded() || sandboxRoot == null) return "";
        // The Rust cdylib must be loaded before the C++ bridge can resolve it by name.
        if (!UniverseLoader.ensureLoaded()) {
            AppLog.warn(TAG, "libcaramel_universe.so is not available; engine packages cannot run");
            return "";
        }
        ensureInitialised();

        List<UniverseModSpecs.Spec> specs = new ArrayList<>();
        for (Mod mod : mods) {
            if (mod == null || !mod.isEnabled()) continue;
            ModPackageFormat format = ModPackageFormat.fromFileName(mod.getEntryPath());
            if (format != ModPackageFormat.UNIVERSE) {
                // `.singularity` is a world-gen pack with no per-frame entry points; it is not a
                // `.universe` library and is handled by its own engine, not this loader.
                continue;
            }
            File packageFile = packageFileOf(mod);
            if (packageFile == null || !packageFile.isFile()) {
                AppLog.warn(TAG, "Skipping engine package with no archive: " + mod.getDisplayName());
                continue;
            }
            UniverseModSpecs.Spec spec = prepareSpec(sandboxRoot, packageFile);
            if (spec != null) {
                specs.add(spec);
            }
        }
        if (specs.isEmpty()) {
            return "";
        }

        String specsJson = UniverseModSpecs.toJson(specs);
        try {
            String report = nativeLoadEngineMods(specsJson);
            if (report != null && !report.isEmpty()) {
                android.util.Log.i(TAG, "engine load report: " + report);
            }
            liveModsPresent = report != null && report.contains("\"live\":[\"");
            return report == null ? "" : report;
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "the engine load entry point is not available", e);
            return "";
        }
    }

    /** Ticks every live engine mod once with the frame's delta time. */
    public static void tick(float deltaSeconds) {
        if (!liveModsPresent || !sweetloaderLoaded) return;
        try {
            nativeTickEngineMods(deltaSeconds);
        } catch (UnsatisfiedLinkError e) {
            // Fail-closed: a build without the entry point leaves the tick a no-op.
            sweetloaderLoaded = false;
        }
    }

    /** Shuts every live engine mod down. Safe to call when nothing is loaded. */
    public static void shutdown() {
        if (!sweetloaderLoaded) return;
        liveModsPresent = false;
        try {
            nativeShutdownEngineMods();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "the engine shutdown entry point is not available", e);
        }
    }

    /** The ids the loader reports live, as the loader's JSON array (or empty). */
    public static String liveMods() {
        if (!sweetloaderLoaded) return "";
        try {
            String report = nativeLiveEngineMods();
            return report == null ? "" : report;
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    /**
     * Extracts one package and builds its loader spec, or null when it cannot be prepared.
     *
     * <p>Extraction and the manifest read both go through the Rust loader, so the path handling and
     * the format validation are the same ones the loader will apply at {@code dlopen} time.
     */
    private static UniverseModSpecs.Spec prepareSpec(File sandboxRoot, File packageFile) {
        JsonObject manifest = readEngineManifest(packageFile);
        String id = optString(manifest, "id");
        if (id.isEmpty()) {
            AppLog.warn(TAG, "Skipping engine package with no manifest id: " + packageFile.getName());
            return null;
        }
        String refusal = UniverseLoader.extract(packageFile, sandboxRoot, sandboxRoot);
        if (refusal != null && !refusal.isEmpty()) {
            AppLog.warn(TAG, "Refusing to extract engine package " + id + ": " + refusal);
            return null;
        }
        File extractedDir = new File(sandboxRoot, id);
        String libPath = firstNativeLib(manifest, extractedDir);
        if (libPath == null) {
            AppLog.warn(TAG, "Engine package " + id + " names no extractable native library");
            return null;
        }
        // `trusted` gates the one permission tied to trust (`write_memory`). A package is trusted
        // only when its detached signature verified against a key set this client ships or the user
        // pinned — and no trusted-key set is supplied yet (audit 6.7), so every package is untrusted
        // here. That is the honest default: an unsigned mod runs (it is native code either way) but
        // does not get the memory-write capability through the host table.
        return new UniverseModSpecs.Spec(id, libPath,
                stringList(manifest, "dependencies"),
                stringList(manifest, "permissions"),
                false);
    }

    /** The absolute path of the package's first declared native library, or null. */
    private static String firstNativeLib(JsonObject manifest, File extractedDir) {
        for (String relative : stringList(manifest, "native_libs")) {
            File candidate = new File(extractedDir, relative);
            if (candidate.isFile()) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    /** The mod's package archive, or null when the directory path is unset. */
    private static File packageFileOf(Mod mod) {
        String root = mod.getModRootPath();
        String entry = mod.getEntryPath();
        if (root == null || entry == null) return null;
        return new File(new File(root), entry);
    }

    /**
     * Reads a package's engine manifest as a JSON object, or an empty object.
     *
     * <p>Pure with respect to the archive: it only reads, and any malformed input yields an empty
     * object so the caller's field reads all fall back rather than throwing.
     */
    static JsonObject readEngineManifest(File packageFile) {
        String json = readZipEntryText(packageFile, MANIFEST_FILE_NAME);
        if (json == null) return new JsonObject();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            AppLog.warnOnce(TAG, "engine-manifest", "Engine manifest is not valid JSON", e);
            return new JsonObject();
        }
    }

    private static String readZipEntryText(File zipFile, String entryName) {
        if (zipFile == null || !zipFile.isFile()) return null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entryName.equals(entry.getName())) {
                    return readBounded(zis);
                }
            }
        } catch (IOException e) {
            AppLog.warnOnce(TAG, "engine-manifest-read", "Could not read the engine manifest", e);
        }
        return null;
    }

    private static String readBounded(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = in.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_MANIFEST_BYTES) {
                return null;
            }
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String optString(JsonObject object, String key) {
        if (object == null || !object.has(key)) return "";
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return "";
        try {
            return value.getAsString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> result = new ArrayList<>();
        if (object == null || !object.has(key)) return result;
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) return result;
        JsonArray array = value.getAsJsonArray();
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                String text = element.getAsString();
                if (text != null && !text.trim().isEmpty()) {
                    result.add(text.trim());
                }
            }
        }
        return result;
    }

    private static native String nativeLoadEngineMods(String specsJson);

    private static native void nativeTickEngineMods(float deltaSeconds);

    private static native void nativeShutdownEngineMods();

    private static native String nativeLiveEngineMods();
}
