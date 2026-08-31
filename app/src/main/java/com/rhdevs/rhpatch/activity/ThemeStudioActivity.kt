package com.rhdevs.rhpatch.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import android.graphics.Outline
import android.view.ViewOutlineProvider
import com.rhdevs.rhpatch.R

class ThemeStudioActivity : Activity() {

    private var isHomeView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_studio)

        val previewContainer = findViewById<FrameLayout>(R.id.preview_container)
        val btnSwitchView = findViewById<Button>(R.id.btn_switch_view)
        val btnExport = findViewById<Button>(R.id.btn_export)

        val seekRadius = findViewById<SeekBar>(R.id.seek_radius)
        val switchCamera = findViewById<Switch>(R.id.switch_camera)
        
        val switchHideRead = findViewById<Switch>(R.id.switch_hideread)
        val switchAntiDelete = findViewById<Switch>(R.id.switch_antidelete)

        // Helper to get views safely regardless of which mock layout is active
        fun updateLivePreview() {
            // Live Radius update on mock toolbar
            val toolbar = findViewById<View>(R.id.toolbar) ?: findViewById<View>(R.id.chat_toolbar)
            if (toolbar != null) {
                val radius = seekRadius.progress.toFloat() * 2 // scale for effect
                toolbar.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radius)
                    }
                }
                toolbar.clipToOutline = true
                toolbar.invalidate()
            }

            // Live visibility update
            val cameraIcon = findViewById<View>(R.id.menuitem_camera)
            if (cameraIcon != null) {
                cameraIcon.visibility = if (switchCamera.isChecked) View.VISIBLE else View.GONE
            }
        }

        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLivePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchCamera.setOnCheckedChangeListener { _, _ -> updateLivePreview() }

        btnSwitchView.setOnClickListener {
            previewContainer.removeAllViews()
            if (isHomeView) {
                layoutInflater.inflate(R.layout.mock_whatsapp_chat, previewContainer, true)
                btnSwitchView.text = "Switch to Home View Preview"
            } else {
                layoutInflater.inflate(R.layout.mock_whatsapp_home, previewContainer, true)
                btnSwitchView.text = "Switch to Chat View Preview"
            }
            isHomeView = !isHomeView
            updateLivePreview() // Reapply current state to new layout
        }

        btnExport.setOnClickListener {
            // We pass the current state to the Exporter (Phase 4)
            val radius = seekRadius.progress
            val showCamera = switchCamera.isChecked
            val hideRead = switchHideRead.isChecked
            val antiDelete = switchAntiDelete.isChecked

            Toast.makeText(this, "Exporting theme (Phase 4)...", Toast.LENGTH_SHORT).show()
            ThemeExporter.export(this, radius, showCamera, hideRead, antiDelete)
        }
    }
}
