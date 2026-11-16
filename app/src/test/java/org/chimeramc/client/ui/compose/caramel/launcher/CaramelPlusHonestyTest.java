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
 * The Get Caramel+ button is drawn unavailable and explains why when tapped.
 *
 * <p>The defect this covers: the button looked live and did nothing when pressed. There is no
 * payment method attached and no payment system behind it, so the honest rendering is a greyed
 * button that still answers a tap with a notice saying exactly that — not a control that swallows
 * the press. This pins the greyed tone, the warning notice, and the one string both buttons share.
 */
public class CaramelPlusHonestyTest {

    @Test
    public void bothButtonsAreGreyedAndRaiseTheSameNotice() throws IOException {
        String home = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelHome.kt");
        String store = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelCosmeticsStore.kt");
        for (String source : new String[]{home, store}) {
            assertTrue("the button must be drawn muted", source.contains("muted = true"));
            assertTrue("a tap must raise a warning notice",
                    source.contains("CaramelNotice(noPayment, warning = true)"));
            assertTrue("the notice text comes from the shared string",
                    source.contains("caramel_plus_no_payment"));
            assertFalse("an empty click handler is the bug", source.contains("caramel_get_plus), {}"));
        }
    }

    @Test
    public void theNoticeStringSaysWhatActuallyHappened() throws IOException {
        String strings = read("app/src/main/res/values/strings.xml");
        int at = strings.indexOf("name=\"caramel_plus_no_payment\"");
        assertTrue("the string must exist", at >= 0);
        String value = strings.substring(at, strings.indexOf("</string>", at));
        assertTrue("it must say a payment was attempted", value.contains("tried to pay for Caramel+"));
        assertTrue("it must say no payment system is set up",
                value.contains("set up a payment system yet"));
    }

    @Test
    public void everyLocaleCarriesTheNotice() throws IOException {
        for (String locale : new String[]{"es", "fr", "hi", "id", "ja", "pt", "ru", "tr", "vi",
                "zh-rCN"}) {
            String strings = read("app/src/main/res/values-" + locale + "/strings.xml");
            assertTrue("the notice must be translated for " + locale,
                    strings.contains("name=\"caramel_plus_no_payment\""));
        }
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
