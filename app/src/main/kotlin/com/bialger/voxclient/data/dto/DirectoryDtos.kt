package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String,
)

data class UsersResponseDto(
    @SerializedName("users")
    val users: List<UserDto>,
)

data class UserDevicesResponseDto(
    @SerializedName("devices")
    val devices: List<UserDeviceDto>,
)

data class UserDeviceDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_label")
    val deviceLabel: String?,
    @SerializedName("is_revoked")
    val isRevoked: Boolean,
    @SerializedName("has_prekeys")
    val hasPrekeys: Boolean,
)

data class UserPreKeyBundlesResponseDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("bundles")
    val bundles: List<UserPreKeyBundleDto>,
)

data class UserPreKeyBundleDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_label")
    val deviceLabel: String?,
    @SerializedName("identity_key_public")
    val identityKeyPublic: String,
    @SerializedName("signed_prekey_public")
    val signedPrekeyPublic: String,
    @SerializedName("signed_prekey_signature")
    val signedPrekeySignature: String,
    @SerializedName("one_time_prekey_public")
    val oneTimePrekeyPublic: String?,
    @SerializedName("one_time_prekey_id")
    val oneTimePrekeyId: String?,
)
