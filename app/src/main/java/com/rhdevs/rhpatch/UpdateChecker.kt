package com.rhdevs.rhpatch

import android.app.Activity
import com.rhdevs.rhpatch.xposed.core.WppCore
import com.rhdevs.rhpatch.xposed.core.components.AlertDialogWpp
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XposedBridge
import io.noties.markwon.Markwon
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpdateChecker(private val mActivity: Activity, private val isManual: Boolean = false) : Runnable {

    companion object {
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/Rhdevs71/apahayo/releases/latest"
        private const val TELEGRAM_UPDATE_URL = "https://t.me/rhdevs"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun run() {
        try {
            val request = okhttp3.Request.Builder()
                .url(LATEST_RELEASE_API)
                .build()

            val hash: String
            val changelog: String
            val publishedAt: String

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404 && isManual) {
                        mActivity.runOnUiThread {
                            val dialog = AlertDialogWpp(mActivity)
                            dialog.setTitle("Pembaruan")
                            dialog.setMessage("Belum ada rilis versi terbaru.")
                            dialog.setPositiveButton("OK") { d, _ -> d.dismiss() }
                            dialog.show()
                        }
                    } else if (isManual) {
                        mActivity.runOnUiThread {
                            android.widget.Toast.makeText(mActivity, "Gagal memeriksa pembaruan: ${response.code}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    return
                }

                val content = response.body.string()
                val release = JSONObject(content)
                val tagName = release.optString("tag_name", "")

                if (tagName.isBlank()) return

                hash = tagName.split("-")[1].trim()
                changelog = release.optString("body", "No changelog available.").trim()
                publishedAt = release.optString("published_at", "")
            }

            if (hash.isBlank()) return

            val packageInfo = try {
                mActivity.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, 0)
            } catch (e: Exception) {
                XposedBridge.log(e)
                return
            }

            val isNewVersion = !packageInfo.versionName!!.lowercase().contains(hash.lowercase().trim())
            val isIgnored = WppCore.getPrivString("ignored_version", "") == hash

            if (isNewVersion && !isIgnored) {
                mActivity.runOnUiThread {
                    showUpdateDialog(hash, changelog, publishedAt)
                }
            }
        } catch (e: Exception) {
            XposedBridge.log(e)
            if (isManual) {
                mActivity.runOnUiThread {
                    android.widget.Toast.makeText(mActivity, "Gagal memeriksa pembaruan: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showUpdateDialog(hash: String, changelog: String, publishedAt: String) {
        try {
            val markwon = Markwon.create(mActivity)
            val dialog = AlertDialogWpp(mActivity)

            val formattedDate = formatPublishedDate(publishedAt)

            val message = buildString {
                append("ðŸ“¦ **Version:** `").append(hash).append("`\n")
                if (formattedDate.isNotEmpty()) {
                    append("ðŸ“… **Released:** ").append(formattedDate).append("\n")
                }
                append("\n### What's New\n\n").append(changelog)
            }

            dialog.setTitle("ðŸŽ‰ New Update Available!")
            dialog.setMessage(markwon.toMarkdown(message))
            dialog.setNegativeButton("Ignore") { dialog, _ ->
                WppCore.setPrivString("ignored_version", hash)
                dialog.dismiss()
            }
            dialog.setPositiveButton("Update Now") { dialog, _ ->
                Utils.openLink(mActivity, TELEGRAM_UPDATE_URL)
                dialog.dismiss()
            }
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatPublishedDate(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return ""

        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = isoFormat.parse(isoDate)
            if (date != null) {
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                displayFormat.format(date)
            } else ""
        } catch (e: Exception) {
            XposedBridge.log(e)
            ""
        }
    }
}
