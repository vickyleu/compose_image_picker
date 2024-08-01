package com.huhx.picker.model

import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.datetime.LocalDateTime

typealias MediaStoreKMP = MediaStore

actual class ImageBitmapFactory {
    actual companion object {
        actual fun decodeFile(uriString: String): ImageBitmap {
            return BitmapFactory.decodeFile(uriString).asImageBitmap()
        }
    }
}

actual class DateTimeFormatterKMP {
    actual companion object {
        actual fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String {
            TODO("Not yet implemented")
        }

        actual fun ofPattern(pattern: String): DateTimeFormatterKMP {
            TODO("Not yet implemented")
        }
    }

    actual fun format(localDateTime: LocalDateTime): String {
        TODO("Not yet implemented")
    }

}