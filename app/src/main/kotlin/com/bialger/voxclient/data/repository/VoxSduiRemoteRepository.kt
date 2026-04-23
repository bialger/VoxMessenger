package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.SduiScreen
import com.bialger.voxclient.data.datasource.remote.VoxPublicApi
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.data.dto.SduiEventRequestDto
import com.bialger.voxclient.data.mapper.SduiMapper
import com.bialger.voxclient.domain.repository.SduiGateway
import com.google.gson.Gson
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class VoxSduiRemoteRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
    private val gson: Gson = Gson(),
) : SduiGateway {
    private val apiByBaseUrl = ConcurrentHashMap<String, VoxPublicApi>()

    override fun fetchScreen(
        serverBaseUrl: String,
        deviceId: String,
        appVersionCode: Int,
        appVersionName: String?,
        locale: String?,
    ): VoxResult<SduiScreen?> {
        return try {
            val response =
                apiFor(serverBaseUrl).getSduiScreen(
                    platform = PLATFORM_ANDROID,
                    deviceId = deviceId,
                    appVersionCode = appVersionCode,
                    appVersionName = appVersionName,
                    locale = locale,
                ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response.errorBody()?.string())
                            ?: "SDUI screen failed with HTTP ${response.code()}.",
                    ),
                )
            } else {
                val body = response.body()
                if (body == null) {
                    VoxResult.Success(null)
                } else {
                    when (val mapped = SduiMapper.toModel(body)) {
                        is VoxResult.Success -> VoxResult.Success(mapped.value)
                        is VoxResult.Failure -> mapped
                    }
                }
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
                VoxError.Unknown("SDUI screen failed unexpectedly.", t),
            )
        }
    }

    override fun postEvent(
        serverBaseUrl: String,
        deviceId: String,
        screenId: String,
        event: String,
    ): VoxResult<Unit> {
        val trimmedEvent = event.trim()
        val trimmedScreenId = screenId.trim()
        if (trimmedEvent.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("SDUI event is blank."))
        }
        if (trimmedScreenId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("SDUI screen_id is blank."))
        }

        return try {
            val response =
                apiFor(serverBaseUrl).postSduiEvent(
                    SduiEventRequestDto(
                        deviceId = deviceId,
                        screenId = trimmedScreenId,
                        event = trimmedEvent.lowercase(Locale.ROOT),
                        meta = null,
                        clientTime = null,
                    ),
                ).execute()
            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response.errorBody()?.string())
                            ?: "SDUI event failed with HTTP ${response.code()}.",
                    ),
                )
            } else {
                val ok = response.body()?.ok
                if (ok == false) {
                    VoxResult.Failure(VoxError.Unknown("SDUI event returned ok=false."))
                } else {
                    VoxResult.Success(Unit)
                }
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
                VoxError.Unknown("SDUI event failed unexpectedly.", t),
            )
        }
    }

    private fun apiFor(serverBaseUrl: String): VoxPublicApi =
        apiByBaseUrl.getOrPut(serverBaseUrl) {
            apiFactory.create(serverBaseUrl)
        }

    private fun extractErrorMessage(raw: String?): String? {
        val body = raw?.trim().orEmpty()
        if (body.isEmpty()) return null
        return try {
            gson.fromJson(body, ApiErrorEnvelopeDto::class.java)?.error?.message
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
        const val PLATFORM_ANDROID = "android"
    }
}

