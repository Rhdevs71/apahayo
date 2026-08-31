package com.rhdevs.rhpatch.activity

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rhdevs.rhpatch.R

class ThemeStudioActivity : AppCompatActivity() {

    private lateinit var mockContainer: FrameLayout
    private var isHomeView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_studio)

        mockContainer = findViewById(R.id.mock_container)
        val btnSwitch = findViewById<View>(R.id.btn_switch_view)
        val btnExport = findViewById<View>(R.id.btn_export_theme)

        loadMockView()

        btnSwitch.setOnClickListener {
            isHomeView = !isHomeView
            loadMockView()
        }

        btnExport.setOnClickListener {
            ThemeExporter.exportTheme(this)
        }
    }

    private fun loadMockView() {
        mockContainer.removeAllViews()
        val layoutRes = if (isHomeView) R.layout.mock_whatsapp_home else R.layout.mock_whatsapp_chat
        val mockView = LayoutInflater.from(this).inflate(layoutRes, mockContainer, false)
        mockContainer.addView(mockView)

        if (isHomeView) {
            setupClickListener(mockView, R.id.toolbar, "#toolbar")
            setupClickListener(mockView, R.id.menuitem_camera, "#menuitem_camera")
            setupClickListener(mockView, R.id.menuitem_search, "#menuitem_search")
            setupClickListener(mockView, R.id.chat_list, "#chat_list")
            setupClickListener(mockView, R.id.main_layout, "#main_layout")
        } else {
            setupClickListener(mockView, R.id.chat_toolbar, "#chat_toolbar")
            setupClickListener(mockView, R.id.chat_background, "#conversation_background")
            setupClickListener(mockView, R.id.bubble_left, "#bubble_left")
            setupClickListener(mockView, R.id.bubble_right, "#bubble_right")
            setupClickListener(mockView, R.id.bottom_nav, "#bottom_nav")
        }
    }

    private fun setupClickListener(parent: View, id: Int, cssKey: String) {
        val view = parent.findViewById<View>(id)
        view?.setOnClickListener {
            showConfigDialog(cssKey, view)
        }
    }

    private fun showConfigDialog(cssKey: String, view: View) {
        val state = ThemeStateManager.getState(cssKey)
        
        val dialog = BottomSheetDialog(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // Hide Switch
        val swHide = Switch(this).apply {
            text = "Hide Element (display: none)"
            isChecked = state.isHidden
            setOnCheckedChangeListener { _, isChecked -> state.isHidden = isChecked }
        }
        container.addView(swHide)

        // Color Input
        val colorInput = EditText(this).apply {
            hint = "Background Color (e.g. #FF0000)"
            setText(state.bgColor ?: "")
        }
        container.addView(colorInput)

        // Radius Input
        val radiusInput = EditText(this).apply {
            hint = "Border Radius (px)"
            setText(state.radius?.toString() ?: "")
        }
        container.addView(radiusInput)

        // Wallpaper Button (only for backgrounds)
        if (cssKey == "#conversation_background" || cssKey == "#main_layout") {
            val btnWallpaper = Button(this).apply {
                text = "Select Wallpaper"
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(intent, 1001)
                    dialog.dismiss()
                }
            }
            container.addView(btnWallpaper)
        }

        val btnSave = Button(this).apply {
            text = "Apply"
            setOnClickListener {
                state.bgColor = colorInput.text.toString().takeIf { it.isNotEmpty() }
                state.radius = radiusInput.text.toString().toIntOrNull()
                
                // Live preview logic (basic)
                if (state.isHidden) {
                    view.visibility = View.GONE
                } else {
                    view.visibility = View.VISIBLE
                    try {
                        state.bgColor?.let { view.setBackgroundColor(Color.parseColor(it)) }
                    } catch (e: Exception) {}
                }
                
                dialog.dismiss()
                Toast.makeText(this@ThemeStudioActivity, "Saved", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(btnSave)

        dialog.setContentView(container)
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            ThemeStateManager.wallpaperUri = data?.data?.toString()
            Toast.makeText(this, "Wallpaper Selected!", Toast.LENGTH_SHORT).show()
        }
    }
}



