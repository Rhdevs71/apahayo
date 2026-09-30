package com.rhdevs.rhpatch.common

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

data class ReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body_html") val releaseNoteHtml: String? = null,
    @SerializedName("body") val releaseNoteBody: String? = null,
    @SerializedName("html_url") val releaseUrl: String,
    @SerializedName("assets") val assets: List<ReleaseAsset>? = null
)

data class VersionInfo(
    val tagName: String,
    val versionName: String,
    val commitHash: String?,
    val isNewVersion: Boolean
) {
    companion object {
        fun fromTagName(tagName: String): VersionInfo {
            val cleanTag = tagName.removePrefix("v").trim()
            val split = cleanTag.split('-', limit = 2)
            val verName = split[0]
            val hash = if (split.size > 1) split[1].trim() else null

            val isNew = isUpdateAvailable(verName, hash)
            return VersionInfo(
                tagName = tagName,
                versionName = if (!hash.isNullOrEmpty()) "$verName ($hash)" else verName,
                commitHash = hash,
                isNewVersion = isNew
            )
        }

        private fun isUpdateAvailable(remoteVersion: String, remoteHash: String?): Boolean {
            // 1. Jika rilis remote memiliki commit hash (format: 1.5.6-706eae3a)
            if (!remoteHash.isNullOrBlank()) {
                val currentHash = BuildConfig.COMMIT_HASH
                val currentVersionName = BuildConfig.VERSION_NAME

                val isSameCommit = currentHash.equals(remoteHash, ignoreCase = true) ||
                        currentVersionName.contains(remoteHash, ignoreCase = true)

                if (!isSameCommit) {
                    return true
                }
            }

            // 2. Periksa versi semantik jika rilis menggunakan format semver (misal 1.5.7 vs 1.5.6)
            return compareSemanticVersion(remoteVersion, BuildConfig.VERSION_NAME.substringBefore(" ")) > 0
        }

        private fun compareSemanticVersion(v1: String, v2: String): Int {
            val parts1 = v1.split('.').mapNotNull { it.toIntOrNull() }
            val parts2 = v2.split('.').mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLen) {
                val num1 = parts1.getOrElse(i) { 0 }
                val num2 = parts2.getOrElse(i) { 0 }
                if (num1 != num2) {
                    return num1.compareTo(num2)
                }
            }
            return 0
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

                if (versionInfo.isNewVersion) {
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

                val note = when {
                    !release.releaseNoteHtml.isNullOrBlank() ->
                        Html.fromHtml(release.releaseNoteHtml, Html.FROM_HTML_MODE_LEGACY)
                    !release.releaseNoteBody.isNullOrBlank() ->
                        release.releaseNoteBody
                    else -> "Pembaruan versi terbaru telah tersedia."
                }

                AlertDialog.Builder(act)
                    .setTitle("Versi Baru RHPatch Tersedia: ${versionInfo.versionName}")
                    .setMessage(note)
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
        val apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        val url = apkAsset?.downloadUrl ?: release?.releaseUrl ?: "https://github.com/$OWNER/$REPO/releases/latest"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val act = getActivity()
        try {
            if (act != null) {
                act.startActivity(intent)
            } else {
                App.instance?.startActivity(intent)
            }
        } catch (_: Exception) {
            try {
                val chooser = Intent.createChooser(intent, "Buka Tautan Unduhan").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (act != null) {
                    act.startActivity(chooser)
                } else {
                    App.instance?.startActivity(chooser)
                }
            } catch (_: Exception) {
                val targetContext = act ?: App.instance
                if (targetContext != null) {
                    try {
                        val clipboard = targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("RHPatch Download URL", url)
                        clipboard?.setPrimaryClip(clip)
                        if (act != null && !act.isFinishing && !act.isDestroyed) {
                            AlertDialog.Builder(act)
                                .setTitle("Browser Tidak Ditemukan")
                                .setMessage("Perangkat Anda tidak memiliki aplikasi browser aktif untuk membuka tautan secara langsung.\n\nTautan unduhan telah disalin ke papan klip:\n$url")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            Toast.makeText(
                                targetContext,
                                "Tidak ada aplikasi browser yang ditemukan. Tautan unduhan telah disalin ke papan klip.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
