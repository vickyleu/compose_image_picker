package com.huhx.picker.model

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
                println("""Setting date format to "$pattern"""")
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