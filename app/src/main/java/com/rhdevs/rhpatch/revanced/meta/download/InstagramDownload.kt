package com.rhdevs.rhpatch.revanced.meta.download

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.recyclerview.widget.RecyclerView
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

var lastKnownMediaUrl: String? = null
var lastKnownIsVideo: Boolean = false

val playerUrls = java.util.WeakHashMap<Any, String>()
val imageUrls = java.util.WeakHashMap<View, String>()

val InstagramDownload = patch(
    name = "Instagram Media Downloader",
    description = "Add native-looking download button to Instagram share sheet (Piko Parity)"
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
                            lastKnownMediaUrl = url
                            lastKnownIsVideo = false
                        }
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [Download] IgImageView tracking hook failed: $it") }

    // ── Inject Native-Looking Download Row into Dialogs/BottomSheet ───────────
    runCatching {
        val dialogClass = android.app.Dialog::class.java
        dialogClass.declaredMethods.filter { it.name == "show" }.forEach { method ->
            XposedBridge.hookMethod(
                method,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as? android.app.Dialog ?: return
                        val context = dialog.context
                        if (!context.packageName.contains("instagram", ignoreCase = true)) return

                        val window = dialog.window ?: return
                        val decorView = window.decorView as? ViewGroup ?: return
                        
                        if (decorView.findViewWithTag<View>("rhp_dl_native_btn") != null) return

                        if (lastKnownMediaUrl.isNullOrEmpty()) {
                            val activity = dialog.ownerActivity ?: (context as? android.app.Activity)
                            activity?.window?.decorView?.let { activityDecor ->
                                if (activityDecor is ViewGroup) {
                                    val url = searchUrlInTree(activityDecor)
                                    if (url != null) {
                                        lastKnownMediaUrl = url
                                        lastKnownIsVideo = url.contains(".mp4") || url.contains("video")
                                    }
                                }
                            }
                        }

                        if (lastKnownMediaUrl.isNullOrEmpty()) return

                        Handler(Looper.getMainLooper()).postDelayed({
                            runCatching {
                                injectNativeDownloadRow(decorView, context)
                            }
                        }, 300)
                    }
                }
            )
        }

        val bottomSheetClass = XposedHelpers.findClassIfExists("com.instagram.igds.components.bottomsheet.BottomSheetFragment", classLoader)
        if (bottomSheetClass != null) {
            bottomSheetClass.declaredMethods.find { it.name == "onViewCreated" }?.let { onViewCreated ->
                XposedBridge.hookMethod(onViewCreated, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.args[0] as? View ?: return
                        val context = view.context
                        if (lastKnownMediaUrl.isNullOrEmpty()) return

                        Handler(Looper.getMainLooper()).postDelayed({
                            runCatching {
                                if (view is ViewGroup) {
                                    injectNativeDownloadRow(view, context)
                                }
                            }
                        }, 500)
                    }
                })
            }
        }
        XposedBridge.log("Rhpatch: [Download] Native row injection hooks installed")
    }.onFailure { XposedBridge.log("Rhpatch: [Download] Dialog/BottomSheet hook failed: $it") }
}

fun searchUrlInTree(view: View): String? {
    if (imageUrls.containsKey(view)) return imageUrls[view]
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
                    val localCfg = runCatching { XposedHelpers.getObjectField(arg, "localConfiguration") }.getOrNull()
                    val uri = runCatching { XposedHelpers.getObjectField(localCfg ?: return@runCatching null, "uri") }.getOrNull()
                    uri?.toString()
                }
            }
            if (!u.isNullOrEmpty() && isInstagramCdnUrl(u)) return@runCatching u
            null
        }.getOrNull()
        if (url != null) return url
    }
    return null
}

fun isInstagramCdnUrl(url: String): Boolean =
    (url.contains("fbcdn.net") || url.contains("cdninstagram.com")) &&
    (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".mp4") || url.contains("video") || url.contains("scontent"))

fun injectNativeDownloadRow(rootView: ViewGroup, context: Context) {
    if (rootView.findViewWithTag<View>("rhp_dl_native_btn") != null) return

    val dp = context.resources.displayMetrics.density

    // Create a native-looking Instagram menu row
    val rowLayout = LinearLayout(context).apply {
        tag = "rhp_dl_native_btn"
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
        setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
    }

    val icon = ImageView(context).apply {
        setImageResource(android.R.drawable.stat_sys_download)
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        setColorFilter(if (isDarkMode) Color.WHITE else Color.BLACK)
        layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()).apply {
            marginEnd = (16 * dp).toInt()
        }
    }

    val text = TextView(context).apply {
        text = "Download (Rhpatch)"
        textSize = 16f
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        setTextColor(if (isDarkMode) Color.WHITE else Color.BLACK)
        typeface = Typeface.DEFAULT_BOLD
    }

    rowLayout.addView(icon)
    rowLayout.addView(text)

    rowLayout.setOnClickListener {
        val url = lastKnownMediaUrl
        val isVideo = lastKnownIsVideo
        if (!url.isNullOrEmpty()) {
            downloadInstagramMedia(context, url, isVideo)
        } else {
            Toast.makeText(context, "URL tidak ditemukan. Putar media sebentar lalu coba lagi.", Toast.LENGTH_LONG).show()
        }
    }

    // Find the best place to insert it (usually inside a RecyclerView or a specific LinearLayout)
    val targetContainer = findVerticalContainer(rootView)
    if (targetContainer != null) {
        // If it's a RecyclerView, we can't add directly. We must add it below/above the RecyclerView inside its parent.
        if (targetContainer is RecyclerView) {
            val parent = targetContainer.parent as? ViewGroup
            if (parent != null) {
                // Determine insertion index based on whether we want it at top or bottom of the list
                val rvIndex = parent.indexOfChild(targetContainer)
                parent.addView(rowLayout, rvIndex + 1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                XposedBridge.log("Rhpatch: [Download] Injected native download row below RecyclerView")
                return
            }
        } else {
            targetContainer.addView(rowLayout, 0, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            XposedBridge.log("Rhpatch: [Download] Injected native download row into LinearLayout")
            return
        }
    }

    // Fallback: Add to Bottom of Root View
    val fallbackContainer = FrameLayout(context).apply {
        tag = "rhp_dl_native_btn" // Tag here too so we don't double inject
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
            bottomMargin = (16 * dp).toInt()
        }
        setBackgroundColor(if ((context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.parseColor("#262626") else Color.WHITE)
        elevation = 10 * dp
    }
    fallbackContainer.addView(rowLayout)
    
    runCatching {
        rootView.addView(fallbackContainer)
        XposedBridge.log("Rhpatch: [Download] Injected native download row as fallback overlay")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Download] Failed to add fallback row: $it")
    }
}

fun findVerticalContainer(view: View): ViewGroup? {
    if (view is RecyclerView && view.layoutManager?.canScrollVertically() == true) {
        return view
    }
    if (view is LinearLayout && view.orientation == LinearLayout.VERTICAL && view.childCount > 1) {
        // Check if children look like list items (clickable, specific height)
        val firstChild = view.getChildAt(0)
        if (firstChild != null && firstChild.isClickable) {
            return view
        }
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i) ?: continue
            val found = findVerticalContainer(child)
            if (found != null) return found
        }
    }
    return null
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
