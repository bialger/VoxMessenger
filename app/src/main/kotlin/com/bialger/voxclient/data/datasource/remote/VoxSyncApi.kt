package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.DeleteSyncRecordRequestDto
import com.bialger.voxclient.data.dto.EnvelopesPageResponseDto
import com.bialger.voxclient.data.dto.SyncChangesResponseDto
import com.bialger.voxclient.data.dto.SyncKeyBundleDto
import com.bialger.voxclient.data.dto.SyncRecordMutationResponseDto
import com.bialger.voxclient.data.dto.UpdateSyncKeyBundleRequestDto
import com.bialger.voxclient.data.dto.UpdateSyncKeyBundleResponseDto
import com.bialger.voxclient.data.dto.PutSyncRecordRequestDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface VoxSyncApi {
    @GET("v1/sync/key-bundle")
    fun getSyncKeyBundle(@Header("Authorization") authorization: String): Call<SyncKeyBundleDto>

    @PUT("v1/sync/key-bundle")
    fun updateSyncKeyBundle(
        @Header("Authorization") authorization: String,
        @Body request: UpdateSyncKeyBundleRequestDto,
    ): Call<UpdateSyncKeyBundleResponseDto>

    @GET("v1/sync/changes")
    fun getSyncChanges(
        @Header("Authorization") authorization: String,
        @Query("collection") collection: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int?,
    ): Call<SyncChangesResponseDto>

    @PUT("v1/sync/records/{collection}/{record_id}")
    fun putSyncRecord(
        @Header("Authorization") authorization: String,
        @Path("collection") collection: String,
        @Path("record_id") recordId: String,
        @Body request: PutSyncRecordRequestDto,
    ): Call<SyncRecordMutationResponseDto>

    @HTTP(method = "DELETE", path = "v1/sync/records/{collection}/{record_id}", hasBody = true)
    fun deleteSyncRecord(
        @Header("Authorization") authorization: String,
        @Path("collection") collection: String,
        @Path("record_id") recordId: String,
        @Body request: DeleteSyncRecordRequestDto,
    ): Call<SyncRecordMutationResponseDto>

    @GET("v1/sync/pending")
    fun loadPendingEnvelopes(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int?,
        @Query("cursor") cursor: String?,
    ): Call<EnvelopesPageResponseDto>
}
