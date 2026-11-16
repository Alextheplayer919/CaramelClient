package org.chimeramc.client.launcher.core.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The splash show's timing rule, pinned without a device.
 *
 * <p>These are the rules the screen depends on and cannot check itself: the twenty seconds are real,
 * every slide is up for the same share of it, a cross-fade is a cross-fade, and the last slide never
 * fades into nothing.
 */
public class SplashShowTest {

    @Test
    public void theStandardShowIsTwentySecondsAcrossTheDefaultSlides() {
        SplashShow show = SplashShow.standard(SplashShow.DEFAULT_SLIDES);

        assertEquals(20_000L, show.totalMs());
        assertEquals(SplashShow.DEFAULT_SLIDES, show.slideCount());
        assertEquals(4_000L, show.slideMs());
    }

    @Test
    public void everySlideGetsTheSameShareOfTheShow() {
        SplashShow show = SplashShow.standard(5);

        assertEquals(0, show.indexAt(0L));
        assertEquals(0, show.indexAt(3_999L));
        assertEquals(1, show.indexAt(4_000L));
        assertEquals(2, show.indexAt(8_000L));
        assertEquals(4, show.indexAt(16_000L));
    }

    @Test
    public void theIndexIsClampedAtBothEnds() {
        SplashShow show = SplashShow.standard(5);

        assertEquals(0, show.indexAt(-5_000L));
        assertEquals(4, show.indexAt(20_000L));
        assertEquals(4, show.indexAt(90_000L));
    }

    @Test
    public void theCrossFadeRunsOverTheHandoverAndThenTheNextSlideOwns() {
        SplashShow show = SplashShow.standard(5);

        // Slide 0 hands over to slide 1 in the last 900ms before the 4s boundary; the moment the
        // boundary passes, slide 1 owns the frame and its own fade restarts from zero.
        assertEquals(0f, show.fadeAt(3_000L), 0.0001f);
        assertEquals(0.5f, show.fadeAt(3_550L), 0.02f);
        assertEquals(1f, show.fadeAt(3_999L), 0.01f);
        assertEquals(0f, show.fadeAt(4_000L), 0.0001f);
        assertEquals(1, show.incomingAt(3_550L));
        assertEquals(-1, show.incomingAt(3_000L));
    }

    @Test
    public void theLastSlideNeverFadesIntoNothing() {
        SplashShow show = SplashShow.standard(5);

        assertEquals(0f, show.fadeAt(19_500L), 0.0001f);
        assertEquals(-1, show.incomingAt(19_500L));
        assertTrue(show.finishedAt(20_000L));
        assertFalse(show.finishedAt(19_999L));
    }

    @Test
    public void progressRunsZeroToOneAndStops() {
        SplashShow show = SplashShow.standard(5);

        assertEquals(0f, show.progressAt(0L), 0.0001f);
        assertEquals(0.5f, show.progressAt(10_000L), 0.0001f);
        assertEquals(1f, show.progressAt(25_000L), 0.0001f);
    }

    @Test
    public void slideProgressRestartsAtEachSlide() {
        SplashShow show = SplashShow.standard(5);

        assertEquals(0f, show.slideProgressAt(0L), 0.0001f);
        assertEquals(0.5f, show.slideProgressAt(2_000L), 0.0001f);
        assertEquals(0f, show.slideProgressAt(4_000L), 0.0001f);
        assertEquals(0.5f, show.slideProgressAt(6_000L), 0.0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aFadeThatDoesNotFitItsSlideIsRefused() {
        SplashShow.of(5, 20_000L, 5_000L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aShowWithNoSlidesIsRefused() {
        SplashShow.of(0, 20_000L, 100L);
    }
}
