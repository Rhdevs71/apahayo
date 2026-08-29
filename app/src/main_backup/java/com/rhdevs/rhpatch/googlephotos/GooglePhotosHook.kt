package com.rhdevs.rhpatch.googlephotos

import com.rhdevs.rhpatch.googlephotos.misc.backup.EnableDCIMFoldersBackupControl
import com.rhdevs.rhpatch.googlephotos.misc.features.SpoofFeaturesPatch

val GooglePhotosPatches = arrayOf(
    SpoofFeaturesPatch,
    EnableDCIMFoldersBackupControl,
)
