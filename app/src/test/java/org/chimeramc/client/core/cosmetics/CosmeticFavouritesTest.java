package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Cosmetics star must actually star something.
 *
 * <p>The tile drew a star that never changed: the host's {@code toggleFavourite} was an empty method
 * and the grid rebuilt every row with {@code favourite = false}, so the star reverted on the next
 * refresh. {@link CosmeticStore#toggledFavourite} is the rule that decides membership; these tests
 * pin it and the order it preserves.
 */
public class CosmeticFavouritesTest {

    @Test
    public void anAbsentIdJoinsAndAPresentIdLeaves() {
        Set<String> afterFirst = CosmeticStore.toggledFavourite(Collections.<String>emptySet(), "cape_1");
        assertTrue(afterFirst.contains("cape_1"));

        Set<String> afterSecond = CosmeticStore.toggledFavourite(afterFirst, "cape_1");
        assertFalse("a second tap must un-star", afterSecond.contains("cape_1"));
    }

    @Test
    public void starringAnotherIdKeepsTheFirst() {
        Set<String> one = CosmeticStore.toggledFavourite(Collections.<String>emptySet(), "cape_1");
        Set<String> two = CosmeticStore.toggledFavourite(one, "hat_2");
        assertEquals(2, two.size());
        assertTrue(two.contains("cape_1"));
        assertTrue(two.contains("hat_2"));
    }

    @Test
    public void theRuleDoesNotMutateItsInput() {
        Set<String> original = new LinkedHashSet<>();
        original.add("cape_1");
        CosmeticStore.toggledFavourite(original, "cape_1");
        assertEquals("the caller's set must be left alone", 1, original.size());
    }

    @Test
    public void aNullOrBlankIdChangesNothing() {
        assertTrue(CosmeticStore.toggledFavourite(Collections.<String>emptySet(), null).isEmpty());
        assertTrue(CosmeticStore.toggledFavourite(Collections.<String>emptySet(), "").isEmpty());
    }

    @Test
    public void aNullCurrentSetIsTreatedAsEmpty() {
        assertEquals(Collections.singleton("cape_1"),
                CosmeticStore.toggledFavourite(null, "cape_1"));
    }
}
