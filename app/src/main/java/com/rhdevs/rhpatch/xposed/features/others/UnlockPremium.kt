package com.rhdevs.rhpatch.xposed.features.others

import android.app.Activity
import android.content.SharedPreferences
import com.rhdevs.rhpatch.xposed.core.Feature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class UnlockPremium(classLoader: ClassLoader, preferences: SharedPreferences) :
    Feature(classLoader, preferences) {

    override fun doHook() {
        // Default to true so WA Plus works out-of-the-box
        if (!prefs.getBoolean("pref_wa_premium", true)) return

        XposedBridge.log("Rhpatch: [UnlockPremium] Initializing WhatsApp Plus (Meta Nova and Consumer Subscriptions)...")

        // 1. Hook Meta One Global Subscription Checkers: LX/1oA
        runCatching {
            val oaClass = XposedHelpers.findClassIfExists("LX.1oA", classLoader)
            if (oaClass != null) {
                for (m in oaClass.declaredMethods) {
                    if (m.name == "A00" && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPremium] Hooked LX.1oA.A00 -> META_ONE Active Subscription: true")
                    }
                    if (m.name == "A01" && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPremium] Hooked LX.1oA.A01 -> Promo Eligibility: true")
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook 1oA: ${it.message}") }

        // 2. Hook Subscription Status Holder: LX/1oG
        runCatching {
            val ogClass = XposedHelpers.findClassIfExists("LX.1oG", classLoader)
            if (ogClass != null) {
                for (m in ogClass.declaredMethods) {
                    if ((m.name == "BLf" || m.name == "BM1" || m.name == "A01") && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPremium] Hooked LX.1oG.${m.name} -> Subscriber Active: true")
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook 1oG: ${it.message}") }

        // 3. Hook Official Meta Nova Controller: com.whatsapp.nova.manager.PromoEligibilityManager
        runCatching {
            val promoManagerClass = XposedHelpers.findClassIfExists("com.whatsapp.nova.manager.PromoEligibilityManager", classLoader)
            if (promoManagerClass != null) {
                for (m in promoManagerClass.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE || m.returnType == java.lang.Boolean::class.java) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPremium] Hooked PromoEligibilityManager.${m.name} -> true")
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook PromoEligibilityManager: ${it.message}") }

        // 4. Hook Consumer Subscription Manager
        runCatching {
            val subManagerClass = XposedHelpers.findClassIfExists("com.whatsapp.subscriptionmanagement.consumer.manager.ConsumerSubscriptionManager", classLoader)
            if (subManagerClass != null) {
                for (m in subManagerClass.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE || m.returnType == java.lang.Boolean::class.java) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                    }
                }
                XposedBridge.log("Rhpatch: [UnlockPremium] Hooked ConsumerSubscriptionManager boolean methods -> true")
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook ConsumerSubscriptionManager: ${it.message}") }

        // 5. Hook Consumer Subscription Eligibility Manager
        runCatching {
            val subEligClass = XposedHelpers.findClassIfExists("com.whatsapp.subscriptionmanagement.consumer.manager.ConsumerSubscriptionEligibilityManager", classLoader)
            if (subEligClass != null) {
                for (m in subEligClass.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE || m.returnType == java.lang.Boolean::class.java) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                    }
                }
                XposedBridge.log("Rhpatch: [UnlockPremium] Hooked ConsumerSubscriptionEligibilityManager boolean methods -> true")
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook ConsumerSubscriptionEligibilityManager: ${it.message}") }

        // 6. Hook Nova Benefit Enums (LX/30G, LX/30g, and LX/0mU)
        runCatching {
            val enumClassNames = arrayOf("LX.30G", "LX.30g", "LX.0mU")
            for (name in enumClassNames) {
                val gClass = XposedHelpers.findClassIfExists(name, classLoader)
                if (gClass != null) {
                    for (m in gClass.declaredMethods) {
                        if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty()) {
                            XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        }
                    }
                    XposedBridge.log("Rhpatch: [UnlockPremium] Hooked $name Nova Benefit model methods")
                }
            }
        }

        // 7. Suppress Subscription Hub Upsell Activity (ConsumerSubscriptionHubActivity)
        runCatching {
            val hubClass = XposedHelpers.findClassIfExists("com.whatsapp.subscriptionui.consumer.bloks.ConsumerSubscriptionHubActivity", classLoader)
            if (hubClass != null) {
                XposedHelpers.findAndHookMethod(
                    hubClass,
                    "onCreate",
                    android.os.Bundle::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val act = param.thisObject as? Activity
                            act?.finish()
                            XposedBridge.log("Rhpatch: [UnlockPremium] Suppressed ConsumerSubscriptionHubActivity")
                        }
                    }
                )
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPremium] Failed to hook ConsumerSubscriptionHubActivity: ${it.message}") }

        // 8. Hook Nova Benefits via Dynamic Method Discovery
        runCatching {
            val targetStrings = arrayOf(
                "CUSTOM_APP_THEME", "CUSTOM_APP_ICON", "PIN_MORE_CHATS",
                "ENHANCED_LISTS", "PREMIUM_STICKERS", "CUSTOM_RINGTONES",
                "wa_plus_custom_app_theme", "wa_plus_custom_app_icon",
                "meta_subs_benefit_wa_custom_app_theme_upsell",
                "meta_subs_benefit_wa_custom_app_icon_upsell",
                "meta_subs_wa_meta_plus_onboarding",
                "is_benefit_active", "is_nova_subscriber", "refreshEligibility"
            )
            val hookedClasses = mutableSetOf<String>()
            for (str in targetStrings) {
                try {
                    val methods = com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator.findAllMethodUsingStrings(
                        classLoader,
                        org.luckypray.dexkit.query.enums.StringMatchType.Contains,
                        str
                    )
                    for (method in methods) {
                        val cls = method.declaringClass
                        if (hookedClasses.contains(cls.name)) continue
                        hookedClasses.add(cls.name)

                        for (cm in cls.declaredMethods) {
                            if (cm.returnType == java.lang.Boolean.TYPE || cm.returnType == java.lang.Boolean::class.java) {
                                runCatching {
                                    XposedBridge.hookMethod(cm, XC_MethodReplacement.returnConstant(true))
                                    XposedBridge.log("Rhpatch: [UnlockPremium] Unlocked ${cls.name}.${cm.name} via '$str'")
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        XposedBridge.log("Rhpatch: [UnlockPremium] WhatsApp Plus and Nova hooks successfully initialized.")
    }

    override fun getPluginName(): String {
        return "Unlock Plus & Premium"
    }
}
