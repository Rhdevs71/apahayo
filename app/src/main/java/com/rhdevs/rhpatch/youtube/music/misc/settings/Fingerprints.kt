package com.rhdevs.rhpatch.youtube.music.misc.settings

import com.rhdevs.rhpatch.youtube.findClassDirect
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.findMethodListDirect
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceFragmentCompatClass
import org.luckypray.dexkit.query.enums.StringMatchType

val PreferenceFragmentCompat_setPreferencesFromResource = findMethodDirect {
    PreferenceFragmentCompatClass().let { preferenceFragmentCompat ->
        preferenceFragmentCompat.findMethod {
            matcher {
                returnType = "void"
                paramTypes("int", "String")
            }
        }.singleOrNull() ?: preferenceFragmentCompat.findMethod {
            matcher {
                name = "setPreferencesFromResource"
            }
        }.single()
    }
}

val googleApiActivityClass = findClassDirect {
    findClass {
        matcher {
            className(".GoogleApiActivity", StringMatchType.EndsWith)
        }
    }.single()
}

internal val googleApiActivityFingerprint = findMethodDirect {
    googleApiActivityClass().findMethod { matcher { name = "onCreate" } }.single()
}

val googleApiActivityNOTonCreate = findMethodListDirect {
    googleApiActivityClass().methods.filter { it.name != "onCreate" && it.isMethod }
}
