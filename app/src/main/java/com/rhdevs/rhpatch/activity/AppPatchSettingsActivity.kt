@file:Suppress("DEPRECATION")

package com.rhdevs.rhpatch.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.AppPatchInfo
import com.rhdevs.rhpatch.Patch
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.activities.base.BaseActivity
import com.rhdevs.rhpatch.appPatchConfigurations
import java.io.File

class AppPatchSettingsActivity : BaseActivity() {

    companion object {
        const val ARGUMENT_APP_NAME = "app_name_key"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var appPatchInfo: AppPatchInfo
    private lateinit var adapter: PatchAdapter
    private var allPatches = listOf<Patch>()
    private var displayedPatches = mutableListOf<Patch>()

    override fun attachBaseContext(newBase: Context) {
        val localized = App.changeLanguage(newBase)
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_patch_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val appName = intent.getStringExtra(ARGUMENT_APP_NAME) ?: "Modul"
        supportActionBar?.title = appName
        supportActionBar?.subtitle = "Konfigurasi Patch & Fitur"
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val foundInfo = appPatchConfigurations.find { it.appName == appName }
        if (foundInfo == null) {
            finish()
            return
        }
        appPatchInfo = foundInfo
        prefs = getSharedPreferences(appPatchInfo.packageName, MODE_PRIVATE)

        allPatches = appPatchInfo.patches
            .filter { it.name.isNotEmpty() && !it.name.startsWith("<") }
            .sortedBy { it.name }

        displayedPatches = allPatches.toMutableList()

        val recyclerView = findViewById<RecyclerView>(R.id.rv_patches)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PatchAdapter(displayedPatches, prefs, this)
        recyclerView.adapter = adapter

        // Setup Header Action Buttons
        val btnDefault = findViewById<MaterialButton>(R.id.btn_default)
        val btnAll = findViewById<MaterialButton>(R.id.btn_all)
        val btnNone = findViewById<MaterialButton>(R.id.btn_none)
        val btnAppInfo = findViewById<MaterialButton>(R.id.btn_app_info)
        val etSearch = findViewById<EditText>(R.id.et_search_patch)

        val isInstalled = runCatching {
            packageManager.getPackageInfo(appPatchInfo.packageName, 0)
        }.isSuccess

        if (!isInstalled) {
            btnAppInfo.visibility = View.GONE
        } else {
            btnAppInfo.setOnClickListener {
                runCatching {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${appPatchInfo.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }

        btnDefault.setOnClickListener {
            val editor = prefs.edit()
            for (patch in allPatches) {
                editor.putBoolean(patch.name, patch.use)
            }
            editor.apply()
            vibrateFeedback()
            adapter.notifyDataSetChanged()
        }

        btnAll.setOnClickListener {
            val editor = prefs.edit()
            for (patch in allPatches) {
                editor.putBoolean(patch.name, true)
            }
            editor.apply()
            vibrateFeedback()
            adapter.notifyDataSetChanged()
        }

        btnNone.setOnClickListener {
            val editor = prefs.edit()
            for (patch in allPatches) {
                editor.putBoolean(patch.name, false)
            }
            editor.apply()
            vibrateFeedback()
            adapter.notifyDataSetChanged()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                displayedPatches.clear()
                if (query.isEmpty()) {
                    displayedPatches.addAll(allPatches)
                } else {
                    displayedPatches.addAll(allPatches.filter {
                        it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
                    })
                }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (appName == "YouTube" || appName == "Instagram") {
            val appPrefs = getSharedPreferences("prefs", MODE_PRIVATE)
            if (!appPrefs.getBoolean("ytdlnis_prompt_shown", false)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Rekomendasi Paket Pendukung")
                    .setMessage("Untuk mendukung pengunduhan media eksternal (terutama dari YouTube/Instagram), sangat disarankan untuk menginstall YTDLnis.\n\nApakah Anda ingin mengunduhnya sekarang?")
                    .setPositiveButton("Unduh") { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deniscerri/ytdlnis/releases")))
                        appPrefs.edit().putBoolean("ytdlnis_prompt_shown", true).apply()
                    }
                    .setNegativeButton("Nanti") { _, _ ->
                        appPrefs.edit().putBoolean("ytdlnis_prompt_shown", true).apply()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun vibrateFeedback() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(40)
            } catch (_: Exception) {}
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            val file = File(filesDir.parentFile, "shared_prefs/${appPatchInfo.packageName}.xml")
            if (file.exists()) {
                file.setReadable(true, false)
            }
        }
    }

    class PatchAdapter(
        private val patches: List<Patch>,
        private val prefs: SharedPreferences,
        private val context: Context
    ) : RecyclerView.Adapter<PatchAdapter.PatchViewHolder>() {

        private val vibrator = context.getSystemService(VIBRATOR_SERVICE) as? Vibrator

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patch_card, parent, false)
            return PatchViewHolder(view)
        }

        override fun onBindViewHolder(holder: PatchViewHolder, position: Int) {
            val patch = patches[position]
            holder.tvName.text = patch.name
            holder.tvDesc.text = if (patch.description.isNotEmpty()) patch.description else "Fitur patch untuk ${patch.name}"

            val isChecked = prefs.getBoolean(patch.name, patch.use)
            holder.switchPatch.setOnCheckedChangeListener(null)
            holder.switchPatch.isChecked = isChecked

            holder.switchPatch.setOnCheckedChangeListener { _, checked ->
                if (vibrator?.hasVibrator() == true) {
                    try {
                        vibrator.vibrate(40)
                    } catch (_: Exception) {}
                }
                prefs.edit().putBoolean(patch.name, checked).apply()
            }

            holder.itemView.setOnClickListener {
                holder.switchPatch.isChecked = !holder.switchPatch.isChecked
            }
        }

        override fun getItemCount(): Int = patches.size

        class PatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_patch_name)
            val tvDesc: TextView = view.findViewById(R.id.tv_patch_description)
            val switchPatch: MaterialSwitch = view.findViewById(R.id.switch_patch)
            val imgIcon: ImageView = view.findViewById(R.id.img_patch_icon)
        }
    }
}
