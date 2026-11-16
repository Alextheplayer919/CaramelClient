package org.chimeramc.client.launcher.core.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * The bundled key-art catalogue.
 *
 * <p>The splash promises "a different picture every launch" and the brief asks for at least
 * seventeen, so both are pinned: a catalogue that quietly shrank to one image would still render,
 * and nothing else in the build would notice.
 */
public class SplashArtCatalogTest {

    @Test
    public void bundlesAtLeastSeventeenPictures() {
        assertTrue("the catalogue has " + SplashArtCatalog.size() + " pictures, wanted at least 17",
                SplashArtCatalog.size() >= 17);
    }

    @Test
    public void everyEntryIsADistinctDrawable() {
        int[] all = SplashArtCatalog.all();
        Set<Integer> unique = new HashSet<>();
        for (int id : all) {
            assertTrue("a drawable id", id != 0);
            unique.add(id);
        }
        assertEquals("no duplicated art", all.length, unique.size());
    }

    @Test
    public void anOutOfRangeIndexWrapsInsteadOfCrashing() {
        int first = SplashArtCatalog.drawableAt(0);

        assertEquals(first, SplashArtCatalog.drawableAt(SplashArtCatalog.size()));
        assertEquals(first, SplashArtCatalog.drawableAt(2 * SplashArtCatalog.size()));
        assertNotEquals(first, SplashArtCatalog.drawableAt(1));
    }

    @Test
    public void allReturnsACopySoCallersCannotReorderTheCatalogue() {
        int[] first = SplashArtCatalog.all();
        first[0] = 0;

        assertNotEquals("the catalogue was mutated through the returned array",
                0, SplashArtCatalog.all()[0]);
    }
}
