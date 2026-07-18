package com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols

import app.morphe.extension.youtube.patches.PlayerControlsVisibilityHookPatch
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
