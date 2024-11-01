package com.huhx.picker.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat.send
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil3.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual class StoragePermissionUtil actual constructor(
    private val context: PlatformContext,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope
) {
    private val flow = MutableSharedFlow<Boolean>()
    private lateinit var launcherScope: ActivityResultLauncher<Intent>
    private var launcher: ActivityResultLauncher<String> = (context as ComponentActivity).registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        scope.launch {
            flow.emit(isGranted)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            launcherScope = (context as ComponentActivity).registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    scope.launch {
                        flow.emit(Environment.isExternalStorageManager())
                    }
                } else {
                    scope.launch {
                        flow.emit(false)
                    }
                }
            }
        }
    }

    actual suspend fun checkStoragePermission(): Boolean {
        if (scopeStorageCheck()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = resolveAllFileAccessInfo(context)
                return if (intent != null) {
                    Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            } else {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    actual suspend fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        withContext(Dispatchers.IO) {
            if (checkStoragePermission().not()) {
                if (scopeStorageCheck()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = resolveAllFileAccessInfo(context)
                        if (intent != null) {
                            if (::launcherScope.isInitialized.not()) {
                                withContext(Dispatchers.Main){
                                    onDenied()
                                }
                                return@withContext
                            }
                            scope.launch {
                                withContext(Dispatchers.IO){
                                    flow.collectLatest {
                                        withContext(Dispatchers.Main){
                                            if(it){
                                                onGranted()
                                            }else{
                                                onDenied()
                                            }
                                        }
                                    }
                                }
                            }
                            launcherScope.launch(intent)
                        } else {
                            scope.launch {
                                withContext(Dispatchers.IO){
                                    flow.collectLatest {
                                        withContext(Dispatchers.Main){
                                            if(it){
                                                onGranted()
                                            }else{
                                                onDenied()
                                            }
                                        }
                                    }
                                }
                            }
                            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    } else {
                        scope.launch {
                            withContext(Dispatchers.IO){
                                flow.collectLatest {
                                    withContext(Dispatchers.Main){
                                        if(it){
                                            onGranted()
                                        }else{
                                            onDenied()
                                        }
                                    }
                                }
                            }
                        }
                        launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                } else {
                    scope.launch {
                        withContext(Dispatchers.IO){
                            flow.collectLatest {
                                withContext(Dispatchers.Main){
                                    if(it){
                                        onGranted()
                                    }else{
                                        onDenied()
                                    }
                                }
                            }
                        }
                    }
                    launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                withContext(Dispatchers.Main){
                    onGranted()
                }
            }
        }
    }

    actual suspend fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        scope.launch{
            withContext(Dispatchers.IO) {
                if (checkCameraPermission().not()) {
                    scope.launch {
                        withContext(Dispatchers.Default){
                            flow.collectLatest {
                                withContext(Dispatchers.Main){
                                    if(it){
                                        onGranted()
                                    }else{
                                        onDenied()
                                    }
                                }
                            }
                        }
                    }
                    launcher.launch(Manifest.permission.CAMERA)
                } else {
                    withContext(Dispatchers.Main){
                        onGranted()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun resolveAllFileAccessInfo(context: Context): Intent? {
        val packageManager = context.packageManager
        val intentWrap = try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse("package:${context.packageName}")
            intent
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            intent
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intentWrap,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intentWrap, 0)?.let {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                intent
            }
        }
        return if (resolveInfo != null) intentWrap else null
    }

    private fun scopeStorageCheck(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q).let {
            if (it) {
                if (HarmonyUtil.isHarmonyOs) {
                    val harmonyVersion = HarmonyUtil.harmonyDisplayVersion
                    compareBeforeIsEqualOrLargeThanAfterVersion(
                        harmonyVersion,
                        "3.0.0.208"
                    )
                } else true
            } else {
                false
            }
        }
    }

    private fun compareBeforeIsEqualOrLargeThanAfterVersion(
        before: String,
        after: String
    ): Boolean {
        val afterArray = after.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList()
        val beforeArray = before.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList()
        val maxArrayLength = maxOf(afterArray.size, beforeArray.size)
        if (maxArrayLength == 0) return false
        for (i in 0 until maxArrayLength - afterArray.size) {
            afterArray.add(0)
        }
        for (i in 0 until maxArrayLength - beforeArray.size) {
            beforeArray.add(0)
        }
        for (i in 0 until maxArrayLength) {
            val af = afterArray[i]
            val bf = beforeArray[i]
            if (bf > af) {
                return true
            } else if (bf < af) {
                return false
            }
        }
        return afterArray.joinToString(".") == beforeArray.joinToString(".")
    }
}

actual fun PlatformContext.goToAppSetting() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
