package com.wmods.wppenhacer.provider

import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import com.wmods.wppenhacer.BuildConfig

import com.rhdevs.rhpatch.appPatchConfigurations

class RemotePreferenceProvider : RemotePreferenceProvider(
    BuildConfig.APPLICATION_ID + ".preferences",
    arrayOf(BuildConfig.APPLICATION_ID + "_preferences", "prefs") + appPatchConfigurations.map { it.packageName }.toTypedArray()
) {
    override fun checkAccess(prefFileName: String, prefKey: String, write: Boolean): Boolean {
        return !write
    }
}