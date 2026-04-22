package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AuthSessionResponseDto
import com.bialger.voxclient.data.dto.HealthDto
import com.bialger.voxclient.data.dto.LoginRequestDto
import com.bialger.voxclient.data.dto.RefreshRequestDto
import com.bialger.voxclient.data.dto.RefreshResponseDto
import com.bialger.voxclient.data.dto.RegisterRequestDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface VoxPublicApi {
    @GET("v1/health")
    fun getHealth(): Call<HealthDto>

    @POST("v1/register")
    fun register(@Body request: RegisterRequestDto): Call<AuthSessionResponseDto>

    @POST("v1/login")
    fun login(@Body request: LoginRequestDto): Call<AuthSessionResponseDto>

    @POST("v1/refresh")
    fun refresh(@Body request: RefreshRequestDto): Call<RefreshResponseDto>
}
