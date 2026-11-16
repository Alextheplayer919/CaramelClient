package org.chimeramc.client.launcher.core.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The About popup's fixed facts, and the strings it draws them with.
 *
 * <p>Every value here is a link a user taps, so a wrong one is a wrong profile: the channel must be
 * the client's own (not the owner's personal one), the date must be the repository's first commit,
 * and each fun fact the screen shows must exist as a translatable string. The last check reads the
 * screen's own source, so a fact added to the popup with no string behind it fails here instead of
 * drawing an empty row.
 */
public class AboutFactsTest {

    @Test
    public void theChannelIsTheClientsOwnAndNotTheOwnersPersonalOne() {
        assertEquals("https://m.youtube.com/@CaramelClientt", AboutFacts.YOUTUBE_URL);
        assertTrue(AboutFacts.YOUTUBE_URL.endsWith("@CaramelClientt"));
    }

    @Test
    public void theRepositoryPointsAtTheProject() {
        assertEquals("https://github.com/ChimeraAnt-DEV/CaramelClient", AboutFacts.REPOSITORY_URL);
    }

    @Test
    public void theDateIsTheDayTheRepositoryStarted() {
        assertEquals("2026-09-04", AboutFacts.MADE_ON);
        assertTrue("the date is shown as-is, so it must be ISO",
                AboutFacts.MADE_ON.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void theFunFactListIsNotAStub() {
        int[] ids = AboutFacts.funFactResIds();
        assertEquals(4, ids.length);
        for (int id : ids) {
            assertTrue("a fun fact must resolve to a real string id", id != 0);
        }
    }

    @Test
    public void everyFunFactThePopupShowsIsATranslatableString() throws IOException {
        Path root = findRepoRoot();
        String facts = read(root.resolve(
                "app/src/main/java/org/chimeramc/client/launcher/core/about/AboutFacts.java"));
        String strings = read(root.resolve("app/src/main/res/values/strings.xml"));

        Set<String> referenced = new LinkedHashSet<>();
        Matcher ref = Pattern.compile("R\\.string\\.(caramel_about_fact_[a-z0-9_]+)").matcher(facts);
        while (ref.find()) referenced.add(ref.group(1));
        assertFalse("the About popup must show the stories it is documented to show",
                referenced.isEmpty());

        List<String> missing = new ArrayList<>();
        for (String key : referenced) {
            Matcher def = Pattern.compile("<string name=\"" + Pattern.quote(key) + "\"([^>]*)>")
                    .matcher(strings);
            if (!def.find()) {
                missing.add(key);
            } else if (def.group(1).contains("translatable=\"false\"")) {
                missing.add(key + " (marked untranslatable)");
            }
        }
        assertTrue("the About fun facts must exist as translatable strings: " + missing, missing.isEmpty());
    }

    @Test
    public void theRosterIsWhatThePopupDraws() {
        List<AboutPeople.Person> all = AboutPeople.all();
        assertEquals(2, all.size());
        for (AboutPeople.Person person : all) {
            assertNotNull(person.avatarUrl);
            assertTrue(person.avatarUrl.startsWith("https://github.com/"));
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path findRepoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("settings.gradle"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo root not found");
    }
}
