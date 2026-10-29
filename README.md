# 🧩 Hyzor Patches

Patches for apps I use, for use with [Morphe](https://morphe.software).

Currently supports **Sleep as Android** (`com.urbandroid.sleep`),
**SnoreLab** (`com.snorelab.app`) and **NYT Games** (`com.nytimes.crossword`).

### How to use these patches

Add this source to Morphe:

```
https://morphe.software/add-source?github=hyzor/hyzor-morphe-patches
```

Then patch a stock APK (or APKM/XAPK bundle) of a supported version. The output is
re-signed by Morphe, so the stock app has to be uninstalled first — back up your data
before you do.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0](https://github.com/hyzor/hyzor-morphe-patches/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 Sleep as Android&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 20261006 | 20260616 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Enable Cloud Premium](#enable-cloud-premium) | Reports the Sleep Cloud subscription as active. Client side only; the backend may still refuse. |  |
| [Enable Premium](#enable-premium) | Unlocks Sleep as Android Premium. Tap Wait I already Paid when Prompted |  |

</details>

<details open>
<summary>📦 SnoreLab&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.33.5.7814 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Enable Premium](#enable-premium) | Enables app features locked behind the subscription paywall. |  |

</details>

<!-- PATCHES_END -->

## 📱 Sleep as Android

Supported targets: `20261006`, `20260616`.

| Patch | Default | What it does |
|---|---|---|
| Enable Premium | on | Unlocks the in-app premium features. |
| Enable Cloud Premium | off | Reports the Sleep Cloud subscription as active (client-side only). |

### How the app decides you are premium

Sleep as Android validates in-app purchases entirely on-device:

1. Play Billing returns a purchase; the app verifies Play's signature over it with an
   RSA key embedded in `classes4.dex` (`Billing.verify`, `SHA1withRSA`).
2. On success it writes `BS = 42` into the `TVF` SharedPreferences file — a plain
   `long`, no MAC, no Keystore wrapping.
3. `TrialFilter.refresh()` maps that to `isTrial = false`, which 58 call sites read.

There is no server-issued licence. The patches therefore rewrite the decision
(`isTrial`, `hasUnlock`, `isUnlockAcked`) rather than trying to forge a receipt, and
additionally clear the two tamper signals a re-signed sideload triggers
(`isOurSignature`, `isUnknownSource`).

The genuine paid unlocker (`com.urbandroid.sleep.full.key`) is *not* required and
does **not** work alongside these patches — Morphe re-signs the app, so the
`checkSignatures()` comparison between the two packages no longer matches.

## 📱 SnoreLab

Supported target: `2.33.5.7814` (XAPK).

| Patch | Default | What it does |
|---|---|---|
| Enable Premium | on | Unlocks the in-app premium features. |

This is a port of [hoo-dles/morphe-patches](https://github.com/hoo-dles/morphe-patches)'
2.27.3.6291 patch. The newer build is not a drop-in replacement for its fingerprints:
the prefs class gained two more boolean getters, and a second class now matches the
"public one-object constructor" anchor, so both reference fingerprints become
ambiguous. `Fingerprints.kt` pins the exact method / premium-source interface and
documents why.

The port was verified end-to-end on a Pixel 9 (Android 17): a premium-only
soundscape (`Soundscape.FOREST`) selects and plays without opening the purchase
screen, Trends renders without its upgrade banner, and *Settings → Profile → Your
Subscription* reports “You have the Premium version of SnoreLab”.

### How the app decides you are premium

SnoreLab uses the `lib-billing` library. `BasePremiumRepository` (`pl.c`) merges Play
Billing with a local premium source (`mp.d`):

1. `mp.d` reads `remotePremiumActive` and `remotePremiumExpirationDate` from the
   user-data SharedPreferences (`vp.h`).
2. It emits `Premium` when the flag is set and the expiry is in the future;
   otherwise `Expired` or `Free`.
3. `pl.c` keeps a local `Premium` even when Play Billing returns no purchases, and
   `pl.a0.isPremium()` follows the merged state.

The patch forces the two inputs of the local source (`vp.h.q()` → true and
`mp.d.p(...)` → true), so the merge always produces `Premium`. It only changes the
client-side decision; anything that authenticates against SnoreLab's backend still
needs a real account.

### Recording immediately returns to the landing page

SnoreLab will not start a session without microphone access, and it does not show
an error when the permission is missing. `NightViewActivity` checks `RECORD_AUDIO`
in `onCreate`, then calls `finish()` and relaunches `MainActivity`, so tapping
**Start** looks like it bounces straight back to the home screen.

Grant the permission and try again:

- **Settings → Apps → SnoreLab → Permissions → Microphone → Allow while using app**, or
- `adb shell pm grant com.snorelab.app android.permission.RECORD_AUDIO`

Reinstalling or updating the app can reset runtime permissions, so re-check this
after every install. A test session can also be stopped from the ongoing
notification's **STOP** action; the in-app slide-to-stop control shows the
30-minute minimum-recording warning first.

## 📱 NYT Games

Supported target: `6.48.0` (APKM).

| Patch | Default | What it does |
|---|---|---|
| Unlock Subscription | on | Reports the account as an active Games subscriber (client-side only). |

This is a port of [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches)'
6.33.0 patch. The newer build flattened the delegate chain that patch's fingerprint was
written against, so the hook had to be re-resolved; `Fingerprints.kt` documents how the
method was identified.

### How the app decides you are a subscriber

The games code does not verify a receipt. The subauth library keeps a local user state
(`NYTUserState`) with three booleans — signed in, has an active entitlement, has a linked
entitlement — and `SubauthEntitlementClientImpl` exposes them:

| Method | Name (from the in-app developer settings) | Reads |
|---|---|---|
| `P()` | `isSubscribedForGames` | the active-entitlement bit |
| `E()` | `isSubscribedAndLinked` | `SubscriptionLevel` is *registered + subscribed + linked* |
| `K()` | `isSubscribedViaGooglePlay` | Play entitlements contain `Games` |
| `R()` | `hasUnlinkedSubscription` | `SubscriptionLevel` is an unlinked subscriber |

`P()` is the one the rest of the app treats as "subscribed":

- `AppEntitlementsImpl.e()` → `UserEntitlements.statusFlow` → `EntitlementStatus`, which
  `GameAvailabilityCalculator`, the archive screens and the bonus-puzzle paywall read;
- `GamesHybridUserConfig.isSubscribed`, handed to the WebView games;
- `GamesSubauth2PurchaseClientImpl.d()` plus ads/analytics targeting.

The patch forces only `P()` to `true`. The signed-in and linked bits are left alone on
purpose: forcing them changes `NYTUserImpl.d()` to *registered + subscribed + linked*, and
the app then tries to load a profile for a session that does not exist — that is the
"Oops, something went wrong" Me-page bug noted in the reference patch.

The port was verified end-to-end on a Pixel 9 (Android 17) with a signed-in **free** NYT
account. Installing only the patched DEX over the stock build (same app data, same
throwaway signing key) flips every subscription surface in the app:

| Surface | Stock | Patched |
|---|---|---|
| Me → Settings | `NOT SUBSCRIBED`, **Subscribe** row | `NYT GAMES SUBSCRIBER`, no Subscribe row |
| Home hero | “Subscribe to Games” banner | normal greeting |
| Crossword / Wordle / Connections / Strands archive | “Access our archives with a subscription” | full calendar; archived puzzles open and play |
| Bonus puzzles | free *Starter set* + “Unlock new Weekly drops” tile | the current weekly drop, playable |

The same comparison holds with no account signed in (anonymous users get the same
unlocks), and the Me page keeps working throughout — no “Oops, something went wrong”.

### Limitations

Only the client-side decision is changed. Anything the NYT backend decides for itself
(account/subscription pages, purchase and restore, pushes) still sees the real account
state, and an anonymous user stays anonymous — the patch reports *subscribed*, not
*signed in*. Game **Packs** are one-time purchases rather than a subscription and are not
covered by this patch.

## ✅ Verification

`tools/verify_fingerprints.py` re-checks every fingerprint against an APK's DEX
files without running Gradle, Morphe or a disassembler. It detects the app from the
classes that are present:

```
python3 tools/verify_fingerprints.py /path/to/app.apk
```

APK, APKM and XAPK all work: bundles are opened through their nested `base.apk`.

Run it after every app update before bumping the matching `Constants.kt`.

## 🚀 Development

Requires JDK 21+ and a GitHub token with `read:packages`, because the
`app.morphe.patches` Gradle plugin resolves from GitHub Packages:

```
export GITHUB_ACTOR=<your-github-username>
export GITHUB_TOKEN=<token with read:packages>
./gradlew :patches:buildAndroid
```

The bundle lands in `patches/build/libs/patches-*.mpp`.

- Make changes on `dev`. `fix:` / `feat:` commits produce pre-releases, `chore:`
  commits do not.
- Merge `dev` into `main` (merge, don't squash) for a stable release.
- Enable *Allow GitHub Actions to create and approve pull requests* in
  Settings → Actions → General → Workflow permissions, or the `dev` → `main` PR
  job fails.

## ⚖️ Licence and credits

GPLv3 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

- Built from [MorpheApp/morphe-patches-template](https://github.com/MorpheApp/morphe-patches-template).
- The Sleep as Android patches descend from
  [Entree3k/Morning-Entree-Patches](https://github.com/Entree3k/Morning-Entree-Patches)
  (GPLv3). The 20261006 compatibility target, the two extra fingerprint hooks and the
  cloud patch are new here.
- The SnoreLab patch is a port of
  [hoo-dles/morphe-patches](https://github.com/hoo-dles/morphe-patches) (GPLv3).
- The NYT Games patch is a port of
  [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches) (GPLv3).
- Not affiliated with Urbandroid, The New York Times, the Morphe project, or the above
  authors.

Patches are provided for interoperability and private study of software I already
have the right to run. They bypass a licence check; using them to avoid paying for
software you use is your call, not mine.
