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
                                extractUrlFromArgs(param.args)
                            }
                        })
                    }
                }
            }
        }
        XposedBridge.log("Rhpatch: [Download] ExoPlayer hooks installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] ExoPlayer failed: $it") }

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
                            lastKnownMediaUrl = url
                            lastKnownIsVideo = false
                        }
                    }
                })
            }
        }
        XposedBridge.log("Rhpatch: [Download] IgImageView hooks installed successfully")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] IgImageView hook failed: $it") }

    // ── Intercept Share Button click to capture target post URL ────────────────
    runCatching {
        XposedHelpers.findAndHookMethod(
            View::class.java,
            "performClick",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as? View ?: return
                    val context = view.context ?: return
                    if (!context.packageName.contains("instagram", ignoreCase = true)) return

                    val desc = view.contentDescription?.toString()?.lowercase() ?: ""
                    if (desc.contains("share") || desc.contains("bagikan") || desc.contains("send") || desc.contains("kirim") || desc.contains("direct") || desc.contains("pesawat")) {
                        shareClickedTime = System.currentTimeMillis()
                        captureUrlFromPostContainer(view)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            View::class.java,
            "setOnClickListener",
            View.OnClickListener::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val listener = param.args[0] as? View.OnClickListener ?: return
                    val view = param.thisObject as? View ?: return
                    
                    // Skip wrapping our own wrapper
                    if (listener.javaClass.name.contains("RhpatchWrapper")) return
                    
                    val desc = view.contentDescription?.toString()?.lowercase() ?: ""
                    if (desc.contains("share") || desc.contains("bagikan") || desc.contains("send") || desc.contains("kirim") || desc.contains("direct") || desc.contains("pesawat")) {
                        // Wrap click listener
                        val wrapper = object : View.OnClickListener {
                            override fun onClick(v: View) {
                                shareClickedTime = System.currentTimeMillis()
                                captureUrlFromPostContainer(v)
                                listener.onClick(v)
                            }
                        }
                        class RhpatchWrapperListener(val orig: View.OnClickListener) : View.OnClickListener {
                            override fun onClick(v: View) {
                                shareClickedTime = System.currentTimeMillis()
                                captureUrlFromPostContainer(v)
                                orig.onClick(v)
                            }
                        }
                        param.args[0] = RhpatchWrapperListener(listener)
                        
                        // Add long click listener as a reliable fallback
                        try {
                            view.setOnLongClickListener { v ->
                                captureUrlFromPostContainer(v)
                                val url = lastKnownMediaUrl
                                if (!url.isNullOrEmpty()) {
                                    downloadInstagramMedia(v.context, url, lastKnownIsVideo)
                                    true
                                } else {
                                    Toast.makeText(v.context, "URL media belum tertangkap. Tonton video lebih lama.", Toast.LENGTH_SHORT).show()
                                    true
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        )
        XposedBridge.log("Rhpatch: [Download] Share click listener hooks installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] Share click hook failed: $it") }

    // ── Inject download row into share sheet via RecyclerView.onAttachedToWindow ──
    runCatching {
        val recyclerViewClass = XposedHelpers.findClassIfExists(
            "androidx.recyclerview.widget.RecyclerView", classLoader
        ) ?: throw ClassNotFoundException("RecyclerView not found")

        XposedHelpers.findAndHookMethod(
            recyclerViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val rv = param.thisObject as? View ?: return
                    val context = rv.context ?: return
                    if (!context.packageName.contains("instagram", ignoreCase = true)) return

                    // Check if share button was clicked recently (within 3 seconds)
                    val timeDiff = System.currentTimeMillis() - shareClickedTime
                    if (timeDiff > 3000) {
                        return
                    }

                    // Avoid double injection
                    if (rv.getTag(0x52680001) == "rhp_ok") return
                    rv.setTag(0x52680001, "rhp_ok")
                    shareClickedTime = 0 // Reset immediately to prevent multi-injection

                    // Add delay to ensure parent view hierarchy is built
                    Handler(Looper.getMainLooper()).postDelayed({
                        runCatching {
                            injectDownloadAboveShareSheet(rv, context)
                        }
                    }, 100)
                }
            }
        )
        XposedBridge.log("Rhpatch: [Download] RecyclerView hooks installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] RecyclerView failed: $it") }
}

fun captureUrlFromPostContainer(clickedView: View) {
    var highestContainer: ViewGroup? = clickedView.parent as? ViewGroup
    var current: View? = clickedView
    repeat(6) {
        val parent = current?.parent as? ViewGroup
        if (parent != null) {
            highestContainer = parent
            current = parent
        }
    }

    val container = highestContainer ?: return
    val igImageView = findPostMediaImageView(container)
    if (igImageView != null) {
        runCatching {
            val imageUrl = XposedHelpers.getObjectField(igImageView, "A0C")
            if (imageUrl != null) {
                val url = XposedHelpers.callMethod(imageUrl, "getUrl") as? String
                if (!url.isNullOrEmpty() && isInstagramCdnUrl(url)) {
                    lastKnownMediaUrl = url
                    lastKnownIsVideo = url.contains(".mp4") || url.contains("video")
                    XposedBridge.log("Rhpatch: [Download] Captured media URL from container: $url")
                }
            }
        }
    }
}

fun findPostMediaImageView(parent: ViewGroup): View? {
    for (i in 0 until parent.childCount) {
        val child = parent.getChildAt(i) ?: continue
        val className = child.javaClass.name
        if (className.contains("IgImageView") || className.contains("IgProgressImageView")) {
            // Filter out small icons/avatars (post media is always large on screen)
            if (child.width > 200 && child.height > 200) {
                if (className.contains("IgProgressImageView") && child is ViewGroup) {
                    val inner = findPostMediaImageView(child)
                    if (inner != null) return inner
                }
                return child
            }
        }
        if (child is ViewGroup) {
            val found = findPostMediaImageView(child)
            if (found != null) return found
        }
    }
    return null
}

/** Extracts Instagram CDN URL from method arguments */
fun extractUrlFromArgs(args: Array<Any?>) {
    args.forEach { arg ->
        if (arg == null) return@forEach
        runCatching {
            val url: String? = when {
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
            if (!url.isNullOrEmpty() && isInstagramCdnUrl(url)) {
                lastKnownMediaUrl = url
                lastKnownIsVideo = url.contains(".mp4") || url.contains("video")
            }
        }
    }
}

fun isInstagramCdnUrl(url: String): Boolean =
    (url.contains("fbcdn.net") || url.contains("cdninstagram.com")) &&
    (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".mp4") ||
        url.contains("video") || url.contains("scontent"))

fun injectDownloadAboveShareSheet(rv: View, context: Context) {
    val parent = rv.parent as? ViewGroup ?: return
    if (parent.findViewWithTag<View>("rhp_dl_row") != null) return

    val dp = context.resources.displayMetrics.density

    // Container row
    val row = LinearLayout(context).apply {
        tag = "rhp_dl_row"
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((20 * dp).toInt(), (14 * dp).toInt(), (20 * dp).toInt(), (10 * dp).toInt())
        isClickable = true
        isFocusable = true
    }

    val iconSize = (46 * dp).toInt()
    val circle = FrameLayout(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF3A3A3C.toInt())
        }
    }
    val icon = ImageView(context).apply {
        setImageResource(android.R.drawable.stat_sys_download)
        setColorFilter(Color.WHITE)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
    }
    circle.addView(icon, FrameLayout.LayoutParams(iconSize, iconSize))

    val label = TextView(context).apply {
        text = "Unduh"
        val typedValue = TypedValue()
        val theme = context.theme
        val colorRes = if (theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            Color.WHITE
        }
        setTextColor(colorRes)
        textSize = 14f
        setPadding((16 * dp).toInt(), 0, 0, 0)
    }

    row.addView(circle, LinearLayout.LayoutParams(iconSize, iconSize))
    row.addView(label, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ))

    row.setOnClickListener {
        val url = lastKnownMediaUrl
        if (url.isNullOrEmpty()) {
            Toast.makeText(context,
                "URL tidak ditemukan. Ulangi kembali atau buka share lagi.",
                Toast.LENGTH_LONG).show()
        } else {
            downloadInstagramMedia(context, url, lastKnownIsVideo)
        }
    }

    // Wrap rv and row in a vertical LinearLayout to prevent overlapping in FrameLayout/CoordinatorLayout
    val rvIndex = (0 until parent.childCount).indexOfFirst { parent.getChildAt(it) == rv }
    val insertIdx = if (rvIndex < 0) 0 else rvIndex
    val origParams = rv.layoutParams

    runCatching {
        // Remove rv from parent
        parent.removeView(rv)

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = origParams // Inherit original layout params from rv
        }

        // Add row and rv to the wrapper
        wrapper.addView(row, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        
        // Add rv below row
        val rvParams = if (origParams is LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams(origParams.width, origParams.height, origParams.weight)
        } else {
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        wrapper.addView(rv, rvParams)

        // Add wrapper back to parent at original index
        parent.addView(wrapper, insertIdx)
        XposedBridge.log("Rhpatch: [Download] Wrapped RecyclerView and injected download row successfully")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Download] Wrap failed, falling back to simple addView: $it")
        runCatching { parent.addView(row) }
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
