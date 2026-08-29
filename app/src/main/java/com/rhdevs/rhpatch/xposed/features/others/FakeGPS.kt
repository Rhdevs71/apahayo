package com.rhdevs.rhpatch.xposed.features.others

import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.FeatureLoader
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class FakeGPS(
    loader: ClassLoader,
    preferences: SharedPreferences
) : Feature(loader, preferences) {

    override fun getPluginName(): String = "Fake GPS"

    override fun doHook() {
        if (!prefs.getBoolean("fake_gps_enable", false)) return
        
        val mode = prefs.getString("fake_gps_mode", "1") ?: "1"
        if (mode == "1") {
            hookLocationManager()
        }
    }

    private fun hookLocationManager() {
        try {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = getFakeLocation()
                    }
                }
            )

            // Overriding requestLocationUpdates is more complex because it passes a LocationListener.
            // But for simple Fake GPS, changing getLastKnownLocation and hooking Location itself might be enough.
            XposedBridge.log("Rhpatch FakeGPS: LocationManager hooked successfully")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch FakeGPS: LocationManager hook failed -> ${e.message}")
        }
    }

    private fun getFakeLocation(): Location {
        // Read lat long from prefs
        val lat = prefs.getFloat("fake_gps_lat", -6.200000f).toDouble()
        val lng = prefs.getFloat("fake_gps_lon", 106.816666f).toDouble()

        val location = Location(LocationManager.GPS_PROVIDER)
        location.latitude = lat
        location.longitude = lng
        location.accuracy = 10.0f
        location.time = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        }
        return location
    }
}
