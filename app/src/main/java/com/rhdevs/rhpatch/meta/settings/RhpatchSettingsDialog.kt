package com.rhdevs.rhpatch.meta.settings

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

object RhpatchSettingsDialog {

    fun showSettingsDialog(context: Context) {
        val dp = context.resources.displayMetrics.density
        val isIndo = java.util.Locale.getDefault().language == "in" || java.util.Locale.getDefault().language == "id"

        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212")) // Dark theme background
            setPadding((16 * dp).toInt(), (24 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt())
        }

        // Title
        val title = TextView(context).apply {
            text = "Rhpatch settings"
            textSize = 24f
            setTextColor(Color.parseColor("#BB86FC")) // Purple accent
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (24 * dp).toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(title)

        fun createCard(): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val shape = GradientDrawable().apply {
                    cornerRadius = 16 * dp
                    setColor(Color.parseColor("#1E1E1E"))
                }
                background = shape
                setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (12 * dp).toInt())
                }
            }
        }

        fun addSwitchToCard(card: LinearLayout, titleStr: String, descStr: String, key: String, defaultVal: Boolean = true) {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            }
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 16f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val itemDesc = TextView(context).apply { text = descStr; textSize = 13f; setTextColor(Color.parseColor("#B0B0B0")); setPadding(0, (4 * dp).toInt(), 0, 0) }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)
            
            val toggle = Switch(context).apply {
                isChecked = prefs.getBoolean(key, defaultVal)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                    Toast.makeText(context, if (isChecked) " AKTIF" else " MATI", Toast.LENGTH_SHORT).show()
                }
            }
            
            itemLayout.addView(textLayout)
            itemLayout.addView(toggle)
            card.addView(itemLayout)
        }

        fun addInputToCard(card: LinearLayout, titleStr: String, descStr: String, key: String, defaultVal: String = "") {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            }
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 16f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val itemDesc = TextView(context).apply { text = descStr; textSize = 13f; setTextColor(Color.parseColor("#B0B0B0")); setPadding(0, (4 * dp).toInt(), 0, (8 * dp).toInt()) }
            
            val input = EditText(context).apply {
                setText(prefs.getString(key, defaultVal))
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                hint = "/storage/emulated/0/Download/Rhpatch"
                background = GradientDrawable().apply {
                    cornerRadius = 8 * dp
                    setColor(Color.parseColor("#2C2C2C"))
                    setStroke((1 * dp).toInt(), Color.parseColor("#333333"))
                }
                setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            
            val saveBtn = TextView(context).apply {
                text = "Rhpatch settings"
                textSize = 14f
                setTextColor(Color.parseColor("#BB86FC"))
                gravity = Gravity.END
                setPadding(0, (8 * dp).toInt(), 0, 0)
                setOnClickListener {
                    prefs.edit().putString(key, input.text.toString()).apply()
                    Toast.makeText(context, "Path Disimpan", Toast.LENGTH_SHORT).show()
                }
            }
            
            itemLayout.addView(itemTitle)
            itemLayout.addView(itemDesc)
            itemLayout.addView(input)
            itemLayout.addView(saveBtn)
            card.addView(itemLayout)
        }

        val card1 = createCard()
        addSwitchToCard(card1, if (isIndo) "Mode Hantu" else "Ghost Mode", if (isIndo) "Baca DM tanpa ketahuan" else "Read DMs secretly", "pref_ghost_mode")
        addSwitchToCard(card1, if (isIndo) "Mode Hantu: Saluran OFF" else "Ghost Mode: Channels OFF", if (isIndo) "Matikan Ghost Mode di Saluran" else "Disable Ghost Mode in Channels", "pref_ghost_mode_channels_off")
        rootLayout.addView(card1)

        val card2 = createCard()
        addSwitchToCard(card2, if (isIndo) "Unduh Media" else "Download Media", if (isIndo) "Tombol unduh di Feed & Reels" else "Download button in Feed & Reels", "pref_downloader")
        addInputToCard(card2, if (isIndo) "Lokasi Unduhan" else "Download Path", if (isIndo) "Tentukan folder tempat menyimpan hasil download" else "Set folder to save downloaded media", "custom_download_path")
        addSwitchToCard(card2, if (isIndo) "Salin Komentar" else "Copy Comments", if (isIndo) "Tahan komentar untuk menyalin" else "Long press comments to copy", "pref_copy_comments")
        addSwitchToCard(card2, if (isIndo) "Sembunyikan Saran Pengguna" else "Hide Suggested Users", if (isIndo) "Sembunyikan 'Mungkin Anda Kenal'" else "Hide 'Suggested for you'", "pref_hide_suggested_users")
        rootLayout.addView(card2)
        
        val card3 = createCard()
        addSwitchToCard(card3, if (isIndo) "Matikan Iklan" else "Disable Ads", if (isIndo) "Sembunyikan postingan bersponsor" else "Hide sponsored posts", "pref_disable_ads")
        addSwitchToCard(card3, if (isIndo) "Matikan Putar Otomatis" else "Disable Autoplay", if (isIndo) "Matikan video otomatis di Feed" else "Disable video autoplay", "pref_disable_video_autoplay")
        addSwitchToCard(card3, if (isIndo) "Buka Kunci IG Plus" else "Unlock IG Plus", if (isIndo) "Buka fitur Creator Plus" else "Unlock Creator Plus features", "pref_ig_plus")
        addSwitchToCard(card3, if (isIndo) "Toast Debug" else "Toast Debug", if (isIndo) "Tampilkan log/pesan toast saat proses download/patch" else "Show toast log messages", "pref_toast_debug", false)
        rootLayout.addView(card3)

        val closeBtn = TextView(context).apply {
            text = "Rhpatch settings"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
            val shape = GradientDrawable().apply {
                cornerRadius = 24 * dp
                setColor(Color.parseColor("#BB86FC")) // Purple accent button
            }
            background = shape
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, (8 * dp).toInt(), 0, 0)
            }
        }
        rootLayout.addView(closeBtn)

        val scrollView = ScrollView(context).apply {
            addView(rootLayout)
        }

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setView(scrollView)
            .show()
            
        closeBtn.setOnClickListener {
            dialog.dismiss()
        }
    }
}
