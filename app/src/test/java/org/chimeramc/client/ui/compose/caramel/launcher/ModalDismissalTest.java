package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A menu closes from its own X (or the system back gesture), never from a tap outside it.
 *
 * <p>The defect this covers: the scrim behind a modal was the dismiss target, so the commonest thing
 * to do while reading a dialog — a stray tap anywhere on the screen — threw it away, and the user
 * had to find the button they were reading about again. The scrim still swallows the tap so it
 * cannot reach the screen behind; it just no longer closes anything.
 *
 * <p>The launch menu had the same problem through {@code Popup.onDismissRequest}, which fires for a
 * tap outside. It now ignores that callback and carries its own close button. These are the two
 * dismiss surfaces the launcher has, so a source check pins both without a device.
 */
public class ModalDismissalTest {

    @Test
    public void theModalScrimDoesNotDismiss() throws IOException {
        String glass = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/"
                + "CaramelGlass.kt");
        int modal = glass.indexOf("fun CaramelModal(");
        assertTrue("CaramelModal must exist", modal > 0);
        String body = glass.substring(modal, glass.indexOf("/** The pack's `Pill`", modal));
        assertTrue("the scrim must swallow the tap", body.contains(".clickable("));
        assertTrue("the modal still closes from the back gesture",
                body.contains("BackHandler(enabled = visible) { onDismiss() }"));
        // The only place onDismiss may be called from is the back gesture; a scrim click or an
        // outside-tap handler that calls it would re-introduce the bug.
        assertFalse("a tap on the scrim must not dismiss the modal",
                body.contains("clickable(remember { MutableInteractionSource() }, null) { onDismiss() }"));
    }

    @Test
    public void theLaunchMenuDoesNotDismissOnAnOutsideTap() throws IOException {
        String overlays = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelOverlays.kt");
        assertFalse("the launch menu must ignore the outside-tap callback",
                overlays.contains("onDismissRequest = { state.launchMenu = false }"));
        assertTrue("the launch menu closes from the back gesture",
                overlays.contains("BackHandler(enabled = state.launchMenu) { state.launchMenu = false }"));
        assertTrue("the launch menu carries its own close button",
                overlays.contains("{ state.launchMenu = false }, icon = Icons.Rounded.Close"));
    }

    private static String read(String relativePath) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }
}
