package com.huhx.app.view

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.parcelize.Parcelize
import java.io.Serializable

actual interface BasicScreenSerializer : Serializable
actual typealias ParcelizeImpl = Parcelize

actual abstract class TabImpl : Tab, Parcelable {
    override val key: ScreenKey = uniqueScreenKey

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
               return object :TabImpl(){
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