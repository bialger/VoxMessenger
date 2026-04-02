package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxUserProfile
import com.bialger.voxclient.domain.repository.AuthSessionGateway
import java.util.concurrent.ConcurrentHashMap

class InMemoryCachingAuthSessionGateway(
    private val delegate: AuthSessionGateway,
    private val currentUserTtlMillis: Long = DEFAULT_CURRENT_USER_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AuthSessionGateway {
    private val currentUserByToken = ConcurrentHashMap<CurrentUserKey, CacheEntry<VoxUserProfile>>()

    override fun login(command: VoxLoginCommand): VoxResult<VoxAuthSession> {
        val result = delegate.login(command)
        if (result is VoxResult.Success) {
            invalidateServerEntries(command.serverBaseUrl)
        }
        return result
    }

    override fun register(command: VoxRegisterCommand): VoxResult<VoxAuthSession> {
        val result = delegate.register(command)
        if (result is VoxResult.Success) {
            invalidateServerEntries(command.serverBaseUrl)
        }
        return result
    }

    override fun loadCurrentUser(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile> {
        val key = CurrentUserKey(serverBaseUrl = serverBaseUrl, accessToken = accessToken)
        val now = nowMillis()
        val cached = currentUserByToken[key]
        if (cached != null && cached.expiresAtMillis > now) {
            return VoxResult.Success(cached.value)
        }

        return when (val result = delegate.loadCurrentUser(serverBaseUrl, accessToken)) {
            is VoxResult.Success -> {
                currentUserByToken[key] = CacheEntry(result.value, now + currentUserTtlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    private fun invalidateServerEntries(serverBaseUrl: String) {
        currentUserByToken.keys.removeIf { it.serverBaseUrl == serverBaseUrl }
    }

    private data class CurrentUserKey(
        val serverBaseUrl: String,
        val accessToken: String,
    )

    private data class CacheEntry<T>(
        val value: T,
        val expiresAtMillis: Long,
    )

    private companion object {
        const val DEFAULT_CURRENT_USER_TTL_MILLIS = 60_000L
    }
}
