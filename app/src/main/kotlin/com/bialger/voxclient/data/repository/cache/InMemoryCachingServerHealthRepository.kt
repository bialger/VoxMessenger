package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ServerHealth
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryCachingServerHealthRepository(
    private val delegate: ServerHealthRepository,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ServerHealthRepository {
    private val cache = ConcurrentHashMap<String, CacheEntry<ServerHealth>>()

    override fun checkHealth(serverBaseUrl: String): VoxResult<ServerHealth> {
        val now = nowMillis()
        val cached = cache[serverBaseUrl]
        if (cached != null && cached.expiresAtMillis > now) {
            return VoxResult.Success(cached.value)
        }

        return when (val result = delegate.checkHealth(serverBaseUrl)) {
            is VoxResult.Success -> {
                cache[serverBaseUrl] = CacheEntry(result.value, now + ttlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    private data class CacheEntry<T>(
        val value: T,
        val expiresAtMillis: Long,
    )

    private companion object {
        const val DEFAULT_TTL_MILLIS = 30_000L
    }
}
