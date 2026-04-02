package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AddConversationMemberRequestDto
import com.bialger.voxclient.data.dto.ConversationDetailDto
import com.bialger.voxclient.data.dto.ConversationMembersResponseDto
import com.bialger.voxclient.data.dto.ConversationsResponseDto
import com.bialger.voxclient.data.dto.CreateConversationRequestDto
import com.bialger.voxclient.data.dto.CreateConversationResponseDto
import com.bialger.voxclient.data.dto.EmptyResponseDto
import com.bialger.voxclient.data.dto.EnvelopesPageResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VoxConversationApi {
    @GET("v1/conversations")
    fun loadConversations(@Header("Authorization") authorization: String): Call<ConversationsResponseDto>

    @GET("v1/conversations/{conversation_id}")
    fun getConversation(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
    ): Call<ConversationDetailDto>

    @GET("v1/conversations/{conversation_id}/members")
    fun getConversationMembers(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
    ): Call<ConversationMembersResponseDto>

    @POST("v1/conversations")
    fun createConversation(
        @Header("Authorization") authorization: String,
        @Body request: CreateConversationRequestDto,
    ): Call<CreateConversationResponseDto>

    @POST("v1/conversations/{conversation_id}/members")
    fun addConversationMember(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
        @Body request: AddConversationMemberRequestDto,
    ): Call<EmptyResponseDto>

    @DELETE("v1/conversations/{conversation_id}/members/{user_id}")
    fun removeConversationMember(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
        @Path("user_id") userId: String,
    ): Call<EmptyResponseDto>

    @POST("v1/conversations/{conversation_id}/subscribe")
    fun subscribe(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
    ): Call<EmptyResponseDto>

    @POST("v1/conversations/{conversation_id}/unsubscribe")
    fun unsubscribe(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
    ): Call<EmptyResponseDto>

    @GET("v1/conversations/{conversation_id}/envelopes")
    fun loadConversationHistory(
        @Header("Authorization") authorization: String,
        @Path("conversation_id") conversationId: String,
        @Query("limit") limit: Int?,
        @Query("cursor") cursor: String?,
        @Query("since") since: Long?,
    ): Call<EnvelopesPageResponseDto>
}
