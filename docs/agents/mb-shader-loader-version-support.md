## MB shader loader (mbl2 / libmtbinloader2) — verified version support

The "MB shader loader" / "better RenderDragon for Android" is **`mtbinloader2`** (mbl2), a Rust
library vendored into this app as the prebuilt `app/src/main/jniLibs/arm64-v8a/libmtbinloader2.so`
(banner string: `Starting, mbl2 version v0.1.12`). It is **not** a renderer — it hooks
`ResourcePackManager` and the AAsset functions to load materialbins, camerafiles and OreUI files,
and it "autofixes" shaders by signature.

### The bundled binary is a fork, and that is what made 26.50/26.60 inert

The shipped `.so` is **not** an upstream release. Upstream `mcbegamerxx954/mtbinloader2` builds at
4 KB page alignment and never exported `addCustomFile`; the bundled library is 16 KB page aligned
(required for the `NativePageAlignmentTest` gate) and does export `addCustomFile`. It was built
from **`PixelBoy78/mtbinloader2`** — a fork ("materialbinloader if it was good") whose default
branch is `master` — at commit **`c725799`** ("Update"). The fork is the only source with the
16 KB link flag, the `addCustomFile` JNI export, and the `persona/` + `vanilla_cameras/` resource
routing all in one tree.

At `c725799` the aarch64 `RPMC_PATTERNS` table held **only four** patterns: `26.40`, `1.21.120.4`,
`1.21.60.21`, `1.19.50-1.21.50`. It had **no `26.50` and no `26.60` pattern at all**. So on
26.50–26.60 — the exact builds this client targets — the loader installed, logged, found no
signature, and did nothing. The "MB shader loader" toggle was honest about loading a library but
the library could not hook those versions.

**Fix:** rebuild the fork at `1062315` ("Fix 1.26.60 pattern byte 9 and add wildcard fallback"),
which adds the `26.60` exact, `26.60` wildcard and `26.50` patterns (seven total), then apply a
fail-closed guard (below). `scripts/` has no build step for this; the rebuild recipe is recorded
here.

### Rebuilding the loader (no Rust toolchain is committed)

```bash
# 1. Rust + the Android target
curl -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal
rustup target add aarch64-linux-android

# 2. Clone the fork (this is the source of the bundled binary)
git clone https://github.com/PixelBoy78/mtbinloader2.git
cd mtbinloader2 && git checkout 1062315   # the commit that adds 26.50/26.60

# 3. Build with the NDK and the 16 KB page flag the app requires
NDK=/path/to/android-sdk/ndk/<ver>/toolchains/llvm/prebuilt/linux-x86_64
export CC_aarch64_linux_android=$NDK/bin/aarch64-linux-android24-clang
export CXX_aarch64_linux_android=$NDK/bin/aarch64-linux-android24-clang++
export AR_aarch64_linux_android=$NDK/bin/llvm-ar
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$CC_aarch64_linux_android
export RUSTFLAGS="-C link-arg=-Wl,-z,max-page-size=16384"
cargo build --profile githubci --target aarch64-linux-android
# -> target/aarch64-linux-android/githubci/libmtbinloader2.so
```

Copy that `.so` over `app/src/main/jniLibs/arm64-v8a/libmtbinloader2.so`. Two gates then check it:
`NativePageAlignmentTest` (16 KB `PT_LOAD`) and `MbShaderLoaderVersionSupportTest` (every aarch64
pattern must appear in the shipped bytes).

### Fail-closed on an unknown build

The crate is built with `panic = "abort"` and its `#[ctor]` runs inside the game's process. The
upstream `main()` used `.expect(...)` on both the maps lookup and the signature search, so an
unmatched build **aborted the game process** before the window appeared — a plausible cause of a
launch that "does not work". The bundled build now logs `mbl2 disabled: ...` and returns instead,
so an unsupported version degrades to "no shader fix" rather than a dead launch. The guard is in
`src/lib.rs::main()` in the fork.

### How it decides what it can do

It has no version table. It scans the mapped `libminecraftpe.so` for a **byte signature** (via
`tinypatscan`) and hooks whatever the first match is. Its aarch64 pattern list (`src/lib.rs`) is
labelled `26.60`, `26.60` (wildcard), `26.50`, `26.40`, `1.21.120.4`, `1.21.60.21`,
`1.19.50-1.21.50`. Support for a version is therefore *empirical*: the only question is whether a
pattern still matches that build's binary **and** whether that pattern is actually in the shipped
`.so`. Before the fix, the second half was false for 26.50/26.60.

### What was verified here (not assumed)

Each APK was downloaded from mcpe-planet, `libminecraftpe.so` was extracted, and the loader's own
patterns were scanned against the real bytes. A match means the loader *would* install on that
build — **provided the shipped `.so` carries that pattern**, which is what
`MbShaderLoaderVersionSupportTest` now pins.

| Version | ABIs in the APK | arm64 pattern result | Verdict |
|---|---|---|---|
| **26.60** | arm64-v8a | `26.60` exact pattern matches **1** (at `0xcf8a670`) | works **after the fix** |
| **26.52** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92b29c`) | works **after the fix** |
| **26.51** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92af44`) | works **after the fix** |
| **26.50** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92af4c`) | works **after the fix** |
| 26.45 | armeabi-v7a **only** | no arm64 binary exists; neither ARM32 pattern matches | see below |
| 26.44 | armeabi-v7a **only** | no arm64 binary exists | see below |
| 26.40 | armeabi-v7a **only** | no arm64 binary exists | see below |
| 26.41 | — | mcpe-planet has no such page (404) | version does not exist there |

Before the fix the shipped `.so` carried only `26.40`, `1.21.120.4`, `1.21.60.21` and
`1.19.50-1.21.50`, so the 26.50–26.60 rows above were the *pattern list's* capability, not the
shipped artifact's. The table is only trustworthy now because a test reads the shipped bytes.

The `26.60` *wildcard* pattern (the second entry) matches 11 places in the 26.50–26.52 binaries, so
it is only a fallback; the exact `26.60` pattern is what pins 26.60, and `26.50` pins 26.50–26.52.
That is why 26.51 and 26.52 work without the loader naming them.

### The 32-bit blocker (26.40, 26.44, 26.45)

Those three APKs ship **only** `lib/armeabi-v7a/libminecraftpe.so`; there is no arm64-v8a directory
at all. Two consequences:

1. This client is **arm64-only** (`abiFilters 'arm64-v8a'` in `app/build.gradle`), so a 32-bit-only
   game build cannot be launched by it in the first place. The loader is not the limiting factor.
2. Even on a 32-bit runtime, the loader's two ARM32 patterns do not match the 26.45 binary, so mbl2
   would not install there either.

**Conclusion:** for 26.40/26.44/26.45 there is nothing to fix in the loader — the client cannot run
those builds at all. The real answer to "make it work on 26.40" is "use the arm64 build of the game",
i.e. 26.50+, which the client already supports.

### If a future arm64 build breaks the loader

1. Extract `lib/arm64-v8a/libminecraftpe.so` from the APK.
2. Find the `ResourcePackManager` constructor by scanning for the current patterns; if none match,
   re-derive the pattern from the equivalent function in the new binary (the constructor's shape is
   stable: a stack-frame prologue followed by the `F4 03 02 AA F3 03 00 AA` tail).
3. Add the new pattern to the front of `RPMC_PATTERNS` in `src/lib.rs` and rebuild with
   `cargo build --release --target aarch64-linux-android` (Rust + NDK r25+).
4. Re-run the scan described above to confirm the new pattern matches **exactly once** — a pattern
   that matches many places will hook the wrong function.

The scan helper used here is `patscan.py` / `scan2.py` (byte + wildcard, anchored search); it needs
no NDK and no device, only the APK.
