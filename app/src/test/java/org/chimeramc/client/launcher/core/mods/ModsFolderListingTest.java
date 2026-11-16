package org.chimeramc.client.launcher.core.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

/**
 * The mods-folder browser lists the folder's real contents, not only the mods the loader recognises.
 *
 * <p>The listing is what "Open mods folder" now draws: every file and sub-directory, directories
 * first, so a user can see a stray {@code .zip} or a stray {@code .so} that ModManager would have
 * skipped.
 */
public class ModsFolderListingTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void listsEveryEntryNotJustRecognisedMods() throws Exception {
        File root = temp.newFolder("mods");
        temp.newFolder("mods", "sodium-mod");
        assertTrue(new File(root, "readme.txt").createNewFile());
        assertTrue(new File(root, "loose.so").createNewFile());

        List<ModsFolderListing.Entry> entries = ModsFolderListing.list(root);
        assertEquals(3, entries.size());
        // Directories sort before files.
        assertEquals("sodium-mod", entries.get(0).name);
        assertTrue(entries.get(0).directory);
        assertTrue(entries.get(1).name.compareToIgnoreCase(entries.get(2).name) < 0);
    }

    @Test
    public void missingFolderIsAnEmptyListNotACrash() {
        assertTrue(ModsFolderListing.list(new File(temp.getRoot(), "does-not-exist")).isEmpty());
        assertTrue(ModsFolderListing.list(null).isEmpty());
    }

    @Test
    public void formatSizeUsesReadableUnits() {
        assertEquals("812 B", ModsFolderListing.formatSize(812));
        assertEquals("4.0 KB", ModsFolderListing.formatSize(4096));
        assertEquals("1.5 MB", ModsFolderListing.formatSize(1572864));
    }

    @Test
    public void relativePathReadsTheRootAsSlashAndChildrenByName() throws Exception {
        File root = temp.newFolder("root");
        File child = temp.newFolder("root", "inner");
        assertEquals("/", ModsFolderListing.relativePath(root, root));
        assertEquals("inner", ModsFolderListing.relativePath(root, child));
    }
}
