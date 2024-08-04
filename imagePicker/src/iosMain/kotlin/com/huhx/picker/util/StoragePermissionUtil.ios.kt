package com.huhx.picker.util

import androidx.lifecycle.Lifecycle
import coil3.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class StoragePermissionUtil actual constructor(
    private val context: PlatformContext,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope
) {

    actual suspend fun checkStoragePermission(): Boolean {
        return when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                true
            }

            else -> {
                false
            }
        }
    }

    actual suspend fun checkCameraPermission(): Boolean {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            // AVAuthorizationStatusNotDetermined 未决定
            // AVAuthorizationStatusRestricted 受限制
            // AVAuthorizationStatusDenied 拒绝
            // AVAuthorizationStatusAuthorized 授权
            AVAuthorizationStatusAuthorized -> {
                true
            }

            else -> {
                false
            }
        }
    }

    actual suspend fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        withContext(Dispatchers.IO) {
            if (checkStoragePermission().not()) {
                PHPhotoLibrary.requestAuthorization {
                    when (it) {
                        PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    onGranted()
                                }
                            }
                        }

                        else -> {
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    onDenied()
                                }
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onGranted()
                }
            }
        }
    }

    actual suspend fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (checkCameraPermission().not()) {
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        if (granted) {
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    onGranted()
                                }
                            }
                        } else {
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    onDenied()
                                }
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onGranted()
                    }
                }
            }
        }
    }

}

actual fun PlatformContext.goToAppSetting() {
    // iOS 跳转到设置页面
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!
    if (UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    }
}
