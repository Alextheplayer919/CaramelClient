package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the import-format classification.
 *
 * <p>The format decides which loader validates and extracts a package, so a wrong answer here either
 * routes a SweetLoader engine package through the legacy preloader (which would read it as a bare
 * zip and reject it) or the reverse. The classification is pure, so it is pinned without a device.
 */
public class ModPackageFormatTest {

    @Test
    public void everySupportedExtensionClassifies() {
        assertEquals(ModPackageFormat.NATIVE_SO, ModPackageFormat.fromFileName("libfoo.so"));
        assertEquals(ModPackageFormat.LEGACY_ZIP, ModPackageFormat.fromFileName("pack.zip"));
        assertEquals(ModPackageFormat.UNIVERSE, ModPackageFormat.fromFileName("mod.universe"));
        assertEquals(ModPackageFormat.SINGULARITY, ModPackageFormat.fromFileName("world.singularity"));
    }

    @Test
    public void theExtensionMatchIsCaseInsensitive() {
        assertEquals(ModPackageFormat.UNIVERSE, ModPackageFormat.fromFileName("MOD.UNIVERSE"));
        assertEquals(ModPackageFormat.SINGULARITY, ModPackageFormat.fromFileName("Pack.Singularity"));
    }

    @Test
    public void anUnrelatedOrNullNameIsNotAnImportablePackage() {
        assertNull(ModPackageFormat.fromFileName(null));
        assertNull(ModPackageFormat.fromFileName("notes.txt"));
        assertNull(ModPackageFormat.fromFileName("archive.tar.gz"));
        // A name that merely contains the word is not the extension.
        assertNull(ModPackageFormat.fromFileName("universe.txt"));
    }

    @Test
    public void onlyTheEngineFormatsAreEnginePackages() {
        assertTrue(ModPackageFormat.UNIVERSE.isEnginePackage());
        assertTrue(ModPackageFormat.SINGULARITY.isEnginePackage());
        assertFalse(ModPackageFormat.NATIVE_SO.isEnginePackage());
        assertFalse(ModPackageFormat.LEGACY_ZIP.isEnginePackage());
    }

    @Test
    public void manifestTypesRoundTrip() {
        assertEquals(ModPackageFormat.UNIVERSE,
                ModPackageFormat.fromManifestType(ModPackageFormat.UNIVERSE.manifestType));
        assertEquals(ModPackageFormat.SINGULARITY,
                ModPackageFormat.fromManifestType(ModPackageFormat.SINGULARITY.manifestType));
        assertNull(ModPackageFormat.fromManifestType("something-else"));
        assertNull(ModPackageFormat.fromManifestType(null));
    }

    @Test
    public void anEngineEntryMustMatchTheFormatItsManifestClaims() {
        assertTrue(ModPackageFormat.entryMatchesFormat(ModPackageFormat.UNIVERSE, "mod.universe"));
        assertTrue(ModPackageFormat.entryMatchesFormat(ModPackageFormat.SINGULARITY, "pack.singularity"));
        assertFalse(ModPackageFormat.entryMatchesFormat(ModPackageFormat.UNIVERSE, "mod.singularity"));
        assertFalse(ModPackageFormat.entryMatchesFormat(ModPackageFormat.SINGULARITY, "mod.universe"));
        assertFalse(ModPackageFormat.entryMatchesFormat(ModPackageFormat.UNIVERSE, null));
    }

    @Test
    public void aNonEngineEntryIsAlwaysConsistent() {
        // The native entry is checked as a library elsewhere, so this rule does not constrain it.
        assertTrue(ModPackageFormat.entryMatchesFormat(ModPackageFormat.NATIVE_SO, "libfoo.so"));
        assertTrue(ModPackageFormat.entryMatchesFormat(null, "anything"));
    }
}
