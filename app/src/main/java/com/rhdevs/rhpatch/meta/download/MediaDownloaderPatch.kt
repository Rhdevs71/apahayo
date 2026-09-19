package com.rhdevs.rhpatch.meta.download

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.util.Base64
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

// Cache objek pembantu overflow terakhir yang dibuka (Feed & Story)
var currentOverflowHelper: Any? = null
var currentStoryHelper: Any? = null
val videoViewCache = java.util.WeakHashMap<View, Boolean>()

val MediaDownloaderPatch = patch(
    name = "Instagram Media Downloader (Feed & Story)",
    description = "Ekstraksi Media Object secara presisi khusus Postingan Feed dan Story"
) {
    runCatching {
        MetaUnobfuscator.init(appContext)

        // 1. Hook MediaOptionsOverflowHelper untuk menangkap objek Media Feed
        runCatching {
            val helperMethods = MetaUnobfuscator.findMethodUsingStrings("MediaOptionsOverflowHelper")
            val helperClass = helperMethods.firstOrNull()?.declaringClass
            if (helperClass != null) {
                XposedBridge.hookAllConstructors(helperClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentOverflowHelper = param.thisObject
                        XposedBridge.log("Rhpatch: Berhasil menyadap instance MediaOptionsOverflowHelper!")
                    }
                })
            }
        }

        // 1.5 Hook Story Options Helper
        runCatching {
            val storyMethods = MetaUnobfuscator.findMethodUsingStrings("friendships/mute_friend_reel/%s/")
            val storyClass = storyMethods.firstOrNull()?.declaringClass
            if (storyClass != null) {
                XposedBridge.hookAllConstructors(storyClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentStoryHelper = param.thisObject
                        XposedBridge.log("Rhpatch: Berhasil menyadap StoryOptionsHelper!")
                    }
                })
            }
        }

        // 2. Injeksi Tombol Pilihan Unduhan ke BottomSheet
        runCatching {
            val igdsBottomSheetClass = XposedHelpers.findClassIfExists("com.instagram.igds.components.bottomsheet.BottomSheetFragment", classLoader)
            if (igdsBottomSheetClass != null) {
                XposedBridge.hookAllMethods(igdsBottomSheetClass, "onViewCreated", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val view = param.args[0] as? ViewGroup ?: return
                            val context = view.context

                            // Periksa preferensi Media Downloader
                            val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                            if (!prefs.getBoolean("pref_downloader", true)) return

                            // GUARD: Jangan suntikkan ke lembar komentar, DM, composer, atau aura icon picker
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

                            if (isCommentOrNonMediaSheet(view)) return

                            val activity = context as? Activity ?: return

                            // Hindari duplikasi tombol
                            if (view.findViewWithTag<View>("rhp_bs_download") != null) return

                            val dp = context.resources.displayMetrics.density
                            val btnLayout = LinearLayout(context).apply {
                                tag = "rhp_bs_download"
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                                setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
                                isClickable = true
                                isFocusable = true
                                setBackgroundResource(android.R.drawable.list_selector_background)

                                val icon = ImageView(context).apply {
                                    setImageResource(android.R.drawable.stat_sys_download)
                                    val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                    setColorFilter(if (isNightMode) Color.parseColor("#F5F5F5") else Color.parseColor("#262626"))
                                    val iconSize = (24 * dp).toInt()
                                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                                        rightMargin = (16 * dp).toInt()
                                    }
                                }

                                val tv = TextView(context).apply {
                                    text = "Pilihan Unduhan (Rhpatch)"
                                    textSize = 16f
                                    val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                    setTextColor(if (isNightMode) Color.WHITE else Color.BLACK)
                                }

                                addView(icon)
                                addView(tv)

                                setOnClickListener {
                                    showPikoStyleMainMenu(context, activity)
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
                            XposedBridge.log("Rhpatch: [Feed/Story] Tombol Pilihan Unduhan berhasil disuntikkan")
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch: [Feed/Story] Gagal menyuntikkan tombol: $e")
                        }
                    }
                })

                XposedBridge.hookAllMethods(igdsBottomSheetClass, "onDestroyView", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        currentOverflowHelper = null
                        currentStoryHelper = null
                    }
                })
            }
        }
    }
}

fun showPikoStyleMainMenu(context: Context, activity: Activity) {
    val items = arrayOf("Pilihan unduhan", "Pilihan lainnya")
    AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Rhpatch")
        .setItems(items) { _, which ->
            when (which) {
                0 -> showPikoStyleDownloadMenu(context, activity)
                1 -> showPikoStyleMoreMenu(context, activity)
            }
        }
        .show()
}

fun showPikoStyleDownloadMenu(context: Context, activity: Activity) {
    Toast.makeText(context, "[Rhpatch] Memproses ekstrak media...", Toast.LENGTH_SHORT).show()

    Thread {
        val allUrls = LinkedHashSet<String>()

        val primaryTarget = currentOverflowHelper ?: currentStoryHelper
        if (primaryTarget != null) {
            DeepMediaExtractor.extractAllUrls(primaryTarget, allUrls, 0, mutableSetOf())
        }

        val mediaRoot = activity.window.decorView.rootView as? ViewGroup
        if (mediaRoot != null) {
            val mostVisible = findMostVisibleMediaView(mediaRoot, context)
            if (mostVisible != null) {
                DeepMediaExtractor.extractAllUrls(mostVisible, allUrls, 0, mutableSetOf())
            }
            DeepMediaExtractor.extractAllUrls(mediaRoot, allUrls, 0, mutableSetOf())
        }

        val allMp4s = deduplicateResolutions(allUrls.filter { isUnifiedMp4(it) })
        val allImages = deduplicateResolutions(allUrls.filter { isHighResImage(it) })
        val allAudios = deduplicateResolutions(allUrls.filter { isAudio(it) })

        activity.runOnUiThread {
            if (allMp4s.isEmpty() && allImages.isEmpty() && allAudios.isEmpty()) {
                Toast.makeText(context, "[Rhpatch] Media tidak ditemukan pada postingan ini.", Toast.LENGTH_SHORT).show()
                return@runOnUiThread
            }

            val optionLabels = mutableListOf<String>()
            val optionActions = mutableListOf<Runnable>()

            val totalMedia = allMp4s.size + allImages.size
            if (totalMedia > 1) {
                optionLabels.add("Unduh semua media ($totalMedia item)")
                optionActions.add(Runnable {
                    allImages.forEach { downloadInstagramMedia(context, it, false) }
                    allMp4s.forEach { downloadInstagramMedia(context, it, true) }
                })
            }

            if (allImages.isNotEmpty()) {
                optionLabels.add("Unduh sebagai gambar")
                optionActions.add(Runnable {
                    downloadInstagramMedia(context, allImages.first(), false)
                })
            }

            if (allMp4s.isNotEmpty()) {
                optionLabels.add("Unduh video")
                optionActions.add(Runnable {
                    val bestMp4 = allMp4s.maxByOrNull { it.length } ?: allMp4s.first()
                    downloadInstagramMedia(context, bestMp4, true)
                })
            }

            if (allAudios.isNotEmpty()) {
                optionLabels.add("Unduh audio")
                optionActions.add(Runnable {
                    downloadInstagramMedia(context, allAudios.first(), isVideo = false, isAudioOnly = true)
                })
            } else if (allMp4s.isNotEmpty()) {
                optionLabels.add("Unduh audio (dari Video)")
                optionActions.add(Runnable {
                    val bestMp4 = allMp4s.maxByOrNull { it.length } ?: allMp4s.first()
                    downloadInstagramMedia(context, bestMp4, isVideo = false, isAudioOnly = true)
                })
            }

            val shareUrl = allMp4s.firstOrNull() ?: allImages.firstOrNull()
            if (shareUrl != null) {
                optionLabels.add("Salin tautan media")
                optionActions.add(Runnable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Media URL", shareUrl))
                    Toast.makeText(context, "[Rhpatch] Tautan media disalin!", Toast.LENGTH_SHORT).show()
                })
            }

            if (allImages.size > 1) {
                optionLabels.add("Varian gambar (${allImages.size})")
                optionActions.add(Runnable {
                    val subLabels = allImages.mapIndexed { idx, _ -> "Gambar ${idx + 1}" }.toTypedArray()
                    AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Pilih Varian Gambar")
                        .setItems(subLabels) { _, which ->
                            downloadInstagramMedia(context, allImages[which], false)
                        }
                        .show()
                })
            }

            if (allMp4s.size > 1) {
                optionLabels.add("Varian video (${allMp4s.size})")
                optionActions.add(Runnable {
                    val subLabels = allMp4s.mapIndexed { idx, _ -> "Video ${idx + 1}" }.toTypedArray()
                    AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Pilih Varian Video")
                        .setItems(subLabels) { _, which ->
                            downloadInstagramMedia(context, allMp4s[which], true)
                        }
                        .show()
                })
            }

            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilihan Unduhan")
                .setItems(optionLabels.toTypedArray()) { _, which ->
                    optionActions[which].run()
                }
                .show()
        }
    }.start()
}

fun showPikoStyleMoreMenu(context: Context, activity: Activity) {
    Toast.makeText(context, "[Rhpatch] Mengekstrak data teks...", Toast.LENGTH_SHORT).show()

    Thread {
        val extractedTexts = LinkedHashSet<String>()
        val startObj = currentOverflowHelper ?: currentStoryHelper ?: activity.window.decorView.rootView
        if (startObj != null) {
            DeepTextExtractor.extractAllTexts(startObj, extractedTexts, 0, mutableSetOf())
        }

        val caption = extractedTexts.filter { it.contains(" ") && it.length > 10 }
            .sortedByDescending { it.length }
            .firstOrNull()

        val mediaId = extractedTexts.firstOrNull { Regex("^[0-9]+_[0-9]+$").matches(it) }

        activity.runOnUiThread {
            val moreLabels = mutableListOf<String>()
            val moreActions = mutableListOf<Runnable>()

            if (caption != null) {
                moreLabels.add("Salin teks caption")
                moreActions.add(Runnable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Caption", caption))
                    Toast.makeText(context, "[Rhpatch] Caption disalin!", Toast.LENGTH_SHORT).show()
                })
            }

            if (mediaId != null) {
                moreLabels.add("Salin Media ID")
                moreActions.add(Runnable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("MediaID", mediaId))
                    Toast.makeText(context, "[Rhpatch] Media ID disalin!", Toast.LENGTH_SHORT).show()
                })
            }

            moreLabels.add("Buka di browser")
            moreActions.add(Runnable {
                val url = "https://www.instagram.com/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            })

            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilihan Lainnya")
                .setItems(moreLabels.toTypedArray()) { _, which ->
                    moreActions[which].run()
                }
                .show()
        }
    }.start()
}

fun isCommentOrNonMediaSheet(view: ViewGroup): Boolean {
    var isComment = false
    fun checkChild(v: View) {
        if (isComment) return
        val tag = (v.tag as? String)?.lowercase() ?: ""
        val desc = v.contentDescription?.toString()?.lowercase() ?: ""
        val clsName = v.javaClass.name.lowercase()

        if (tag.contains("comment") || tag.contains("composer") || tag.contains("emoji_picker") ||
            desc.contains("comment") || desc.contains("komentar") ||
            clsName.contains("comment") || clsName.contains("composer")) {
            isComment = true
            return
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                checkChild(v.getChildAt(i))
            }
        }
    }
    checkChild(view)
    return isComment
}

fun findMostVisibleMediaView(root: ViewGroup, context: Context): View? {
    val displayMetrics = context.resources.displayMetrics
    val screenCenterY = displayMetrics.heightPixels / 2f

    var bestVideoView: View? = null
    var minVideoDist = Float.MAX_VALUE
    var bestImageView: View? = null
    var minImageDist = Float.MAX_VALUE

    fun traverse(view: View) {
        val name = view.javaClass.name.lowercase()
        val isVideo = name.contains("textureview") || name.contains("videoplayerview") || name.contains("surfaceview")
        val isImage = name.contains("imageview") || name.contains("mediaview")

        if (isVideo || isImage) {
            val rect = Rect()
            if (view.getGlobalVisibleRect(rect)) {
                val viewCenterY = (rect.top + rect.bottom) / 2f
                val dist = Math.abs(viewCenterY - screenCenterY)
                val width = rect.width()
                val height = rect.height()

                if (width > 200 && height > 200) {
                    if (isVideo && dist < minVideoDist) {
                        minVideoDist = dist
                        bestVideoView = view
                    } else if (isImage && dist < minImageDist && !name.contains("avatar") && !name.contains("icon")) {
                        minImageDist = dist
                        bestImageView = view
                    }
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverse(view.getChildAt(i))
            }
        }
    }
    traverse(root)

    val bestView = if (bestVideoView != null && minVideoDist < 500f) bestVideoView else bestImageView ?: bestVideoView

    var current = bestView
    var itemView = bestView
    while (current?.parent is View) {
        val p = current.parent as View
        if (p is androidx.recyclerview.widget.RecyclerView || p is android.widget.ListView) {
            itemView = current
            break
        }
        current = p
    }

    if (itemView != bestView && itemView != null) {
        if (bestView == bestVideoView) {
            videoViewCache[itemView] = true
        }
        return itemView
    }

    var parent = bestView?.parent as? View
    for (i in 0 until 3) {
        if (parent?.parent is View) {
            parent = parent.parent as View
        }
    }
    if (bestView == bestVideoView && parent != null) {
        videoViewCache[parent] = true
    }
    return parent ?: bestView
}

object DeepMediaExtractor {
    fun extractAllUrls(obj: Any?, urls: MutableSet<String>, depth: Int, visited: MutableSet<Int>) {
        if (obj == null || depth > 10) return
        val hash = System.identityHashCode(obj)
        if (!visited.add(hash)) return

        if (obj is String) {
            if (isInstagramCdnUrl(obj)) {
                urls.add(obj.replace("&amp;", "&"))
            }
            return
        }

        try {
            val cls = obj.javaClass
            if (cls.name.startsWith("android.") || cls.name.startsWith("java.") || cls.name.startsWith("androidx.")) {
                if (obj is Collection<*>) {
                    for (item in obj) extractAllUrls(item, urls, depth + 1, visited)
                }
                return
            }

            if (obj is View) {
                val tag = obj.tag
                if (tag != null) extractAllUrls(tag, urls, depth + 1, visited)
            }

            var currentCls: Class<*>? = cls
            while (currentCls != null && currentCls != Any::class.java) {
                for (field in currentCls.declaredFields) {
                    if (field.type.isPrimitive) continue
                    field.isAccessible = true
                    val value = field.get(obj) ?: continue
                    extractAllUrls(value, urls, depth + 1, visited)
                }
                currentCls = currentCls.superclass
            }
        } catch (_: Exception) {}
    }
}

object DeepTextExtractor {
    fun extractAllTexts(obj: Any?, texts: MutableSet<String>, depth: Int, visited: MutableSet<Int>) {
        if (obj == null || depth > 8) return
        val hash = System.identityHashCode(obj)
        if (!visited.add(hash)) return

        if (obj is String) {
            if (obj.length in 5..5000 && !obj.startsWith("http") && !obj.contains("facebook") && !obj.contains("instagram.com")) {
                texts.add(obj)
            }
            return
        }

        try {
            val cls = obj.javaClass
            if (cls.name.startsWith("android.") || cls.name.startsWith("java.") || cls.name.startsWith("androidx.")) {
                if (obj is Collection<*>) {
                    for (item in obj) extractAllTexts(item, texts, depth + 1, visited)
                }
                return
            }

            var currentCls: Class<*>? = cls
            while (currentCls != null && currentCls != Any::class.java) {
                for (field in currentCls.declaredFields) {
                    if (field.type.isPrimitive) continue
                    field.isAccessible = true
                    val value = field.get(obj) ?: continue
                    extractAllTexts(value, texts, depth + 1, visited)
                }
                currentCls = currentCls.superclass
            }
        } catch (_: Exception) {}
    }
}

fun isInstagramCdnUrl(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
        return false
    }
    return (lower.contains("fbcdn.net") || lower.contains("cdninstagram.com")) &&
           (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".mp4") || lower.contains("video") || lower.contains("scontent") || lower.contains("audio") || lower.contains(".m4a"))
}

fun isUnifiedMp4(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".mp4") && !lower.contains("video")) return false
    if (lower.contains("mime=audio") || lower.contains("/audio/") || lower.contains("audio_aac")) return false

    try {
        val uri = Uri.parse(url)
        val efg = uri.getQueryParameter("efg")
        if (efg != null) {
            val decoded = String(Base64.decode(efg, Base64.DEFAULT)).lowercase()
            if (decoded.contains("mpx_audio") || decoded.contains("dash_ln_heaac") || decoded.contains("progressive_audio")) return false
        }
        val ncVs = uri.getQueryParameter("_nc_vs")
        if (ncVs != null) {
            val decoded = String(Base64.decode(ncVs, Base64.DEFAULT)).lowercase()
            if (decoded.contains("mpx_audio") || decoded.contains("dash_ln_heaac") || decoded.contains("progressive_audio")) return false
        }
    } catch (_: Exception) {}

    return true
}

fun isAudio(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    if (!lower.contains("fbcdn.net") && !lower.contains("cdninstagram.com")) return false
    return lower.contains("mime=audio") || lower.contains("/audio/") || lower.contains("audio_aac") || lower.contains(".m4a")
}

fun deduplicateResolutions(urls: List<String>): List<String> {
    val result = mutableMapOf<String, String>()
    for (url in urls) {
        val beforeQuery = url.substringBefore("?")
        val name = beforeQuery.substringAfterLast("/")
        val existing = result[name]
        if (existing == null || url.length > existing.length) {
            result[name] = url
        }
    }
    return result.values.toList()
}

fun isHighResImage(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".jpg") && !lower.contains(".jpeg") && !lower.contains(".png")) return false
    if (lower.contains("profile_pic") || lower.contains("/s150x150/") || lower.contains("/s320x320/")) return false
    if (lower.contains("43985629_311105916145351_58064759811405776") || lower.contains("44884218_345707102882519_2446069589734326272")) return false
    if (lower.contains("t51.12442-15") || lower.contains("t51.2885-19") || lower.contains("t51.71878-15")) return false

    try {
        val uri = Uri.parse(url)
        val efg = uri.getQueryParameter("efg")
        if (efg != null) {
            val decoded = String(Base64.decode(efg, Base64.DEFAULT)).lowercase()
            if (decoded.contains("cover_frame") || decoded.contains("scrubber")) return false
        }
        val ncVs = uri.getQueryParameter("_nc_vs")
        if (ncVs != null) {
            val decoded = String(Base64.decode(ncVs, Base64.DEFAULT)).lowercase()
            if (decoded.contains("cover_frame") || decoded.contains("scrubber")) return false
        }
    } catch (_: Exception) {}

    return true
}

fun downloadInstagramMedia(context: Context, url: String, isVideo: Boolean, isAudioOnly: Boolean = false) {
    runCatching {
        val ext = if (isAudioOnly) "m4a" else if (isVideo) "mp4" else "jpg"
        val filename = "rhpatch_" + System.currentTimeMillis() + "." + ext
        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
        val downloadPath = prefs.getString("pref_download_path", "Rhpatch") ?: "Rhpatch"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("User-Agent", "Instagram 300.0.0.35.109 Android")
            addRequestHeader("Referer", "https://www.instagram.com/")
            setTitle("Rhpatch Downloader")
            setDescription(filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadPath + "/" + filename)
            setMimeType(if (isAudioOnly) "audio/mp4" else if (isVideo) "video/mp4" else "image/jpeg")
        }
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "[Rhpatch] Download dimulai...", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "[Rhpatch] Gagal mengunduh: " + it.message, Toast.LENGTH_SHORT).show()
    }
}
