package com.bialger.voxclient.data.repository

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.dto.AddConversationMemberRequestDto
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.data.dto.ConversationDetailDto
import com.bialger.voxclient.data.dto.ConversationMembersResponseDto
import com.bialger.voxclient.data.dto.ConversationSummaryDto
import com.bialger.voxclient.data.dto.CreateConversationRequestDto
import com.bialger.voxclient.data.dto.EnvelopeDto
import com.bialger.voxclient.data.dto.SendMessageRequestDto
import com.bialger.voxclient.data.dto.SendMessageResponseDto
import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

class VoxConversationRemoteRepository(
    private val apiFactory: VoxPublicApiFactory = VoxPublicApiFactory(),
    private val gson: Gson = Gson(),
) {

    fun resolveUsernameByUserId(
        serverBaseUrl: String,
        accessToken: String,
        userId: String,
    ): VoxResult<String> {
        return try {
            val directoryApi = apiFactory.createDirectoryApi(serverBaseUrl)
            val response =
                directoryApi
                    .getUser(
                        authorization = bearer(accessToken),
                        userId = userId,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to resolve username (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Resolve username response body is empty."),
                    )
                VoxResult.Success(body.username)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Resolving username failed unexpectedly.", t),
            )
        }
    }

    fun resolveUsernamesByUserIds(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>> {
        val normalizedIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) {
            return VoxResult.Success(emptyMap())
        }

        return try {
            val directoryApi = apiFactory.createDirectoryApi(serverBaseUrl)
            val resolved = linkedMapOf<String, String>()
            val unresolved = normalizedIds.toMutableSet()

            normalizedIds.chunked(MAX_USER_BATCH_SIZE).forEach { chunk ->
                val csvResponse =
                    directoryApi
                        .getUsersByIds(
                            authorization = bearer(accessToken),
                            ids = chunk.joinToString(","),
                        ).execute()

                val response =
                    if (csvResponse.isSuccessful) {
                        csvResponse
                    } else {
                        directoryApi
                            .getUsersByIdsRepeated(
                                authorization = bearer(accessToken),
                                ids = chunk,
                            ).execute()
                    }

                if (response.isSuccessful) {
                    val body = response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Resolve users batch response body is empty."),
                    )
                    body.users.forEach { user ->
                        if (user.userId.isNotBlank() && user.username.isNotBlank()) {
                            resolved[user.userId] = user.username
                            unresolved.remove(user.userId)
                        }
                    }
                }
            }

            // Fallback for ids omitted from batch response (unknown/disabled/restricted deployments).
            unresolved.forEach { unresolvedId ->
                when (
                    val single =
                        resolveUsernameByUserId(
                            serverBaseUrl = serverBaseUrl,
                            accessToken = accessToken,
                            userId = unresolvedId,
                        )
                ) {
                    is VoxResult.Success -> {
                        if (single.value.isNotBlank()) {
                            resolved[unresolvedId] = single.value
                        }
                    }

                    is VoxResult.Failure -> Unit
                }
            }

            VoxResult.Success(resolved)
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Resolving users batch failed unexpectedly.", t),
            )
        }
    }

    fun resolveUserIdByUsername(
        serverBaseUrl: String,
        accessToken: String,
        username: String,
    ): VoxResult<String> {
        return try {
            val response = apiFactory
                .createDirectoryApi(serverBaseUrl)
                .resolveByUsername(
                    authorization = bearer(accessToken),
                    username = username,
                ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to resolve user (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body = response.body()
                    ?: return VoxResult.Failure(
                        VoxError.Unknown("Resolve user response body is empty."),
                    )
                VoxResult.Success(body.userId)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Resolving user failed unexpectedly.", t),
            )
        }
    }

    fun createDmConversation(
        serverBaseUrl: String,
        accessToken: String,
        peerUserId: String,
    ): VoxResult<String> {
        return try {
            val response = apiFactory
                .createConversationApi(serverBaseUrl)
                .createConversation(
                    authorization = bearer(accessToken),
                    request = CreateConversationRequestDto(
                        type = "dm",
                        peerUserId = peerUserId,
                    ),
                ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to create conversation (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body = response.body()
                    ?: return VoxResult.Failure(
                        VoxError.Unknown("Create conversation response body is empty."),
                    )
                VoxResult.Success(body.conversationId)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Creating conversation failed unexpectedly.", t),
            )
        }
    }

    fun createGroupConversation(
        serverBaseUrl: String,
        accessToken: String,
        memberUserIds: List<String>,
    ): VoxResult<String> {
        return createConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            request =
                CreateConversationRequestDto(
                    type = "group",
                    members = memberUserIds,
                ),
            errorPrefix = "Unable to create group",
        )
    }

    fun createChannelConversation(
        serverBaseUrl: String,
        accessToken: String,
        adminUserIds: List<String>,
        subscriberUserIds: List<String>,
    ): VoxResult<String> {
        return createConversation(
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            request =
                CreateConversationRequestDto(
                    type = "channel",
                    admins = adminUserIds,
                    subscribers = subscriberUserIds,
                ),
            errorPrefix = "Unable to create channel",
        )
    }

    fun addConversationMember(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        userId: String,
        role: String? = null,
    ): VoxResult<Unit> {
        return try {
            val response =
                apiFactory
                    .createConversationApi(serverBaseUrl)
                    .addConversationMember(
                        authorization = bearer(accessToken),
                        conversationId = conversationId,
                        request = AddConversationMemberRequestDto(userId = userId, role = role),
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to add conversation member (HTTP ${response.code()}).",
                    ),
                )
            } else {
                VoxResult.Success(Unit)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Adding conversation member failed unexpectedly.", t),
            )
        }
    }

    fun subscribeToChannel(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<Unit> {
        return try {
            val response =
                apiFactory
                    .createConversationApi(serverBaseUrl)
                    .subscribe(
                        authorization = bearer(accessToken),
                        conversationId = conversationId,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to subscribe to channel (HTTP ${response.code()}).",
                    ),
                )
            } else {
                VoxResult.Success(Unit)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Subscribing to channel failed unexpectedly.", t),
            )
        }
    }

    fun loadConversationDetails(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<ConversationDetailDto> {
        return try {
            val response =
                apiFactory
                    .createConversationApi(serverBaseUrl)
                    .getConversation(
                        authorization = bearer(accessToken),
                        conversationId = conversationId,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to load conversation details (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Conversation details response body is empty."),
                    )
                VoxResult.Success(body)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Loading conversation details failed unexpectedly.", t),
            )
        }
    }

    fun loadConversationMembers(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
    ): VoxResult<ConversationMembersResponseDto> {
        return try {
            val response =
                apiFactory
                    .createConversationApi(serverBaseUrl)
                    .getConversationMembers(
                        authorization = bearer(accessToken),
                        conversationId = conversationId,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to load conversation members (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Conversation members response body is empty."),
                    )
                VoxResult.Success(body)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Loading conversation members failed unexpectedly.", t),
            )
        }
    }

    fun loadConversations(serverBaseUrl: String, accessToken: String): VoxResult<List<ConversationSummaryDto>> {
        return try {
            val response = apiFactory
                .createConversationApi(serverBaseUrl)
                .loadConversations(bearer(accessToken))
                .execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to load conversations (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Conversation list response body is empty."),
                    )
                VoxResult.Success(body.conversations)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Loading conversations failed unexpectedly.", t),
            )
        }
    }

    fun loadHistory(
        serverBaseUrl: String,
        accessToken: String,
        conversationId: String,
        limit: Int = DEFAULT_HISTORY_LIMIT,
    ): VoxResult<List<EnvelopeDto>> {
        return try {
            val response = apiFactory
                .createConversationApi(serverBaseUrl)
                .loadConversationHistory(
                    authorization = bearer(accessToken),
                    conversationId = conversationId,
                    limit = limit,
                    cursor = null,
                    since = null,
                ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to load conversation history (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Conversation history response body is empty."),
                    )
                VoxResult.Success(body.envelopes)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Loading conversation history failed unexpectedly.", t),
            )
        }
    }

    fun sendMessage(
        serverBaseUrl: String,
        accessToken: String,
        request: SendMessageRequestDto,
    ): VoxResult<SendMessageResponseDto> {
        return try {
            val response = apiFactory
                .createMessagingApi(serverBaseUrl)
                .sendMessage(
                    authorization = bearer(accessToken),
                    request = request,
                ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "Unable to send message (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Send message response body is empty."),
                    )
                VoxResult.Success(body)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Sending message failed unexpectedly.", t),
            )
        }
    }

    private fun createConversation(
        serverBaseUrl: String,
        accessToken: String,
        request: CreateConversationRequestDto,
        errorPrefix: String,
    ): VoxResult<String> {
        return try {
            val response =
                apiFactory
                    .createConversationApi(serverBaseUrl)
                    .createConversation(
                        authorization = bearer(accessToken),
                        request = request,
                    ).execute()

            if (!response.isSuccessful) {
                VoxResult.Failure(
                    VoxError.Network(
                        code = response.code(),
                        message = extractErrorMessage(response) ?: "$errorPrefix (HTTP ${response.code()}).",
                    ),
                )
            } else {
                val body =
                    response.body() ?: return VoxResult.Failure(
                        VoxError.Unknown("Create conversation response body is empty."),
                    )
                VoxResult.Success(body.conversationId)
            }
        } catch (ioe: IOException) {
            VoxResult.Failure(
                VoxError.Network(
                    code = NETWORK_IO_ERROR_CODE,
                    message = ioe.message ?: "Unable to reach the server.",
                ),
            )
        } catch (t: Throwable) {
            VoxResult.Failure(
                VoxError.Unknown("Creating conversation failed unexpectedly.", t),
            )
        }
    }

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    private fun extractErrorMessage(response: Response<*>): String? {
        val raw = response.errorBody()?.string()?.trim().orEmpty()
        if (raw.isEmpty()) {
            return null
        }
        return try {
            gson.fromJson(raw, ApiErrorEnvelopeDto::class.java)?.error?.message
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val NETWORK_IO_ERROR_CODE = -1
        const val DEFAULT_HISTORY_LIMIT = 100
        const val MAX_USER_BATCH_SIZE = 100
    }
}
