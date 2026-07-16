package com.wmods.wppenhacer.xposed.features.general

import android.content.SharedPreferences
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File

class FakeDisplayHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "FakeDisplayHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("fake_display_enabled", false)
        if (!enabled) return

        hookContactManager()
        hookProfilePhotoManager()
    }

    private fun hookContactManager() {
        try {
            val getWaContactMethod = Unobfuscator.loadGetWaContactMethod(classLoader)
            XposedBridge.hookMethod(getWaContactMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val contact = param.result ?: return
                    
                    // Extract JID of contact
                    val userJidField = ReflectionUtils.findFieldUsingFilter(contact.javaClass) { f ->
                        f.type.name.contains("Jid") || f.type.simpleName == "UserJid"
                    } ?: return
                    userJidField.isAccessible = true
                    val jidObj = userJidField.get(contact) ?: return
                    
                    val isMe = XposedHelpers.callMethod(jidObj, "isMe") as? Boolean ?: false
                    val rawJid = XposedHelpers.callMethod(jidObj, "getRawString") as? String ?: return
                    val jidKey = if (isMe || rawJid.contains("me")) "me" else rawJid

                    val nameEnabled = prefs.getBoolean("fake_display_name_enabled_$jidKey", false)
                    val fakeName = prefs.getString("fake_display_name_$jidKey", "") ?: ""
                    
                    if (nameEnabled && fakeName.isNotEmpty()) {
                        setContactNameFields(contact, fakeName)
                    }
                }
            })
            XposedBridge.log("WaEnhancer FakeDisplayHook: ContactManager hook registered successfully")
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer FakeDisplayHook Error: ContactManager hook failed: ${e.message}")
        }
    }

    private fun setContactNameFields(waContact: Any, fakeName: String) {
        var cls: Class<*>? = waContact.javaClass
        while (cls != null) {
            for (field in cls.declaredFields) {
                if (field.type == String::class.java) {
                    try {
                        field.isAccessible = true
                        val currentVal = field.get(waContact) as? String
                        if (currentVal != fakeName) {
                            field.set(waContact, fakeName)
                        }
                    } catch (_: Exception) {}
                } else if (field.type.name.contains("WaContactData") || field.type.simpleName == "WaContactData") {
                    try {
                        field.isAccessible = true
                        val dataObj = field.get(waContact)
                        if (dataObj != null) {
                            setContactNameFields(dataObj, fakeName)
                        }
                    } catch (_: Exception) {}
                }
            }
            cls = cls.superclass
        }
    }

    private fun hookProfilePhotoManager() {
        try {
            val getProfilePhotoMethod = Unobfuscator.loadGetProfilePhotoMethod(classLoader)
            XposedBridge.hookMethod(getProfilePhotoMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val jidObj = param.args.find { arg ->
                        arg != null && (arg.javaClass.name.contains("Jid") || arg.javaClass.simpleName == "UserJid")
                    }
                    val jidKey = if (jidObj != null) {
                        val isMe = XposedHelpers.callMethod(jidObj, "isMe") as? Boolean ?: false
                        val rawJid = XposedHelpers.callMethod(jidObj, "getRawString") as? String ?: ""
                        if (isMe || rawJid.contains("me")) "me" else rawJid
                    } else {
                        // Check if contact object is passed
                        val contactObj = param.args.find { arg ->
                            arg != null && (arg.javaClass.name.contains("Contact") || arg.javaClass.simpleName.contains("Contact"))
                        } ?: return
                        
                        // Try to extract Jid from contact object
                        val userJidField = ReflectionUtils.findFieldUsingFilter(contactObj.javaClass) { f ->
                            f.type.name.contains("Jid") || f.type.simpleName == "UserJid"
                        } ?: return
                        userJidField.isAccessible = true
                        val j = userJidField.get(contactObj) ?: return
                        val isMe = XposedHelpers.callMethod(j, "isMe") as? Boolean ?: false
                        val rawJid = XposedHelpers.callMethod(j, "getRawString") as? String ?: ""
                        if (isMe || rawJid.contains("me")) "me" else rawJid
                    }

                    val photoEnabled = prefs.getBoolean("fake_display_photo_enabled_$jidKey", false)
                    val photoPath = prefs.getString("fake_display_photo_$jidKey", "") ?: ""
                    if (photoEnabled && photoPath.isNotEmpty()) {
                        val file = File(photoPath)
                        if (file.exists()) {
                            param.result = file.inputStream()
                        }
                    }
                }
            })
            XposedBridge.log("WaEnhancer FakeDisplayHook: ProfilePhotoManager hook registered successfully")
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer FakeDisplayHook Error: ProfilePhotoManager hook failed: ${e.message}")
        }
    }
}
