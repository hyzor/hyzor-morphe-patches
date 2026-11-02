# Nova Launcher

Package `com.teslacoilsw.launcher`, supported target `8.9.5` (versionCode `89500`, versionName
`89500 (8.9.5)`).
Source: [`patches/src/main/kotlin/dev/hyzor/patches/nova/`](../../patches/src/main/kotlin/dev/hyzor/patches/nova/).

| Patch | Default | What it does |
|---|---|---|
| Enable Prime | on | Unlocks the features gated on Nova Prime. |

## Port notes

Two upstream patches cover neighbouring Nova versions and neither drops in on 8.9.5:

- [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches) targets 8.8.8
  and patches the static isPrime gate, the runtime Prime field and the subscription check.
- [hoo-dles/morphe-patches](https://github.com/hoo-dles/morphe-patches) targets 8.8.6 and
  rewrites only the persisted Prime value that the settings initialiser reads.

8.9.5 moved every class involved (`wu/v0` → `px/b1`, `oy/h2` → `i10/g2`, `lv/c0` → `fy/d0`)
and added a Play licence (LVL) revocation path, so the reference fingerprints miss. The hooks
here are re-resolved against 8.9.5 and use instruction shapes rather than the obfuscated
class/method names wherever possible. The static gate's fingerprint still anchors on the
`i10/g2` field and the `fy/g0` call, so re-resolve those two anchors if they move.

One naming detail matters for compatibility: Nova's `versionName` is literally
`89500 (8.9.5)`, not `8.9.5`. Morphe compares the version name exactly, so
[`Constants.kt`](../../patches/src/main/kotlin/dev/hyzor/patches/nova/shared/Constants.kt)
declares the full string.

## How the app decides you are Prime

The Prime boolean is `i10.g2.h`. More than forty classes read that field directly; 17 more
call the static gate `px.b1.b()`, whose body is:

```java
return i10.g2.h || px.b1.b || fy.g0.a(context);
```

`px.b1.b` (the field) is set when the separate Nova Prime unlocker APK is installed, and
`fy.g0.a()` is the time-limited grant read from the `nova_prime_grant` preferences.

The field itself is derived on startup by `i10.g2.a(SharedPreferences)` from the persisted
`"1"` integer: `h = value != 0` and `c = value == 0x200`, where `0x200` is the value the app
writes when it accepts a direct licence or the unlocker APK.

8.9.5 also added a revocation path. `px.b1.c(context)` returns immediately when
`fy.d0.w(context)` reports a fresh subscription; otherwise it installs the Play licence
checker. On a re-signed build that check always fails; `ci.j4.d()` clears `"1"` and posts
`px.b1.g`, which re-enters `px.b1.c()` and can drop `i10.g2.h` back to `false`. The same
`fy.d0.w()` result makes the billing sync keep the cached subscription and skip the "Play
query returned nothing" downgrade.

## What the patch changes

1. `px.b1.b()` returns `true`.
2. The `getInt("1", 0)` result in `i10.g2.a()` is overwritten with `0x200`, so the app starts
   in its own Prime state (`h = true`, `c = true`).
3. `fy.d0.w(context)` returns `true`. This reports an active subscription and, because
   `px.b1.c()` short-circuits on it, keeps the licence revocation path from ever starting.

Only client-side decisions change. Anything Nova asks its backend for still needs a real
account, and the Manage Subscription screen keeps offering the Nova AI / Plus upsell.

## Verification

- `python3 tools/verify_fingerprints.py <stock apk>` resolves all three pinned methods.
- `morphe-desktop patch --validate` applied the bundle to the stock 8.9.5 APK; each
  fingerprint matched exactly one method (`Changed 3 of 43463 classes to mutable`).
- The patched DEX was decoded and checked: `px.b1.b()` and `fy.d0.w()` start with
  `const/4 v0, 0x1; return v0`, and `i10.g2.a()` has `const/16 p1, 0x200` immediately after
  the `getInt("1", 0)` result.
- Runtime A/B on an Android 16 (API 36) x86_64 emulator with ARM translation, same APK,
  stock vs patched (not repeated on physical hardware):

  | | Stock | Patched |
  |---|---|---|
  | Settings header | `Nova Launcher 89500 (8.9.5)` | `Nova Prime 89500 (8.9.5)` |
  | Manage Subscription summary | `Unlock more with Nova Prime or Plus` | `Nova Prime · Upgrade for Nova AI` |
  | App drawer → Hide apps | `Prime` badge, opens Manage Subscription | no badge, opens the hide-apps list |
  | First launch | GDPR ad-consent dialog | no consent dialog |

  The Prime state survived a force-stop and relaunch, and logcat showed no fatal exceptions.

Not exercised: any real purchase, Play account flow, physical hardware, or the behaviour after
the 48-hour subscription cache expiry (the patched `fy.d0.w()` skips that check entirely).
