package com.rhdevs.rhpatch.morphe.youtube

import android.app.Activity
import app.morphe.extension.shared.Utils
import com.rhdevs.rhpatch.ExtensionResourceHook
import com.rhdevs.rhpatch.addModuleAssets
import com.rhdevs.rhpatch.injectHostClassLoaderToSelf
import com.rhdevs.rhpatch.injectSelfClassLoaderToHost
import com.rhdevs.rhpatch.morphe.shared.misc.CheckRecycleBitmapMediaSession
import com.rhdevs.rhpatch.morphe.youtube.ad.HideAds
import com.rhdevs.rhpatch.morphe.youtube.interaction.copyvideolink.CopyVideoLinkButtonPatch
import com.rhdevs.rhpatch.morphe.youtube.interaction.downloads.Downloads
import com.rhdevs.rhpatch.morphe.youtube.interaction.swipecontrols.SwipeControls
import com.rhdevs.rhpatch.morphe.youtube.layout.buttons.action.HideVideoActionButtons
import com.rhdevs.rhpatch.morphe.youtube.layout.buttons.navigation.NavigationBar
import com.rhdevs.rhpatch.morphe.youtube.layout.captions.AutoCaptions
import com.rhdevs.rhpatch.morphe.youtube.layout.hide.general.HideLayoutComponents
import com.rhdevs.rhpatch.morphe.youtube.layout.hide.shorts.HideShortsComponents
import com.rhdevs.rhpatch.morphe.youtube.layout.shortsnoresume.DisableShortsResumingOnStartup
import com.rhdevs.rhpatch.morphe.youtube.layout.sponsorblock.SponsorBlock
import com.rhdevs.rhpatch.morphe.youtube.layout.thumbnails.AlternativeThumbnailsPatch
import com.rhdevs.rhpatch.morphe.youtube.layout.thumbnails.BypassImageRegionRestrictionsPatch
import com.rhdevs.rhpatch.morphe.youtube.misc.backgroundplayback.BackgroundPlayback
import com.rhdevs.rhpatch.morphe.youtube.misc.debugging.EnableDebugging
import com.rhdevs.rhpatch.morphe.youtube.misc.privacy.SanitizeSharingLinks
import com.rhdevs.rhpatch.morphe.youtube.misc.settings.SettingsHook
import com.rhdevs.rhpatch.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import com.rhdevs.rhpatch.morphe.youtube.video.audio.ForceOriginalAudio
import com.rhdevs.rhpatch.morphe.youtube.video.codecs.DisableVideoCodecs
import com.rhdevs.rhpatch.morphe.youtube.video.quality.VideoQuality
import com.rhdevs.rhpatch.morphe.youtube.video.speed.PlaybackSpeed
import com.rhdevs.rhpatch.patch
import org.luckypray.dexkit.wrap.DexMethod

val ExtensionHook = patch(name = "<ExtensionHook>") {
    injectHostClassLoaderToSelf(this::class.java.classLoader!!, classLoader)
    injectSelfClassLoaderToHost(this::class.java.classLoader!!, classLoader)
    DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onCreate(Landroid/os/Bundle;)V").hookMethod {
        before {
            val mainActivity = it.thisObject as Activity
            mainActivity.addModuleAssets()
            Utils.setContext(mainActivity)
        }
    }

    ExtensionResourceHook.run(this)
}

val YouTubePatches = arrayOf(
    ExtensionHook,
    BackgroundPlayback,
    SanitizeSharingLinks,
    HideAds,
    SponsorBlock,
    CopyVideoLinkButtonPatch,
    Downloads,
    HideShortsComponents,
    DisableShortsResumingOnStartup,
    NavigationBar,
    SwipeControls,
    VideoQuality,
    HideLayoutComponents,
    HideVideoActionButtons,
    PlaybackSpeed,
    AutoCaptions,
    EnableDebugging,
    ForceOriginalAudio,
    DisableVideoCodecs,
    AlternativeThumbnailsPatch,
    BypassImageRegionRestrictionsPatch,
    CheckRecycleBitmapMediaSession,
    // make sure settingsHook at end to build preferences
    SettingsHook
)
