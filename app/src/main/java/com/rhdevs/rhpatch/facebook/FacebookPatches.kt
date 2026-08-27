package com.rhdevs.rhpatch.facebook

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val FacebookHideAdsPatch = patch(
    name = "Facebook Feed & Story AdBlocker",
    description = "Menghilangkan postingan bersponsor, unit iklan multi-feed, dan iklan story di Facebook"
) {
    // 1. Hook GraphQL Story & Model Layer
    runCatching {
        val graphQLClasses = listOf(
            "com.facebook.graphql.model.GraphQLStory",
            "com.facebook.graphql.model.GraphQLFBMultiAdsFeedUnit",
            "com.facebook.graphql.model.GraphQLSponsoredData",
            "com.facebook.graphql.modelutil.BaseModelWithTree",
            "com.facebook.graphql.model.FeedUnit"
        )
        for (className in graphQLClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                val retType = method.returnType
                
                if (retType == Boolean::class.javaPrimitiveType || retType == java.lang.Boolean::class.java) {
                    if (mName.contains("sponsored") || mName.contains("isad") || mName.contains("promoted") || mName.contains("commercial")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                } else if (mName.contains("sponsoreddata") || mName.contains("adunit") || mName.contains("multiads")) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null))
                }
            }
        }
    }

    // 2. Hook Story Ads Controller & Ad Insertion Runnables
    runCatching {
        val storyAdRunnables = listOf(
            "com.facebook.stories.features.ads.StoryAdsController",
            "com.facebook.katana.ad.AdBucketDataSourceUtil\$attemptAdsInsertion\$1",
            "com.facebook.katana.ad.AdBucketDataSourceUtil\$attemptFetchMoreAds\$1",
            "com.facebook.feed.storyads.StoryAdsPlugin"
        )
        for (className in storyAdRunnables) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                if (method.name == "run" || method.name.contains("attemptAdsInsertion") || method.name.contains("attemptFetchMoreAds")) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null))
                }
            }
        }
    }

    // 3. Recursive View Text Scanner: Sembunyikan View jika memuat teks bersponsor / iklan
    fun isSponsoredView(view: View): Boolean {
        if (view is TextView) {
            val txt = view.text?.toString()?.trim()?.lowercase() ?: ""
            if (txt == "sponsored" || txt == "bersponsor" || txt == "promoted" || txt == "iklan" ||
                txt.contains("suggested for you") || txt.contains("disarankan untuk anda")) {
                return true
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (isSponsoredView(view.getChildAt(i))) return true
            }
        }
        return false
    }

    // 4. Hook Litho and RecyclerView View Binding in Feed Adapters
    runCatching {
        val lithoCls = XposedHelpers.findClassIfExists("com.facebook.litho.LithoView", classLoader)
        if (lithoCls != null) {
            XposedBridge.hookAllMethods(lithoCls, "onAttachedToWindow", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as? View ?: return
                    view.post {
                        if (isSponsoredView(view)) {
                            view.visibility = View.GONE
                            val params = view.layoutParams
                            if (params != null) {
                                params.height = 0
                                params.width = 0
                                view.layoutParams = params
                            }
                        }
                    }
                }
            })
        }

        val adapterCls = XposedHelpers.findClassIfExists("androidx.recyclerview.widget.RecyclerView\$Adapter", classLoader)
        if (adapterCls != null) {
            XposedBridge.hookAllMethods(adapterCls, "onBindViewHolder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val holder = param.args[0] ?: return
                        val itemView = XposedHelpers.getObjectField(holder, "itemView") as? View ?: return
                        val holderClass = holder.javaClass.name.lowercase()
                        
                        val isAd = holderClass.contains("sponsored") || holderClass.contains("adunit") || 
                                   holderClass.contains("multiads") || holderClass.contains("feedad")
                                   
                        if (isAd) {
                            itemView.visibility = View.GONE
                            val params = itemView.layoutParams
                            if (params != null) {
                                params.height = 0
                                params.width = 0
                                itemView.layoutParams = params
                            }
                        } else {
                            itemView.post {
                                if (isSponsoredView(itemView)) {
                                    itemView.visibility = View.GONE
                                    val params = itemView.layoutParams
                                    if (params != null) {
                                        params.height = 0
                                        params.width = 0
                                        itemView.layoutParams = params
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
            })
        }
    }
}

val FacebookPatches = arrayOf(FacebookHideAdsPatch)
