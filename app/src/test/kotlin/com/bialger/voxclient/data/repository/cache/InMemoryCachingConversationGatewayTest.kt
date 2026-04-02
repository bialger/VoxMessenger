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
    fun sendMessage_invalidatesHistoryCacheForConversation() {
        val delegate = FakeConversationGateway()
        val gateway = InMemoryCachingConversationGateway(delegate)

        gateway.loadHistory(BASE_URL, TOKEN, CONVERSATION_ID, 50)
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

        assertEquals(2, delegate.loadHistoryCalls)
    }

    private class FakeConversationGateway : ConversationGateway {
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
        ): VoxResult<VoxConversationDetail> =
            VoxResult.Success(
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

        override fun loadConversationMembers(
            serverBaseUrl: String,
            accessToken: String,
            conversationId: String,
        ): VoxResult<VoxConversationMembers> =
            VoxResult.Success(
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

        override fun loadConversations(
            serverBaseUrl: String,
            accessToken: String,
        ): VoxResult<List<VoxConversationSummary>> = VoxResult.Success(emptyList())

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
