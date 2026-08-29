package com.rhdevs.rhpatch.tiktok.misc

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import com.rhdevs.rhpatch.hookMethod
import android.net.Uri

val ShareUrlTrackerFingerprint = findMethodDirect(
    fingerprint {
        returns("Ljava/lang/String;")
        strings("utm_campaign", "share_link_id")
        accessFlags(AccessFlags.STATIC)
    }
)

val SanitizeShareUrlsPatch = patch(
    name = "Sanitize sharing links",
    description = "Removes tracking parameters from shared links."
) {
    runCatching {
        ::ShareUrlTrackerFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val originalUrl = param.result as? String ?: return
                
                // Strip all query params
                try {
                    val uri = Uri.parse(originalUrl)
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        val sanitized = uri.buildUpon().clearQuery().build().toString()
                        param.result = sanitized
                    }
                } catch (e: Exception) {
                    XposedBridge.log("Rhpatch: [TikTok Share Sanitize] Failed to parse URL: $e")
                }
            }
        })
        XposedBridge.log("Rhpatch: [TikTok] Share URL Sanitizer hook installed")
    }.onFailure {
        XposedBridge.log("Rhpatch: [TikTok] Share URL Sanitizer failed: $it")
    }
}
