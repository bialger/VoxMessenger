package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.DeviceId
import com.bialger.voxclient.core.model.SyncChangesPage
import com.bialger.voxclient.core.model.SyncCollection
import com.bialger.voxclient.core.model.SyncRecordMutation
import com.bialger.voxclient.core.model.SyncRecordMutationResult
import com.bialger.voxclient.domain.repository.EncryptedSyncRepository

class PullSyncChangesUseCase(
    private val encryptedSyncRepository: EncryptedSyncRepository,
) {
    operator fun invoke(
        collection: SyncCollection,
        cursor: String? = null,
        limit: Int = DEFAULT_LIMIT,
    ): VoxResult<SyncChangesPage> {
        val normalizedLimit = limit.coerceIn(1, MAX_LIMIT)
        return encryptedSyncRepository.getChanges(
            collection = collection,
            cursor = cursor,
            limit = normalizedLimit,
        )
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 200
    }
}

class UpsertEncryptedSyncRecordUseCase(
    private val encryptedSyncRepository: EncryptedSyncRepository,
) {
    operator fun invoke(
        collection: SyncCollection,
        recordId: String,
        deviceId: DeviceId,
        ciphertext: String,
        contentHash: String,
        baseVersion: Long?,
        clientUpdatedAt: Long,
    ): VoxResult<SyncRecordMutationResult> {
        if (recordId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Record id must not be blank."))
        }
        if (ciphertext.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Ciphertext must not be blank."))
        }
        if (contentHash.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Content hash must not be blank."))
        }

        val mutation = SyncRecordMutation(
            collection = collection,
            recordId = recordId,
            deviceId = deviceId,
            ciphertext = ciphertext,
            contentHash = contentHash,
            baseVersion = baseVersion,
            clientUpdatedAt = clientUpdatedAt,
        )
        return encryptedSyncRepository.putRecord(mutation)
    }
}

class DeleteEncryptedSyncRecordUseCase(
    private val encryptedSyncRepository: EncryptedSyncRepository,
) {
    operator fun invoke(
        collection: SyncCollection,
        recordId: String,
        deviceId: DeviceId,
        baseVersion: Long,
        clientUpdatedAt: Long,
    ): VoxResult<SyncRecordMutationResult> {
        if (recordId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Record id must not be blank."))
        }
        if (baseVersion < 0) {
            return VoxResult.Failure(VoxError.Validation("Base version must be non-negative."))
        }

        return encryptedSyncRepository.deleteRecord(
            collection = collection,
            recordId = recordId,
            deviceId = deviceId,
            baseVersion = baseVersion,
            clientUpdatedAt = clientUpdatedAt,
        )
    }
}

