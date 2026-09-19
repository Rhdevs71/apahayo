package com.rhdevs.rhpatch.meta.settings

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.XposedBridge

object RhpatchSettingsDialog {
    fun showSettingsDialog(context: Context) {
        val dp = context.resources.displayMetrics.density

        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val bgColor = if (isDarkMode) Color.parseColor("#0F172A") else Color.parseColor("#F8FAFC")
        val textColor = if (isDarkMode) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val subTextColor = if (isDarkMode) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
        val primaryColor = Color.parseColor("#0D9488") 

        val scrollView = ScrollView(context).apply {
            setPadding(0, 0, 0, 0)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt())
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 24f * dp
            }
        }

        val title = TextView(context).apply {
            text = "Rhpatch Menu"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, 0, 0, (20 * dp).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        layout.addView(title)

        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)

        fun createSwitch(titleStr: String, descStr: String, key: String, defaultVal: Boolean = false): LinearLayout {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
                gravity = Gravity.CENTER_VERTICAL
            }
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 16f; setTextColor(textColor); setTypeface(null, android.graphics.Typeface.BOLD) }
            val itemDesc = TextView(context).apply { text = descStr; textSize = 13f; setTextColor(subTextColor); setPadding(0, (4 * dp).toInt(), 0, 0) }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)

            val switch = Switch(context).apply {
                isChecked = prefs.getBoolean(key, defaultVal)
                val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
                val trackColors = intArrayOf(primaryColor, Color.parseColor("#CBD5E1"))
                val thumbColors = intArrayOf(Color.WHITE, Color.parseColor("#94A3B8"))
                trackTintList = ColorStateList(states, trackColors)
                thumbTintList = ColorStateList(states, thumbColors)

                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                }
            }
            itemLayout.addView(textLayout)
            itemLayout.addView(switch)
            return itemLayout
        }

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
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 16f; setTextColor(textColor); setTypeface(null, android.graphics.Typeface.BOLD) }
            val itemDesc = TextView(context).apply {
                val currentIndex = prefs.getInt(key, defaultIndex)
                text = "$descStr\nSaat ini: ${options.getOrElse(currentIndex) { options[0] }}"
                textSize = 13f; setTextColor(subTextColor); setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)
            
            itemLayout.addView(textLayout)
            itemLayout.setOnClickListener {
                val currentIndex = prefs.getInt(key, defaultIndex)
                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(titleStr)
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
            val itemTitle = TextView(context).apply { text = titleStr; textSize = 16f; setTextColor(textColor); setTypeface(null, android.graphics.Typeface.BOLD) }
            val itemDesc = TextView(context).apply {
                val currentVal = prefs.getString(key, defaultVal)
                text = "$descStr\nSaat ini: $currentVal"
                textSize = 13f; setTextColor(subTextColor); setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            textLayout.addView(itemTitle)
            textLayout.addView(itemDesc)
            
            itemLayout.addView(textLayout)
            itemLayout.setOnClickListener {
                val currentVal = prefs.getString(key, defaultVal)
                val input = android.widget.EditText(context).apply {
                    setText(currentVal)
                    setTextColor(textColor)
                }
                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(titleStr)
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

        layout.addView(createSwitch("Ghost Mode", "Sembunyikan status dilihat pada DM dan Story", "pref_ghost_mode", false))
        layout.addView(createSwitch("Tandai Sebagai Dibaca", "Tambahkan tombol di header DM untuk menandai obrolan telah dibaca (Ghost Mode)", "pref_mark_as_read", false))
        layout.addView(createSwitch("Disable Typing Status", "Sembunyikan status sedang mengetik di DM", "pref_disable_typing", false))
        layout.addView(createSwitch("Make Ephemeral Permanent", "Ubah pesan View Once menjadi permanen", "pref_ephemeral", false))
        layout.addView(createSwitch("View Live Anonymously", "Tonton Live tanpa diketahui host atau penonton lain", "pref_view_live_anon", false))
        
        // Dua toggle mandiri untuk Downloader sesuai instruksi
        layout.addView(createSwitch("Media Downloader (Feed & Story)", "Aktifkan tombol download pada postingan Feed dan Story", "pref_downloader", true))
        layout.addView(createSwitch("Reels Downloader (HD)", "Aktifkan tombol download khusus video Reels HD", "pref_reels_downloader", true))
        
        layout.addView(createSwitch("Salin Komentar (Global)", "Ketuk lama pada komentar untuk menyalin teks", "pref_copy_comments", false))
        layout.addView(createSwitch("Disable Swipe To Create", "Mencegah buka kamera saat swipe kanan di Beranda", "pref_disable_swipe", false))
        layout.addView(createSwitch("Disable Video Autoplay", "Mematikan putar otomatis video di Feed", "pref_disable_video_autoplay", false))
        layout.addView(createSwitch("Disable Stories Audio Autoplay", "Mematikan audio otomatis di Story", "pref_disable_stories_audio", false))
        layout.addView(createSwitch("Unlock IG Plus", "Membuka kunci fitur berlangganan Creator Plus", "pref_ig_plus", true))
        layout.addView(createSwitch("Disable Double Tap Like", "Matikan fungsi 2 kali ketuk untuk like", "pref_disable_double_tap_like", false))
        
        layout.addView(createSwitch("Buka Tautan Secara Eksternal", "Buka link web langsung di browser sistem (Chrome/dll)", "pref_open_links_externally", false))
        layout.addView(createSwitch("Aktifkan Mode Pengembang", "Tampilkan opsi Developer IG (Ketuk lama Home)", "pref_enable_dev_options", false))

        val devOptionsBtn = TextView(context).apply {
            text = "[Dev] Buka Menu Developer Options Sekarang"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#3B82F6"))
            setPadding(0, (8 * dp).toInt(), 0, (12 * dp).toInt())
            setOnClickListener {
                val act = context as? android.app.Activity
                if (act != null) {
                    com.rhdevs.rhpatch.meta.misc.launchInstagramDeveloperOptions(act)
                } else {
                    android.widget.Toast.makeText(context, "Silakan buka dari dalam layar Instagram", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(devOptionsBtn)
        val changeIconBtn = TextView(context).apply {
            text = "[Icon] Pilih Ikon Aplikasi Instagram (14 Ikon)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#EC4899"))
            setPadding(0, (8 * dp).toInt(), 0, (12 * dp).toInt())
            setOnClickListener {
                val iconNames = arrayOf(
                    "Default (Standar)",
                    "Cannes Neon",
                    "Cannes Flame",
                    "Cannes Floral",
                    "Cannes Slime",
                    "Cannes Metal",
                    "Cannes K-Pop",
                    "Haruko",
                    "Felipe",
                    "Humberto",
                    "Zipeng",
                    "Uzo",
                    "Ricky",
                    "Throwback Classic"
                )
                val iconAliases = arrayOf(
                    "com.instagram.android.activity.MainTabActivity",
                    "com.instagram.android.activity.MainTabActivity.neon",
                    "com.instagram.android.activity.MainTabActivity.flame",
                    "com.instagram.android.activity.MainTabActivity.floral",
                    "com.instagram.android.activity.MainTabActivity.slime",
                    "com.instagram.android.activity.MainTabActivity.metal",
                    "com.instagram.android.activity.MainTabActivity.kpop",
                    "com.instagram.android.activity.MainTabActivity.haruko",
                    "com.instagram.android.activity.MainTabActivity.felipe",
                    "com.instagram.android.activity.MainTabActivity.humberto",
                    "com.instagram.android.activity.MainTabActivity.zipeng",
                    "com.instagram.android.activity.MainTabActivity.uzo",
                    "com.instagram.android.activity.MainTabActivity.ricky",
                    "com.instagram.android.activity.MainTabActivity.throwback"
                )

                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Pilih Ikon Aplikasi Instagram")
                    .setItems(iconNames) { _, which ->
                        val selectedAlias = iconAliases[which]
                        val name = iconNames[which]
                        try {
                            com.rhdevs.rhpatch.meta.misc.applyLauncherIcon(context, selectedAlias)
                            Toast.makeText(context, "Ikon berhasil diubah ke: $name!\n(Muat ulang launcher HP Anda jika belum berubah)", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal mengubah ikon: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
        layout.addView(changeIconBtn)
        layout.addView(createSwitch("Hapus Ruang Kosong Bawah", "Hilangkan space kosong di bawah layar (aktifkan jika belum dipatch di APK)", "pref_remove_empty_bottom", false))
                
        val likeAnimations = arrayOf("DEFAULT", "RINGS", "PRIDE", "SPARKLES")
        layout.addView(createOptionSelector("Ubah Animasi Suka", "Pilih animasi love kustom", "pref_like_animation_type", likeAnimations, 0))
        layout.addView(createSwitch("Indikator Pertemanan Berwarna", "Beri warna pada tombol Follow, Following, dan Follow Back di profil", "pref_colored_friendship", false))

        layout.addView(createSwitch("Simpan Komentar Media", "Izinkan penyimpanan GIF dan stiker gambar dari komentar", "pref_media_comments", false))

        val debugTitle = TextView(context).apply {
            text = "Pelacak Hook & Fallbacks"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#F59E0B"))
            setPadding(0, (24 * dp).toInt(), 0, (8 * dp).toInt())
        }
        layout.addView(debugTitle)

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
