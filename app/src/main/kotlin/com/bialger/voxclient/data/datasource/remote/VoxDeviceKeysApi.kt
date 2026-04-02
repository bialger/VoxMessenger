package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.DevicePreKeyBundleDto
import com.bialger.voxclient.data.dto.EmptyResponseDto
import com.bialger.voxclient.data.dto.PublishPreKeysRequestDto
import com.bialger.voxclient.data.dto.RotateSignedPreKeyRequestDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface VoxDeviceKeysApi {
    @POST("v1/devices/{device_id}/prekeys")
    fun publishOneTimePreKeys(
        @Header("Authorization") authorization: String,
        @Path("device_id") deviceId: String,
        @Body request: PublishPreKeysRequestDto,
    ): Call<EmptyResponseDto>

    @PUT("v1/devices/{device_id}/signed-prekey")
    fun rotateSignedPreKey(
        @Header("Authorization") authorization: String,
        @Path("device_id") deviceId: String,
        @Body request: RotateSignedPreKeyRequestDto,
    ): Call<EmptyResponseDto>

    @GET("v1/devices/{device_id}/prekey-bundle")
    fun getDevicePreKeyBundle(
        @Header("Authorization") authorization: String,
        @Path("device_id") deviceId: String,
    ): Call<DevicePreKeyBundleDto>
}
