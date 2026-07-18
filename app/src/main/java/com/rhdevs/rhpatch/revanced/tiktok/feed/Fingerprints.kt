package com.rhdevs.rhpatch.revanced.tiktok.feed

import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint

val fetchFeedListMethod = findMethodDirect(
    fingerprint {
        name("fetchFeedList")
        definingClass(".*FeedApiService.*")
    }
)
