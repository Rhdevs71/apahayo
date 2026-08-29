package com.rhdevs.rhpatch.youtube.shared.misc.initialization

import com.rhdevs.rhpatch.youtube.extension.shared.patches.InitializationPatch
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.scopedHook


internal fun initializationPatch() = patch (
    description = "Prompts to restart the app on first load of a clean install",
) {
    GlobalConfigGroupFingerprint.hookMethod(scopedHook(::handleColdFingerprint.member) {
        InitializationPatch.onGlobalConfigUpdated()
    })
}
