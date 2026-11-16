package org.chimeramc.client.launcher.core.about;

import org.chimeramc.client.R;

/**
 * The fixed facts the About popup states: when the client was made, where its channel is, where its
 * source lives, and the stories it tells about itself.
 *
 * <p>These are claims about the project, so they live in one place and are unit-tested rather than
 * being typed into the layout. The date is the repository's first commit — the day the client
 * started — and the channel is the client's own channel, which is not the same link as the owner's
 * personal one.
 *
 * <p>The stories are resource ids rather than prose: the screen resolves each one with
 * {@code stringResource}, so they are translated like any other text and this class holds no
 * display text at all.
 */
public final class AboutFacts {

    /** The day the client started: the repository's first commit. */
    public static final String MADE_ON = "2026-09-04";

    /** The client's own YouTube channel. */
    public static final String YOUTUBE_URL = "https://m.youtube.com/@CaramelClientt";

    /** The client's repository, opened by the About screen's source link. */
    public static final String REPOSITORY_URL = "https://github.com/ChimeraAnt-DEV/CaramelClient";

    private static final int[] FUN_FACT_IDS = {
            R.string.caramel_about_fact_levimc,
            R.string.caramel_about_fact_hacks,
            R.string.caramel_about_fact_broken,
            R.string.caramel_about_fact_best,
    };

    private AboutFacts() {
    }

    /** The fun facts, in the order the screen shows them. */
    public static int[] funFactResIds() {
        return FUN_FACT_IDS.clone();
    }
}
