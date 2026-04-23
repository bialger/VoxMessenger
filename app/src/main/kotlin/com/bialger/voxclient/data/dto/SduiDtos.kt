package com.bialger.voxclient.data.dto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class SduiScreenDto(
    @SerializedName("schema_version")
    val schemaVersion: Int,
    @SerializedName("screen_id")
    val screenId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: List<SduiComponentDto> = emptyList(),
    @SerializedName("meta")
    val meta: SduiMetaDto? = null,
)

data class SduiComponentDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("style")
    val style: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("action")
    val action: SduiActionDto? = null,
    @SerializedName("buttons")
    val buttons: List<SduiButtonDto>? = null,
)

data class SduiButtonDto(
    @SerializedName("style")
    val style: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("action")
    val action: SduiActionDto? = null,
)

data class SduiActionDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("event")
    val event: String? = null,
)

data class SduiMetaDto(
    @SerializedName("eula_version")
    val eulaVersion: String? = null,
    @SerializedName("min_client_version_code")
    val minClientVersionCode: Int? = null,
    @SerializedName("latest_client_version_code")
    val latestClientVersionCode: Int? = null,
    @SerializedName("update_policy")
    val updatePolicy: String? = null,
)

data class SduiEventRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("screen_id")
    val screenId: String,
    @SerializedName("event")
    val event: String,
    @SerializedName("meta")
    val meta: JsonObject? = null,
    @SerializedName("client_time")
    val clientTime: Long? = null,
)

data class SduiEventResponseDto(
    @SerializedName("ok")
    val ok: Boolean,
)
