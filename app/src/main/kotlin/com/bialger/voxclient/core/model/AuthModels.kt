package com.bialger.voxclient.core.model

data class AuthSession(
    val userId: UserId,
    val deviceId: DeviceId,
    val accessToken: String,
    val refreshToken: String,
    val syncKeyVersion: Int,
)

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val session: AuthSession) : AuthState
    data class Failure(val reason: String) : AuthState
}

data class RegisterCommand(
    val serverBaseUrl: String,
    val username: String,
    val password: String,
    val deviceId: DeviceId,
    val wrappedSyncKey: String,
    val syncWrapSalt: String,
    val syncWrapParams: SyncWrapParams,
)

