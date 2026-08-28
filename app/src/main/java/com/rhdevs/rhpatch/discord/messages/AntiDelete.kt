package com.rhdevs.rhpatch.discord.messages

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
        val sqliteClasses = listOf(
            "android.database.sqlite.SQLiteDatabase",
            "io.requery.android.database.sqlite.SQLiteDatabase",
            "net.sqlcipher.database.SQLiteDatabase",
            "com.balthazargargani.sqlite.Database"
        )
        
        var hookedCount = 0
        sqliteClasses.forEach { className ->
            val sqliteClass = XposedHelpers.findClassIfExists(className, classLoader) ?: return@forEach
            
            runCatching {
                // Hook delete method
                XposedHelpers.findAndHookMethod(
                    sqliteClass,
                    "delete",
                    String::class.java, // table
                    String::class.java, // whereClause
                    Array<String>::class.java, // whereArgs
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val table = param.args[0] as? String ?: return
                            val whereClause = param.args[1] as? String
                    val whereArgs = param.args[2] as? Array<String>
                    
                    if (table == "messages" || table == "message_records") {
                        XposedBridge.log("Rhpatch: [Discord] Intercepted message deletion: where=$whereClause args=${whereArgs?.joinToString()}")
                        param.result = 1 
                    }
                }
            }
                )

                // Also hook the raw execSQL for "DELETE FROM"
                XposedHelpers.findAndHookMethod(
                    sqliteClass,
                    "execSQL",
                    String::class.java,
                    Array<Any>::class.java,
                    object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val sql = (param.args[0] as? String)?.uppercase() ?: return
                    if (sql.startsWith("DELETE FROM MESSAGES") || sql.startsWith("DELETE FROM MESSAGE_RECORDS")) {
                        XposedBridge.log("Rhpatch: [Discord] Intercepted execSQL message deletion: $sql")
                        param.result = null // Skip execution
                    }
                }
            }
        )

        // Single arg execSQL
        XposedHelpers.findAndHookMethod(
            sqliteClass, // Fixed: use sqliteClass instead of hardcoded SQLiteDatabase to support other engines
            "execSQL",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val sql = (param.args[0] as? String)?.uppercase() ?: return
                    if (sql.startsWith("DELETE FROM MESSAGES") || sql.startsWith("DELETE FROM MESSAGE_RECORDS")) {
                        XposedBridge.log("Rhpatch: [Discord] Intercepted execSQL message deletion: $sql")
                        param.result = null // Skip execution
                    }
                }
            }
                )
                
                hookedCount++
            }
        }
        
        if (hookedCount > 0) {
            XposedBridge.log("Rhpatch: [Discord] Anti-Delete (SQLite Hook) installed successfully on $hookedCount engines")
        } else {
            XposedBridge.log("Rhpatch: [Discord] Anti-Delete initialization failed: No SQLite engine found.")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [Discord] Anti-Delete initialization failed: $it")
    }
}
