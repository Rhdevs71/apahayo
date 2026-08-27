package com.rhdevs.rhpatch.tiktok.feed

import de.robv.android.xposed.XC_MethodHook
import com.rhdevs.rhpatch.patch

val FeedFilter = patch(
    name = "Feed Filter (Ads, Live, Images)",
) {
    ::fetchFeedListMethod.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val result = param.result ?: return
            
            // Check if result is FeedItemList
            if (result.javaClass.name.endsWith("FeedItemList")) {
                try {
                    val itemsField = result.javaClass.getDeclaredField("items")
                    itemsField.isAccessible = true
                    val items = itemsField.get(result) as? MutableList<Any> ?: return
                    
                    val iterator = items.iterator()
                    while (iterator.hasNext()) {
                        val aweme = iterator.next() ?: continue
                        val awemeClass = aweme.javaClass
                        
                        var shouldRemove = false
                        
                        // Check Ads
                        try {
                            val isAdMethod = awemeClass.getMethod("isAd")
                            val isAd = isAdMethod.invoke(aweme) as? Boolean ?: false
                            val isPromoMethod = awemeClass.getMethod("isWithPromotionalMusic")
                            val isPromo = isPromoMethod.invoke(aweme) as? Boolean ?: false
                            if (isAd || isPromo) shouldRemove = true
                        } catch (e: Exception) { }
                        
                        // Check Live
                        try {
                            val isLiveMethod = awemeClass.getMethod("isLive")
                            val isLive = isLiveMethod.invoke(aweme) as? Boolean ?: false
                            val isLiveReplayMethod = awemeClass.getMethod("isLiveReplay")
                            val isLiveReplay = isLiveReplayMethod.invoke(aweme) as? Boolean ?: false
                            if (isLive || isLiveReplay) shouldRemove = true
                        } catch (e: Exception) { }
                        
                        // Check Images
                        try {
                            val getImagesMethod = awemeClass.getMethod("getImages")
                            val images = getImagesMethod.invoke(aweme)
                            if (images != null) shouldRemove = true
                        } catch (e: Exception) { }
                        
                        if (shouldRemove) {
                            iterator.remove()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore reflection errors on incompatible versions
                }
            }
        }
    })
}
