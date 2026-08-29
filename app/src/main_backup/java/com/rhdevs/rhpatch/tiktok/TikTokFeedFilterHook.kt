package com.rhdevs.rhpatch.tiktok

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.json.JSONObject
import org.json.JSONArray

object TikTokFeedFilterHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        
        
        val removeAds = prefs.getBoolean("tiktok_remove_ads", false)
        val hideShop = prefs.getBoolean("tiktok_hide_shop", false)
        val hideLive = prefs.getBoolean("tiktok_hide_live", false)
        val hideStory = prefs.getBoolean("tiktok_hide_story", false)
        val hideImage = prefs.getBoolean("tiktok_hide_image", false)
        
        val viewsStr = prefs.getString("tiktok_min_max_views", "") ?: ""
        val likesStr = prefs.getString("tiktok_min_max_likes", "") ?: ""
        
        var minViews = -1L
        var maxViews = -1L
        var minLikes = -1L
        var maxLikes = -1L
        
        if (viewsStr.contains("-")) {
            val parts = viewsStr.split("-")
            minViews = parts.getOrNull(0)?.toLongOrNull() ?: -1L
            maxViews = parts.getOrNull(1)?.toLongOrNull() ?: -1L
        }
        
        if (likesStr.contains("-")) {
            val parts = likesStr.split("-")
            minLikes = parts.getOrNull(0)?.toLongOrNull() ?: -1L
            maxLikes = parts.getOrNull(1)?.toLongOrNull() ?: -1L
        }
        
        if (!removeAds && !hideShop && !hideLive && !hideStory && !hideImage && minViews == -1L && maxViews == -1L && minLikes == -1L && maxLikes == -1L) return
        
        try {
            // Hooking org.json.JSONObject constructor to intercept Aweme JSON responses
            XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val json = param.thisObject as JSONObject
                        
                        // Check if this JSON object represents an Aweme feed list
                        if (json.has("aweme_list")) {
                            val awemeList = json.optJSONArray("aweme_list")
                            if (awemeList != null) {
                                val filteredList = JSONArray()
                                for (i in 0 until awemeList.length()) {
                                    val item = awemeList.optJSONObject(i)
                                    if (item != null) {
                                        var shouldKeep = true
                                        
                                        // 1. Remove Ads
                                        if (removeAds && (item.optBoolean("is_ads", false) || item.has("ad_info") || item.has("ad_aweme_source"))) {
                                            shouldKeep = false
                                        }
                                        
                                        // 2. Hide Shop (usually has anchor_info with type 4 or commerce_info)
                                        if (hideShop && (item.has("commerce_info") || item.has("product_info"))) {
                                            shouldKeep = false
                                        }
                                        
                                        // 3. Hide Livestreams (aweme_type == 101 or has room_info)
                                        if (hideLive && (item.optInt("aweme_type") == 101 || item.has("room"))) {
                                            shouldKeep = false
                                        }
                                        
                                        // 4. Hide Story (aweme_type == 40 or 150)
                                        if (hideStory && (item.optInt("aweme_type") == 40 || item.optInt("aweme_type") == 150 || item.has("story"))) {
                                            shouldKeep = false
                                        }
                                        
                                        // 5. Hide Image (aweme_type == 68 or 150)
                                        if (hideImage && item.optInt("aweme_type") == 68) {
                                            shouldKeep = false
                                        }
                                        
                                        // 6. Filter by Views & Likes
                                        val stats = item.optJSONObject("statistics")
                                        if (stats != null) {
                                            val playCount = stats.optLong("play_count", 0)
                                            val diggCount = stats.optLong("digg_count", 0)
                                            
                                            if (minViews != -1L && playCount < minViews) shouldKeep = false
                                            if (maxViews != -1L && playCount > maxViews) shouldKeep = false
                                            if (minLikes != -1L && diggCount < minLikes) shouldKeep = false
                                            if (maxLikes != -1L && diggCount > maxLikes) shouldKeep = false
                                        }
                                        
                                        if (shouldKeep) {
                                            filteredList.put(item)
                                        }
                                    }
                                }
                                json.put("aweme_list", filteredList)
                            }
                        }
                    } catch (e: Throwable) {
                        // Ignore JSON parsing errors during hook
                    }
                }
            })
            XposedBridge.log("Rhpatch TikTok: Feed Filter hooks applied successfully.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok Feed Filter Error: ${e.message}")
        }
    }
}
