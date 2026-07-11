package com.wmods.wppenhacer.xposed.features.privacy

import android.app.Activity
import android.media.MediaPlayer
import android.media.Ringtone
import android.os.Bundle
import android.os.Message
import android.widget.Toast
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp
import com.wmods.wppenhacer.xposed.core.components.WaContactWpp
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.features.general.Tasker
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File

class CallPrivacy(loader: ClassLoader, preferences: SharedPreferences) :
    Feature(loader, preferences) {

    private var mVoipManager: Any? = null

    companion object {
        private var customPlayer: MediaPlayer? = null
        private var isCustomRingtonePlaying = false

        @JvmStatic
        fun stopCustomRingtone() {
            if (isCustomRingtonePlaying) {
                XposedBridge.log("WaEnhancer CallPrivacy: Stopping custom ringtone")
                try {
                    customPlayer?.stop()
                    customPlayer?.release()
                } catch (_: Exception) {}
                customPlayer = null
                isCustomRingtonePlaying = false
            }
        }
    }

    override fun doHook() {
        val voipManagerClass = Unobfuscator.loadVoipManager(classLoader)
        XposedBridge.hookAllConstructors(voipManagerClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                mVoipManager = param.thisObject
            }
        })

        val clazzVoip = WppCore.voipManagerClass
        val endCallMethod = clazzVoip.declaredMethods.first { it.name == "endCall" }
        val rejectCallMethod = clazzVoip.declaredMethods.first { it.name == "rejectCall" }

        val onCallReceivedMethod = Unobfuscator.loadAntiRevokeOnCallReceivedMethod(classLoader)

        XposedBridge.hookMethod(onCallReceivedMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val callInfoClass = WppCore.voipCallInfoClass
                val callinfo: Any? = when {
                    param.args[0] is Message -> (param.args[0] as Message).obj
                    param.args.size > 1 && callInfoClass.isInstance(param.args[1]) -> param.args[1]
                    else -> {
                        Utils.showToast("Invalid call info", Toast.LENGTH_SHORT)
                        return
                    }
                }
                if (callinfo == null || !callInfoClass.isInstance(callinfo)) return
                if (XposedHelpers.getObjectField(callinfo, "callState")
                        ?.toString() != "RECEIVED_CALL"
                ) return
                val userJid = FMessageWpp.UserJid(XposedHelpers.callMethod(callinfo, "getPeerJid"))
                val callId = XposedHelpers.callMethod(callinfo, "getCallId")
                val type = prefs.getString("call_privacy", "0")!!.toInt()
                val waContact = WaContactWpp.getWaContactFromJid(userJid)
                val contactName = waContact?.displayName ?: userJid.phoneNumber
                Tasker.sendTaskerEvent(
                    contactName,
                    userJid.phoneNumber,
                    "call_received"
                )

                val privacyType = PrivacyType.getByValue(type)
                val blockCall = checkCallBlock(userJid, privacyType)
                if (blockCall) {
                    var rejectType = prefs.getString("call_type", null) ?: "no_internet"
                    when (rejectType) {
                        "uncallable", "declined", "busy" -> {
                            if (rejectType == "declined") {
                                rejectType = ""
                            }
                            val params = ReflectionUtils.initArray(rejectCallMethod.parameterTypes)
                            params[0] = callId
                            params[1] = rejectType
                            ReflectionUtils.callMethod(rejectCallMethod, mVoipManager, *params)
                            param.result = true
                        }
                        "ended" -> {
                            val params = ReflectionUtils.initArray(endCallMethod.parameterTypes)
                            params[0] = true
                            ReflectionUtils.callMethod(endCallMethod, mVoipManager, *params)
                            param.result = true
                        }
                    }
                    return
                }

                // If not blocked, check if there is a custom ringtone for this contact
                val phoneNum = userJid.phoneNumber ?: return
                val customRingtonePath = prefs.getString("ringtone_$phoneNum", null)
                if (!customRingtonePath.isNullOrEmpty()) {
                    val file = File(customRingtonePath)
                    if (file.exists()) {
                        XposedBridge.log("WaEnhancer CallPrivacy: Playing custom ringtone for $phoneNum from $customRingtonePath")
                        playCustomRingtone(customRingtonePath)
                    }
                }
            }
        })

        XposedBridge.hookAllMethods(
            WppCore.voipManagerClass,
            "nativeHandleIncomingXmppOffer",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val jidClass = Unobfuscator.findFirstClassUsingName(
                        classLoader, StringMatchType.EndsWith, "jid.Jid"
                    )
                    val jidObj = ReflectionUtils.getArg(param.args, jidClass, 0)
                    val userJid = FMessageWpp.UserJid(jidObj)
                    val rejectType = prefs.getString("call_type", null) ?: "no_internet"
                    if (rejectType == "no_internet") {
                        val type = prefs.getString("call_privacy", "0")!!.toInt()
                        val privacyType = PrivacyType.getByValue(type)
                        val block = checkCallBlock(userJid, privacyType)
                        if (block) {
                            param.result = 1
                        }
                    }
                }
            })

        // Hook Ringtone play to silence the default tone if custom ringtone is playing
        try {
            XposedHelpers.findAndHookMethod(
                Ringtone::class.java,
                "play",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isCustomRingtonePlaying) {
                            XposedBridge.log("WaEnhancer CallPrivacy: Silenced default WhatsApp ringtone")
                            param.result = null // Bypass default play
                        }
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Stop ringtone when call ends or is destroyed
        try {
            val clsCallEventCallback = Unobfuscator.findFirstClassUsingName(
                classLoader,
                StringMatchType.EndsWith,
                "VoiceServiceEventCallback"
            )
            XposedBridge.hookAllMethods(
                clsCallEventCallback,
                "fieldstatsReady",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        stopCustomRingtone()
                    }
                }
            )

            val voipActivityClass = Unobfuscator.findFirstClassUsingName(
                classLoader,
                StringMatchType.Contains,
                "VoipActivity"
            )
            XposedBridge.hookAllMethods(
                voipActivityClass,
                "onDestroy",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        stopCustomRingtone()
                    }
                }
            )
        } catch (_: Exception) {}
    }

    private fun playCustomRingtone(path: String) {
        stopCustomRingtone()
        try {
            customPlayer = MediaPlayer().apply {
                setDataSource(path)
                isLooping = true
                prepare()
                start()
            }
            isCustomRingtonePlaying = true
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer CallPrivacy: Failed to play custom ringtone: ${e.message}")
            e.printStackTrace()
        }
    }

    fun checkCallBlock(userJid: FMessageWpp.UserJid, type: PrivacyType?): Boolean {
        val phoneNumber = userJid.phoneNumber ?: return false

        val customprivacy = CustomPrivacy.getJSON(phoneNumber)

        return when (type) {
            PrivacyType.ALL_BLOCKED -> customprivacy.optBoolean("BlockCall", true)
            PrivacyType.ALL_PERMITTED -> customprivacy.optBoolean("BlockCall", false)
            PrivacyType.ONLY_UNKNOWN -> {
                val waContact = WaContactWpp.getWaContactFromJid(userJid) ?: return true
                !waContact.isSavedContact()
            }
            PrivacyType.BACKLIST -> {
                if (customprivacy.optBoolean("BlockCall", false)) return true
                val callBlockList = prefs.getString("call_block_contacts", "[]")!!
                val blockList = callBlockList.substring(1, callBlockList.length - 1).split(", ")
                    .map { it.trim() }
                blockList.any { it.isNotEmpty() && it == userJid.phoneRawString }
            }

            PrivacyType.WHITELIST -> {
                if (customprivacy.optBoolean("BlockCall", false)) return true
                val callWhiteList = prefs.getString("call_white_contacts", "[]")!!
                val whiteList = callWhiteList.substring(1, callWhiteList.length - 1).split(", ")
                    .map { it.trim() }
                whiteList.none { it.isNotEmpty() && it == userJid.phoneRawString }
            }

            null -> false
        }
    }

    override fun getPluginName() = "Call Privacy"

    enum class PrivacyType(val value: Int) {
        ALL_PERMITTED(0), ALL_BLOCKED(
            1
        ),
        ONLY_UNKNOWN(2), BACKLIST(
            3
        ),
        WHITELIST(4);

        companion object {
            fun getByValue(value: Int) = entries.find { it.value == value }
        }
    }

}
