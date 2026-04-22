package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.data.dto.AuthSessionResponseDto
import com.bialger.voxclient.data.dto.LoginRequestDto
import com.bialger.voxclient.data.dto.RegisterRequestDto
import com.bialger.voxclient.data.dto.SyncWrapParamsDto
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxUserProfile
import com.bialger.voxclient.domain.repository.AuthSessionGateway
import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

class VoxAuthRemoteRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
    private val gson: Gson = Gson(),
) : AuthSessionGateway {

    override fun login(command: VoxLoginCommand): VoxResult<VoxAuthSession> =
        executeAuthCall {
            apiFactory
                .create(command.serverBaseUrl)
                .login(
                    LoginRequestDto(
                        username = command.username,
                        passwordDerivedValue = command.passwordDerivedValue,
                        deviceId = command.deviceId,
                        deviceLabel = command.deviceLabel,
                        identityKeyPublic = command.identityKeyPublic,
                        signedPrekeyPublic = command.signedPrekeyPublic,
                        signedPrekeySignature = command.signedPrekeySignature,
                    ),
                ).execute()
        }.map { session ->
            session.copy(deviceId = command.deviceId)
        }

    override fun register(command: VoxRegisterCommand): VoxResult<VoxAuthSession> =
        executeAuthCall {
            apiFactory
                .create(command.serverBaseUrl)
                .register(
                    RegisterRequestDto(
                        username = command.username,
                        passwordDerivedValue = command.passwordDerivedValue,
                        deviceId = command.deviceId,
                        deviceLabel = command.deviceLabel,
                        identityKeyPublic = command.identityKeyPublic,
                        signedPrekeyPublic = command.signedPrekeyPublic,
                        signedPrekeySignature = command.signedPrekeySignature,
                        wrappedSyncKey = command.wrappedSyncKey,
                        syncWrapSalt = command.syncWrapSalt,
                        syncWrapParams =
                            SyncWrapParamsDto(
                                algorithm = command.syncWrapParams.algorithm,
                                memoryKiB = command.syncWrapParams.memoryKiB,
                                iterations = command.syncWrapParams.iterations,
                                parallelism = command.syncWrapParams.parallelism,
                            ),
                    ),
                ).execute()
        }.map { session ->
            session.copy(deviceId = command.deviceId)
        }

    override fun loadCurrentUser(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile> {
        return try {
            val response =
                apiFactory
                    .createAccountApi(serverBaseUrl)
                    .me(authorization = bearer(accessToken))
                    .execute()
            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Loading profile failed with HTTP ${response.code()}.",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Profile response is empty."),
                    )
                VoxResult.Success(
                    VoxUserProfile(
                        userId = body.userId,
                        username = body.username,
                        currentDeviceId = body.currentDeviceId,
                        syncKeyVersion = body.syncKeyVersion,
                    ),
                )
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Loading profile failed unexpectedly.", t),
            )
        }
    }

    private fun executeAuthCall(block: () -> Response<AuthSessionResponseDto>): VoxResult<VoxAuthSession> {
        return try {
            val response = block()
            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Authentication failed with HTTP ${response.code()}.",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Authentication returned an empty response body."),
                    )
                VoxResult.Success(
                    VoxAuthSession(
                        userId = body.userId,
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        deviceId = "",
                        syncKeyVersion = body.syncKeyVersion,
                    ),
                )
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Authentication failed unexpectedly.", t),
            )
        }
    }

    private fun extractErrorMessage(response: Response<*>): String? {
        val raw = response.errorBody()?.string()?.trim().orEmpty()
        if (raw.isEmpty()) {
            return null
        }
        return try {
            gson.fromJson(raw, ApiErrorEnvelopeDto::class.java)?.error?.message
        } catch (_: Throwable) {
            null
        }
    }

    private fun VoxResult<VoxAuthSession>.map(transform: (VoxAuthSession) -> VoxAuthSession): VoxResult<VoxAuthSession> =
        when (this) {
            is VoxResult.Success -> VoxResult.Success(transform(value))
            is VoxResult.Failure -> this
        }

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
    }
}
