package com.bialger.voxclient.domain.repository

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxConversationDetail
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.domain.entity.VoxSendMessageReceipt

interface ConversationGateway {
    fun resolveUserIdByUsername(
        serverBaseUrl: String,
        accessToken: String,
        username: String,
    ): VoxResult<String>

    fun resolveUsernamesByUserIds(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>>

    fun createDmConversation(
        serverBaseUrl: String,
        accessToken: String,
        peerUserId: String,
    ): VoxResult<String>

    fun createGroupConversation(
        serverBaseUrl: String,
        accessToken: String,
        memberUserIds: List<String>,
    ): VoxResult<String>

    fun createChannelConversation(
        serverBaseUrl: String,
        accessToken: String,
        adminUserIds: List<String>,
        subscriberUserIds: List<String>,
    ): VoxResult<String>

    fun addConversationMember(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        userId: String,
        role: String? = null,
    ): VoxResult<Unit>

    fun subscribeToChannel(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<Unit>

    fun loadConversationDetails(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationDetail>

    fun loadConversationMembers(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<VoxConversationMembers>

    fun loadConversations(
        serverBaseUrl: String,
        accessToken: String,
    ): VoxResult<List<VoxConversationSummary>>

    fun loadHistory(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        limit: Int = 100,
    ): VoxResult<List<VoxConversationEnvelope>>

    fun sendMessage(
        serverBaseUrl: String,
        accessToken: String,
        command: VoxSendMessageCommand,
    ): VoxResult<VoxSendMessageReceipt>
}
