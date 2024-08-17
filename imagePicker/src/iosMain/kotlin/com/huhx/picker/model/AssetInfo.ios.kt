package com.huhx.picker.model

import coil3.Uri
import coil3.toUri
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.provider.toPHAsset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHContentEditingInputRequestOptions
import platform.Photos.requestContentEditingInputWithOptions

actual class MediaStoreKMP {
    actual object Files {
        actual object FileColumns {
            actual val MEDIA_TYPE_IMAGE: Int
                get() = PHAssetMediaTypeImage.toInt()
            actual val MEDIA_TYPE_VIDEO: Int
                get() = PHAssetMediaTypeVideo.toInt()
        }
    }
}


actual class DateTimeFormatterKMP(private val formatter: NSDateFormatter) {
    actual companion object {
        private fun LocalDateTime.toNSDateComponentsImpl(): NSDateComponents {
            val components = NSDateComponents()
            components.year = this.year.toLong()
            components.month = this.monthNumber.toLong()
            components.day = this.dayOfMonth.toLong()
            components.hour = this.hour.toLong()
            components.minute = this.minute.toLong()
            components.second = this.second.toLong()
            return components
        }

        actual fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String {
            val nsDateComponents = this.toNSDateComponentsImpl()
            val calendar = NSCalendar.currentCalendar
            return (nsDateComponents.date?.let {
                formatter.formatter.stringFromDate(it)
            } ?: calendar.dateFromComponents(nsDateComponents)?.let {
                formatter.formatter.stringFromDate(it)
            } ?: "").apply {
                println("Formatted date: $this ${formatter.formatter.dateFormat}")
            }
        }

        actual fun ofPattern(pattern: String): DateTimeFormatterKMP {
            return DateTimeFormatterKMP(NSDateFormatter().apply {
                dateFormat = pattern
                locale = NSLocale(localeIdentifier = "zh_CN")
            })
        }
    }

    actual fun format(localDateTime: LocalDateTime): String {
        val nsDateComponents = localDateTime.toNSDateComponents()
        val calendar = NSCalendar.currentCalendar
        return nsDateComponents.date?.let {
            formatter.stringFromDate(it)
        } ?: calendar.dateFromComponents(nsDateComponents)?.let {
            formatter.stringFromDate(it)
        } ?: ""
    }

    actual fun parse(time: String): LocalDateTime {
        val date = formatter.dateFromString(time) ?: return LocalDateTime(0, 1, 1, 1, 1)
        val components = NSCalendar.currentCalendar.components(
            unitFlags = NSCalendarUnitYear or
                    NSCalendarUnitMonth or
                    NSCalendarUnitDay or
                    NSCalendarUnitHour or
                    NSCalendarUnitMinute or
                    NSCalendarUnitSecond,
            date
        )
        return LocalDateTime(
            year = components.year.toInt(),
            monthNumber = components.month.toInt(),
            dayOfMonth = components.day.toInt(),
            hour = components.hour.toInt(),
            minute = components.minute.toInt(),
            second = components.second.toInt()
        )
    }


}

actual suspend fun AssetInfo.toUri(): Uri {
    val uriStr = this.uriString
    val completer = CompletableDeferred<String>()
    withContext(Dispatchers.IO) {
        val filePathByImpl = if(this@toUri is AssetLoader.AssetInfoImpl){
            this@toUri._filePath
        }else{
            this@toUri.filepath
        }
        if (filePathByImpl.isNotBlank()) {
            completer.complete(filePathByImpl)
        } else {
            if (uriStr.startsWith("phasset://")) {
                this@toUri.size.toInt()
                val localIdentifier = uriStr.substring("phasset://".length)
                val asset = localIdentifier.toPHAsset() ?: return@withContext run {
                    completer.complete(uriStr)
                }
                asset.requestContentEditingInputWithOptions(options = PHContentEditingInputRequestOptions.new()) { contentEditingInput, info ->
                    val imageURL = contentEditingInput?.fullSizeImageURL?.absoluteString ?: ""
                    println("imageURL: $imageURL")
                    if(this@toUri is AssetLoader.AssetInfoImpl){
                        this@toUri.setFilePath(imageURL)
                    }
                    completer.complete(imageURL)
                }
            } else {
                completer.complete(uriStr)
            }
        }
    }
    val imageURL = completer.await()
    return imageURL.toUri()
}
