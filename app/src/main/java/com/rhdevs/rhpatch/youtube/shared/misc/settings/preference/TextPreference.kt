@file:Suppress("DEPRECATION", "DiscouragedApi")

package com.rhdevs.rhpatch.youtube.shared.misc.settings.preference

import android.preference.Preference
import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.ResettableEditTextPreference

class TextPreference(
    key: String? = null,
    titleKey: String = "${key}_title",
    summaryKey: String? = "${key}_summary",
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
    tag: Class<out Preference> = ResettableEditTextPreference::class.java,
    val inputType: InputType = InputType.TEXT
) : BasePreference(key, titleKey, summaryKey, icon, iconBold, layout, tag) {
}
