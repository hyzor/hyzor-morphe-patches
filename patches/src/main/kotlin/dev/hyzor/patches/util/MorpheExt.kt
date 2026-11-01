package dev.hyzor.patches.util

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

/**
 * The small subset of `app.morphe.util` that these patches need, implemented locally so the
 * bundle does not have to pull in the shared patch library just for a handful of helpers.
 */

/** Casts this instruction's reference to [T], or returns null when it carries another kind. */
inline fun <reified T : Reference> Instruction.getReference(): T? =
    (this as? ReferenceInstruction)?.reference as? T

/** Index of the first instruction matching [predicate], or -1. */
fun Method.indexOfFirstInstruction(predicate: Instruction.() -> Boolean): Int {
    this.implementation?.instructions?.forEachIndexed { index, instruction ->
        if (instruction.predicate()) return index
    }
    return -1
}

/** Index of the first instruction matching [predicate]. Throws when there is none. */
fun Method.indexOfFirstInstructionOrThrow(predicate: Instruction.() -> Boolean): Int =
    indexOfFirstInstruction(predicate).also {
        if (it < 0) {
            throw PatchException("Could not find instruction in ${this.definingClass}->${this.name}")
        }
    }

/**
 * The first constructor declared by the class (the primary one for Kotlin classes).
 *
 * Matched by name: `<clinit>` also carries [AccessFlags.CONSTRUCTOR], and picking it would inject
 * constructor-parameter instructions into the static initializer.
 */
val MutableClass.constructor: MutableMethod
    get() = this.methods.first { it.name == "<init>" }

/** The field declared by the class with the given name. */
fun MutableClass.fieldByName(name: String): MutableField =
    this.fields.first { it.name == name }

/** Clears one access flag from the field. */
fun MutableField.removeFlag(flag: AccessFlags) {
    this.accessFlags = this.accessFlags and flag.value.inv()
}

/** The public, no-argument `toString()` of the class, if it declares one. */
fun ClassDef.toStringMethod(): Method? = this.methods.firstOrNull {
    it.name == "toString" &&
        AccessFlags.PUBLIC.isSet(it.accessFlags) &&
        it.parameterTypes.isEmpty()
}

/** Inserts [smaliInstructions] immediately before the method's final instruction. */
fun MutableMethod.addInstructionsBeforeReturn(smaliInstructions: String) {
    this.addInstructions(this.instructions.count() - 1, smaliInstructions)
}
