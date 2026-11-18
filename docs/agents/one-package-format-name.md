## One package format name (no `.AntEgg`, `.chimerapack` or `.levipack`)
- **`.AntEgg` is removed, along with `.chimerapack` and `.levipack`.** The launcher no longer
  accepts a named mod archive: an import is either a native `.so`, a bare `.zip` (the
  unpacked-layout import), or a `.universe` / `.singularity` package handled by the SweetLoader
  Rust engine. `FileHandler` accepts only those, the manifest lists only those filters, and
  `invalid_mod_file_reason` names only those in every locale.
- `grep -rn -i antegg app/src` must return nothing.
- **Accepting a name is not the same as running it.** `.universe` / `.singularity` are now in the
  import set and the manifest filters, and discovery lists them, but their runtime is not wired into
  the client yet, so the Mods row shows "Preview only" rather than a working toggle. See
  `docs/agents/engine-packages-universe-singularity-import-discovery-honest-availability.md`.
- Adding a format back means touching `FileHandler`, the manifest intent filters, every locale's
  string, and the JNI parity table. Do not re-add a name to one place only.
