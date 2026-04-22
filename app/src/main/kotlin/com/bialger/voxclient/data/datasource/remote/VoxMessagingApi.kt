package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AckEnvelopeRequestDto
import com.bialger.voxclient.data.dto.EmptyResponseDto
import com.bialger.voxclient.data.dto.SendMessageRequestDto
import com.bialger.voxclient.data.dto.SendMessageResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface VoxMessagingApi {
    @POST("v1/messages/send")
    fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: SendMessageRequestDto,
    ): Call<SendMessageResponseDto>

    @POST("v1/messages/ack")
    fun ackMessage(
        @Header("Authorization") authorization: String,
        @Body request: AckEnvelopeRequestDto,
    ): Call<EmptyResponseDto>
}
