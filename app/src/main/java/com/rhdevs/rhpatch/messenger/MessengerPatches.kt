package com.rhdevs.rhpatch.messenger

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val MessengerFeaturesPatch = patch(
    name = "Messenger Enhancements (Ghost Mode & No Ads)",
    description = "Menyembunyikan iklan, indikator mengetik (Ghost Mode), Meta AI, dan membuka tautan eksternal"
) {
    // 1. Hide Inbox Ads
    runCatching {
        val threadItemCls = XposedHelpers.findClassIfExists("com.facebook.messaging.inbox.units.InboxTrackableItem", classLoader)
        if (threadItemCls != null) {
            for (method in threadItemCls.declaredMethods) {
                if (method.name.lowercase().contains("isad") && method.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                }
            }
        }
    }

    // 2. Disable Typing Indicator (Ghost Mode)
    runCatching {
        val typingCls = XposedHelpers.findClassIfExists("com.facebook.messaging.typingattribution.TypingAttributionSender", classLoader)
        if (typingCls != null) {
            for (method in typingCls.declaredMethods) {
                if (method.name.lowercase().contains("sendtyping") || method.name.lowercase().contains("starttyping")) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.DO_NOTHING)
                }
            }
        }
    }

    // 3. Remove Meta AI from composer
    runCatching {
        val metaAiCls = XposedHelpers.findClassIfExists("com.facebook.messaging.metaai.composer.MetaAiComposerGating", classLoader)
        if (metaAiCls != null) {
            for (method in metaAiCls.declaredMethods) {
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                }
            }
        }
    }
}

val MessengerPatches = arrayOf(MessengerFeaturesPatch)
