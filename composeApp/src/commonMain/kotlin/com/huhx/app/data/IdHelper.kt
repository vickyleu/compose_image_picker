package com.huhx.app.data

object IdHelper {
    private var id: Int = 0

    fun nextID(): Int = id++
}