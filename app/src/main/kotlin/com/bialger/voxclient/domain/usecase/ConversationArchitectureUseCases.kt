package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.boundary.AddConversationMemberInputPort
import com.bialger.voxclient.domain.boundary.CreateChannelInputPort
import com.bialger.voxclient.domain.boundary.CreateDmInputPort
import com.bialger.voxclient.domain.boundary.CreateGroupInputPort
import com.bialger.voxclient.domain.boundary.LoadChatListInputPort
import com.bialger.voxclient.domain.boundary.LoadConversationDetailsInputPort
import com.bialger.voxclient.domain.boundary.LoadConversationHistoryInputPort
import com.bialger.voxclient.domain.boundary.LoadConversationMembersInputPort
import com.bialger.voxclient.domain.boundary.ResolveUserIdByUsernameInputPort
import com.bialger.voxclient.domain.boundary.ResolveUsernamesByUserIdsInputPort
import com.bialger.voxclient.domain.boundary.SendMessageInputPort
import com.bialger.voxclient.domain.boundary.SubscribeToChannelInputPort
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt
import com.bialger.voxclient.domain.repository.ConversationGateway

class LoadChatListUseCase(
    private val conversationGateway: ConversationGateway,
) : LoadChatListInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
    ): VoxResult<List<VoxConversationSummary>> {
        if (serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (accessToken.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Access token must not be blank."))
        }
        return conversationGateway.loadConversations(serverBaseUrl = serverBaseUrl, accessToken = accessToken)
    }
}

class ResolveUserIdByUsernameUseCase(
    private val conversationGateway: ConversationGateway,
) : ResolveUserIdByUsernameInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        username: String,
    ): VoxResult<String> {
        if (username.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Username must not be blank."))
        }
        return conversationGateway.resolveUserIdByUsername(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            username = username,
        )
    }
}

class ResolveUsernamesByUserIdsUseCase(
    private val conversationGateway: ConversationGateway,
) : ResolveUsernamesByUserIdsInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>> {
        return conversationGateway.resolveUsernamesByUserIds(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            userIds = userIds,
        )
    }
}

class CreateDmUseCase(
    private val conversationGateway: ConversationGateway,
) : CreateDmInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        peerUserId: String,
    ): VoxResult<String> {
        if (peerUserId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Peer user id must not be blank."))
        }
        return conversationGateway.createDmConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            peerUserId = peerUserId,
        )
    }
}

class CreateGroupUseCase(
    private val conversationGateway: ConversationGateway,
) : CreateGroupInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        memberUserIds: List<String>,
    ): VoxResult<String> {
        val members = memberUserIds.filter { it.isNotBlank() }.distinct()
        if (members.isEmpty()) {
            return VoxResult.Failure(VoxError.Validation("At least one member is required."))
        }
        return conversationGateway.createGroupConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            memberUserIds = members,
        )
    }
}

class CreateChannelUseCase(
    private val conversationGateway: ConversationGateway,
) : CreateChannelInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        adminUserIds: List<String>,
        subscriberUserIds: List<String>,
    ): VoxResult<String> {
        val admins = adminUserIds.filter { it.isNotBlank() }.distinct()
        if (admins.isEmpty()) {
            return VoxResult.Failure(VoxError.Validation("At least one admin is required."))
        }
        return conversationGateway.createChannelConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            adminUserIds = admins,
            subscriberUserIds = subscriberUserIds.filter { it.isNotBlank() }.distinct(),
        )
    }
}

class SubscribeToChannelUseCase(
    private val conversationGateway: ConversationGateway,
) : SubscribeToChannelInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<Unit> {
        if (conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        return conversationGateway.subscribeToChannel(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
        )
    }
}

class LoadConversationDetailsUseCase(
    private val conversationGateway: ConversationGateway,
) : LoadConversationDetailsInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationDetail> {
        if (conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        return conversationGateway.loadConversationDetails(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
        )
    }
}

class LoadConversationMembersUseCase(
    private val conversationGateway: ConversationGateway,
) : LoadConversationMembersInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationMembers> {
        if (conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        return conversationGateway.loadConversationMembers(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
        )
    }
}

class AddConversationMemberUseCase(
    private val conversationGateway: ConversationGateway,
) : AddConversationMemberInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        userId: String,
        role: String?,
    ): VoxResult<Unit> {
        if (conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        if (userId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("User id must not be blank."))
        }
        return conversationGateway.addConversationMember(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
            userId = userId,
            role = role,
        )
    }
}

class LoadConversationHistoryUseCase(
    private val conversationGateway: ConversationGateway,
) : LoadConversationHistoryInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        limit: Int,
    ): VoxResult<List<VoxConversationEnvelope>> {
        if (conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        val normalizedLimit = limit.coerceIn(1, MAX_HISTORY_LIMIT)
        return conversationGateway.loadHistory(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            conversationId = conversationId,
            limit = normalizedLimit,
        )
    }

    private companion object {
        const val MAX_HISTORY_LIMIT = 200
    }
}

class SendMessageUseCase(
    private val conversationGateway: ConversationGateway,
) : SendMessageInputPort {
    override operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        command: VoxSendMessageCommand,
    ): VoxResult<VoxSendMessageReceipt> {
        if (command.deviceId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Device id must not be blank."))
        }
        if (command.conversationId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Conversation id must not be blank."))
        }
        if (command.ciphertext.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Ciphertext must not be blank."))
        }
        if (command.envelopeId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Envelope id must not be blank."))
        }

        return conversationGateway.sendMessage(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            command = command,
        )
    }
}
