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

    // ── Inject Download Button into Dialogs (Share Sheet) ─────────────────────
    runCatching {
        // Try hooking DialogFragment.show since Instagram uses IgBottomSheetFragment
        val dialogFragmentClass = XposedHelpers.findClassIfExists("androidx.fragment.app.DialogFragment", classLoader)
        if (dialogFragmentClass != null) {
            val showMethods = dialogFragmentClass.declaredMethods.filter { it.name == "show" }
            showMethods.forEach { method ->
                XposedBridge.hookMethod(
                    method,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val dialogFragment = param.thisObject
                            
                            // Try to get the underlying Dialog
                            val dialog = XposedHelpers.callMethod(dialogFragment, "getDialog") as? android.app.Dialog ?: return
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
        }
        XposedBridge.log("Rhpatch: [Download] DialogFragment.show hook installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] DialogFragment.show hook failed: $it") }
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
