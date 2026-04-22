package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.DiscoveredUser
import com.bialger.voxclient.core.model.RemoteDeviceSummary
import com.bialger.voxclient.core.model.RemotePreKeyBundle
import com.bialger.voxclient.core.model.UserId
import com.bialger.voxclient.core.model.filterDirectory
import com.bialger.voxclient.domain.repository.DirectoryRepository

class SearchUsersByUsernameUseCase(
    private val directoryRepository: DirectoryRepository,
) {
    operator fun invoke(rawQuery: String, limit: Int = DEFAULT_LIMIT): VoxResult<List<DiscoveredUser>> {
        val query = rawQuery.trim()
        if (query.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Search query must not be blank."))
        }

        val normalizedLimit = limit.coerceIn(1, MAX_LIMIT)
        return directoryRepository.searchUsers(query = query, limit = normalizedLimit)
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 50
    }
}

class ResolveUserByUsernameUseCase(
    private val directoryRepository: DirectoryRepository,
) {
    operator fun invoke(rawUsername: String): VoxResult<DiscoveredUser> {
        val username = rawUsername.trim()
        if (username.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Username must not be blank."))
        }
        return directoryRepository.resolveByUsername(username)
    }
}

class LoadUserDevicesUseCase(
    private val directoryRepository: DirectoryRepository,
) {
    operator fun invoke(userId: UserId): VoxResult<List<RemoteDeviceSummary>> =
        directoryRepository.loadUserDevices(userId)
}

class LoadUserPreKeyBundlesUseCase(
    private val directoryRepository: DirectoryRepository,
) {
    operator fun invoke(userId: UserId): VoxResult<List<RemotePreKeyBundle>> =
        directoryRepository.loadUserPreKeyBundles(userId)
}

class FilterDiscoveredUsersUseCase {
    operator fun invoke(users: List<DiscoveredUser>, query: String): List<DiscoveredUser> =
        filterDirectory(users, query)
}

