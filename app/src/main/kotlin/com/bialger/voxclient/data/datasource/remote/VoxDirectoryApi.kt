package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.UserDevicesResponseDto
import com.bialger.voxclient.data.dto.UserDto
import com.bialger.voxclient.data.dto.UserPreKeyBundlesResponseDto
import com.bialger.voxclient.data.dto.UsersResponseDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface VoxDirectoryApi {
    @GET("v1/users/by-username/{username}")
    fun resolveByUsername(
        @Header("Authorization") authorization: String,
        @Path("username") username: String,
    ): Call<UserDto>

    @GET("v1/users/search")
    fun searchUsers(
        @Header("Authorization") authorization: String,
        @Query("q") query: String?,
        @Query("limit") limit: Int?,
    ): Call<UsersResponseDto>

    @GET("v1/users/{user_id}")
    fun getUser(
        @Header("Authorization") authorization: String,
        @Path("user_id") userId: String,
    ): Call<UserDto>

    @GET("v1/users/{user_id}/devices")
    fun getUserDevices(
        @Header("Authorization") authorization: String,
        @Path("user_id") userId: String,
    ): Call<UserDevicesResponseDto>

    @GET("v1/users/{user_id}/prekey-bundles")
    fun getUserPreKeyBundles(
        @Header("Authorization") authorization: String,
        @Path("user_id") userId: String,
    ): Call<UserPreKeyBundlesResponseDto>
}
