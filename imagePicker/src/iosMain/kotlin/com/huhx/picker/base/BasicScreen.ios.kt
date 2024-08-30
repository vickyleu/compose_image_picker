package com.huhx.picker.base

import cafe.adriel.voyager.navigator.tab.Tab

@Suppress("unused")
internal actual interface BasicScreenSerializer

@Suppress("unused")
internal actual interface TabImpl : Tab

@Suppress("unused")
actual annotation class ParcelizeImpl actual constructor() {}