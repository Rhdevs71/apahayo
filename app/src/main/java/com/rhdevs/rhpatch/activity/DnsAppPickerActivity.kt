package com.rhdevs.rhpatch.activity

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wmods.wppenhacer.R
import java.util.Locale
import kotlin.concurrent.thread

class DnsAppPickerActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: AppListAdapter
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var appRecycler: RecyclerView

    private var allApps = mutableListOf<AppItem>()
    private var filteredApps = mutableListOf<AppItem>()
    private val selectedPackages = mutableSetOf<String>()

    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: Drawable?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)
        actionBar?.hide()

        prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        val switchGlobalDns = findViewById<Switch>(R.id.switch_global_dns)
        val searchBox = findViewById<EditText>(R.id.search_box)
        val btnSetAdguardDns = findViewById<android.widget.Button>(R.id.btn_set_adguard_dns)
        appRecycler = findViewById(R.id.app_recycler)
        loadingSpinner = findViewById(R.id.loading_spinner)

        // Load existing whitelist
        val whitelistStr = prefs.getString("dns_bypass_whitelist", "") ?: ""
        if (whitelistStr.isNotEmpty()) {
            selectedPackages.addAll(whitelistStr.split(",").map { it.trim() })
        }

        switchGlobalDns.isChecked = prefs.getBoolean("dns_bypass_enabled", false)
        switchGlobalDns.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dns_bypass_enabled", isChecked).apply()
            makePrefsReadable()
        }

        btnSetAdguardDns.setOnClickListener {
            thread {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.adguard.com"))
                    runOnUiThread {
                        android.widget.Toast.makeText(this@DnsAppPickerActivity, "DNS berhasil diatur ke dns.adguard.com", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        android.widget.Toast.makeText(this@DnsAppPickerActivity, "Gagal mengatur DNS. Pastikan akses Root diberikan.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        adapter = AppListAdapter()
        appRecycler.layoutManager = LinearLayoutManager(this)
        appRecycler.adapter = adapter

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadApps()
    }

    private fun loadApps() {
        loadingSpinner.visibility = View.VISIBLE
        appRecycler.visibility = View.GONE

        thread {
            val pm = packageManager
            val packages = pm.getInstalledApplications(0)
            val tempApps = mutableListOf<AppItem>()

            for (appInfo in packages) {
                // Filter out some system apps if desired, but here we show all user apps and essential system apps
                if (appInfo.packageName == packageName) continue // Skip ourself
                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                tempApps.add(AppItem(name, appInfo.packageName, icon))
            }

            tempApps.sortBy { it.name.lowercase(Locale.getDefault()) }

            runOnUiThread {
                allApps.clear()
                allApps.addAll(tempApps)
                filteredApps.clear()
                filteredApps.addAll(tempApps)
                adapter.notifyDataSetChanged()

                loadingSpinner.visibility = View.GONE
                appRecycler.visibility = View.VISIBLE
            }
        }
    }

    private fun filterApps(query: String) {
        val lowerQuery = query.lowercase(Locale.getDefault())
        filteredApps.clear()
        if (lowerQuery.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            for (app in allApps) {
                if (app.name.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    app.packageName.lowercase(Locale.getDefault()).contains(lowerQuery)
                ) {
                    filteredApps.add(app)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveWhitelist() {
        prefs.edit().putString("dns_bypass_whitelist", selectedPackages.joinToString(",")).apply()
        makePrefsReadable()
    }

    private fun makePrefsReadable() {
        runCatching {
            val file = java.io.File(filesDir.parentFile, "shared_prefs/prefs.xml")
            if (file.exists()) {
                file.setReadable(true, false)
            }
        }
    }

    inner class AppListAdapter : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
            val packageName: TextView = view.findViewById(R.id.app_package)
            val checkbox: CheckBox = view.findViewById(R.id.app_checkbox)

            init {
                view.setOnClickListener {
                    val isChecked = !checkbox.isChecked
                    checkbox.isChecked = isChecked
                    val item = filteredApps[adapterPosition]
                    if (isChecked) {
                        selectedPackages.add(item.packageName)
                    } else {
                        selectedPackages.remove(item.packageName)
                    }
                    saveWhitelist()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredApps[position]
            holder.name.text = item.name
            holder.packageName.text = item.packageName
            holder.icon.setImageDrawable(item.icon)
            holder.checkbox.isChecked = selectedPackages.contains(item.packageName)
        }

        override fun getItemCount(): Int = filteredApps.size
    }
}
