package com.rhdevs.rhpatch.activity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.BuildConfig
import com.rhdevs.rhpatch.service.MockLocationService
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var fabSaveLocation: ExtendedFloatingActionButton
    private lateinit var switchMockLocation: SwitchMaterial
    private var myLocationOverlay: MyLocationNewOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val osmdroidPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        try {
            Configuration.getInstance().load(applicationContext, osmdroidPrefs)
        } catch (e: Exception) {
            // If SharedPreferences were corrupted (e.g., Long saved as Int by JSON restore), clear osmdroid keys
            val editor = osmdroidPrefs.edit()
            osmdroidPrefs.all.keys.forEach {
                if (it.startsWith("osmdroid") || it == "osmdroid.basePath" || it == "osmdroid.cachePath") {
                    editor.remove(it)
                }
            }
            editor.apply()
            Configuration.getInstance().load(applicationContext, osmdroidPrefs)
        }
        Configuration.getInstance().userAgentValue = packageName
        
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_map)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mapView = findViewById(R.id.mapView)
        searchEditText = findViewById(R.id.searchEditText)
        fabSaveLocation = findViewById(R.id.fabSaveLocation)
        switchMockLocation = findViewById(R.id.switchMockLocation)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        val currentLat = prefs.getFloat("fake_gps_lat", -6.175110f).toDouble()
        val currentLon = prefs.getFloat("fake_gps_lon", 106.827153f).toDouble()
        val startPoint = GeoPoint(currentLat, currentLon)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        val isServiceRunning = prefs.getBoolean("fake_gps_running", false)
        switchMockLocation.isChecked = isServiceRunning

        switchMockLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isMockLocationEnabled()) {
                    switchMockLocation.isChecked = false
                    showMockLocationDialog()
                } else {
                    val center = mapView.mapCenter as GeoPoint
                    prefs.edit {
                        putFloat("fake_gps_lat", center.latitude.toFloat())
                        putFloat("fake_gps_lon", center.longitude.toFloat())
                        putBoolean("fake_gps_running", true)
                    }
                    val serviceIntent = Intent(this, MockLocationService::class.java).apply {
                        putExtra("lat", center.latitude)
                        putExtra("lon", center.longitude)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    Toast.makeText(this, "Fake GPS (Play) dimulai", Toast.LENGTH_SHORT).show()
                }
            } else {
                prefs.edit { putBoolean("fake_gps_running", false) }
                stopService(Intent(this, MockLocationService::class.java))
                Toast.makeText(this, "Fake GPS (Stop) dihentikan", Toast.LENGTH_SHORT).show()
            }
        }

        fabSaveLocation.setOnClickListener {
            val center = mapView.mapCenter as GeoPoint
            prefs.edit {
                putFloat("fake_gps_lat", center.latitude.toFloat())
                putFloat("fake_gps_lon", center.longitude.toFloat())
            }
            if (switchMockLocation.isChecked) {
                val serviceIntent = Intent(this, MockLocationService::class.java).apply {
                    putExtra("lat", center.latitude)
                    putExtra("lon", center.longitude)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
            Toast.makeText(this, "Lokasi palsu disimpan: ${center.latitude}, ${center.longitude}", Toast.LENGTH_SHORT).show()
        }

        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotEmpty()) searchLocation(query)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }
        
        if (!isMockLocationEnabled()) {
            showMockLocationDialog()
        }
    }
    
    private fun isMockLocationEnabled(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val mode = appOpsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun showMockLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Anda belum mengatur Rhpatch sebagai Aplikasi Lokasi Palsu (Mock Location App).\n\nSilakan masuk ke Opsi Pengembang -> Pilih aplikasi lokasi palsu -> Pilih Rhpatch.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun searchLocation(query: String) {
        Toast.makeText(this, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val urlString = "https://nominatim.openstreetmap.org/search?q=${query.replace(" ", "+")}&format=json&limit=1"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RhpatchFakeGPS/1.0")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getString("lat").toDouble()
                        val lon = firstResult.getString("lon").toDouble()
                        runOnUiThread { mapView.controller.animateTo(GeoPoint(lat, lon)) }
                    } else {
                        runOnUiThread { Toast.makeText(this@MapActivity, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show() }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MapActivity, "Error mencari lokasi", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
    private fun enableMyLocation() {
        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
            mapView.overlays.add(myLocationOverlay)
        }
        
        myLocationOverlay?.enableMyLocation()
        
        if (myLocationOverlay?.myLocation != null) {
            mapView.controller.animateTo(myLocationOverlay?.myLocation)
            mapView.controller.setZoom(18.0)
        } else {
            myLocationOverlay?.runOnFirstFix {
                runOnUiThread {
                    mapView.controller.animateTo(myLocationOverlay?.myLocation)
                    mapView.controller.setZoom(18.0)
                }
            }
            Toast.makeText(this, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }
}


