# 🧩 Hyzor Patches

Patches for apps I use, for use with [Morphe](https://morphe.software).

Currently supports **Sleep as Android** (`com.urbandroid.sleep`).

### How to use these patches

Add this source to Morphe:

```
https://morphe.software/add-source?github=hyzor/hyzor-morphe-patches
```

Then patch a stock APK of a supported version. The output is re-signed by Morphe,
so the stock app has to be uninstalled first — back up your data before you do.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/hyzor/hyzor-morphe-patches/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
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

### Verification

`tools/verify_fingerprints.py` re-checks every fingerprint against an APK's DEX
files without running Gradle, Morphe or a disassembler:

```
python3 tools/verify_fingerprints.py /path/to/sleep.apk
```

Run it after every app update before bumping `Constants.kt`.

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
- Not affiliated with Urbandroid, the Morphe project, or the above authors.

Patches are provided for interoperability and private study of software I already
have the right to run. They bypass a licence check; using them to avoid paying for
software you use is your call, not mine.
