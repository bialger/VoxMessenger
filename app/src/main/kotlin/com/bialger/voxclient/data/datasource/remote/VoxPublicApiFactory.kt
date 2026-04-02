package com.bialger.voxclient.data.datasource.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VoxPublicApiFactory(
    private val gson: Gson = GsonBuilder().create(),
    private val baseClient: OkHttpClient = OkHttpClient(),
) {
    fun create(serverBaseUrl: String): VoxPublicApi = retrofit(serverBaseUrl).create(VoxPublicApi::class.java)

    fun createAccountApi(serverBaseUrl: String): VoxAccountApi = retrofit(serverBaseUrl).create(VoxAccountApi::class.java)

    fun createDirectoryApi(serverBaseUrl: String): VoxDirectoryApi = retrofit(serverBaseUrl).create(VoxDirectoryApi::class.java)

    fun createDeviceKeysApi(serverBaseUrl: String): VoxDeviceKeysApi = retrofit(serverBaseUrl).create(VoxDeviceKeysApi::class.java)

    fun createSyncApi(serverBaseUrl: String): VoxSyncApi = retrofit(serverBaseUrl).create(VoxSyncApi::class.java)

    fun createConversationApi(serverBaseUrl: String): VoxConversationApi = retrofit(serverBaseUrl).create(VoxConversationApi::class.java)

    fun createMessagingApi(serverBaseUrl: String): VoxMessagingApi = retrofit(serverBaseUrl).create(VoxMessagingApi::class.java)

    fun createAttachmentsApi(serverBaseUrl: String): VoxAttachmentsApi = retrofit(serverBaseUrl).create(VoxAttachmentsApi::class.java)

    fun createAdminApi(serverBaseUrl: String): VoxAdminApi = retrofit(serverBaseUrl).create(VoxAdminApi::class.java)

    private fun retrofit(serverBaseUrl: String): Retrofit {
        val client =
            baseClient
                .newBuilder()
                .addInterceptor { chain ->
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
                            .build()
                    chain.proceed(request)
                }.build()

        return Retrofit
            .Builder()
            .baseUrl(serverBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private companion object {
        const val HEADER_USER_AGENT = "User-Agent"
        const val USER_AGENT_VALUE = "VoxAndroid"
    }
}
