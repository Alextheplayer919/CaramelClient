# The white screen after the launch screen (the game window's theme)

## Symptom

Launching Minecraft showed the launch screen correctly, then a plain white screen
instead of the game.

## Cause

`MinecraftActivity` (the game activity, `app/src/main/AndroidManifest.xml`) was themed
`AppFullScreenTheme` in `app/src/main/res/values/styles.xml`. That style:

- parented `Theme.AppCompat.NoActionBar`, which is **not** a DayNight theme, so
  `@color/background` resolved to the light near-white `#F7F5FF` in every mode;
- set `android:windowBackground` and `android:colorBackground` to that near-white;
- set `android:windowDisablePreview="false"`, so the system painted that near-white as the
  activity's starting window.

`MinecraftActivity.onCreate` also paints its own window from `colorBackground`
(`resolveLaunchBackgroundColor()`), so both the framework starting window and the in-code
override were the near-white. The game draws its own (dark) GL frame, so the white showed
only during the handoff and read as "white screen".

The launcher had already been fixed for exactly this class of bug: `LauncherTheme` sets
`windowBackground`/`windowSplashScreenBackground` to `@color/launcher_backdrop` (the Caramel
navy `#060B1A`) with `windowDisablePreview="true"`. The game window was missed.

## Fix

`AppFullScreenTheme.Base` (new) carries the window attributes with the Caramel navy for both
`windowBackground` and `colorBackground`, `windowDisablePreview="true"`, and
`windowLightStatusBar="false"`. `AppFullScreenTheme` extends the base, and the API 31+ variants
(`values-v31/theme.xml`, `values-v35/theme.xml`) extend it again to set the system splash to the
navy, so neither the framework starting window nor the API 31+ system splash flashes white.

## Do not regress

- The game window must stay dark on **both** attributes. `MinecraftActivity` reads
  `colorBackground` at runtime, so fixing only `windowBackground` leaves the white frame behind.
- `GameWindowThemeTest` pins all of this against the real resource files: the navy on both
  attributes, `windowDisablePreview="true"`, the base/extending split, the manifest theme, the
  API 31/35 splash, and that `launcher_backdrop` is actually dark by WCAG luminance.
