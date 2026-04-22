package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class PublishPreKeysRequestDto(
    @SerializedName("prekeys")
    val prekeys: List<OneTimePreKeyDto>,
)

data class OneTimePreKeyDto(
    @SerializedName("prekey_id")
    val prekeyId: String,
    @SerializedName("prekey_public")
    val prekeyPublic: String,
)

data class RotateSignedPreKeyRequestDto(
    @SerializedName("signed_prekey_public")
    val signedPrekeyPublic: String,
    @SerializedName("signed_prekey_signature")
    val signedPrekeySignature: String,
)

data class DevicePreKeyBundleDto(
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("device_id")
    val deviceId: String,
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
