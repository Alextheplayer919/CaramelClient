package org.chimeramc.client.ui.compose.caramel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * One notice surface, everywhere.
 *
 * <p>The launcher's own toast slides in and out and honours the reduce-motion setting; the square
 * system bubble does neither. Screens outside the Compose tree (the in-game overlays, the logcat
 * dialog, the screenshot share sheet, the replay panel) could not reach the Compose toast and drew a
 * raw {@code android.widget.Toast} instead, so the same app showed two different notice styles
 * depending on where you were. They now post to {@link CaramelNoticeBus}, and the launcher installs
 * a sink on start.
 *
 * <p>This pins that: no shipped source raises a raw system toast, and the launcher both installs and
 * removes its sink. The bus itself is the single place a {@code Toast} is still allowed, as the
 * fallback for a process with no launcher tree.
 */
public class NoticeRoutingTest {

    private static final String BUS =
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelNoticeBus.kt";
    private static final String MAIN_ACTIVITY =
            "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java";

    @Test
    public void noShippedSourceRaisesARawSystemToast() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : shippedSources()) {
            String name = file.getFileName().toString();
            // The bus is the one fallback: it draws a system toast only when no launcher is mounted.
            if (name.equals("CaramelNoticeBus.kt")) continue;
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (source.contains("Toast.makeText")) {
                offenders.add(file.toString());
            }
        }
        assertTrue("every notice must route through CaramelNoticeBus, but these still raise a raw "
                + "system Toast: " + offenders, offenders.isEmpty());
    }

    @Test
    public void theLauncherInstallsAndRemovesItsNoticeSink() throws IOException {
        String activity = read(MAIN_ACTIVITY);
        assertTrue("the launcher must install its notice sink so the bus can reach its toast",
                activity.contains("CaramelNoticeBus.attach("));
        assertTrue("the launcher must remove its notice sink so a finished activity is not kept alive",
                activity.contains("CaramelNoticeBus.detach("));
    }

    @Test
    public void theBusFallsBackToASystemToastWhenNothingIsMounted() throws IOException {
        String bus = read(BUS);
        assertTrue("the bus must keep a fallback so a notice in a game-only process is not lost",
                bus.contains("Toast.makeText"));
        assertTrue("the fallback must be marshalled to the main thread",
                bus.contains("MainThread.post"));
    }

    private static List<Path> shippedSources() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String root : new String[]{"app/src/main/java", "app/src/main/kotlin"}) {
            Path base = projectRoot().resolve(root);
            if (!Files.isDirectory(base)) continue;
            try (Stream<Path> walk = Files.walk(base)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> {
                            String n = p.getFileName().toString();
                            return n.endsWith(".java") || n.endsWith(".kt");
                        })
                        .forEach(files::add);
            }
        }
        return files;
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("app/src/main/java");
            if (Files.exists(candidate)) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
