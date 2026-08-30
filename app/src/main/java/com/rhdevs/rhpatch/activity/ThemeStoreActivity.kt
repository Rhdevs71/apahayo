package com.rhdevs.rhpatch.activity

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.preference.ThemePreference
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class ThemeModel(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val cssUrl: String,
    val previewUrl: String
)

class ThemeAdapter(
    private val themes: List<ThemeModel>,
    private val onApplyClick: (ThemeModel) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    class ThemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.img_preview)
        val tvName: TextView = view.findViewById(R.id.tv_theme_name)
        val tvAuthor: TextView = view.findViewById(R.id.tv_theme_author)
        val tvDesc: TextView = view.findViewById(R.id.tv_theme_desc)
        val btnApply: Button = view.findViewById(R.id.btn_apply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_card, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.tvName.text = theme.name
        holder.tvAuthor.text = "By ${theme.author}"
        holder.tvDesc.text = theme.description

        // Basic image downloader
        holder.imgPreview.setImageDrawable(null)
        if (theme.previewUrl.isNotEmpty()) {
            thread {
                try {
                    val url = URL(theme.previewUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true
                    conn.connect()
                    val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                    holder.imgPreview.post {
                        holder.imgPreview.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        holder.btnApply.setOnClickListener {
            onApplyClick(theme)
        }
    }

    override fun getItemCount(): Int = themes.size
}

class ThemeStoreActivity : AppCompatActivity() {

    private lateinit var recyclerThemes: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    // Default store URL (can be changed later)
    private val STORE_URL = "https://raw.githubusercontent.com/Rhdevs71/apahayo/main/themes/store.json"

    override fun attachBaseContext(newBase: Context) {
        val localized = App.changeLanguage(newBase)
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_store)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Theme Store"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerThemes = findViewById(R.id.recycler_themes)
        progressBar = findViewById(R.id.progress_bar)
        tvError = findViewById(R.id.tv_error)

        recyclerThemes.layoutManager = LinearLayoutManager(this)
        
        fetchThemes()
    }

    private fun fetchThemes() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        recyclerThemes.visibility = View.GONE

        thread {
            try {
                val url = URL(STORE_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val themeList = mutableListOf<ThemeModel>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        themeList.add(
                            ThemeModel(
                                id = obj.optString("id", "theme_$i"),
                                name = obj.optString("name", "Unknown Theme"),
                                author = obj.optString("author", "Unknown"),
                                description = obj.optString("description", ""),
                                cssUrl = obj.optString("css_url", ""),
                                previewUrl = obj.optString("preview_img", "")
                            )
                        )
                    }

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (themeList.isEmpty()) {
                            tvError.text = "No themes found."
                            tvError.visibility = View.VISIBLE
                        } else {
                            recyclerThemes.visibility = View.VISIBLE
                            recyclerThemes.adapter = ThemeAdapter(themeList) { selectedTheme ->
                                downloadAndApplyTheme(selectedTheme)
                            }
                        }
                    }
                } else {
                    throw Exception("Server returned HTTP ${conn.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvError.text = "Failed to load themes:\n${e.message}"
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun downloadAndApplyTheme(theme: ThemeModel) {
        if (theme.cssUrl.isEmpty()) {
            Toast.makeText(this, "Theme CSS URL is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Downloading ${theme.name}...", Toast.LENGTH_SHORT).show()
        
        thread {
            try {
                val url = URL(theme.cssUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connect()

                val cssContent = conn.inputStream.bufferedReader().use { it.readText() }
                
                // Save to ThemePreference.rootDirectory
                val themeDir = File(ThemePreference.rootDirectory, theme.id)
                if (!themeDir.exists()) themeDir.mkdirs()

                val cssFile = File(themeDir, "theme.css")
                FileOutputStream(cssFile).use {
                    it.write(cssContent.toByteArray())
                }

                // Apply to SharedPreferences
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("folder_theme", theme.id)
                    .putString("css_theme", "theme.css")
                    .putBoolean("custom_filters", true) // Enable CSS Engine
                    .apply()

                runOnUiThread {
                    Toast.makeText(this, "Theme applied! Please restart WhatsApp.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to apply theme: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
