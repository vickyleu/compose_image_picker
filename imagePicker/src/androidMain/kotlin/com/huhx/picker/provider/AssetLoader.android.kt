package com.huhx.picker.provider

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.fetch.FetchResult
import coil3.request.Options
import coil3.toAndroidUri
import coil3.toCoilUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.RequestType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual abstract class AssetLoader {

    actual companion object {
        actual suspend fun insertImage(context: PlatformContext): Uri? {
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "camera-${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            return context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )?.toCoilUri()
        }

        actual suspend fun deleteByUri(context: PlatformContext, uri: Uri) {
            context.contentResolver.delete(uri.toAndroidUri(), null, null)
        }

        actual suspend fun findByUri(context: PlatformContext, uri: Uri): AssetInfo? {
            val cursor =
                context.contentResolver.query(
                    uri.toAndroidUri(),
                    projection,
                    null,
                    null,
                    null,
                    null
                )
            cursor?.use {
                val indexId = it.getColumnIndex(projection[0])
                val indexFilename = it.getColumnIndex(projection[1])
                val indexDate = it.getColumnIndex(projection[2])
                val indexMediaType = it.getColumnIndex(projection[3])
                val indexMimeType = it.getColumnIndex(projection[4])
                val indexSize = it.getColumnIndex(projection[5])
                val indexDuration = it.getColumnIndex(projection[6])
                val indexDirectory = it.getColumnIndex(projection[7])
                val indexFilepath = it.getColumnIndex(projection[8])

                if (it.moveToNext()) {
                    val id = it.getLong(indexId)
                    val mediaType = it.getInt(indexMediaType)
                    val filepathIndex =
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                            it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        } else {
                            it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        }

                    return AssetInfo(
                        id = id.toString(),
                        uriString = it.getString(filepathIndex),
                        filepath = it.getString(indexFilepath),
                        filename = it.getString(indexFilename),
                        date = it.getLong(indexDate),
                        mediaType = mediaType,
                        mimeType = it.getString(indexMimeType),
                        size = it.getLong(indexSize),
                        duration = it.getLong(indexDuration),
                        directory = it.getString(indexDirectory),
                    )
                }
            }
            return null
        }

        actual suspend fun load(
            context: PlatformContext,
            requestType: RequestType,
            onlyLast: Boolean
        ): List<AssetInfo> {
            val assets = mutableListOf<AssetInfo>()
            val cursor = createCursor(context, requestType, onlyLast)
            val completer = CompletableDeferred<MutableList<AssetInfo>>()
            withContext(Dispatchers.IO) {
                cursor?.use {
                    val indexId = it.getColumnIndex(projection[0])
                    val indexFilename = it.getColumnIndex(projection[1])
                    val indexDate = it.getColumnIndex(projection[2])
                    val indexMediaType = it.getColumnIndex(projection[3])
                    val indexMimeType = it.getColumnIndex(projection[4])
                    val indexSize = it.getColumnIndex(projection[5])
                    val indexDuration = it.getColumnIndex(projection[6])
                    val indexDirectory = it.getColumnIndex(projection[7])
                    val indexFilepath = it.getColumnIndex(projection[8])

                    while (it.moveToNext()) {
                        val id = it.getLong(indexId)
                        val mediaType = it.getInt(indexMediaType)
                        val contentUri =
                            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                            } else {
                                ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                            }
                        assets.add(
                            AssetInfo(
                                id = id.toString(),
                                uriString = contentUri.toString(),
                                filepath = it.getString(indexFilepath),
                                filename = it.getString(indexFilename),
                                date = it.getLong(indexDate),
                                mediaType = mediaType,
                                mimeType = it.getString(indexMimeType),
                                size = it.getLong(indexSize),
                                duration = it.getLong(indexDuration),
                                directory = it.getStringOrNull(indexDirectory) ?: "",
                            )
                        )
                    }
                }
                completer.complete(assets)
            }
            completer.await()
            return assets
        }

        private fun createCursor(
            context: PlatformContext,
            requestType: RequestType,
            onlyLast: Boolean
        ): Cursor? {
            val mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE
            val image = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            val video = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

            val selection = when (requestType) {
                RequestType.COMMON -> Selection(
                    selection = "$mediaType=? OR $mediaType=?",
                    arguments = listOf(image.toString(), video.toString())
                )

                RequestType.IMAGE -> Selection(
                    selection = "$mediaType=?",
                    arguments = listOf(image.toString())
                )

                RequestType.VIDEO -> Selection(
                    selection = "$mediaType=?",
                    arguments = listOf(video.toString())
                )
            }
            return createMediaCursor(context, selection, onlyLast)
        }

        private fun createMediaCursor(
            context: Context,
            selection: Selection,
            onlyLast: Boolean
        ): Cursor? {
            val cursor = context.contentResolver.query(
                /* uri = */ MediaStore.Files.getContentUri("external"),
                /* projection = */ projection,
                /* selection = */ selection.selection,
                /* selectionArgs = */ selection.arguments.toTypedArray(),
                /* sortOrder = */ "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )
            return if (cursor != null && onlyLast) {
                // 如果只需要最新的一条记录
                MatrixCursor(cursor.columnNames).apply {
                    if (cursor.moveToFirst()) {
                        addRow((0 until cursor.columnCount).map { cursor.getString(it) }
                            .toTypedArray())
                    }
                }
            } else {
                cursor
            }
        }
        /*private fun createMediaCursor(context: PlatformContext, selection: Selection,onlyLast:Boolean): Cursor? {
            return context.contentResolver.query(
                *//* uri = *//* MediaStore.Files.getContentUri("external"),
                *//* projection = *//* projection,
                *//* selection = *//* selection.selection,
                *//* selectionArgs = *//* selection.arguments.toTypedArray(),
                *//* sortOrder = *//* "${MediaStore.Files.FileColumns.DATE_ADDED} DESC ${if(onlyLast) "LIMIT 1" else ""}",
                *//*cancellationSignal=*//*null
            )
        }*/

        private data class Selection(val selection: String, val arguments: List<String>)

        private val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
        )
    }
}


actual class PHAssetFetcherBridge {
    actual suspend fun fetchFromPhotoLibrary(
        uri: Uri,
        fileName: String,
        options: Options,
        imageLoader: ImageLoader
    ): FetchResult {
        // 这里需要实现从iOS照片库获取PHAsset的逻辑
        // 由于iOS平台特定的实现，这里需要使用平台特定的API来获取PHAsset
        throw NotImplementedError("This method should be implemented for iOS platform")
    }
}