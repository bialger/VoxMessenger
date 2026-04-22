package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class MeResponseDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("current_device_id")
    val currentDeviceId: String,
    @SerializedName("sync_key_version")
    val syncKeyVersion: Int,
)

data class ChangePasswordRequestDto(
    @SerializedName("current_password_derived_value")
    val currentPasswordDerivedValue: String,
    @SerializedName("new_password_derived_value")
    val newPasswordDerivedValue: String,
    @SerializedName("wrapped_sync_key")
    val wrappedSyncKey: String,
    @SerializedName("sync_wrap_salt")
    val syncWrapSalt: String,
    @SerializedName("sync_wrap_params")
    val syncWrapParams: SyncWrapParamsDto,
)

data class ChangePasswordResponseDto(
    @SerializedName("sync_key_version")
    val syncKeyVersion: Int,
)

data class MyDevicesResponseDto(
    @SerializedName("devices")
    val devices: List<MyDeviceDto>,
)

data class MyDeviceDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_label")
    val deviceLabel: String?,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("last_seen_at")
    val lastSeenAt: Long,
    @SerializedName("is_current")
    val isCurrent: Boolean,
    @SerializedName("is_revoked")
    val isRevoked: Boolean,
)
