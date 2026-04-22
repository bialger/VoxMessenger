package com.bialger.voxclient.ui.session

import android.os.Bundle
import androidx.core.os.bundleOf

data class UserSessionArgs(
    val serverBaseUrl: String,
    val accessToken: String,
    val deviceId: String,
    val userId: String,
    val username: String,
    val isMock: Boolean,
)

fun UserSessionArgs.toBundle(): Bundle =
    bundleOf(
        KEY_SERVER_BASE_URL to serverBaseUrl,
        KEY_ACCESS_TOKEN to accessToken,
        KEY_DEVICE_ID to deviceId,
        KEY_USER_ID to userId,
        KEY_USERNAME to username,
        KEY_IS_MOCK to isMock,
    )

fun Bundle.readUserSessionArgs(): UserSessionArgs =
    UserSessionArgs(
        serverBaseUrl = getString(KEY_SERVER_BASE_URL).orEmpty(),
        accessToken = getString(KEY_ACCESS_TOKEN).orEmpty(),
        deviceId = getString(KEY_DEVICE_ID).orEmpty(),
        userId = getString(KEY_USER_ID).orEmpty(),
        username = getString(KEY_USERNAME).orEmpty(),
        isMock = getBoolean(KEY_IS_MOCK, false),
    )

const val KEY_SERVER_BASE_URL = "session_server_base_url"
const val KEY_ACCESS_TOKEN = "session_access_token"
const val KEY_DEVICE_ID = "session_device_id"
const val KEY_USER_ID = "session_user_id"
const val KEY_USERNAME = "session_username"
const val KEY_IS_MOCK = "session_is_mock"
