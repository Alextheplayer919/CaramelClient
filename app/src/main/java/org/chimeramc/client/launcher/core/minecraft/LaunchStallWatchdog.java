package org.chimeramc.client.launcher.core.minecraft;

import android.content.Context;

import org.chimeramc.client.core.minecraft.MinecraftProcessRestarter;
import org.chimeramc.client.util.AppLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recovers from a launch that hangs on the game activity's main thread.
 *
 * <p>The loading screen's own watchdog ({@link LaunchWatchdog}) runs on the main thread, so it
 * cannot fire while that thread is blocked — which is exactly the failure the user reported: the
 * loading screen finished, the game activity opened, and the whole app froze with the nav bar gone
 * and no way out but a power cycle. This watchdog runs on its own daemon thread, so it survives a
 * blocked main thread, and its recovery action (kill the frozen process, reopen the launcher) is
 * performed by another process ({@code :crash}), so it does not depend on the frozen one either.
 *
 * <p>It is armed at the start of {@code MinecraftActivity.onCreate} — the window that opens the
 * moment the game process is in play — and settled on the game's first frame tick, which is proof
 * the engine's loop is running rather than blocked. A launch that never ticks within
 * {@link #BUDGET_MS} is treated as hung: the next launch is started with mods disabled (the
 * one-shot flag {@link org.chimeramc.client.core.mods.ModSafeMode} already provides), the cause is
 * recorded as {@link LaunchErrorDiagnosis.Cause#LAUNCH_STALLED} so the relaunched launcher can name
 * it, and the frozen process is torn down.
 *
 * <p>Deliberately armed and settled by explicit calls rather than by an activity lifecycle
 * callback: {@code onCreate} is the earliest point the freeze can begin, and a frame tick is the
 * strongest "not frozen" signal available without a device.
 */
public final class LaunchStallWatchdog {

    /** How long the game may go without a frame tick before it is treated as hung. */
    public static final long BUDGET_MS = 240_000L;

    /** How often the watchdog thread re-checks. Small enough to react promptly, cheap either way. */
    private static final long POLL_MS = 1_000L;

    private static final String TAG = "LaunchStallWatchdog";

    private static final AtomicBoolean armed = new AtomicBoolean(false);
    private static volatile boolean settled;
    private static volatile long armedAtMs;

    private LaunchStallWatchdog() {
    }

    /**
     * Arms the watchdog for the launch that is beginning.
     *
     * <p>Idempotent per process: a second arm while one is pending is ignored, so an activity
     * recreated mid-launch cannot start a second watchdog. The timer resets only after the first
     * one settles or fires, via {@link #disarm}.
     */
    public static void arm(Context context) {
        if (context == null) return;
        if (!armed.compareAndSet(false, true)) return;
        settled = false;
        armedAtMs = android.os.SystemClock.elapsedRealtime();
        final Context appContext = context.getApplicationContext();
        org.chimeramc.client.util.AppExecutors.thread("launch-stall-watchdog", () -> run(appContext));
    }

    /**
     * Marks the launch as alive; called from the game's frame tick.
     *
     * <p>A tick means the game loop is running, so the main thread is not blocked and the watchdog
     * has nothing to do. It does not disarm the slot — that happens on {@link #disarm} — so a later
     * stall in the same session is not silently unwatched.
     */
    public static void settle() {
        settled = true;
    }

    /**
     * Releases the watchdog so a future launch can arm it again.
     *
     * <p>Called when the game activity is destroyed. After this, {@link #arm} may start a fresh
     * watchdog for the next launch.
     */
    public static void disarm() {
        settled = true;
        armed.set(false);
    }

    /** True while the watchdog is armed and has not settled. For diagnostics and tests. */
    public static boolean isArmed() {
        return armed.get() && !settled;
    }

    /**
     * The pure decision: has the budget elapsed without the launch settling?
     *
     * <p>Kept separate from the thread so the boundary is unit-tested without a device or a real
     * clock. A negative elapsed (the clock moved backwards) is never a stall.
     */
    public static boolean shouldRecover(long armedAtMs, boolean settled, long nowMs,
                                        long budgetMs) {
        if (settled) return false;
        long elapsed = nowMs - armedAtMs;
        return elapsed >= budgetMs;
    }

    private static void run(Context appContext) {
        while (!settled) {
            long elapsed = android.os.SystemClock.elapsedRealtime() - armedAtMs;
            if (elapsed >= BUDGET_MS) {
                recover(appContext);
                return;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Tears down the frozen launch and reopens the launcher without mods.
     *
     * <p>Every step is best-effort and individually guarded: the whole point is to escape a stuck
     * process, so a failure in one step must not stop the rest. The process teardown goes through
     * {@link MinecraftProcessRestarter}, whose restart screen runs in its own {@code :crash}
     * process, so it works even though this process's main thread is blocked.
     */
    private static void recover(Context appContext) {
        if (settled) return;
        settled = true;
        AppLog.warn(TAG,
                "No frame tick within " + (BUDGET_MS / 1000) + "s; recovering from the frozen launch");
        try {
            org.chimeramc.client.core.mods.ModSafeMode.suppressModsForNextLaunch(appContext);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:suppress", "Could not suppress mods for the recovery launch", t);
        }
        try {
            LastLaunchFailure.write(appContext,
                    new IllegalStateException("Launch hung: the game window did not draw a frame within "
                            + (BUDGET_MS / 1000) + " seconds."),
                    false);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:report", "Could not record the launch-hang report", t);
        }
        try {
            MinecraftProcessRestarter.INSTANCE.restartLauncherAfterMinecraftExit(appContext);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:restart", "Could not restart the launcher after a hang", t);
        }
    }
}
