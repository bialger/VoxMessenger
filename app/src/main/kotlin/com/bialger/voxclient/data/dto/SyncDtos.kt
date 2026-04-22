package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class SyncWrapParamsDto(
    @SerializedName("algorithm")
    val algorithm: String,
    @SerializedName("memory_kib")
    val memoryKiB: Int,
    @SerializedName("iterations")
    val iterations: Int,
    @SerializedName("parallelism")
    val parallelism: Int,
)

data class SyncKeyBundleDto(
    @SerializedName("sync_key_version")
    val syncKeyVersion: Int,
    @SerializedName("wrapped_sync_key")
    val wrappedSyncKey: String,
    @SerializedName("sync_wrap_salt")
    val syncWrapSalt: String,
    @SerializedName("sync_wrap_params")
    val syncWrapParams: SyncWrapParamsDto,
)

data class UpdateSyncKeyBundleRequestDto(
    @SerializedName("wrapped_sync_key")
    val wrappedSyncKey: String,
    @SerializedName("sync_wrap_salt")
    val syncWrapSalt: String,
    @SerializedName("sync_wrap_params")
    val syncWrapParams: SyncWrapParamsDto,
)

data class UpdateSyncKeyBundleResponseDto(
    @SerializedName("sync_key_version")
    val syncKeyVersion: Int,
)

data class SyncChangesResponseDto(
    @SerializedName("collection")
    val collection: String,
    @SerializedName("changes")
    val changes: List<SyncRecordDto>,
    @SerializedName("next_cursor")
    val nextCursor: String?,
    @SerializedName("has_more")
    val hasMore: Boolean,
)

data class SyncRecordDto(
    @SerializedName("record_id")
    val recordId: String,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("content_hash")
    val contentHash: String,
    @SerializedName("version")
    val version: Long,
    @SerializedName("server_updated_at")
    val serverUpdatedAt: Long,
    @SerializedName("deleted")
    val deleted: Boolean,
)

data class PutSyncRecordRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("content_hash")
    val contentHash: String,
    @SerializedName("base_version")
    val baseVersion: Long?,
    @SerializedName("client_updated_at")
    val clientUpdatedAt: Long,
)

data class DeleteSyncRecordRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("base_version")
    val baseVersion: Long,
    @SerializedName("client_updated_at")
    val clientUpdatedAt: Long,
)

data class SyncRecordMutationResponseDto(
    @SerializedName("record_id")
    val recordId: String,
    @SerializedName("version")
    val version: Long,
    @SerializedName("server_updated_at")
    val serverUpdatedAt: Long,
    @SerializedName("deleted")
    val deleted: Boolean = false,
)
