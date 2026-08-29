@file:Suppress("DEPRECATION") @file:SuppressLint("WorldReadableFiles")
package com.rhdevs.rhpatch.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.AppPatchInfo
import com.rhdevs.rhpatch.BuildConfig
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.adapter.LogLineAdapter
import com.rhdevs.rhpatch.appPatchConfigurations
import com.rhdevs.rhpatch.common.UpdateChecker
import com.rhdevs.rhpatch.databinding.DialogDiagnosticsLogBinding
import com.rhdevs.rhpatch.utils.RootDiagnostics
import com.rhdevs.rhpatch.youtube.extension.shared.Utils
import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.about.MorpheAboutPreference
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {
    private lateinit var aboutPreference: MorpheAboutPreference

    companion object {
        @JvmStatic
        fun isModuleActive(): Boolean {
            return System.getProperty("rhpatch.active") == "true"
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val localized = App.changeLanguage(newBase)
        super.attachBaseContext(localized)
    }

    override fun onResume() {
        super.onResume()
        com.rhdevs.rhpatch.utils.PermissionWizardHelper.checkAndRequestPermissions(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBackPressed()
            }
        }
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide() // We use our own header!

        Utils.setContext(this)
        aboutPreference = MorpheAboutPreference(this).apply {
            setTitle(R.string.about_title)
        }

        // Check module active status
        val badge = findViewById<TextView>(R.id.active_status_badge)
        if (isModuleActive()) {
            badge.text = getString(R.string.module_status_active)
            badge.setTextColor(Color.parseColor("#10B981")) // Green-500
        } else {
            badge.text = getString(R.string.module_status_inactive)
            badge.setTextColor(Color.parseColor("#EF4444")) // Red-500
        }

        setupTabs()

        badge.post {
            if (savedInstanceState == null) {
                switchFragment(OverviewFragment())
            }
        }
    }

    private fun setupTabs() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> switchFragment(OverviewFragment())
                R.id.nav_scheduler -> {
                    startActivity(Intent(this, SchedulerDashboardActivity::class.java))
                    false // Don't select the tab visually since it opens a new activity
                }
                R.id.nav_modules -> switchFragment(ModulesFragment())
                R.id.nav_about -> switchFragment(AboutFragment())
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.settings_container, fragment)
            .commit()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.xp_settings_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val aliasName = ComponentName(this, SettingsActivity::class.java.name + "Alias")
        menu.findItem(R.id.menu_hide_icon).isChecked =
            packageManager.getComponentEnabledSetting(aliasName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        try {
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            menu.findItem(R.id.menu_disable_auto_check).isChecked =
                prefs.getBoolean("disable_auto_check_update", false)
        } catch (_: Exception) {
            menu.findItem(R.id.menu_disable_auto_check).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                aboutPreference.onPreferenceClickListener?.onPreferenceClick(aboutPreference)
                true
            }
            R.id.menu_hide_icon -> {
                val newChecked = !item.isChecked
                item.isChecked = newChecked
                val aliasName = ComponentName(this, SettingsActivity::class.java.name + "Alias")
                val status = if (newChecked) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                             else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                packageManager.setComponentEnabledSetting(aliasName, status, PackageManager.DONT_KILL_APP)
                true
            }
            R.id.menu_disable_auto_check -> {
                val newChecked = !item.isChecked
                item.isChecked = newChecked
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putBoolean("disable_auto_check_update", newChecked).apply()
                // Set file readable so module can check it
                runCatching {
                    val file = File(filesDir.parentFile, "shared_prefs/prefs.xml")
                    if (file.exists()) {
                        file.setReadable(true, false)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAndRemoveTask()
        exitProcess(0)
    }

    class OverviewFragment : Fragment() {
        private lateinit var exportLauncher: ActivityResultLauncher<String>
        private lateinit var importLauncher: ActivityResultLauncher<Array<String>>

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Lifecycle-safe registration for document creation (Export)
            exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) {
                    performGlobalExport(uri)
                }
            }

            // Lifecycle-safe registration for document opening (Import)
            importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    performGlobalImport(uri)
                }
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_overview, container, false)
            val context = requireContext()
            
            // Set active status
            val statusText = view.findViewById<TextView>(R.id.card_active_status)
            val descText = view.findViewById<TextView>(R.id.status_description)
            if (isModuleActive()) {
                statusText.text = getString(R.string.module_status_active)
                statusText.setTextColor(Color.parseColor("#10B981"))
                descText.text = getString(R.string.module_status_active_desc)
            } else {
                statusText.text = getString(R.string.module_status_inactive)
                statusText.setTextColor(Color.parseColor("#EF4444"))
                descText.text = getString(R.string.module_status_inactive_desc)
            }

            // Version info
            val versionInfo = view.findViewById<TextView>(R.id.text_version_info)
            val buildDateStr = DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)
            versionInfo.text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})\nBuild: $buildDateStr"

            // Core Modules Click Listeners
            view.findViewById<View>(R.id.btn_feat_whatsapp)?.setOnClickListener {
                startActivity(Intent(context, com.rhdevs.rhpatch.activities.MainActivity::class.java))
            }
                        view.findViewById<View>(R.id.btn_feat_youtube)?.setOnClickListener {
                val intent = Intent(context, AppPatchSettingsActivity::class.java).apply {
                    putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, "YouTube")
                }
                startActivity(intent)
            }
            view.findViewById<View>(R.id.btn_feat_tiktok)?.setOnClickListener {
                startActivity(Intent(context, TikTokSettingsActivity::class.java))
            }
            view.findViewById<View>(R.id.btn_feat_instagram)?.setOnClickListener {
                val intent = Intent(context, AppPatchSettingsActivity::class.java).apply {
                    putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, "Instagram")
                }
                startActivity(intent)
            }

            // Battery Optimization Anti-Kill Listener
            view.findViewById<View>(R.id.btn_battery_optimization)?.setOnClickListener {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                    // Attempt to disable Phantom Process Killer (requires root, safe if fails)
                    Thread {
                        try {
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put global settings_enable_monitor_phantom_procs false"))
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }.start()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Tidak dapat membuka pengaturan baterai.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            // Click listeners
            view.findViewById<View>(R.id.card_faq)?.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhpatch/Rhpatch/wiki/Frequently-Asked-Questions"))
                startActivity(intent)
            }

            view.findViewById<View>(R.id.card_update)?.setOnClickListener {
                UpdateChecker(requireActivity()).checkUpdate(silent = false)
            }

            view.findViewById<View>(R.id.card_diag)?.setOnClickListener {
                showDiagnosticsDialog(context)
            }

            // Theme Customization Dialog
            view.findViewById<View>(R.id.btn_change_theme)?.setOnClickListener {
                showThemeDialog()
            }

            // Language Customization Dialog
            view.findViewById<View>(R.id.btn_change_language)?.setOnClickListener {
                showLanguageDialog()
            }
            
            // Global Backup & Restore
            view.findViewById<View>(R.id.btn_export_global)?.setOnClickListener {
                val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                val defaultFileName = "rhpatch_global_backup_" + sdf.format(Date()) + ".json"
                try {
                    exportLauncher.launch(defaultFileName)
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<View>(R.id.btn_import_global)?.setOnClickListener {
                try {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<View>(R.id.btn_reset_global)?.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Reset Semua Pengaturan")
                    .setMessage("Apakah Anda yakin ingin menghapus semua konfigurasi di SEMUA modul?")
                    .setPositiveButton("Reset") { _, _ ->
                        // Clear all appPatchConfigurations
                        for (appPatchInfo in appPatchConfigurations) {
                            val prefs = context.getSharedPreferences(appPatchInfo.packageName, Context.MODE_PRIVATE)
                            prefs?.edit()?.clear()?.apply()
                        }
                        // Clear WA prefs
                        val waPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        waPrefs?.edit()?.clear()?.apply()
                        Toast.makeText(context, "Semua pengaturan berhasil di-reset!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            return view
        }

        private fun showThemeDialog() {
            val context = requireContext()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val currentMode = prefs.getString("thememode", "0")?.toIntOrNull() ?: 0

            val options = arrayOf(
                "Mengikuti Sistem",
                "Gelap AMOLED",
                "Terang"
            )
            val checkedItem = when (currentMode) {
                1 -> 1 // Dark AMOLED
                2 -> 2 // Light
                else -> 0 // Follow system
            }

            MaterialAlertDialogBuilder(context)
                .setTitle("Pilih Tema RHPatch")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val selectedMode = when (which) {
                        1 -> 1
                        2 -> 2
                        else -> 0
                    }
                    prefs.edit().putString("thememode", selectedMode.toString()).apply()
                    App.setThemeMode(selectedMode)
                    dialog.dismiss()
                    activity?.recreate()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        private fun showLanguageDialog() {
            val context = requireContext()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val currentLang = prefs.getString("app_language", if (prefs.getBoolean("force_english", false)) "en" else "id")

            val options = arrayOf(
                "Bahasa Indonesia (ID)",
                "English (EN)"
            )
            val checkedItem = if (currentLang == "en") 1 else 0

            MaterialAlertDialogBuilder(context)
                .setTitle("Pilih Bahasa Aplikasi")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val selectedLang = if (which == 1) "en" else "id"
                    prefs.edit()
                        .putString("app_language", selectedLang)
                        .putBoolean("force_english", selectedLang == "en")
                        .apply()
                    App.changeLanguage(context)
                    dialog.dismiss()
                    activity?.recreate()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        private fun performGlobalExport(uri: Uri) {
            val context = context ?: return
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    val globalJson = JSONObject()
                    // Backup all appPatchConfigurations
                    for (appPatchInfo in appPatchConfigurations) {
                        val prefs = context.getSharedPreferences(appPatchInfo.packageName, Context.MODE_PRIVATE)
                        if (prefs != null) {
                            val prefsJson = JSONObject()
                            prefs.all.forEach { (key, value) ->
                                if (value is Boolean || value is String || value is Int || value is Float || value is Long) {
                                    prefsJson.put(key, value)
                                }
                            }
                            globalJson.put(appPatchInfo.packageName, prefsJson)
                        }
                    }
                    // Also backup WA prefs
                    val waPrefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                    if (waPrefs != null) {
                        val waJson = JSONObject()
                        waPrefs.all.forEach { (key, value) ->
                            if (value is Boolean || value is String || value is Int || value is Float || value is Long) {
                                waJson.put(key, value)
                            }
                        }
                        globalJson.put("com.whatsapp.prefs", waJson)
                    }
                    output.write(globalJson.toString(4).toByteArray())
                }
                Toast.makeText(context, "Semua konfigurasi berhasil dicadangkan (Export Success)!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        private fun performGlobalImport(uri: Uri) {
            val context = context ?: return
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val data = String(input.readBytes())
                    val globalJson = JSONObject(data)
                    
                    // Restore appPatchConfigurations
                    for (appPatchInfo in appPatchConfigurations) {
                        if (globalJson.has(appPatchInfo.packageName)) {
                            val prefs = context.getSharedPreferences(appPatchInfo.packageName, Context.MODE_PRIVATE)
                            val prefsJson = globalJson.getJSONObject(appPatchInfo.packageName)
                            val editor = prefs?.edit()
                            val keys = prefsJson.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = prefsJson.get(key)
                                when (value) {
                                    is Boolean -> editor?.putBoolean(key, value)
                                    is String -> editor?.putString(key, value)
                                    is Int -> editor?.putInt(key, value)
                                    is Float -> editor?.putFloat(key, (value as Number).toFloat())
                                    is Long -> editor?.putLong(key, value)
                                }
                            }
                            editor?.apply()
                        }
                    }
                    // Restore WA prefs
                    if (globalJson.has("com.whatsapp.prefs")) {
                        val waPrefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                        val waJson = globalJson.getJSONObject("com.whatsapp.prefs")
                        val editor = waPrefs?.edit()
                        val keys = waJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = waJson.get(key)
                            when (value) {
                                is Boolean -> editor?.putBoolean(key, value)
                                is String -> editor?.putString(key, value)
                                is Int -> editor?.putInt(key, value)
                                is Float -> editor?.putFloat(key, (value as Number).toFloat())
                                is Long -> editor?.putLong(key, value)
                            }
                        }
                        editor?.apply()
                    }
                }
                Toast.makeText(context, "Konfigurasi berhasil dipulihkan (Import Success)!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showDiagnosticsDialog(context: Context) {
            val dialogBinding = DialogDiagnosticsLogBinding.inflate(layoutInflater)
            val adapter = LogLineAdapter()

            dialogBinding.logRecycler.layoutManager = LinearLayoutManager(context)
            dialogBinding.logRecycler.adapter = adapter

            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.diag_dialog_title)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.diag_close, null)
                .setNeutralButton(R.string.diag_share) { _, _ ->
                    try {
                        val logText = adapter.getLogs().joinToString("\n") { it.message }
                        val logHeader = "=== DIAGNOSTIK RHPATCH ===\n\n" + logText + "\n\n--- BEGIN LOGCAT ---\n"
                        val cacheHeaderFile = java.io.File(context.cacheDir, "rhpatch_log_header.txt")
                        val sdcardFile = java.io.File(context.cacheDir, "logAndroid_Rhpatch.txt")
                        
                        cacheHeaderFile.writeText(logHeader)
                        
                        val shellCmd = "cat " + cacheHeaderFile.absolutePath + " > " + sdcardFile.absolutePath + " && logcat -d >> " + sdcardFile.absolutePath + " && cat /data/adb/lspd/log/error.log >> " + sdcardFile.absolutePath + " && cat /data/adb/lspd/log/modules.log >> " + sdcardFile.absolutePath + " && cat /data/adb/lspd/log/verbose.log >> " + sdcardFile.absolutePath + " && chown " + android.os.Process.myUid() + ":" + android.os.Process.myUid() + " " + sdcardFile.absolutePath + " && chmod 644 " + sdcardFile.absolutePath
                        com.topjohnwu.superuser.Shell.cmd(shellCmd).exec()
                        
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", sdcardFile)
                        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Log Diagnosa Rhpatch")
                        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Terlampir file log Diagnosa Root & Sistem Rhpatch secara lengkap.")
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        
                        startActivity(android.content.Intent.createChooser(intent, getString(R.string.diag_share)))
                    } catch (e: Exception) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Gagal melampirkan file.\n\nLog Diagnosa Root:\n\n" + adapter.getLogs().joinToString("\n") { it.message })
                        startActivity(android.content.Intent.createChooser(intent, getString(R.string.diag_share)))
                    }
                }
                .setCancelable(true)
                .show()

            val handler = Handler(Looper.getMainLooper())
            val queue = java.util.ArrayList<RootDiagnostics.LogEntry>()

            RootDiagnostics.runDiagnostics(context, object : RootDiagnostics.Callback {
                override fun onLog(entry: RootDiagnostics.LogEntry) {
                    if (!isAdded) return
                    queue.add(entry)
                }
            })

            val poller = object : Runnable {
                private var emptyCycles = 0

                override fun run() {
                    if (!isAdded || dialog == null || !dialog.isShowing) return

                    if (queue.isNotEmpty()) {
                        emptyCycles = 0
                        adapter.add(queue.removeAt(0))
                        dialogBinding.logRecycler.smoothScrollToPosition(adapter.itemCount - 1)
                        handler.postDelayed(this, 120)
                    } else if (emptyCycles < 50) {
                        emptyCycles++
                        handler.postDelayed(this, 120)
                    }
                }
            }
            handler.postDelayed(poller, 120)
        }
    }

    class ModulesFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_modules, container, false)
            val context = requireContext()
            val containerLayout = view.findViewById<LinearLayout>(R.id.modules_container)
            val etSearch = view.findViewById<android.widget.EditText>(R.id.et_search_modules)
            val pm = context.packageManager

            // Fast batch lookup: 1 IPC call instead of 30+ sequential calls
            val installedPackagesSet = runCatching {
                pm.getInstalledPackages(0).map { it.packageName }.toSet()
            }.getOrDefault(emptySet())

            fun renderModules(query: String = "") {
                containerLayout.removeAllViews()
                val q = query.trim().lowercase()

                // 1. System Anti-Spam Card
                val spamName = "🛡️ System Anti-Spam"
                val spamPkg = "Blokir SMS, Telepon Spam, Penipuan & Judi Online"
                if (q.isEmpty() || spamName.lowercase().contains(q) || spamPkg.lowercase().contains(q) || q.contains("spam") || q.contains("sms")) {
                    val spamCard = inflater.inflate(R.layout.item_module_card, containerLayout, false)
                    spamCard.findViewById<TextView>(R.id.app_name).text = spamName
                    spamCard.findViewById<TextView>(R.id.app_package).text = spamPkg
                    val spamBadge = spamCard.findViewById<TextView>(R.id.status_badge)
                    spamBadge.text = "System Module"
                    spamBadge.setTextColor(Color.parseColor("#8B5CF6"))
                    spamCard.setOnClickListener {
                        startActivity(Intent(context, AntiSpamActivity::class.java))
                    }
                    containerLayout.addView(spamCard)
                }

                // 2. DNS AdGuard Bypass Card
                val dnsName = "🌐 DNS AdGuard Bypass"
                val dnsPkg = "Bypass Private DNS & Blokir Iklan Global Tingkat Sistem"
                if (q.isEmpty() || dnsName.lowercase().contains(q) || dnsPkg.lowercase().contains(q) || q.contains("dns") || q.contains("adguard") || q.contains("bypass")) {
                    val dnsCard = inflater.inflate(R.layout.item_module_card, containerLayout, false)
                    dnsCard.findViewById<TextView>(R.id.app_name).text = dnsName
                    dnsCard.findViewById<TextView>(R.id.app_package).text = dnsPkg
                    val dnsBadge = dnsCard.findViewById<TextView>(R.id.status_badge)
                    dnsBadge.text = "System Module"
                    dnsBadge.setTextColor(Color.parseColor("#8B5CF6"))
                    dnsCard.setOnClickListener {
                        startActivity(Intent(context, DnsAppPickerActivity::class.java))
                    }
                    containerLayout.addView(dnsCard)
                }

                // 3. Explicit WhatsApp Card
                val waName = "WhatsApp / WA Business"
                val waPkg = "com.whatsapp / com.whatsapp.w4b"
                if (q.isEmpty() || waName.lowercase().contains(q) || waPkg.lowercase().contains(q)) {
                    val waCard = inflater.inflate(R.layout.item_module_card, containerLayout, false)
                    waCard.findViewById<TextView>(R.id.app_name).text = waName
                    waCard.findViewById<TextView>(R.id.app_package).text = waPkg
                    val waBadge = waCard.findViewById<TextView>(R.id.status_badge)
                    val isWaInstalled = installedPackagesSet.contains("com.whatsapp") || installedPackagesSet.contains("com.whatsapp.w4b")
                    if (isWaInstalled) {
                        waBadge.text = "Installed"
                        waBadge.setTextColor(Color.parseColor("#10B981"))
                    } else {
                        waBadge.text = "Not Installed"
                        waBadge.setTextColor(Color.parseColor("#94A3B8"))
                    }
                    waCard.setOnClickListener {
                        startActivity(Intent(context, com.rhdevs.rhpatch.activities.MainActivity::class.java))
                    }
                    containerLayout.addView(waCard)
                }

                for (appPatchInfo in appPatchConfigurations) {
                    val name = appPatchInfo.appName
                    val pkg = appPatchInfo.packageName
                    val matchesQuery = q.isEmpty() || name.lowercase().contains(q) || pkg.lowercase().contains(q) ||
                            appPatchInfo.patches.any { it.name.lowercase().contains(q) || it.description?.lowercase()?.contains(q) == true }

                    if (!matchesQuery) continue

                    val isInstalled = installedPackagesSet.contains(appPatchInfo.packageName)

                    val card = inflater.inflate(R.layout.item_module_card, containerLayout, false)
                    card.findViewById<TextView>(R.id.app_name).text = name
                    card.findViewById<TextView>(R.id.app_package).text = pkg
                    
                    val badge = card.findViewById<TextView>(R.id.status_badge)
                    if (isInstalled) {
                        badge.text = "Installed"
                        badge.setTextColor(Color.parseColor("#10B981"))
                    } else {
                        badge.text = "Not Installed"
                        badge.setTextColor(Color.parseColor("#94A3B8"))
                    }

                    card.setOnClickListener {
                        if (appPatchInfo.appName.startsWith("TikTok") && !appPatchInfo.appName.contains("Lite")) {
                            startActivity(Intent(context, TikTokSettingsActivity::class.java))
                        } else {
                            val intent = Intent(context, AppPatchSettingsActivity::class.java).apply {
                                putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, appPatchInfo.appName)
                            }
                            startActivity(intent)
                        }
                    }

                    containerLayout.addView(card)
                }
            }

            renderModules()

            etSearch?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderModules(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            return view
        }
    }

    class AboutFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_about, container, false)
            view.findViewById<TextView>(R.id.about_version).text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})"
            
            view.findViewById<View>(R.id.btn_bagibagi)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bagibagi.co/Rh7155")))
            }
            view.findViewById<View>(R.id.btn_saweria)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/RH7155")))
            }
            view.findViewById<View>(R.id.btn_sociabuzz)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sociabuzz.com/abogoboga7155/tribe")))
            }
            view.findViewById<View>(R.id.btn_install_guide)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/installation_guide.md")))
            }
            view.findViewById<View>(R.id.btn_contribution)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/contribution.md")))
            }
            view.findViewById<View>(R.id.btn_github)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo")))
            }
            view.findViewById<View>(R.id.btn_telegram)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apahayo")))
            }
            view.findViewById<View>(R.id.btn_license)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/LICENSE")))
            }

            return view
        }
    }
}



