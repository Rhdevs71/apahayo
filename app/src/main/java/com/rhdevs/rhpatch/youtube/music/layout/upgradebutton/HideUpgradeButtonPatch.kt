package com.rhdevs.rhpatch.youtube.music.layout.upgradebutton

import de.robv.android.xposed.XposedHelpers
import com.rhdevs.rhpatch.patch

val HideUpgradeButton = patch(
    name = "Hide upgrade button",
    description = "Hides the upgrade tab from the pivot bar.",
) {
    // TODO Patch is obsolete and was replaced by navigation bar patch
    ::pivotBarConstructorFingerprint.hookMethod {
        val pivotBarElementField = ::pivotBarElementField.field

        after { param ->
            val list = pivotBarElementField.get(param.thisObject)
            try {
                XposedHelpers.callMethod(list, "remove", 4)
            } catch (e: XposedHelpers.InvocationTargetError) {
                if (e.cause !is IndexOutOfBoundsException) throw e
            }
        }
    }
}
