package com.rhdevs.rhpatch.youtube.youtube.layout.shortsnoresume

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.accessFlags
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint
import com.rhdevs.rhpatch.youtube.literal
import com.rhdevs.rhpatch.youtube.opcodes
import com.rhdevs.rhpatch.youtube.parameters
import com.rhdevs.rhpatch.youtube.returns

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
