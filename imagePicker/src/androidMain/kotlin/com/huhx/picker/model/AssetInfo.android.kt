package com.huhx.picker.model

import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter

actual class MediaStoreKMP {
    actual object Files {
        actual object FileColumns {
            actual val MEDIA_TYPE_IMAGE: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            actual val MEDIA_TYPE_VIDEO: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        }
    }
}

actual class ImageBitmapFactory {
    actual companion object {
        actual fun decodeFile(uriString: String): ImageBitmap {
            return BitmapFactory.decodeFile(uriString).asImageBitmap()
        }
    }
}

actual class DateTimeFormatterKMP(private val formatter: DateTimeFormatter) {
    actual companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        actual fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String {
            return this.toJavaLocalDateTime().format(formatter.formatter)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        actual fun ofPattern(pattern: String): DateTimeFormatterKMP {
            return DateTimeFormatterKMP(DateTimeFormatter.ofPattern(pattern))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    actual fun format(localDateTime: LocalDateTime): String {
        return localDateTime.toJavaLocalDateTime().format(formatter)
    }

}