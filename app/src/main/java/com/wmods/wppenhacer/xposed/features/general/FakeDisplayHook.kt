package com.wmods.wppenhacer.xposed.features.general

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.db.MessageStore
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
        hookConversationMenu()
        registerBroadcastReceiver(Utils.application)
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

    private fun hookConversationMenu() {
        try {
            val conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", classLoader)
            
            XposedBridge.hookAllMethods(conversationClass, "onCreateOptionsMenu", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val menu = param.args[0] as android.view.Menu
                    // Add "Fake Settings" menu item
                    menu.add(android.view.Menu.NONE, 992211, 0, "Fake Settings")
                }
            })

            XposedBridge.hookAllMethods(conversationClass, "onOptionsItemSelected", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val item = param.args[0] as android.view.MenuItem
                    if (item.itemId == 992211) {
                        val activity = param.thisObject as android.app.Activity
                        val userJid = WppCore.getCurrentUserJid()
                        val rawJid = userJid?.phoneRawString ?: ""
                        
                        // Launch FakeDisplayActivity directly
                        val intent = Intent().apply {
                            component = android.content.ComponentName(
                                BuildConfig.APPLICATION_ID,
                                "com.wmods.wppenhacer.activities.FakeDisplayActivity"
                            )
                            putExtra("CHAT_JID", rawJid)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(intent)
                        param.result = true
                    }
                }
            })
            XposedBridge.log("WaEnhancer FakeDisplayHook: Conversation menu hook registered successfully")
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer FakeDisplayHook Error: Conversation menu hook failed: ${e.message}")
        }
    }

    private fun registerBroadcastReceiver(context: Context) {
        val filter = IntentFilter().apply {
            addAction("com.wmods.wppenhacer.INJECT_FAKE_MESSAGE")
            addAction("com.wmods.wppenhacer.INJECT_FAKE_CALL")
        }
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val action = intent.action ?: return
                val db = MessageStore.getInstance().getDatabase() ?: return
                
                if (action == "com.wmods.wppenhacer.INJECT_FAKE_MESSAGE") {
                    val chatJid = intent.getStringExtra("chat_jid") ?: return
                    val text = intent.getStringExtra("text") ?: return
                    val fromMe = intent.getBooleanExtra("from_me", true)
                    val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
                    val status = intent.getIntExtra("status", 13)
                    
                    try {
                        injectFakeMessage(db, chatJid, text, fromMe, timestamp, status)
                        XposedBridge.log("WaEnhancer FakeDisplayHook: Injected fake message successfully")
                    } catch (e: Exception) {
                        XposedBridge.log("WaEnhancer FakeDisplayHook Error: Failed to inject fake message: ${e.message}")
                    }
                } else if (action == "com.wmods.wppenhacer.INJECT_FAKE_CALL") {
                    val chatJid = intent.getStringExtra("chat_jid") ?: return
                    val isOutgoing = intent.getBooleanExtra("is_outgoing", true)
                    val isVideo = intent.getBooleanExtra("is_video", false)
                    val isConnected = intent.getBooleanExtra("is_connected", true)
                    val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
                    val duration = intent.getIntExtra("duration", 60)
                    
                    try {
                        injectFakeCallLog(db, chatJid, isOutgoing, isVideo, isConnected, timestamp, duration)
                        XposedBridge.log("WaEnhancer FakeDisplayHook: Injected fake call log successfully")
                    } catch (e: Exception) {
                        XposedBridge.log("WaEnhancer FakeDisplayHook Error: Failed to inject fake call log: ${e.message}")
                    }
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun injectFakeMessage(db: SQLiteDatabase, chatJid: String, text: String, fromMe: Boolean, timestamp: Long, status: Int) {
        db.execSQL("INSERT OR IGNORE INTO jid (raw_string) VALUES (?)", arrayOf(chatJid))
        var jidRowId: Long = -1
        db.rawQuery("SELECT _id FROM jid WHERE raw_string = ?", arrayOf(chatJid)).use { cursor ->
            if (cursor.moveToFirst()) {
                jidRowId = cursor.getLong(0)
            }
        }
        if (jidRowId == -1L) return

        db.execSQL("INSERT OR IGNORE INTO chat (jid_row_id) VALUES (?)", arrayOf(jidRowId))
        var chatRowId: Long = -1
        db.rawQuery("SELECT _id FROM chat WHERE jid_row_id = ?", arrayOf(jidRowId.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                chatRowId = cursor.getLong(0)
            }
        }
        if (chatRowId == -1L) return

        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val sb = java.lang.StringBuilder("3EB0")
        val random = java.util.Random()
        for (i in 1..12) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        val randomKey = sb.toString()

        db.execSQL(
            "INSERT INTO message (chat_row_id, from_me, key_id, sender_jid_row_id, message_type, text_data, timestamp, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                chatRowId,
                if (fromMe) 1 else 0,
                randomKey,
                if (fromMe) -1 else jidRowId,
                0,
                text,
                timestamp,
                status
            )
        )
    }

    private fun injectFakeCallLog(db: SQLiteDatabase, chatJid: String, isOutgoing: Boolean, isVideo: Boolean, isConnected: Boolean, timestamp: Long, durationSeconds: Int) {
        db.execSQL("INSERT OR IGNORE INTO jid (raw_string) VALUES (?)", arrayOf(chatJid))
        var jidRowId: Long = -1
        db.rawQuery("SELECT _id FROM jid WHERE raw_string = ?", arrayOf(chatJid)).use { cursor ->
            if (cursor.moveToFirst()) {
                jidRowId = cursor.getLong(0)
            }
        }
        if (jidRowId == -1L) return

        val callResult = if (isConnected) 1 else (if (isOutgoing) 3 else 2)

        db.execSQL(
            "INSERT INTO call_log (jid_row_id, from_me, timestamp, video_call, call_result, duration) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(
                jidRowId,
                if (isOutgoing) 1 else 0,
                timestamp,
                if (isVideo) 1 else 0,
                callResult,
                durationSeconds
            )
        )
    }
}
