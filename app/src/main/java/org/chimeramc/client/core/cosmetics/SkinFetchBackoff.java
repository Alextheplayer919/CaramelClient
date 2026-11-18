package org.chimeramc.client.core.cosmetics;

/**
 * The wait between account-skin fetch attempts for one account, backing off while they fail.
 *
 * <p>The preview re-resolves the skin every time a fetch finishes: the service notified its
 * listeners unconditionally, the preview refreshed, the cache was still missing (the fetch had
 * failed), and that counted as stale, so it fetched again. On a failing lookup that is back-to-back
 * token and HTTP requests for as long as the preview is on screen, with no backoff. This gates the
 * attempts so a broken lookup costs one request a minute instead of one a frame.
 *
 * <p>Pure and clock-free apart from the caller-supplied timestamp, so the decision is unit-tested
 * without a device or a network.
 */
final class SkinFetchBackoff {

    private final CloudPollBackoff backoff = new CloudPollBackoff();
    private long nextAllowedAtMs;

    /** True when an attempt may start now. The first attempt for a fresh account always may. */
    boolean shouldAttempt(long nowMs) {
        return nowMs >= nextAllowedAtMs;
    }

    /** Records a fetch that produced a skin; the next attempt is allowed after the base delay. */
    void onSuccess(long nowMs) {
        backoff.onSuccess();
        nextAllowedAtMs = nowMs + backoff.nextDelayMs();
    }

    /** Records a fetch that produced nothing; the next attempt is allowed after a growing delay. */
    void onFailure(long nowMs) {
        backoff.onFailure();
        nextAllowedAtMs = nowMs + backoff.nextDelayMs();
    }

    /** How many consecutive failures have been recorded. For tests and diagnostics. */
    int consecutiveFailures() {
        return backoff.consecutiveFailures();
    }
}
