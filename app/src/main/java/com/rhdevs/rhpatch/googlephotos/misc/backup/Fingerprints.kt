package com.rhdevs.rhpatch.googlephotos.misc.backup

import com.rhdevs.rhpatch.youtube.Fingerprint

internal object isDCIMFolderBackupControlMethod : Fingerprint(
    strings = listOf(
        "/dcim",
        "/mars_files/"
    ),
    returnType = "Z"
)
