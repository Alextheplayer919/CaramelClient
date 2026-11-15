# The Compose launcher's real-data screens (skin preview and cosmetic tiles)

## Symptom

Two Cosmetics-tab defects with the same shape — the screen drew a placeholder where real
art belongs:

- the 3D skin slot showed a white rounded square with a label instead of the player's
  character;
- every cosmetic tile showed the same generic coloured rectangle, so a cape, a hat and a
  pet were indistinguishable and a player could not see a cosmetic before equipping it.

## Cause

Both are leftovers from the migration to the Compose launcher (the Caramel UI pack's
`CaramelLauncherScreen`). The renderers existed and worked, but nothing hosted them:

- The 3D preview is `CapePreviewView`, a plain Android `View` (a software rasteriser since
  Phase D). The Compose host (`CaramelLauncherHost`) exposed a `skinViewerProvider` slot
  that defaulted to `EmptySkinSlot`, and no Kotlin caller ever set the provider — a
  `@Composable` lambda cannot be passed from the Java `MainActivity`, so the slot stayed
  empty.
- The cosmetic grid (`CaramelCosmeticsStore`) painted a `Box(Modifier.size(40.dp, 68.dp)
  .background(accent))`. The client ships `CapeThumbnail` (a cape's cloth) and
  `CosmeticThumbnail` (a hat, back piece or pet), both pure and unit-tested, but nothing
  called them.

## Fix

- `CaramelSkinPreview` (new, Kotlin) hosts `CapePreviewView` through `AndroidView` and binds
  the equipped cape (via `CosmeticStore.getEquippedCapeForDisplay()`, so the Optifine default
  is honoured exactly as at launch), accessory and pet. It rebinds only when the equipped set
  actually changes, and releases the view's bitmaps on `onRelease`. The launcher screen now
  defaults the slot to it; `skinViewerProvider` stays a Kotlin-only override.
- `CaramelCosmeticThumbnail` (new, Kotlin) renders each tile from its real catalogue entry:
  `CapeThumbnail.render` for a procedural cape, `CapeAtlas.sample` over the decoded authored
  PNG for an authored cape, and `CosmeticThumbnail.renderAccessory`/`renderPet` for a
  hat/back/pet — parsing the authored `.geo.json` through `BedrockGeometryParser` when the
  model id has one. Fail-closed: a missing asset paints nothing and the tile's gradient shows
  through. Bitmaps are recycled on dispose, so a scrolled grid does not leak.

## Do not regress

- The skin slot must host the real renderer, not `EmptySkinSlot`. `CaramelSkinPreviewWiringTest`
  pins the `AndroidView` + `CapePreviewView` bridge and that the launcher screen does not fall
  back to the empty slot.
- The grid tile must delegate to the shipped renderers. `CaramelCosmeticThumbnailWiringTest`
  pins the delegation to `CapeThumbnail`, `CapeAtlas`, `CosmeticThumbnail` and
  `BedrockGeometryParser`, and that the plain coloured swatch is gone.
