package com.rhdevs.rhpatch.morphe.music

import com.rhdevs.rhpatch.ExtensionResourceHook
import com.rhdevs.rhpatch.morphe.music.ad.HideAds
import com.rhdevs.rhpatch.morphe.music.audio.exclusiveaudio.EnableExclusiveAudioPlayback
import com.rhdevs.rhpatch.morphe.music.layout.upgradebutton.HideUpgradeButton
import com.rhdevs.rhpatch.morphe.music.misc.backgroundplayback.BackgroundPlayback
import com.rhdevs.rhpatch.morphe.music.misc.debugging.EnableDebugging
import com.rhdevs.rhpatch.morphe.music.misc.privacy.SanitizeSharingLinks
import com.rhdevs.rhpatch.morphe.music.misc.settings.SettingsHook
import com.rhdevs.rhpatch.morphe.shared.misc.CheckRecycleBitmapMediaSession

val YTMusicPatches = arrayOf(
    ExtensionResourceHook,
    BackgroundPlayback,
    HideUpgradeButton,
    HideAds,
    EnableExclusiveAudioPlayback,
    CheckRecycleBitmapMediaSession,
    EnableDebugging,
    SanitizeSharingLinks,
    SettingsHook
)
