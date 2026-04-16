package com.bialger.voxclient.data.repository.cache

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMember
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt
import com.bialger.voxclient.domain.repository.ConversationGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCachingConversationGatewayTest {
    @Test
    fun loadHistory_cachesEncryptedPayloadAsIs() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        val first =
            gateway.loadHistory(
                serverBaseUrl = BASE_URL,
                accessToken = TOKEN,
                conversationId = CONVERSATION_ID,
                limit = 50,
            )
        val second =
            gateway.loadHistory(
                serverBaseUrl = BASE_URL,
                accessToken = TOKEN,
                conversationId = CONVERSATION_ID,
                limit = 50,
            )

        assertTrue(first is VoxResult.Success)
        assertTrue(second is VoxResult.Success)
        assertEquals(1, delegate.loadHistoryCalls)
        val envelopes = (second as VoxResult.Success).value
        assertEquals(1, envelopes.size)
        assertEquals("enc_payload_from_server", envelopes.first().ciphertext)
    }

    @Test
    fun sendMessage_invalidatesHistoryAndConversationListCaches() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        gateway.loadHistory(BASE_URL, TOKEN, CONVERSATION_ID, 50)
        gateway.loadConversations(BASE_URL, TOKEN)
        gateway.loadConversations(BASE_URL, TOKEN)

        gateway.sendMessage(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            command =
                VoxSendMessageCommand(
                    deviceId = "dev_1",
                    conversationId = CONVERSATION_ID,
                    ciphertext = "enc_outgoing",
                    envelopeId = "env_2",
                ),
        )

        gateway.loadHistory(BASE_URL, TOKEN, CONVERSATION_ID, 50)
        gateway.loadConversations(BASE_URL, TOKEN)

        assertEquals(2, delegate.loadHistoryCalls)
        assertEquals(2, delegate.loadConversationsCalls)
    }

    @Test
    fun addConversationMember_invalidatesConversationAndListCaches() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        primeConversationCaches(gateway)

        gateway.addConversationMember(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            conversationId = CONVERSATION_ID,
            userId = "usr_new",
            role = "member",
        )

        primeConversationCaches(gateway)

        assertEquals(2, delegate.loadConversationDetailsCalls)
        assertEquals(2, delegate.loadConversationMembersCalls)
        assertEquals(2, delegate.loadHistoryCalls)
        assertEquals(2, delegate.loadConversationsCalls)
    }

    @Test
    fun subscribeToChannel_invalidatesConversationAndListCaches() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        primeConversationCaches(gateway)

        gateway.subscribeToChannel(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            conversationId = CONVERSATION_ID,
        )

        primeConversationCaches(gateway)

        assertEquals(2, delegate.loadConversationDetailsCalls)
        assertEquals(2, delegate.loadConversationMembersCalls)
        assertEquals(2, delegate.loadHistoryCalls)
        assertEquals(2, delegate.loadConversationsCalls)
    }

    @Test
    fun createConversation_invalidatesConversationListCache() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        gateway.loadConversations(BASE_URL, TOKEN)
        gateway.loadConversations(BASE_URL, TOKEN)

        gateway.createDmConversation(
            serverBaseUrl = BASE_URL,
            accessToken = TOKEN,
            peerUserId = "usr_2",
        )

        gateway.loadConversations(BASE_URL, TOKEN)

        assertEquals(2, delegate.loadConversationsCalls)
    }

    private fun primeConversationCaches(gateway: InMemoryCachingConversationGateway) {
        gateway.loadConversationDetails(BASE_URL, TOKEN, CONVERSATION_ID)
        gateway.loadConversationMembers(BASE_URL, TOKEN, CONVERSATION_ID)
        gateway.loadHistory(BASE_URL, TOKEN, CONVERSATION_ID, 50)
        gateway.loadConversations(BASE_URL, TOKEN)
    }

    private class FakeConversationGateway : ConversationGateway {
        var loadConversationDetailsCalls: Int = 0
        var loadConversationMembersCalls: Int = 0
        var loadConversationsCalls: Int = 0
        var loadHistoryCalls: Int = 0

        override fun resolveUserIdByUsername(
            serverBaseUrl: String,
            accessToken: String,
            username: String,
        ): VoxResult<String> = VoxResult.Success("usr_2")

        override fun resolveUsernamesByUserIds(
            serverBaseUrl: String,
            accessToken: String,
            userIds: Collection<String>,
        ): VoxResult<Map<String, String>> = VoxResult.Success(emptyMap())

        override fun createDmConversation(
            serverBaseUrl: String,
            accessToken: String,
            peerUserId: String,
        ): VoxResult<String> = VoxResult.Success("conv_new")

        override fun createGroupConversation(
            serverBaseUrl: String,
            accessToken: String,
            memberUserIds: List<String>,
        ): VoxResult<String> = VoxResult.Success("conv_new")

        override fun createChannelConversation(
            serverBaseUrl: String,
            accessToken: String,
            adminUserIds: List<String>,
            subscriberUserIds: List<String>,
        ): VoxResult<String> = VoxResult.Success("conv_new")

        override fun addConversationMember(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
            userId: String,
            role: String?,
        ): VoxResult<Unit> = VoxResult.Success(Unit)

        override fun subscribeToChannel(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
        ): VoxResult<Unit> = VoxResult.Success(Unit)

        override fun loadConversationDetails(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
        ): VoxResult<VoxConversationDetail> {
            loadConversationDetailsCalls += 1
            return VoxResult.Success(
                VoxConversationDetail(
                    conversationId = conversationId,
                    type = 0,
                    createdBy = "usr_1",
                    createdByUsername = "alice",
                    createdAt = 1L,
                    membershipVersion = 1L,
                    myRole = "member",
                    title = null,
                    channelPostPolicy = null,
                ),
            )
        }

        override fun loadConversationMembers(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
        ): VoxResult<VoxConversationMembers> {
            loadConversationMembersCalls += 1
            return VoxResult.Success(
                VoxConversationMembers(
                    conversationId = conversationId,
                    membershipVersion = 1L,
                    members = listOf(VoxConversationMember("usr_1", "alice", "member")),
                    admins = emptyList(),
                    subscribers = emptyList(),
                    subscriptionState = null,
                    memberCount = null,
                ),
            )
        }

        override fun loadConversations(
            serverBaseUrl: String,
            accessToken: String,
        ): VoxResult<List<VoxConversationSummary>> {
            loadConversationsCalls += 1
            return VoxResult.Success(
                listOf(
                    VoxConversationSummary(
                        conversationId = CONVERSATION_ID,
                        type = 0,
                        createdBy = "usr_1",
                        createdByUsername = "alice",
                        createdAt = 1L,
                        membershipVersion = 1L,
                        lastActivityAt = 2L,
                    ),
                ),
            )
        }

        override fun loadHistory(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
            limit: Int,
        ): VoxResult<List<VoxConversationEnvelope>> {
            loadHistoryCalls += 1
            return VoxResult.Success(
                listOf(
                    VoxConversationEnvelope(
                        envelopeId = "env_1",
                        conversationId = conversationId,
                        senderUserId = "usr_remote",
                        senderDeviceId = "dev_remote",
                        ciphertext = "enc_payload_from_server",
                        serverTimestamp = 100L,
                        envelopeType = 0,
                        orderingEpoch = null,
                    ),
                ),
            )
        }

        override fun sendMessage(
            serverBaseUrl: String,
            accessToken: String,
            command: VoxSendMessageCommand,
        ): VoxResult<VoxSendMessageReceipt> =
            VoxResult.Success(
                VoxSendMessageReceipt(
                    envelopeId = command.envelopeId,
                    serverTimestamp = 101L,
                    deliveredToCount = 1,
                ),
            )
    }

    private companion object {
        const val BASE_URL = "https://vox.example/"
        const val TOKEN = "token"
        const val CONVERSATION_ID = "conv_1"
    }
}
