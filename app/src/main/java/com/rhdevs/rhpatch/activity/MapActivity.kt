package com.rhdevs.rhpatch.activity

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.rhdevs.rhpatch.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid configuration
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = packageName
        
        // Allowed to do network operations for Geocoding on separate thread, 
        // but for safety in OSMDroid sometimes we need this (though we'll use thread anyway)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_map)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mapView = findViewById(R.id.mapView)
        searchEditText = findViewById(R.id.searchEditText)
        fabSaveLocation = findViewById(R.id.fabSaveLocation)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        
        // Initialize Map Position
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        
        // Get existing fake location, default to Jakarta (Monas)
        val currentLat = prefs.getFloat("fake_gps_lat", -6.175110f).toDouble()
        val currentLon = prefs.getFloat("fake_gps_lon", 106.827153f).toDouble()
        val startPoint = GeoPoint(currentLat, currentLon)
        mapController.setCenter(startPoint)

        fabSaveLocation.setOnClickListener {
            val center = mapView.mapCenter as GeoPoint
            prefs.edit {
                putFloat("fake_gps_lat", center.latitude.toFloat())
                putFloat("fake_gps_lon", center.longitude.toFloat())
            }
            Toast.makeText(this, "Lokasi palsu disimpan: ${center.latitude}, ${center.longitude}", Toast.LENGTH_SHORT).show()
            finish()
        }

        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                }
                
                // Hide keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun searchLocation(query: String) {
        Toast.makeText(this, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                // OpenStreetMap Nominatim API for Geocoding
                val urlString = "https://nominatim.openstreetmap.org/search?q=${query.replace(" ", "+")}&format=json&limit=1"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RhpatchFakeGPS/1.0")
                
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getString("lat").toDouble()
                        val lon = firstResult.getString("lon").toDouble()
                        
                        runOnUiThread {
                            val geoPoint = GeoPoint(lat, lon)
                            mapView.controller.animateTo(geoPoint)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MapActivity, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Error mencari lokasi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
