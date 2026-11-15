# The Screenshots entry points (the "Content" button and the blank Library)

## Symptom

The Home hero's camera button (labelled "Content") led to a plain white screen. The Screenshots
modal existed and worked, but nothing in the Caramel launcher opened it, and the modal's own
"Open folder" buttons did nothing.

## Cause

Two dead ends, both leftovers of the legacy-UI removal:

- `MainActivity.openContent()` started `LibraryActivity`, which is a documented placeholder — it
  extends `BaseActivity` and `onCreate` only calls `super`, so it draws nothing. Under the default
  (non-DayNight) window theme that is the near-white `#F7F5FF` background, i.e. the white screen.
  `CaramelLauncherScreen` routed the Home camera button to `actions::openContent`.
- The Screenshots modal's "Open folder" buttons called `actions::openScreenshots`, whose only job is
  to set `state.screenshotsModal = true` — re-opening the modal the button was already in. The
  `LocalFileOpener.openFolder` helper was never called from the launcher.

The modal also drew a tinted gradient per tile rather than the screenshot, so a player could not
tell one shot from another.

## Fix

- The Home camera button is labelled `caramel_screenshots_title` ("Screenshots") and opens the
  Screenshots modal (`actions::openScreenshots`). The dead `openContent` action is deleted from
  `CaramelLauncherHost.Actions` and `MainActivity` (the legacy `IntentHandler` /
  `MinecraftNotFoundDialog` paths that target `LibraryActivity` are untouched).
- `openScreenshotsFolder()` is a new action that hands the selected instance's real `Screenshots`
  folder to `LocalFileOpener.openFolder`, with a named toast
  (`caramel_open_folder_failed`, translated in all 10 locales) when no file manager exists.
- `CaramelScreenshotThumbnail` decodes each captured file at a bounded size off the main thread and
  draws it, falling back to the file name when the file has gone. `sampleSize` is the pure
  downsample rule (unit-tested).
- `LibraryActivity` now carries `@style/LauncherTheme` in the manifest, so even the legacy deep-link
  path can no longer paint white.

## Do not regress

- `ScreenshotEntryPointsTest` pins the Home button → `openScreenshots` route, that `openContent` no
  longer starts the blank `LibraryActivity`, that the folder action opens the real
  `Screenshots` directory, and that the modal's buttons call `openScreenshotsFolder`.
- `CaramelScreenshotThumbnailTest` pins the downsample rule.
