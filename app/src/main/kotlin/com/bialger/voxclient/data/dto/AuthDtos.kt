package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequestDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("password_derived_value")
    val passwordDerivedValue: String,
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
    @SerializedName("wrapped_sync_key")
    val wrappedSyncKey: String,
    @SerializedName("sync_wrap_salt")
    val syncWrapSalt: String,
    @SerializedName("sync_wrap_params")
    val syncWrapParams: SyncWrapParamsDto,
)

data class LoginRequestDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("password_derived_value")
    val passwordDerivedValue: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_label")
    val deviceLabel: String? = null,
    @SerializedName("identity_key_public")
    val identityKeyPublic: String? = null,
    @SerializedName("signed_prekey_public")
    val signedPrekeyPublic: String? = null,
    @SerializedName("signed_prekey_signature")
    val signedPrekeySignature: String? = null,
)

data class AuthSessionResponseDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("device_status")
    val deviceStatus: String,
    @SerializedName("sync_key_version")
    val syncKeyVersion: Int,
)

data class RefreshRequestDto(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("device_id")
    val deviceId: String,
)

data class RefreshResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
)
