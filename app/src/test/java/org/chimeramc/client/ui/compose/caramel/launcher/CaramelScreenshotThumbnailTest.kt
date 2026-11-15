package org.chimeramc.client.ui.compose.caramel.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The screenshot thumbnail downsamples before it decodes.
 *
 * <p>A captured screenshot is 1080p or larger; decoding it whole for a 118dp tile would allocate
 * several megabytes per tile and thrash the grid. [sampleSize] is the pure rule that picks the
 * power-of-two downsample so the longest edge lands at or above the target and never below.
 */
class CaramelScreenshotThumbnailTest {

    @Test
    fun aSmallImageIsNotDownsampled() {
        assertEquals(1, sampleSize(400, 300, 512))
    }

    @Test
    fun aLargeImageIsDownsampledToTheTarget() {
        // 1920 -> 480 is below 512, so the step stops at 960 (the 2x step).
        assertEquals(2, sampleSize(1920, 1080, 512))
        // 3840 -> 1920 -> 960 -> 480 (below), so 4x keeps 960 >= 512.
        assertEquals(4, sampleSize(3840, 2160, 512))
    }

    @Test
    fun theLongestEdgeDecidesTheSample() {
        // Portrait: the height is the long edge.
        assertEquals(2, sampleSize(1080, 1920, 512))
    }

    @Test
    fun nonsenseInputFallsBackToNoDownsample() {
        assertEquals(1, sampleSize(0, 100, 512))
        assertEquals(1, sampleSize(100, 100, 0))
    }
}
