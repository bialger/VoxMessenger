package com.bialger.voxclient.core.model

enum class SyncCollection(val wireName: String) {
    CONTACTS("contacts"),
    CONTACT_TRUST("contact_trust"),
    BLOCKLIST("blocklist"),
    PREFERENCES("preferences"),
    DRAFTS("drafts"),
    ;

    companion object {
        fun fromWireName(wireName: String): SyncCollection? =
            entries.firstOrNull { it.wireName == wireName }
    }
}

data class SyncWrapParams(
    val algorithm: String,
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
)

data class SyncKeyBundle(
    val syncKeyVersion: Int,
    val wrappedSyncKey: String,
    val syncWrapSalt: String,
    val syncWrapParams: SyncWrapParams,
)

data class EncryptedSyncRecord(
    val collection: SyncCollection,
    val recordId: String,
    val ciphertext: String,
    val contentHash: String,
    val version: Long,
    val serverUpdatedAt: Long,
    val deleted: Boolean,
)

data class SyncChangesPage(
    val collection: SyncCollection,
    val changes: List<EncryptedSyncRecord>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class SyncRecordMutation(
    val collection: SyncCollection,
    val recordId: String,
    val deviceId: DeviceId,
    val ciphertext: String,
    val contentHash: String,
    val baseVersion: Long?,
    val clientUpdatedAt: Long,
)

data class SyncRecordMutationResult(
    val recordId: String,
    val version: Long,
    val serverUpdatedAt: Long,
    val deleted: Boolean,
)

