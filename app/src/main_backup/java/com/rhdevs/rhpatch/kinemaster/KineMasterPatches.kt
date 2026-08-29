package com.rhdevs.rhpatch.kinemaster

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val KineMasterUnlockPatch = patch(
    name = "KineMaster Premium & No Watermark",
    description = "Membuka fitur langganan Premium, menghapus watermark ekspor video, dan membuka aset toko VIP"
) {
    runCatching {
        val kmClasses = listOf(
            "com.kinemaster.module.network.communication.account.dto.SubscribeResponseDto",
            "com.kinemaster.app.modules.lifeline.LifelineManager",
            "com.kinemaster.app.modules.main.home.viewmodel.BaseViewModel",
            "com.kinemaster.app.modules.save.SaveAsProcessPresenter",
            "com.nexstreaming.app.kinemasterfree.service.SubscriptionService",
            "com.kinemaster.module.account.SubscribeResponseDto",
            "com.kinemaster.app.account.LifelineManager"
        )
        for (className in kmClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName == "issubscribed" || mName == "j" || mName == "f0" || 
                        mName.contains("subscribed") || mName.contains("haspremiumpurchase") || 
                        mName.contains("ispremium") || mName.contains("ispro") || mName.contains("isvip")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val KineMasterPatches = arrayOf(KineMasterUnlockPatch)
