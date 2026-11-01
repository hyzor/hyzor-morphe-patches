package dev.hyzor.patches.duolingo.shared.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import dev.hyzor.patches.util.getReference
import dev.hyzor.patches.util.indexOfFirstInstruction

/**
 * Duolingo attaches a login integrity signal to the email login request. A re-signed sideload
 * cannot produce a valid attestation for it, and login fails.
 *
 * The app already ships an empty signal singleton — the instance the normal email login builder
 * passes. The patch loads that instance instead of whatever signal the caller supplied, inside
 * `LoginRequest$EmailLoginRequest.<init>`, so every construction site (email login builder, the
 * shared-login lambda and the serializer) sends the empty signal.
 *
 * Every other Duolingo patch depends on this one: without it a re-signed build cannot log in.
 */
@Suppress("unused")
val disableLoginIntegrityPatch = bytecodePatch {
    execute {
        // The empty signal singleton, e.g. `sget-object v1, Loi00;->a:Loi00;` in the email login
        // builder. Its superclass is the declared type of the request's signal parameter.
        val emptySignalRef = BasicLoginFingerprint.method.let { method ->
            method.getInstruction<Instruction21c>(
                method.indexOfFirstInstruction { opcode == Opcode.SGET_OBJECT },
            ).getReference<FieldReference>()
        } ?: throw PatchException("Could not find the empty login signal instance")

        val abstractSignalClass = classDefBy(emptySignalRef.type).superclass
            ?: throw PatchException("Login signal ${emptySignalRef.type} has no superclass")

        LoginStateFingerprint.method.apply {
            // The constructor stores the signal parameter into the request's signal field.
            val setSignalIndex = indexOfFirstInstruction {
                getReference<FieldReference>()?.type == abstractSignalClass
            }
            if (setSignalIndex < 0) {
                throw PatchException("Could not find the login signal field write")
            }

            val signalParamReg = getInstruction<Instruction22c>(setSignalIndex).registerA
            addInstruction(setSignalIndex, "sget-object v$signalParamReg, $emptySignalRef")
        }
    }
}
