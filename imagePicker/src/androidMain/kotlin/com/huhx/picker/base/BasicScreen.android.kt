package com.huhx.picker.base

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.parcelize.Parcelize
import java.io.Serializable

internal actual interface BasicScreenSerializer : Serializable
actual typealias ParcelizeImpl = Parcelize

internal actual interface TabImpl : Tab, Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {

    }

    companion object {
        @JvmField
        @Suppress("unused")
        val CREATOR: Parcelable.Creator<TabImpl> = object : Parcelable.Creator<TabImpl> {
            override fun createFromParcel(parcel: Parcel): TabImpl {
                // 需要返回具体的子类实例
                return object : TabImpl {
                    override val options: TabOptions
                        @Composable
                        get() = TabOptions(index = 0u, title = "${this::class.simpleName}")

                    @Composable
                    override fun Content() {

                    }
                }
            }

            override fun newArray(size: Int): Array<TabImpl?> {
                return arrayOfNulls(size)
            }
        }
    }
}