package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.repository.UsernameGateway
import java.util.concurrent.ConcurrentHashMap

class InMemoryCachingUsernameGateway(
    private val delegate: UsernameGateway,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : UsernameGateway {
    private val usernameByUserId = ConcurrentHashMap<UserIdKey, CacheEntry<String>>()

    override fun fetchUsernameByUserId(
        serverBaseUrl: String,
        accessToken: String,
        userId: String,
    ): VoxResult<String> {
        val key = UserIdKey(serverBaseUrl, accessToken, userId)
        getFresh(key)?.let { return VoxResult.Success(it) }

        return when (val result = delegate.fetchUsernameByUserId(serverBaseUrl, accessToken, userId)) {
            is VoxResult.Success -> {
                put(key, result.value)
                result
            }

            is VoxResult.Failure -> result
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

        val resolved = linkedMapOf<String, String>()
        val missing = mutableListOf<String>()
        normalizedIds.forEach { userId ->
            val key = UserIdKey(serverBaseUrl, accessToken, userId)
            val cached = getFresh(key)
            if (cached != null) {
                resolved[userId] = cached
            } else {
                missing += userId
            }
        }

        if (missing.isEmpty()) {
            return VoxResult.Success(resolved)
        }

        return when (val remote = delegate.fetchUsernamesBatchByUserIds(serverBaseUrl, accessToken, missing)) {
            is VoxResult.Success -> {
                remote.value.forEach { (userId, username) ->
                    resolved[userId] = username
                    put(UserIdKey(serverBaseUrl, accessToken, userId), username)
                }
                VoxResult.Success(resolved)
            }

            is VoxResult.Failure -> remote
        }
    }

    override fun registerUsernamesBatch(
        serverBaseUrl: String,
        accessToken: String,
        usernamesByUserId: Map<String, String>,
    ): VoxResult<Unit> {
        usernamesByUserId.forEach { (userId, username) ->
            val normalizedUserId = userId.trim()
            val normalizedUsername = username.trim()
            if (normalizedUserId.isNotBlank() && normalizedUsername.isNotBlank()) {
                put(UserIdKey(serverBaseUrl, accessToken, normalizedUserId), normalizedUsername)
            }
        }
        // Delegate is optional/no-op for this method; invoke it to preserve behavior contract.
        return delegate.registerUsernamesBatch(serverBaseUrl, accessToken, usernamesByUserId)
    }

    private fun getFresh(key: UserIdKey): String? {
        val entry = usernameByUserId[key] ?: return null
        return if (entry.expiresAtMillis > nowMillis()) {
            entry.value
        } else {
            usernameByUserId.remove(key)
            null
        }
    }

    private fun put(key: UserIdKey, username: String) {
        usernameByUserId[key] = CacheEntry(username, nowMillis() + ttlMillis)
    }

    private data class UserIdKey(
        val serverBaseUrl: String,
        val accessToken: String,
        val userId: String,
    )

    private data class CacheEntry<T>(
        val value: T,
        val expiresAtMillis: Long,
    )

    private companion object {
        const val DEFAULT_TTL_MILLIS = 300_000L
    }
}
