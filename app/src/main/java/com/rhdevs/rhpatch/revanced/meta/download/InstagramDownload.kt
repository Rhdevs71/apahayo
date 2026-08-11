package com.rhdevs.rhpatch.revanced.meta.download

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/** Last known Instagram CDN media URL captured from network/player/UI hooks. */
var lastKnownMediaUrl: String? = null
var lastKnownIsVideo: Boolean = false
var shareClickedTime: Long = 0

val playerUrls = java.util.WeakHashMap<Any, String>()
val imageUrls = java.util.WeakHashMap<View, String>()

val InstagramDownload = patch(
    name = "Instagram Media Downloader",
    description = "Add download button to Instagram share sheet"
) {

    // ── Capture video URL via ExoPlayer hook ───────────────────────────────────
    runCatching {
        for (playerClassName in listOf(
            "com.google.android.exoplayer2.ExoPlayerImpl",
            "com.google.android.exoplayer2.SimpleExoPlayer"
        )) {
            runCatching {
                val cls = XposedHelpers.findClassIfExists(playerClassName, classLoader) ?: continue
                cls.declaredMethods.filter { m ->
                    m.name.contains("setMedia", ignoreCase = true) && m.parameterCount >= 1
                }.take(4).forEach { method ->
                    runCatching {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val url = extractUrlFromArgs(param.args)
                                if (url != null) {
                                    playerUrls[param.thisObject] = url
                                }
                            }
                        })
                    }
                }
                
                // Track when video actually starts playing
                cls.declaredMethods.filter { m ->
                    m.name == "setPlayWhenReady" && m.parameterCount == 1
                }.forEach { method ->
                    runCatching {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val play = param.args[0] as? Boolean ?: return
                                if (play) {
                                    playerUrls[param.thisObject]?.let { url ->
                                        lastKnownMediaUrl = url
                                        lastKnownIsVideo = url.contains(".mp4") || url.contains("video")
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
        XposedBridge.log("Rhpatch: [Download] ExoPlayer tracking hooks installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] ExoPlayer tracking failed: $it") }

    // ── Capture image URL via IgImageView ─────────────────────────────────────
    runCatching {
        val igImageViewClass = XposedHelpers.findClassIfExists(
            "com.instagram.common.ui.widget.imageview.IgImageView", classLoader
        ) ?: throw ClassNotFoundException("IgImageView not found")

        igImageViewClass.declaredMethods.filter { it.name == "setUrl" || it.name == "setUrlWithFallback" }.forEach { method ->
            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val imageUrl = param.args.firstOrNull { it != null && it::class.java.name.contains("ImageUrl") } ?: return
                        val url = XposedHelpers.callMethod(imageUrl, "getUrl") as? String ?: return
                        if (isInstagramCdnUrl(url) && !url.contains("profile_pic")) {
                            imageUrls[param.thisObject as View] = url
                        }
                    }
                })
            }
        }
        
        // Track when image is actually drawn
        XposedHelpers.findAndHookMethod(igImageViewClass, "onDraw", android.graphics.Canvas::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                imageUrls[param.thisObject as View]?.let { url ->
                    lastKnownMediaUrl = url
                    lastKnownIsVideo = false
                }
            }
        })
        XposedBridge.log("Rhpatch: [Download] IgImageView tracking hooks installed successfully")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] IgImageView tracking hook failed: $it") }

    // ── Enable DM Media Saver (Piko) ──────────────────────────────────────────
    runCatching {
        if (com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) {
            val saverMethods = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("DirectThreadMediaSaver")
            if (saverMethods.isNotEmpty()) {
                val cls = saverMethods.first().declaringClass
                cls.declaredMethods.filter { it.returnType == Void.TYPE && it.name != "<init>" }.forEach { method ->
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // Piko bypasses the messageDownloadCheck. We will just hook this to allow saving.
                            // If it checks permission, we can force it. But the easiest way without complex logic
                            // is just logging or finding the boolean methods in this class and forcing true.
                        }
                    })
                }
                
                // Hook boolean checks in the saver class to return true
                cls.declaredMethods.filter { it.returnType == Boolean::class.javaPrimitiveType }.forEach { method ->
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })
                }
                XposedBridge.log("Rhpatch: [Download] DM Media Saver hooks installed")
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [Download] DM Media Saver hook failed: $it") }

    // ── Inject Download Button into Dialogs (Share Sheet) ─────────────────────
    runCatching {
        val dialogClass = android.app.Dialog::class.java
        val showMethods = dialogClass.declaredMethods.filter { it.name == "show" }
        showMethods.forEach { method ->
            XposedBridge.hookMethod(
                method,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as? android.app.Dialog ?: return
                        val context = dialog.context
                        if (!context.packageName.contains("instagram", ignoreCase = true)) return

                        val window = dialog.window ?: return
                        val decorView = window.decorView as? ViewGroup ?: return
                        
                        // Avoid double injection
                        if (decorView.findViewWithTag<View>("rhp_dl_btn") != null) return

                        // Check if lastKnownMediaUrl is populated, if not, try to scan the Activity
                        if (lastKnownMediaUrl.isNullOrEmpty()) {
                            val activity = dialog.ownerActivity ?: (context as? android.app.Activity)
                            if (activity != null) {
                                val activityDecor = activity.window?.decorView as? ViewGroup
                                if (activityDecor != null) {
                                    val url = searchUrlInTree(activityDecor)
                                    if (url != null) {
                                        lastKnownMediaUrl = url
                                        lastKnownIsVideo = url.contains(".mp4") || url.contains("video")
                                        XposedBridge.log("Rhpatch: [Download] Fallback scan found URL: $url")
                                    }
                                }
                            }
                        }

                        if (lastKnownMediaUrl.isNullOrEmpty()) return

                        // Add delay to ensure views are laid out
                        Handler(Looper.getMainLooper()).postDelayed({
                            runCatching {
                                injectFloatingDownloadButton(decorView, context)
                            }
                        }, 200)
                    }
                }
            )
        }
        XposedBridge.log("Rhpatch: [Download] Dialog.show hook installed")

        // Also hook BottomSheetFragment which is common in modern Instagram
        val bottomSheetClass = XposedHelpers.findClassIfExists("com.instagram.igds.components.bottomsheet.BottomSheetFragment", classLoader)
        if (bottomSheetClass != null) {
            val onViewCreated = bottomSheetClass.declaredMethods.find { it.name == "onViewCreated" }
            if (onViewCreated != null) {
                XposedBridge.hookMethod(onViewCreated, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.args[0] as? View ?: return
                        val context = view.context
                        if (lastKnownMediaUrl.isNullOrEmpty()) return

                        Handler(Looper.getMainLooper()).postDelayed({
                            runCatching {
                                if (view is ViewGroup) {
                                    // Piko Parity: Instead of a floating button, inject a row that mimics Instagram's menu
                                    injectPikoStyleDownloadRow(view, context)
                                }
                            }
                        }, 200)
                    }
                })
                XposedBridge.log("Rhpatch: [Download] BottomSheetFragment hook installed")
            }
        }

    }.onFailure { XposedBridge.log("Rhpatch: [Download] Dialog/BottomSheet hook failed: $it") }
}

fun searchUrlInTree(view: View): String? {
    if (imageUrls.containsKey(view)) {
        return imageUrls[view]
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i) ?: continue
            val found = searchUrlInTree(child)
            if (found != null) return found
        }
    }
    return null
}

fun extractUrlFromArgs(args: Array<Any?>): String? {
    args.forEach { arg ->
        if (arg == null) return@forEach
        val url = runCatching {
            val u: String? = when {
                arg is Uri -> arg.toString()
                arg is String -> arg
                else -> {
                    val localCfg = runCatching {
                        XposedHelpers.getObjectField(arg, "localConfiguration")
                    }.getOrNull()
                    val uri = runCatching {
                        XposedHelpers.getObjectField(localCfg ?: return@runCatching null, "uri")
                    }.getOrNull()
                    uri?.toString()
                }
            }
            if (!u.isNullOrEmpty() && isInstagramCdnUrl(u)) {
                return@runCatching u
            }
            null
        }.getOrNull()
        
        if (url != null) return url
    }
    return null
}

fun isInstagramCdnUrl(url: String): Boolean =
    (url.contains("fbcdn.net") || url.contains("cdninstagram.com")) &&
    (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".mp4") ||
        url.contains("video") || url.contains("scontent"))

fun injectPikoStyleDownloadRow(decorView: ViewGroup, context: Context) {
    if (decorView.findViewWithTag<View>("rhp_dl_row") != null) return

    val dp = context.resources.displayMetrics.density

    // Find the RecyclerView inside the BottomSheet
    var recyclerView: androidx.recyclerview.widget.RecyclerView? = null
    fun findRV(v: View) {
        if (v is androidx.recyclerview.widget.RecyclerView) {
            recyclerView = v
            return
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                findRV(v.getChildAt(i))
                if (recyclerView != null) return
            }
        }
    }
    findRV(decorView)

    val targetContainer = (recyclerView?.parent as? ViewGroup) ?: decorView

    // Create a row that mimics IgdsListCell
    val row = LinearLayout(context).apply {
        tag = "rhp_dl_row"
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        
        // Ripple effect
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }

    val icon = ImageView(context).apply {
        setImageResource(android.R.drawable.stat_sys_download)
        setColorFilter(Color.parseColor("#F5F5F5")) // Instagram Dark mode icon color
        layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()).apply {
            marginEnd = (16 * dp).toInt()
        }
    }

    val text = TextView(context).apply {
        text = "Download Media"
        setTextColor(Color.parseColor("#F5F5F5"))
        textSize = 16f
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    row.addView(icon)
    row.addView(text)

    row.setOnClickListener {
        val url = lastKnownMediaUrl
        val isVideo = lastKnownIsVideo
        if (!url.isNullOrEmpty()) {
            downloadInstagramMedia(context, url, isVideo)
        } else {
            Toast.makeText(context, "URL tidak ditemukan. Putar ulang media.", Toast.LENGTH_LONG).show()
        }
    }

    runCatching {
        if (targetContainer is LinearLayout) {
            targetContainer.addView(row, 0) // Add to top
        } else {
            targetContainer.addView(row)
        }
        XposedBridge.log("Rhpatch: [Download] Injected Piko-style download row")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Download] Failed to add row: $it")
    }
}

fun injectFloatingDownloadButton(decorView: ViewGroup, context: Context) {
    if (decorView.findViewWithTag<View>("rhp_dl_btn") != null) return

    val dp = context.resources.displayMetrics.density

    // Create a floating action button style container
    val btn = FrameLayout(context).apply {
        tag = "rhp_dl_btn"
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E1306C")) // Instagram Pink-ish
            setStroke((1.5f * dp).toInt(), Color.WHITE)
        }
        isClickable = true
        isFocusable = true
        elevation = 8 * dp
    }

    val iconSize = (56 * dp).toInt()
    val icon = ImageView(context).apply {
        setImageResource(android.R.drawable.stat_sys_download)
        setColorFilter(Color.WHITE)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
    }
    btn.addView(icon, FrameLayout.LayoutParams(iconSize, iconSize))

    btn.setOnClickListener {
        val url = lastKnownMediaUrl
        val isVideo = lastKnownIsVideo
        if (!url.isNullOrEmpty()) {
            downloadInstagramMedia(context, url, isVideo)
        } else {
            Toast.makeText(context, "URL tidak ditemukan. Ulangi setelah memutar media.", Toast.LENGTH_LONG).show()
        }
    }

    // Add to top-right of the dialog
    val params = FrameLayout.LayoutParams(iconSize, iconSize).apply {
        gravity = Gravity.TOP or Gravity.END
        topMargin = (24 * dp).toInt()
        marginEnd = (24 * dp).toInt()
    }

    runCatching {
        decorView.addView(btn, params)
        XposedBridge.log("Rhpatch: [Download] Injected floating download button")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Download] Failed to add floating button: $it")
    }
}

fun downloadInstagramMedia(context: Context, url: String, isVideo: Boolean) {
    runCatching {
        val ext = if (isVideo) "mp4" else "jpg"
        val filename = "ig_${System.currentTimeMillis()}.$ext"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("User-Agent", "Instagram 300.0.0.35.109 Android")
            addRequestHeader("Referer", "https://www.instagram.com/")
            setTitle("Mengunduh dari Instagram")
            setDescription(filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Rhpatch/$filename")
            setMimeType(if (isVideo) "video/mp4" else "image/jpeg")
        }
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "⬇ Download dimulai: $filename", Toast.LENGTH_SHORT).show()
    }.onFailure {
        XposedBridge.log(it)
        Toast.makeText(context, "Gagal download: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}
