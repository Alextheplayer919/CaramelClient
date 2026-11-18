## Engine packages (`.universe` / `.singularity`): import, discovery, honest availability
- **The import set is `.so`, `.zip`, `.universe`, `.singularity`.** `ModPackageFormat`
  (`launcher.core.mods`) is the one place the extension rules live: `fromFileName` classifies a
  name, `fromManifestType` classifies a manifest `type`, and `isEnginePackage` says whether the
  package is handed to the SweetLoader Rust engines rather than the legacy `.so`/`.zip` loaders.
  Keep it the single source: `FileHandler`, `ModManager` discovery, the manifest filters and the
  row presentation all read it, so a format added in one place only is the bug this prevents.
- **An engine package is claimed before the legacy zip check.** A `.universe` file is also a zip,
  so `FileHandler.processIncomingFilesWithConfirmation` routes an engine name to
  `prepareEngineImport` *before* `isZipModPackageFile`, otherwise it would be validated as a legacy
  package (exactly one `.so` plus a manifest) and imported down the wrong path. The launcher copies
  the archive whole and writes a small launcher manifest; the Rust engine validates and extracts its
  own format, fail-closed (a build without the cdylib, or a package the engine refuses, stops the
  import with the engine's reason).
- **Discovery reads an engine manifest's `type`, and the entry must match the format it claims.**
  `ModManager.parseDirectoryMod` accepts `type` `universe`/`singularity` and, for those, requires the
  entry file's extension to agree (`ModPackageFormat.entryMatchesFormat`), so a directory whose
  entry was renamed to the other engine's extension is refused rather than listed under the wrong
  runtime. `ModRowPresentation.kindOf` reports `UNIVERSE`/`SINGULARITY` so the row is not "unknown".
- **The legacy native loader skips engine packages.** `ModNativeLoader` calls `System.load` on the
  entry path; a `.universe` archive there would fail with a confusing "not a valid ELF". It skips
  any entry that is not a native `.so` instead.
- **The row says "Preview only", and that is deliberate honesty.** Discovery, listing and signing of
  an engine package work, but the SweetLoader host table those engines are handed does not exist
  yet, so an enabled engine package cannot run. `ModRowPresentation.isRuntimeWired` is the single
  pure rule the UI reads (`false` for the engine kinds); the Mods card and the detail modal show a
  "Preview only" badge and a one-line note rather than a green "Enabled" that lies. Flip that method
  the day the host table ships; do not delete the badge while the runtime is unwired.
- **The loader budget is not preemption.** A running native call cannot be interrupted from another
  thread, so a mod that blocks inside `universe_tick` cannot be stopped mid-call. `universe-core`'s
  tick budget (2 ms per frame) only *measures* the tick and counts an overrun as a fault through the
  same tracker a panic uses, so a mod that overruns on every frame is disabled after three
  consecutive overruns; the first overrun still costs that frame. `universe_init` is not budgeted;
  it runs once, under the launcher's own bounded enable (`ModManager::enableLoadedMods`, Issue 4).
