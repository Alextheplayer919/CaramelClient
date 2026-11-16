package org.chimeramc.client.launcher.core.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

/**
 * The rule that decides which key art one launch shows.
 *
 * <p>The promise is "a different handful every time, and never the same opening picture twice in a
 * row". Those are the two properties a random pick does not give for free, so both are pinned here.
 */
public class SplashArtTest {

    @Test
    public void picksTheRequestedNumberOfDistinctIndices() {
        int[] picks = SplashArt.pick(18, 5, new Random(7L), -1);

        assertEquals(5, picks.length);
        Set<Integer> unique = new HashSet<>();
        for (int pick : picks) {
            assertTrue("index in range", pick >= 0 && pick < 18);
            unique.add(pick);
        }
        assertEquals("no repeats", 5, unique.size());
    }

    @Test
    public void neverOpensOnThePreviousLaunchesPicture() {
        Random random = new Random(11L);
        for (int i = 0; i < 200; i++) {
            int[] picks = SplashArt.pick(18, 5, random, 3);
            assertNotEquals("opening slide repeated", 3, picks[0]);
        }
    }

    @Test
    public void aSinglePictureCatalogueStillOpens() {
        int[] picks = SplashArt.pick(1, 5, new Random(1L), 0);

        assertEquals(1, picks.length);
        assertEquals(0, picks[0]);
    }

    @Test
    public void askingForMoreThanTheCatalogueReturnsTheWholeCatalogue() {
        int[] picks = SplashArt.pick(3, 9, new Random(2L), -1);

        assertEquals(3, picks.length);
        Set<Integer> unique = new HashSet<>();
        for (int pick : picks) unique.add(pick);
        assertEquals(3, unique.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void anEmptyCatalogueIsRefused() {
        SplashArt.pick(0, 5, new Random(1L), -1);
    }
}
