package com.rhdevs.rhpatch.tiktok.feed

import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint

val fetchFeedListMethod = findMethodDirect(
    fingerprint {
        name("fetchFeedList")
        definingClass(".*FeedApiService.*")
    }
)
