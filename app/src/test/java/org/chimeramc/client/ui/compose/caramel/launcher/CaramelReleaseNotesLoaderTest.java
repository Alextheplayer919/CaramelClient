package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.compose.caramel.CaramelReleaseNotes;
import org.chimeramc.client.ui.compose.caramel.CaramelReleaseSection;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The Release notes popup must show this build's notes.
 *
 * <p>The menu item used to switch to the News tab, so pressing "Release notes" showed a feed of
 * posts rather than the release's notes. [CaramelReleaseNotesLoader] folds the bundled changelog into
 * the Added / Changed / Fixed order the popup draws; these tests pin the fold, the order, and the
 * empty-document rule, and read the real bundled document so a change to it that would empty the
 * popup fails here rather than on a device.
 */
public class CaramelReleaseNotesLoaderTest {

    private static String bundledChangelog() throws IOException {
        return new String(Files.readAllBytes(
                Paths.get("..", "resources", "launcher", "changelog.json")), StandardCharsets.UTF_8);
    }

    @Test
    public void theBundledDocumentProducesANonEmptyPopup() throws IOException {
        CaramelReleaseNotes notes = CaramelReleaseNotesLoader.fromJson(bundledChangelog(), "BETA3");
        assertFalse("the shipped changelog must give the popup something to show", notes.isEmpty());
        assertEquals("BETA3", notes.getVersion());
        assertFalse(notes.getHeadline().isEmpty());
        assertTrue("a dated release must render its date line", notes.getDateLine().startsWith("Released "));
        assertFalse(notes.getSections().isEmpty());
    }

    @Test
    public void sectionsAreFoldedIntoTheAddedChangedFixedOrder() throws IOException {
        CaramelReleaseNotes notes = CaramelReleaseNotesLoader.fromJson(bundledChangelog(), "BETA3");
        List<String> keys = new java.util.ArrayList<>();
        for (CaramelReleaseSection section : notes.getSections()) keys.add(section.getTitle());
        assertEquals("the three buckets, in the order a player reads them",
                java.util.Arrays.asList("added", "changed", "fixed"), keys.subList(0, 3));
        for (CaramelReleaseSection section : notes.getSections()) {
            assertFalse("a bucket with no lines must not be drawn", section.getLines().isEmpty());
        }
    }

    @Test
    public void aMissingOrForeignDocumentShowsTheEmptyLine() {
        assertTrue(CaramelReleaseNotesLoader.fromJson(null, "BETA3").isEmpty());
        assertTrue(CaramelReleaseNotesLoader.fromJson("", "BETA3").isEmpty());
        assertTrue(CaramelReleaseNotesLoader.fromJson("{\"schemaVersion\":99}", "BETA3").isEmpty());
        assertTrue(CaramelReleaseNotesLoader.fromJson("not json", "BETA3").isEmpty());
    }

    @Test
    public void theVersionIsCarriedThroughEvenWhenTheDocumentIsEmpty() {
        CaramelReleaseNotes notes = CaramelReleaseNotesLoader.fromJson("{\"schemaVersion\":99}", "BETA3");
        assertEquals("the header still names the running build", "BETA3", notes.getVersion());
    }
}
