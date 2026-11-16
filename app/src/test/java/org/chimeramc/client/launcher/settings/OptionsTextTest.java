package org.chimeramc.client.launcher.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The options-file surgery, which is the one place a mistake corrupts a player's settings.
 *
 * <p>A Bedrock {@code options.txt} is the game's file: the launcher only ever edits keys it was
 * asked to edit. This pins the two properties that keeps true — an unknown key survives untouched,
 * and the file's own newline convention is preserved — plus the round-trip a slider depends on.
 */
public class OptionsTextTest {

    private static final String GAME_FILE =
            "gfx_viewdistance:320\n"
                    + "gfx_fancyskies:1\n"
                    + "game_difficulty_new:2\n";

    @Test
    public void readingFindsAValueAndReportsAMissingKeyAsNull() {
        assertEquals("320", OptionsText.get(GAME_FILE, "gfx_viewdistance"));
        assertEquals("1", OptionsText.get(GAME_FILE, "gfx_fancyskies"));
        assertNull(OptionsText.get(GAME_FILE, "no_such_key"));
        assertNull(OptionsText.get(null, "gfx_viewdistance"));
        assertNull(OptionsText.get(GAME_FILE, null));
    }

    @Test
    public void writingReplacesTheLineInPlaceAndLeavesEveryOtherByteAlone() {
        String updated = OptionsText.set(GAME_FILE, "gfx_fancyskies", "0");
        assertEquals("gfx_viewdistance:320\ngfx_fancyskies:0\ngame_difficulty_new:2\n", updated);
        assertEquals("the other keys survive", "320",
                OptionsText.get(updated, "gfx_viewdistance"));
        assertEquals("the other keys survive", "2",
                OptionsText.get(updated, "game_difficulty_new"));
    }

    @Test
    public void writingAMissingKeyAppendsItAndKeepsTheTrailingNewline() {
        String updated = OptionsText.set(GAME_FILE, "gfx_vsync", "1");
        assertTrue(updated.startsWith(GAME_FILE));
        assertTrue(updated.endsWith("gfx_vsync:1\n"));
        assertEquals("1", OptionsText.get(updated, "gfx_vsync"));
    }

    @Test
    public void aFileWithNoTrailingNewlineDoesNotGainOne() {
        String updated = OptionsText.set("gfx_vsync:0", "gfx_vsync", "1");
        assertEquals("gfx_vsync:1", updated);
        assertFalse(updated.endsWith("\n"));
    }

    @Test
    public void writingIntoAnEmptyOrNullFileCreatesTheKey() {
        assertEquals("gfx_vsync:1", OptionsText.set("", "gfx_vsync", "1"));
        assertEquals("gfx_vsync:1", OptionsText.set(null, "gfx_vsync", "1"));
    }

    @Test
    public void aValueThatContainsAColonIsReadBackWhole() {
        String updated = OptionsText.set("", "key", "a:b:c");
        assertEquals("a:b:c", OptionsText.get(updated, "key"));
    }

    @Test
    public void hasReportsPresence() {
        assertTrue(OptionsText.has(GAME_FILE, "gfx_viewdistance"));
        assertFalse(OptionsText.has(GAME_FILE, "gfx_vsync"));
    }

    @Test
    public void aRoundTripOfEveryCatalogueKeyIsStable() {
        StringBuilder builder = new StringBuilder();
        for (org.chimeramc.client.core.minecraft.PerfFeatureCatalog.Feature f
                : org.chimeramc.client.core.minecraft.PerfFeatureCatalog.all()) {
            builder.append(f.key).append(':').append(f.defaultValue).append('\n');
        }
        String text = builder.toString();
        for (org.chimeramc.client.core.minecraft.PerfFeatureCatalog.Feature f
                : org.chimeramc.client.core.minecraft.PerfFeatureCatalog.all()) {
            assertEquals("every catalogue key round-trips: " + f.id,
                    f.defaultValue, OptionsText.get(text, f.key));
        }
        // A second pass must not change a single byte, or a slider drag would grow the file.
        String again = text;
        for (org.chimeramc.client.core.minecraft.PerfFeatureCatalog.Feature f
                : org.chimeramc.client.core.minecraft.PerfFeatureCatalog.all()) {
            again = OptionsText.set(again, f.key, f.defaultValue);
        }
        assertEquals("writing the same values is idempotent", text, again);
    }
}
