package com.rhdevs.rhpatch.morphe.youtube.misc.litho.observer

import app.morphe.extension.youtube.patches.LayoutReloadObserverPatch
import com.rhdevs.rhpatch.morphe.youtube.misc.litho.node.TreeNodeElementHook
import com.rhdevs.rhpatch.morphe.youtube.misc.litho.node.hookTreeNodeResult
import com.rhdevs.rhpatch.patch


val LayoutReloadObserver = patch(
    description = "Hooks a method to detect in the extension when the RecyclerView at the bottom of the player is redrawn.",
) {
    dependsOn(
        TreeNodeElementHook
    )

    hookTreeNodeResult(LayoutReloadObserverPatch::onLazilyConvertedElementLoaded)
}
