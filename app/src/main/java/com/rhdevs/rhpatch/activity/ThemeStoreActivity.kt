package com.rhdevs.rhpatch.activity

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.preference.ThemePreference
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.util.zip.ZipInputStream
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
        val tvTitle: TextView = view.findViewById(R.id.tv_theme_name)
        val tvAuthor: TextView = view.findViewById(R.id.tv_theme_author)
        val imgPreview: ImageView = view.findViewById(R.id.img_preview)
        val btnApply: Button = view.findViewById(R.id.btn_apply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_card, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.tvTitle.text = theme.name
        holder.tvAuthor.text = "By ${theme.author}"

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
    private val STORE_URL = "https://raw.githubusercontent.com/Rhdevs71/apahayo/main/themes/store.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_store)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerThemes = findViewById(R.id.recycler_themes)
        progressBar = findViewById(R.id.progress_bar)
        tvError = findViewById(R.id.tv_error)

        recyclerThemes.layoutManager = LinearLayoutManager(this)

        val btnResetTheme = findViewById<View>(R.id.btnResetTheme)
        btnResetTheme.setOnClickListener {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit()
                .remove("folder_theme")
                .remove("css_theme")
                .remove("custom_css")
                .putBoolean("custom_filters", false)
                .apply()
            Toast.makeText(this, "Tema berhasil di-reset ke Default WhatsApp!", Toast.LENGTH_SHORT).show()
        }

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
                                cssUrl = obj.optString("zip_url", ""),
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
            Toast.makeText(this, "Theme ZIP URL is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Mengunduh dan mengekstrak ${theme.name}...", Toast.LENGTH_LONG).show()
        
        thread {
            try {
                val url = URL(theme.cssUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connect()

                val themeDir = File(ThemePreference.rootDirectory, theme.id)
                if (!themeDir.exists()) themeDir.mkdirs()

                // Download and Unzip
                ZipInputStream(conn.inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(themeDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { fos ->
                                val buffer = ByteArray(1024)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                // Apply to SharedPreferences
                val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@ThemeStoreActivity)
                val edit = prefs.edit()
                edit.putString("folder_theme", theme.id)
                edit.putString("css_theme", "style.css")
                edit.putBoolean("custom_filters", true)
                
                // Read style.css and put it into custom_css
                val styleCssFile = File(themeDir, "style.css")
                if (styleCssFile.exists()) {
                    val cssContent = styleCssFile.readText(Charsets.UTF_8)
                    edit.putString("custom_css", cssContent)
                }
                edit.apply()

                runOnUiThread {
                    Toast.makeText(this@ThemeStoreActivity, "Tema ${theme.name} berhasil diterapkan!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ThemeStoreActivity, "Gagal memasang tema: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
