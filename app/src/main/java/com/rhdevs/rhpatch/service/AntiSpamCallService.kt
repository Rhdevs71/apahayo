package com.rhdevs.rhpatch.service

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import com.crossbowffs.remotepreferences.RemotePreferences

class AntiSpamCallService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        
        var isSpam = false
        try {
            val prefs = RemotePreferences(this, "com.rhdevs.rhpatch.preferences", "prefs")
            if (prefs.getBoolean("antispam_call_enabled", false)) {
                // If the user wants to block everyone EXCEPT contacts
                val blockUnknown = prefs.getBoolean("antispam_block_unknown_enabled", false)
                if (blockUnknown) {
                    if (!isNumberInContacts(this, phoneNumber)) {
                        isSpam = true
                    }
                }
                
                // Fallback to keyword/blacklist checking if not already marked as spam
                if (!isSpam) {
                    val keywordsStr = prefs.getString("antispam_call_numbers", "") ?: ""
                    if (keywordsStr.isNotEmpty()) {
                        val blockedNumbers = keywordsStr.split(Regex("[,\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                        if (blockedNumbers.any { phoneNumber.contains(it) }) {
                            isSpam = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isSpam) {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, response)
        } else {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun isNumberInContacts(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) return false
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If we don't have contacts permission, it throws an exception. We shouldn't block all calls blindly if it fails.
            return true 
        }
        return false
    }
}