package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.ChangePasswordRequestDto
import com.bialger.voxclient.data.dto.ChangePasswordResponseDto
import com.bialger.voxclient.data.dto.EmptyResponseDto
import com.bialger.voxclient.data.dto.MeResponseDto
import com.bialger.voxclient.data.dto.MyDevicesResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface VoxAccountApi {
    @GET("v1/me")
    fun me(@Header("Authorization") authorization: String): Call<MeResponseDto>

    @POST("v1/logout")
    fun logout(
        @Header("Authorization") authorization: String,
        @Body request: EmptyResponseDto = EmptyResponseDto(),
    ): Call<EmptyResponseDto>

    @POST("v1/account/change-password")
    fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: ChangePasswordRequestDto,
    ): Call<ChangePasswordResponseDto>

    @GET("v1/me/devices")
    fun loadMyDevices(@Header("Authorization") authorization: String): Call<MyDevicesResponseDto>

    @DELETE("v1/me/devices/{device_id}")
    fun revokeDevice(
        @Header("Authorization") authorization: String,
        @Path("device_id") deviceId: String,
    ): Call<EmptyResponseDto>
}
