package com.bialger.voxclient.domain.boundary

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt

interface LoadChatListInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String): VoxResult<List<VoxConversationSummary>>
}

interface ResolveUserIdByUsernameInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String, username: String): VoxResult<String>
}

interface ResolveUsernamesByUserIdsInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>>
}

interface CreateDmInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String, peerUserId: String): VoxResult<String>
}

interface CreateGroupInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        memberUserIds: List<String>,
    ): VoxResult<String>
}

interface CreateChannelInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        adminUserIds: List<String>,
        subscriberUserIds: List<String>,
    ): VoxResult<String>
}

interface SubscribeToChannelInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String, conversationId: String): VoxResult<Unit>
}

interface LoadConversationDetailsInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationDetail>
}

interface LoadConversationMembersInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationMembers>
}

interface AddConversationMemberInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        userId: String,
        role: String?,
    ): VoxResult<Unit>
}

interface LoadConversationHistoryInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        limit: Int = 100,
    ): VoxResult<List<VoxConversationEnvelope>>
}

interface SendMessageInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        command: VoxSendMessageCommand,
    ): VoxResult<VoxSendMessageReceipt>
}
