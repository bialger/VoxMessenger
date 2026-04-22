package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.repository.UsernameGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCachingUsernameGatewayTest {
    @Test
    fun fetchUsernameByUserId_usesCacheAfterFirstCall() {
        val delegate = FakeUsernameGateway()
        val gateway = InMemoryCachingUsernameGateway(delegate)

        val first = gateway.fetchUsernameByUserId(BASE_URL, TOKEN, "usr_1")
        val second = gateway.fetchUsernameByUserId(BASE_URL, TOKEN, "usr_1")

        assertTrue(first is VoxResult.Success)
        assertTrue(second is VoxResult.Success)
        assertEquals("alice", (second as VoxResult.Success).value)
        assertEquals(1, delegate.singleFetchCalls)
    }

    @Test
    fun registerUsernamesBatch_primesCacheForSingleFetch() {
        val delegate = FakeUsernameGateway()
        val gateway = InMemoryCachingUsernameGateway(delegate)

        gateway.registerUsernamesBatch(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            usernamesByUserId = mapOf("usr_2" to "bob"),
        )
        val fromCache = gateway.fetchUsernameByUserId(BASE_URL, TOKEN, "usr_2")

        assertTrue(fromCache is VoxResult.Success)
        assertEquals("bob", (fromCache as VoxResult.Success).value)
        assertEquals(0, delegate.singleFetchCalls)
    }

    @Test
    fun fetchUsernamesBatchByUserIds_fetchesOnlyMissingIds() {
        val delegate = FakeUsernameGateway()
        val gateway = InMemoryCachingUsernameGateway(delegate)

        gateway.registerUsernamesBatch(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            usernamesByUserId = mapOf("usr_1" to "alice"),
        )

        val result =
            gateway.fetchUsernamesBatchByUserIds(
                serverBaseUrl = BASE_URL,
                accessToken = TOKEN,
                userIds = listOf("usr_1", "usr_2"),
            )

        assertTrue(result is VoxResult.Success)
        val usernames = (result as VoxResult.Success).value
        assertEquals("alice", usernames["usr_1"])
        assertEquals("bob", usernames["usr_2"])
        assertEquals(1, delegate.batchFetchCalls)
    }

    private class FakeUsernameGateway : UsernameGateway {
        var singleFetchCalls: Int = 0
        var batchFetchCalls: Int = 0

        override fun fetchUsernameByUserId(
            serverBaseUrl: String,
            accessToken: String,
            userId: String,
        ): VoxResult<String> {
            singleFetchCalls += 1
            return VoxResult.Success(
                when (userId) {
                    "usr_1" -> "alice"
                    "usr_2" -> "bob"
                    else -> "unknown"
                },
            )
        }

        override fun fetchUsernamesBatchByUserIds(
            serverBaseUrl: String,
            accessToken: String,
            userIds: Collection<String>,
        ): VoxResult<Map<String, String>> {
            batchFetchCalls += 1
            val result =
                userIds.associateWith { userId ->
                    when (userId) {
                        "usr_1" -> "alice"
                        "usr_2" -> "bob"
                        else -> "unknown"
                    }
                }
            return VoxResult.Success(result)
        }

        override fun registerUsernamesBatch(
            serverBaseUrl: String,
            accessToken: String,
            usernamesByUserId: Map<String, String>,
        ): VoxResult<Unit> = VoxResult.Success(Unit)
    }

    private companion object {
        const val BASE_URL = "https://vox.example/"
        const val TOKEN = "token"
    }
}
