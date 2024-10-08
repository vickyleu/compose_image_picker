package com.huhx.picker.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/**
 * 获取鸿蒙系统信息
 */
internal object HarmonyUtil {
    val isHarmonyOs: Boolean
        /**
         * 是否为鸿蒙系统
         *
         * @return true为鸿蒙系统
         */
        get() = try {
            val buildExClass = Class.forName("com.huawei.system.BuildEx")
            val osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass)
            if (osBrand == null) {
                false
            } else {
                val isHarmony = "Harmony".equals(osBrand.toString(), ignoreCase = true)
//        Log.i("HarmonyUtil", "当前设备是鸿蒙系统")
                isHarmony
            }
        } catch (ignored: Exception) {
            false
        }
    @JvmStatic
    val harmonyVersion: String
        /**
         * 获取鸿蒙系统版本号
         *
         * @return 版本号
         */
        get() = getProp("hw_sc.build.platform.version", "")

    val harmonyDisplayVersion: String
        /**
         * 获取鸿蒙系统版本号
         *
         * @return 版本号
         */
        get() = android.os.Build.DISPLAY.let {
            try {
                val subString = it.substring(it.indexOf(harmonyVersion), it.length)
                val harmonyVersion = subString.substring(0, subString.indexOf("("))
                harmonyVersion
            } catch (ignored: Exception) {
                if (harmonyVersion.isEmpty()) {
                    "0.0.0"
                } else {
                    "$harmonyVersion.0"
                }
            }
        }

    @Suppress("SameParameterValue")
    @SuppressLint("PrivateApi")
    private fun getProp(property: String, defaultValue: String): String {
        try {
            val spClz = Class.forName("android.os.SystemProperties")
            val method = spClz.getDeclaredMethod("get", String::class.java)
            val value = method.invoke(spClz, property) as String
            if (TextUtils.isEmpty(value)) {
                return defaultValue
            }
//      Log.i("HarmonyUtil", "当前设备是鸿蒙" + value + "系统")
            return value
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    /**
     * 判断是否开启鸿蒙纯净模式
     */
    fun isPureMode(context: Context?): Boolean {
        var result = false
        if (!isHarmonyOs) {
            return false
        }
        try {
            if (context != null) {
                result = 0 == Settings.Secure.getInt(context.contentResolver, "pure_mode_state", 0)
            }
        } catch (ignored: Exception) {
        }
        return result
    }
}