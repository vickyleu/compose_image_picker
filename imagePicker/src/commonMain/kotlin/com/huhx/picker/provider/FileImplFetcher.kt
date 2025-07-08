package com.huhx.picker.provider

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.github.jing332.filepicker.base.FileImpl
import com.github.jing332.filepicker.base.absolutePath
import com.github.jing332.filepicker.base.inputStream
import com.github.jing332.filepicker.base.isFile
import com.github.jing332.filepicker.base.name
import com.github.jing332.filepicker.base.source
import com.github.jing332.filepicker.base.uri
import okio.FileSystem
import okio.SYSTEM
import okio.buffer

class FileImplFetcher(
    private val file: FileImpl,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher{
    init {
        println("FileImplFetcher initialized with file: ${file.name}, exists=${file.exists()}, isFile=${file.isFile}")
    }
    private val pHAssetFetcherBridge = PHAssetFetcherBridge()
    override suspend fun fetch(): FetchResult {
        return try {
            // 检查是否是iOS照片库路径，如果是则桥接到PHAsset
            val filePath = file.absolutePath
            if (filePath.contains("/var/mobile/Media/DCIM/") || filePath.contains("/DCIM/")) {
                // iOS照片库路径，需要特殊处理
                return fetchFromPhotoLibrary(file.uri(),options,imageLoader)
            }

            // 普通文件路径，直接读取
            val inputStream = file.inputStream()
            val bufferedSource = inputStream.source().buffer()
            SourceFetchResult(
                source = ImageSource(
                    source = bufferedSource,
                    fileSystem = FileSystem.SYSTEM,
                    metadata = null,
                ),
                mimeType = getMimeType(file.name),
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            throw IllegalStateException("Unable to read file: ${file.name}", e)
        }
    }

    // 桥接到照片库的方法，需要平台特定实现
    private suspend fun fetchFromPhotoLibrary(
        uri: Uri,
        options: Options,
        imageLoader: ImageLoader
    ): FetchResult {
        return pHAssetFetcherBridge.fetchFromPhotoLibrary(uri, file.name,options, imageLoader)
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            else -> "image/*"
        }
    }


}

class FileImplFetcherFactory : Fetcher.Factory<FileImpl> {
    override fun create(
        data: FileImpl,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher? {
        println("FileImplFetcherFactory.create called with: $data, exists=${data.exists()}, isFile=${data.isFile}")
        // 判断是否是我们需要处理的FileImpl
        if (data.exists() && data.isFile) {
            return FileImplFetcher(data,options, imageLoader)
        }
        println("FileImplFetcherFactory create are you sure?")
        return null
    }
}

expect class PHAssetFetcherBridge() {
    suspend fun fetchFromPhotoLibrary(
        uri: Uri,
        fileName: String,
        options: Options,
        imageLoader: ImageLoader
    ): FetchResult
}
