package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AdminStatsDto
import com.bialger.voxclient.data.dto.EmptyResponseDto
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VoxAdminApi {
    @GET("v1/admin/stats")
    fun loadStats(@Header("X-Admin-Token") adminToken: String): Call<AdminStatsDto>

    @DELETE("v1/admin/users/{user_id}")
    fun deleteUser(
        @Header("X-Admin-Token") adminToken: String,
        @Path("user_id") userId: String,
    ): Call<EmptyResponseDto>
}
