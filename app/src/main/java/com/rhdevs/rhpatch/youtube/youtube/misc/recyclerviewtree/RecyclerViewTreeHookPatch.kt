package com.rhdevs.rhpatch.youtube.youtube.misc.recyclerviewtree

import android.support.v7.widget.RecyclerView
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.scopedHook

val addRecyclerViewTreeHook = mutableListOf<(RecyclerView) -> Unit>()

val recyclerViewTreeHook = patch {
    ::recyclerViewTreeObserverFingerprint.hookMethod(scopedHook(::RecyclerView_addOnScrollListener.member) {
        before {
            val recyclerView = it.thisObject as RecyclerView
            addRecyclerViewTreeHook.forEach { hook ->
                hook(recyclerView)
            }
        }
    })
}
