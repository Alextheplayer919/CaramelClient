package org.chimeramc.client.launcher.core.splash;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import org.chimeramc.client.R;
import org.chimeramc.client.util.AppLog;

/**
 * The short pop the splash plays as it hands over to the launcher.
 *
 * <p>{@link SoundPool} rather than a one-shot {@code MediaPlayer}: the clip is a fifth of a second
 * and is wanted at one instant, which is exactly what a pre-loaded sound pool is for. It is loaded
 * once at splash start, so the sample is resident before it is asked for and the pop lands on the
 * hand-over rather than a decode later.
 *
 * <p>Audio is best-effort. A device with no output, a codec that refuses the sample, or a pool that
 * fails to build must not take the launch down with it, so every failure is logged once and the
 * splash continues silently.
 */
public final class SplashSound {

    private static final String TAG = "SplashSound";

    private SoundPool pool;
    private int sampleId;
    private boolean loaded;

    /** Pre-loads the sample. Safe to call when audio is unavailable: nothing throws. */
    public void prepare(Context context) {
        if (pool != null) return;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(attributes)
                    .build();
            pool.setOnLoadCompleteListener((soundPool, id, status) -> loaded = status == 0);
            sampleId = pool.load(context.getApplicationContext(), R.raw.splash_pop, 1);
        } catch (Throwable error) {
            AppLog.warnOnce(TAG, "prepare", "the splash pop could not be prepared", error);
            release();
        }
    }

    /** Plays the pop once, if it is loaded. */
    public void play() {
        if (pool == null || !loaded) return;
        try {
            pool.play(sampleId, 1f, 1f, 1, 0, 1f);
        } catch (Throwable error) {
            AppLog.warnOnce(TAG, "play", "the splash pop could not be played", error);
        }
    }

    /** Frees the pool. Called when the splash goes away, so the sample does not outlive it. */
    public void release() {
        SoundPool current = pool;
        pool = null;
        loaded = false;
        sampleId = 0;
        if (current == null) return;
        try {
            current.release();
        } catch (Throwable error) {
            AppLog.warnOnce(TAG, "release", "the splash sound pool could not be released", error);
        }
    }
}
