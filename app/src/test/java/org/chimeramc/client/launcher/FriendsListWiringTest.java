package org.chimeramc.client.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;

/**
 * The Friends card and modal draw the account's real Xbox Live friends.
 *
 * <p>{@code XboxFriendsClient} and {@code XboxPeopleHub} existed and were unit-tested, but nothing
 * ever called them, so the card drew a fixed empty state and the modal drew the empty-state body as
 * a list item. These pin the wiring: the activity loads friends and the screens render rows.
 */
public class FriendsListWiringTest {

    private static String read(String relative) {
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            File f = new File(dir, relative);
            if (f.isFile()) {
                try {
                    return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("could not find " + relative);
    }

    @Test
    public void theActivityLoadsFriendsThroughTheRealClient() {
        String activity = read(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertTrue("the activity must call the Xbox friends client",
                activity.contains("friendsClient.fetch("));
        assertTrue("the friends read must use the account's XSTS token",
                activity.contains("MsftAuthManager.xboxLiveXsts("));
        assertTrue("a rebuild must not refetch while the same account is signed in",
                activity.contains("friendsAccountId"));
    }

    @Test
    public void theFriendsScreensRenderRows() {
        String home = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelHome.kt");
        assertTrue("the card must draw real friends", home.contains("CaramelFriendRow(friend)"));
        String modals = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelModals.kt");
        assertTrue("the modal must list real friends",
                modals.contains("items(visible) { friend -> CaramelFriendRow(friend) }"));
        assertFalse("the modal must not draw the empty-state body as a row",
                modals.contains("stringResource(R.string.caramel_no_friends_body),\n                            style = T.Body,\n                            modifier = Modifier.padding(start = 8.dp, top = 6.dp)"));
    }

    @Test
    public void aFriendWearsTheBadgeOnlyWhenConfirmedOnACaramelChannel() {
        String activity = read(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertTrue("a friend row's badge must come from the Caramel-user directory",
                activity.contains("CaramelUserDirectory.isCaramelUser(f.xuid)"));
        String labels = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelLabels.kt");
        assertTrue("the friend row must draw the Caramel chip only for a confirmed account",
                labels.contains("if (friend.caramel) CaramelUserChip()"));
        String models = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelModels.kt");
        assertTrue("CaramelFriendInfo must carry the caramel flag",
                models.contains("val caramel: Boolean = false"));
    }
}
