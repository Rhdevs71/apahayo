package com.rhdevs.rhpatch.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.rhdevs.rhpatch.ACTION_SAVE_LOG") {
            try {
                val message = intent.getStringExtra("message") ?: return
                val type = intent.getStringExtra("type") ?: return
                
                val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                val currentLogs = sharedPrefs.getString("antispam_logs", "[]") ?: "[]"
                val jsonArray = org.json.JSONArray(currentLogs)
                
                val logObj = org.json.JSONObject()
                logObj.put("time", System.currentTimeMillis())
                logObj.put("type", type)
                logObj.put("message", message)
                
                jsonArray.put(logObj)
                
                // Keep only last 50 logs
                val trimmedArray = org.json.JSONArray()
                val startIdx = if (jsonArray.length() > 50) jsonArray.length() - 50 else 0
                for (i in startIdx until jsonArray.length()) {
                    trimmedArray.put(jsonArray.getJSONObject(i))
                }
                
                sharedPrefs.edit().putString("antispam_logs", trimmedArray.toString()).apply()
                Log.d("Rhpatch", "Log received and saved: $type -> $message")
            } catch (e: Exception) {
                Log.e("Rhpatch", "Error saving log via receiver", e)
            }
        }
    }
}
