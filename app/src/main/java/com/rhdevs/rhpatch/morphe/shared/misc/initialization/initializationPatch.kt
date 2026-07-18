package com.rhdevs.rhpatch.morphe.shared.misc.initialization

import app.morphe.extension.shared.patches.InitializationPatch
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.scopedHook


internal fun initializationPatch() = patch (
    description = "Prompts to restart the app on first load of a clean install",
) {
    GlobalConfigGroupFingerprint.hookMethod(scopedHook(::handleColdFingerprint.member) {
        InitializationPatch.onGlobalConfigUpdated()
    })
}
