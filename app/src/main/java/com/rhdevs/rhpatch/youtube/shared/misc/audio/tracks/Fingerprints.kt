package com.rhdevs.rhpatch.youtube.shared.misc.audio.tracks

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.findClassDirect
import com.rhdevs.rhpatch.youtube.findMethodListDirect
import com.rhdevs.rhpatch.youtube.fingerprint

internal val formatStreamModelToStringFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/String;")
    name("toString")
    strings(
        // Strings are partial matches.
        "isDefaultAudioTrack=",
        "audioTrackId="
    )
}

val formatStringModelClass = findClassDirect {
    formatStreamModelToStringFingerprint().declaredClass!!
}

/*
* isDefaultAudioTrack
* audioTrackId
* audioTrackDisplayName
* */
val getFormatStreamModelGetter = findMethodListDirect {
    val formatStringModelClass = formatStringModelClass().name
    formatStreamModelToStringFingerprint().invokes.windowed(3).first {
        it[0].returnTypeName == "boolean" &&
                it[1].returnTypeName == "java.lang.String" &&
                it[2].returnTypeName == "java.lang.String"
    }.also {
        it.forEach { m ->
            require(m.paramCount == 0) { "Expected no parameters for FormatStreamModel getter methods" }
            require(m.declaredClassName == formatStringModelClass) { "Expected FormatStreamModel instance method" }
        }
    }
}

internal const val AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG = 45666189L
