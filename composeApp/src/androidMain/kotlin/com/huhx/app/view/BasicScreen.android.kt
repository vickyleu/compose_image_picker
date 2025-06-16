package com.huhx.app.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

actual interface BasicScreenSerializer : Serializable
actual typealias ParcelizeImpl = Parcelize