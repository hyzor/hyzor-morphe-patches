/*
 * Copyright 2026 rushiranpise
 * https://github.com/rushiranpise/morphe-patches
 *
 * Ported from the 8.8.8 Nova Launcher patch in rushiranpise/morphe-patches and the 8.8.6
 * patch in hoo-dles/morphe-patches (both GPLv3), re-resolved for 8.9.5.
 */

package dev.hyzor.patches.nova.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import dev.hyzor.patches.nova.shared.Constants.NOVA_LAUNCHER
import dev.hyzor.patches.util.returnEarly

/**
 * Nova Launcher 8.9.5 keeps the Prime decision on the device, in two places that both have to
 * agree before the paid surfaces appear.
 *
 * ### The Prime state
 *
 * `i10.g2.h` is the Prime boolean. More than sixty classes read it directly, and the static
 * `px.b1.b()` gate reads it too and ORs in two fallbacks: the separate Nova Prime unlocker
 * APK (the `px.b1.b` field) and the time-limited grant in `fy.g0.a()`.
 *
 * `i10.g2.a(SharedPreferences)` derives the field on every startup from the persisted `"1"`
 * integer: `h = value != 0` and `c = value == 0x200`, where `0x200` is the value the app
 * itself writes when it accepts the unlocker APK. Forcing that one read to `0x200` therefore
 * reproduces the app's own "Prime" state, including the marker that says Prime came from a
 * direct licence rather than from a subscription.
 *
 * ### The revocation path
 *
 * `fy.d0.w(Context)` reports whether a fresh subscription is on record. Three pieces of the
 * 8.9.5 anti-tamper flow consult it:
 *
 *   - `px.b1.c(Context)` returns immediately when it is true, so the Play licence (LVL)
 *     checker is never installed. A re-signed APK always fails LVL, and the failure handler
 *     (`ci.j4.d()`) clears `"1"` and posts a runnable that sets `i10.g2.h` back to false.
 *   - the billing sync takes it as "trust the cache" when Play returns no purchases.
 *   - `fy.d0.o()` reports the subscription as active.
 *
 * The patch only changes these client-side decisions. Anything the app asks Nova's backend
 * for still needs a real account.
 */
@Suppress("unused")
val enablePrimePatch = bytecodePatch(
    name = "Enable Prime",
    description = "Unlocks Nova Launcher Prime by starting the app in the prime state and " +
        "reporting an active subscription, so the settings and features gated on Prime are " +
        "available.",
    default = true,
) {
    compatibleWith(NOVA_LAUNCHER)

    execute {
        // The static gate, so callers that use px.b1.b() instead of reading the field agree
        // with the field value forced below.
        IsPrimeFingerprint.method.returnEarly(true)

        // The startup read of the persisted Prime value. Overwriting the getInt() result with
        // 0x200 makes both the field and the "direct licence" marker come out as the app's own
        // Prime state; it is re-applied on every startup.
        val primeStateInit = PrimeStateInitFingerprint.method
        val persistedPrimeRead = PrimeStateInitFingerprint.instructionMatches.last()
        val valueRegister = persistedPrimeRead.getInstruction<OneRegisterInstruction>().registerA

        primeStateInit.addInstructions(
            persistedPrimeRead.index + 1,
            "const/16 v$valueRegister, 0x200",
        )

        // Report an active subscription. Besides unlocking the subscription-gated surfaces this
        // keeps the LVL-based Prime revocation from ever starting.
        IsSubscriptionActiveFingerprint.method.returnEarly(true)
    }
}
