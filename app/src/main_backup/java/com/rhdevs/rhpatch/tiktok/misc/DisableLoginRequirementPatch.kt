package com.rhdevs.rhpatch.tiktok.misc

import com.rhdevs.rhpatch.youtube.findMethodListDirect
import com.rhdevs.rhpatch.youtube.fingerprintList
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.query.enums.StringMatchType

val MandatoryLoginServiceMethods = findMethodListDirect(
    fingerprintList {
        classMatcher {
            className("MandatoryLoginService", StringMatchType.EndsWith)
        }
    }
)

val DisableLoginRequirementPatch = patch(
    name = "Disable login requirement",
    description = "Disables mandatory login for TikTok."
) {
    runCatching {
        var hooked = 0
        val methods = ::MandatoryLoginServiceMethods.dexMethodList
        methods.forEach { dexMethod ->
            if (dexMethod.name == "enableForcedLogin" || dexMethod.name == "shouldShowForcedLogin") {
                val method = dexMethod.toMethod()
                if (method != null && !java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    hooked++
                }
            }
        }
        
        if (hooked > 0) {
            XposedBridge.log("Rhpatch: [TikTok] Disabled login requirement ($hooked hooks)")
        } else {
            XposedBridge.log("Rhpatch: [TikTok] Disable login requirement hooks failed (methods not found)")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [TikTok] Disable login requirement failed: $it")
    }
}
