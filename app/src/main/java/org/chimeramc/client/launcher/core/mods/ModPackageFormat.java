package org.chimeramc.client.core.mods;

import java.util.Locale;

/**
 * The kinds of mod package the launcher can import.
 *
 * <p>There are three import routes and they are not interchangeable: a native {@code .so} is the
 * legacy preloader library, a bare {@code .zip} is the unpacked-layout package, and a
 * {@code .universe} / {@code .singularity} archive is a package for the SweetLoader Rust engines.
 * The format decides which loader is asked to validate and extract the file, so it is derived here,
 * once, from the file name rather than guessed at each call site.
 *
 * <p>Pure and Android-free so the classification can be unit-tested without a device.
 */
public enum ModPackageFormat {
    /** A bare native library; the legacy preloader route. */
    NATIVE_SO("preload-native"),
    /** A bare {@code .zip} in the unpacked layout (one manifest, one {@code .so}). */
    LEGACY_ZIP("preload-native"),
    /** A {@code .universe} package for the {@code libcaramel_universe.so} loader. */
    UNIVERSE("universe"),
    /** A {@code .singularity} world-gen pack for the {@code libcaramel_singularity.so} engine. */
    SINGULARITY("singularity");

    /** The value written to a mod manifest's {@code type} field. */
    public final String manifestType;

    ModPackageFormat(String manifestType) {
        this.manifestType = manifestType;
    }

    /** The format a file name names, or {@code null} when it is not an importable mod package. */
    public static ModPackageFormat fromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".so")) {
            return NATIVE_SO;
        }
        if (lower.endsWith(".universe")) {
            return UNIVERSE;
        }
        if (lower.endsWith(".singularity")) {
            return SINGULARITY;
        }
        if (lower.endsWith(".zip")) {
            return LEGACY_ZIP;
        }
        return null;
    }

    /** True for a package the SweetLoader Rust engines own, rather than the legacy preloader. */
    public boolean isEnginePackage() {
        return this == UNIVERSE || this == SINGULARITY;
    }

    /** The manifest {@code type} value that names this format. */
    public static ModPackageFormat fromManifestType(String type) {
        if (type == null) {
            return null;
        }
        for (ModPackageFormat format : values()) {
            if (format.manifestType.equals(type)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Whether an engine package's entry file is named for the format its manifest claims.
     *
     * <p>An engine package's entry is the archive itself, so a manifest claiming {@code universe}
     * whose entry is {@code thing.singularity} is a package the loader would hand to the wrong
     * engine. A non-engine format is always consistent, because its entry is a library checked
     * separately. Pure, so the discovery rule is pinned without a device.
     */
    public static boolean entryMatchesFormat(ModPackageFormat format, String entryFileName) {
        if (format == null || !format.isEnginePackage()) {
            return true;
        }
        return fromFileName(entryFileName) == format;
    }
}
