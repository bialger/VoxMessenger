package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.VoxWebSocketEventDto
import com.bialger.voxclient.data.dto.WebSocketAuthFrameDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class VoxWebSocketService(
    private val baseUrl: String,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = GsonBuilder().create(),
) {
    fun connectWithBearer(
        accessToken: String,
        listener: WebSocketListener,
    ): WebSocket {
        val request =
            Request
                .Builder()
                .url(resolveWebSocketUrl())
                .header(HEADER_AUTHORIZATION, "Bearer $accessToken")
                .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
                .build()
        return okHttpClient.newWebSocket(request, listener)
    }

    fun connectWithDeferredAuth(
        accessToken: String,
        listener: WebSocketListener,
    ): WebSocket {
        val authSendingListener =
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(gson.toJson(WebSocketAuthFrameDto(accessToken = accessToken)))
                    listener.onOpen(webSocket, response)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    listener.onMessage(webSocket, text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    listener.onMessage(webSocket, bytes)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosing(webSocket, code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosed(webSocket, code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    listener.onFailure(webSocket, t, response)
                }
            }

        val request =
            Request
                .Builder()
                .url(resolveWebSocketUrl())
                .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
                .build()

        return okHttpClient.newWebSocket(request, authSendingListener)
    }

    fun parseEvent(rawPayload: String): VoxWebSocketEventDto {
        val jsonObject = runCatching { JsonParser.parseString(rawPayload).asJsonObject }.getOrNull()
            ?: return VoxWebSocketEventDto.UnknownEvent(type = null, rawPayload = rawPayload)

        val type = jsonObject.stringOrNull(KEY_TYPE)
        return when (type) {
            TYPE_ENVELOPE -> gson.fromJson(rawPayload, VoxWebSocketEventDto.EnvelopeEvent::class.java)
            TYPE_CONVERSATION_MEMBERSHIP_CHANGED ->
                gson.fromJson(rawPayload, VoxWebSocketEventDto.ConversationMembershipChangedEvent::class.java)
            TYPE_USER_DEVICES_CHANGED ->
                gson.fromJson(rawPayload, VoxWebSocketEventDto.UserDevicesChangedEvent::class.java)
            TYPE_SYNC_RECORD_CHANGED ->
                gson.fromJson(rawPayload, VoxWebSocketEventDto.SyncRecordChangedEvent::class.java)
            else -> VoxWebSocketEventDto.UnknownEvent(type = type, rawPayload = rawPayload)
        }
    }

    private fun resolveWebSocketUrl(): String {
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: error("Base URL must be a valid HTTP(S) URL.")

        val httpUrlWithPath = httpUrl.newBuilder().encodedPath("/v1/ws").build().toString()
        return when (httpUrl.scheme) {
            SCHEME_HTTPS -> httpUrlWithPath.replaceFirst("$SCHEME_HTTPS://", "$SCHEME_WSS://")
            SCHEME_HTTP -> httpUrlWithPath.replaceFirst("$SCHEME_HTTP://", "$SCHEME_WS://")
            else -> error("Unsupported scheme '${httpUrl.scheme}' for WebSocket base URL.")
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        if (has(name) && !get(name).isJsonNull) get(name).asString else null

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_USER_AGENT = "User-Agent"
        const val USER_AGENT_VALUE = "VoxAndroid"

        const val SCHEME_HTTP = "http"
        const val SCHEME_HTTPS = "https"
        const val SCHEME_WS = "ws"
        const val SCHEME_WSS = "wss"

        const val KEY_TYPE = "type"
        const val TYPE_ENVELOPE = "envelope"
        const val TYPE_CONVERSATION_MEMBERSHIP_CHANGED = "conversation_membership_changed"
        const val TYPE_USER_DEVICES_CHANGED = "user_devices_changed"
        const val TYPE_SYNC_RECORD_CHANGED = "sync_record_changed"
    }
}
