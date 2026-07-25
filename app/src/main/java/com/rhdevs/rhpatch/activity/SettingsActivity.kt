@file:Suppress("DEPRECATION") @file:SuppressLint("WorldReadableFiles")
package com.rhdevs.rhpatch.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import com.rhdevs.rhpatch.AppPatchInfo
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.appPatchConfigurations
import com.rhdevs.rhpatch.common.UpdateChecker
import kotlin.system.exitProcess

class SettingsActivity : Activity() {
    private lateinit var aboutPreference: MorpheAboutPreference

    companion object {
        @JvmStatic
        fun isModuleActive(): Boolean {
            return System.getProperty("rhpatch.active") == "true"
        }
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
        actionBar?.hide() // We use our own header!

        Utils.setContext(this)
        aboutPreference = MorpheAboutPreference(this).apply {
            setTitle(R.string.about_title)
        }

        // Check module active status
        val badge = findViewById<TextView>(R.id.active_status_badge)
        if (isModuleActive()) {
            badge.text = "Active"
            badge.setTextColor(Color.parseColor("#10B981")) // Green-500
        } else {
            badge.text = "Inactive"
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
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
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
        fragmentManager.beginTransaction()
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
                    val file = java.io.File(filesDir.parentFile, "shared_prefs/prefs.xml")
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
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_overview, container, false)
            
            // Set active status
            val statusText = view.findViewById<TextView>(R.id.card_active_status)
            val descText = view.findViewById<TextView>(R.id.status_description)
            if (isModuleActive()) {
                statusText.text = "Active"
                statusText.setTextColor(Color.parseColor("#10B981"))
                descText.text = "Rhpatch is loaded and functioning correctly."
            } else {
                statusText.text = "Inactive"
                statusText.setTextColor(Color.parseColor("#EF4444"))
                descText.text = "Module is not active in LSPosed. Please enable it and reboot."
            }

            // Version info
            val versionInfo = view.findViewById<TextView>(R.id.text_version_info)
            versionInfo.text = "Current version: ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH}) ${BuildConfig.BUILD_TYPE}\nBuild Date: ${DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)}"

            // Click listeners
            view.findViewById<View>(R.id.card_faq).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhpatch/Rhpatch/wiki/Frequently-Asked-Questions"))
                startActivity(intent)
            }

            view.findViewById<View>(R.id.card_update).setOnClickListener {
                UpdateChecker().apply {
                    setActivity(activity)
                    checkUpdate(silent = false)
                }
            }

            view.findViewById<View>(R.id.card_diag).setOnClickListener {
                showDiagnosticsDialog(view.context)
            }

            return view
        }

        private fun showDiagnosticsDialog(context: android.content.Context) {
            val dialogBinding = com.wmods.wppenhacer.databinding.DialogDiagnosticsLogBinding.inflate(layoutInflater)
            val adapter = com.wmods.wppenhacer.adapter.LogLineAdapter()

            dialogBinding.logRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            dialogBinding.logRecycler.adapter = adapter

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(com.wmods.wppenhacer.R.string.diag_dialog_title)
                .setView(dialogBinding.root)
                .setPositiveButton(com.wmods.wppenhacer.R.string.diag_close, null)
                .setCancelable(true)
                .show()

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val queue = java.util.ArrayList<com.wmods.wppenhacer.utils.RootDiagnostics.LogEntry>()

            com.wmods.wppenhacer.utils.RootDiagnostics.runDiagnostics(context, object : com.wmods.wppenhacer.utils.RootDiagnostics.Callback {
                override fun onLog(entry: com.wmods.wppenhacer.utils.RootDiagnostics.LogEntry) {
                    if (!isAdded) return
                    queue.add(entry)
                }
            })

            val poller = object : Runnable {
                private var emptyCycles = 0
                override fun run() {
                    if (!isAdded || !dialog.isShowing) return
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

    class UniversalSchedulerFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            // For now we will return a simple programmatic view, we can extract this to XML later
            val context = activity
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                setBackgroundColor(Color.parseColor("#0F172A")) // apahayo_bg
            }

            val title = TextView(context).apply {
                text = "Universal Scheduler"
                textSize = 24f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 40)
            }
            layout.addView(title)

            val desc = TextView(context).apply {
                text = "Jadwalkan pesan otomatis untuk Telegram, SMS, Email, dan lainnya lengkap dengan fitur Screen Unlock Keyguard Bypass."
                textSize = 14f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 0, 0, 60)
            }
            layout.addView(desc)
            
            // Temporary button to Lock Screen Config
            val btnLockConfig = android.widget.Button(context).apply {
                text = "Konfigurasi Kunci Layar (PIN/Sandi)"
                setBackgroundColor(Color.parseColor("#3B82F6"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    startActivity(Intent(context, com.rhdevs.rhpatch.ui.ScreenLockConfigActivity::class.java))
                }
            }
            layout.addView(btnLockConfig)

            return layout
        }
    }

    class ModulesFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_modules, container, false)
            val containerLayout = view.findViewById<LinearLayout>(R.id.modules_container)
            
            val pm = context.packageManager
            val waCard = inflater.inflate(R.layout.item_module_card, containerLayout, false)
            waCard.findViewById<TextView>(R.id.app_name).text = "WhatsApp/Business"
            waCard.findViewById<TextView>(R.id.app_package).text = "com.whatsapp / com.whatsapp.w4b"
            val waBadge = waCard.findViewById<TextView>(R.id.status_badge)
            waBadge.text = "Installed"
            waBadge.setTextColor(Color.parseColor("#10B981"))
            waCard.setOnClickListener {
                startActivity(Intent(context, com.wmods.wppenhacer.activities.MainActivity::class.java))
            }
            containerLayout.addView(waCard)
            for (appPatchInfo in appPatchConfigurations) {
                val isInstalled = runCatching {
                    pm.getPackageInfo(appPatchInfo.packageName, 0)
                }.isSuccess

                val card = inflater.inflate(R.layout.item_module_card, containerLayout, false)
                card.findViewById<TextView>(R.id.app_name).text = appPatchInfo.appName
                card.findViewById<TextView>(R.id.app_package).text = appPatchInfo.packageName
                
                val badge = card.findViewById<TextView>(R.id.status_badge)
                if (isInstalled) {
                    badge.text = "Installed"
                    badge.setTextColor(Color.parseColor("#10B981"))
                } else {
                    badge.text = "Not Installed"
                    badge.setTextColor(Color.parseColor("#94A3B8"))
                }

                card.setOnClickListener {
                    val intent = Intent(context, AppPatchSettingsActivity::class.java).apply {
                        putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, appPatchInfo.appName)
                    }
                    startActivity(intent)
                }

                containerLayout.addView(card)
            }
            return view
        }
    }

    class AboutFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_about, container, false)
            view.findViewById<TextView>(R.id.about_version).text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})"
            
            view.findViewById<View>(R.id.btn_bagibagi).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bagibagi.co/Rh7155")))
            }
            view.findViewById<View>(R.id.btn_saweria).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/RH7155")))
            }
            view.findViewById<View>(R.id.btn_sociabuzz).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sociabuzz.com/abogoboga7155/tribe")))
            }
            view.findViewById<View>(R.id.btn_install_guide).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/installation_guide.md")))
            }
            view.findViewById<View>(R.id.btn_contribution).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/contribution.md")))
            }
            view.findViewById<View>(R.id.btn_github).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo")))
            }
            view.findViewById<View>(R.id.btn_telegram).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apahayo")))
            }
            view.findViewById<View>(R.id.btn_license).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rhdevs71/apahayo/blob/main/LICENSE")))
            }

            return view
        }
    }
}
