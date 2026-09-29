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
        private const val STATUS_MAX_SEGMENT_US = 30_000_000L // 30 seconds in microseconds
    }

    override fun getPluginName(): String {
        return "StatusVideoSplitter"
    }

    override fun doHook() {
        if (!prefs.getBoolean("status_video_splitter_enabled", true)) return

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
        if (root.findViewWithTag<View>(SPLIT_BTN_TAG) != null) return

        val activity = WppCore.getCurrentActivity() ?: return
        val uri = getMediaUri(fragment) ?: return

        // Check if video is longer than 30s
        val durationMs = getVideoDurationMs(activity, uri)
        if (durationMs <= 32_000L) return // Less than or equal to ~30s, no split needed

        val btn = TextView(activity).apply {
            tag = SPLIT_BTN_TAG
            text = try { Utils.getString(R.string.status_splitter_btn) } catch (_: Throwable) { "✂️ Split Status (30s)" }
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER

            val padH = Utils.dipToPixels(10)
            val padV = Utils.dipToPixels(6)
            setPadding(padH, padV, padH, padV)

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC00A884")) // WhatsApp Green accented
                cornerRadius = Utils.dipToPixels(16).toFloat()
            }

            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = Utils.dipToPixels(64)
                rightMargin = Utils.dipToPixels(16)
            }

            setOnClickListener {
                startVideoSplitting(activity, uri, durationMs)
            }
        }

        root.addView(btn)
    }

    private fun startVideoSplitting(activity: Activity, uri: Uri, durationMs: Long) {
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
                outputDir.listFiles()?.forEach { if (it.isFile) it.delete() } // cleanup old

                val splitFiles = splitVideoLossless(
                    context = activity,
                    inputUri = uri,
                    segmentDurationUs = STATUS_MAX_SEGMENT_US,
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
                        Toast.makeText(activity, "Gagal memotong video", Toast.LENGTH_SHORT).show()
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

    private fun getMediaUri(fragment: Any): Uri? {
        try {
            val f = fragment as? androidx.fragment.app.Fragment
            val stream = f?.arguments?.getParcelable<Uri>(Intent.EXTRA_STREAM)
            if (stream != null) return stream
        } catch (_: Throwable) {}

        for (field in fragment.javaClass.declaredFields) {
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
        return null
    }
}
