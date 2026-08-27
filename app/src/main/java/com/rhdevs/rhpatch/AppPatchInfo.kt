package com.rhdevs.rhpatch

import com.rhdevs.rhpatch.hoodles.morphe.alltrails.AllTrailsPatches
import com.rhdevs.rhpatch.youtube.music.YTMusicPatches
import com.rhdevs.rhpatch.youtube.reddit.RedditPatches
import com.rhdevs.rhpatch.youtube.youtube.YouTubePatches
import com.rhdevs.rhpatch.cloudflare.CloudflarePatches
import com.rhdevs.rhpatch.googlephotos.GooglePhotosPatches
import com.rhdevs.rhpatch.meta.MetaPatches
import com.rhdevs.rhpatch.photomath.PhotomathPatches
import com.rhdevs.rhpatch.strava.StravaPatches

import com.rhdevs.rhpatch.duolingo.DuolingoPatches
import com.rhdevs.rhpatch.camscanner.CamScannerPatches
import com.rhdevs.rhpatch.lightroom.LightroomPatches
import com.rhdevs.rhpatch.ibispaint.IbisPaintPatches

class AppPatchInfo(val appName: String, val packageName: String, val patches: Array<Patch>)

val appPatchConfigurations = listOf(
    AppPatchInfo("YouTube", "com.google.android.youtube", YouTubePatches),
    AppPatchInfo("YT Music", "com.google.android.apps.youtube.music", YTMusicPatches),
    AppPatchInfo("Reddit", "com.reddit.frontpage", RedditPatches),
    AppPatchInfo("Google Photos", "com.google.android.apps.photos", GooglePhotosPatches),
    AppPatchInfo("Photomath", "com.microblink.photomath", PhotomathPatches),
    AppPatchInfo("Instagram", "com.instagram.android", MetaPatches),
    AppPatchInfo("Instagram Lite", "com.instagram.lite", com.rhdevs.rhpatch.metalite.MetaLitePatches),
    AppPatchInfo("Threads", "com.instagram.barcelona", com.rhdevs.rhpatch.meta.ThreadsPatches),
    AppPatchInfo("TikTok", "com.zhiliaoapp.musically", com.rhdevs.rhpatch.tiktok.TikTokPatches),
    AppPatchInfo("TikTok (Play Store)", "com.ss.android.ugc.trill", com.rhdevs.rhpatch.tiktok.TikTokPatches),
    AppPatchInfo("TikTok (Global)", "com.ss.android.ugc.aweme", com.rhdevs.rhpatch.tiktok.TikTokPatches),
    AppPatchInfo("Duolingo", "com.duolingo", DuolingoPatches),
    AppPatchInfo("CamScanner", "com.intsig.camscanner", CamScannerPatches),
    AppPatchInfo("Adobe Lightroom", "com.adobe.lrmobile", LightroomPatches),
    AppPatchInfo("IbisPaint X", "jp.ne.ibis.ibispaintx.app", IbisPaintPatches),
    AppPatchInfo("Strava", "com.strava", StravaPatches),
    AppPatchInfo("AllTrails", "com.alltrails.alltrails", AllTrailsPatches),
    AppPatchInfo("1.1.1.1", "com.cloudflare.onedotonedotonedotone", CloudflarePatches),
    AppPatchInfo("Discord", "com.discord", com.rhdevs.rhpatch.discord.DiscordPatches),
    AppPatchInfo("Facebook", "com.facebook.katana", com.rhdevs.rhpatch.facebook.FacebookPatches),
    AppPatchInfo("Facebook Lite", "com.facebook.lite", com.rhdevs.rhpatch.facebook.FacebookPatches),
    AppPatchInfo("Messenger", "com.facebook.orca", com.rhdevs.rhpatch.messenger.MessengerPatches),
    AppPatchInfo("Twitch", "tv.twitch.android.app", com.rhdevs.rhpatch.twitch.TwitchPatches),
    AppPatchInfo("Getcontact", "app.source.getcontact", com.rhdevs.rhpatch.getcontact.GetcontactPatches),
    AppPatchInfo("Getcontact (Alt)", "com.getcontact.app", com.rhdevs.rhpatch.getcontact.GetcontactPatches),
    AppPatchInfo("KineMaster", "com.nexstreaming.app.kinemasterfree", com.rhdevs.rhpatch.kinemaster.KineMasterPatches),
    AppPatchInfo("Sticker.ly", "com.snowcorp.stickerly.android", com.rhdevs.rhpatch.stickerly.StickerlyPatches),
    AppPatchInfo("SD Maid SE", "eu.darken.sdmse", com.rhdevs.rhpatch.sdmaid.SdMaidPatches),
    AppPatchInfo("WolframAlpha", "com.wolfram.android.alpha", com.rhdevs.rhpatch.wolfram.WolframAlphaPatches),
    AppPatchInfo("Kahoot!", "com.kahoot.android", com.rhdevs.rhpatch.kahoot.KahootPatches),
    AppPatchInfo("Adobe Scan", "com.adobe.scan.android", com.rhdevs.rhpatch.adobescan.AdobeScanPatches),
    AppPatchInfo("MangaPlus", "jp.co.shueisha.mangaplus", com.rhdevs.rhpatch.mangaplus.MangaPlusPatches),
    AppPatchInfo("Speedtest", "org.zwanoo.android.speedtest", com.rhdevs.rhpatch.speedtest.SpeedtestPatches),
    AppPatchInfo("AccuBattery", "com.digibites.accubattery", com.rhdevs.rhpatch.accubattery.AccuBatteryPatches),
    AppPatchInfo("MacroDroid", "com.arlosoft.macrodroid", com.rhdevs.rhpatch.macrodroid.MacroDroidPatches),
    AppPatchInfo("RAR", "com.rarlab.rar", com.rhdevs.rhpatch.rar.RarPatches),
    AppPatchInfo("Telegram", "org.telegram.messenger", com.rhdevs.rhpatch.telegram.TelegramPatches),
    AppPatchInfo("Telegram (Direct)", "org.telegram.messenger.web", com.rhdevs.rhpatch.telegram.TelegramPatches),
    AppPatchInfo("Plus Messenger", "org.telegram.plus", com.rhdevs.rhpatch.telegram.TelegramPatches),
    AppPatchInfo("TikTok Lite", "com.zhiliaoapp.musically.go", com.rhdevs.rhpatch.tiktoklite.TikTokLitePatches),
    AppPatchInfo("TikTok Lite (Trill)", "com.ss.android.ugc.trill.go", com.rhdevs.rhpatch.tiktoklite.TikTokLitePatches),
)

val patchesByPackage = appPatchConfigurations.associate { it.packageName to it.patches }
