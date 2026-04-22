package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ChatMessage
import com.bialger.voxclient.core.model.ConversationId
import com.bialger.voxclient.core.model.ConversationSummary
import com.bialger.voxclient.core.model.ConversationType
import com.bialger.voxclient.core.model.ContactNote
import com.bialger.voxclient.core.model.CryptoEngine
import com.bialger.voxclient.core.model.DeviceId
import com.bialger.voxclient.core.model.DiscoveredUser
import com.bialger.voxclient.core.model.EnvelopeId
import com.bialger.voxclient.core.model.RemoteDeviceSummary
import com.bialger.voxclient.core.model.RemotePreKeyBundle
import com.bialger.voxclient.core.model.SendState
import com.bialger.voxclient.core.model.SyncChangesPage
import com.bialger.voxclient.core.model.SyncCollection
import com.bialger.voxclient.core.model.SyncKeyBundle
import com.bialger.voxclient.core.model.SyncRecordMutation
import com.bialger.voxclient.core.model.SyncRecordMutationResult
import com.bialger.voxclient.core.model.SyncWrapParams
import com.bialger.voxclient.core.model.UserId
import com.bialger.voxclient.core.model.filterDirectory
import com.bialger.voxclient.core.model.preview
import com.bialger.voxclient.core.model.titleFor
import com.bialger.voxclient.core.model.totalUnread
import com.bialger.voxclient.domain.repository.DirectoryRepository
import com.bialger.voxclient.domain.repository.EncryptedSyncRepository
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StageOneBusinessLogicTest {
    @Test
    fun primitiveTypesExample_usesVoxValues() {
        val example = primitiveExamples()

        assertEquals("alice", example.username)
        assertEquals(4, example.unreadCount)
        assertEquals(1_712_500_000L, example.serverTimestamp)
        assertEquals(4_194_304L, example.attachmentSizeBytes)
        assertTrue(example.isOnline)
    }

    @Test
    fun arraysExample_buildsDeviceIdsAndPreKeyIds() {
        val deviceIds = sampleDeviceIds()
        val preKeyIds = samplePreKeyIds()

        assertEquals(listOf("alice-phone", "alice-tablet"), deviceIds.map { it.value })
        assertArrayEquals(intArrayOf(1, 2, 3, 4, 5), preKeyIds)
    }

    @Test
    fun nullableExample_keepsOptionalAliasAndNote() {
        val withAlias = ContactNote(UserId("usr_bob"), alias = "Bob", note = null)
        val withoutAlias = ContactNote(UserId("usr_alice"), alias = null, note = null)

        assertEquals("Bob", withAlias.alias)
        assertNull(withAlias.note)
        assertNull(withoutAlias.alias)
        assertNull(withoutAlias.note)
    }

    @Test
    fun whenExample_resolvesConversationTitles() {
        assertEquals("Direct Message", titleFor(ConversationType.DM))
        assertEquals("Group", titleFor(ConversationType.GROUP))
        assertEquals("Channel", titleFor(ConversationType.CHANNEL))
    }

    @Test
    fun dataClassExample_representsChatMessage() {
        val message = ChatMessage(
            envelopeId = EnvelopeId("env_1"),
            conversationId = ConversationId("conv_1"),
            authorUserId = UserId("usr_alice"),
            body = "Hello, Bob!",
            sentAt = 1_710_000_000L,
        )

        assertEquals("env_1", message.envelopeId.value)
        assertEquals("conv_1", message.conversationId.value)
        assertEquals("usr_alice", message.authorUserId.value)
        assertEquals("Hello, Bob!", message.body)
    }

    @Test
    fun sealedStateExample_coversSendStates() {
        val idle: SendState = SendState.Idle
        val encrypting: SendState = SendState.Encrypting
        val sending: SendState = SendState.Sending
        val failed: SendState = SendState.Failed("timeout")
        val sent: SendState = SendState.Sent

        assertTrue(idle is SendState.Idle)
        assertTrue(encrypting is SendState.Encrypting)
        assertTrue(sending is SendState.Sending)
        assertEquals("timeout", (failed as SendState.Failed).reason)
        assertTrue(sent is SendState.Sent)
    }

    @Test
    fun interfaceExample_usesCryptoEngineContract() {
        val cryptoEngine = object : CryptoEngine {
            override fun generateIdentityKeyPair(): ByteArray = byteArrayOf(1, 2, 3)

            override fun verifySignedPrekey(
                identityKeyPublic: ByteArray,
                signedPrekeyPublic: ByteArray,
                signature: ByteArray,
            ): Boolean = identityKeyPublic.isNotEmpty() && signedPrekeyPublic.isNotEmpty() && signature.isNotEmpty()
        }

        assertArrayEquals(byteArrayOf(1, 2, 3), cryptoEngine.generateIdentityKeyPair())
        assertTrue(
            cryptoEngine.verifySignedPrekey(
                identityKeyPublic = byteArrayOf(1),
                signedPrekeyPublic = byteArrayOf(2),
                signature = byteArrayOf(3),
            ),
        )
        assertFalse(
            cryptoEngine.verifySignedPrekey(
                identityKeyPublic = byteArrayOf(),
                signedPrekeyPublic = byteArrayOf(2),
                signature = byteArrayOf(3),
            ),
        )
    }

    @Test
    fun extensionExample_formatsPreview() {
        val message = ChatMessage(
            envelopeId = EnvelopeId("env_2"),
            conversationId = ConversationId("conv_2"),
            authorUserId = UserId("usr_bob"),
            body = "1234567890",
            sentAt = 1_710_000_100L,
        )

        assertEquals("1234567890", message.preview(max = 10))
        assertEquals("12345...", message.preview(max = 5))
    }

    @Test
    fun localFunctionExample_filtersDirectory() {
        val users = listOf(
            DiscoveredUser(userId = UserId("usr_alice"), username = "alice"),
            DiscoveredUser(userId = UserId("usr_bob"), username = "Bob"),
            DiscoveredUser(userId = UserId("usr_charlie"), username = "charlie"),
        )

        val result = filterDirectory(users, " bo ")

        assertEquals(listOf("Bob"), result.map { it.username })
    }

    @Test
    fun lambdaExample_aggregatesUnreadCounters() {
        val unread = totalUnread(
            listOf(
                ConversationSummary(id = ConversationId("dm_1"), unreadCount = 2),
                ConversationSummary(id = ConversationId("group_7"), unreadCount = 5),
            ),
        )

        assertEquals(7, unread)
    }

    @Test
    fun searchUsersUseCase_validatesAndClampsLimit() {
        val repository = FakeDirectoryRepository()
        val useCase = SearchUsersByUsernameUseCase(repository)

        val invalid = useCase("   ")
        val valid = useCase("  bob  ", limit = 100)

        val invalidFailure = invalid as VoxResult.Failure
        assertTrue(invalidFailure.error is VoxError.Validation)

        assertTrue(valid is VoxResult.Success)
        assertEquals("bob", repository.lastQuery)
        assertEquals(50, repository.lastLimit)
    }

    @Test
    fun syncUseCases_validateAndForwardArguments() {
        val repository = FakeEncryptedSyncRepository()
        val pullUseCase = PullSyncChangesUseCase(repository)
        val upsertUseCase = UpsertEncryptedSyncRecordUseCase(repository)

        val pullResult = pullUseCase(collection = SyncCollection.CONTACTS, cursor = "c1", limit = 999)
        assertTrue(pullResult is VoxResult.Success)
        assertEquals(200, repository.lastPullLimit)

        val invalidUpsert = upsertUseCase(
            collection = SyncCollection.CONTACTS,
            recordId = " ",
            deviceId = DeviceId("dev_phone"),
            ciphertext = "cipher",
            contentHash = "hash",
            baseVersion = 1,
            clientUpdatedAt = 1_710_001_000L,
        )
        val invalidFailure = invalidUpsert as VoxResult.Failure
        assertTrue(invalidFailure.error is VoxError.Validation)
    }

    private class FakeDirectoryRepository : DirectoryRepository {
        var lastQuery: String? = null
        var lastLimit: Int? = null

        override fun resolveByUsername(username: String): VoxResult<DiscoveredUser> =
            VoxResult.Success(DiscoveredUser(userId = UserId("usr_$username"), username = username))

        override fun searchUsers(query: String, limit: Int): VoxResult<List<DiscoveredUser>> {
            lastQuery = query
            lastLimit = limit
            return VoxResult.Success(
                listOf(DiscoveredUser(userId = UserId("usr_bob"), username = "bob")),
            )
        }

        override fun loadUserDevices(userId: UserId): VoxResult<List<RemoteDeviceSummary>> =
            VoxResult.Success(
                listOf(
                    RemoteDeviceSummary(
                        deviceId = DeviceId("dev_phone"),
                        deviceLabel = "Phone",
                        isRevoked = false,
                        hasPrekeys = true,
                    ),
                ),
            )

        override fun loadUserPreKeyBundles(userId: UserId): VoxResult<List<RemotePreKeyBundle>> =
            VoxResult.Success(
                listOf(
                    RemotePreKeyBundle(
                        userId = userId,
                        username = "bob",
                        deviceId = DeviceId("dev_phone"),
                        deviceLabel = "Phone",
                        identityKeyPublic = "id",
                        signedPrekeyPublic = "spk",
                        signedPrekeySignature = "sig",
                        oneTimePrekeyPublic = "opk",
                        oneTimePrekeyId = "opk_1",
                    ),
                ),
            )
    }

    private class FakeEncryptedSyncRepository : EncryptedSyncRepository {
        var lastPullLimit: Int? = null

        override fun getSyncKeyBundle(): VoxResult<SyncKeyBundle> =
            VoxResult.Success(
                SyncKeyBundle(
                    syncKeyVersion = 1,
                    wrappedSyncKey = "wrapped",
                    syncWrapSalt = "salt",
                    syncWrapParams = SyncWrapParams(
                        algorithm = "argon2id",
                        memoryKiB = 65_536,
                        iterations = 3,
                        parallelism = 1,
                    ),
                ),
            )

        override fun updateSyncKeyBundle(
            wrappedSyncKey: String,
            syncWrapSalt: String,
            syncWrapParams: SyncWrapParams,
        ): VoxResult<Int> = VoxResult.Success(2)

        override fun getChanges(
            collection: SyncCollection,
            cursor: String?,
            limit: Int,
        ): VoxResult<SyncChangesPage> {
            lastPullLimit = limit
            return VoxResult.Success(
                SyncChangesPage(
                    collection = collection,
                    changes = emptyList(),
                    nextCursor = cursor,
                    hasMore = false,
                ),
            )
        }

        override fun putRecord(mutation: SyncRecordMutation): VoxResult<SyncRecordMutationResult> =
            VoxResult.Success(
                SyncRecordMutationResult(
                    recordId = mutation.recordId,
                    version = (mutation.baseVersion ?: 0) + 1,
                    serverUpdatedAt = mutation.clientUpdatedAt + 1,
                    deleted = false,
                ),
            )

        override fun deleteRecord(
            collection: SyncCollection,
            recordId: String,
            deviceId: DeviceId,
            baseVersion: Long,
            clientUpdatedAt: Long,
        ): VoxResult<SyncRecordMutationResult> =
            VoxResult.Success(
                SyncRecordMutationResult(
                    recordId = recordId,
                    version = baseVersion + 1,
                    serverUpdatedAt = clientUpdatedAt + 1,
                    deleted = true,
                ),
            )
    }
}

