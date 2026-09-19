package com.rhdevs.rhpatch.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import com.crossbowffs.remotepreferences.RemotePreferences

class AntiSpamCallService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val isHidden = phoneNumber.isEmpty() || callDetails.handle == null || 
                       callDetails.callerDisplayNamePresentation != TelecomManager.PRESENTATION_ALLOWED

        var isSpam = false
        var spamReason = ""

        try {
            val prefs = RemotePreferences(this, "com.rhdevs.rhpatch.preferences", "prefs")
            if (prefs.getBoolean("antispam_call_enabled", false)) {

                // 1. Cek Blokir Nomor Privat / Tersembunyi
                if (prefs.getBoolean("antispam_call_hidden", false) && isHidden) {
                    isSpam = true
                    spamReason = "Nomor Privat/Tersembunyi"
                }

                // 2. Cek Blokir Panggilan di Luar Kontak
                if (!isSpam && prefs.getBoolean("antispam_call_non_contacts", false)) {
                    if (!isNumberInContacts(this, phoneNumber)) {
                        isSpam = true
                        spamReason = "Bukan Kontak Tersimpan"
                    }
                }

                // 3. Cek Daftar Blacklist Nomor / Awalan
                if (!isSpam && phoneNumber.isNotEmpty()) {
                    val numbersStr = prefs.getString("antispam_call_numbers", "") ?: ""
                    if (numbersStr.isNotEmpty()) {
                        val blockedNumbers = numbersStr.split(Regex("[,\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                        for (prefix in blockedNumbers) {
                            if (phoneNumber.contains(prefix)) {
                                isSpam = true
                                spamReason = "Blacklist ($prefix)"
                                break
                            }
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
                .setSkipCallLog(false) // Tetap simpan di call log agar pengguna tahu panggilan telah diblokir
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, response)
            saveSpamLog(phoneNumber.ifEmpty { "Nomor Tersembunyi" }, spamReason)
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
            // Jika izin belum diberikan, jangan blokir membabi-buta
            return true
        }
        return false
    }

    private fun saveSpamLog(number: String, reason: String) {
        try {
            val intent = Intent("com.rhdevs.rhpatch.LOG_SPAM").apply {
                putExtra("message", "Panggilan Masuk Ditolak: $number ($reason)")
                putExtra("type", "Panggilan")
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Throwable) {}
    }
}
