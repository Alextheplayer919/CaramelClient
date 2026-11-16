# The signed-in Xbox profile picture (the avatar slot that stayed a ring)

## Symptom

After signing in with a Microsoft account, the account menu, the account-settings modal and the
title-bar pill all showed a bare ring where the Xbox profile picture should be. The account name
synced; the picture did not.

## Cause

The URL was never missing. `MsftAuthManager.saveAccountOrThrow` reads `AppDisplayPicRaw` from the
Xbox profile and `MsftAccountStore.addOrUpdate` persists it as `MsftAccount.xboxAvatarUrl` (and
`MsftAccountStore.setActive` exports it). The Compose launcher simply never read it: `refreshState()`
copied `accountName` and `signedIn` but not the avatar, `CaramelLauncherState` had no field for it,
and the three avatar slots (`CaramelAvatar`) drew an empty ring — its doc comment even said "the
profile picture is drawn by the caller over this", but no caller did.

## Fix

- `CaramelLauncherState.accountAvatarUrl` holds the URL; `refreshState()` sets it from the active
  account (empty when signed out).
- `CaramelAvatarLoader` fetches a small HTTPS image with a bounded in-memory LRU plus a copy under
  `cacheDir/avatars`, so a picture is downloaded once. A non-http URL or a network failure returns
  null, and the caller keeps its placeholder — the slot is never blank.
- `CaramelAvatarPicture` draws the ring and the loaded picture over it; the account menu (44dp), the
  account-settings modal (88dp) and the title-bar pill (20dp) all use it.

## Do not regress

- `CaramelAvatarLoaderTest` pins the http(s)-only gating, the stable filesystem-safe cache name, and
  that the avatar composable loads through the loader.
- The pill must not go back to a static `AccountCircle` icon when signed in; it shows the real
  picture.
