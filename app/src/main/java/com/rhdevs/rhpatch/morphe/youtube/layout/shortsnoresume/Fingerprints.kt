package com.rhdevs.rhpatch.morphe.youtube.layout.shortsnoresume

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.accessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import com.rhdevs.rhpatch.morphe.literal
import com.rhdevs.rhpatch.morphe.opcodes
import com.rhdevs.rhpatch.morphe.parameters
import com.rhdevs.rhpatch.morphe.returns

val userWasInShortsFingerprint = findMethodDirect {
    runCatching {
        fingerprint {
            returns("V")
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            parameters("Ljava/lang/Object;")
            strings("userIsInShorts: ")
        }
    }.getOrElse {
        findMethod {
            matcher {
                returns("V")
                accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
                parameters("Ljava/lang/Object;")
                opcodes(
                    Opcode.INVOKE_INTERFACE, // userWasInShortsProtoStoreProvider
                    Opcode.MOVE_RESULT_OBJECT,
                    Opcode.CHECK_CAST,
                    Opcode.NEW_INSTANCE,
                    Opcode.INVOKE_DIRECT, // userWasInShortsBuilder
                    Opcode.INVOKE_INTERFACE,
                    Opcode.RETURN_VOID,
                )
            }
        }.findMethod {
            matcher {
                opcodes(
                    Opcode.CHECK_CAST, // p1, Ljava/lang/Boolean; // userIsInShorts
                    Opcode.INVOKE_VIRTUAL, // Ljava/lang/Boolean;->booleanValue()Z
                    Opcode.MOVE_RESULT,
                    Opcode.IGET_OBJECT,
                    Opcode.MOVE_OBJECT,
                    Opcode.CHECK_CAST,
                    Opcode.IGET_OBJECT,
                    Opcode.INVOKE_INTERFACE, // userWasInShortsProtoStoreProvider
                )
            }
        }.single()
    }
}

/**
 * 18.15.40+
 */
internal object UserWasInShortsConfigFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45358360L)
    ),
)
