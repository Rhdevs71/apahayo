package com.rhdevs.rhpatch.youtube.music

import com.rhdevs.rhpatch.ExtensionResourceHook
import com.rhdevs.rhpatch.youtube.music.ad.HideAds
import com.rhdevs.rhpatch.youtube.music.audio.exclusiveaudio.EnableExclusiveAudioPlayback
import com.rhdevs.rhpatch.youtube.music.layout.upgradebutton.HideUpgradeButton
import com.rhdevs.rhpatch.youtube.music.misc.backgroundplayback.BackgroundPlayback
import com.rhdevs.rhpatch.youtube.music.misc.debugging.EnableDebugging
import com.rhdevs.rhpatch.youtube.music.misc.privacy.SanitizeSharingLinks
import com.rhdevs.rhpatch.youtube.music.misc.settings.SettingsHook
import com.rhdevs.rhpatch.youtube.shared.misc.CheckRecycleBitmapMediaSession

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
