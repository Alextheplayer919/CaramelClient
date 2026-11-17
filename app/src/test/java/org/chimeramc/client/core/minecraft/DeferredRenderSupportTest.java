package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Vibrant Visuals gate: the deferred pipeline must be offered only on a device that can
 * actually run it.
 *
 * <p>A tester saw "Vibrant Visuals" in the instance graphics dropdown on an Amazon tablet with no
 * support for it, which either meant the option was faked or that the client was tricking the game.
 * This test pins the rule that decides whether the option is shown, and the catalogue filter that
 * removes it from the list, so a device without a Vulkan driver never sees the mode.
 */
public class DeferredRenderSupportTest {

    private static DeferredRenderSupport.Facts facts(boolean vulkan, int major, int minor,
                                                     boolean software) {
        DeferredRenderSupport.Facts f = new DeferredRenderSupport.Facts();
        f.hasVulkan = vulkan;
        f.glEsVersion = (major << 16) | minor;
        f.softwareRenderer = software;
        return f;
    }

    @Test
    public void aVulkanDeviceSupportsTheDeferredPipeline() {
        assertTrue(DeferredRenderSupport.supported(facts(true, 3, 0, false)));
    }

    @Test
    public void aVulkanLessDeviceNeedsModernGles() {
        assertTrue("GLES 3.2 is enough without Vulkan",
                DeferredRenderSupport.supported(facts(false, 3, 2, false)));
        assertTrue("GLES 3.3 also carries it",
                DeferredRenderSupport.supported(facts(false, 3, 3, false)));
        assertTrue("GLES 4.0 carries it",
                DeferredRenderSupport.supported(facts(false, 4, 0, false)));
        assertFalse("GLES 3.1 is not enough",
                DeferredRenderSupport.supported(facts(false, 3, 1, false)));
        assertFalse("GLES 2.0 is not enough",
                DeferredRenderSupport.supported(facts(false, 2, 0, false)));
    }

    @Test
    public void aSoftwareRasteriserNeverSupportsIt() {
        assertFalse(DeferredRenderSupport.supported(facts(true, 3, 2, true)));
    }

    @Test
    public void anUnreadDeviceReadsAsUnsupported() {
        assertFalse(DeferredRenderSupport.supported(null));
        assertFalse("nothing read is not a capability", DeferredRenderSupport.supported(facts(false, 0, 0, false)));
    }

    @Test
    public void theEncodedVersionDecodes() {
        int encoded = (3 << 16) | 2;
        assertEquals(3, DeferredRenderSupport.glEsMajor(encoded));
        assertEquals(2, DeferredRenderSupport.glEsMinor(encoded));
    }

    @Test
    public void theCatalogueMarksVibrantVisualsAsDeferredOnly() {
        PerfFeatureCatalog.Feature mode = PerfFeatureCatalog.find("graphics_mode");
        assertNotNull(mode);
        int vibrant = -1;
        for (int i = 0; i < mode.values.length; i++) {
            if ("Vibrant Visuals".equals(mode.labels[i])) vibrant = i;
        }
        assertTrue("the catalogue must name a Vibrant Visuals option", vibrant >= 0);
        assertTrue("Vibrant Visuals needs the deferred pipeline",
                mode.optionNeedsDeferred(vibrant));
        assertFalse("Simple does not need the deferred pipeline", mode.optionNeedsDeferred(0));
    }

    @Test
    public void anUnsupportedDeviceIsNotOfferedVibrantVisuals() {
        PerfFeatureCatalog.Feature mode = PerfFeatureCatalog.find("graphics_mode");
        int[] offered = PerfFeatureCatalog.offeredChoiceIndices(mode, false);
        assertEquals("two of three options remain without the deferred pipeline", 2, offered.length);
        for (int index : offered) {
            assertFalse("the offered option must not need the deferred pipeline: "
                    + mode.labels[index], mode.optionNeedsDeferred(index));
        }
    }

    @Test
    public void aSupportedDeviceIsOfferedEveryOption() {
        PerfFeatureCatalog.Feature mode = PerfFeatureCatalog.find("graphics_mode");
        int[] offered = PerfFeatureCatalog.offeredChoiceIndices(mode, true);
        assertEquals(mode.values.length, offered.length);
    }

    @Test
    public void aFeatureWithoutDeferredOptionsOffersEveryValue() {
        PerfFeatureCatalog.Feature msaa = PerfFeatureCatalog.find("msaa");
        int[] offered = PerfFeatureCatalog.offeredChoiceIndices(msaa, false);
        assertEquals(msaa.values.length, offered.length);
    }
}
