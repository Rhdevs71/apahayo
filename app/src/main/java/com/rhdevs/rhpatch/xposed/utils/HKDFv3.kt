package com.rhdevs.rhpatch.xposed.utils

class HKDFv3 : HKDF() {
    override val iterationStartOffset: Int
        get() = 1
}
