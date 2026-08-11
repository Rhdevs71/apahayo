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
    AppPatchInfo("Threads", "com.instagram.barcelona", com.rhdevs.rhpatch.revanced.meta.ThreadsPatches),
    AppPatchInfo("TikTok", "com.zhiliaoapp.musically", emptyArray()),
    AppPatchInfo("TikTok (Play Store)", "com.ss.android.ugc.trill", emptyArray()),
    AppPatchInfo("TikTok (Global)", "com.ss.android.ugc.aweme", emptyArray()),
    AppPatchInfo("Strava", "com.strava", StravaPatches),
    AppPatchInfo("AllTrails", "com.alltrails.alltrails", AllTrailsPatches),
    AppPatchInfo("1.1.1.1", "com.cloudflare.onedotonedotonedotone", CloudflarePatches),
    AppPatchInfo("Discord", "com.discord", com.rhdevs.rhpatch.revanced.discord.DiscordPatches),
    AppPatchInfo("Facebook", "com.facebook.katana", com.rhdevs.rhpatch.revanced.facebook.FacebookPatches),
    AppPatchInfo("Facebook Lite", "com.facebook.lite", com.rhdevs.rhpatch.revanced.facebook.FacebookPatches),
)

val patchesByPackage = appPatchConfigurations.associate { it.packageName to it.patches }
