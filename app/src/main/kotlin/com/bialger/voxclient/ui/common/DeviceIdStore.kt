package com.bialger.voxclient.ui.common

import android.content.Context
import java.util.UUID

object DeviceIdStore {
    private const val PREFS_NAME = "vox_auth_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)?.trim()
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }
}

