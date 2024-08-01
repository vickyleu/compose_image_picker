package com.huhx.app.data

import kotlin.jvm.Synchronized

object IdHelper {
    private var id: Int = moments.size

    @Synchronized
    fun nextID(): Int = id++
}