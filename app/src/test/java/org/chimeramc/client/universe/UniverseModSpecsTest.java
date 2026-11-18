package org.chimeramc.client.universe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The {@code .universe} loader spec marshalling. Pure: no Android, no native code.
 *
 * <p>This pins the exact JSON shape {@code caramel_universe_load_mods} parses, because a field
 * rename or a dropped key would not throw — the loader would simply see a mod with no library or no
 * permissions, and the package would be "loaded" with every capability silently withdrawn.
 */
public class UniverseModSpecsTest {

    @Test
    public void rendersEveryFieldTheLoaderReads() {
        UniverseModSpecs.Spec spec = new UniverseModSpecs.Spec(
                "example", "/data/app/lib/libexample.so",
                Arrays.asList("core", "utils"),
                Arrays.asList("read_memory", "register_hook"),
                true);
        String json = UniverseModSpecs.toJson(Collections.singletonList(spec));
        assertTrue(json, json.contains("\"id\":\"example\""));
        assertTrue(json, json.contains("\"lib_path\":\"/data/app/lib/libexample.so\""));
        assertTrue(json, json.contains("\"dependencies\":[\"core\",\"utils\"]"));
        assertTrue(json, json.contains("\"permissions\":[\"read_memory\",\"register_hook\"]"));
        assertTrue(json, json.contains("\"trusted\":true"));
    }

    @Test
    public void aBlankIdIsDropped() {
        // The loader keys load order, permissions and unload on the id, so an id-less spec could not
        // be planned; it must not reach the loader as an empty-id entry.
        List<UniverseModSpecs.Spec> specs = Arrays.asList(
                new UniverseModSpecs.Spec("  ", "/x.so", null, null, false),
                new UniverseModSpecs.Spec(null, "/y.so", null, null, false),
                new UniverseModSpecs.Spec("real", "/z.so", null, null, false));
        String json = UniverseModSpecs.toJson(specs);
        assertTrue(json, json.contains("\"id\":\"real\""));
        assertFalse(json, json.contains("\"id\":\"\""));
    }

    @Test
    public void nullListsRenderAsEmptyArrays() {
        UniverseModSpecs.Spec spec = new UniverseModSpecs.Spec("a", "/a.so", null, null, false);
        String json = UniverseModSpecs.toJson(Collections.singletonList(spec));
        assertTrue(json, json.contains("\"dependencies\":[]"));
        assertTrue(json, json.contains("\"permissions\":[]"));
        assertTrue(json, json.contains("\"trusted\":false"));
    }

    @Test
    public void aQuoteOrBackslashInAPathIsEscapedNotBroken() {
        // Hand-built JSON would have produced an unparseable array here; the loader would then see
        // "the mod list is not valid JSON" and load nothing.
        UniverseModSpecs.Spec spec = new UniverseModSpecs.Spec(
                "weird", "/tmp/a\"b\\c.so", null, null, false);
        String json = UniverseModSpecs.toJson(Collections.singletonList(spec));
        assertTrue(json, json.contains("\\\""));
        assertTrue(json, json.contains("\\\\"));
    }

    @Test
    public void blankListEntriesAreDropped() {
        UniverseModSpecs.Spec spec = new UniverseModSpecs.Spec(
                "a", "/a.so", Arrays.asList("", "  ", "dep"), Arrays.asList("  ", "read_memory"),
                false);
        String json = UniverseModSpecs.toJson(Collections.singletonList(spec));
        assertTrue(json, json.contains("\"dependencies\":[\"dep\"]"));
        assertTrue(json, json.contains("\"permissions\":[\"read_memory\"]"));
    }

    @Test
    public void anEmptyListIsAnEmptyArray() {
        assertEquals("[]", UniverseModSpecs.toJson(Collections.<UniverseModSpecs.Spec>emptyList()));
        assertEquals("[]", UniverseModSpecs.toJson(null));
    }
}
