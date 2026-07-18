package com.rhdevs.rhpatch.morphe.youtube.misc.recyclerviewtree

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import org.luckypray.dexkit.result.ClassData

val recyclerViewTreeObserverFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
    )
    strings("LithoRVSLCBinder")
}

private val ClassData.isObject
    get() = this.descriptor.startsWith("L")

val RecyclerView_addOnScrollListener = findMethodDirect {
    recyclerViewTreeObserverFingerprint().let { method ->
        val recyclerView = method.paramTypes[1]
        method.invokes.single {
            it.declaredClass == recyclerView && it.paramTypes.singleOrNull { clz -> clz.isObject } != null
        }
    }
}
