package com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.PlayerControlsVisibilityHookPatch
import com.rhdevs.rhpatch.patch

val PlayerControlsOverlayVisibility = patch {
    PlayerControlsVisibilityEntityModelInit.hookMethod {
        val getPlayerControlsVisibilityMethod =
            PlayerControlsVisibilityEntityModelFingerprint.method
        after {
            PlayerControlsVisibilityHookPatch.setPlayerControlsVisibility(
                getPlayerControlsVisibilityMethod(it.thisObject) as Enum<*>?
            )
        }
    }
}
