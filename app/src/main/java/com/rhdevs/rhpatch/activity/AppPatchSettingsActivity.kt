@file:Suppress("DEPRECATION")

package com.rhdevs.rhpatch.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.rhdevs.rhpatch.appPatchConfigurations
import com.wmods.wppenhacer.R

class AppPatchSettingsActivity : Activity() {

    companion object {
        const val ARGUMENT_APP_NAME = "app_name_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_patch_settings)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        val appName = intent.getStringExtra(ARGUMENT_APP_NAME)
        actionBar?.title = appName

        if (savedInstanceState != null) return
        val fragment = AppPatchSettingsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_APP_NAME, appName)
            }
        }
        fragmentManager.beginTransaction()
            .replace(R.id.app_patch_settings_container, fragment)
            .commit()

        if (appName == "YouTube" || appName == "Instagram") {
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("ytdlnis_prompt_shown", false)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Rekomendasi Paket Pendukung")
                    .setMessage("Untuk mendukung pengunduhan media eksternal (terutama dari YouTube/Instagram), sangat disarankan untuk menginstall YTDLnis.\n\nApakah Anda ingin mengunduhnya sekarang?")
                    .setPositiveButton("Unduh") { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deniscerri/ytdlnis/releases")))
                        prefs.edit().putBoolean("ytdlnis_prompt_shown", true).apply()
                    }
                    .setNegativeButton("Nanti") { _, _ ->
                        prefs.edit().putBoolean("ytdlnis_prompt_shown", true).apply()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("WorldReadableFiles")
    class AppPatchSettingsFragment : PreferenceFragment() {

        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Retrieve appName from the Activity's Intent extras
            val appName = arguments?.getString(ARGUMENT_APP_NAME)
            val appPatchInfo = appPatchConfigurations.find { it.appName == appName }
            if (appPatchInfo == null) throw Exception("AppPatchInfo not found, app_name: $appName")
            val defaultPatchStates = appPatchInfo.patches.associate { it.name to it.use }

            val screen = preferenceManager.createPreferenceScreen(context)
            /** XSharedPreference
             * @see com.rhdevs.rhpatch.PatchExecutor.patchPreferences */
            preferenceManager.sharedPreferencesMode = MODE_PRIVATE
            preferenceManager.sharedPreferencesName = appPatchInfo.packageName

            object : Preference(context) {
                @Deprecated("Deprecated in Java")
                override fun onBindView(view: View) {
                    super.onBindView(view)
                    view.findViewById<Button>(R.id.button_default).setOnClickListener {
                        restoreDefaultPreferences(defaultPatchStates)
                    }
                    view.findViewById<Button>(R.id.button_none).setOnClickListener {
                        setAllPreferences(false)
                    }
                    val isInstalled = runCatching {
                        context.packageManager.getPackageInfo(appPatchInfo.packageName, 0)
                    }.isSuccess

                    view.findViewById<Button>(R.id.button_app_info).apply {
                        if (!isInstalled) visibility = View.GONE
                        setOnClickListener {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${appPatchInfo.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                }
            }.apply {
                layoutResource = R.layout.preference_header_buttons
                screen.addPreference(this)
            }

            for (patchInfo in appPatchInfo.patches.sortedBy { it.name }) {
                if (patchInfo.name == "") continue
                if (patchInfo.name.startsWith("<")) continue
                CheckBoxPreference(context).apply {
                    /** XSharedPreference
                     * @see com.rhdevs.rhpatch.PatchExecutor.applyPatches */
                    key = patchInfo.name // Pref Key
                    title = patchInfo.name
                    summary = patchInfo.description
                    setDefaultValue(patchInfo.use)
                    setOnPreferenceChangeListener { _, _ ->
                        val vibrator =
                            context.getSystemService(VIBRATOR_SERVICE) as Vibrator?
                        if (vibrator?.hasVibrator() == true) {
                            try {
                                vibrator.vibrate(50)
                            } catch (_: Exception) {}
                        }
                        true
                    }
                    screen.addPreference(this)
                }
            }

            preferenceScreen = screen
        }

        fun setAllPreferences(enable: Boolean) {
            if (!isAdded) return
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked = enable
                }
            }
        }

        fun restoreDefaultPreferences(defaultPatchStates: Map<String, Boolean>) {
            if (!isAdded) return
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked = defaultPatchStates[preference.key] ?: preference.isChecked
                }
            }
        }

        override fun onPause() {
            super.onPause()
            runCatching {
                val appName = arguments?.getString(ARGUMENT_APP_NAME)
                val appPatchInfo = appPatchConfigurations.find { it.appName == appName }
                if (appPatchInfo != null) {
                    val file = java.io.File(context.filesDir.parentFile, "shared_prefs/${appPatchInfo.packageName}.xml")
                    if (file.exists()) {
                        file.setReadable(true, false)
                    }
                }
            }
        }
    }
}
