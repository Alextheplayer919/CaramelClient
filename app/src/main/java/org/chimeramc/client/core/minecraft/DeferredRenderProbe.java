package org.chimeramc.client.core.minecraft;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Reads the device facts {@link DeferredRenderSupport} needs, on Android.
 *
 * <p>Kept apart from the pure rule so the decision stays unit-testable: this class only gathers the
 * readings (Vulkan presence, GLES version, whether the renderer is a software rasteriser) and hands
 * them to {@link DeferredRenderSupport#supported}. A reading that throws — an old ROM with no Vulkan
 * feature constant, a missing GLES version — reads as unsupported, which hides the Vibrant Visuals
 * option rather than advertising one the device cannot run.
 */
public final class DeferredRenderProbe {

    private static final String TAG = "DeferredRenderProbe";

    /** The feature string the platform declares when a Vulkan driver is present. */
    private static final String FEATURE_VULKAN_VERSION = "android.hardware.vulkan.version";

    private DeferredRenderProbe() {
    }

    /** The facts for this device. Never null. */
    public static DeferredRenderSupport.Facts facts(Context context) {
        DeferredRenderSupport.Facts facts = new DeferredRenderSupport.Facts();
        if (context == null) return facts;
        facts.hasVulkan = hasVulkan(context);
        facts.glEsVersion = glEsVersion(context);
        facts.softwareRenderer = isSoftwareRenderer();
        return facts;
    }

    /**
     * True when the only renderer the native probe could name is a software rasteriser
     * (SwiftShader, llvmpipe).
     *
     * <p>A Vulkan feature flag alone is not enough: an emulator or a device whose GPU is blocked
     * can advertise Vulkan while every draw runs on SwiftShader, where the deferred pipeline
     * cannot reach a usable rate. The native probe already classifies the vendor/renderer string
     * for the cosmetics quality path, so this reads the same reading rather than inventing a
     * second one. An unread family (or a missing native library) is not a software renderer.
     */
    private static boolean isSoftwareRenderer() {
        try {
            int code = org.chimeramc.client.preloader.PreloaderInput.gpuFamily();
            return org.chimeramc.client.core.cosmetics.GpuFamily
                    .isSoftware(org.chimeramc.client.core.cosmetics.GpuFamily.fromNativeCode(code));
        } catch (Throwable t) {
            Log.w(TAG, "GPU family could not be read; not treating as software", t);
            return false;
        }
    }

    /** True when the device can run the deferred pipeline. */
    public static boolean supported(Context context) {
        return DeferredRenderSupport.supported(facts(context));
    }

    private static boolean hasVulkan(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm != null && pm.hasSystemFeature(FEATURE_VULKAN_VERSION);
        } catch (Throwable t) {
            Log.w(TAG, "Vulkan feature could not be read; treating as absent", t);
            return false;
        }
    }

    private static int glEsVersion(Context context) {
        try {
            ActivityManager am =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return 0;
            ConfigurationInfo info = am.getDeviceConfigurationInfo();
            return info == null ? 0 : info.reqGlEsVersion;
        } catch (Throwable t) {
            Log.w(TAG, "GLES version could not be read; treating as absent", t);
            return 0;
        }
    }
}
