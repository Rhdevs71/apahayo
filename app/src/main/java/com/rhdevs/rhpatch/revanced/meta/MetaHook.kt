package com.rhdevs.rhpatch.revanced.meta

import com.rhdevs.rhpatch.revanced.meta.ads.HideAds
import com.rhdevs.rhpatch.revanced.meta.download.MediaDownloaderPatch
import com.rhdevs.rhpatch.revanced.meta.flags.HookFlagsPatch
import com.rhdevs.rhpatch.revanced.meta.distractionFree.DistractionFreePatches
import com.rhdevs.rhpatch.revanced.meta.feed.HideSuggestedContent
import com.rhdevs.rhpatch.revanced.meta.privacy.GhostModePatch
import com.rhdevs.rhpatch.revanced.meta.settings.ProfileSettingsPatch
import com.rhdevs.rhpatch.revanced.meta.privacy.EphemeralMediaPatch
import com.rhdevs.rhpatch.revanced.meta.misc.UnlockPlusBenefitsPatch
import com.rhdevs.rhpatch.revanced.meta.stories.StoriesReelsPatches
import com.rhdevs.rhpatch.revanced.meta.misc.DeveloperOptionsPatch
import com.rhdevs.rhpatch.revanced.meta.misc.MiscPatches

val MetaPatches = arrayOf(HideAds, MediaDownloaderPatch, HookFlagsPatch, HideSuggestedContent, GhostModePatch, ProfileSettingsPatch, EphemeralMediaPatch, UnlockPlusBenefitsPatch, DeveloperOptionsPatch, *DistractionFreePatches, *StoriesReelsPatches, *MiscPatches)
val ThreadsPatches = arrayOf(HideAds)
