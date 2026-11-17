package org.chimeramc.client.core.minecraft;

/**
 * Whether this device can actually run Bedrock's deferred pipeline (the one "Vibrant Visuals"
 * selects).
 *
 * <p>The instance Performance tab offered "Vibrant Visuals" on every device, including hardware that
 * cannot render it — a tester saw it on an Amazon tablet with no support. Offering a mode the GPU
 * cannot run is not a harmless extra option: the game silently falls back or draws nothing, so the
 * user believes they enabled something that never took effect. This class is the rule that decides
 * whether the option is shown at all, and it is pure so it can be pinned without a device.
 *
 * <p>The gate follows what the game itself requires. The deferred renderer needs a Vulkan driver on
 * Android; a device without Vulkan can only carry it on a modern GLES 3.2+ backend, and a software
 * rasteriser can never run it at a usable rate. Anything unread is treated as unsupported, which is
 * the honest failure: better to hide a mode we cannot confirm than to advertise one that does
 * nothing.
 */
public final class DeferredRenderSupport {

    /** The GLES version a Vulkan-less device needs to carry the deferred pipeline. */
    private static final int MIN_GLES_MAJOR = 3;
    private static final int MIN_GLES_MINOR = 2;

    private DeferredRenderSupport() {
    }

    /**
     * The device facts the decision reads. A missing reading is 0 / false, which reads as
     * unsupported rather than as a capability that was never confirmed.
     */
    public static final class Facts {
        /**
         * The Vulkan API version the platform reports, encoded {@code VK_MAKE_VERSION}
         * ({@code major<<22 | minor<<12 | patch}); 0 when there is no Vulkan driver.
         */
        public int vulkanVersion;
        /** The GLES version the platform reports, encoded as {@code major<<16 | minor}. */
        public int glEsVersion;
        /** True when the only renderer is a software rasteriser (SwiftShader, llvmpipe). */
        public boolean softwareRenderer;
    }

    /** The Vulkan major version a driver must report to carry the deferred pipeline. */
    private static final int MIN_VULKAN_MAJOR = 1;
    /** The Vulkan minor version a driver must report to carry the deferred pipeline. */
    private static final int MIN_VULKAN_MINOR = 1;

    /** Reads the major half of a {@code VK_MAKE_VERSION} value. */
    public static int vulkanMajor(int encoded) {
        return (encoded >> 22) & 0x3FF;
    }

    /** Reads the minor half of a {@code VK_MAKE_VERSION} value. */
    public static int vulkanMinor(int encoded) {
        return (encoded >> 12) & 0x3FF;
    }

    /** True when the reported Vulkan API version is at least 1.1. */
    public static boolean vulkanSupportsDeferred(int encoded) {
        int major = vulkanMajor(encoded);
        int minor = vulkanMinor(encoded);
        return major > MIN_VULKAN_MAJOR
                || (major == MIN_VULKAN_MAJOR && minor >= MIN_VULKAN_MINOR);
    }

    /** Reads the major half of an encoded GLES version ({@code major<<16 | minor}). */
    public static int glEsMajor(int encoded) {
        return (encoded >> 16) & 0xFFFF;
    }

    /** Reads the minor half of an encoded GLES version ({@code major<<16 | minor}). */
    public static int glEsMinor(int encoded) {
        return encoded & 0xFFFF;
    }

    /** True when the device can run the deferred pipeline. */
    public static boolean supported(Facts facts) {
        if (facts == null) return false;
        if (facts.softwareRenderer) return false;
        // A Vulkan driver carries the deferred pipeline only from API 1.1; a 1.0 driver is a weak
        // driver that would advertise the mode and then fail to run it, which is exactly the
        // "it showed on a device that cannot render it" report.
        if (vulkanSupportsDeferred(facts.vulkanVersion)) return true;
        int major = glEsMajor(facts.glEsVersion);
        int minor = glEsMinor(facts.glEsVersion);
        return major > MIN_GLES_MAJOR
                || (major == MIN_GLES_MAJOR && minor >= MIN_GLES_MINOR);
    }
}
