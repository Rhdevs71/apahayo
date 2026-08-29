package com.rhdevs.rhpatch.youtube.youtube.layout.buttons.action

import com.rhdevs.rhpatch.youtube.extension.youtube.innertube.NextResponseOuterClass
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.components.QuickActionButtonsFilter
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.components.VideoActionButtonsFilter
import com.rhdevs.rhpatch.callMethod
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceCategory
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceScreenPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.litho.filter.LithoFilter
import com.rhdevs.rhpatch.youtube.shared.misc.litho.filter.addLithoFilter
import com.rhdevs.rhpatch.youtube.youtube.misc.litho.node.TreeNodeElementHook
import com.rhdevs.rhpatch.youtube.youtube.misc.litho.node.hookTreeNodeResult
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.youtube.shared.WatchNextResponseParserFingerprint
import com.rhdevs.rhpatch.youtube.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.patch

val HideVideoActionButtons = patch(
    name = "Hide video action buttons",
    description = "Adds options to hide video action buttons in fullscreen and portrait modes.",
) {
    dependsOn(
        LithoFilter,
        TreeNodeElementHook,
        VideoInformationPatch
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_action_buttons_screen",
            preferences = setOf(
                PreferenceCategory(
                    titleKey = "morphe_portrait_buttons",
                    preferences = setOf(
                        SwitchPreference("morphe_disable_like_subscribe_glow", summary = true),
                        SwitchPreference("morphe_hide_action_bar"),
                        SwitchPreference("morphe_hide_ask_button"),
                        SwitchPreference("morphe_hide_clip_button", summary = true),
                        SwitchPreference("morphe_hide_comments_button"),
                        SwitchPreference("morphe_hide_connect_button"),
                        SwitchPreference("morphe_hide_download_button"),
                        SwitchPreference("morphe_hide_hype_button"),
                        SwitchPreference("morphe_hide_like_dislike_button"),
                        SwitchPreference("morphe_hide_more_button"),
                        SwitchPreference("morphe_hide_promote_button"),
                        SwitchPreference("morphe_hide_remix_button"),
                        SwitchPreference("morphe_hide_report_button"),
                        SwitchPreference("morphe_hide_save_button"),
                        SwitchPreference("morphe_hide_share_button"),
                        SwitchPreference("morphe_hide_shop_button"),
                        SwitchPreference("morphe_hide_stop_ads_button"),
                        SwitchPreference("morphe_hide_thanks_button")
                    )
                ),
                PreferenceCategory(
                    titleKey = "morphe_fullscreen_buttons",
                    preferences = setOf(
//                        NonInteractivePreference(
//                            key = "morphe_quick_actions_top_margin",
//                            tag = "com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.SeekBarPreference"
//                        ),
                        SwitchPreference("morphe_hide_quick_actions"),
                        SwitchPreference("morphe_hide_quick_actions_ask_button"),
                        SwitchPreference("morphe_hide_quick_actions_comments_button"),
                        SwitchPreference("morphe_hide_quick_actions_dislike_button"),
                        SwitchPreference("morphe_hide_quick_actions_like_button"),
                        SwitchPreference("morphe_hide_quick_actions_live_chat_button"),
                        SwitchPreference("morphe_hide_quick_actions_mix_button"),
                        SwitchPreference("morphe_hide_quick_actions_more_button"),
                        SwitchPreference("morphe_hide_quick_actions_more_videos_button"),
                        SwitchPreference("morphe_hide_quick_actions_playlist_button"),
                        SwitchPreference("morphe_hide_quick_actions_save_button"),
                        SwitchPreference("morphe_hide_quick_actions_share_button")
                    )
                )
            )
        )
    )

    addLithoFilter(VideoActionButtonsFilter())
    addLithoFilter(QuickActionButtonsFilter())

    hookTreeNodeResult { identifier, list ->
        VideoActionButtonsFilter.onLazilyConvertedElementLoaded(identifier, list)
    }

    WatchNextResponseParserFingerprint.hookMethod {
        before {
            val messageLite = it.args[0]

            val nextResponse =
                NextResponseOuterClass.NextResponse.parseFrom(messageLite.callMethod("toByteArray") as ByteArray)

            if (!nextResponse.hasPrimaryContents()) {
                return@before
            }

            val primaryContents = nextResponse.primaryContents
            if (primaryContents.hasSingleColumnWatchNextResults()) {
                VideoActionButtonsFilter.onSingleColumnWatchNextResultsLoaded(primaryContents.singleColumnWatchNextResults)
            }
        }
    }
}
