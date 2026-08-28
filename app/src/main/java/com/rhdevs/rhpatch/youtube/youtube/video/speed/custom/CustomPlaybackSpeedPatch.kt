package com.rhdevs.rhpatch.youtube.youtube.video.speed.custom

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.components.PlaybackSpeedMenuFilter
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.playback.speed.CustomPlaybackSpeedPatch
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.playback.speed.CustomPlaybackSpeedPatch.customPlaybackSpeeds
import de.robv.android.xposed.XC_MethodReplacement
import com.rhdevs.rhpatch.invokeOriginalMethod
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.InputType
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.TextPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.litho.filter.LithoFilter
import com.rhdevs.rhpatch.youtube.shared.misc.litho.filter.addLithoFilter
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.is_20_34_or_greater
import com.rhdevs.rhpatch.youtube.youtube.misc.recyclerviewtree.addRecyclerViewTreeHook
import com.rhdevs.rhpatch.youtube.youtube.misc.recyclerviewtree.recyclerViewTreeHook
import com.rhdevs.rhpatch.youtube.youtube.video.information.doOverridePlaybackSpeed
import com.rhdevs.rhpatch.youtube.youtube.video.speed.settingsMenuVideoSpeedGroup
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.scopedHook
import java.lang.reflect.Method

private var INSTANCE: Any? = null
private lateinit var showOldPlaybackSpeedMenuMethod: Method
fun doShowOldPlaybackSpeedMenu() {
    if (INSTANCE != null) showOldPlaybackSpeedMenuMethod(INSTANCE)
}

val CustomPlaybackSpeed = patch(
    description = "Adds custom playback speed options.",
) {
    dependsOn(
        LithoFilter, recyclerViewTreeHook
    )

    settingsMenuVideoSpeedGroup.addAll(
        listOf(
            SwitchPreference("morphe_custom_speed_menu"),
            SwitchPreference("morphe_restore_old_speed_menu"),
            TextPreference(
                "morphe_custom_playback_speeds",
                inputType = InputType.TEXT_MULTI_LINE
            ),
        )
    )

    // Override the min/max speeds that can be used.
    ::speedLimiterFingerprint.hookMethod(scopedHook(::clampFloatFingerprint.member) {
        before {
            it.args[1] = 0.0f
            it.args[2] = 8.0f
        }
    })

    // Turn off client side flag that use server provided min/max speeds.
    if (is_20_34_or_greater) {
        ServerSideMaxSpeedFeatureFlagFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }

    // region Force old playback speed.

    // Replace the speeds float array with custom speeds.

    ::speedArrayGeneratorFingerprint.hookMethod {
        val source = ::speedsFloatArrayField.field.get(null) as FloatArray
        val chunkSize = source.size
        before {
            /*
            * The method hardcoded array length (determined during compilation/obfuscation)
            * to iterate through the playback speed float values in PlayerConfigModel.
            * To bypass this constraint,
            * We divide the custom speeds into chunks matching the original array's size,
            * repeatedly populate the original static float array,
            * Invoke the original method for each chunk to transform raw floats into the expected Model objects.
            * */
            val result = customPlaybackSpeeds.asIterable().chunked(chunkSize).map { chunk ->
                chunk.forEachIndexed { index, value -> source[index] = value }
                (it.invokeOriginalMethod() as Array<*>)
            }.flatMap { it.asIterable() }

            val arr = java.lang.reflect.Array.newInstance(result.first()!!.javaClass, result.size)
            result.forEachIndexed { i, v -> java.lang.reflect.Array.set(arr, i, v) }

            it.result = arr
        }
    }

    GetOldPlaybackSpeedsFingerprint.hookMethod {
        before {
            INSTANCE = it.thisObject
        }
    }
    showOldPlaybackSpeedMenuMethod = ::showOldPlaybackSpeedMenuFingerprint.method


    // Fix restore old playback speed menu.
    // TODO

    // endregion

    // Close the unpatched playback dialog and show the custom speeds.
    addRecyclerViewTreeHook.add { CustomPlaybackSpeedPatch.onFlyoutMenuCreate(it) }

    // Required to check if the playback speed menu is currently shown.
    addLithoFilter(PlaybackSpeedMenuFilter())

    // region Custom tap and hold 2x speed.
    val tapAndHoldPath = ThreadLocal<Boolean>()
    ::onSpeedTapAndHoldFingerprint.hookMethod {
        before { tapAndHoldPath.remove() }
        after {
            if (tapAndHoldPath.get() == true) {
                doOverridePlaybackSpeed(CustomPlaybackSpeedPatch.getTapAndHoldSpeed())
            }
        }
    }
    ::onSpeedTapAndHoldFingerprint.hookMethod(
        scopedHook(::getPlaybackSpeedMethodReference.member) {
            before {
                tapAndHoldPath.set(true)
            }
        })

    settingsMenuVideoSpeedGroup.add(
        TextPreference("morphe_speed_tap_and_hold", inputType = InputType.NUMBER_DECIMAL),
    )

    // endregion
}

