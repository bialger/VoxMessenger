package com.bialger.voxclient.domain.repository

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.AttachmentId
import com.bialger.voxclient.core.model.AuthSession
import com.bialger.voxclient.core.model.ChatMessage
import com.bialger.voxclient.core.model.ConversationId
import com.bialger.voxclient.core.model.ConversationSummary
import com.bialger.voxclient.core.model.DeviceId
import com.bialger.voxclient.core.model.DiscoveredUser
import com.bialger.voxclient.core.model.EncryptedSyncRecord
import com.bialger.voxclient.core.model.EnvelopeId
import com.bialger.voxclient.core.model.KnownContact
import com.bialger.voxclient.core.model.RegisterCommand
import com.bialger.voxclient.core.model.RemoteDeviceSummary
import com.bialger.voxclient.core.model.RemotePreKeyBundle
import com.bialger.voxclient.core.model.SyncChangesPage
import com.bialger.voxclient.core.model.SyncCollection
import com.bialger.voxclient.core.model.SyncKeyBundle
import com.bialger.voxclient.core.model.SyncRecordMutation
import com.bialger.voxclient.core.model.SyncRecordMutationResult
import com.bialger.voxclient.core.model.SyncWrapParams
import com.bialger.voxclient.core.model.UserId

interface AuthRepository {
    fun register(command: RegisterCommand): VoxResult<AuthSession>

    fun login(
        serverBaseUrl: String,
        username: String,
        password: String,
        deviceId: DeviceId,
    ): VoxResult<AuthSession>

    fun refresh(refreshToken: String): VoxResult<AuthSession>

    fun logout(deviceId: DeviceId): VoxResult<Unit>
}

interface DirectoryRepository {
    fun resolveByUsername(username: String): VoxResult<DiscoveredUser>

    fun searchUsers(query: String, limit: Int): VoxResult<List<DiscoveredUser>>

    fun loadUserDevices(userId: UserId): VoxResult<List<RemoteDeviceSummary>>

    fun loadUserPreKeyBundles(userId: UserId): VoxResult<List<RemotePreKeyBundle>>
}

interface DeviceRepository {
    fun loadMyDevices(): VoxResult<List<RemoteDeviceSummary>>

    fun revokeDevice(deviceId: DeviceId): VoxResult<Unit>

    fun publishOneTimePreKeys(deviceId: DeviceId, preKeys: Map<String, String>): VoxResult<Unit>

    fun rotateSignedPreKey(
        deviceId: DeviceId,
        signedPreKeyPublic: String,
        signedPreKeySignature: String,
    ): VoxResult<Unit>
}

interface ConversationRepository {
    fun loadConversations(): VoxResult<List<ConversationSummary>>

    fun loadConversation(conversationId: ConversationId): VoxResult<ConversationSummary>

    fun loadConversationHistory(
        conversationId: ConversationId,
        cursor: String?,
        limit: Int,
    ): VoxResult<List<ChatMessage>>
}

interface MessageRepository {
    fun sendMessage(
        conversationId: ConversationId,
        senderDeviceId: DeviceId,
        envelopeCiphertext: String,
    ): VoxResult<EnvelopeId>

    fun ackEnvelope(envelopeId: EnvelopeId): VoxResult<Unit>

    fun loadPendingEnvelopes(limit: Int, cursor: String?): VoxResult<List<EnvelopeId>>
}

interface AttachmentRepository {
    fun initUpload(ciphertextSizeBytes: Long, contentHash: String): VoxResult<AttachmentId>

    fun uploadChunk(attachmentId: AttachmentId, offsetBytes: Long, ciphertextChunk: ByteArray): VoxResult<Unit>

    fun finalizeUpload(attachmentId: AttachmentId, contentHash: String): VoxResult<Unit>

    fun resolveDownloadUrl(attachmentId: AttachmentId): VoxResult<String>
}

interface ContactsRepository {
    fun loadKnownContacts(): VoxResult<List<KnownContact>>

    fun saveAlias(userId: UserId, alias: String?): VoxResult<KnownContact>

    fun saveNote(userId: UserId, note: String?): VoxResult<KnownContact>

    fun pinFingerprint(userId: UserId, fingerprint: String): VoxResult<KnownContact>

    fun setBlocked(userId: UserId, blocked: Boolean): VoxResult<KnownContact>
}

interface EncryptedSyncRepository {
    fun getSyncKeyBundle(): VoxResult<SyncKeyBundle>

    fun updateSyncKeyBundle(
        wrappedSyncKey: String,
        syncWrapSalt: String,
        syncWrapParams: SyncWrapParams,
    ): VoxResult<Int>

    fun getChanges(
        collection: SyncCollection,
        cursor: String?,
        limit: Int,
    ): VoxResult<SyncChangesPage>

    fun putRecord(mutation: SyncRecordMutation): VoxResult<SyncRecordMutationResult>

    fun deleteRecord(
        collection: SyncCollection,
        recordId: String,
        deviceId: DeviceId,
        baseVersion: Long,
        clientUpdatedAt: Long,
    ): VoxResult<SyncRecordMutationResult>
}

interface CryptoRepository {
    fun generateIdentityKeyPair(): VoxResult<ByteArray>

    fun verifySignedPreKey(
        identityKeyPublic: ByteArray,
        signedPreKeyPublic: ByteArray,
        signature: ByteArray,
    ): VoxResult<Boolean>

    fun encryptSyncPayload(plaintext: ByteArray): VoxResult<EncryptedSyncRecord>

    fun decryptSyncPayload(record: EncryptedSyncRecord): VoxResult<ByteArray>
}

interface SettingsRepository {
    fun selectedServerUrl(): VoxResult<String?>

    fun saveSelectedServerUrl(serverBaseUrl: String): VoxResult<Unit>

    fun isOnboardingCompleted(): VoxResult<Boolean>

    fun setOnboardingCompleted(completed: Boolean): VoxResult<Unit>
}

