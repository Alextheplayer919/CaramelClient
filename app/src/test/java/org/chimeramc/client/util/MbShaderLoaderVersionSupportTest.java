package org.chimeramc.client.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The bundled MB shader loader must be able to hook {@code ResourcePackManager} on every Bedrock
 * build this client can launch.
 *
 * <p>The loader ({@code libmtbinloader2.so}) is a prebuilt with no source in this repository, so the
 * only device-free way to check its coverage is to read its compiled-in signature table out of the
 * shipped bytes. It installs by scanning the mapped {@code libminecraftpe.so} for a byte signature;
 * if no pattern matches, it logs "Cannot find signature in any region" and does nothing. The loader
 * shipped before the 26.50/26.60 patterns existed, so on those builds it was advertised but inert.
 *
 * <p>{@code tinypatscan::Pattern::from_str} stores a wildcard ({@code ??}) as the byte {@code 0x00}
 * and, because of an off-by-one in its compile-time parser, leaves the final byte out of the stored
 * length. This test reproduces both quirks, then asserts each aarch64 pattern is present in the
 * library. It fails if the library is ever replaced with one that drops the newest patterns.
 */
public class MbShaderLoaderVersionSupportTest {

    /** The ABI the APK ships, so the only directory that carries a pattern table. */
    private static final Path JNI_LIBS = Paths.get("src", "main", "jniLibs", "arm64-v8a");

    /**
     * The aarch64 {@code RPMC_PATTERNS} table, in the order the loader tries them. Copied verbatim
     * from the loader's source; the order does not matter to this test, only that each is present.
     */
    private static final String[] AARCH64_PATTERNS = {
            // 26.60
            "FF C3 07 D1 FD 7B 1A A9 FC 67 1B A9 F8 5F 1C A9 F6 57 1D A9 F4 4F 1E A9 FD 83 06 91 58 D0 3B D5 F4 03 02 AA F3 03 00 AA",
            // v26.60 (wildcard)
            "?? ?? ?? D1 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? 91 ?? ?? ?? D5 F4 03 02 AA F3 03 00 AA",
            // v26.50
            "?? ?? ?? D1 ?? ?? ?? A9 ?? ?? ?? F9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? 91 ?? ?? ?? D5 F6 03 03 2A F5 03 02 AA ?? ?? ?? F9 F3 03 00 AA",
            // v26.40
            "?? ?? ?? D1 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? A9 ?? ?? ?? 91 ?? ?? ?? D5 F6 03 03 2A F5 03 02 AA ?? ?? ?? F9 F3 03 00 AA",
            // 1.21.120.4
            "FF ?? 02 D1 FD 7B ?? A9 ?? ?? ?? ?? FA 67 ?? A9 F8 5F ?? A9 F6 57 ?? A9 F4 4F ?? A9 FD ?? 01 91 ?? D0 3B D5 ?? 03 03 2A ?? 03 02 AA ?? 17 40 F9 F3 03 00 AA A8 83 1F F8",
            // V1.21.60.21
            "FF 83 02 D1 FD 7B 06 A9 FD 83 01 91 F8 5F 07 A9 F6 57 08 A9 F4 4F 09 A9 58 D0 3B D5 F6 03 03 2A 08 17 40 F9 F5 03 02 AA F3 03 00 AA A8 83 1F F8 28 10 40 F9 28 01 00 B4",
            // V1.19.50-1.21.50
            "FF 03 03 D1 FD 7B 07 A9 FD C3 01 91 F9 43 00 F9 F8 5F 09 A9 F6 57 0A A9 F4 4F 0B A9 59 D0 3B D5 F6 03 03 2A 28 17 40 F9 F5 03 02 AA F3 03 00 AA A8 83 1F F8 28 10 40 F9",
    };

    @Test
    public void theLoaderKnowsEverySupportedBedrockVersionPattern() throws IOException {
        Path lib = findDir(JNI_LIBS).resolve("libmtbinloader2.so");
        assertTrue("the bundled MB shader loader is missing: " + lib, Files.isRegularFile(lib));
        byte[] image = Files.readAllBytes(lib);

        List<String> missing = new ArrayList<>();
        for (String pattern : AARCH64_PATTERNS) {
            if (indexOf(image, storedBytes(pattern)) < 0) {
                missing.add(pattern);
            }
        }
        assertTrue(
                "the bundled MB shader loader is missing these aarch64 patterns, so it cannot hook"
                        + " ResourcePackManager on the Bedrock builds they pin (it will log \"Cannot"
                        + " find signature in any region\" and never install):\n  "
                        + String.join("\n  ", missing),
                missing.isEmpty());
    }

    /** The bytes the loader's compile-time parser stores for a pattern: wildcard -> 0x00, last byte
     *  dropped by the parser's off-by-one. */
    private static byte[] storedBytes(String pattern) {
        String[] tokens = pattern.trim().split("\\s+");
        byte[] full = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            full[i] = "??".equals(tokens[i]) || "?".equals(tokens[i])
                    ? (byte) 0
                    : (byte) Integer.parseInt(tokens[i], 16);
        }
        return Arrays.copyOf(full, full.length - 1);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) return -1;
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static Path findDir(Path relative) {
        if (Files.isDirectory(relative)) return relative;
        Path candidate = Paths.get("app").resolve(relative);
        if (Files.isDirectory(candidate)) return candidate;
        candidate = Paths.get("..").resolve(relative);
        if (Files.isDirectory(candidate)) return candidate;
        throw new IllegalStateException("cannot locate " + relative + " from "
                + Paths.get("").toAbsolutePath());
    }
}
