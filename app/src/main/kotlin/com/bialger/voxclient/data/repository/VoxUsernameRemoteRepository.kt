package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.domain.repository.UsernameGateway
import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

class VoxUsernameRemoteRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
    private val gson: Gson = Gson(),
) : UsernameGateway {

    override fun fetchUsernameByUserId(
        serverBaseUrl: String,
        accessToken: String,
        userId: String,
    ): VoxResult<String> {
        return try {
            val response =
                apiFactory
                    .createDirectoryApi(serverBaseUrl)
                    .getUser(
                        authorization = bearer(accessToken),
                        userId = userId,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to fetch username (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body = response.body()
                    ?: return VoxResult.Failure(
                        VoxError.Unknown("Fetch username response body is empty."),
                    )
                VoxResult.Success(body.username)
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
                VoxError.Unknown("Fetching username failed unexpectedly.", t),
            )
        }
    }

    override fun fetchUsernamesBatchByUserIds(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>> {
        val normalizedIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) {
            return VoxResult.Success(emptyMap())
        }

        return try {
            val directoryApi = apiFactory.createDirectoryApi(serverBaseUrl)
            val resolved = linkedMapOf<String, String>()
            val unresolved = normalizedIds.toMutableSet()

            normalizedIds.chunked(MAX_USER_BATCH_SIZE).forEach { chunk ->
                val csvResponse =
                    directoryApi
                        .getUsersByIds(
                            authorization = bearer(accessToken),
                            ids = chunk.joinToString(","),
                        ).execute()

                val response =
                    if (csvResponse.isSuccessful) {
                        csvResponse
                    } else {
                        directoryApi
                            .getUsersByIdsRepeated(
                                authorization = bearer(accessToken),
                                ids = chunk,
                            ).execute()
                    }

                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return VoxResult.Failure(
                            VoxError.Unknown("Batch username response body is empty."),
                        )
                    body.users.forEach { user ->
                        if (user.userId.isNotBlank() && user.username.isNotBlank()) {
                            resolved[user.userId] = user.username
                            unresolved.remove(user.userId)
                        }
                    }
                }
            }

            unresolved.forEach { unresolvedId ->
                when (
                    val single =
                        fetchUsernameByUserId(
                            serverBaseUrl = serverBaseUrl,
                            accessToken = accessToken,
                            userId = unresolvedId,
                        )
                ) {
                    is VoxResult.Success -> {
                        if (single.value.isNotBlank()) {
                            resolved[unresolvedId] = single.value
                        }
                    }

                    is VoxResult.Failure -> Unit
                }
            }

            VoxResult.Success(resolved)
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Batch username fetch failed unexpectedly.", t),
            )
        }
    }

    override fun registerUsernamesBatch(
        serverBaseUrl: String,
        accessToken: String,
        usernamesByUserId: Map<String, String>,
    ): VoxResult<Unit> = VoxResult.Success(Unit)

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

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

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
        const val MAX_USER_BATCH_SIZE = 100
    }
}
