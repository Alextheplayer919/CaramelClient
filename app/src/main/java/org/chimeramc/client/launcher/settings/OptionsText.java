package org.chimeramc.client.launcher.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * The line surgery for a Bedrock {@code options.txt}, as a pure string-to-string function.
 *
 * <p>The game owns this file's grammar: one {@code key:value} per line, a colon as the separator.
 * Reading and writing it lives here rather than in the file store so the parse and the update are
 * unit-testable without a device or a file system — a mistake in either would corrupt a player's
 * settings, which is the one thing this class must never do.
 *
 * <p>An update is surgical on purpose: it rewrites the one key's line and leaves every other byte
 * of the file alone, so a key the launcher does not understand (the game adds new ones each
 * release) survives a launcher write untouched.
 */
public final class OptionsText {

    private OptionsText() {
    }

    /** The value for [key], or null when the file has no such key. */
    public static String get(String text, String key) {
        if (text == null || key == null) return null;
        for (String line : text.split("\n", -1)) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            if (line.substring(0, colon).trim().equals(key)) {
                return line.substring(colon + 1).trim();
            }
        }
        return null;
    }

    /**
     * Returns [text] with [key] set to [value].
     *
     * <p>An existing line is replaced in place, so the file's own ordering is preserved; a missing
     * key is appended at the end. The trailing newline convention of the source is kept, so a file
     * the game wrote with a final newline does not lose it and one without does not gain a stray
     * blank line.
     */
    public static String set(String text, String key, String value) {
        if (key == null || value == null) return text;
        String source = text == null ? "" : text;
        boolean endsWithNewline = source.endsWith("\n");
        String body = endsWithNewline ? source.substring(0, source.length() - 1) : source;
        List<String> lines = new ArrayList<>();
        if (!body.isEmpty()) {
            for (String line : body.split("\n", -1)) {
                lines.add(line);
            }
        }
        String replacement = key + ":" + value;
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            if (line.substring(0, colon).trim().equals(key)) {
                lines.set(i, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lines.add(replacement);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(lines.get(i));
        }
        if (endsWithNewline) out.append('\n');
        return out.toString();
    }

    /** Whether [text] already carries [key]. */
    public static boolean has(String text, String key) {
        return get(text, key) != null;
    }
}
