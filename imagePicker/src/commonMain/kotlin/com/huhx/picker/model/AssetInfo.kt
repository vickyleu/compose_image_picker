package com.huhx.picker.model

import androidx.compose.ui.graphics.ImageBitmap
import com.huhx.picker.model.DateTimeFormatterKMP.Companion.format
import com.huhx.picker.util.StringUtil
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

expect class MediaStoreKMP{
    object Files {
        object FileColumns {
            val MEDIA_TYPE_IMAGE: Int
            val MEDIA_TYPE_VIDEO: Int
        }
    }
}
expect class ImageBitmapFactory {
    companion object {
        fun decodeFile(uriString: String): ImageBitmap
    }
}

expect class DateTimeFormatterKMP {
    companion object {
        fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String
        fun ofPattern(pattern: String): DateTimeFormatterKMP
    }

    fun format(localDateTime: LocalDateTime): String
}

data class AssetInfo(
    val id: Long,
    val uriString: String,
    val filepath: String,
    val filename: String,
    val directory: String,
    val size: Long,
    val mediaType: Int,
    val mimeType: String,
    val duration: Long? = null,
    val date: Long,
) {
    fun isImage(): Boolean {
        return mediaType == MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_IMAGE
    }

    fun isGif(): Boolean {
        return mimeType == "image/gif"
    }

    fun isVideo(): Boolean {
        return mediaType == MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_VIDEO
    }

    fun getBitmap(): ImageBitmap {
        return ImageBitmapFactory.decodeFile(uriString)
    }

    val dateString: String get() {
        val instant = Instant.fromEpochMilliseconds(date)
      return  instant.toLocalDateTime(TimeZone.UTC).toString()
//        return instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }

    val resourceType: AssetResourceType = AssetResourceType.fromFileName(filename)

    // todo: 这种方式还是存在问题
    val randomName: String = run {
        val formatter = DateTimeFormatterKMP.ofPattern("yyyyMMddHHmmss")
        val dateTimeString = Clock.System.now().toLocalDateTime(TimeZone.UTC).format(formatter)
        val fileExtension = filename.split(".").last()
        val randomString = StringUtil.randomNumeric(6)

        "${dateTimeString}$randomString.$fileExtension"
    }

    fun formatDuration(): String {
        if (duration == null) {
            return ""
        }
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 % 60

        return "${minutes.prefixZero()}:${seconds.prefixZero()}"
    }

    private fun Long.prefixZero(): String {
        return if (this < 10) "0$this" else "$this"
    }
}