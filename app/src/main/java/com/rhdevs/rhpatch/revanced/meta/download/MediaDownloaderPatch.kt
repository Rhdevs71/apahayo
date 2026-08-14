package com.rhdevs.rhpatch.revanced.meta.download

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

// Cache untuk menyimpan objek pembantu overflow terakhir yang dibuka
var currentOverflowHelper: Any? = null

val MediaDownloaderPatch = patch(
    name = "Instagram Media Downloader (Piko Native Style)",
    description = "Ekstraksi Media Object secara presisi (Tanpa ExoPlayer intercept)"
) {
    // 1. Hook MediaOptionsOverflowHelper untuk menangkap objek Media secara 100% akurat
    runCatching {
        val helperMethods = MetaUnobfuscator.findMethodUsingStrings("MediaOptionsOverflowHelper")
        val helperClass = helperMethods.firstOrNull()?.declaringClass
        if (helperClass != null) {
            XposedBridge.log("Rhpatch: Berhasil menemukan MediaOptionsOverflowHelper -> ${helperClass.name}")
            XposedBridge.hookAllConstructors(helperClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentOverflowHelper = param.thisObject
                    XposedBridge.log("Rhpatch: Berhasil menyadap instance MediaOptionsOverflowHelper!")
                }
            })
        } else {
            XposedBridge.log("Rhpatch: Gagal menemukan MediaOptionsOverflowHelper. Fallback ke scan UI.")
        }
    }

    // 2. Injeksi UI Tombol Unduh ke BottomSheet
    runCatching {
        val igdsBottomSheetClass = XposedHelpers.findClassIfExists("com.instagram.igds.components.bottomsheet.BottomSheetFragment", classLoader)
        if (igdsBottomSheetClass != null) {
            XposedBridge.hookAllMethods(igdsBottomSheetClass, "onViewCreated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.args[0] as? ViewGroup ?: return
                        val context = view.context
                        
                        if (view.findViewWithTag<View>("rhp_bs_download") != null) return
                        
                        val dp = context.resources.displayMetrics.density
                        
                        val btnLayout = android.widget.LinearLayout(context).apply {
                            tag = "rhp_bs_download"
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * dp).toInt())
                            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
                            gravity = Gravity.CENTER_VERTICAL
                            
                            val typedValue = android.util.TypedValue()
                            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                            setBackgroundResource(typedValue.resourceId)
                            
                            val icon = android.widget.ImageView(context).apply {
                                setImageResource(android.R.drawable.ic_menu_save)
                                val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                setColorFilter(if (isNightMode) Color.parseColor("#F5F5F5") else Color.parseColor("#262626"))
                                layoutParams = android.widget.LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()).apply {
                                    marginEnd = (16 * dp).toInt()
                                }
                            }
                            
                            val tv = android.widget.TextView(context).apply {
                                text = "Pilihan Unduhan (Rhpatch)"
                                textSize = 16f
                                val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                                setTextColor(if (isNightMode) Color.WHITE else Color.BLACK)
                            }
                            
                            addView(icon)
                            addView(tv)
                            
                            setOnClickListener {
                                val activity = context as? android.app.Activity ?: return@setOnClickListener
                                showPikoStyleMainMenu(context, activity)
                            }
                        }
                        
                        var added = false
                        for (i in 0 until view.childCount) {
                            val child = view.getChildAt(i)
                            if (child is android.widget.LinearLayout) {
                                child.addView(btnLayout, 0)
                                added = true
                                break
                            } else if (child is androidx.recyclerview.widget.RecyclerView) {
                                val parent = child.parent as? ViewGroup
                                if (parent is android.widget.LinearLayout) {
                                    parent.addView(btnLayout, parent.indexOfChild(child))
                                    added = true
                                    break
                                }
                            }
                        }
                        
                        if (!added) {
                            view.addView(btnLayout, 0)
                        }
                    } catch (e: Exception) {}
                }
            })
        }
    }
}

fun showPikoStyleMainMenu(context: Context, activity: android.app.Activity) {
    val options = arrayOf("Pilihan unduhan", "Pilihan lainnya")
    AlertDialog.Builder(context)
        .setTitle("Rhpatch")
        .setItems(options) { _, which ->
            when (which) {
                0 -> showPikoStyleDownloadMenu(context, activity)
                1 -> showPikoStyleMoreMenu(context, activity)
            }
        }
        .show()
}

fun showPikoStyleDownloadMenu(context: Context, activity: android.app.Activity) {
    Toast.makeText(context, "Rhpatch: Memproses ekstrak media...", Toast.LENGTH_SHORT).show()
                                
    Thread {
        val extractedUrls = mutableSetOf<String>()
        val startObj = currentOverflowHelper ?: findMostVisibleMediaView(activity.window.decorView.rootView as ViewGroup, context)
        
        if (startObj != null) {
            DeepMediaExtractor.extractAllUrls(startObj, extractedUrls, 0, mutableSetOf())
        }
        
        val unifiedMp4Urls = extractedUrls.filter { isUnifiedMp4(it) }
        val imageUrls = extractedUrls.filter { isHighResImage(it) }
        val audioUrls = extractedUrls.filter { isAudio(it) }
        
        activity.runOnUiThread {
            val bestVideo = unifiedMp4Urls.maxByOrNull { it.length } ?: unifiedMp4Urls.firstOrNull()
            val bestImage = imageUrls.maxByOrNull { it.length } ?: imageUrls.firstOrNull()
            val bestAudio = audioUrls.maxByOrNull { it.length } ?: audioUrls.firstOrNull()
            
            val options = mutableListOf<String>()
            val actions = mutableListOf<Runnable>()
            
            if (bestVideo != null || bestImage != null) {
                options.add("Unduh media saat ini")
                actions.add(Runnable {
                    if (bestVideo != null) downloadInstagramMedia(context, bestVideo, true)
                    else if (bestImage != null) downloadInstagramMedia(context, bestImage, false)
                })
            }
            
            if (bestImage != null) {
                options.add("Unduh sebagai gambar")
                actions.add(Runnable { downloadInstagramMedia(context, bestImage, false) })
            }
            
            if (bestAudio != null) {
                options.add("Unduh audio")
                actions.add(Runnable { downloadInstagramMedia(context, bestAudio, true, isAudioOnly = true) })
            } else if (bestVideo != null) {
                // Jika tidak ada audio stream terpisah, izinkan ekstrak dari mp4 (secara label UI saja)
                options.add("Unduh audio (dari Video)")
                actions.add(Runnable { downloadInstagramMedia(context, bestVideo, true, isAudioOnly = true) })
            }
            
            if (bestVideo != null || bestImage != null) {
                options.add("Salin tautan media")
                actions.add(Runnable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("URL", bestVideo ?: bestImage)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Tautan media disalin!", Toast.LENGTH_SHORT).show()
                })
            }
            
            if (imageUrls.size > 1) {
                options.add("Varian gambar")
                actions.add(Runnable {
                    val variants = imageUrls.toList()
                    AlertDialog.Builder(context)
                        .setTitle("Varian gambar")
                        .setItems(variants.map { "Gambar [Resolusi/Varian]" }.toTypedArray()) { _, vWhich ->
                            downloadInstagramMedia(context, variants[vWhich], false)
                        }.show()
                })
            }
            
            if (bestImage != null) {
                options.add("Buka gambar secara eksternal")
                actions.add(Runnable {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(bestImage))
                    context.startActivity(intent)
                })
            }

            if (options.isEmpty()) {
                Toast.makeText(context, "Rhpatch: Media tidak ditemukan pada post ini.", Toast.LENGTH_SHORT).show()
                return@runOnUiThread
            }

            AlertDialog.Builder(context)
                .setTitle("Pilihan unduhan")
                .setItems(options.toTypedArray()) { _, which ->
                    actions[which].run()
                }
                .show()
        }
    }.start()
}

fun showPikoStyleMoreMenu(context: Context, activity: android.app.Activity) {
    Toast.makeText(context, "Mengekstrak data teks...", Toast.LENGTH_SHORT).show()
    Thread {
        val extractedTexts = mutableSetOf<String>()
        val startObj = currentOverflowHelper ?: findMostVisibleMediaView(activity.window.decorView.rootView as ViewGroup, context)
        
        if (startObj != null) {
            DeepTextExtractor.extractAllTexts(startObj, extractedTexts, 0, mutableSetOf())
        }
        
        // Coba cari caption, username (pendek, tanpa spasi), dan full name
        val possibleUsernames = extractedTexts.filter { !it.contains(" ") && it.length in 3..30 && it.matches(Regex("^[a-zA-Z0-9_.]+$")) }.toList()
        val possibleCaptions = extractedTexts.filter { it.length > 30 && it.contains(" ") }.sortedByDescending { it.length }.toList()
        
        activity.runOnUiThread {
            val options = mutableListOf<String>()
            val actions = mutableListOf<Runnable>()
            
            options.add("Salin deskripsi postingan")
            actions.add(Runnable {
                val caption = possibleCaptions.firstOrNull() ?: "Tidak ada deskripsi"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Caption", caption)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Deskripsi disalin!", Toast.LENGTH_SHORT).show()
            })
            
            options.add("Salin nama pengguna pemilik post")
            actions.add(Runnable {
                val username = possibleUsernames.firstOrNull { it != "instagram" } ?: "Tidak diketahui"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Username", username)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Nama pengguna disalin!", Toast.LENGTH_SHORT).show()
            })
            
            options.add("Salin ID Media (Debug)")
            actions.add(Runnable {
                val mediaId = extractedTexts.firstOrNull { it.contains("_") && it.length > 15 && it.matches(Regex("^[0-9]+_[0-9]+$")) } ?: "Tidak ditemukan"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("MediaID", mediaId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Media ID disalin!", Toast.LENGTH_SHORT).show()
            })

            AlertDialog.Builder(context)
                .setTitle("Pilihan lainnya")
                .setItems(options.toTypedArray()) { _, which ->
                    actions[which].run()
                }
                .show()
        }
    }.start()
}

fun findMostVisibleMediaView(root: ViewGroup, context: Context): View? {
    var bestView: View? = null
    var minDistanceToCenter = Float.MAX_VALUE
    
    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels
    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f
    
    fun traverse(view: View) {
        if (view.visibility != View.VISIBLE) return
        
        val viewName = view.javaClass.name
        if (viewName.contains("IgProgressImageView") || viewName.contains("TextureView") || viewName.contains("SurfaceView") || viewName.contains("MediaFrameLayout")) {
            val rect = Rect()
            if (view.getGlobalVisibleRect(rect)) {
                if (rect.width() > 200 && rect.height() > 200) {
                    val viewCenterX = rect.exactCenterX()
                    val viewCenterY = rect.exactCenterY()
                    val distance = Math.hypot((viewCenterX - centerX).toDouble(), (viewCenterY - centerY).toDouble()).toFloat()
                    
                    if (distance < minDistanceToCenter) {
                        minDistanceToCenter = distance
                        bestView = view
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
    
    var parent = bestView?.parent as? View
    for (i in 0 until 3) {
        if (parent?.parent is View) {
            parent = parent.parent as View
        }
    }
    return parent ?: bestView
}

object DeepMediaExtractor {
    fun extractAllUrls(obj: Any?, urls: MutableSet<String>, depth: Int, visited: MutableSet<Int>) {
        if (obj == null || depth > 8) return
        val hash = System.identityHashCode(obj)
        if (!visited.add(hash)) return

        if (obj is String) {
            if (isInstagramCdnUrl(obj)) urls.add(obj)
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
                if (obj is ViewGroup) {
                    for (i in 0 until obj.childCount) {
                        extractAllUrls(obj.getChildAt(i), urls, depth + 1, visited)
                    }
                }
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
        } catch (e: Exception) {}
    }
}

object DeepTextExtractor {
    fun extractAllTexts(obj: Any?, texts: MutableSet<String>, depth: Int, visited: MutableSet<Int>) {
        if (obj == null || depth > 8) return
        val hash = System.identityHashCode(obj)
        if (!visited.add(hash)) return

        if (obj is String) {
            if (obj.isNotBlank() && !isInstagramCdnUrl(obj) && !obj.contains("{") && !obj.contains("https://")) {
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
        } catch (e: Exception) {}
    }
}

fun isInstagramCdnUrl(url: String): Boolean {
    val lower = url.lowercase()
    return (lower.contains("fbcdn.net") || lower.contains("cdninstagram.com")) &&
           (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".mp4") || lower.contains("video") || lower.contains("scontent") || lower.contains("audio"))
}

fun isUnifiedMp4(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".mp4") && !lower.contains("video")) return false
    if (lower.contains("dash") || lower.contains("audio") || lower.contains("init") || lower.contains("m4s") || lower.contains("bytestart")) return false
    return true
}

fun isAudio(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("audio") || lower.contains("mime=audio") || lower.contains("m4a")
}

fun isHighResImage(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".jpg") && !lower.contains(".jpeg")) return false
    if (lower.contains("profile_pic")) return false
    return true
}

fun downloadInstagramMedia(context: Context, url: String, isVideo: Boolean, isAudioOnly: Boolean = false) {
    runCatching {
        val ext = if (isAudioOnly) "m4a" else if (isVideo) "mp4" else "jpg"
        val filename = "rhpatch_${System.currentTimeMillis()}.$ext"
        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
        val downloadPath = prefs.getString("pref_download_path", "Rhpatch") ?: "Rhpatch"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("User-Agent", "Instagram 300.0.0.35.109 Android")
            addRequestHeader("Referer", "https://www.instagram.com/")
            setTitle("Rhpatch Downloader")
            setDescription(filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$downloadPath/$filename")
            setMimeType(if (isAudioOnly) "audio/mp4" else if (isVideo) "video/mp4" else "image/jpeg")
        }
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "⬇ Rhpatch: Download dimulai", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Rhpatch Error: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}
