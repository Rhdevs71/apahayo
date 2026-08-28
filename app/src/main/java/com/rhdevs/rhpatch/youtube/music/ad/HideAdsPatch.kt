package com.rhdevs.rhpatch.youtube.music.ad

import android.view.View
import com.rhdevs.rhpatch.youtube.extension.music.patches.HideAdsPatch
import com.rhdevs.rhpatch.youtube.extension.shared.Logger
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils
import com.rhdevs.rhpatch.youtube.music.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.shared.ad.HideFullscreenAds
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide fullscreen ads, Premium promotions and video ads."
) {
    dependsOn(
        HideFullscreenAds(PreferenceScreen.ADS),
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_music_hide_get_premium_label"),
        SwitchPreference("morphe_music_hide_video_ads"),
    )

    // Hide 'Get Music Premium' label
    ::hideGetPremiumFingerprint.hookMethod {
        val id = ResourceUtils.getIdIdentifier("unlimited_panel")
        after { param ->
            val thiz = param.thisObject
            for (field in thiz.javaClass.fields) {
                val view = field.get(thiz)
                if (view !is View) continue
                val panelView = view.findViewById<View>(id) ?: continue
                Logger.printDebug { "hide get premium" }
                panelView.visibility = View.GONE
                break
            }
        }
    }

    ::membershipSettingsFingerprint.hookMethod {
        before {
            if (HideAdsPatch.hideGetPremiumLabel()) it.result = null
        }
    }

    ::showVideoAds.hookMethod {
        before { param ->
            param.args[0] = HideAdsPatch.hideVideoAds(param.args[0] as Boolean)
        }
    }
}
