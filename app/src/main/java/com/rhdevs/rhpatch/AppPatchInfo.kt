package com.rhdevs.rhpatch

import com.rhdevs.rhpatch.hoodles.morphe.alltrails.AllTrailsPatches
import com.rhdevs.rhpatch.morphe.music.YTMusicPatches
import com.rhdevs.rhpatch.morphe.reddit.RedditPatches
import com.rhdevs.rhpatch.morphe.youtube.YouTubePatches
import com.rhdevs.rhpatch.revanced.cloudflare.CloudflarePatches
import com.rhdevs.rhpatch.revanced.googlephotos.GooglePhotosPatches
import com.rhdevs.rhpatch.revanced.meta.MetaPatches
import com.rhdevs.rhpatch.revanced.photomath.PhotomathPatches
import com.rhdevs.rhpatch.revanced.strava.StravaPatches

class AppPatchInfo(val appName: String, val packageName: String, val patches: Array<Patch>)

val appPatchConfigurations = listOf(
    AppPatchInfo("YouTube", "com.google.android.youtube", YouTubePatches),
    AppPatchInfo("YT Music", "com.google.android.apps.youtube.music", YTMusicPatches),
    AppPatchInfo("Reddit", "com.reddit.frontpage", RedditPatches),
    AppPatchInfo("Google Photos", "com.google.android.apps.photos", GooglePhotosPatches),
    AppPatchInfo("Photomath", "com.microblink.photomath", PhotomathPatches),
    AppPatchInfo("Instagram", "com.instagram.android", MetaPatches),
    AppPatchInfo("Instagram Lite", "com.instagram.lite", com.rhdevs.rhpatch.revanced.metalite.MetaLitePatches),
    AppPatchInfo("Threads", "com.instagram.barcelona", MetaPatches),
    AppPatchInfo("TikTok", "com.zhiliaoapp.musically", com.rhdevs.rhpatch.revanced.tiktok.TikTokPatches),
    AppPatchInfo("TikTok (Play Store)", "com.ss.android.ugc.trill", com.rhdevs.rhpatch.revanced.tiktok.TikTokPatches),
    AppPatchInfo("Strava", "com.strava", StravaPatches),
    AppPatchInfo("AllTrails", "com.alltrails.alltrails", AllTrailsPatches),
    AppPatchInfo("1.1.1.1", "com.cloudflare.onedotonedotonedotone", CloudflarePatches),
)

val patchesByPackage = appPatchConfigurations.associate { it.packageName to it.patches }
