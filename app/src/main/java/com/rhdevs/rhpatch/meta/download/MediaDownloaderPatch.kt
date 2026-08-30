package com.rhdevs.rhpatch.meta.download

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
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

// Cache untuk menyimpan objek pembantu overflow terakhir yang dibuka
var currentOverflowHelper: Any? = null
var currentStoryHelper: Any? = null
val videoViewCache = java.util.WeakHashMap<View, Boolean>()

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

    // 1.5 Hook Story Options Helper (menggunakan string unik dari Piko)
    runCatching {
        val storyMethods = MetaUnobfuscator.findMethodUsingStrings("friendships/mute_friend_reel/%s/")
        val storyClass = storyMethods.firstOrNull()?.declaringClass
        if (storyClass != null) {
            XposedBridge.log("Rhpatch: Berhasil menemukan StoryOptionsHelper -> ${storyClass.name}")
            XposedBridge.hookAllConstructors(storyClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentStoryHelper = param.thisObject
                    XposedBridge.log("Rhpatch: Berhasil menyadap instance StoryOptionsHelper!")
                }
            })
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
            
            XposedBridge.hookAllMethods(igdsBottomSheetClass, "onDestroyView", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentOverflowHelper = null
                    currentStoryHelper = null
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
        val startObj = currentOverflowHelper ?: currentStoryHelper ?: findMostVisibleMediaView(activity.window.decorView.rootView as ViewGroup, context)
        if (startObj != null) {
            DeepMediaExtractor.extractAllUrls(startObj, extractedUrls, 0, mutableSetOf())
        }
        
        val visibleExtractedUrls = mutableSetOf<String>()
        val visibleView = findMostVisibleMediaView(activity.window.decorView.rootView as ViewGroup, context)
        if (visibleView != null) {
            DeepMediaExtractor.extractAllUrls(visibleView, visibleExtractedUrls, 0, mutableSetOf())
        }
        
        // 1. Ekstrak dari seluruh objek (berguna untuk fitur 'Unduh Semua')
        val allUnifiedMp4Urls = deduplicateResolutions(extractedUrls.filter { isUnifiedMp4(it) })
        val allImageUrls = deduplicateResolutions(extractedUrls.filter { isHighResImage(it) })
        val allAudioUrls = deduplicateResolutions(extractedUrls.filter { isAudio(it) })
        
        // 2. Ekstrak HANYA dari view yang sedang tampil (menjamin presisi Carousel/Story)
        val visMp4 = deduplicateResolutions(visibleExtractedUrls.filter { isUnifiedMp4(it) })
        val visImg = deduplicateResolutions(visibleExtractedUrls.filter { isHighResImage(it) })
        val visAud = deduplicateResolutions(visibleExtractedUrls.filter { isAudio(it) })
        
        XposedBridge.log("=== RHPATCH MEDIA DEBUG START ===")
        XposedBridge.log("All Images (${allImageUrls.size}):")
        allImageUrls.forEach { XposedBridge.log("IMG: $it") }
        XposedBridge.log("All MP4s (${allUnifiedMp4Urls.size}):")
        allUnifiedMp4Urls.forEach { XposedBridge.log("MP4: $it") }
        XposedBridge.log("Vis Images (${visImg.size}):")
        visImg.forEach { XposedBridge.log("V_IMG: $it") }
        XposedBridge.log("Vis MP4s (${visMp4.size}):")
        visMp4.forEach { XposedBridge.log("V_MP4: $it") }
        XposedBridge.log("=== RHPATCH MEDIA DEBUG END ===")
        
        activity.runOnUiThread {
            // Prioritaskan media yang sedang tampil di layar, jika gagal baru fallback ke seluruh objek
            val bestVideo = visMp4.maxByOrNull { it.length } ?: visMp4.firstOrNull() ?: allUnifiedMp4Urls.maxByOrNull { it.length } ?: allUnifiedMp4Urls.firstOrNull()
            val bestImage = visImg.maxByOrNull { it.length } ?: visImg.firstOrNull() ?: allImageUrls.maxByOrNull { it.length } ?: allImageUrls.firstOrNull()
            val bestAudio = visAud.maxByOrNull { it.length } ?: visAud.firstOrNull() ?: allAudioUrls.maxByOrNull { it.length } ?: allAudioUrls.firstOrNull()
            
            val options = mutableListOf<String>()
            val actions = mutableListOf<Runnable>()
            
            // Cek apakah view yang aktif di layar adalah Video atau Gambar berdasarkan tag
            val isVisibleVideo = visibleView != null && videoViewCache[visibleView] == true
            
            if (bestVideo != null || bestImage != null) {
                options.add("Unduh media saat ini")
                actions.add(Runnable {
                    if (isVisibleVideo && bestVideo != null) {
                        downloadInstagramMedia(context, bestVideo, true)
                    } else if (bestImage != null) {
                        downloadInstagramMedia(context, bestImage, false)
                    } else if (bestVideo != null) {
                        downloadInstagramMedia(context, bestVideo, true)
                    }
                })
            }
            
            // Cek apakah ini Story
            var isStory = currentStoryHelper != null
            var v = visibleView
            while (v != null) {
                val name = v.javaClass.name.lowercase()
                if (name.contains("story") || name.contains("reelviewer") || name.contains("reel")) {
                    isStory = true
                    break
                }
                v = v.parent as? View
            }
            
            try {
                val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity
                val fragments = fragmentActivity?.supportFragmentManager?.fragments
                fragments?.forEach { f ->
                    val fName = f.javaClass.name.lowercase()
                    if (fName.contains("story") || fName.contains("reel")) {
                        isStory = true
                    }
                }
            } catch (e: Exception) {}
            
            // Opsi tambahan untuk mengunduh seluruh isi Carousel
            // Sembunyikan opsi ini jika ini adalah Story (Story hanya 1 per 1)
            if (!isStory && (allImageUrls.size > 1 || allUnifiedMp4Urls.size > 1)) {
                val totalMedia = allImageUrls.size + allUnifiedMp4Urls.size
                if (totalMedia > 1) {
                    options.add("Unduh semua media ($totalMedia item)")
                    actions.add(Runnable {
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                        var delayMs = 0L
                        for (url in allImageUrls) {
                            handler.postDelayed({ downloadInstagramMedia(context, url, false) }, delayMs)
                            delayMs += 500
                        }
                        for (url in allUnifiedMp4Urls) {
                            handler.postDelayed({ downloadInstagramMedia(context, url, true) }, delayMs)
                            delayMs += 500
                        }
                        Toast.makeText(context, "Rhpatch: Memulai pengunduhan $totalMedia item secara berurutan...", Toast.LENGTH_LONG).show()
                    })
                }
            }
            
            if (bestImage != null) {
                options.add("Unduh sebagai gambar")
                actions.add(Runnable { downloadInstagramMedia(context, bestImage, false) })
            }
            
            if (bestAudio != null) {
                options.add("Unduh audio")
                actions.add(Runnable { downloadInstagramMedia(context, bestAudio, true, isAudioOnly = true) })
            } else if (bestVideo != null) {
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
            
            if (allImageUrls.size > 1) {
                options.add("Varian gambar")
                actions.add(Runnable {
                    val variants = allImageUrls.toList()
                    AlertDialog.Builder(context)
                        .setTitle("Varian gambar")
                        .setItems(variants.map { "Gambar [Resolusi/Varian]" }.toTypedArray()) { _, vWhich ->
                            downloadInstagramMedia(context, variants[vWhich], false)
                        }.show()
                })
            }
            
            if (bestImage != null) {
                options.add("Buka di browser")
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
        val startObj = currentOverflowHelper ?: currentStoryHelper ?: findMostVisibleMediaView(activity.window.decorView.rootView as ViewGroup, context)
        
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
    var bestVideoView: View? = null
    var bestImageView: View? = null
    var minVideoDist = Float.MAX_VALUE
    var minImageDist = Float.MAX_VALUE
    
    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels
    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f
    
    fun traverse(view: View) {
        if (view.visibility != View.VISIBLE) return
        
        val viewName = view.javaClass.name
        val isVideo = viewName.contains("TextureView") || viewName.contains("SurfaceView") || viewName.contains("MediaFrameLayout") || viewName.contains("IgVideoView") || viewName.contains("Video")
        val isImage = viewName.contains("IgProgressImageView") || viewName.contains("IgImageView")
        
        if (isVideo || isImage) {
            val rect = Rect()
            if (view.getGlobalVisibleRect(rect)) {
                if (rect.width() > 200 && rect.height() > 200) {
                    val distance = Math.hypot((rect.exactCenterX() - centerX).toDouble(), (rect.exactCenterY() - centerY).toDouble()).toFloat()
                    if (isVideo && distance < minVideoDist) {
                        minVideoDist = distance
                        bestVideoView = view
                    } else if (isImage && distance < minImageDist) {
                        minImageDist = distance
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
        val pName = p.javaClass.name.lowercase()
        if (pName.contains("recyclerview") || pName.contains("viewpager") || pName.contains("listview")) {
            itemView = current
            break
        }
        current = p
    }
    
    if (itemView != bestView && itemView != null) {
        // Tandai bahwa ini berasal dari video jika bestView adalah video
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
            if (isInstagramCdnUrl(obj)) urls.add(obj)
            return
        }

        try {
            val cls = obj.javaClass
            if (cls.name.startsWith("android.") || cls.name.startsWith("java.") || cls.name.startsWith("androidx.")) {
                if (obj is Collection<*>) {
                    for (item in obj) extractAllUrls(item, urls, depth + 1, visited)
                } else if (obj is Array<*>) {
                    for (item in obj) extractAllUrls(item, urls, depth + 1, visited)
                } else if (obj is Map<*, *>) {
                    for (value in obj.values) extractAllUrls(value, urls, depth + 1, visited)
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
                    val fieldName = field.name.lowercase()
                    // Abaikan field yang kemungkinan berisi foto profil atau avatar
                    if (fieldName.contains("profile") || fieldName.contains("avatar")) continue
                    
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
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
        return false
    }
    return (lower.contains("fbcdn.net") || lower.contains("cdninstagram.com")) &&
           (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".mp4") || lower.contains("video") || lower.contains("scontent") || lower.contains("audio"))
}

fun isUnifiedMp4(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".mp4") && !lower.contains("video")) return false
    
    // Blokir jika URL mentah secara eksplisit menyebut dash/audio track
    if (lower.contains("mime=audio") || lower.contains("/audio/") || lower.contains("audio_aac")) return false
    
    // Dekode parameter base64 efg dan _nc_vs untuk mendeteksi track audio tersembunyi
    try {
        val uri = android.net.Uri.parse(url)
        val efg = uri.getQueryParameter("efg")
        if (efg != null) {
            val decoded = String(android.util.Base64.decode(efg, android.util.Base64.DEFAULT)).lowercase()
            // Blokir track audio murni (bukan video ber-audio)
            if (decoded.contains("mpx_audio") || decoded.contains("dash_ln_heaac") || decoded.contains("progressive_audio")) return false
        }
        val ncVs = uri.getQueryParameter("_nc_vs")
        if (ncVs != null) {
            val decoded = String(android.util.Base64.decode(ncVs, android.util.Base64.DEFAULT)).lowercase()
            if (decoded.contains("mpx_audio") || decoded.contains("dash_ln_heaac") || decoded.contains("progressive_audio")) return false
        }
    } catch (e: Exception) {}
    
    return true
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

fun isAudio(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("audio") || lower.contains("mime=audio") || lower.contains("m4a")
}

fun isHighResImage(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains(".jpg") && !lower.contains(".jpeg") && !lower.contains(".png")) return false
    if (lower.contains("profile_pic") || lower.contains("/s150x150/") || lower.contains("/s320x320/")) return false
    // Filter ID avatar default abu-abu Instagram
    if (lower.contains("43985629_311105916145351_58064759811405776") || lower.contains("44884218_345707102882519_2446069589734326272")) return false
    // Filter path khusus foto profil dan sprite sheet scrubber
    if (lower.contains("t51.12442-15") || lower.contains("t51.2885-19") || lower.contains("t51.71878-15")) return false
    
    // Filter cover/thumbnail dari video (agar tidak muncul di 'unduh semua' feed post video)
    try {
        val uri = android.net.Uri.parse(url)
        val efg = uri.getQueryParameter("efg")
        if (efg != null) {
            val decoded = String(android.util.Base64.decode(efg, android.util.Base64.DEFAULT)).lowercase()
            if (decoded.contains("cover_frame") || decoded.contains("scrubber")) return false
        }
        val ncVs = uri.getQueryParameter("_nc_vs")
        if (ncVs != null) {
            val decoded = String(android.util.Base64.decode(ncVs, android.util.Base64.DEFAULT)).lowercase()
            if (decoded.contains("cover_frame") || decoded.contains("scrubber")) return false
        }
    } catch (e: Exception) {}
    
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
