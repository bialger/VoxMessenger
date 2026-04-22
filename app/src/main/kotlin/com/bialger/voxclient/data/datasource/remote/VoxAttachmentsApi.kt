package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.EmptyResponseDto
import com.bialger.voxclient.data.dto.FinalizeAttachmentRequestDto
import com.bialger.voxclient.data.dto.UploadAttachmentInitRequestDto
import com.bialger.voxclient.data.dto.UploadAttachmentInitResponseDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface VoxAttachmentsApi {
    @POST("v1/attachments/upload-init")
    fun initUpload(
        @Header("Authorization") authorization: String,
        @Body request: UploadAttachmentInitRequestDto,
    ): Call<UploadAttachmentInitResponseDto>

    @PUT("v1/attachments/{attachment_id}/chunk")
    fun uploadChunk(
        @Header("Authorization") authorization: String,
        @Path("attachment_id") attachmentId: String,
        @Query("offset") offset: Long,
        @Body chunk: RequestBody,
    ): Call<EmptyResponseDto>

    @POST("v1/attachments/{attachment_id}/finalize")
    fun finalizeUpload(
        @Header("Authorization") authorization: String,
        @Path("attachment_id") attachmentId: String,
        @Body request: FinalizeAttachmentRequestDto,
    ): Call<EmptyResponseDto>

    @Streaming
    @GET("v1/attachments/{attachment_id}")
    fun downloadAttachment(
        @Header("Authorization") authorization: String,
        @Path("attachment_id") attachmentId: String,
    ): Call<ResponseBody>
}
