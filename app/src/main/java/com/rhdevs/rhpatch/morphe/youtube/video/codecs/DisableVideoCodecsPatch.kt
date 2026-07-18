package com.rhdevs.rhpatch.morphe.youtube.video.codecs

import app.morphe.extension.youtube.patches.DisableVideoCodecsPatch
import app.morphe.extension.youtube.settings.Settings
import com.rhdevs.rhpatch.invokeOriginalMethod
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.morphe.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.patch
import org.luckypray.dexkit.wrap.DexMethod

val DisableVideoCodecs = patch(
    name = "Disable video codecs",
    description = "Adds options to disable HDR and VP9 codecs.",
) {
    PreferenceScreen.VIDEO.addPreferences(
        SwitchPreference("morphe_disable_hdr_video"),
        SwitchPreference(
            key = "morphe_force_avc_codec",
            tag = app.morphe.extension.youtube.settings.preference.ForceAVCSwitchPreference::class.java
        )
    )

    DexMethod("Landroid/view/Display\$HdrCapabilities;->getSupportedHdrTypes()[I").hookMethod {
        before {
            it.result = if (Settings.DISABLE_HDR_VIDEO.get())
                IntArray(0)
            else
                it.invokeOriginalMethod()
        }
    }

    Vp9CapabilityFingerprint.hookMethod {
        before {
            if (!DisableVideoCodecsPatch.allowVP9()) {
                it.result = false
            }
        }
    }
}
