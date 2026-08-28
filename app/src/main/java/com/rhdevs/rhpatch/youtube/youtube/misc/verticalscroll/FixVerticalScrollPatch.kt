package com.rhdevs.rhpatch.youtube.youtube.misc.verticalscroll

import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import com.rhdevs.rhpatch.youtube.shared.misc.litho.filter.featureFlagCheck
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.is_21_18_or_greater
import com.rhdevs.rhpatch.patch

val FixVerticalScroll = patch(
    description = "Fixes issues with refreshing the feed when the first component is of type EmptyComponent."
) {
    dependsOn(VersionCheck)

    if (is_21_18_or_greater) {
        // Can cause issues with scrolling.
        ::featureFlagCheck.hookMethod {
            before {
                if (it.args[0] == 45782902L)
                    it.result = false
            }
        }
    }

    ::canScrollVerticallyFingerprint.hookMethod(returnConstant(false))
}
