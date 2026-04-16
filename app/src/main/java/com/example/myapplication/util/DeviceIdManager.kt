package com.example.myapplication.util

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * 获取设备唯一识别码。
 * 优先使用 ANDROID_ID（每个手机+用户组合唯一），
 * 若获取不到则生成一个 UUID 作为兜底并缓存在 SharedPreferences 中。
 */
object DeviceIdManager {

    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_FALLBACK_ID = "fallback_device_id"

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            return androidId.uppercase()
        }

        // 兜底逻辑：部分老旧设备 ANDROID_ID 可能不可靠，使用本地缓存的 UUID
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fallback = prefs.getString(KEY_FALLBACK_ID, null)
        return if (!fallback.isNullOrBlank()) {
            fallback.uppercase()
        } else {
            val newId = UUID.randomUUID().toString().replace("-", "").uppercase()
            prefs.edit().putString(KEY_FALLBACK_ID, newId).apply()
            newId
        }
    }
}
