package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The account-skin fetch backoff.
 *
 * <p>The preview re-resolved the skin on every listener notification, so a failing lookup retried
 * back-to-back for as long as the preview was on screen. These pin that a failure now pushes the
 * next attempt out instead of allowing it immediately.
 */
public class SkinFetchBackoffTest {

    @Test
    public void theFirstAttemptIsAllowed() {
        SkinFetchBackoff backoff = new SkinFetchBackoff();
        assertTrue(backoff.shouldAttempt(1_000L));
    }

    @Test
    public void aFailureBlocksTheImmediateRetry() {
        SkinFetchBackoff backoff = new SkinFetchBackoff();
        long now = 1_000L;
        assertTrue(backoff.shouldAttempt(now));
        backoff.onFailure(now);
        assertFalse("the retry the preview would trigger must be blocked",
                backoff.shouldAttempt(now));
        assertFalse("and still blocked a moment later", backoff.shouldAttempt(now + 500L));
    }

    @Test
    public void repeatedFailuresGrowTheWait() {
        SkinFetchBackoff backoff = new SkinFetchBackoff();
        long now = 0L;
        backoff.onFailure(now);
        long first = backoff.consecutiveFailures();
        backoff.onFailure(now);
        long afterTwo = backoff.consecutiveFailures();
        assertTrue("the failure count must grow", afterTwo > first);
        // The second failure's wait must be longer than the first's, which is the point of backoff.
        assertFalse(backoff.shouldAttempt(now + CloudPollBackoff.BASE_DELAY_MS));
    }

    @Test
    public void aSuccessRestoresTheBaseDelay() {
        SkinFetchBackoff backoff = new SkinFetchBackoff();
        backoff.onFailure(0L);
        backoff.onFailure(0L);
        backoff.onSuccess(0L);
        assertTrue("after a success the next attempt is allowed again",
                backoff.shouldAttempt(CloudPollBackoff.BASE_DELAY_MS));
        assertTrue("and the failure streak is reset", backoff.consecutiveFailures() == 0);
    }
}
