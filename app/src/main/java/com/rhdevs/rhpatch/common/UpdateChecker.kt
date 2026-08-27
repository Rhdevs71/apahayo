package com.rhdevs.rhpatch.common

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.BuildConfig
import com.rhdevs.rhpatch.youtube.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readString
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body_html") val releaseNoteHtml: String,
    @SerializedName("html_url") val releaseUrl: String
)

data class VersionInfo(val versionCode: Int, val versionName: String) {
    companion object {
        fun fromTagName(tagName: String): VersionInfo {
            val versionCode: Int
            val versionName: String

            val split = tagName.split('-', limit = 2)
            if (split.count() == 2) {
                versionCode = split[0].toIntOrNull() ?: 0
                versionName = split[1]
            } else {
                versionCode = tagName.split('.').last().toIntOrNull() ?: 0
                versionName = tagName
            }
            return VersionInfo(versionCode, versionName)
        }
    }
}

const val OWNER = "Rhdevs71"
const val REPO = "apahayo"
const val currentVersionCode = BuildConfig.VERSION_CODE

class UpdateChecker(activity: Activity? = null) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, err ->
            Logger.printException({ "coroutineContext error" }, err)
        }

    private var currentActivity = WeakReference<Activity>(activity)
    private var latestVersionInfo: VersionInfo? = null
    private var latestRelease: ReleaseInfo? = null

    private var unhook: XC_MethodHook.Unhook? = null

    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun getActivity(): Activity? = currentActivity.get()

    fun hookNewActivity() {
        runCatching {
            unhook = XposedHelpers.findAndHookMethod(
                Instrumentation::class.java,
                "newActivity",
                ClassLoader::class.java,
                String::class.java,
                Intent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentActivity = WeakReference(param.result as Activity)
                        autoCheckUpdate()
                        unhook?.unhook()
                    }
                })
        }
    }

    fun autoCheckUpdate() {
        if (Random.nextInt(0, 10) != 0) return
        Logger.printInfo { "start auto check update." }
        runCatching { checkUpdate() }
    }

    fun checkUpdate(silent: Boolean = true) {
        if (!silent) {
            Handler(Looper.getMainLooper()).post {
                val act = getActivity()
                if (act != null) {
                    Toast.makeText(act, "Memeriksa pembaruan RHPatch...", Toast.LENGTH_SHORT).show()
                } else {
                    App.instance?.let { Toast.makeText(it, "Memeriksa pembaruan RHPatch...", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        launch {
            try {
                val response = Fuel.get(
                    "https://api.github.com/repos/$OWNER/$REPO/releases/latest",
                    headers = mapOf("Accept" to "application/vnd.github.html+json")
                )
                if (response.statusCode != 200) {
                    if (response.statusCode != 404) {
                        Logger.printException { "Failed to fetch latest release: HTTP ${response.statusCode}" }
                    }
                    if (!silent) {
                        Handler(Looper.getMainLooper()).post {
                            val act = getActivity()
                            val message = if (response.statusCode == 404) {
                                "Belum ada rilis versi terbaru di repositori."
                            } else {
                                "Gagal memeriksa pembaruan (HTTP ${response.statusCode})."
                            }
                            if (act != null && !act.isFinishing && !act.isDestroyed) {
                                AlertDialog.Builder(act)
                                    .setTitle("Pembaruan")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                App.instance?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                    return@launch
                }

                val content = response.source.readString()
                val release = Gson().fromJson(content, ReleaseInfo::class.java)
                val versionInfo = VersionInfo.fromTagName(release.tagName)
                latestRelease = release
                latestVersionInfo = versionInfo

                if (versionInfo.versionCode > currentVersionCode) {
                    Logger.printInfo { "Found new version of Rhpatch ${release.tagName}" }
                    showUpdateDialog(release, versionInfo)
                } else {
                    Logger.printInfo { "no update found for Rhpatch" }
                    if (!silent) {
                        Handler(Looper.getMainLooper()).post {
                            val act = getActivity()
                            if (act != null && !act.isFinishing && !act.isDestroyed) {
                                AlertDialog.Builder(act)
                                    .setTitle("Pembaruan")
                                    .setMessage("Versi RHPatch saat ini (${BuildConfig.VERSION_NAME}) sudah yang terbaru.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                App.instance?.let { Toast.makeText(it, "Versi RHPatch saat ini sudah yang terbaru.", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Logger.printException({ "checkUpdate error" }, e)
                if (!silent) {
                    Handler(Looper.getMainLooper()).post {
                        val act = getActivity()
                        val errMessage = "Gagal memeriksa pembaruan: ${e.message}"
                        if (act != null && !act.isFinishing && !act.isDestroyed) {
                            AlertDialog.Builder(act)
                                .setTitle("Error")
                                .setMessage(errMessage)
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            App.instance?.let { Toast.makeText(it, errMessage, Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(release: ReleaseInfo, versionInfo: VersionInfo) {
        Handler(Looper.getMainLooper()).post {
            try {
                val act = getActivity() ?: return@post
                if (act.isFinishing || act.isDestroyed) return@post

                AlertDialog.Builder(act)
                    .setTitle("Versi Baru RHPatch Ditemukan: ${versionInfo.versionName}")
                    .setMessage(Html.fromHtml(release.releaseNoteHtml, Html.FROM_HTML_MODE_LEGACY))
                    .setPositiveButton("Unduh") { _, _ ->
                        openReleasePage(release)
                    }
                    .setNegativeButton("Batal", null)
                    .create()
                    .show()
            } catch (_: Exception) {}
        }
    }

    private fun openReleasePage(release: ReleaseInfo? = latestRelease) {
        val url = release?.releaseUrl ?: "https://github.com/$OWNER/$REPO/releases/latest"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val act = getActivity()
        if (act != null) {
            act.startActivity(intent)
        } else {
            App.instance?.startActivity(intent)
        }
    }
}
