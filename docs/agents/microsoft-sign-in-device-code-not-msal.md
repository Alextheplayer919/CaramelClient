## Microsoft sign-in: device code, not MSAL

The "Sign in with Microsoft" button runs a **device-code** grant, and the code, the pre-filled
link, the progress and the failure message all live in the add-account modal. Anything that hides
one of those turns the button back into one that appears to do nothing.

### Why MSAL was removed, not fixed

`com.microsoft.identity.client:msal` was in the build and `MicrosoftOAuthManager` wrapped it, but
the class was unreferenced and the flow **cannot authenticate this app's client id**. The id is
`MsaConstants.BEDROCK_ANDROID_TITLE_ID` (`0000000048183522`), an MSA *title* id, not an Azure app
registration. Verified against the live endpoints:

- `login.microsoftonline.com/consumers/oauth2/v2.0/authorize` (and `/devicecode`) answer
  `AADSTS700016: Application with identifier '0000000048183522' was not found in the directory`.
  MSAL only talks to this Azure/consumer endpoint, so its interactive flow is rejected before a
  browser opens.
- `login.live.com/oauth20_authorize.srf` (the MSA endpoint that *does* know the title id) rejects
  the `msauth://<package>/<hash>` redirect with `invalid_request: The provided value for the input
  parameter 'redirect_uri' is not valid` — the title id has only
  `https://login.live.com/oauth20_desktop.srf` registered.

So the removed pieces (the `msal` dependency, `MicrosoftOAuthManager`, `MsalRedirectRule`, the
`BrowserTabActivity` manifest filter and its `msauth` `<queries>`, the `getMsalSignatureHash()`
Gradle helper and `BuildConfig.MSAL_SIGNATURE_HASH`, and the proguard keeps) were scaffolding for a
path that could never complete. `SignInFlowTest` fails if the dependency or the activity returns.

### The device-code flow

`MainActivity.startSignIn()` → `MsftAuthManager.loginWithDeviceCode(...)` →
`BedrockAuthManager` (XBL → XSTS → Minecraft certificate) → `saveAccountAndActivate`. The three
things the old version dropped, each enough on its own to make the press look dead:

- `setDeviceCode(code.getUserCode())` — the code must reach the modal.
- `setDeviceCodeUrl(code.getDirectVerificationUri())` and `UrlOpener.open(...)` — the direct URI is
  `verification_uri?otc=<code>`, so the browser lands with the code already filled in. The modal
  keeps a button to reopen it and one to copy the code.
- `MsftAuthManager.describeError(t)` into `setSignInError(...)` — every failure used to be logged
  and discarded, so the modal closed with no account and no message.

`SignInFlowTest` scans `startSignIn()`'s body and pins all four tokens, so a change that drops one
fails here rather than on a device.

### Limits

There is no emulator in this container, so the flow is verified by compilation, the unit suite and
the token scan above; the live HTTP behaviour of the grant itself is verified by the endpoint probes
recorded here, not by running the app.
