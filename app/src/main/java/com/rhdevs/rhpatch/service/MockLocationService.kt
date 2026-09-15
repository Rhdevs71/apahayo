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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var fusedClient: FusedLocationProviderClient? = null
    private val providers = arrayOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    private var wakeLock: PowerManager.WakeLock? = null

    private var handlerThread: HandlerThread? = null
    private var locationHandler: Handler? = null
    private var mockRunnable: Runnable? = null
    @Volatile private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()

        // Acquire WakeLock to prevent CPU sleep when heavy apps (e.g. Google Maps) open
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rhpatch:FakeGpsWakeLock").apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        } catch (e: Throwable) {
            Log.e("RhpatchFakeGPS", "WakeLock error: " + e.message)
        }

        // Initialize Google Play Services Fused Location Client for modern apps
        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(this).apply {
                setMockMode(true)
            }
        } catch (e: Throwable) {
            Log.w("RhpatchFakeGPS", "FusedLocationProviderClient init note: " + e.message)
        }

        // Dedicated native foreground HandlerThread for continuous background location updates
        handlerThread = HandlerThread("FakeGpsLocationThread", Process.THREAD_PRIORITY_FOREGROUND).apply {
            start()
            locationHandler = Handler(looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lat = intent?.getDoubleExtra("lat", -6.200000) ?: -6.200000
        val lon = intent?.getDoubleExtra("lon", 106.816666) ?: 106.816666

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1999, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1999, createNotification())
        }

        setupMockProviders()
        startMocking(lat, lon)

        return START_STICKY
    }

    private fun setupMockProviders() {
        for (provider in providers) {
            try {
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {}

            try {
                locationManager.addTestProvider(
                    provider,
                    false, // requiresNetwork
                    false, // requiresSatellite
                    false, // requiresCell
                    false, // hasMonetaryCost
                    true,  // supportsAltitude
                    true,  // supportsSpeed
                    true,  // supportsBearing
                    1,     // powerRequirement
                    1      // accuracy
                )
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {
                Log.w("RhpatchFakeGPS", "Test provider setup for " + provider + ": " + e.message)
            }
        }
    }

    private fun startMocking(lat: Double, lon: Double) {
        mockRunnable?.let { locationHandler?.removeCallbacks(it) }
        isRunning = true

        mockRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val now = System.currentTimeMillis()
                val elapsedNanos = SystemClock.elapsedRealtimeNanos()

                // 1. Dispatch to Android LocationManager test providers (gps, network, passive)
                for (provider in providers) {
                    try {
                        val location = Location(provider).apply {
                            latitude = lat
                            longitude = lon
                            altitude = 15.0
                            accuracy = 1.0f // High precision to override real cell/GPS
                            bearing = 0.0f
                            speed = 0.0f
                            time = now
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                elapsedRealtimeNanos = elapsedNanos
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                bearingAccuracyDegrees = 0.1f
                                speedAccuracyMetersPerSecond = 0.1f
                                verticalAccuracyMeters = 0.5f
                            }
                        }
                        locationManager.setTestProviderLocation(provider, location)
                    } catch (_: Exception) {}
                }

                // 2. Dispatch to Google Play Services Fused Location Client (Google Maps, Ride-hailing, etc.)
                try {
                    val fusedLoc = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = lat
                        longitude = lon
                        altitude = 15.0
                        accuracy = 1.0f
                        bearing = 0.0f
                        speed = 0.0f
                        time = now
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            elapsedRealtimeNanos = elapsedNanos
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            bearingAccuracyDegrees = 0.1f
                            speedAccuracyMetersPerSecond = 0.1f
                            verticalAccuracyMeters = 0.5f
                        }
                    }
                    fusedClient?.setMockLocation(fusedLoc)
                } catch (_: Throwable) {}

                // Repeat every 300ms (keeps GPS stream alive even under heavy foreground app loads)
                if (isRunning) {
                    locationHandler?.postDelayed(this, 300L)
                }
            }
        }

        locationHandler?.post(mockRunnable!!)
    }

    override fun onDestroy() {
        isRunning = false
        mockRunnable?.let { locationHandler?.removeCallbacks(it) }

        try {
            fusedClient?.setMockMode(false)
        } catch (_: Throwable) {}

        for (provider in providers) {
            try {
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {}
        }

        try {
            handlerThread?.quitSafely()
        } catch (_: Throwable) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {}

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
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "fake_gps_channel")
            .setContentTitle("Rhpatch Fake GPS Aktif")
            .setContentText("Memancarkan lokasi palsu secara konsisten (Fused + Hardware GPS)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
