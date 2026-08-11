package com.rhdevs.rhpatch.revanced.meta

import com.rhdevs.rhpatch.revanced.meta.ads.HideAds
import com.rhdevs.rhpatch.revanced.meta.download.InstagramDownload
import com.rhdevs.rhpatch.revanced.meta.flags.HookFlagsPatch
import com.rhdevs.rhpatch.revanced.meta.distractionFree.DistractionFreePatches

import com.rhdevs.rhpatch.revanced.meta.feed.HideSuggestedContent
import com.rhdevs.rhpatch.revanced.meta.privacy.GhostModePatch

val MetaPatches = arrayOf(HideAds, InstagramDownload, HookFlagsPatch, HideSuggestedContent, GhostModePatch, *DistractionFreePatches)

val ThreadsPatches = arrayOf(HideAds)
