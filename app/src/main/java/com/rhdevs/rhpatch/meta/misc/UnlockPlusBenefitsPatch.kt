package com.rhdevs.rhpatch.meta.misc

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock Creator Plus",
    description = "Mengaktifkan fitur eksklusif berlangganan Creator Plus (Story Fonts, Story Preview, Custom App Icon, dll)."
) {
    runCatching {
        val prefs = appContext.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("pref_ig_plus", true)
        if (!isEnabled) return@runCatching

        XposedBridge.log("Rhpatch: [UnlockPlus] Initializing Instagram Plus and Story Plus unlocker...")

        // 1. Hook Gatekeeper: Story Fonts Direct Checker (LX/0GjV;->A00)
        runCatching {
            val gjvClass = XposedHelpers.findClassIfExists("LX.0GjV", classLoader)
            if (gjvClass != null) {
                for (m in gjvClass.declaredMethods) {
                    if (m.name == "A00" && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPlus] Successfully hooked LX.0GjV.A00 -> Unlocked Story Fonts!")
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Failed to hook 0GjV: " + it.message) }

        // 2. Hook Gatekeeper: Plus Feature Entrypoint (LX/01oH;->A00 and LX/07sj;->A01)
        runCatching {
            val o1oHClass = XposedHelpers.findClassIfExists("LX.01oH", classLoader)
            if (o1oHClass != null) {
                for (m in o1oHClass.declaredMethods) {
                    if (m.name == "A00" && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPlus] Successfully hooked LX.01oH.A00 -> Unlocked Plus Benefit Gatekeeper!")
                    }
                }
            }

            val sjClass = XposedHelpers.findClassIfExists("LX.07sj", classLoader)
            if (sjClass != null) {
                for (m in sjClass.declaredMethods) {
                    if (m.name == "A01" && m.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                        XposedBridge.log("Rhpatch: [UnlockPlus] Successfully hooked LX.07sj.A01 -> Unlocked Plus UI Gatekeeper!")
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Failed to hook Gatekeepers: " + it.message) }

        // 3. Hook Central Benefit Data Provider: LX/07pt (SUBSBenefitDataProvider)
        val hookedBenefitMethods = mutableListOf<String>()
        runCatching {
            val ptClass = XposedHelpers.findClassIfExists("LX.07pt", classLoader)
            if (ptClass != null) {
                for (m in ptClass.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = true
                            }
                        })
                        hookedBenefitMethods.add("LX.07pt." + m.name)
                    }
                }
            }
        }

        // Dynamic discovery fallback if LX.07pt is renamed in other builds
        if (hookedBenefitMethods.isEmpty()) {
            runCatching {
                if (com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) {
                    val methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("is_benefit_active")
                    for (method in methods) {
                        val declaringClass = method.declaringClass
                        for (classMethod in declaringClass.declaredMethods) {
                            if (classMethod.returnType == java.lang.Boolean.TYPE && classMethod.parameterTypes.size == 1 && classMethod.parameterTypes[0] == String::class.java) {
                                XposedBridge.hookMethod(classMethod, XC_MethodReplacement.returnConstant(true))
                                hookedBenefitMethods.add(declaringClass.name + "." + classMethod.name)
                            }
                        }
                    }
                }
            }
        }
        XposedBridge.log("Rhpatch: [UnlockPlus] Hooked " + hookedBenefitMethods.size + " benefit checker methods")

        // 4. Hook Jetpack Compose Icon Click Handler (LX/0RAH;->invoke)
        // Intercepts the click in the AuraAppIcon grid cell, selects the icon in ViewModel,
        // and immediately applies the launcher icon while totally suppressing the Case 42 paywall popup!
        runCatching {
            val rahClass = XposedHelpers.findClassIfExists("LX.0RAH", classLoader)
            if (rahClass != null) {
                for (m in rahClass.declaredMethods) {
                    if (m.name == "invoke") {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    val firstArg = param.args.firstOrNull() ?: return
                                    val thisObj = param.thisObject
                                    val fieldA00 = runCatching { XposedHelpers.getObjectField(thisObj, "A00") }.getOrNull()
                                    if (fieldA00 == null) return

                                    XposedBridge.log("Rhpatch: [UnlockPlus] Intercepted Compose icon click: " + firstArg)

                                    // 1. Select icon in ViewModel state (LX.0EKv.A00)
                                    val ekvClass = XposedHelpers.findClassIfExists("LX.0EKv", classLoader)
                                    if (ekvClass != null) {
                                        val fA01 = runCatching { XposedHelpers.getObjectField(fieldA00, "A01") }.getOrNull()
                                        val vm = runCatching { XposedHelpers.callMethod(fA01, "getValue") }.getOrNull()
                                        if (vm != null) {
                                            runCatching {
                                                XposedHelpers.callStaticMethod(ekvClass, "A00", firstArg, vm)
                                                XposedBridge.log("Rhpatch: [UnlockPlus] Called LX.0EKv.A00 to update UI state")
                                            }
                                        }
                                    }

                                    // 2. Obtain context and apply icon to launcher immediately
                                    val ctx = (runCatching { XposedHelpers.callMethod(fieldA00, "getContext") as? Context }.getOrNull())
                                        ?: (runCatching { XposedHelpers.callMethod(fieldA00, "requireContext") as? Context }.getOrNull())
                                        ?: (fieldA00 as? Context)
                                        ?: appContext

                                    applyLauncherIcon(ctx, firstArg)

                                    // 3. Suppress original lambda execution (bypasses Case 42 paywall popup!)
                                    param.result = kotlin.Unit
                                } catch (e: Throwable) {
                                    XposedBridge.log("Rhpatch: [UnlockPlus] Error in 0RAH hook: " + e.message)
                                }
                            }
                        })
                    }
                }
                XposedBridge.log("Rhpatch: [UnlockPlus] Hooked Compose icon click handler LX.0RAH.invoke")
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Failed to hook 0RAH: " + it.message) }

        // 4b. Explicitly hook LX.0Oqc.A04 to kill Case 41/42 upsell popup from ANY code path
        runCatching {
            val oqcClass = XposedHelpers.findClassIfExists("LX.0Oqc", classLoader)
            if (oqcClass != null) {
                for (m in oqcClass.declaredMethods) {
                    if (m.name == "A04") {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val tag = param.args.getOrNull(2) as? Int
                                if (tag == 41 || tag == 42) {
                                    param.result = null
                                    XposedBridge.log("Rhpatch: [UnlockPlus] Suppressed Case " + tag + " upsell popup in LX.0Oqc.A04")
                                }
                            }
                        })
                    }
                }
            }
        }

        // 5. Neutralize Status Enum (LX/0GuK) so icons are never marked as locked
        runCatching {
            val gukClass = XposedHelpers.findClassIfExists("LX.0GuK", classLoader)
            if (gukClass != null) {
                for (m in gukClass.declaredMethods) {
                    if (m.name == "valueOf" && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if ("IG_PLUS_LOCKED" == param.args[0]) {
                                    param.args[0] = "IG_PLUS_AVAILABLE"
                                }
                            }
                        })
                    }
                }

                runCatching {
                    if (gukClass.isEnum) {
                        val availableConst = gukClass.enumConstants?.firstOrNull { it.toString().contains("AVAILABLE", ignoreCase = true) }
                        if (availableConst != null) {
                            val fA06 = XposedHelpers.findFieldIfExists(gukClass, "A06")
                            if (fA06 != null) {
                                fA06.isAccessible = true
                                fA06.set(null, availableConst)
                                XposedBridge.log("Rhpatch: [UnlockPlus] Set LX.0GuK.A06 -> IG_PLUS_AVAILABLE")
                            }
                        }
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Failed to neutralize 0GuK: " + it.message) }

        // 6. Hook Action Button in ViewModel (LX/0EKv;->A0w) to apply icon directly
        runCatching {
            val ekvClass = XposedHelpers.findClassIfExists("LX.0EKv", classLoader)
            if (ekvClass != null) {
                for (m in ekvClass.declaredMethods) {
                    if (m.name == "A0w" && m.parameterTypes.size == 1) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    val vm = param.thisObject
                                    val fA08 = XposedHelpers.getObjectField(vm, "A08")
                                    val state = XposedHelpers.callMethod(fA08, "getValue")
                                    if (state != null) {
                                        val iconObj = XposedHelpers.getObjectField(state, "A00")
                                        if (iconObj != null) {
                                            applyLauncherIcon(appContext, iconObj)
                                            param.result = null
                                            XposedBridge.log("Rhpatch: [UnlockPlus] Handled A0w action button click directly")
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }
                        })
                        XposedBridge.log("Rhpatch: [UnlockPlus] Hooked ViewModel action button LX.0EKv.A0w")
                    }
                }
            }
        }

        // 7. Suppress Bloks Subscription Upsell Dialog (Only void method LX.0Hff.A00)
        runCatching {
            val hffClass = XposedHelpers.findClassIfExists("LX.0Hff", classLoader)
            if (hffClass != null) {
                for (m in hffClass.declaredMethods) {
                    if (m.name == "A00" && m.returnType == java.lang.Void.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.DO_NOTHING)
                        XposedBridge.log("Rhpatch: [UnlockPlus] Hooked LX.0Hff.A00 -> Suppressed Bloks paywall popup!")
                    }
                }
            }
        }

        // 8. Protect launcher alias against Dalvik bug in onStop (which sets state 0 / DISABLED)
        runCatching {
            XposedBridge.hookAllMethods(android.content.pm.PackageManager::class.java, "setComponentEnabledSetting", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val component = param.args[0] as? ComponentName ?: return
                        val state = param.args[1] as? Int ?: return
                        if (component.className.contains("MainTabActivity") && state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                            param.args[1] = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        }
                    } catch (_: Throwable) {}
                }
            })
        }

        // 9. Direct Launcher Icon Switcher: LX/07qq (AuraAppIconSwitchManager)
        runCatching {
            val switchManagerClass = XposedHelpers.findClassIfExists("LX.07qq", classLoader)
            if (switchManagerClass != null) {
                for (m in switchManagerClass.declaredMethods) {
                    val params = m.parameterTypes
                    if (params.isNotEmpty() && params[0] == Context::class.java && params.size >= 2) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    val ctx = param.args[0] as? Context ?: appContext
                                    val iconObj = param.args[1] ?: return
                                    applyLauncherIcon(ctx, iconObj)
                                } catch (e: Throwable) {
                                    XposedBridge.log("Rhpatch: [UnlockPlus] Error in applyLauncherIcon: " + e.message)
                                }
                            }
                        })
                        XposedBridge.log("Rhpatch: [UnlockPlus] Hooked icon switcher method LX.07qq." + m.name)
                    }
                }
            }
        }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Failed to hook icon switch manager: " + it.message) }

    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: " + it) }
}

private val ALL_ICON_ALIASES = arrayOf(
    "com.instagram.android.activity.MainTabActivity",
    "com.instagram.android.activity.MainTabActivity.neon",
    "com.instagram.android.activity.MainTabActivity.flame",
    "com.instagram.android.activity.MainTabActivity.floral",
    "com.instagram.android.activity.MainTabActivity.slime",
    "com.instagram.android.activity.MainTabActivity.metal",
    "com.instagram.android.activity.MainTabActivity.kpop",
    "com.instagram.android.activity.MainTabActivity.haruko",
    "com.instagram.android.activity.MainTabActivity.felipe",
    "com.instagram.android.activity.MainTabActivity.humberto",
    "com.instagram.android.activity.MainTabActivity.zipeng",
    "com.instagram.android.activity.MainTabActivity.uzo",
    "com.instagram.android.activity.MainTabActivity.ricky",
    "com.instagram.android.activity.MainTabActivity.throwback"
)

fun applyLauncherIcon(context: Context, rawIcon: Any) {
    try {
        val pm = context.packageManager
        val packageName = context.packageName

        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)

        val resolveList = pm.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_DISABLED_COMPONENTS or 0x00020000 /* MATCH_ALL */
        )
        val validAliases = if (resolveList.isNotEmpty()) {
            resolveList.map { it.activityInfo.name }
        } else {
            ALL_ICON_ALIASES.toList()
        }

        var targetAlias: String? = null
        val rawStr = rawIcon.toString().lowercase()

        for (alias in validAliases) {
            val suffix = alias.substringAfterLast('.').lowercase()
            if (suffix != "maintabactivity" && rawStr.contains(suffix)) {
                targetAlias = alias
                break
            }
        }

        if (targetAlias == null) {
            if (rawStr.contains("default") || rawStr.endsWith("maintabactivity")) {
                targetAlias = validAliases.firstOrNull { it.endsWith("MainTabActivity", ignoreCase = true) }
            }
        }

        if (targetAlias == null) {
            targetAlias = validAliases.firstOrNull() ?: "com.instagram.android.activity.MainTabActivity"
        }

        val targetComp = ComponentName(packageName, targetAlias)
        pm.setComponentEnabledSetting(
            targetComp,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        for (alias in validAliases) {
            if (!alias.equals(targetAlias, ignoreCase = true)) {
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(packageName, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (_: Throwable) {}
            }
        }

        XposedBridge.log("Rhpatch: [UnlockPlus] Switched launcher icon to: " + targetAlias)
        android.widget.Toast.makeText(context, "[Rhpatch] Icon aplikasi diubah ke: " + targetAlias.substringAfterLast('.') + "\n(Muat ulang launcher jika belum berubah di beranda)", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        XposedBridge.log("Rhpatch: [UnlockPlus] Error applying icon: " + e.message)
    }
}
