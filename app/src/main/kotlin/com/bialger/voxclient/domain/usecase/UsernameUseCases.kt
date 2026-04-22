package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.boundary.FetchUsernameByUserIdInputPort
import com.bialger.voxclient.domain.boundary.FetchUsernamesBatchByUserIdsInputPort
import com.bialger.voxclient.domain.boundary.RegisterUsernamesBatchInputPort
import com.bialger.voxclient.domain.repository.UsernameGateway

class FetchUsernameByUserIdUseCase(
    private val usernameGateway: UsernameGateway,
) : FetchUsernameByUserIdInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        userId: String,
    ): VoxResult<String> {
        if (serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (accessToken.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Access token must not be blank."))
        }
        if (userId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("User id must not be blank."))
        }

        return usernameGateway.fetchUsernameByUserId(serverBaseUrl, accessToken, userId.trim())
    }
}

class FetchUsernamesBatchByUserIdsUseCase(
    private val usernameGateway: UsernameGateway,
) : FetchUsernamesBatchByUserIdsInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>> {
        if (serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (accessToken.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Access token must not be blank."))
        }
        val normalizedIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) {
            return VoxResult.Success(emptyMap())
        }

        return usernameGateway.fetchUsernamesBatchByUserIds(serverBaseUrl, accessToken, normalizedIds)
    }
}

class RegisterUsernamesBatchUseCase(
    private val usernameGateway: UsernameGateway,
) : RegisterUsernamesBatchInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        usernamesByUserId: Map<String, String>,
    ): VoxResult<Unit> {
        if (serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (accessToken.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Access token must not be blank."))
        }

        val normalized =
            usernamesByUserId
                .mapNotNull { (userId, username) ->
                    val id = userId.trim()
                    val name = username.trim()
                    if (id.isBlank() || name.isBlank()) null else id to name
                }.toMap()

        if (normalized.isEmpty()) {
            return VoxResult.Success(Unit)
        }

        return usernameGateway.registerUsernamesBatch(serverBaseUrl, accessToken, normalized)
    }
}
