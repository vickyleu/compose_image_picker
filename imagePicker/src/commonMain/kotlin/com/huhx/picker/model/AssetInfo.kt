package com.huhx.picker.model

import coil3.PlatformContext
import coil3.Uri
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

expect class MediaStoreKMP {
    object Files {
        object FileColumns {
            val MEDIA_TYPE_IMAGE: Int
            val MEDIA_TYPE_VIDEO: Int
        }
    }
}


expect class DateTimeFormatterKMP {
    companion object {
        fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String
        fun ofPattern(pattern: String): DateTimeFormatterKMP
    }

    fun format(localDateTime: LocalDateTime): String
    fun parse(time: String): LocalDateTime
}

open class AssetInfo(
    val id: String,
    val uriString: String,
    open val filepath: String,
    val filename: String,
    val directory: String,
    open val size: Long,
    val mediaType: Int,
    val mimeType: String,
    val duration: Long? = null,
    val date: Long,
) {
    fun isImage(): Boolean {
        return mediaType == MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_IMAGE
    }

    open fun checkIfVideoNotDownload(context: PlatformContext,callback: (Uri) -> Unit = {}) {
    }


    fun isGif(): Boolean {
        return mimeType == "image/gif"
    }

    fun isVideo(): Boolean {
        return mediaType == MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_VIDEO
    }

    companion object {
        private val formatter = DateTimeFormatterKMP.ofPattern("yyyy年MM月dd日")
    }

    val dateString: String
        get() {
            val instant = Instant.fromEpochMilliseconds(date)
            return formatter.format(instant.toLocalDateTime(TimeZone.UTC))
//        return instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

    val resourceType: AssetResourceType = AssetResourceType.fromFileName(filename)


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

    data object Camera : AssetInfo(
        id = "-1",
        uriString = "",
        filepath = "",
        filename = "",
        directory = "",
        size = 0,
        mediaType = MediaStoreKMP.Files.FileColumns.MEDIA_TYPE_IMAGE,
        mimeType = "image/png",
        date = 0,
    )
}


expect suspend fun AssetInfo.toUri(): Uri