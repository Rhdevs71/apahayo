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
            // 1. Hooking Java Model Level (FeedItemList & FollowFeedList)
            val modelClasses = listOf(
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                "com.ss.android.ugc.aweme.follow.presenter.FollowFeedList"
            )
            
            fun filterAwemeObject(aweme: Any): Boolean {
                try {
                    val awemeType = try {
                        val field = aweme.javaClass.getField("awemeType")
                        field.getInt(aweme)
                    } catch (_: Throwable) {
                        try {
                            val method = aweme.javaClass.getMethod("getAwemeType")
                            (method.invoke(aweme) as? Number)?.toInt() ?: -1
                        } catch (_: Throwable) { -1 }
                    }

                    // 1. Remove Ads
                    if (removeAds) {
                        try {
                            val isAd = aweme.javaClass.getMethod("isAd").invoke(aweme) as? Boolean
                            if (isAd == true) return false
                        } catch (_: Throwable) {}
                        if (awemeType == 13 || awemeType == 1001) return false
                    }

                    // 2. Hide Shop
                    if (hideShop) {
                        if (awemeType == 60) return false
                    }

                    // 3. Hide Live
                    if (hideLive) {
                        if (awemeType == 101) return false
                        try {
                            val liveId = (aweme.javaClass.getMethod("getLiveId").invoke(aweme) as? Number)?.toLong() ?: 0L
                            if (liveId > 0L) return false
                        } catch (_: Throwable) {}
                        try {
                            val isLiveReplay = aweme.javaClass.getMethod("isLiveReplay").invoke(aweme) as? Boolean
                            if (isLiveReplay == true) return false
                        } catch (_: Throwable) {}
                    }

                    // 4. Hide Story
                    if (hideStory) {
                        if (awemeType == 40 || awemeType == 150) return false
                        try {
                            val isStory = aweme.javaClass.getMethod("getIsTikTokStory").invoke(aweme) as? Boolean
                            if (isStory == true) return false
                        } catch (_: Throwable) {}
                    }

                    // 5. Hide Image / Photo Posts
                    if (hideImage) {
                        if (awemeType == 68 || awemeType == 150) return false
                        try {
                            val imageInfos = aweme.javaClass.getMethod("getImageInfos").invoke(aweme) as? List<*>
                            if (imageInfos != null && imageInfos.isNotEmpty()) return false
                        } catch (_: Throwable) {}
                        try {
                            val photoInfo = aweme.javaClass.getMethod("getPhotoModeImageInfo").invoke(aweme)
                            if (photoInfo != null) return false
                        } catch (_: Throwable) {}
                        try {
                            val photoText = aweme.javaClass.getMethod("getPhotoModeTextInfo").invoke(aweme)
                            if (photoText != null) return false
                        } catch (_: Throwable) {}
                    }

                    // 6. Filter by Views & Likes
                    if (minViews != -1L || maxViews != -1L || minLikes != -1L || maxLikes != -1L) {
                        try {
                            val stats = aweme.javaClass.getMethod("getStatistics").invoke(aweme)
                            if (stats != null) {
                                val playCount = (stats.javaClass.getMethod("getPlayCount").invoke(stats) as? Number)?.toLong() ?: 0L
                                val diggCount = (stats.javaClass.getMethod("getDiggCount").invoke(stats) as? Number)?.toLong() ?: 0L
                                
                                if (minViews != -1L && playCount < minViews) return false
                                if (maxViews != -1L && playCount > maxViews) return false
                                if (minLikes != -1L && diggCount < minLikes) return false
                                if (maxLikes != -1L && diggCount > maxLikes) return false
                            }
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
                return true
            }

            fun filterListContainer(list: Any?) {
                if (list !is MutableList<*>) return
                try {
                    val it = list.iterator()
                    while (it.hasNext()) {
                        val item = it.next() ?: continue
                        var awemeObj: Any? = item
                        
                        // Check if item is FollowFeed wrapper
                        if (item.javaClass.name.contains("FollowFeed")) {
                            try {
                                val field = item.javaClass.getDeclaredField("aweme")
                                field.isAccessible = true
                                awemeObj = field.get(item)
                            } catch (_: Throwable) {
                                try {
                                    val method = item.javaClass.getMethod("getAweme")
                                    awemeObj = method.invoke(item)
                                } catch (_: Throwable) {}
                            }
                        }
                        
                        if (awemeObj != null && !filterAwemeObject(awemeObj)) {
                            it.remove()
                        }
                    }
                } catch (_: Throwable) {}
            }

            for (modelClassName in modelClasses) {
                try {
                    val modelClass = classLoader.loadClass(modelClassName)
                    for (method in modelClass.declaredMethods) {
                        if (method.returnType == java.util.List::class.java || java.util.List::class.java.isAssignableFrom(method.returnType)) {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    filterListContainer(param.result)
                                }
                            })
                        }
                        if (method.name == "setItems" && method.parameterTypes.isNotEmpty() && java.util.List::class.java.isAssignableFrom(method.parameterTypes[0])) {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    filterListContainer(param.args[0])
                                }
                            })
                        }
                    }
                } catch (_: Throwable) {}
            }

            // 2. Hooking org.json.JSONObject constructor as secondary fallback
            XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val json = param.thisObject as JSONObject
                        
                        val listKeys = listOf("aweme_list", "data", "items")
                        for (listKey in listKeys) {
                            if (json.has(listKey)) {
                                val awemeList = json.optJSONArray(listKey)
                                if (awemeList != null && awemeList.length() > 0) {
                                    val filteredList = JSONArray()
                                    for (i in 0 until awemeList.length()) {
                                        val item = awemeList.optJSONObject(i)
                                        if (item != null) {
                                            var shouldKeep = true
                                            val awemeType = item.optInt("aweme_type", -1)
                                            
                                            // 1. Remove Ads
                                            if (removeAds && (
                                                item.optBoolean("is_ads", false) || 
                                                item.has("ad_info") || 
                                                item.has("ad_aweme_source") || 
                                                item.has("raw_ad_data") || 
                                                item.has("advertisement_info") ||
                                                awemeType == 13 || awemeType == 1001
                                            )) {
                                                shouldKeep = false
                                            }
                                            
                                            // 2. Hide Shop & Commerce
                                            if (hideShop && (
                                                item.has("commerce_info") || 
                                                item.has("product_info") || 
                                                item.has("anchor_info") || 
                                                item.has("anchors") || 
                                                item.has("cart_info") || 
                                                item.has("shop_info") ||
                                                awemeType == 60
                                            )) {
                                                shouldKeep = false
                                            }
                                            
                                            // 3. Hide Livestreams
                                            if (hideLive && (
                                                awemeType == 101 || 
                                                item.has("room") || 
                                                item.has("new_live_room_data") || 
                                                item.has("cached_live_room_struct") || 
                                                item.has("room_feed_cell_struct") || 
                                                item.has("mRoomFeedCellStruct") || 
                                                item.has("live_reason") || 
                                                item.optLong("live_id", 0L) > 0L || 
                                                item.optString("live_type", "").isNotEmpty()
                                            )) {
                                                shouldKeep = false
                                            }
                                            
                                            // 4. Hide Story
                                            if (hideStory && (
                                                awemeType == 40 || 
                                                awemeType == 150 || 
                                                item.optBoolean("is_tiktok_story", false) || 
                                                item.optBoolean("is_story", false) || 
                                                item.has("story") || 
                                                item.has("story_group")
                                            )) {
                                                shouldKeep = false
                                            }
                                            
                                            // 5. Hide Image / Photo Posts
                                            if (hideImage && (
                                                awemeType == 68 || 
                                                awemeType == 150 || 
                                                item.has("image_infos") || 
                                                item.has("image_post_info") || 
                                                item.has("photo_mode_image_info") || 
                                                item.has("photo_mode_text_info") || 
                                                item.has("images")
                                            )) {
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
                                        } else {
                                            // Keep non-JSONObject items
                                            filteredList.put(awemeList.get(i))
                                        }
                                    }
                                    json.put(listKey, filteredList)
                                }
                            }
                        }
                    } catch (_: Throwable) {
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
