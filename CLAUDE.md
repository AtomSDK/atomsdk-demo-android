# CLAUDE.md

Guidance for Claude Code working in this repository.

## What this project is

`atomsdk-demo-android` is the **public reference/demo app for the ATOM VPN SDK** (PureVPN). It is not a product app — its job is to show integrators the smallest correct way to call the SDK, and `README.md` is the customer-facing integration guide that ships alongside it. Treat the demo code and the README as one deliverable: a change to how the SDK is used almost always needs a matching README section, and README sections must stay copy-pasteable.

The SDK itself lives elsewhere and is consumed as a binary artifact:

```groovy
implementation 'org.bitbucket.purevpn:purevpn-sdk-android:7.2.0'   // app/build.gradle
```

### Reading the real SDK API

SDK sources aren't here, but the compiled API is, and **API claims in the README should be verified against it rather than trusted**. The Maven artifact in the Gradle cache is a 277-byte stub (`META-INF` only) — the real classes live in two AARs pulled in transitively:

```bash
find ~/.gradle/caches/modules-2 -name "AtomSdk-*.aar"       # com.atom.sdk.android.*
find ~/.gradle/caches/modules-2 -name "android-core-*.aar"  # com.atom.core.*

# extract into the scratchpad, never into the repo
unzip -q AtomSdk-7.2.0.aar -d atom && cd atom && mkdir cls && cd cls && unzip -q ../classes.jar

javap -cp . com.atom.sdk.android.VPNStateListener
javap -cp . 'com.atom.sdk.android.VPNProperties$Builder'
javap -cp . -constants com.atom.sdk.android.Errors | grep _51
javap -v -cp . com.atom.sdk.android.VPNStateListener | awk '/void on/{m=$0} /Deprecated: true/{print m}'
```

Older versions are usually cached too (e.g. `AtomSdk-7.1.1.aar`), which is how to check when a symbol was added or removed. `annotations.zip` inside the AAR holds the `StringDef`/`@Nullable` metadata that `javap` doesn't show.

## Build & run

Single Gradle module (`:app`), Groovy DSL, Gradle wrapper 8.14.3, AGP 8.11.2, Kotlin 2.1.20, Java 17, `minSdk 24` / `compileSdk`+`targetSdk` 36.

```bash
./gradlew :app:assembleDebug --offline    # works once deps are cached; verified green
./gradlew installDebug                    # build + install on a connected device
./gradlew clean
```

Notes that matter:

- **Only the `debug` build type is configured.** The `release` block in `app/build.gradle` is commented out (it referenced a `signingConfigs.config` that no longer exists). Don't run `assembleRelease` and don't uncomment that block without being asked — `app/cert.jks` and `app/release/` are gitignored local artifacts.
- The SDK resolves from two non-default repos declared in the root `build.gradle`: JitPack (authenticated with the `authToken` in `gradle.properties`) and `https://bitbucket.org/purevpn/atom-android-releases/raw/master`. Build failures are usually resolution/credential problems, not code problems. `--offline` avoids both when the cache is warm.
- **There are no tests.** `testInstrumentationRunner` is declared but no `test/` or `androidTest/` source sets exist. Verification here means building and exercising the app on a real device — a VPN tunnel cannot be established on an emulator.
- `lint { abortOnError false }`, so lint noise won't fail a build.
- Gradle reports deprecation warnings against Gradle 9.0. Expected, not a regression.

## Architecture

Plain Android views + fragments, mostly **Java** (one Kotlin file, `AtomDemoAppCallback.kt`). No DI, no ViewModels, no coroutines/Rx — deliberately, so integrators can read it. State is reached through a static application singleton.

```
AtomDemoApplicationController      Application; builds AtomConfiguration and calls
                                   AtomManager.initialize(); holds the AtomManager instance
  └─ SampleMainActivity            launcher; routes on first run
       ├─ VpnSetupFragment         shown when isVPNServicePrepared() == false → asks for VPN permission
       └─ MainFragment             username/password entry + three "connect with…" buttons
  └─ ConnectActivity               hosts one connection fragment + the on-screen log pane
       ├─ ConnectWithParamsFragment        (connection_type = 1) country/city + up to 3 protocols
       ├─ ConnectWithDedicatedIPFragment   (connection_type = 2) host + IKEv2 only
       └─ ConnectWithChannelFragment       (connection_type = 3) channel + up to 3 protocols
```

Key conventions to follow when editing:

- **Getting the manager:** always `AtomDemoApplicationController.getInstance().getAtomManager()` and always null-check it. It is null until `InitializeCallback.onInitialized` fires, and every call site in this repo guards for that. `AtomManager.getInstance()` is used only for the static/permission helpers.
- **Which fragment to open** is decided by the `connection_type` int extra (1/2/3) passed from `MainFragment` into `ConnectActivity`; credentials ride along as `vpnUsername` / `vpnPassword` (or `uuid`) extras and are re-read in each fragment's `onCreate`.
- **Connection flow** in every fragment is the same shape: build a `VPNProperties.Builder` (from a `Country`, `City`, `Channel` or dedicated host, plus a primary `Protocol`), layer optional `withSecondaryProtocol` / `withTertiaryProtocol` / `withOptimization` / `withSmartDialing` / port options, set credentials via `setVPNCredentials(...)` or `setUUID(...)`, then `connect(activity, properties)`. `AtomValidationException` must be caught around the builder.
- **State listening:** each connection fragment implements `VPNStateListener`, registers with `AtomManager.addVPNStateListener(this)` in `onCreate`, calls `bindIKEVStateService(activity)` after a short delay, and **must** mirror that in `onDestroyView` with `removeVPNStateListener` + `unBindIKEVStateService`. Adding a listener without the teardown leaks.
- **The single Connect button is tri-state**, driven by `getCurrentVpnStatus(context)`: CONNECTED → `disconnect()`, CONNECTING → `cancel()`, otherwise → connect. `Utilities.changeButtonState` / `changeButtonText` own the label; both post with a delay, so don't also set the text inline.
- **Pause/Resume** requires `atomConfigurationBuilder.enableVPNPause()` at init (already on). The Pause button is only enabled on CONNECTED/PAUSED, and `Utilities.getPauseTimerList` supplies the `PauseVPNTimer` choices.
- **Logging goes on screen.** `com.atom.vpn.demo.common.logger.Log` is a local drop-in for `android.util.Log` that feeds `LogWrapper → MessageOnlyLogFilter → LogView` inside `ConnectActivity`. Use this `Log`, not `android.util.Log`, in demo code so callbacks stay visible to the user.
- `VPNState` (e.g. `VPNState.RECONNECTING` in `ConnectWithParamsFragment`) resolves unqualified with no import because it is a nested type of the implemented `VPNStateListener` interface. Not a missing import.
- Country/city/channel/protocol lists come back via `CollectionCallback` and are filtered client-side with the `androidlinq` `stream(...)` helper, then fed to the `ArrayAdapter` subclasses in `adapter/`.
- User-visible strings for hints/tooltips live in `common/Constants.java`; layout strings live in `res/values/strings.xml`.

## Writing in the README

It is API documentation for paying integrators, so:

- **Current API in the body, superseded API in a version-scoped note** — never the reverse. A reader on the pinned version must not be handed symbols that no longer exist.
- **No pseudo-signatures.** Write `atomManager.pause(pauseVPNTimer)`, not `atomManager.pause(@NonNull PauseVPNTimer)`. Every snippet should compile as written.
- **The table of contents is hand-maintained.** Renaming a heading means updating its TOC anchor in the same edit; GitHub slugs lowercase the text, drop punctuation and hyphenate spaces, and append `-1`, `-2` to duplicates.
- Heading hierarchy is `#` title → `##` majors → `###`/`####` below. Keep it.
- Version numbers in **Compatibility** are minimum requirements; those in **Recommendation** are the toolchain this demo is actually built with, so they track `build.gradle` exactly and get bumped during the release ritual.

### SDK facts already verified against the AAR

- `AtomShieldFeature`: only `TRACKER_AND_AD_BLOCKER` exists in 7.1.1 and 7.2.0. `TRACKER` and `AD_BLOCKER` were **removed** in 7.1.0, as were `ConnectionDetails.isTrackerBlockerRequested()` / `isAdBlockerRequested()` (replaced by `isTrackerAndAdBlockerRequested()`).
- `VPNStateListener`: `onConnected()`, `onConnecting()` and `onDisconnected(boolean)` are deprecated in favour of the `ConnectionDetails` / `VPNProperties` overloads. `onSessionTraffic(long,long,long,long)` is a `default` method added in 7.2.0.
- Traffic callbacks: `onSessionTraffic` and `onPacketsTransmitted` are dispatched from one `TrafficUpdate` in `AtomManager.publishTrafficUpdate`, delivered via `MutableLiveData` (hence main-thread). `ConnectionTools.bytesToSize` divides by 1024, emits `B`/`KB`/`MB`/`GB`/`TB`, and formats with `DecimalFormat("0.00")` built on `DecimalFormatSymbols.getInstance(Locale.US)` — so the output really is locale-independent ASCII.
- `DialingType` has exactly one constant, `VPN`.
- `onStateChange(String)` carries a `VPNStateListener.VPNState` `StringDef` with 20 constants; `AtomManager.VPNStatus` exposes only 6 of them.
- `AtomConfiguration.Builder` takes a `String` secret key, not a resource id.
- SDK bytecode is Java 17 (class major version 61), so Java 17 is a hard floor for integrators.

### Known documentation gaps

Deliberate, not bugs — raise them rather than silently fixing:

- The "SDK Features covered in this Demo" list omits Connection with Channel and Pause/Resume, both implemented here, and Smart Dialing / Optimization are undocumented although `withSmartDialing()` / `withOptimization()` are live, non-deprecated, and wired to switches in `ConnectWithParamsFragment`.
- No `VPNState` table, even though the text claims the SDK "provides all VPN states".

## The secret key

`res/values/strings.xml` ships `atom_secret_key` **intentionally empty** — integrators paste their own. With it empty the app toasts "Secret Key is required" and the SDK never initializes. Never commit a real secret key, and don't "fix" the empty value.

## Gotcha: the dependency exclude looks wrong but isn't

`app/build.gradle` contains, nested inside `android { }`:

```groovy
configurations {
    all*.exclude module: 'xpp3_min'
}
```

This is correct and deliberate — it matches the README's dependency-conflict snippet on purpose. Three things make it look broken when it isn't:

1. `configurations` is a `Project` method, not part of the android extension. Groovy resolves closures **owner-first**, so the call forwards to `Project.configurations` and behaves exactly as it would at the top level.
2. `all*` is eager, so it only touches configurations existing at that moment (~139 of the eventual 256). Inspecting `debugRuntimeClasspath.excludeRules` therefore returns `[]`.
3. The exclusion still applies, because the resolvable classpaths extend `implementation`/`api` (which do carry the rule) and excludes are inherited at resolution time. `./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency xpp3` confirms nothing resolves.

Don't "fix" it, and verify empirically before reporting anything here as broken.

## Release ritual

Demo versions track the SDK version, one commit per bump:

1. Branch from `develop` as `atom-sdk-update-<x.y.z>`.
2. In `app/build.gradle`: bump the SDK dependency to `<x.y.z>`, set `versionName "<x.y.z>"`, increment `versionCode` by 1.
3. Update `README.md` — the dependency snippet, the **Recommendation** toolchain versions, and any new/removed SDK feature sections, plus the hand-maintained table of contents.
4. Commit as `Atom sdk update version <x.y.z>`, PR into `develop`, then `develop` → `master`.

Current branch `atom-sdk-update-7.2.0` is mid-ritual and **uncommitted**: `app/build.gradle` is at 7.2.0 (versionCode 56) with the reworked `configurations` block, and `README.md` carries a large revision — the new `onSessionTraffic` / Network Traffic Updates section, a `Dialing Type` section, Compatibility split into Compatibility + Recommendation, the AtomShield version notes reordered, consistent heading levels, and ProGuard promoted to a top-level section.
