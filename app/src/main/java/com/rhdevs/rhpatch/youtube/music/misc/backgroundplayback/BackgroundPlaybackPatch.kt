package com.rhdevs.rhpatch.youtube.music.misc.backgroundplayback

import de.robv.android.xposed.XC_MethodReplacement
import com.rhdevs.rhpatch.patch

val BackgroundPlayback = patch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
) {
    ::backgroundPlaybackDisableFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    ::kidsBackgroundPlaybackPolicyControllerFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
}
