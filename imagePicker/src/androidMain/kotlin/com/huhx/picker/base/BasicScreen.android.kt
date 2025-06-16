package com.huhx.picker.base

import kotlinx.parcelize.Parcelize
import java.io.Serializable

internal actual interface BasicScreenSerializer : Serializable
actual typealias ParcelizeImpl = Parcelize