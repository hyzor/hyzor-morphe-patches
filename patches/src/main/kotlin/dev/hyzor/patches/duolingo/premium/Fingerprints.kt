package dev.hyzor.patches.duolingo.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Matches LoggedInState.toString(); the state object that wraps the signed-in User.
object LoggedInStateFingerprint : Fingerprint(
    strings = listOf("LoggedIn(user=", ")"),
)

// Matches User.toString().
object UserFingerprint : Fingerprint(
    strings = listOf("User(adsConfig=", ", id=", ", betaStatus="),
)

/**
 * The helper that reads the user's "paid" bit. It is a public static `(User, L…)Z` that calls
 * `getClass()` and immediately reads a boolean field of `User`, i.e. `z22.k(User, f9d)Z` in
 * 6.104.3 (the field is `R0`). The field is computed rather than serialized, which is why it is
 * located through its usage instead of the serializer's field names.
 */
object UserIsPaidFieldUsageFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lcom/duolingo/data/user/User;", "L"),
    filters = listOf(
        methodCall(name = "getClass"),
        fieldAccess(definingClass = "Lcom/duolingo/data/user/User;"),
    ),
)

/**
 * `MaxHooksUserData` reads the user's "MAX" bit in its constructor (`Q0` in 6.104.3).
 */
object UserHasGoldFieldUsageFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("MaxHooksUserData(isAdmin="),
    ),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/duolingo/data/user/User;",
            type = "Z",
        ),
    ),
)

/**
 * A check of `SubscriptionFeatureGroup.contains(SubscriptionFeatures.<feature>)`. The matched
 * `move-result` is the boolean the caller branches on; MAX mode forces it to true.
 */
fun getFeatureFingerprint(feature: String) = Fingerprint(
    filters = listOf(
        fieldAccess(
            name = feature,
            definingClass = "Lcom/duolingo/data/subscription/models/SubscriptionFeatures;",
        ),
        methodCall(
            name = "contains",
            location = InstructionLocation.MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
    ),
)

/**
 * Locates the field behind `VideoCallTabCtaButtonState`'s upsell eligibility flag by the label
 * printed by its `toString()`, then the boolean read that follows it.
 */
fun getVideoCallTabCtaButtonStateFieldFingerprint(field: String): Fingerprint {
    return Fingerprint(
        strings = listOf("VideoCallTabCtaButtonState(userHasMax="),
        filters = listOf(
            string(field),
            // `const-string` / `append` / `iget-boolean` / `append` in 6.104.3.
            opcode(Opcode.IGET_BOOLEAN, InstructionLocation.MatchAfterAnywhere()),
        ),
    )
}

/**
 * `EnergyConfig.<init>(current, max, …)` in 6.104.3. The primary constructor gained the trailing
 * `Ljava/lang/Integer;` parameter (`EnergyConfig(IIJJJIJLjava/lang/Integer;)V`).
 */
object EnergyConfigCtorFingerprint : Fingerprint(
    definingClass = "Lcom/duolingo/core/energy/models/EnergyConfig;",
    name = "<init>",
    parameters = listOf("I", "I", "J", "J", "J", "I", "J", "Ljava/lang/Integer;"),
)
