package com.rhdevs.rhpatch.system

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class SpamConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        
        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        
        val key = uri.lastPathSegment ?: return null
        
        if (prefs.contains(key)) {
            val allEntries = prefs.all
            val value = allEntries[key]
            
            when (value) {
                is String -> cursor.addRow(arrayOf(key, value, "string"))
                is Boolean -> cursor.addRow(arrayOf(key, if (value) "1" else "0", "boolean"))
                is Int -> cursor.addRow(arrayOf(key, value.toString(), "int"))
                is Long -> cursor.addRow(arrayOf(key, value.toString(), "long"))
                is Float -> cursor.addRow(arrayOf(key, value.toString(), "float"))
                else -> cursor.addRow(arrayOf(key, value?.toString() ?: "", "string"))
            }
        }
        
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
