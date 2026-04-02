package com.bialger.voxclient.domain.entity

data class VoxSyncWrapParams(
    val algorithm: String,
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
)

data class VoxLoginCommand(
    val serverBaseUrl: String,
    val username: String,
    val passwordDerivedValue: String,
    val deviceId: String,
    val deviceLabel: String? = null,
    val identityKeyPublic: String? = null,
    val signedPrekeyPublic: String? = null,
    val signedPrekeySignature: String? = null,
)

data class VoxRegisterCommand(
    val serverBaseUrl: String,
    val username: String,
    val passwordDerivedValue: String,
    val deviceId: String,
    val deviceLabel: String? = null,
    val identityKeyPublic: String,
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val wrappedSyncKey: String,
    val syncWrapSalt: String,
    val syncWrapParams: VoxSyncWrapParams,
)

data class VoxAuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val deviceId: String,
    val syncKeyVersion: Int,
)

data class VoxUserProfile(
    val userId: String,
    val username: String,
    val currentDeviceId: String,
    val syncKeyVersion: Int,
)
