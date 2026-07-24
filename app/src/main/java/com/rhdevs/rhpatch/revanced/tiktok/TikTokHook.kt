package com.rhdevs.rhpatch.revanced.tiktok

import com.rhdevs.rhpatch.revanced.tiktok.download.NoWatermark
import com.rhdevs.rhpatch.revanced.tiktok.feed.FeedFilter
import com.rhdevs.rhpatch.revanced.tiktok.speed.PlaybackSpeed

import com.rhdevs.rhpatch.revanced.tiktok.misc.DisableLoginRequirementPatch
import com.rhdevs.rhpatch.revanced.tiktok.misc.SanitizeShareUrlsPatch

val TikTokPatches = arrayOf(
    NoWatermark,
    FeedFilter,
    PlaybackSpeed,
    DisableLoginRequirementPatch,
    SanitizeShareUrlsPatch
)
