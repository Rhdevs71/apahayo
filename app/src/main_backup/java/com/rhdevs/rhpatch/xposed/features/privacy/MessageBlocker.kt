package com.rhdevs.rhpatch.xposed.features.privacy

import android.content.ContentValues
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import com.rhdevs.rhpatch.xposed.core.Feature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class MessageBlocker(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun doHook() {
        if (!prefs.getBoolean("message_blocker_enabled", false)) return

        val blockHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val table = param.args[0] as? String ?: return
                    if (table == "message" || table == "messages") {
                        val valuesIndex = param.args.indexOfFirst { it is ContentValues }
                        if (valuesIndex != -1) {
                            val values = param.args[valuesIndex] as ContentValues
                            if (shouldBlockMessage(values)) {
                                XposedBridge.log("Rhpatch Message Blocker: Blocked incoming message.")
                                param.result = -1L // Simulate failed insertion or success without actually inserting
                                
                                // Save to secret log
                                val text = values.getAsString("text_data") ?: values.getAsString("data") ?: "Unknown"
                                val jid = values.getAsString("remote_jid") ?: "Unknown"
                                saveSpamLog(jid, text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    XposedBridge.log("Rhpatch Message Blocker Error: ${e.message}")
                }
            }
        }

        try {
            XposedBridge.hookAllMethods(SQLiteDatabase::class.java, "insert", blockHook)
            XposedBridge.hookAllMethods(SQLiteDatabase::class.java, "insertWithOnConflict", blockHook)
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch Message Blocker: Failed to hook SQLiteDatabase ${e.message}")
        }
        
        // Also try to hook SQLiteStatement if they use prepared statements
        try {
            XposedBridge.hookAllMethods(
                android.database.sqlite.SQLiteStatement::class.java,
                "executeInsert",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val statement = param.thisObject
                            val sql = XposedHelpers.callMethod(statement, "getSql") as? String ?: return
                            if (sql.contains("INTO message") || sql.contains("INTO messages")) {
                                // For SQLiteStatement, getting bind args is tricky as it's an internal array
                                val mBindArgs = XposedHelpers.getObjectField(statement, "mBindArgs") as? Array<*>
                                if (mBindArgs != null) {
                                    // Just convert all args to string and check for keywords
                                    val fullText = mBindArgs.joinToString(" ").lowercase()
                                    if (shouldBlockFromText(fullText)) {
                                        XposedBridge.log("Rhpatch Message Blocker: Blocked incoming message from SQLiteStatement.")
                                        param.result = -1L
                                        saveSpamLog("Unknown JID", "Message matched keywords (SQLiteStatement)")
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            )
        } catch (e: Exception) {}
    }

    private fun shouldBlockMessage(values: ContentValues): Boolean {
        val jid = values.getAsString("remote_jid") ?: values.getAsString("key_remote_jid")
        val text = values.getAsString("text_data") ?: values.getAsString("data")
        
        if (text.isNullOrEmpty()) return false
        
        return shouldBlockFromText(text.lowercase())
    }
    
    private fun shouldBlockFromText(text: String): Boolean {
        val keywordsStr = prefs.getString("message_block_keywords", "") ?: ""
        if (keywordsStr.isEmpty()) return false
        
        val keywords = keywordsStr.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        for (keyword in keywords) {
            if (text.contains(keyword)) {
                return true
            }
        }
        return false
    }
    
    private fun saveSpamLog(sender: String, message: String) {
        try {
            val logsJson = prefs.getString("wa_antispam_logs", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(logsJson)
            
            val newLog = org.json.JSONObject()
            newLog.put("type", "WhatsApp Spam: $sender")
            newLog.put("message", message)
            newLog.put("time", System.currentTimeMillis())
            
            jsonArray.put(newLog)
            
            // Limit to 50
            val limit = 50
            val tempArray = org.json.JSONArray()
            val startIdx = if (jsonArray.length() > limit) jsonArray.length() - limit else 0
            for (i in startIdx until jsonArray.length()) {
                tempArray.put(jsonArray.getJSONObject(i))
            }
            
            prefs.edit().putString("wa_antispam_logs", tempArray.toString()).apply()
        } catch (e: Exception) {}
    }

    override fun getPluginName(): String {
        return "Message Blocker"
    }
}
