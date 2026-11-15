# The mods-folder browser (the "Open mods folder" button that did nothing)

## Symptom

"Open mods folder" in the Mods tab and the instance-settings Mods tab looked dead. Where it
did anything at all, the user only ever saw the mods the loader recognised — a folder with a
manifest, or a loose `.so` — and never the rest of the files that were actually in the folder.

## Cause

The action called `LocalFileOpener.openFolder(this, selected.modsDir)`, which builds an
`ACTION_VIEW` intent with a `resource/folder` type and hands it to whatever app handles it. A
bare device has no such app, so `startActivity` threw and the opener returned false — and the
call site ignored the return, so the press produced nothing at all. There was also no in-app
view of the folder: `ModManager.discoverMods()` only lists entries it can parse as a mod, so
anything else (a stray `.zip`, a readme, a `.so` with no manifest) was invisible.

## Fix

- `ModsFolderListing` (new, `launcher.core.mods`) lists a directory's real contents — directories
  first, then files, each sorted case-insensitively — as `Entry(name, path, directory, sizeBytes)`.
  It never throws: a missing or unreadable folder is an empty list. It also carries the two pure
  helpers the browser needs: `formatSize` (B / KB / MB / GB) and `relativePath` (the root reads as
  `/`, a child as its name relative to the root).
- `CaramelModsFolderModal` (new, Kotlin) draws that listing in-app: a breadcrumb, an up button that
  never steps above the mods root, a row per file or sub-directory, and an "Open in file manager"
  button for the rare device that has one.
- `MainActivity` now opens the browser (`openModsFolder`), descends into a directory
  (`openModsFolderEntry`), steps up (`openModsFolderUp`) and fills the state (`showModsFolder`). A
  file row opens an image inline; anything that is not an image falls back to the parent folder in
  a file manager, and reports `caramel_open_folder_failed` when even that is unavailable.
- `GlassButton` gained an `enabled` parameter (dimmed, non-clickable) so the up button can grey out
  at the root. It defaults to true, so no existing call site changed.

## Do not regress

- The button must list the folder in-app, not delegate to a file manager. `ModsFolderListingTest`
  pins that every entry is listed (not just recognised mods), that directories sort first, that a
  missing folder is an empty list, and the size/relative-path helpers.
- The browser must stay scoped to the instance's mods root: `openModsFolderUp` clamps at the root.
