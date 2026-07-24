package com.rhdevs.rhpatch.revanced.discord.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val AntiDelete = patch(
    name = "Anti-Delete Messages",
    description = "Prevents messages from being deleted in Discord."
) {
    runCatching {
        // Hook Android's built-in SQLiteDatabase delete method
        XposedHelpers.findAndHookMethod(
            SQLiteDatabase::class.java,
            "delete",
            String::class.java, // table
            String::class.java, // whereClause
            Array<String>::class.java, // whereArgs
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val table = param.args[0] as? String ?: return
                    val whereClause = param.args[1] as? String
                    val whereArgs = param.args[2] as? Array<String>
                    
                    if (table == "messages") {
                        // Instead of deleting, we can update the message to mark it as deleted but keep it in UI
                        // Discord messages table usually has 'content' or 'author' columns.
                        XposedBridge.log("Rhpatch: [Discord] Intercepted message deletion: where=$whereClause args=${whereArgs?.joinToString()}")
                        
                        // Prevent the actual DELETE query
                        param.result = 1 
                    }
                }
            }
        )
        
        // Also hook the raw execSQL for "DELETE FROM"
        XposedHelpers.findAndHookMethod(
            SQLiteDatabase::class.java,
            "execSQL",
            String::class.java,
            Array<Any>::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val sql = (param.args[0] as? String)?.uppercase() ?: return
                    if (sql.startsWith("DELETE FROM MESSAGES")) {
                        XposedBridge.log("Rhpatch: [Discord] Intercepted execSQL message deletion: $sql")
                        param.result = null // Skip execution
                    }
                }
            }
        )

        // Single arg execSQL
        XposedHelpers.findAndHookMethod(
            SQLiteDatabase::class.java,
            "execSQL",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val sql = (param.args[0] as? String)?.uppercase() ?: return
                    if (sql.startsWith("DELETE FROM MESSAGES")) {
                        XposedBridge.log("Rhpatch: [Discord] Intercepted execSQL message deletion: $sql")
                        param.result = null // Skip execution
                    }
                }
            }
        )
        
        XposedBridge.log("Rhpatch: [Discord] Anti-Delete (SQLite Hook) installed successfully")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Discord] Anti-Delete initialization failed: $it")
    }
}
