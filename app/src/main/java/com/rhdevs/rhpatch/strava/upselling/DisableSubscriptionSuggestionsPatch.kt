package com.rhdevs.rhpatch.strava.upselling

import com.rhdevs.rhpatch.getObjectFieldOrNullAs
import com.rhdevs.rhpatch.patch
import java.util.Collections

val DisableSubscriptionSuggestions = patch(
    name = "Disable subscription suggestions",
) {
    ::getModulesFingerprint.hookMethod {
        before { param ->
            val pageValue = param.thisObject.getObjectFieldOrNullAs<String>("page") ?: return@before
            if (pageValue.contains("_upsell") || pageValue.contains("promo")) {
                param.result = Collections.EMPTY_LIST
            }
        }
    }
}
