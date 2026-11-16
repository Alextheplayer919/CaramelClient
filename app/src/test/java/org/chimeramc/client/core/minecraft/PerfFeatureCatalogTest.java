package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The performance tab is a promise that every control writes a real game setting.
 *
 * <p>The tab was empty. Filling it with switches that only moved a launcher flag would have been
 * worse than leaving it empty, so this test pins the two properties that make the tab honest:
 *
 * <ul>
 *   <li>every entry names a distinct, well-formed {@code options.txt} key — two features sharing a
 *       key would silently overwrite each other, and a key with a space or a colon could not be
 *       parsed back;</li>
 *   <li>the value a control writes is a value the game accepts for that key — an off/on pair for a
 *       toggle, one of the listed values for a choice, and a range value that clamps to the bounds
 *       and formats to something the game can parse for a slider.</li>
 * </ul>
 *
 * <p>The catalogue is pure, so this runs on the JVM with no device and no file system.
 */
public class PerfFeatureCatalogTest {

    /** The game's own value grammar: no whitespace, no colon, non-empty. */
    private static boolean isWellFormedKey(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isWhitespace(c) || c == ':') return false;
        }
        return true;
    }

    @Test
    public void theCatalogueIsLargeEnoughToBeWorthATab() {
        assertTrue("the tab promises a real feature set, not a token few: "
                + PerfFeatureCatalog.all().size(), PerfFeatureCatalog.all().size() >= 50);
    }

    @Test
    public void everyFeatureNamesAWellFormedDistinctKey() {
        Set<String> keys = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
            assertTrue("a key the game can parse: " + f.id + " -> " + f.key,
                    isWellFormedKey(f.key));
            assertTrue("no two features share a key: " + f.key, keys.add(f.key));
            assertTrue("no two features share an id: " + f.id, ids.add(f.id));
            assertNotNull("every feature needs a display name: " + f.id, f.name);
            assertFalse("every feature needs a display name: " + f.id, f.name.trim().isEmpty());
            assertNotNull("every feature needs a description: " + f.id, f.description);
            assertFalse("every feature needs a description: " + f.id,
                    f.description.trim().isEmpty());
            assertNotNull("every feature needs a group: " + f.id, f.group);
        }
    }

    @Test
    public void everyFeatureHasAValueTheGameAccepts() {
        for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
            assertNotNull("a default value is what a reset writes: " + f.id, f.defaultValue);
            switch (f.kind) {
                case TOGGLE:
                    assertEquals("a toggle is an off/on pair: " + f.id, 2, f.values.length);
                    assertEquals("0", f.offValue());
                    assertEquals("1", f.onValue());
                    assertTrue("a toggle default is off or on: " + f.id,
                            "0".equals(f.defaultValue) || "1".equals(f.defaultValue));
                    break;
                case CHOICE:
                    assertNotNull("a choice needs values: " + f.id, f.values);
                    assertNotNull("a choice needs labels: " + f.id, f.labels);
                    assertEquals("one label per value: " + f.id, f.values.length, f.labels.length);
                    assertTrue("a choice needs at least two options: " + f.id, f.values.length >= 2);
                    boolean carriesDefault = false;
                    for (String v : f.values) {
                        if (v.equals(f.defaultValue)) carriesDefault = true;
                    }
                    assertTrue("a choice default must be one of its values: " + f.id,
                            carriesDefault);
                    break;
                case RANGE:
                    assertTrue("a range needs a real span: " + f.id, f.max > f.min);
                    assertTrue("a range step is positive: " + f.id, f.step > 0f);
                    assertTrue("a range default parses: " + f.id,
                            parses(f.defaultValue));
                    float d = Float.parseFloat(f.defaultValue);
                    assertTrue("a range default is inside the range: " + f.id,
                            d >= f.min && d <= f.max);
                    break;
                default:
                    throw new AssertionError("unhandled kind for " + f.id);
            }
        }
    }

    @Test
    public void aRangeClampsToItsOwnBoundsAndFormats() {
        for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
            if (f.kind != PerfFeatureCatalog.Kind.RANGE) continue;
            assertEquals("below the range clamps to the minimum: " + f.id,
                    formatExpected(f.min, f.decimals),
                    PerfFeatureCatalog.rangeValue(f, -1f));
            assertEquals("above the range clamps to the maximum: " + f.id,
                    formatExpected(f.max, f.decimals),
                    PerfFeatureCatalog.rangeValue(f, 2f));
            String mid = PerfFeatureCatalog.rangeValue(f, 0.5f);
            float parsed = Float.parseFloat(mid);
            assertTrue("a mid value stays inside the range: " + f.id,
                    parsed >= f.min && parsed <= f.max);
            // A written value must round-trip: the fraction the panel derives from it must map back
            // to the same value, or the slider would drift on every recomposition.
            assertEquals("a written value round-trips: " + f.id, mid,
                    PerfFeatureCatalog.rangeValue(f, PerfFeatureCatalog.rangeFraction(f, mid)));
        }
    }

    @Test
    public void aMissingOrInvalidRangeValueReadsAsTheFloor() {
        PerfFeatureCatalog.Feature f = PerfFeatureCatalog.find("view_distance");
        assertNotNull(f);
        assertEquals(0f, PerfFeatureCatalog.rangeFraction(f, null), 0.0001f);
        assertEquals(0f, PerfFeatureCatalog.rangeFraction(f, "not a number"), 0.0001f);
        assertEquals(0f, PerfFeatureCatalog.rangeFraction(f, ""), 0.0001f);
    }

    @Test
    public void findIsByIdAndNullSafe() {
        assertNull(PerfFeatureCatalog.find(null));
        assertNull(PerfFeatureCatalog.find("no_such_feature"));
        assertNotNull(PerfFeatureCatalog.find("view_distance"));
        assertEquals("gfx_viewdistance", PerfFeatureCatalog.find("view_distance").key);
    }

    @Test
    public void groupsCoverEveryFeatureExactlyOnce() {
        List<String> groups = PerfFeatureCatalog.groups();
        assertFalse(groups.isEmpty());
        int counted = 0;
        for (String group : groups) {
            List<PerfFeatureCatalog.Feature> inGroup = PerfFeatureCatalog.inGroup(group);
            assertFalse("a group is never empty: " + group, inGroup.isEmpty());
            counted += inGroup.size();
            for (PerfFeatureCatalog.Feature f : inGroup) {
                assertEquals(group, f.group);
            }
        }
        assertEquals("every feature belongs to exactly one group",
                PerfFeatureCatalog.all().size(), counted);
    }

    @Test
    public void aChoiceLabelNeverFallsBackToTheRawValueForAKnownValue() {
        for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
            if (f.kind != PerfFeatureCatalog.Kind.CHOICE) continue;
            for (int i = 0; i < f.values.length; i++) {
                assertEquals("a known value shows its label: " + f.id,
                        f.labels[i], f.labelFor(f.values[i]));
            }
        }
    }

    private static boolean parses(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String formatExpected(float value, int decimals) {
        if (decimals <= 0) return Integer.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }
}
