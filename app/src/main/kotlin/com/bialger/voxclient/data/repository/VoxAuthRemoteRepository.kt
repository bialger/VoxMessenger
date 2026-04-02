package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.data.dto.LoginRequestDto
import com.bialger.voxclient.data.dto.MeResponseDto
import com.bialger.voxclient.data.dto.RegisterRequestDto
import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

data class AuthNetworkSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val deviceId: String,
)

class VoxAuthRemoteRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
    private val gson: Gson = Gson(),
) {

    fun login(serverBaseUrl: String, request: LoginRequestDto): VoxResult<AuthNetworkSession> =
        executeAuthCall {
            apiFactory.create(serverBaseUrl).login(request).execute()
        }.map { session ->
            session.copy(deviceId = request.deviceId)
        }

    fun register(serverBaseUrl: String, request: RegisterRequestDto): VoxResult<AuthNetworkSession> =
        executeAuthCall {
            apiFactory.create(serverBaseUrl).register(request).execute()
        }.map { session ->
            session.copy(deviceId = request.deviceId)
        }

    fun loadMe(serverBaseUrl: String, accessToken: String): VoxResult<MeResponseDto> {
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
                VoxResult.Success(body)
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

    private fun executeAuthCall(block: () -> Response<com.bialger.voxclient.data.dto.AuthSessionResponseDto>): VoxResult<AuthNetworkSession> {
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
                    AuthNetworkSession(
                        userId = body.userId,
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        deviceId = "",
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

    private fun VoxResult<AuthNetworkSession>.map(transform: (AuthNetworkSession) -> AuthNetworkSession): VoxResult<AuthNetworkSession> =
        when (this) {
            is VoxResult.Success -> VoxResult.Success(transform(value))
            is VoxResult.Failure -> this
        }

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
    }
}
