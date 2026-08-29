package com.rhdevs.rhpatch.youtube.youtube.ad

import android.view.View
import com.rhdevs.rhpatch.youtube.extension.music.patches.HideAdsPatch
import com.rhdevs.rhpatch.youtube.extension.shared.Logger
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.components.AdsFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import com.rhdevs.rhpatch.youtube.shared.ad.HideFullscreenAds
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.layout.hide.general.HideHorizontalShelves
import com.rhdevs.rhpatch.youtube.youtube.misc.engagement.EngagementPanelHook
import com.rhdevs.rhpatch.youtube.youtube.misc.engagement.addEngagementPanelIdHook
import com.rhdevs.rhpatch.youtube.youtube.misc.litho.filter.LithoFilter
import com.rhdevs.rhpatch.youtube.shared.misc.litho.filter.addLithoFilter
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide general ads, Premium promotions and video ads.",
) {
    dependsOn(
        LithoFilter,
        EngagementPanelHook,
        HideHorizontalShelves,

        HideFullscreenAds(PreferenceScreen.ADS),
        VersionCheck,
    )

    PreferenceScreen.ADS.addPreferences(
        SwitchPreference("morphe_hide_creator_store_shelf"),
//        SwitchPreference("morphe_hide_end_screen_store_banner"),
        SwitchPreference("morphe_hide_general_ads"),
        SwitchPreference("morphe_hide_merchandise_banners"),
        SwitchPreference("morphe_hide_paid_promotion_label"),
        SwitchPreference("morphe_hide_player_popup_ads"),
        SwitchPreference("morphe_hide_self_sponsor_ads"),
        SwitchPreference("morphe_hide_shopping_links"),
        SwitchPreference("morphe_hide_video_ads"),
        SwitchPreference("morphe_hide_youtube_premium_promotions"),
    )

    addLithoFilter(AdsFilter())
    addEngagementPanelIdHook(AdsFilter::hidePlayerPopupAds)

    // Hide video ads

    setOf(
        LoadVideoAdsFingerprint,
        PlayerBytesAdLayoutFingerprint,
    ).forEach { fingerprint ->
        fingerprint.hookMethod {
            before {
                if(AdsFilter.hideVideoAds())
                    it.result = null
            }
        }
    }

    // TODO: Hide YouTube Premium promotions

    // TODO: Hide end screen store banner

    // Hide get premium
    GetPremiumViewFingerprint.hookMethod {
        after {
            if (AdsFilter.hideGetPremiumView()) {
                val view = it.thisObject as View
                XposedHelpers.callMethod(view, "setMeasuredDimension", 0, 0)
            }
        }
    }

    // Hide player overlay view. This can be hidden with a regular litho filter
    // but an empty space remains.

    PlayerOverlayTimelyShelfFingerprint.hookMethod {
        val playerOverlayEventClass = ::PlayerOverlayEventType.clazz
        val playerOverlayIdField = ::PlayerOverlayIdField.field
        before {
            val obj = it.args[0]
            if (playerOverlayEventClass.isInstance(obj)) {
                val id = playerOverlayIdField.get(obj) as String

                if (!AdsFilter.allowAds(id == "player_overlay_timely_shelf"))
                    it.result = null
            }
        }
    }

    // Hide ad views
    val adAttributionId = ResourceUtils.getIdIdentifier("ad_attribution")

    XposedHelpers.findAndHookMethod(
        View::class.java.name,
        lpparam.classLoader,
        "findViewById",
        Int::class.java.name,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.args[0] == adAttributionId) {
                    Logger.printDebug { "Hide Ad Attribution View" }
                    AdsFilter.hideAdAttributionView(param.result as View)
                }
            }
        })

    // TODO Hide paid promotion label in miniplayer

    /**
     * TODO [AdsFilter.hideAds] OsNameHook
     */
    /**
     * TODO [AdsFilter.hideVideoAds] OsNameHook
     */
}
