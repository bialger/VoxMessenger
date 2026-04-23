package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AuthSessionResponseDto
import com.bialger.voxclient.data.dto.HealthDto
import com.bialger.voxclient.data.dto.LoginRequestDto
import com.bialger.voxclient.data.dto.RefreshRequestDto
import com.bialger.voxclient.data.dto.RefreshResponseDto
import com.bialger.voxclient.data.dto.RegisterRequestDto
import com.bialger.voxclient.data.dto.SduiEventRequestDto
import com.bialger.voxclient.data.dto.SduiEventResponseDto
import com.bialger.voxclient.data.dto.SduiScreenDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

interface VoxPublicApi {
    @GET("v1/health")
    fun getHealth(): Call<HealthDto>

    @GET("v1/sdui/screen")
    fun getSduiScreen(
        @Query("platform") platform: String,
        @Query("device_id") deviceId: String,
        @Query("app_version_code") appVersionCode: Int,
        @Query("app_version_name") appVersionName: String?,
        @Query("locale") locale: String?,
    ): Call<SduiScreenDto?>

    @POST("v1/sdui/event")
    fun postSduiEvent(
        @Body request: SduiEventRequestDto,
    ): Call<SduiEventResponseDto>

    @POST("v1/register")
    fun register(@Body request: RegisterRequestDto): Call<AuthSessionResponseDto>

    @POST("v1/login")
    fun login(@Body request: LoginRequestDto): Call<AuthSessionResponseDto>

    @POST("v1/refresh")
    fun refresh(@Body request: RefreshRequestDto): Call<RefreshResponseDto>
}