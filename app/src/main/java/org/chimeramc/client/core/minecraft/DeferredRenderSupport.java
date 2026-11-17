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
        /** True when the platform reports a Vulkan driver ({@code android.hardware.vulkan.version}). */
        public boolean hasVulkan;
        /** The GLES version the platform reports, encoded as {@code major<<16 | minor}. */
        public int glEsVersion;
        /** True when the only renderer is a software rasteriser (SwiftShader, llvmpipe). */
        public boolean softwareRenderer;
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
        if (facts.hasVulkan) return true;
        int major = glEsMajor(facts.glEsVersion);
        int minor = glEsMinor(facts.glEsVersion);
        return major > MIN_GLES_MAJOR
                || (major == MIN_GLES_MAJOR && minor >= MIN_GLES_MINOR);
    }
}
