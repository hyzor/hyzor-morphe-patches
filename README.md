# 🧩 Hyzor Patches

Patches for apps I use, for use with [Morphe](https://morphe.software).

Currently supports **Sleep as Android** (`com.urbandroid.sleep`) and
**SnoreLab** (`com.snorelab.app`).

### How to use these patches

Add this source to Morphe:

```
https://morphe.software/add-source?github=hyzor/hyzor-morphe-patches
```

Then patch a stock APK (or XAPK) of a supported version. The output is re-signed by
Morphe, so the stock app has to be uninstalled first — back up your data before you do.

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

## ✅ Verification

`tools/verify_fingerprints.py` re-checks every fingerprint against an APK's DEX
files without running Gradle, Morphe or a disassembler. It detects the app from the
classes that are present:

```
python3 tools/verify_fingerprints.py /path/to/app.apk
```

For an XAPK, point it at the extracted base APK (`com.snorelab.app.apk`), not the
`.xapk` archive.

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
- Not affiliated with Urbandroid, the Morphe project, or the above authors.

Patches are provided for interoperability and private study of software I already
have the right to run. They bypass a licence check; using them to avoid paying for
software you use is your call, not mine.
