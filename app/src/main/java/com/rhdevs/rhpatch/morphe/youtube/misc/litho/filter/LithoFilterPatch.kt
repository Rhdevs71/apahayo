package com.rhdevs.rhpatch.morphe.youtube.misc.litho.filter

import com.rhdevs.rhpatch.morphe.shared.misc.litho.filter.sharedLithoFilterPatch
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.is_20_22_or_greater
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.is_21_15_or_greater
import com.rhdevs.rhpatch.morphe.youtube.misc.verticalscroll.FixVerticalScroll

val LithoFilter = sharedLithoFilterPatch(
    // YouTube 20.22+ always uses the native Upb encode path.
    hookNonNativeBuffer = { !is_20_22_or_greater },
    // Flag was removed in 21.15+.
    overrideUpbFeatureFlag = { !is_21_15_or_greater }
) {
    dependsOn(
        FixVerticalScroll,
        VersionCheck,
    )
}
