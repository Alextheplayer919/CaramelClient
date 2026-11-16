package org.chimeramc.client.launcher.core.splash;

/**
 * The cold-start show's timeline: which key art owns the frame, and how it hands over to the next.
 *
 * <p>Pure and Android-free, so the timing rule is unit-testable without a device — the Compose
 * screen reads {@link #indexAt}, {@link #incomingAt} and {@link #fadeAt} once per frame and does no
 * arithmetic of its own. The old splash navigated on a warm-up milestone with a fixed-length
 * fallback, which meant the art was on screen for however long the disk happened to take; the show
 * is now the clock, and the warm-up runs beside it.
 *
 * <p>The last slide never fades: there is nothing after it to fade into, so the show holds it until
 * {@link #finishedAt} is true.
 */
public final class SplashShow {

    /** How long the show runs, in total. */
    public static final long DEFAULT_TOTAL_MS = 20_000L;

    /** How long one slide takes to hand over to the next. */
    public static final long DEFAULT_FADE_MS = 900L;

    /** How many of the bundled key-art slides one launch shows. */
    public static final int DEFAULT_SLIDES = 5;

    private final int slides;
    private final long totalMs;
    private final long fadeMs;

    private SplashShow(int slides, long totalMs, long fadeMs) {
        this.slides = slides;
        this.totalMs = totalMs;
        this.fadeMs = fadeMs;
    }

    /**
     * A show of [slides] slides over [totalMs], each handing over with a [fadeMs] cross-fade.
     *
     * @throws IllegalArgumentException when the numbers cannot describe a show, rather than
     *     silently drawing something the caller did not ask for.
     */
    public static SplashShow of(int slides, long totalMs, long fadeMs) {
        if (slides < 1) {
            throw new IllegalArgumentException("a show needs at least one slide, got " + slides);
        }
        if (totalMs < slides) {
            throw new IllegalArgumentException(
                    "a show of " + slides + " slides needs at least " + slides + "ms, got " + totalMs);
        }
        long slideMs = totalMs / slides;
        if (fadeMs < 0 || fadeMs * 2 > slideMs) {
            throw new IllegalArgumentException(
                    "a " + fadeMs + "ms fade does not fit in a " + slideMs + "ms slide");
        }
        return new SplashShow(slides, totalMs, fadeMs);
    }

    /** The default 20-second show, cut into [slides] slides. */
    public static SplashShow standard(int slides) {
        return of(slides, DEFAULT_TOTAL_MS, DEFAULT_FADE_MS);
    }

    /** How many slides the show draws. */
    public int slideCount() {
        return slides;
    }

    /** The whole show's length. */
    public long totalMs() {
        return totalMs;
    }

    /** One slide's length. */
    public long slideMs() {
        return totalMs / slides;
    }

    /** The cross-fade's length. */
    public long fadeMs() {
        return fadeMs;
    }

    /**
     * The slide that owns the frame at [elapsedMs]. Clamped at both ends: before the show the first
     * slide is already up, and after it the last one holds.
     */
    public int indexAt(long elapsedMs) {
        if (elapsedMs <= 0L) return 0;
        if (elapsedMs >= totalMs) return slides - 1;
        int index = (int) (elapsedMs / slideMs());
        return Math.min(index, slides - 1);
    }

    /** The slide fading in at [elapsedMs], or -1 when the frame is not inside a cross-fade. */
    public int incomingAt(long elapsedMs) {
        int outgoing = indexAt(elapsedMs);
        if (fadeMs == 0L || outgoing + 1 >= slides) return -1;
        return fadeAt(elapsedMs) > 0f ? outgoing + 1 : -1;
    }

    /** How far the cross-fade into the next slide has run, 0..1; 0 outside a cross-fade. */
    public float fadeAt(long elapsedMs) {
        if (fadeMs == 0L) return 0f;
        int outgoing = indexAt(elapsedMs);
        if (outgoing + 1 >= slides) return 0f;
        long boundary = (long) (outgoing + 1) * slideMs();
        long from = boundary - fadeMs;
        if (elapsedMs <= from) return 0f;
        if (elapsedMs >= boundary) return 1f;
        return (float) (elapsedMs - from) / (float) fadeMs;
    }

    /** How far the show has run, 0..1. */
    public float progressAt(long elapsedMs) {
        if (elapsedMs <= 0L) return 0f;
        if (elapsedMs >= totalMs) return 1f;
        return (float) elapsedMs / (float) totalMs;
    }

    /**
     * How far the current slide has run, 0..1. Drives the slow push-in, so the still image is not
     * perfectly frozen for the seconds it is up.
     */
    public float slideProgressAt(long elapsedMs) {
        if (elapsedMs <= 0L) return 0f;
        if (elapsedMs >= totalMs) return 1f;
        long per = slideMs();
        long into = elapsedMs - (long) indexAt(elapsedMs) * per;
        return Math.min(1f, (float) into / (float) per);
    }

    /** Whether the show has run its length. */
    public boolean finishedAt(long elapsedMs) {
        return elapsedMs >= totalMs;
    }
}
