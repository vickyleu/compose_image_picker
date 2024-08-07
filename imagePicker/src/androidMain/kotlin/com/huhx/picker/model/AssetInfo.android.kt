package com.huhx.picker.model

import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import coil3.Uri
import coil3.toUri
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual class MediaStoreKMP {
    actual object Files {
        actual object FileColumns {
            actual val MEDIA_TYPE_IMAGE: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            actual val MEDIA_TYPE_VIDEO: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        }
    }
}


actual class DateTimeFormatterKMP(private val formatter: Any) {
    actual companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private val reservedPattern = DateTimeFormatter.ofPattern("yyyy年MM月dd日",Locale.getDefault())
        @RequiresApi(Build.VERSION_CODES.O)
        actual fun LocalDateTime.format(formatter: DateTimeFormatterKMP): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (formatter.formatter as DateTimeFormatter).let {
                    val format = it
                    this.toJavaLocalDateTime().let {
                       try {
                            it.format(format).toString()
                        }catch (e:Exception){
                            reservedPattern.format(it.toLocalDate().atStartOfDay())
                        }
                    }
                }
            } else {
                (formatter.formatter as SimpleDateFormat).let {
                    val date = Date(this.toJavaLocalDateTime().second.times(1000).toLong())
                    it.format(date)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        actual fun ofPattern(pattern: String): DateTimeFormatterKMP {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatterKMP(DateTimeFormatter.ofPattern(pattern))
            } else {
                DateTimeFormatterKMP(SimpleDateFormat(pattern, Locale.getDefault()))
            }
        }
    }

    actual fun parse(time: String): LocalDateTime {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localDateTime =   try {
                java.time.LocalDateTime.from((formatter as DateTimeFormatter).parse(time))
            }catch (e:Exception){
                reservedPattern.parse(time).let {
                    val ld = java.time.LocalDate.from(it)
                    ld.atStartOfDay()
                }
            }
            localDateTime.toKotlinLocalDateTime()
        } else {
            val date = (formatter as SimpleDateFormat).parse(time)
                ?: throw IllegalArgumentException("无法解析时间字符串：$time")
            val calendar = Calendar.getInstance().apply {
                this.time = date
            }
            LocalDateTime(
                year = calendar.get(Calendar.YEAR),
                monthNumber = calendar.get(Calendar.MONTH) + 1,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
                second = calendar.get(Calendar.SECOND),
                nanosecond = 0 // SimpleDateFormat 不支持纳秒
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    actual fun format(localDateTime: LocalDateTime): String {
        return localDateTime.format(this)
    }

}

actual suspend fun AssetInfo.toUri(): Uri {
    return this.filepath.toUri()
}