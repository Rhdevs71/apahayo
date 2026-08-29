package com.rhdevs.rhpatch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private val providerName = LocationManager.GPS_PROVIDER
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lat = intent?.getDoubleExtra("lat", -6.200000) ?: -6.200000
        val lon = intent?.getDoubleExtra("lon", 106.816666) ?: 106.816666

        startForeground(1999, createNotification())
        
        setupMockProvider()
        startMocking(lat, lon)

        return START_STICKY
    }

    private fun setupMockProvider() {
        try {
            locationManager.addTestProvider(
                providerName, false, false, false, false, true, true, true, 1, 1
            )
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: Exception) {
            Log.e("RhpatchFakeGPS", "Failed to add test provider: ${e.message}")
        }
    }

    private fun startMocking(lat: Double, lon: Double) {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            while (isActive) {
                try {
                    val location = Location(providerName).apply {
                        latitude = lat
                        longitude = lon
                        accuracy = 3.0f
                        time = System.currentTimeMillis()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        }
                    }
                    locationManager.setTestProviderLocation(providerName, location)
                } catch (e: Exception) {
                    Log.e("RhpatchFakeGPS", "Failed to set location: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        try {
            locationManager.removeTestProvider(providerName)
        } catch (e: Exception) {
            // Ignore
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fake_gps_channel",
                "Fake GPS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running Fake GPS in background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "fake_gps_channel")
            .setContentTitle("Rhpatch Fake GPS Aktif")
            .setContentText("Memancarkan lokasi palsu (Global)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
