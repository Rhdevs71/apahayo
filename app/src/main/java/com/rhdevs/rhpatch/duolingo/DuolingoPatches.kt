package com.rhdevs.rhpatch.duolingo

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val DuolingoSuperMaxPatch = patch(
    name = "Duolingo Super/MAX, Unlimited Hearts & Debug Menu",
    description = "Membuka status keanggotaan Super/MAX, fitur latihan AI, nyawa tanpa batas, dan menu debug"
) {
    // 1. Hook User classes (com.duolingo.data.user.User, etc.)
    val userClassNames = listOf(
        "com.duolingo.data.user.User",
        "com.duolingo.user.User",
        "com.duolingo.session.User",
        "com.duolingo.core.user.User"
    )

    fun patchUserObject(userObj: Any) {
        val cls = userObj.javaClass
        var currentCls: Class<*>? = cls
        while (currentCls != null && currentCls != Any::class.java) {
            for (field in currentCls.declaredFields) {
                val fName = field.name.lowercase()
                field.isAccessible = true
                try {
                    if (field.type == Boolean::class.javaPrimitiveType || field.type == java.lang.Boolean::class.java) {
                        if (fName == "hasplus" || fName == "ispaid" || fName == "hasgold" || fName == "hasmax" || fName == "unlimitedhearts" || fName == "p0") {
                            field.set(userObj, true)
                        } else if (fName.contains("eligibleforupsell") || fName.contains("eligibleforsecondaryupsell")) {
                            field.set(userObj, false)
                        }
                    } else if (fName == "subscriberlevel" || fName == "a0") {
                        // Set subscriber level enum or string to GOLD / SUPER / PREMIUM
                        if (field.type.isEnum) {
                            val constants = field.type.enumConstants
                            val targetConst = constants?.firstOrNull { it.toString().contains("GOLD", ignoreCase = true) }
                                ?: constants?.firstOrNull { it.toString().contains("SUPER", ignoreCase = true) }
                                ?: constants?.firstOrNull { it.toString().contains("PREMIUM", ignoreCase = true) }
                            if (targetConst != null) field.set(userObj, targetConst)
                        } else if (field.type == String::class.java) {
                            field.set(userObj, "GOLD")
                        }
                    }
                } catch (_: Throwable) {}
            }
            currentCls = currentCls.superclass
        }
    }

    for (className in userClassNames) {
        runCatching {
            val userCls = XposedHelpers.findClassIfExists(className, classLoader)
            if (userCls != null) {
                XposedBridge.hookAllConstructors(userCls, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        patchUserObject(param.thisObject)
                    }
                })
                // Also hook any getters
                for (method in userCls.declaredMethods) {
                    val mName = method.name.lowercase()
                    if (method.returnType == Boolean::class.javaPrimitiveType) {
                        if (mName == "hasplus" || mName == "ispaid" || mName == "hasgold" || mName == "hasmax" || mName == "hasunlimitedhearts") {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                        }
                    }
                }
                XposedBridge.log("Rhpatch: Berhasil menyadap Duolingo User -> $className")
            }
        }
    }

    // 2. Hook LoggedInState constructor to intercept the User instance
    runCatching {
        val loggedInClasses = listOf(
            "com.duolingo.core.session.SessionState\$LoggedIn",
            "com.duolingo.session.SessionState\$LoggedIn",
            "com.duolingo.session.LoggedIn"
        )
        for (className in loggedInClasses) {
            val loggedInCls = XposedHelpers.findClassIfExists(className, classLoader)
            if (loggedInCls != null) {
                XposedBridge.hookAllConstructors(loggedInCls, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        for (arg in param.args) {
                            if (arg != null && arg.javaClass.name.contains("User")) {
                                patchUserObject(arg)
                            }
                        }
                    }
                })
            }
        }
    }

    // 3. Hook SubscriptionInfo Constructor (Super & MAX Product ID metadata)
    runCatching {
        val subInfoCls = XposedHelpers.findClassIfExists("com.duolingo.data.plus.SubscriptionInfo", classLoader)
        if (subInfoCls != null) {
            XposedBridge.hookAllConstructors(subInfoCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val subObj = param.thisObject
                    val cls = subObj.javaClass
                    for (field in cls.declaredFields) {
                        field.isAccessible = true
                        val fName = field.name.lowercase()
                        val fType = field.type
                        try {
                            if (fType == Boolean::class.javaPrimitiveType) {
                                if (fName.contains("active") || fName.contains("plus") || fName.contains("super")) {
                                    field.set(subObj, true)
                                }
                            } else if (fType == String::class.java) {
                                if (fName.contains("productid") || fName.contains("sku")) {
                                    field.set(subObj, "gold_subscription_fam_twelve_month")
                                } else if (fName.contains("provider")) {
                                    field.set(subObj, "GOOGLE_PLAY")
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                }
            })
        }
    }

    // 4. Hook SubscriptionFeatures (Unlock AI Video Call, Practice Hub, Explain My Answer, Roleplay)
    runCatching {
        val featuresClasses = listOf(
            "com.duolingo.core.subscription.models.SubscriptionFeatures",
            "com.duolingo.subscription.models.SubscriptionFeatures"
        )
        for (className in featuresClasses) {
            val featCls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in featCls.declaredMethods) {
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                }
            }
        }
    }

    // 5. Unlimited Hearts / Energy Config Hook
    val energyClasses = listOf(
        "com.duolingo.core.energy.models.EnergyConfig",
        "com.duolingo.energy.models.EnergyConfig",
        "com.duolingo.energy.EnergyConfig",
        "com.duolingo.hearts.HeartsConfig"
    )
    for (className in energyClasses) {
        runCatching {
            val energyCls = XposedHelpers.findClassIfExists(className, classLoader)
            if (energyCls != null) {
                XposedBridge.hookAllConstructors(energyCls, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (param.args.size >= 2 && param.args[0] is Int && param.args[1] is Int) {
                                param.args[0] = 99999 // Current energy
                                param.args[1] = 99999 // Max energy
                            }
                        } catch (_: Throwable) {}
                    }
                })
                XposedBridge.log("Rhpatch: Berhasil menyadap Duolingo EnergyConfig -> $className")
            }
        }
    }

    // 6. Enable Developer / Debug Menu in Duolingo Settings
    runCatching {
        val debugClasses = listOf(
            "com.duolingo.debug.DebugMenuProvider",
            "com.duolingo.core.debug.DebugMenuProvider",
            "com.duolingo.debug.DebugSettings"
        )
        for (className in debugClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (field in cls.declaredFields) {
                if (field.type == Boolean::class.javaPrimitiveType && (field.name.lowercase().contains("debug") || field.name.lowercase().contains("enabled"))) {
                    field.isAccessible = true
                    runCatching { field.set(null, true) }
                }
            }
        }
    }
}

val DuolingoPatches = arrayOf(DuolingoSuperMaxPatch)
