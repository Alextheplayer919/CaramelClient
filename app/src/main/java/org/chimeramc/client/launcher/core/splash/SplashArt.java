package org.chimeramc.client.launcher.core.splash;

import java.util.Random;

/**
 * Chooses which key art one launch of the splash shows.
 *
 * <p>Pure and Android-free: the catalogue is a size and the result is indices, so the selection rule
 * is unit-testable without a device. The rule exists because "show one random picture" and "show
 * five, and never open on the same one twice in a row" are different promises, and only the second
 * one keeps the splash from looking like a single static image.
 */
public final class SplashArt {

    private SplashArt() {}

    /**
     * Picks [wanted] distinct indices out of a catalogue of [catalogueSize], in the order they are
     * to be shown.
     *
     * <p>[avoidFirst] is the slide the previous launch opened on, or -1. The first pick is not that
     * slide, so two launches in a row never open on the same art. Fewer than [wanted] indices come
     * back when the catalogue is smaller than the request, and a single-entry catalogue is returned
     * as-is rather than throwing — a one-art build still opens.
     */
    public static int[] pick(int catalogueSize, int wanted, Random random, int avoidFirst) {
        if (catalogueSize < 1) {
            throw new IllegalArgumentException("the catalogue is empty");
        }
        if (wanted < 1) {
            throw new IllegalArgumentException("wanted must be positive, got " + wanted);
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        int[] pool = new int[catalogueSize];
        for (int i = 0; i < catalogueSize; i++) pool[i] = i;
        // Fisher-Yates over the whole catalogue: taking the first `wanted` of a full shuffle is
        // what makes the selection uniform, rather than a shuffle of a short prefix.
        for (int i = pool.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = pool[i];
            pool[i] = pool[j];
            pool[j] = swap;
        }
        if (avoidFirst >= 0 && avoidFirst < catalogueSize && pool[0] == avoidFirst && pool.length > 1) {
            int swap = pool[0];
            pool[0] = pool[1];
            pool[1] = swap;
        }
        int take = Math.min(wanted, catalogueSize);
        int[] out = new int[take];
        System.arraycopy(pool, 0, out, 0, take);
        return out;
    }
}
