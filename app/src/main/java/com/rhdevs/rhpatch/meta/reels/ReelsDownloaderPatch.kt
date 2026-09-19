package com.rhdevs.rhpatch.meta.reels

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

// Objek penyimpan sementara khusus Reels (Terisolasi dari Feed dan Story)
var currentClipsHelper: Any? = null
var latestPlayingReelUrl: String? = null

val ReelsDownloaderPatch = patch(
    name = "Instagram Reels Downloader (Standalone)",
    description = "Modul pengunduh video Reels HD mandiri (Terpisah dari Feed dan Story)"
) {
    runCatching {
        MetaUnobfuscator.init(appContext)

        // 1. Hook Video Player / HeroPlayer untuk menangkap URL video Reels yang sedang diputar
        runCatching {
            val playerClasses = listOf(
                "com.facebook.video.heroplayer.ipc.VideoPlayRequest",
                "com.facebook.video.heroplayer.ipc.VideoSource",
                "com.instagram.ui.videoplayer.MediaViewVideoPlayer"
            )
            for (className in playerClasses) {
                val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
                XposedBridge.hookAllConstructors(cls, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val obj = param.thisObject
                            val extracted = mutableSetOf<String>()
                            extractReelsVideoUrls(obj, extracted, 0, mutableSetOf())
                            val best = extracted.filter { isReelMp4(it) }.maxByOrNull { it.length }
                            if (best != null) {
                                latestPlayingReelUrl = best
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        }

        // 2. Hook Clips/Reels Options & Action Sheet untuk menangkap objek Media Reels aktif
        runCatching {
            val clipsMethods = MetaUnobfuscator.findMethodUsingStrings("clips_viewer")
            for (method in clipsMethods) {
                val declaringClass = method.declaringClass ?: continue
                XposedBridge.hookAllConstructors(declaringClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentClipsHelper = param.thisObject
                    }
                })
            }
        }

        runCatching {
            val clipsActionMethods = MetaUnobfuscator.findMethodUsingStrings("clips_action_sheet")
            for (method in clipsActionMethods) {
                val declaringClass = method.declaringClass ?: continue
                XposedBridge.hookAllConstructors(declaringClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentClipsHelper = param.thisObject
                    }
                })
            }
        }

        // 3. Fallback hook: cari Fragment Clips Viewer langsung jika MetaUnobfuscator berbeda
        runCatching {
            val clipsViewerClasses = listOf(
                "com.instagram.clips.viewer.ClipsViewerFragment",
                "com.instagram.clips.viewer.ClipsMoreOptionsBottomsheetFragment"
            )
            for (clsName in clipsViewerClasses) {
                val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
                XposedBridge.hookAllMethods(cls, "onViewCreated", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentClipsHelper = param.thisObject
                    }
                })
            }
        }

        // 4. Injeksi Tombol Unduh Khusus Reels di BottomSheet/ActionSheet Reels
        val bottomSheetClasses = listOf(
            "com.instagram.igds.components.bottomsheet.BottomSheetFragment",
            "com.instagram.clips.viewer.ClipsMoreOptionsBottomsheetFragment"
        )

        for (bsClassName in bottomSheetClasses) {
            val bottomSheetClass = XposedHelpers.findClassIfExists(bsClassName, classLoader) ?: continue

            XposedBridge.hookAllMethods(bottomSheetClass, "onViewCreated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.args[0] as? ViewGroup ?: return
                        val context = view.context

                        // Periksa preferensi Reels Downloader
                        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("pref_reels_downloader", true)) return

                        // Hindari injeksi pada komentar, DM, composer, dan aura icon picker
                        val bsFrag = param.thisObject as? androidx.fragment.app.Fragment
                        if (bsFrag != null) {
                            for (f in bsFrag.childFragmentManager.fragments) {
                                val fName = f.javaClass.name.lowercase()
                                if (fName.contains("comment") || fName.contains("composer") ||
                                    fName.contains("direct_thread") || fName.contains("broadcast") ||
                                    fName.contains("iconpicker") || fName.contains("aura")) {
                                    return
                                }
                            }
                        }

                        val activity = context as? Activity ?: return

                        // Pastikan konteks berada pada tampilan Reels/Clips
                        if (!isReelsActiveContext(activity)) return

                        // Hindari duplikasi tombol
                        if (view.findViewWithTag<View>("rhp_reels_download_btn") != null) return

                        val dp = context.resources.displayMetrics.density
                        val btnLayout = LinearLayout(context).apply {
                            tag = "rhp_reels_download_btn"
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
                            isClickable = true
                            isFocusable = true
                            setBackgroundResource(android.R.drawable.list_selector_background)

                            val icon = ImageView(context).apply {
                                setImageResource(android.R.drawable.stat_sys_download)
                                val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                setColorFilter(if (isNightMode) Color.parseColor("#10B981") else Color.parseColor("#059669"))
                                val iconSize = (24 * dp).toInt()
                                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                                    rightMargin = (16 * dp).toInt()
                                }
                            }

                            val tv = TextView(context).apply {
                                text = "Unduh Video Reels (HD)"
                                textSize = 16f
                                val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                setTextColor(if (isNightMode) Color.WHITE else Color.BLACK)
                            }

                            addView(icon)
                            addView(tv)

                            setOnClickListener {
                                extractAndDownloadReel(context, activity)
                            }
                        }

                        var added = false
                        for (i in 0 until view.childCount) {
                            val child = view.getChildAt(i)
                            if (child is LinearLayout) {
                                child.addView(btnLayout, 0)
                                added = true
                                break
                            } else if (child is androidx.recyclerview.widget.RecyclerView) {
                                val parent = child.parent as? ViewGroup
                                if (parent is LinearLayout) {
                                    parent.addView(btnLayout, parent.indexOfChild(child))
                                    added = true
                                    break
                                }
                            }
                        }

                        if (!added) {
                            view.addView(btnLayout, 0)
                        }
                        XposedBridge.log("Rhpatch: [Reels] Tombol unduh Reels HD berhasil disuntikkan")
                    } catch (e: Throwable) {
                        XposedBridge.log("Rhpatch: [Reels] Gagal menyuntikkan tombol: $e")
                    }
                }
            })

            XposedBridge.hookAllMethods(bottomSheetClass, "onDestroyView", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentClipsHelper = null
                }
            })
        }
    }
}

fun isReelsActiveContext(activity: Activity): Boolean {
    if (currentClipsHelper != null || latestPlayingReelUrl != null) return true
    val fa = activity as? androidx.fragment.app.FragmentActivity
    if (fa != null) {
        try {
            for (f in fa.supportFragmentManager.fragments) {
                val fName = f.javaClass.name.lowercase()
                if (fName.contains("clips") || (fName.contains("reel") && !fName.contains("story") && !fName.contains("detail"))) {
                    return true
                }
            }
        } catch (_: Throwable) {}
    }
    return false
}

fun isReelMp4(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".mp4") && !lower.contains("video")) return false
    if (lower.contains("mime=audio") || lower.contains("/audio/") || lower.contains("audio_aac")) return false
    return true
}

fun extractAndDownloadReel(context: Context, activity: Activity) {
    Toast.makeText(context, "[Rhpatch] Mengekstrak video Reels HD...", Toast.LENGTH_SHORT).show()

    Thread {
        val extractedUrls = mutableSetOf<String>()

        // Prioritas 1: Gunakan URL video pemutar aktif jika tersedia
        latestPlayingReelUrl?.let {
            if (it.isNotBlank() && isReelMp4(it)) extractedUrls.add(it)
        }

        // Prioritas 2: Ekstraksi dari objek Clips Helper & View Decorator
        val startObj = currentClipsHelper ?: activity.window.decorView.rootView
        if (startObj != null) {
            extractReelsVideoUrls(startObj, extractedUrls, 0, mutableSetOf())
        }

        // Filter URL MP4 beresolusi penuh
        val mp4Urls = extractedUrls.filter { isReelMp4(it) }

        // Ambil URL dengan string terpanjang (resolusi terbaik dengan token lengkap)
        val bestVideoUrl = mp4Urls.maxByOrNull { it.length }

        activity.runOnUiThread {
            if (bestVideoUrl != null) {
                downloadReelsFile(context, bestVideoUrl)
            } else {
                Toast.makeText(context, "[Rhpatch] URL video Reels tidak ditemukan pada post ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}

// Regex untuk mencocokkan URL MP4 langsung maupun di dalam DASH XML Manifest (<MPD>, <BaseURL>)
private val URL_REGEX = Regex("https?://[^\"'<>\\s]+(?:fbcdn\\.net|cdninstagram\\.com)[^\"'<>\\s]*(?:\\.mp4|oe=)[^\"'<>\\s]*")
private val BASE_URL_XML_REGEX = Regex("<BaseURL>([^<]+)</BaseURL>")

fun extractReelsVideoUrls(obj: Any?, urls: MutableSet<String>, depth: Int, visited: MutableSet<Int>) {
    if (obj == null || depth > 12) return
    val hash = System.identityHashCode(obj)
    if (!visited.add(hash)) return

    if (obj is String) {
        val lower = obj.lowercase()
        if ((lower.startsWith("http://") || lower.startsWith("https://")) &&
            (lower.contains("fbcdn.net") || lower.contains("cdninstagram.com")) &&
            (lower.contains(".mp4") || lower.contains("video"))) {
            urls.add(obj.replace("&amp;", "&"))
            return
        }

        if (obj.contains("<MPD") || obj.contains("<BaseURL>") || obj.contains("fbcdn.net") || obj.contains("cdninstagram.com")) {
            BASE_URL_XML_REGEX.findAll(obj).forEach { match ->
                val rawUrl = match.groupValues[1].replace("&amp;", "&")
                if (rawUrl.startsWith("http")) urls.add(rawUrl)
            }
            URL_REGEX.findAll(obj).forEach { match ->
                val rawUrl = match.value.replace("&amp;", "&")
                urls.add(rawUrl)
            }
        }
        return
    }

    try {
        val cls = obj.javaClass
        if (cls.name.startsWith("android.") || cls.name.startsWith("java.") || cls.name.startsWith("androidx.")) {
            if (obj is Collection<*>) {
                for (item in obj) extractReelsVideoUrls(item, urls, depth + 1, visited)
            } else if (obj is Array<*>) {
                for (item in obj) extractReelsVideoUrls(item, urls, depth + 1, visited)
            } else if (obj is Map<*, *>) {
                for (value in obj.values) extractReelsVideoUrls(value, urls, depth + 1, visited)
            }
            return
        }

        if (obj is View) {
            val tag = obj.tag
            if (tag != null) extractReelsVideoUrls(tag, urls, depth + 1, visited)
            if (obj is ViewGroup) {
                for (i in 0 until obj.childCount) {
                    extractReelsVideoUrls(obj.getChildAt(i), urls, depth + 1, visited)
                }
            }
        }

        var currentCls: Class<*>? = cls
        while (currentCls != null && currentCls != Any::class.java) {
            for (field in currentCls.declaredFields) {
                if (field.type.isPrimitive) continue
                val fieldName = field.name.lowercase()
                if (fieldName.contains("profile") || fieldName.contains("avatar")) continue

                field.isAccessible = true
                val value = field.get(obj) ?: continue
                extractReelsVideoUrls(value, urls, depth + 1, visited)
            }
            currentCls = currentCls.superclass
        }
    } catch (_: Exception) {}
}

fun downloadReelsFile(context: Context, url: String) {
    runCatching {
        val filename = "rhpatch_reel_" + System.currentTimeMillis() + ".mp4"
        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
        val downloadPath = prefs.getString("pref_download_path", "Rhpatch") ?: "Rhpatch"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("User-Agent", "Instagram 300.0.0.35.109 Android")
            addRequestHeader("Referer", "https://www.instagram.com/")
            setTitle("Rhpatch Reels Downloader")
            setDescription(filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadPath + "/" + filename)
            setMimeType("video/mp4")
        }
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "[Rhpatch] Mengunduh Reels HD...", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "[Rhpatch] Gagal mengunduh: " + it.message, Toast.LENGTH_SHORT).show()
    }
}
