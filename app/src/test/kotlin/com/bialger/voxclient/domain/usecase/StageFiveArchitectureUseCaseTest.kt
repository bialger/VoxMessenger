package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMember
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt
import com.bialger.voxclient.domain.entity.VoxSyncWrapParams
import com.bialger.voxclient.domain.entity.VoxUserProfile
import com.bialger.voxclient.domain.repository.AuthSessionGateway
import com.bialger.voxclient.domain.repository.ConversationGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageFiveArchitectureUseCaseTest {
    @Test
    fun loginUseCase_failsWhenUsernameIsBlank() {
        val gateway = FakeAuthSessionGateway()
        val useCase = LoginUseCase(gateway)

        val result =
            useCase(
                VoxLoginCommand(
                    serverBaseUrl = "https://vox.example/",
                    username = "   ",
                    passwordDerivedValue = "pwd",
                    deviceId = "dev_1",
                ),
            )

        val failure = result as VoxResult.Failure
        assertTrue(failure.error is VoxError.Validation)
        assertEquals(0, gateway.loginCalls)
    }

    @Test
    fun loadChatListUseCase_delegatesToConversationGateway() {
        val gateway = FakeConversationGateway()
        val useCase = LoadChatListUseCase(gateway)

        val result = useCase(serverBaseUrl = "https://vox.example/", accessToken = "token")

        assertTrue(result is VoxResult.Success)
        assertEquals("https://vox.example/", gateway.lastServerBaseUrl)
        assertEquals("token", gateway.lastAccessToken)
    }

    @Test
    fun sendMessageUseCase_failsWhenCiphertextIsBlank() {
        val gateway = FakeConversationGateway()
        val useCase = SendMessageUseCase(gateway)

        val result =
            useCase(
                serverBaseUrl = "https://vox.example/",
                accessToken = "token",
                command =
                    VoxSendMessageCommand(
                        deviceId = "dev_1",
                        conversationId = "conv_1",
                        ciphertext = "  ",
                        envelopeId = "env_1",
                    ),
            )

        val failure = result as VoxResult.Failure
        assertTrue(failure.error is VoxError.Validation)
        assertEquals(0, gateway.sendMessageCalls)
    }

    private class FakeAuthSessionGateway : AuthSessionGateway {
        var loginCalls: Int = 0

        override fun login(command: VoxLoginCommand): VoxResult<VoxAuthSession> {
            loginCalls += 1
            return VoxResult.Success(
                VoxAuthSession(
                    userId = "usr_1",
                    accessToken = "token",
                    refreshToken = "refresh",
                    deviceId = command.deviceId,
                    syncKeyVersion = 1,
                ),
            )
        }

        override fun register(command: VoxRegisterCommand): VoxResult<VoxAuthSession> =
            VoxResult.Success(
                VoxAuthSession(
                    userId = "usr_1",
                    accessToken = "token",
                    refreshToken = "refresh",
                    deviceId = command.deviceId,
                    syncKeyVersion = 1,
                ),
            )

        override fun loadCurrentUser(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile> =
            VoxResult.Success(
                VoxUserProfile(
                    userId = "usr_1",
                    username = "alice",
                    currentDeviceId = "dev_1",
                    syncKeyVersion = 1,
                ),
            )
    }

    private class FakeConversationGateway : ConversationGateway {
        var lastServerBaseUrl: String? = null
        var lastAccessToken: String? = null
        var sendMessageCalls: Int = 0

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
        ): VoxResult<String> = VoxResult.Success("conv_dm")

        override fun createGroupConversation(
            serverBaseUrl: String,
            accessToken: String,
            memberUserIds: List<String>,
        ): VoxResult<String> = VoxResult.Success("conv_group")

        override fun createChannelConversation(
            serverBaseUrl: String,
            accessToken: String,
            adminUserIds: List<String>,
            subscriberUserIds: List<String>,
        ): VoxResult<String> = VoxResult.Success("conv_channel")

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
        ): VoxResult<List<VoxConversationSummary>> {
            lastServerBaseUrl = serverBaseUrl
            lastAccessToken = accessToken
            return VoxResult.Success(
                listOf(
                    VoxConversationSummary(
                        conversationId = "conv_1",
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
        ): VoxResult<List<VoxConversationEnvelope>> = VoxResult.Success(emptyList())

        override fun sendMessage(
            serverBaseUrl: String,
            accessToken: String,
            command: VoxSendMessageCommand,
        ): VoxResult<VoxSendMessageReceipt> {
            sendMessageCalls += 1
            return VoxResult.Success(
                VoxSendMessageReceipt(
                    envelopeId = command.envelopeId,
                    serverTimestamp = 1L,
                    deliveredToCount = 1,
                ),
            )
        }
    }
}
