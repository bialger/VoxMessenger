package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt
import com.bialger.voxclient.domain.repository.ConversationGateway
import java.util.concurrent.ConcurrentHashMap

class InMemoryCachingConversationGateway(
    private val delegate: ConversationGateway,
    private val conversationListTtlMillis: Long = DEFAULT_LIST_TTL_MILLIS,
    private val metadataTtlMillis: Long = DEFAULT_METADATA_TTL_MILLIS,
    private val historyTtlMillis: Long = DEFAULT_HISTORY_TTL_MILLIS,
    private val identityTtlMillis: Long = DEFAULT_IDENTITY_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ConversationGateway {
    private val conversationsByAuth = ConcurrentHashMap<AuthKey, CacheEntry<List<VoxConversationSummary>>>()
    private val detailByConversation = ConcurrentHashMap<ConversationKey, CacheEntry<VoxConversationDetail>>()
    private val membersByConversation = ConcurrentHashMap<ConversationKey, CacheEntry<VoxConversationMembers>>()
    private val historyByConversation = ConcurrentHashMap<HistoryKey, CacheEntry<List<VoxConversationEnvelope>>>()
    private val userIdByUsername = ConcurrentHashMap<UsernameKey, CacheEntry<String>>()
    private val usernameByUserId = ConcurrentHashMap<UserIdKey, CacheEntry<String>>()

    override fun resolveUserIdByUsername(
        serverBaseUrl: String,
        accessToken: String,
        username: String,
    ): VoxResult<String> {
        val normalizedUsername = username.trim()
        val key = UsernameKey(serverBaseUrl, accessToken, normalizedUsername.lowercase())
        getFresh(userIdByUsername, key)?.let { return VoxResult.Success(it) }

        return when (
            val result =
                delegate.resolveUserIdByUsername(
                    serverBaseUrl = serverBaseUrl,
                    accessToken = accessToken,
                    username = normalizedUsername,
                )
        ) {
            is VoxResult.Success -> {
                put(userIdByUsername, key, result.value, identityTtlMillis)
                put(
                    usernameByUserId,
                    UserIdKey(serverBaseUrl, accessToken, result.value),
                    normalizedUsername,
                    identityTtlMillis,
                )
                result
            }

            is VoxResult.Failure -> result
        }
    }

    override fun resolveUsernamesByUserIds(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>> {
        val normalizedUserIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedUserIds.isEmpty()) {
            return VoxResult.Success(emptyMap())
        }

        val resolved = linkedMapOf<String, String>()
        val missing = mutableListOf<String>()
        normalizedUserIds.forEach { userId ->
            val key = UserIdKey(serverBaseUrl, accessToken, userId)
            val cachedUsername = getFresh(usernameByUserId, key)
            if (cachedUsername != null) {
                resolved[userId] = cachedUsername
            } else {
                missing += userId
            }
        }

        if (missing.isEmpty()) {
            return VoxResult.Success(resolved)
        }

        return when (
            val result =
                delegate.resolveUsernamesByUserIds(
                    serverBaseUrl = serverBaseUrl,
                    accessToken = accessToken,
                    userIds = missing,
                )
        ) {
            is VoxResult.Success -> {
                result.value.forEach { (userId, username) ->
                    resolved[userId] = username
                    put(
                        usernameByUserId,
                        UserIdKey(serverBaseUrl, accessToken, userId),
                        username,
                        identityTtlMillis,
                    )
                    put(
                        userIdByUsername,
                        UsernameKey(serverBaseUrl, accessToken, username.lowercase()),
                        userId,
                        identityTtlMillis,
                    )
                }
                VoxResult.Success(resolved)
            }

            is VoxResult.Failure -> result
        }
    }

    override fun createDmConversation(
        serverBaseUrl: String,
        accessToken: String,
        peerUserId: String,
    ): VoxResult<String> =
        delegate.createDmConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            peerUserId = peerUserId,
        ).also(invalidateAuthOnSuccess(serverBaseUrl, accessToken))

    override fun createGroupConversation(
        serverBaseUrl: String,
        accessToken: String,
        memberUserIds: List<String>,
    ): VoxResult<String> =
        delegate.createGroupConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            memberUserIds = memberUserIds,
        ).also(invalidateAuthOnSuccess(serverBaseUrl, accessToken))

    override fun createChannelConversation(
        serverBaseUrl: String,
        accessToken: String,
        adminUserIds: List<String>,
        subscriberUserIds: List<String>,
    ): VoxResult<String> =
        delegate.createChannelConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            adminUserIds = adminUserIds,
            subscriberUserIds = subscriberUserIds,
        ).also(invalidateAuthOnSuccess(serverBaseUrl, accessToken))

    override fun addConversationMember(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        userId: String,
        role: String?,
    ): VoxResult<Unit> =
        delegate.addConversationMember(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
            userId = userId,
            role = role,
        ).also { result ->
            if (result is VoxResult.Success) {
                invalidateConversation(serverBaseUrl, accessToken, conversationId)
                // Membership changes can affect list metadata (membershipVersion/visibility), so refresh list cache.
                invalidateConversationList(serverBaseUrl, accessToken)
            }
        }

    override fun subscribeToChannel(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<Unit> =
        delegate.subscribeToChannel(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
        ).also { result ->
            if (result is VoxResult.Success) {
                invalidateConversation(serverBaseUrl, accessToken, conversationId)
                invalidateConversationList(serverBaseUrl, accessToken)
            }
        }

    override fun loadConversationDetails(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationDetail> {
        val key = ConversationKey(serverBaseUrl, accessToken, conversationId)
        getFresh(detailByConversation, key)?.let { return VoxResult.Success(it) }

        return when (
            val result =
                delegate.loadConversationDetails(
                    serverBaseUrl = serverBaseUrl,
                    accessToken = accessToken,
                    conversationId = conversationId,
                )
        ) {
            is VoxResult.Success -> {
                put(detailByConversation, key, result.value, metadataTtlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    override fun loadConversationMembers(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationMembers> {
        val key = ConversationKey(serverBaseUrl, accessToken, conversationId)
        getFresh(membersByConversation, key)?.let { return VoxResult.Success(it) }

        return when (
            val result =
                delegate.loadConversationMembers(
                    serverBaseUrl = serverBaseUrl,
                    accessToken = accessToken,
                    conversationId = conversationId,
                )
        ) {
            is VoxResult.Success -> {
                put(membersByConversation, key, result.value, metadataTtlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    override fun loadConversations(
        serverBaseUrl: String,
        accessToken: String,
    ): VoxResult<List<VoxConversationSummary>> {
        val key = AuthKey(serverBaseUrl, accessToken)
        getFresh(conversationsByAuth, key)?.let { return VoxResult.Success(it.toList()) }

        return when (val result = delegate.loadConversations(serverBaseUrl, accessToken)) {
            is VoxResult.Success -> {
                put(conversationsByAuth, key, result.value.toList(), conversationListTtlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    override fun loadHistory(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        limit: Int,
    ): VoxResult<List<VoxConversationEnvelope>> {
        val key = HistoryKey(serverBaseUrl, accessToken, conversationId, limit)
        getFresh(historyByConversation, key)?.let { return VoxResult.Success(it.toList()) }

        return when (
            val result =
                delegate.loadHistory(
                    serverBaseUrl = serverBaseUrl,
                    accessToken = accessToken,
                    conversationId = conversationId,
                    limit = limit,
                )
        ) {
            is VoxResult.Success -> {
                // Store envelopes exactly as received from server; ciphertext stays encrypted in memory cache.
                put(historyByConversation, key, result.value.toList(), historyTtlMillis)
                result
            }

            is VoxResult.Failure -> result
        }
    }

    override fun sendMessage(
        serverBaseUrl: String,
        accessToken: String,
        command: VoxSendMessageCommand,
    ): VoxResult<VoxSendMessageReceipt> =
        delegate.sendMessage(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            command = command,
        ).also { result ->
            if (result is VoxResult.Success) {
                invalidateHistory(serverBaseUrl, accessToken, command.conversationId)
                // Sending updates conversation ordering/timestamp on chat list, so list cache must be refreshed.
                invalidateConversationList(serverBaseUrl, accessToken)
            }
        }

    private fun invalidateAuthOnSuccess(serverBaseUrl: String, accessToken: String): (VoxResult<*>) -> Unit =
        { result ->
            if (result is VoxResult.Success) {
                invalidateConversationList(serverBaseUrl, accessToken)
            }
        }

    private fun invalidateConversationList(serverBaseUrl: String, accessToken: String) {
        conversationsByAuth.remove(AuthKey(serverBaseUrl, accessToken))
    }

    private fun invalidateConversation(serverBaseUrl: String, accessToken: String, conversationId: String) {
        detailByConversation.keys.removeIf {
            it.serverBaseUrl == serverBaseUrl &&
                it.accessToken == accessToken &&
                it.conversationId == conversationId
        }
        membersByConversation.keys.removeIf {
            it.serverBaseUrl == serverBaseUrl &&
                it.accessToken == accessToken &&
                it.conversationId == conversationId
        }
        invalidateHistory(serverBaseUrl, accessToken, conversationId)
    }

    private fun invalidateHistory(serverBaseUrl: String, accessToken: String, conversationId: String) {
        historyByConversation.keys.removeIf {
            it.serverBaseUrl == serverBaseUrl &&
                it.accessToken == accessToken &&
                it.conversationId == conversationId
        }
    }

    private fun <K, V> getFresh(cache: ConcurrentHashMap<K, CacheEntry<V>>, key: K): V? {
        val entry = cache[key] ?: return null
        val now = nowMillis()
        return if (entry.expiresAtMillis > now) {
            entry.value
        } else {
            cache.remove(key)
            null
        }
    }

    private fun <K, V> put(cache: ConcurrentHashMap<K, CacheEntry<V>>, key: K, value: V, ttlMillis: Long) {
        cache[key] = CacheEntry(value = value, expiresAtMillis = nowMillis() + ttlMillis)
    }

    private data class CacheEntry<T>(
        val value: T,
        val expiresAtMillis: Long,
    )

    private data class AuthKey(
        val serverBaseUrl: String,
        val accessToken: String,
    )

    private data class ConversationKey(
        val serverBaseUrl: String,
        val accessToken: String,
        val conversationId: String,
    )

    private data class HistoryKey(
        val serverBaseUrl: String,
        val accessToken: String,
        val conversationId: String,
        val limit: Int,
    )

    private data class UsernameKey(
        val serverBaseUrl: String,
        val accessToken: String,
        val usernameNormalized: String,
    )

    private data class UserIdKey(
        val serverBaseUrl: String,
        val accessToken: String,
        val userId: String,
    )

    private companion object {
        const val DEFAULT_LIST_TTL_MILLIS = 30_000L
        const val DEFAULT_METADATA_TTL_MILLIS = 60_000L
        const val DEFAULT_HISTORY_TTL_MILLIS = 20_000L
        const val DEFAULT_IDENTITY_TTL_MILLIS = 300_000L
    }
}
