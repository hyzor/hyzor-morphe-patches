package dev.hyzor.patches.duolingo.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import dev.hyzor.patches.util.getReference
import dev.hyzor.patches.util.indexOfFirstInstructionOrThrow
import dev.hyzor.patches.util.toStringMethod

internal object Utils {
    /**
     * Resolves the field printed by `toString()` right after the literal containing [subStr].
     *
     * Duolingo's generated `toString()`s print `", <serializedName>="` and then the field, but
     * the compiler batches several label/field pairs into a helper call, so the instruction
     * immediately after the label can be another `const-string`. The field access is always the
     * second instruction after the label in that shape, and the plain
     * `const-string` / `append` / `iget` shape of older versions happens to line up with it too.
     */
    internal fun ClassDef.fieldFromToString(subStr: String): FieldReference {
        val toString = this.toStringMethod()
            ?: throw PatchException("Could not find toString() on ${this.type}")

        val strIndex = toString.indexOfFirstInstructionOrThrow {
            this.opcode == Opcode.CONST_STRING &&
                getReference<StringReference>()?.string?.contains(subStr) == true
        }

        // The field access comes after the const-string and the (possibly batched) append.
        return toString.getInstruction<ReferenceInstruction>(strIndex + 2)
            .getReference<FieldReference>()
            ?: throw PatchException("Could not find field: $subStr")
    }
}
