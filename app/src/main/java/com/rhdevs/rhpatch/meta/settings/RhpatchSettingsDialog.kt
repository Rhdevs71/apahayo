package com.rhdevs.rhpatch.meta.settings

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.widget.Toast
import android.app.AlertDialog
import android.view.ViewGroup

object RhpatchSettingsDialog {

    fun showSettingsDialog(context: Context) {
        val dp = context.resources.displayMetrics.density
        
        val scrollView = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#121212")) // Dark mode IG
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        // Title
        val title = TextView(context).apply {
            text = "✨ Rhpatch Settings (Rhpatch Port)"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (24 * dp).toInt())
        }
        layout.addView(title)

        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)

        fun createSwitch(titleStr: String, descStr: String, key: String, defaultVal: Boolean = true): LinearLayout {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
                gravity = Gravity.CENTER_VERTICAL
            }

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val itemTitle = TextView(context).apply {
                text = titleStr
                textSize = 18f
                setTextColor(Color.WHITE)
            }
            
            val itemDesc = TextView(context).apply {
                text = descStr
                textSize = 14f
                setTextColor(Color.parseColor("#A0A0A0"))
                setPadding(0, (4 * dp).toInt(), 0, 0)
            }

            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)

            val toggle = Switch(context).apply {
                isChecked = prefs.getBoolean(key, defaultVal)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                    Toast.makeText(context, "$titleStr ${if(isChecked) "Diaktifkan" else "Dimatikan"}\nRestart IG untuk menerapkan", Toast.LENGTH_SHORT).show()
                }
            }

            itemLayout.addView(textLayout)
            itemLayout.addView(toggle)
            
            return itemLayout
        }

        layout.addView(createSwitch("Ghost Mode", "Sembunyikan status dilihat pada DM dan Stories", "pref_ghost_mode"))
        layout.addView(createSwitch("Disable Typing Status", "Sembunyikan status sedang mengetik di DM", "pref_disable_typing"))
        layout.addView(createSwitch("Make Ephemeral Permanent", "Ubah pesan View Once menjadi permanen", "pref_ephemeral"))
        layout.addView(createSwitch("View Live Anonymously", "Tonton Live tanpa diketahui host atau penonton lain", "pref_view_live_anon"))
        layout.addView(createSwitch("Media Downloader", "Aktifkan tombol download pada Feed, Reels, dan Stories", "pref_downloader"))
        layout.addView(createSwitch("Copy Comments", "Tahan lama (Long Press) komentar untuk menyalin", "pref_copy_comments"))
        layout.addView(createSwitch("Disable Swipe To Create", "Mencegah buka kamera saat swipe kanan di Beranda", "pref_disable_swipe"))
        layout.addView(createSwitch("Disable Video Autoplay", "Mematikan putar otomatis video di Feed", "pref_disable_video_autoplay"))
        layout.addView(createSwitch("Disable Stories Audio Autoplay", "Mematikan audio otomatis di Story", "pref_disable_stories_audio"))
        layout.addView(createSwitch("Unlock IG Plus", "Membuka kunci fitur berlangganan Creator Plus", "pref_ig_plus"))
        layout.addView(createSwitch("Disable Double Tap Like", "Matikan fungsi 2 kali ketuk untuk like", "pref_disable_double_tap_like"))

        // --- SECTION: DEBUG & FALLBACKS ---
        val debugTitle = TextView(context).apply {
            text = "🔬 Pelacak Hook & Fallbacks"
            textSize = 18f
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, (24 * dp).toInt(), 0, (8 * dp).toInt())
        }
        layout.addView(debugTitle)

        fun createOptionSelector(titleStr: String, descStr: String, key: String, options: Array<String>, defaultIndex: Int = 0): LinearLayout {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
                gravity = Gravity.CENTER_VERTICAL
            }
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 18f; setTextColor(Color.WHITE) }
            val itemDesc = TextView(context).apply {
                val currentIndex = prefs.getInt(key, defaultIndex)
                text = "$descStr\nSaat ini: ${options.getOrElse(currentIndex) { options[0] }}"
                textSize = 14f; setTextColor(Color.parseColor("#A0A0A0")); setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)
            
            itemLayout.addView(textLayout)
            itemLayout.setOnClickListener {
                val currentIndex = prefs.getInt(key, defaultIndex)
                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Pilih $titleStr")
                    .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                        prefs.edit().putInt(key, which).apply()
                        itemDesc.text = "$descStr\nSaat ini: ${options[which]}"
                        Toast.makeText(context, "Disimpan! Restart IG untuk menerapkan.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .show()
            }
            return itemLayout
        }

        fun createTextInputOption(titleStr: String, descStr: String, key: String, defaultVal: String): LinearLayout {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
                gravity = Gravity.CENTER_VERTICAL
            }
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 18f; setTextColor(Color.WHITE) }
            val itemDesc = TextView(context).apply {
                val currentVal = prefs.getString(key, defaultVal)
                text = "$descStr\nSaat ini: $currentVal"
                textSize = 14f; setTextColor(Color.parseColor("#A0A0A0")); setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)
            
            itemLayout.addView(textLayout)
            itemLayout.setOnClickListener {
                val currentVal = prefs.getString(key, defaultVal)
                val input = android.widget.EditText(context).apply {
                    setText(currentVal)
                    setTextColor(Color.WHITE)
                }
                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Atur $titleStr")
                    .setView(input)
                    .setPositiveButton("Simpan") { dialog, _ ->
                        val newVal = input.text.toString()
                        prefs.edit().putString(key, newVal).apply()
                        itemDesc.text = "$descStr\nSaat ini: $newVal"
                        Toast.makeText(context, "Disimpan!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            return itemLayout
        }

        layout.addView(createTextInputOption("Download Path", "Lokasi folder penyimpanan (dalam folder Download)", "pref_download_path", "Rhpatch"))
        layout.addView(createSwitch("Hook Tracker (Toast)", "Munculkan peringatan Toast saat fungsi disadap. Berguna untuk mencari tahu hook mana yang jalan.", "pref_hook_tracker", false))
        layout.addView(createSwitch("Ghost Mode: Saluran OFF", "Matikan Ghost Mode pada Broadcast Channels untuk menghindari bug joining", "pref_ghost_mode_channels_off", true))
        
        layout.addView(createSwitch("Hide Suggested Users", "Sembunyikan deretan akun/profil yang disarankan (Mungkin Anda Kenal) di feed", "pref_hide_suggested_users", true))
        
        scrollView.addView(layout)

        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(scrollView)
            .setPositiveButton("Tutup") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
