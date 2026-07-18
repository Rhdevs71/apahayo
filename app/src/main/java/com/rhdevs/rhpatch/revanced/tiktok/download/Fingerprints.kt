package com.rhdevs.rhpatch.revanced.tiktok.download

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint

val aclCommonShareGetCode = findMethodDirect(
    fingerprint {
        name("getCode")
        definingClass(".*ACLCommonShare.*")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returns("int")
    }
)

val aclCommonShareGetShowType = findMethodDirect(
    fingerprint {
        name("getShowType")
        definingClass(".*ACLCommonShare.*")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returns("int")
    }
)

val aclCommonShareGetTranscode = findMethodDirect(
    fingerprint {
        name("getTranscode")
        definingClass(".*ACLCommonShare.*")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returns("int")
    }
)
