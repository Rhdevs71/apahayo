package com.rhdevs.rhpatch.revanced.googlephotos.misc.backup

import com.rhdevs.rhpatch.morphe.Fingerprint

internal object isDCIMFolderBackupControlMethod : Fingerprint(
    strings = listOf(
        "/dcim",
        "/mars_files/"
    ),
    returnType = "Z"
)
