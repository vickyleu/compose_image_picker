package com.huhx.picker.model

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.datetime.LocalDateTime

actual class MediaStoreKMP {
    actual object Files {
        actual object FileColumns {
            actual val MEDIA_TYPE_IMAGE: Int
                get() = TODO("Not yet implemented")
            actual val MEDIA_TYPE_VIDEO: Int
                get() =TODO("Not yet implemented")
        }
    }
}

actual class ImageBitmapFactory {
    actual companion object {
        actual fun decodeFile(uriString: String): ImageBitmap {
            TODO("Not yet implemented")
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