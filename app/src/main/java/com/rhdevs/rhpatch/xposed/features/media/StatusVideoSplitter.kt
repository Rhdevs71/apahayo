package com.rhdevs.rhpatch.xposed.features.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.WppCore
import com.rhdevs.rhpatch.xposed.core.components.AlertDialogWpp
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class StatusVideoSplitter(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    companion object {
        private const val SPLIT_BTN_TAG = "rhpatch_status_split_btn"
    }

    override fun getPluginName(): String {
        return "StatusVideoSplitter"
    }

    override fun doHook() {
        try {
            val fragmentClass = Unobfuscator.loadVideoComposerFragmentClass(classLoader) ?: return

            XposedHelpers.findAndHookMethod(
                fragmentClass,
                "onViewCreated",
                View::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            // Check if feature is enabled in settings (optional)
                            if (!prefs.getBoolean("status_video_splitter_enabled", false)) return

                            val fragment = param.thisObject ?: return
                            val rootView = param.args[0] as? ViewGroup ?: return

                            rootView.postDelayed({
                                checkAndInjectSplitButton(fragment, rootView)
                            }, 500L)
                        } catch (t: Throwable) {
                            logDebug("StatusVideoSplitter onViewCreated error", t)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            logDebug("StatusVideoSplitter doHook error", t)
        }
    }

    private fun checkAndInjectSplitButton(fragment: Any, root: ViewGroup) {
        if (!prefs.getBoolean("status_video_splitter_enabled", false)) return
        if (root.findViewWithTag<View>(SPLIT_BTN_TAG) != null) return

        val activity = WppCore.getCurrentActivity() ?: return
        val uri = getMediaUri(fragment, activity) ?: return

        val splitSec = prefs.getString("status_split_duration", "30")?.toLongOrNull() ?: 30L
        val minThresholdMs = (splitSec + 2) * 1000L

        // Check if video is longer than threshold
        var durationMs = getVideoDurationMs(activity, uri)
        if (durationMs <= 0L) {
            // Fallback to player duration
            val player = getPlayer(fragment)
            if (player != null) {
                durationMs = (XposedHelpers.callMethod(player, "getDuration") as? Number)?.toLong() ?: 0L
            }
        }

        if (durationMs > 0L && durationMs <= minThresholdMs) return

        val btn = TextView(activity).apply {
            tag = SPLIT_BTN_TAG
            text = "✂️ Split Status (${splitSec}s)"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER

            val padH = Utils.dipToPixels(12)
            val padV = Utils.dipToPixels(7)
            setPadding(padH, padV, padH, padV)

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E600A884")) // Solid accented WhatsApp green
                cornerRadius = Utils.dipToPixels(18).toFloat()
                setStroke(Utils.dipToPixels(1), Color.WHITE)
            }

            val topOffset = Utils.dipToPixels(72)
            val endOffset = Utils.dipToPixels(16)

            layoutParams = when (root) {
                is FrameLayout -> FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = topOffset
                    rightMargin = endOffset
                }
                is RelativeLayout -> RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    topMargin = topOffset
                    rightMargin = endOffset
                }
                else -> ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = topOffset
                    leftMargin = endOffset
                }
            }

            elevation = Utils.dipToPixels(20).toFloat()

            setOnClickListener {
                startVideoSplitting(activity, uri, splitSec)
            }
        }

        root.addView(btn)
        btn.bringToFront()
    }

    private fun startVideoSplitting(activity: Activity, uri: Uri, splitSeconds: Long) {
        val pad = Utils.dipToPixels(16)
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Utils.dipToPixels(12)
                bottomMargin = Utils.dipToPixels(8)
            }
        }

        val tvStatus = TextView(activity).apply {
            text = "Menyiapkan pemotongan video..."
            textSize = 14f
            gravity = Gravity.CENTER
        }

        container.addView(tvStatus)
        container.addView(progressBar)

        val dialog = AlertDialogWpp(activity).apply {
            setTitle(try { Utils.getString(R.string.status_splitter_title) } catch (_: Throwable) { "Auto-Split Video Status" })
            setView(container)
            setNegativeButton("Batal") { d, _ -> d.dismiss() }
        }

        val createdDialog = dialog.create()
        createdDialog.setCanceledOnTouchOutside(false)
        createdDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outputDir = File(activity.cacheDir, "status_splits").apply { mkdirs() }
                outputDir.listFiles()?.forEach { if (it.isFile) it.delete() }

                val segmentDurationUs = splitSeconds * 1_000_000L
                val splitFiles = splitVideoLossless(
                    context = activity,
                    inputUri = uri,
                    segmentDurationUs = segmentDurationUs,
                    outputDir = outputDir
                ) { percent ->
                    activity.runOnUiThread {
                        progressBar.progress = percent
                        tvStatus.text = "Memotong video status ($percent%)..."
                    }
                }

                withContext(Dispatchers.Main) {
                    createdDialog.dismiss()

                    if (splitFiles.isNotEmpty()) {
                        val splitUris = ArrayList(splitFiles.map { Uri.fromFile(it) })
                        val intent = Intent()
                        intent.setClassName(activity.packageName, activity.javaClass.name)
                        intent.putExtra("jids", arrayListOf("status@broadcast"))
                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, splitUris)

                        activity.startActivity(intent)
                        activity.finish()
                    } else {
                        Toast.makeText(activity, "Gagal memotong video status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                logDebug("Split video error", e)
                withContext(Dispatchers.Main) {
                    createdDialog.dismiss()
                    Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun splitVideoLossless(
        context: Context,
        inputUri: Uri,
        segmentDurationUs: Long,
        outputDir: File,
        onProgress: (Int) -> Unit
    ): List<File> {
        val splitFiles = mutableListOf<File>()
        val extractor = MediaExtractor()

        val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return emptyList()
        extractor.setDataSource(pfd.fileDescriptor)

        val trackCount = extractor.trackCount
        var videoTrackIndex = -1
        var audioTrackIndex = -1

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/") && videoTrackIndex == -1) {
                videoTrackIndex = i
            } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                audioTrackIndex = i
            }
        }

        val videoFormat = if (videoTrackIndex != -1) extractor.getTrackFormat(videoTrackIndex) else null
        val durationUs = videoFormat?.getLong(MediaFormat.KEY_DURATION) ?: 0L
        if (durationUs <= 0L) {
            pfd.close()
            extractor.release()
            return emptyList()
        }

        val totalSegments = ((durationUs + segmentDurationUs - 1) / segmentDurationUs).toInt()
        val buffer = ByteBuffer.allocateDirect(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        for (segIndex in 0 until totalSegments) {
            val startUs = segIndex * segmentDurationUs
            val endUs = ((segIndex + 1) * segmentDurationUs).coerceAtMost(durationUs)

            val outFile = File(outputDir, "status_part_${segIndex + 1}.mp4")
            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            if (videoTrackIndex != -1) {
                trackMap[videoTrackIndex] = muxer.addTrack(extractor.getTrackFormat(videoTrackIndex))
            }
            if (audioTrackIndex != -1) {
                trackMap[audioTrackIndex] = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
            }

            muxer.start()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            if (videoTrackIndex != -1) extractor.selectTrack(videoTrackIndex)
            if (audioTrackIndex != -1) extractor.selectTrack(audioTrackIndex)

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime >= endUs) break

                val muxerTrack = trackMap[trackIndex]
                if (muxerTrack != null && sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags

                    if (bufferInfo.size > 0) {
                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    }
                }

                if (!extractor.advance()) break
            }

            if (videoTrackIndex != -1) extractor.unselectTrack(videoTrackIndex)
            if (audioTrackIndex != -1) extractor.unselectTrack(audioTrackIndex)

            try {
                muxer.stop()
                muxer.release()
                splitFiles.add(outFile)
            } catch (_: Throwable) {}

            onProgress(((segIndex + 1) * 100) / totalSegments)
        }

        pfd.close()
        extractor.release()
        return splitFiles
    }

    private fun getVideoDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            dur?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    private fun getMediaUri(fragment: Any, activity: Activity?): Uri? {
        if (activity != null) {
            val intent = activity.intent
            val streamList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (!streamList.isNullOrEmpty()) return streamList[0]

            val singleStream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (singleStream != null) return singleStream

            val dataUri = intent.data
            if (dataUri != null) return dataUri

            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val clipUri = clipData.getItemAt(0).uri
                if (clipUri != null) return clipUri
            }
        }

        try {
            val f = fragment as? androidx.fragment.app.Fragment
            val stream = f?.arguments?.getParcelable<Uri>(Intent.EXTRA_STREAM)
            if (stream != null) return stream
        } catch (_: Throwable) {}

        var currClass: Class<*>? = fragment.javaClass
        while (currClass != null && currClass != Any::class.java) {
            for (field in currClass.declaredFields) {
                try {
                    field.isAccessible = true
                    if (field.type == Uri::class.java) {
                        val uri = field.get(fragment) as? Uri
                        if (uri != null) return uri
                    } else if (field.type == File::class.java) {
                        val file = field.get(fragment) as? File
                        if (file != null && file.exists()) return Uri.fromFile(file)
                    }
                } catch (_: Throwable) {}
            }
            currClass = currClass.superclass
        }
        return null
    }

    private fun getPlayer(fragment: Any): Any? {
        return try {
            XposedHelpers.getObjectField(fragment, "A0N")
        } catch (_: Throwable) {
            null
        }
    }
}
