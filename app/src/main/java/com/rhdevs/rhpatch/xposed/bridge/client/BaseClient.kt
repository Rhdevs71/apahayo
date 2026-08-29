package com.rhdevs.rhpatch.xposed.bridge.client

import com.rhdevs.rhpatch.xposed.bridge.WaeIIFace

abstract class BaseClient {
    abstract val service: WaeIIFace?

    abstract suspend fun connect(): Boolean

    abstract fun tryReconnect()
}
