package com.rhdevs.rhpatch.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThemeStudioActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PICK_WALLPAPER = 1001
        private const val REQUEST_PICK_CUSTOM_ICON = 1002
    }

    private lateinit var mockContainer: FrameLayout
    private var isHomeView = true
    private var pendingIconKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_studio)

        mockContainer = findViewById(R.id.mock_container)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val btnSwitch = findViewById<Button>(R.id.btn_switch_view)
        btnSwitch.setOnClickListener {
            isHomeView = !isHomeView
            btnSwitch.text = if (isHomeView) "Home" else "Chat"
            loadMockView()
        }

        findViewById<View>(R.id.btn_apply_theme).setOnClickListener {
            applyThemeToWhatsApp()
        }

        findViewById<View>(R.id.btn_more_menu).setOnClickListener { v ->
            showMoreOptionsMenu(v)
        }

        // Category Buttons
        findViewById<View>(R.id.chip_presets).setOnClickListener { showPresetsDialog() }
        findViewById<View>(R.id.chip_header).setOnClickListener { showHeaderConfig() }
        findViewById<View>(R.id.chip_chat_bubble).setOnClickListener { showChatBubbleConfig() }
        findViewById<View>(R.id.chip_bottom_nav).setOnClickListener { showBottomNavConfig() }
        findViewById<View>(R.id.chip_input_bar).setOnClickListener { showInputBarConfig() }
        findViewById<View>(R.id.chip_wallpaper).setOnClickListener { showWallpaperConfig() }
        findViewById<View>(R.id.chip_icons).setOnClickListener { showCustomIconsConfig() }
        findViewById<View>(R.id.chip_features).setOnClickListener { showModFeaturesConfig() }

        loadMockView()
    }

    private fun loadMockView() {
        mockContainer.removeAllViews()
        val layoutRes = if (isHomeView) R.layout.mock_whatsapp_home else R.layout.mock_whatsapp_chat
        val mockView = LayoutInflater.from(this).inflate(layoutRes, mockContainer, false)
        mockContainer.addView(mockView)

        if (isHomeView) {
            setupHomeInteractions(mockView)
        } else {
            setupChatInteractions(mockView)
        }

        applyLivePreview()
    }

    private fun applyLivePreview() {
        val root = mockContainer.getChildAt(0) ?: return

        if (isHomeView) {
            // Main background & Wallpaper
            ThemeStateManager.wallpaperUri?.let { uriStr ->
                try {
                    val uri = Uri.parse(uriStr)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            root.background = android.graphics.drawable.BitmapDrawable(resources, bmp)
                        }
                    }
                } catch (_: Exception) {}
            } ?: run {
                ThemeStateManager.states["#main_layout"]?.bgColor?.let {
                    try { root.setBackgroundColor(Color.parseColor(it)) } catch (_: Exception) {}
                }
            }

            // Toolbar
            val toolbar = root.findViewById<View>(R.id.toolbar)
            ThemeStateManager.states["#toolbar"]?.bgColor?.let {
                try { toolbar?.setBackgroundColor(Color.parseColor(it)) } catch (_: Exception) {}
            }
            // Title text
            val titleColor = ThemeStateManager.states["#toolbar"]?.textColor
                ?: ThemeStateManager.states["#toolbar_logo"]?.textColor
            titleColor?.let {
                try {
                    val tv = (toolbar as? LinearLayout)?.getChildAt(0) as? TextView
                    tv?.setTextColor(Color.parseColor(it))
                } catch (_: Exception) {}
            }
            // Bottom Nav
            val bNav = root.findViewById<View>(R.id.bottom_nav)
            ThemeStateManager.states["#bottom_nav"]?.bgColor?.let {
                try { bNav?.setBackgroundColor(Color.parseColor(it)) } catch (_: Exception) {}
            }
            // Search Bar
            val searchBar = root.findViewById<View>(R.id.my_search_bar)
            ThemeStateManager.states["#my_search_bar"]?.bgColor?.let {
                try { searchBar?.background?.setTint(Color.parseColor(it)) } catch (_: Exception) {}
            }
            // Camera icon in Toolbar
            val homeCameraBtn = root.findViewById<ImageView>(R.id.menuitem_camera)
            ThemeStateManager.states["#menuitem_camera"]?.textColor?.let {
                try { homeCameraBtn?.imageTintList = ColorStateList.valueOf(Color.parseColor(it)) } catch (_: Exception) {}
            }
            ThemeStateManager.customIcons["menuitem_camera"]?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) homeCameraBtn?.setImageBitmap(bmp)
            }
            // FAB
            val fabContainer = root.findViewById<View>(R.id.fab_container)
            val fab = root.findViewById<ImageView>(R.id.fab)
            ThemeStateManager.states["#fab"]?.bgColor?.let {
                try { fabContainer?.background?.setTint(Color.parseColor(it)) } catch (_: Exception) {}
            }
            ThemeStateManager.states["#fab"]?.iconTint?.let {
                try { fab?.imageTintList = ColorStateList.valueOf(Color.parseColor(it)) } catch (_: Exception) {}
            }
            ThemeStateManager.customIcons["fab"]?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) fab?.setImageBitmap(bmp)
            } ?: run {
                fab?.setImageResource(android.R.drawable.ic_menu_add)
            }
        } else {
            // Chat background & Wallpaper
            val bgView = root.findViewById<View>(R.id.chat_background)
            ThemeStateManager.wallpaperUri?.let { uriStr ->
                try {
                    val uri = Uri.parse(uriStr)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            bgView?.background = android.graphics.drawable.BitmapDrawable(resources, bmp)
                        }
                    }
                } catch (_: Exception) {}
            } ?: run {
                ThemeStateManager.states["#conversation_background"]?.bgColor?.let {
                    try { bgView?.setBackgroundColor(Color.parseColor(it)) } catch (_: Exception) {}
                }
            }

            // Chat Toolbar
            val cToolbar = root.findViewById<View>(R.id.chat_toolbar)
            ThemeStateManager.states["#chat_toolbar"]?.bgColor?.let {
                try { cToolbar?.setBackgroundColor(Color.parseColor(it)) } catch (_: Exception) {}
            }
            // Bubble Left
            val bubbleLeft = root.findViewById<View>(R.id.bubble_left)
            val stLeft = ThemeStateManager.states["#bubble_left"]
            if (stLeft != null && (stLeft.bgColor != null || stLeft.radius != null)) {
                try {
                    val bg = GradientDrawable()
                    stLeft.bgColor?.let { bg.setColor(Color.parseColor(it)) }
                    stLeft.radius?.let { bg.cornerRadius = it.toFloat() * 2f }
                    bubbleLeft?.background = bg
                } catch (_: Exception) {}
            }
            // Bubble Right
            val bubbleRight = root.findViewById<View>(R.id.bubble_right)
            val stRight = ThemeStateManager.states["#bubble_right"]
            if (stRight != null && (stRight.bgColor != null || stRight.radius != null)) {
                try {
                    val bg = GradientDrawable()
                    stRight.bgColor?.let { bg.setColor(Color.parseColor(it)) }
                    stRight.radius?.let { bg.cornerRadius = it.toFloat() * 2f }
                    bubbleRight?.background = bg
                } catch (_: Exception) {}
            }
            // Message text color
            ThemeStateManager.states["#message_text"]?.textColor?.let {
                try {
                    val c = Color.parseColor(it)
                    val tvLeft = (bubbleLeft as? LinearLayout)?.getChildAt(0) as? TextView
                    val tvRight = (bubbleRight as? LinearLayout)?.getChildAt(0) as? TextView
                    tvLeft?.setTextColor(c)
                    tvRight?.setTextColor(c)
                } catch (_: Exception) {}
            }
            // Entry Box
            val entry = root.findViewById<View>(R.id.entry)
            val stEntry = ThemeStateManager.states["#entry"]
            if (stEntry != null && (stEntry.bgColor != null || stEntry.radius != null)) {
                try {
                    val bg = GradientDrawable()
                    stEntry.bgColor?.let { bg.setColor(Color.parseColor(it)) }
                    stEntry.radius?.let { bg.cornerRadius = it.toFloat() * 2f }
                    entry?.background = bg
                } catch (_: Exception) {}
            }
            // Send button
            val send = root.findViewById<View>(R.id.voice_note_btn)
            ThemeStateManager.states["#send"]?.bgColor?.let {
                try { send?.background?.setTint(Color.parseColor(it)) } catch (_: Exception) {}
            }

            // Custom Icons live preview di Chat
            val voiceNoteBtn = root.findViewById<ImageView>(R.id.voice_note_btn)
            val cameraBtn = root.findViewById<ImageView>(R.id.camera_btn)
            val attachBtn = root.findViewById<ImageView>(R.id.input_attach_button)
            val backBtn = root.findViewById<ImageView>(R.id.chat_back_btn)
            val inputEditText = root.findViewById<EditText>(R.id.chat_input_edittext)
            val hasText = !inputEditText?.text.isNullOrBlank()

            ThemeStateManager.customIcons["voice_note_btn"]?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) voiceNoteBtn?.setImageBitmap(bmp)
            } ?: run {
                if (hasText) {
                    voiceNoteBtn?.setImageResource(android.R.drawable.ic_menu_send)
                } else {
                    voiceNoteBtn?.setImageResource(android.R.drawable.ic_btn_speak_now)
                }
            }

            ThemeStateManager.customIcons["camera_btn"]?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) cameraBtn?.setImageBitmap(bmp)
            } ?: run {
                cameraBtn?.setImageResource(android.R.drawable.ic_menu_camera)
            }

            ThemeStateManager.customIcons["input_attach_button"]?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) attachBtn?.setImageBitmap(bmp)
            } ?: run {
                attachBtn?.setImageResource(android.R.drawable.ic_menu_add)
            }

            val backIconPath = ThemeStateManager.customIcons["back"] ?: ThemeStateManager.customIcons["chat_back_btn"]
            backIconPath?.let { path ->
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) backBtn?.setImageBitmap(bmp)
            } ?: run {
                backBtn?.setImageResource(android.R.drawable.ic_media_previous)
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private fun setupChatInteractions(root: View) {
        val toolbar = root.findViewById<View>(R.id.chat_toolbar)
        val backBtn = root.findViewById<View>(R.id.chat_back_btn)
        val bubbleLeft = root.findViewById<View>(R.id.bubble_left)
        val bubbleRight = root.findViewById<View>(R.id.bubble_right)
        val bgView = root.findViewById<View>(R.id.chat_background)
        val scrollView = root.findViewById<ScrollView>(R.id.chat_scroll_view)
        val conversationList = root.findViewById<LinearLayout>(R.id.conversation_list)
        val cameraBtn = root.findViewById<View>(R.id.camera_btn)
        val attachBtn = root.findViewById<View>(R.id.input_attach_button)
        val emojiBtn = root.findViewById<View>(R.id.emoji_picker_btn)
        val voiceNoteBtn = root.findViewById<ImageView>(R.id.voice_note_btn)
        val inputEditText = root.findViewById<EditText>(R.id.chat_input_edittext)

        // 1. Toolbar click -> Header config
        toolbar?.setOnClickListener { showHeaderConfig() }

        // 2. Back button click -> Direct icon / reset
        backBtn?.setOnClickListener { showDirectIconDialog("Tombol Kembali (Back)", "back", canTint = false) }

        // 3. Bubbles direct tap -> Direct Bubble Config
        bubbleLeft?.setOnClickListener { showBubbleConfigDialog(isOutgoing = false) }
        bubbleRight?.setOnClickListener { showBubbleConfigDialog(isOutgoing = true) }

        // 4. Wallpaper background click -> Wallpaper config
        bgView?.setOnClickListener { showWallpaperConfig() }
        scrollView?.setOnClickListener { showWallpaperConfig() }

        // 5. Icons direct tap -> Direct Icon Dialog
        cameraBtn?.setOnClickListener { showDirectIconDialog("Tombol Kamera", "camera_btn", canTint = true) }
        attachBtn?.setOnClickListener { showDirectIconDialog("Tombol Lampiran", "input_attach_button", canTint = true) }
        emojiBtn?.setOnClickListener { showDirectIconDialog("Tombol Emoji", "emoji_picker_btn", canTint = true) }

        // 6. Interactive Send / Voice Note button
        voiceNoteBtn?.setOnClickListener {
            val text = inputEditText?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty() && conversationList != null && scrollView != null) {
                sendMockMessage(text, conversationList, scrollView)
                inputEditText?.text?.clear()
            } else {
                showDirectIconDialog("Tombol Kirim / Voice Note", "voice_note_btn", canTint = true)
            }
        }
        voiceNoteBtn?.setOnLongClickListener {
            showDirectIconDialog("Tombol Kirim / Voice Note", "voice_note_btn", canTint = true)
            true
        }

        // 7. Input EditText dynamic text change & send action
        inputEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                if (!ThemeStateManager.customIcons.containsKey("voice_note_btn")) {
                    if (hasText) {
                        voiceNoteBtn?.setImageResource(android.R.drawable.ic_menu_send)
                    } else {
                        voiceNoteBtn?.setImageResource(android.R.drawable.ic_btn_speak_now)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        inputEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val text = inputEditText.text?.toString()?.trim() ?: ""
                if (text.isNotEmpty() && conversationList != null && scrollView != null) {
                    sendMockMessage(text, conversationList, scrollView)
                    inputEditText.text?.clear()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun setupHomeInteractions(root: View) {
        val toolbar = root.findViewById<View>(R.id.toolbar)
        val cameraIcon = root.findViewById<View>(R.id.menuitem_camera)
        val searchBar = root.findViewById<View>(R.id.my_search_bar)
        val bottomNav = root.findViewById<View>(R.id.bottom_nav)
        val fab = root.findViewById<View>(R.id.fab)
        val fabContainer = root.findViewById<View>(R.id.fab_container)

        toolbar?.setOnClickListener { showHeaderConfig() }
        cameraIcon?.setOnClickListener { showDirectIconDialog("Ikon Kamera Toolbar", "menuitem_camera", canTint = true) }
        searchBar?.setOnClickListener {
            showColorPicker("Warna Search Bar", ThemeStateManager.getState("#my_search_bar").bgColor) { hex ->
                ThemeStateManager.getState("#my_search_bar").bgColor = hex
                applyLivePreview()
            }
        }
        bottomNav?.setOnClickListener { showBottomNavConfig() }
        val onFabClick = View.OnClickListener { showDirectIconDialog("Tombol Aksi Terapung (FAB)", "fab", canTint = true) }
        fab?.setOnClickListener(onFabClick)
        fabContainer?.setOnClickListener(onFabClick)
        root.setOnClickListener { showWallpaperConfig() }
    }

    private fun sendMockMessage(text: String, conversationList: LinearLayout, scrollView: ScrollView) {
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginStart = 64.dpToPx()
                bottomMargin = 12.dpToPx()
            }
            layoutParams = lp
            elevation = 1f.dpToPx()
            setPadding(10.dpToPx(), 6.dpToPx(), 10.dpToPx(), 6.dpToPx())

            // Background sesuai tema
            val bg = GradientDrawable()
            val stRight = ThemeStateManager.states["#bubble_right"]
            val bgColor = stRight?.bgColor ?: "#005C4B"
            val radius = (stRight?.radius ?: 10).toFloat() * 2f
            try { bg.setColor(Color.parseColor(bgColor)) } catch (_: Exception) {}
            bg.cornerRadius = radius
            background = bg

            // Teks pesan
            val tvMsg = TextView(this@ThemeStudioActivity).apply {
                this.text = text
                textSize = 16f
                val txtColor = ThemeStateManager.states["#message_text"]?.textColor ?: "#E9EDEF"
                try { setTextColor(Color.parseColor(txtColor)) } catch (_: Exception) {}
            }
            addView(tvMsg)

            // Baris jam & centang
            val timeRow = LinearLayout(this@ThemeStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                    topMargin = 2.dpToPx()
                }

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val tvTime = TextView(this@ThemeStudioActivity).apply {
                    this.text = timeStr
                    textSize = 11f
                    val dateColor = ThemeStateManager.states["#date"]?.textColor ?: "#8696A0"
                    try { setTextColor(Color.parseColor(dateColor)) } catch (_: Exception) {}
                }
                addView(tvTime)

                val tickImg = ImageView(this@ThemeStudioActivity).apply {
                    setImageResource(android.R.drawable.checkbox_on_background)
                    val lpImg = LinearLayout.LayoutParams(14.dpToPx(), 14.dpToPx()).apply {
                        marginStart = 4.dpToPx()
                    }
                    layoutParams = lpImg
                    imageTintList = ColorStateList.valueOf(Color.parseColor("#53BDEB"))
                }
                addView(tickImg)
            }
            addView(timeRow)

            // Klik langsung pada bubble baru untuk langsung kustomisasi
            setOnClickListener {
                showBubbleConfigDialog(isOutgoing = true)
            }
        }

        conversationList.addView(bubble)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showBubbleConfigDialog(isOutgoing: Boolean) {
        val key = if (isOutgoing) "#bubble_right" else "#bubble_left"
        val title = if (isOutgoing) "Gelembung Pesan Keluar (Kanan)" else "Gelembung Pesan Masuk (Kiri)"
        val state = ThemeStateManager.getState(key)

        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 40)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val tvTitle = TextView(this).apply {
            text = "💬 $title"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        root.addView(tvTitle)

        // 1. Warna Latar Gelembung
        val btnBgColor = Button(this).apply {
            text = "🎨 Ubah Warna Gelembung (${state.bgColor ?: "#Default"})"
            setBackgroundColor(Color.parseColor("#2A3942"))
            setTextColor(Color.parseColor("#53BDEB"))
            setOnClickListener {
                showColorPicker("Warna $title", state.bgColor) { hex ->
                    state.bgColor = hex
                    applyLivePreview()
                    dialog.dismiss()
                }
            }
        }
        root.addView(btnBgColor)

        // 2. Warna Teks Pesan
        val btnTextColor = Button(this).apply {
            val textState = ThemeStateManager.getState("#message_text")
            text = "✏️ Ubah Warna Teks Pesan (${textState.textColor ?: "#Default"})"
            setBackgroundColor(Color.parseColor("#2A3942"))
            setTextColor(Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16
            }
            setOnClickListener {
                showColorPicker("Warna Teks Pesan", textState.textColor) { hex ->
                    textState.textColor = hex
                    applyLivePreview()
                    dialog.dismiss()
                }
            }
        }
        root.addView(btnTextColor)

        // 3. Radius Sudut
        val btnRadius = Button(this).apply {
            text = "📐 Atur Kelengkungan Radius (${state.radius ?: 16}px)"
            setBackgroundColor(Color.parseColor("#2A3942"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16
            }
            setOnClickListener {
                showRadiusDialog(title, state.radius ?: 16) { rad ->
                    state.radius = rad
                    applyLivePreview()
                    dialog.dismiss()
                }
            }
        }
        root.addView(btnRadius)

        val btnClose = Button(this).apply {
            text = "Tutup"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun showDirectIconDialog(label: String, targetKey: String, canTint: Boolean = true) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 40)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val tvTitle = TextView(this).apply {
            text = "🎨 Kustomisasi: $label"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        root.addView(tvTitle)

        val isCustom = ThemeStateManager.customIcons.containsKey(targetKey)
        val tvStatus = TextView(this).apply {
            text = "Status: " + (if (isCustom) "Ikon Kustom Dipasang ✓" else "Ikon Bawaan WhatsApp")
            textSize = 13f
            setTextColor(if (isCustom) Color.parseColor("#00F0FF") else Color.parseColor("#8696A0"))
            setPadding(0, 0, 0, 20)
        }
        root.addView(tvStatus)

        // 1. Pilih Gambar dari Galeri / File
        val btnPick = Button(this).apply {
            text = "📁 Pilih Ikon dari Galeri / File"
            setBackgroundColor(Color.parseColor("#2A3942"))
            setTextColor(Color.parseColor("#53BDEB"))
            setOnClickListener {
                pendingIconKey = targetKey
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                try {
                    startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON)
                } catch (_: Exception) {
                    val pickIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                    startActivityForResult(pickIntent, REQUEST_PICK_CUSTOM_ICON)
                }
                dialog.dismiss()
            }
        }
        root.addView(btnPick)

        // 2. Ubah Warna (Latar / Tint)
        val state = ThemeStateManager.getState("#$targetKey")
        if (canTint) {
            val btnColor = Button(this).apply {
                text = "🎨 Ubah Warna (${state.bgColor ?: state.iconTint ?: "#Default"})"
                setBackgroundColor(Color.parseColor("#2A3942"))
                setTextColor(Color.parseColor("#00F0FF"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showColorPicker("Warna $label", state.bgColor ?: state.iconTint) { hex ->
                        state.bgColor = hex
                        state.iconTint = hex
                        applyLivePreview()
                        dialog.dismiss()
                    }
                }
            }
            root.addView(btnColor)
        }

        // 3. Reset ke Bawaan
        if (isCustom || state.bgColor != null || state.iconTint != null) {
            val btnReset = Button(this).apply {
                text = "🔄 Kembalikan ke Bawaan Default"
                setBackgroundColor(Color.parseColor("#3A2020"))
                setTextColor(Color.parseColor("#FF6B6B"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    ThemeStateManager.customIcons.remove(targetKey)
                    state.bgColor = null
                    state.iconTint = null
                    applyLivePreview()
                    dialog.dismiss()
                    Toast.makeText(this@ThemeStudioActivity, "Ikon $label dikembalikan ke default.", Toast.LENGTH_SHORT).show()
                }
            }
            root.addView(btnReset)
        }

        val btnClose = Button(this).apply {
            text = "Tutup"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }

    // --- DIALOG PRESET TEMA ---
    private fun showPresetsDialog() {
        val presets = arrayOf(
            "🍏 iOS Clean Dark (Blue iMessage)",
            "🖤 Pure AMOLED Black (Hemat Baterai)",
            "💜 Cyberpunk Neon (Cyan & Purple Glow)",
            "🌿 Emerald Modern (Hijau Khas WhatsApp)",
            "🌸 Soft Pastel (Pink Peach & Warm White)"
        )
        val keys = arrayOf("ios", "amoled", "cyberpunk", "emerald", "pastel")

        AlertDialog.Builder(this)
            .setTitle("Pilih Preset Tema Siap Pakai")
            .setItems(presets) { _, which ->
                ThemeStateManager.applyPreset(keys[which])
                applyLivePreview()
                Toast.makeText(this, "Preset '${presets[which]}' berhasil dimuat!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- KONTROL HEADER & TOOLBAR ---
    private fun showHeaderConfig() {
        showElementConfigSheet(
            title = "Header & Toolbar",
            items = listOf(
                ConfigItem("Warna Latar Toolbar", "#toolbar", isColor = true),
                ConfigItem("Warna Judul / Logo", "#toolbar_logo", isTextColor = true),
                ConfigItem("Warna Ikon Toolbar", "#menuitem_camera", isTextColor = true),
                ConfigItem("Warna Search Bar", "#my_search_bar", isColor = true)
            )
        )
    }

    // --- KONTROL CHAT & BUBBLE ---
    private fun showChatBubbleConfig() {
        showElementConfigSheet(
            title = "Chat & Gelembung Pesan",
            items = listOf(
                ConfigItem("Gelembung Pesan Masuk (Kiri)", "#bubble_left", isColor = true, isRadius = true),
                ConfigItem("Gelembung Pesan Keluar (Kanan)", "#bubble_right", isColor = true, isRadius = true),
                ConfigItem("Warna Teks Pesan", "#message_text", isTextColor = true),
                ConfigItem("Warna Jam / Waktu", "#date", isTextColor = true),
                ConfigItem("Warna Nama di Grup", "#name_in_group", isTextColor = true),
                ConfigItem("Kotak Balasan Quote", "#reply_bar_background", isColor = true, isRadius = true)
            )
        )
    }

    // --- KONTROL BOTTOM NAVIGATION ---
    private fun showBottomNavConfig() {
        showElementConfigSheet(
            title = "Bilah Navigasi Bawah",
            items = listOf(
                ConfigItem("Warna Latar Bar Bawah", "#bottom_nav", isColor = true),
                ConfigItem("Warna Kapsul Indikator Aktif", "#bottom_nav_indicator", isColor = true, isRadius = true),
                ConfigItem("Warna Ikon & Label Tab", "#bottom_nav_item", isTextColor = true)
            )
        )
    }

    // --- KONTROL INPUT BAR ---
    private fun showInputBarConfig() {
        showElementConfigSheet(
            title = "Kotak Input & Tombol Aksi",
            items = listOf(
                ConfigItem("Kotak Ketik Pesan", "#entry", isColor = true, isRadius = true, isTextColor = true),
                ConfigItem("Tombol Kirim (Send)", "#send", isColor = true),
                ConfigItem("Tombol Mikrofon (VN)", "#voice_note_btn", isColor = true),
                ConfigItem("Tombol Lampiran (Attach)", "#input_attach_button", isTextColor = true),
                ConfigItem("Tombol Emoji", "#emoji_picker_btn", isTextColor = true)
            )
        )
    }

    // --- KONTROL WALLPAPER & LATAR ---
    private fun showWallpaperConfig() {
        showElementConfigSheet(
            title = "Latar & Wallpaper",
            items = listOf(
                ConfigItem("Warna Latar Ruang Chat", "#conversation_background", isColor = true),
                ConfigItem("Warna Latar Layar Utama", "#main_layout", isColor = true)
            ),
            extraAction = { container ->
                val btnPick = Button(this).apply {
                    text = "🖼️ Pilih Wallpaper dari Galeri / File"
                    setBackgroundColor(Color.parseColor("#2A3942"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        try {
                            startActivityForResult(intent, REQUEST_PICK_WALLPAPER)
                        } catch (_: Exception) {
                            val pickIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                            startActivityForResult(pickIntent, REQUEST_PICK_WALLPAPER)
                        }
                    }
                }
                container.addView(btnPick)
            }
        )
    }

    // --- KONTROL IKON KUSTOM (GALERI / FILE MANAGER) ---
    private fun showCustomIconsConfig() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 40)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val tvTitle = TextView(this).apply {
            text = "🎨 Kustomisasi Ikon WhatsApp"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }
        root.addView(tvTitle)

        val tvDesc = TextView(this).apply {
            text = "Pilih gambar/ikon dari Galeri atau File Manager untuk menggantikan ikon bawaan WhatsApp."
            textSize = 12f
            setTextColor(Color.parseColor("#8696A0"))
            setPadding(0, 0, 0, 20)
        }
        root.addView(tvDesc)

        data class IconTarget(val label: String, val key: String)
        val iconList = listOf(
            IconTarget("Tombol Kirim / Voice Note", "voice_note_btn"),
            IconTarget("Tombol Kamera", "camera_btn"),
            IconTarget("Tombol Lampiran (Attach)", "input_attach_button"),
            IconTarget("Tombol Kembali (Back)", "back")
        )

        iconList.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 14)
            }

            val isCustom = ThemeStateManager.customIcons.containsKey(item.key)
            val tvLabel = TextView(this).apply {
                text = item.label + (if (isCustom) " ✓ (Kustom)" else " (Bawaan)")
                textSize = 14f
                setTextColor(if (isCustom) Color.parseColor("#00F0FF") else Color.parseColor("#E9EDEF"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            row.addView(tvLabel)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 0)
            }

            val btnPick = Button(this).apply {
                text = "📁 Pilih dari Galeri / File"
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, 8, 0)
                }
                setBackgroundColor(Color.parseColor("#2A3942"))
                setTextColor(Color.parseColor("#53BDEB"))
                setOnClickListener {
                    pendingIconKey = item.key
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    try {
                        startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON)
                    } catch (_: Exception) {
                        val pickIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                        startActivityForResult(pickIntent, REQUEST_PICK_CUSTOM_ICON)
                    }
                    dialog.dismiss()
                }
            }
            btnRow.addView(btnPick)

            if (isCustom) {
                val btnReset = Button(this).apply {
                    text = "Reset"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setBackgroundColor(Color.parseColor("#3A2020"))
                    setTextColor(Color.parseColor("#FF6B6B"))
                    setOnClickListener {
                        ThemeStateManager.customIcons.remove(item.key)
                        applyLivePreview()
                        dialog.dismiss()
                        Toast.makeText(this@ThemeStudioActivity, "Ikon ${item.label} dikembalikan ke default.", Toast.LENGTH_SHORT).show()
                    }
                }
                btnRow.addView(btnReset)
            }

            row.addView(btnRow)
            root.addView(row)
        }

        val btnClose = Button(this).apply {
            text = "Tutup"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }

    // --- FITUR MOD ---
    private fun showModFeaturesConfig() {
        val dialog = BottomSheetDialog(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 50)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val title = TextView(this).apply {
            text = "Fitur Tambahan Mod Tema"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 30)
        }
        container.addView(title)

        val swHideRead = Switch(this).apply {
            text = "Paksa Sembunyikan Centang Biru (Hide Read)"
            setTextColor(Color.WHITE)
            isChecked = ThemeStateManager.hideReadEnabled
            setOnCheckedChangeListener { _, isChecked -> ThemeStateManager.hideReadEnabled = isChecked }
            setPadding(0, 10, 0, 10)
        }
        container.addView(swHideRead)

        val swAntiDelete = Switch(this).apply {
            text = "Paksa Anti-Tarik Pesan (Anti-Delete)"
            setTextColor(Color.WHITE)
            isChecked = ThemeStateManager.antiDeleteEnabled
            setOnCheckedChangeListener { _, isChecked -> ThemeStateManager.antiDeleteEnabled = isChecked }
            setPadding(0, 10, 0, 20)
        }
        container.addView(swAntiDelete)

        val btnDone = Button(this).apply {
            text = "Selesai"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            setOnClickListener { dialog.dismiss() }
        }
        container.addView(btnDone)

        dialog.setContentView(container)
        dialog.show()
    }

    // --- REUSABLE CONFIG SHEET GENERATOR ---
    private data class ConfigItem(
        val label: String,
        val cssKey: String,
        val isColor: Boolean = false,
        val isTextColor: Boolean = false,
        val isRadius: Boolean = false
    )

    private fun showElementConfigSheet(
        title: String,
        items: List<ConfigItem>,
        extraAction: ((LinearLayout) -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 40)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val tvTitle = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        root.addView(tvTitle)

        items.forEach { item ->
            val state = ThemeStateManager.getState(item.cssKey)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 16)
            }

            val tvLabel = TextView(this).apply {
                text = item.label
                textSize = 14f
                setTextColor(Color.parseColor("#E9EDEF"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            row.addView(tvLabel)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 0)
            }

            if (item.isColor) {
                val btnColor = Button(this).apply {
                    val hex = state.bgColor ?: "#Default"
                    text = "Warna Latar ($hex)"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, 8, 0)
                    }
                    setBackgroundColor(Color.parseColor("#2A3942"))
                    setTextColor(Color.parseColor("#53BDEB"))
                    setOnClickListener {
                        showColorPicker("Pilih " + item.label, state.bgColor) { chosenHex ->
                            state.bgColor = chosenHex
                            text = "Warna Latar ($chosenHex)"
                            applyLivePreview()
                        }
                    }
                }
                btnRow.addView(btnColor)
            }

            if (item.isTextColor) {
                val btnTextColor = Button(this).apply {
                    val hex = state.textColor ?: "#Default"
                    text = "Warna Teks ($hex)"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, 8, 0)
                    }
                    setBackgroundColor(Color.parseColor("#2A3942"))
                    setTextColor(Color.parseColor("#00F0FF"))
                    setOnClickListener {
                        showColorPicker("Pilih Warna Teks " + item.label, state.textColor) { chosenHex ->
                            state.textColor = chosenHex
                            text = "Warna Teks ($chosenHex)"
                            applyLivePreview()
                        }
                    }
                }
                btnRow.addView(btnTextColor)
            }

            if (item.isRadius) {
                val btnRadius = Button(this).apply {
                    val rad = state.radius ?: 0
                    text = "Radius: ${rad}px"
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setBackgroundColor(Color.parseColor("#2A3942"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        showRadiusDialog(item.label, state.radius ?: 16) { chosenRadius ->
                            state.radius = chosenRadius
                            text = "Radius: ${chosenRadius}px"
                            applyLivePreview()
                        }
                    }
                }
                btnRow.addView(btnRadius)
            }

            row.addView(btnRow)
            root.addView(row)
        }

        extraAction?.invoke(root)

        val btnClose = Button(this).apply {
            text = "Tutup & Simpan"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            setOnClickListener {
                applyLivePreview()
                dialog.dismiss()
            }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }

    // --- VISUAL COLOR PICKER DENGAN PALET PRESET ---
    private fun showColorPicker(title: String, initialHex: String?, onColorChosen: (String) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 40)
            setBackgroundColor(Color.parseColor("#1F2C34"))
        }

        val tvTitle = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        root.addView(tvTitle)

        val tvPalette = TextView(this).apply {
            text = "Pilih dari Palet Cepat:"
            textSize = 12f
            setTextColor(Color.parseColor("#8696A0"))
            setPadding(0, 4, 0, 8)
        }
        root.addView(tvPalette)

        val palette = listOf(
            "#000000", "#0B141A", "#1F2C34", "#2A3942",
            "#00A884", "#25D366", "#007AFF", "#53BDEB",
            "#00F0FF", "#7122FA", "#FF007F", "#FFD1DC",
            "#FFDEE9", "#B5FFFC", "#E9EDEF", "#FFFFFF"
        )

        for (row in 0..1) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            for (col in 0..7) {
                val colorHex = palette[row * 8 + col]
                val swatch = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 80, 1f).apply {
                        setMargins(6, 4, 6, 4)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(colorHex))
                        setStroke(2, Color.parseColor("#3A4B54"))
                    }
                    setOnClickListener {
                        onColorChosen(colorHex)
                        dialog.dismiss()
                    }
                }
                rowLayout.addView(swatch)
            }
            root.addView(rowLayout)
        }

        val tvHex = TextView(this).apply {
            text = "Atau Masukkan Kode HEX:"
            textSize = 12f
            setTextColor(Color.parseColor("#8696A0"))
            setPadding(0, 16, 0, 8)
        }
        root.addView(tvHex)

        val hexLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(50, 50).apply {
                setMargins(0, 0, 16, 0)
            }
            val initial = try { Color.parseColor(initialHex ?: "#00A884") } catch (_: Exception) { Color.parseColor("#00A884") }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(initial)
            }
        }
        hexLayout.addView(previewDot)

        val etHex = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "#RRGGBB"
            setText(initialHex ?: "")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8696A0"))
        }
        hexLayout.addView(etHex)

        val btnSelect = Button(this).apply {
            text = "Pilih"
            setBackgroundColor(Color.parseColor("#00A884"))
            setTextColor(Color.parseColor("#0B141A"))
            setOnClickListener {
                var hex = etHex.text.toString().trim()
                if (hex.isNotEmpty()) {
                    if (!hex.startsWith("#")) hex = "#$hex"
                    try {
                        Color.parseColor(hex)
                        onColorChosen(hex)
                        dialog.dismiss()
                    } catch (_: Exception) {
                        Toast.makeText(this@ThemeStudioActivity, "Kode warna HEX tidak valid!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    dialog.dismiss()
                }
            }
        }
        hexLayout.addView(btnSelect)
        root.addView(hexLayout)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun showRadiusDialog(label: String, initialRadius: Int, onRadiusChosen: (Int) -> Unit) {
        val dialog = AlertDialog.Builder(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val tvValue = TextView(this).apply {
            text = "$initialRadius px"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        container.addView(tvValue)

        val seekBar = SeekBar(this).apply {
            max = 40
            progress = initialRadius
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvValue.text = "$progress px"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(seekBar)

        dialog.setTitle("Atur Radius Kelengkungan: $label")
            .setView(container)
            .setPositiveButton("OK") { _, _ -> onRadiusChosen(seekBar.progress) }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- APPLY DIRECTLY TO WHATSAPP ---
    private fun applyThemeToWhatsApp() {
        val success = ThemeExporter.applyDirectlyToWhatsApp(this)
        if (success) {
            AlertDialog.Builder(this)
                .setTitle("⚡ Tema Berhasil Diterapkan!")
                .setMessage("Konfigurasi tema Anda langsung disimpan dan aktif.\n\nBuka atau restart WhatsApp untuk menikmati tema baru Anda sekarang?")
                .setPositiveButton("Buka WhatsApp") { _, _ ->
                    val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Aplikasi WhatsApp tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Nanti", null)
                .show()
        } else {
            Toast.makeText(this, "Gagal menerapkan tema.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MORE MENU ---
    private fun showMoreOptionsMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "📦 Ekspor Tema ke ZIP")
        popup.menu.add(0, 2, 1, "🔄 Reset ke Tema Default WhatsApp")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    ThemeExporter.exportTheme(this)
                    true
                }
                2 -> {
                    ThemeStateManager.resetAll()
                    val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                    prefs.edit()
                        .remove("custom_css")
                        .remove("css_theme")
                        .remove("folder_theme")
                        .remove("bubble_right")
                        .remove("bubble_left")
                        .remove("bubble_color")
                        .remove("wallpaper")
                        .putBoolean("custom_filters", false)
                        .apply()
                    applyLivePreview()
                    Toast.makeText(this, "Tema berhasil di-reset ke default!", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_WALLPAPER && resultCode == RESULT_OK) {
            ThemeStateManager.wallpaperUri = data?.data?.toString()
            applyLivePreview()
            Toast.makeText(this, "Wallpaper galeri berhasil dipilih!", Toast.LENGTH_SHORT).show()
        } else if (requestCode == REQUEST_PICK_CUSTOM_ICON && resultCode == RESULT_OK) {
            val uri = data?.data
            val key = pendingIconKey
            if (uri != null && key != null) {
                try {
                    val iconsDir = File(App.RhpatchFolder, "icons")
                    if (!iconsDir.exists()) iconsDir.mkdirs()
                    val iconFile = File(iconsDir, "${key}.png")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(iconFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ThemeStateManager.customIcons[key] = iconFile.absolutePath
                    applyLivePreview()
                    Toast.makeText(this, "Ikon kustom berhasil dipasang!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal memuat ikon: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


