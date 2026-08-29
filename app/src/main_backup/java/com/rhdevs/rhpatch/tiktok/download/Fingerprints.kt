package com.rhdevs.rhpatch.tiktok.download

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint

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
