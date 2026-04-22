package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ServerHealth
import com.bialger.voxclient.data.datasource.remote.VoxPublicApi
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class RetrofitServerHealthRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
) : ServerHealthRepository {
    private val apiByBaseUrl = ConcurrentHashMap<String, VoxPublicApi>()

    override fun checkHealth(serverBaseUrl: String): VoxResult<ServerHealth> {
        return try {
            val response = apiFor(serverBaseUrl).getHealth().execute()
            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = "Health check failed with HTTP ${response.code()}.",
                    ),
                )
            } else {
                val body = response.body()
                    ?: return VoxResult.Failure(
                        VoxError.Unknown("Health check returned an empty response body."),
                    )

                val status = body.status.trim()
                if (status.isBlank()) {
                    VoxResult.Failure(
                        VoxError.Unknown("Health check returned a blank status."),
                    )
                } else {
                    VoxResult.Success(ServerHealth(status = status))
                }
            }
        } catch (networkException: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = networkException.message ?: "Unable to reach server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown(message = "Health check failed unexpectedly.", cause = t),
            )
        }
    }

    private fun apiFor(serverBaseUrl: String): VoxPublicApi =
        apiByBaseUrl.getOrPut(serverBaseUrl) {
            apiFactory.create(serverBaseUrl)
        }

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
    }
}
