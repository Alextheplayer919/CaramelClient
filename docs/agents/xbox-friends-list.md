# The Xbox friends list (the card and modal that never showed a friend)

## Symptom

The Friends card on Home always drew its empty state, and the Friends modal listed the empty-state
sentence as a row. Neither ever showed a real Xbox Live friend.

## Cause

The whole read path already existed and was unit-tested — `XboxPeopleHub` parses
`/users/xuid({xuid})/people` and `XboxFriendsClient` fetches it with the account's own
`http://xboxlive.com` XSTS token (which the Bedrock chain already mints, so no extra consent). What
was missing was the wiring: nothing called `XboxFriendsClient.fetch`, `CaramelLauncherState` had no
friends field, and both screens hard-coded their empty states.

## Fix

- `CaramelLauncherState.friends` (list) and `friendsStatus` (a "loading" line, an error reason, or
  empty when loaded) carry the data.
- `MainActivity.loadFriends()` mints the XSTS token off the UI thread (`MsftAuthManager.xboxLiveXsts`)
  and fetches through `XboxFriendsClient`; `refreshState()` triggers it once per signed-in account id,
  so a recompose or a tab switch does not refetch. A missing token or a PeopleHub error becomes a
  status line, never an empty list with no explanation. `onDestroy` closes the client.
- `CaramelFriendRow` (the real picture, gamertag, presence) is drawn by the Home card (up to four)
  and by the modal's searchable `LazyColumn`. The modal filters on the gamertag and shows a distinct
  "no matching friends" empty state when a search excludes everything.

## Do not regress

- `FriendsListWiringTest` pins that the activity calls the client with the account's XSTS token and
  that both screens render `CaramelFriendRow`; it also pins that the modal no longer draws the
  empty-state body as a list item.
- The friends fetch must stay off the UI thread and must not be retriggered on every recompose.
