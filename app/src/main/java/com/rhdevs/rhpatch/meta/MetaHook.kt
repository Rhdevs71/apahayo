package com.rhdevs.rhpatch.meta

import com.rhdevs.rhpatch.meta.ads.HideAds
import com.rhdevs.rhpatch.meta.download.MediaDownloaderPatch
import com.rhdevs.rhpatch.meta.reels.ReelsDownloaderPatch
import com.rhdevs.rhpatch.meta.distractionFree.DistractionFreePatches
import com.rhdevs.rhpatch.meta.privacy.GhostModePatch
import com.rhdevs.rhpatch.meta.settings.ProfileSettingsPatch
import com.rhdevs.rhpatch.meta.privacy.EphemeralMediaPatch
import com.rhdevs.rhpatch.meta.misc.UnlockPlusBenefitsPatch
import com.rhdevs.rhpatch.meta.misc.MiscPatches
import com.rhdevs.rhpatch.meta.stories.StoriesReelsPatches

val MetaPatches = arrayOf(
    com.rhdevs.rhpatch.meta.privacy.MarkAsReadPatch,
    HideAds,
    MediaDownloaderPatch,
    ReelsDownloaderPatch,
    GhostModePatch,
    ProfileSettingsPatch,
    EphemeralMediaPatch,
    UnlockPlusBenefitsPatch,
    *DistractionFreePatches,
    *StoriesReelsPatches,
    *MiscPatches
)
val ThreadsPatches = arrayOf(HideAds)
