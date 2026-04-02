package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.AckEnvelopeRequestDto
import com.bialger.voxclient.data.dto.AddConversationMemberRequestDto
import com.bialger.voxclient.data.dto.ApiErrorEnvelopeDto
import com.bialger.voxclient.data.dto.ChangePasswordRequestDto
import com.bialger.voxclient.data.dto.CreateConversationRequestDto
import com.bialger.voxclient.data.dto.DeleteSyncRecordRequestDto
import com.bialger.voxclient.data.dto.FinalizeAttachmentRequestDto
import com.bialger.voxclient.data.dto.LoginRequestDto
import com.bialger.voxclient.data.dto.OneTimePreKeyDto
import com.bialger.voxclient.data.dto.PublishPreKeysRequestDto
import com.bialger.voxclient.data.dto.PutSyncRecordRequestDto
import com.bialger.voxclient.data.dto.RefreshRequestDto
import com.bialger.voxclient.data.dto.RegisterRequestDto
import com.bialger.voxclient.data.dto.RotateSignedPreKeyRequestDto
import com.bialger.voxclient.data.dto.SendMessageRequestDto
import com.bialger.voxclient.data.dto.SyncWrapParamsDto
import com.bialger.voxclient.data.dto.UpdateSyncKeyBundleRequestDto
import com.bialger.voxclient.data.dto.UploadAttachmentInitRequestDto
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class VoxApiServicesMockNetworkTest {
    private val gson = Gson()
    private lateinit var server: MockWebServer

    private lateinit var publicApi: VoxPublicApi
    private lateinit var accountApi: VoxAccountApi
    private lateinit var directoryApi: VoxDirectoryApi
    private lateinit var deviceKeysApi: VoxDeviceKeysApi
    private lateinit var syncApi: VoxSyncApi
    private lateinit var conversationApi: VoxConversationApi
    private lateinit var messagingApi: VoxMessagingApi
    private lateinit var attachmentsApi: VoxAttachmentsApi
    private lateinit var adminApi: VoxAdminApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val factory = VoxPublicApiFactory()
        val baseUrl = server.url("/").toString()

        publicApi = factory.create(baseUrl)
        accountApi = factory.createAccountApi(baseUrl)
        directoryApi = factory.createDirectoryApi(baseUrl)
        deviceKeysApi = factory.createDeviceKeysApi(baseUrl)
        syncApi = factory.createSyncApi(baseUrl)
        conversationApi = factory.createConversationApi(baseUrl)
        messagingApi = factory.createMessagingApi(baseUrl)
        attachmentsApi = factory.createAttachmentsApi(baseUrl)
        adminApi = factory.createAdminApi(baseUrl)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun healthEndpoint_isCovered() {
        enqueueJson("""{"status":"ok"}""")

        val response = publicApi.getHealth().execute()
        assertTrue(response.isSuccessful)
        assertEquals("ok", response.body()?.status)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/health", request.path)
    }

    @Test
    fun registerEndpoint_isCovered() {
        enqueueJson(
            """
            {
              "user_id":"usr_1",
              "access_token":"acc_1",
              "refresh_token":"ref_1",
              "device_status":"created",
              "sync_key_version":1
            }
            """.trimIndent(),
        )

        val response =
            publicApi.register(
                RegisterRequestDto(
                    username = "alice",
                    passwordDerivedValue = "pwd",
                    deviceId = "dev_phone",
                    deviceLabel = "Pixel 9",
                    identityKeyPublic = "id_pub",
                    signedPrekeyPublic = "spk_pub",
                    signedPrekeySignature = "spk_sig",
                    wrappedSyncKey = "wrapped",
                    syncWrapSalt = "salt",
                    syncWrapParams = SyncWrapParamsDto("argon2id", 65536, 3, 1),
                ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals("usr_1", response.body()?.userId)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/register", request.path)
        val json = requestJson(request)
        assertEquals("alice", json["username"].asString)
        assertEquals("dev_phone", json["device_id"].asString)
        assertTrue(json.has("sync_wrap_params"))
    }

    @Test
    fun loginEndpoint_isCovered() {
        enqueueJson(
            """
            {
              "user_id":"usr_1",
              "access_token":"acc_2",
              "refresh_token":"ref_2",
              "device_status":"existing",
              "sync_key_version":2
            }
            """.trimIndent(),
        )

        val response =
            publicApi.login(
                LoginRequestDto(
                    username = "alice",
                    passwordDerivedValue = "pwd",
                    deviceId = "dev_phone",
                ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals("existing", response.body()?.deviceStatus)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/login", request.path)
        val json = requestJson(request)
        assertEquals("alice", json["username"].asString)
    }

    @Test
    fun refreshEndpoint_isCovered() {
        enqueueJson("""{"access_token":"acc_new","refresh_token":"ref_new"}""")

        val response =
            publicApi.refresh(
                RefreshRequestDto(refreshToken = "ref_old", deviceId = "dev_phone"),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals("acc_new", response.body()?.accessToken)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/refresh", request.path)
        val json = requestJson(request)
        assertEquals("ref_old", json["refresh_token"].asString)
        assertEquals("dev_phone", json["device_id"].asString)
    }

    @Test
    fun meEndpoint_isCovered() {
        enqueueJson("""{"user_id":"usr_1","username":"alice","current_device_id":"dev_phone","sync_key_version":1}""")

        val response = accountApi.me(BEARER).execute()
        assertTrue(response.isSuccessful)
        assertEquals("alice", response.body()?.username)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/me", request.path)
        assertEquals(BEARER, request.getHeader("Authorization"))
    }

    @Test
    fun logoutEndpoint_isCovered() {
        enqueueJson("{}")

        val response = accountApi.logout(BEARER).execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/logout", request.path)
        assertEquals(BEARER, request.getHeader("Authorization"))
        assertEquals("{}", request.body.readUtf8())
    }

    @Test
    fun changePasswordEndpoint_isCovered() {
        enqueueJson("""{"sync_key_version":3}""")

        val response =
            accountApi.changePassword(
                authorization = BEARER,
                request =
                    ChangePasswordRequestDto(
                        currentPasswordDerivedValue = "old",
                        newPasswordDerivedValue = "new",
                        wrappedSyncKey = "wrapped",
                        syncWrapSalt = "salt",
                        syncWrapParams = SyncWrapParamsDto("argon2id", 65536, 3, 1),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals(3, response.body()?.syncKeyVersion)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/account/change-password", request.path)
        assertEquals(BEARER, request.getHeader("Authorization"))
    }

    @Test
    fun myDevicesEndpoint_isCovered() {
        enqueueJson(
            """
            {"devices":[{"device_id":"dev_phone","device_label":"Pixel 9","created_at":1,"last_seen_at":2,"is_current":true,"is_revoked":false}]}
            """.trimIndent(),
        )

        val response = accountApi.loadMyDevices(BEARER).execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.devices?.size)
        assertTrue(response.body()?.devices?.first()?.isCurrent == true)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/me/devices", request.path)
    }

    @Test
    fun revokeDeviceEndpoint_isCovered() {
        enqueueJson("{}")

        val response = accountApi.revokeDevice(BEARER, "dev_old").execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/me/devices/dev_old", request.path)
    }

    @Test
    fun resolveByUsernameEndpoint_isCovered() {
        enqueueJson("""{"user_id":"usr_bob","username":"bob"}""")

        val response = directoryApi.resolveByUsername(BEARER, "bob").execute()
        assertTrue(response.isSuccessful)
        assertEquals("usr_bob", response.body()?.userId)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/by-username/bob", request.path)
    }

    @Test
    fun searchUsersEndpoint_isCovered() {
        enqueueJson("""{"users":[{"user_id":"usr_bob","username":"bob"}]}""")

        val response = directoryApi.searchUsers(BEARER, "bo", 20).execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.users?.size)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/search?q=bo&limit=20", request.path)
    }

    @Test
    fun getUserEndpoint_isCovered() {
        enqueueJson("""{"user_id":"usr_bob","username":"bob"}""")

        val response = directoryApi.getUser(BEARER, "usr_bob").execute()
        assertTrue(response.isSuccessful)
        assertEquals("bob", response.body()?.username)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/usr_bob", request.path)
    }

    @Test
    fun getUserDevicesEndpoint_isCovered() {
        enqueueJson("""{"devices":[{"device_id":"dev_phone","device_label":"Bob phone","is_revoked":false,"has_prekeys":true}]}""")

        val response = directoryApi.getUserDevices(BEARER, "usr_bob").execute()
        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.devices?.first()?.hasPrekeys == true)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/usr_bob/devices", request.path)
    }

    @Test
    fun getUserPreKeyBundlesEndpoint_isCovered() {
        enqueueJson(
            """
            {
              "user_id":"usr_bob",
              "username":"bob",
              "bundles":[
                {
                  "device_id":"dev_phone",
                  "device_label":"Bob phone",
                  "identity_key_public":"id_pub",
                  "signed_prekey_public":"spk_pub",
                  "signed_prekey_signature":"spk_sig",
                  "one_time_prekey_public":"opk_pub",
                  "one_time_prekey_id":"opk_1"
                }
              ]
            }
            """.trimIndent(),
        )

        val response = directoryApi.getUserPreKeyBundles(BEARER, "usr_bob").execute()
        assertTrue(response.isSuccessful)
        assertEquals("usr_bob", response.body()?.userId)
        assertEquals(1, response.body()?.bundles?.size)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/usr_bob/prekey-bundles", request.path)
    }

    @Test
    fun publishPreKeysEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            deviceKeysApi.publishOneTimePreKeys(
                authorization = BEARER,
                deviceId = "dev_phone",
                request =
                    PublishPreKeysRequestDto(
                        prekeys = listOf(OneTimePreKeyDto("opk_1", "pub_1")),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/devices/dev_phone/prekeys", request.path)
        val json = requestJson(request)
        assertEquals(1, json["prekeys"].asJsonArray.size())
    }

    @Test
    fun publishPreKeysForUserDeviceEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            deviceKeysApi.publishOneTimePreKeysForUserDevice(
                authorization = BEARER,
                userId = "usr_alice",
                deviceId = "dev_phone",
                request =
                    PublishPreKeysRequestDto(
                        prekeys = listOf(OneTimePreKeyDto("opk_1", "pub_1")),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/users/usr_alice/devices/dev_phone/prekeys", request.path)
    }

    @Test
    fun rotateSignedPreKeyEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            deviceKeysApi.rotateSignedPreKey(
                authorization = BEARER,
                deviceId = "dev_phone",
                request = RotateSignedPreKeyRequestDto("spk_pub", "spk_sig"),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/devices/dev_phone/signed-prekey", request.path)
    }

    @Test
    fun rotateSignedPreKeyForUserDeviceEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            deviceKeysApi.rotateSignedPreKeyForUserDevice(
                authorization = BEARER,
                userId = "usr_alice",
                deviceId = "dev_phone",
                request = RotateSignedPreKeyRequestDto("spk_pub", "spk_sig"),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/users/usr_alice/devices/dev_phone/signed-prekey", request.path)
    }

    @Test
    fun getDevicePreKeyBundleEndpoint_isCovered() {
        enqueueJson(
            """
            {"user_id":"usr_bob","device_id":"dev_phone","identity_key_public":"id","signed_prekey_public":"spk","signed_prekey_signature":"sig","one_time_prekey_public":"opk","one_time_prekey_id":"opk_1"}
            """.trimIndent(),
        )

        val response = deviceKeysApi.getDevicePreKeyBundle(BEARER, "dev_phone").execute()
        assertTrue(response.isSuccessful)
        assertEquals("usr_bob", response.body()?.userId)
        assertEquals("dev_phone", response.body()?.deviceId)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/devices/dev_phone/prekey-bundle", request.path)
    }

    @Test
    fun getUserDevicePreKeyBundleEndpoint_isCovered() {
        enqueueJson(
            """
            {"user_id":"usr_bob","device_id":"dev_phone","identity_key_public":"id","signed_prekey_public":"spk","signed_prekey_signature":"sig","one_time_prekey_public":"opk","one_time_prekey_id":"opk_1"}
            """.trimIndent(),
        )

        val response = deviceKeysApi.getUserDevicePreKeyBundle(BEARER, "usr_bob", "dev_phone").execute()
        assertTrue(response.isSuccessful)
        assertEquals("usr_bob", response.body()?.userId)
        assertEquals("dev_phone", response.body()?.deviceId)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/users/usr_bob/devices/dev_phone/prekey-bundle", request.path)
    }

    @Test
    fun getSyncKeyBundleEndpoint_isCovered() {
        enqueueJson(
            """
            {"sync_key_version":1,"wrapped_sync_key":"wrapped","sync_wrap_salt":"salt","sync_wrap_params":{"algorithm":"argon2id","memory_kib":65536,"iterations":3,"parallelism":1}}
            """.trimIndent(),
        )

        val response = syncApi.getSyncKeyBundle(BEARER).execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.syncKeyVersion)
        assertEquals("argon2id", response.body()?.syncWrapParams?.algorithm)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/sync/key-bundle", request.path)
    }

    @Test
    fun updateSyncKeyBundleEndpoint_isCovered() {
        enqueueJson("""{"sync_key_version":2}""")

        val response =
            syncApi.updateSyncKeyBundle(
                authorization = BEARER,
                request =
                    UpdateSyncKeyBundleRequestDto(
                        wrappedSyncKey = "wrapped",
                        syncWrapSalt = "salt",
                        syncWrapParams = SyncWrapParamsDto("argon2id", 65536, 3, 1),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()?.syncKeyVersion)

        val request = takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/sync/key-bundle", request.path)
    }

    @Test
    fun getSyncChangesEndpoint_isCovered() {
        enqueueJson(
            """
            {"collection":"contacts","changes":[{"record_id":"r1","ciphertext":"c","content_hash":"h","version":7,"server_updated_at":10,"deleted":false}],"next_cursor":"c1","has_more":true}
            """.trimIndent(),
        )

        val response = syncApi.getSyncChanges(BEARER, "contacts", "cursor-1", 100).execute()
        assertTrue(response.isSuccessful)
        assertEquals("contacts", response.body()?.collection)
        assertTrue(response.body()?.hasMore == true)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/sync/changes?collection=contacts&cursor=cursor-1&limit=100", request.path)
    }

    @Test
    fun putSyncRecordEndpoint_isCovered() {
        enqueueJson("""{"record_id":"r1","version":8,"server_updated_at":11,"deleted":false}""")

        val response =
            syncApi.putSyncRecord(
                authorization = BEARER,
                collection = "contacts",
                recordId = "r1",
                request =
                    PutSyncRecordRequestDto(
                        deviceId = "dev_phone",
                        ciphertext = "cipher",
                        contentHash = "hash",
                        baseVersion = 7,
                        clientUpdatedAt = 100,
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals(8L, response.body()?.version)

        val request = takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/sync/records/contacts/r1", request.path)
    }

    @Test
    fun deleteSyncRecordEndpoint_isCovered() {
        enqueueJson("""{"record_id":"r1","version":9,"server_updated_at":12,"deleted":true}""")

        val response =
            syncApi.deleteSyncRecord(
                authorization = BEARER,
                collection = "contacts",
                recordId = "r1",
                request =
                    DeleteSyncRecordRequestDto(
                        deviceId = "dev_phone",
                        baseVersion = 8,
                        clientUpdatedAt = 101,
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.deleted == true)

        val request = takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/sync/records/contacts/r1", request.path)
    }

    @Test
    fun syncPendingEndpoint_isCovered() {
        enqueueJson(
            """
            {"envelopes":[{"envelope_id":"env_1","conversation_id":"conv_1","sender_user_id":"usr_remote","sender_device_id":"dev_remote","ciphertext":"cipher","server_timestamp":100,"envelope_type":0,"ordering_epoch":1}],"next_cursor":"n1","has_more":false}
            """.trimIndent(),
        )

        val response = syncApi.loadPendingEnvelopes(BEARER, 20, "cur").execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.envelopes?.size)
        assertEquals(1L, response.body()?.envelopes?.first()?.orderingEpoch)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/sync/pending?limit=20&cursor=cur", request.path)
    }

    @Test
    fun listConversationsEndpoint_isCovered() {
        enqueueJson(
            """{"conversations":[{"conversation_id":"conv_1","type":0,"created_by":"usr_1","created_by_username":"alice","peer_user_id":"usr_2","peer_username":"bob","created_at":1,"membership_version":3}]}""",
        )

        val response = conversationApi.loadConversations(BEARER).execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.conversations?.size)
        assertEquals("usr_2", response.body()?.conversations?.first()?.peerUserId)
        assertEquals("bob", response.body()?.conversations?.first()?.peerUsername)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/conversations", request.path)
    }

    @Test
    fun getConversationEndpoint_isCovered() {
        enqueueJson(
            """
            {"conversation_id":"conv_channel","type":2,"created_by":"usr_1","created_at":1,"membership_version":8,"title":"Announcements","my_role":"admin","channel_post_policy":"admins_only"}
            """.trimIndent(),
        )

        val response = conversationApi.getConversation(BEARER, "conv_channel").execute()
        assertTrue(response.isSuccessful)
        assertEquals("Announcements", response.body()?.title)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/conversations/conv_channel", request.path)
    }

    @Test
    fun getConversationDmShape_isCovered() {
        enqueueJson(
            """
            {"conversation_id":"conv_dm","type":0,"created_by":"usr_1","peer_user_id":"usr_2","peer_username":"bob","created_at":1,"membership_version":2,"my_role":"member"}
            """.trimIndent(),
        )

        val response = conversationApi.getConversation(BEARER, "conv_dm").execute()
        assertTrue(response.isSuccessful)
        assertEquals(0, response.body()?.type)
        assertEquals("usr_2", response.body()?.peerUserId)
        assertEquals("bob", response.body()?.peerUsername)
        assertEquals(null, response.body()?.title)
        assertEquals(null, response.body()?.channelPostPolicy)
    }

    @Test
    fun getConversationMembersEndpoint_isCovered() {
        enqueueJson(
            """
            {"conversation_id":"conv_channel","membership_version":8,"admins":[{"user_id":"usr_1","role":"owner"}],"subscribers":[{"user_id":"usr_2","role":"member"}]}
            """.trimIndent(),
        )

        val response = conversationApi.getConversationMembers(BEARER, "conv_channel").execute()
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.admins?.size)
        assertEquals(1, response.body()?.subscribers?.size)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/conversations/conv_channel/members", request.path)
    }

    @Test
    fun getConversationMembersSubscriberShape_isCovered() {
        enqueueJson(
            """
            {"conversation_id":"conv_channel","membership_version":8,"admins":[{"user_id":"usr_1","role":"owner"}],"subscription_state":"subscribed","member_count":523}
            """.trimIndent(),
        )

        val response = conversationApi.getConversationMembers(BEARER, "conv_channel").execute()
        assertTrue(response.isSuccessful)
        assertEquals("subscribed", response.body()?.subscriptionState)
        assertEquals(523, response.body()?.memberCount)
    }

    @Test
    fun createConversationEndpoint_isCovered() {
        enqueueJson("""{"conversation_id":"conv_new"}""")

        val response =
            conversationApi.createConversation(
                authorization = BEARER,
                request =
                    CreateConversationRequestDto(
                        type = "channel",
                        admins = listOf("usr_1"),
                        subscribers = listOf("usr_2", "usr_3"),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals("conv_new", response.body()?.conversationId)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations", request.path)
        val json = requestJson(request)
        assertEquals("channel", json["type"].asString)
        assertEquals(2, json["subscribers"].asJsonArray.size())
    }

    @Test
    fun createConversationDmPayload_isCovered() {
        enqueueJson("""{"conversation_id":"conv_dm"}""")

        val response =
            conversationApi.createConversation(
                authorization = BEARER,
                request =
                    CreateConversationRequestDto(
                        type = "dm",
                        peerUserId = "usr_peer",
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations", request.path)
        val json = requestJson(request)
        assertEquals("dm", json["type"].asString)
        assertEquals("usr_peer", json["peer_user_id"].asString)
    }

    @Test
    fun createConversationGroupPayload_isCovered() {
        enqueueJson("""{"conversation_id":"conv_group"}""")

        val response =
            conversationApi.createConversation(
                authorization = BEARER,
                request =
                    CreateConversationRequestDto(
                        type = "group",
                        members = listOf("usr_2", "usr_3"),
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations", request.path)
        val json = requestJson(request)
        assertEquals("group", json["type"].asString)
        assertEquals(2, json["members"].asJsonArray.size())
    }

    @Test
    fun addConversationMemberEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            conversationApi.addConversationMember(
                authorization = BEARER,
                conversationId = "conv_1",
                request = AddConversationMemberRequestDto("usr_2", "member"),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations/conv_1/members", request.path)
    }

    @Test
    fun removeConversationMemberEndpoint_isCovered() {
        enqueueJson("{}")

        val response = conversationApi.removeConversationMember(BEARER, "conv_1", "usr_2").execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/conversations/conv_1/members/usr_2", request.path)
    }

    @Test
    fun subscribeEndpoint_isCovered() {
        enqueueJson("{}")

        val response = conversationApi.subscribe(BEARER, "conv_channel").execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations/conv_channel/subscribe", request.path)
    }

    @Test
    fun unsubscribeEndpoint_isCovered() {
        enqueueJson("{}")

        val response = conversationApi.unsubscribe(BEARER, "conv_channel").execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/conversations/conv_channel/unsubscribe", request.path)
    }

    @Test
    fun conversationHistoryWithCursorEndpoint_isCovered() {
        enqueueJson("""{"envelopes":[{"envelope_id":"env_1","conversation_id":"conv_1","sender_user_id":"usr_remote","sender_device_id":"dev_remote","ciphertext":"cipher","server_timestamp":100,"envelope_type":0}],"next_cursor":"h1","has_more":true}""")

        val response = conversationApi.loadConversationHistory(BEARER, "conv_1", 50, "c1", null).execute()
        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.hasMore == true)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/conversations/conv_1/envelopes?limit=50&cursor=c1", request.path)
    }

    @Test
    fun conversationHistoryWithSinceEndpoint_isCovered() {
        enqueueJson("""{"envelopes":[],"next_cursor":"","has_more":false}""")

        val response = conversationApi.loadConversationHistory(BEARER, "conv_1", 50, null, 1710000000).execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/conversations/conv_1/envelopes?limit=50&since=1710000000", request.path)
    }

    @Test
    fun sendMessageEndpoint_isCovered() {
        enqueueJson("""{"envelope_id":"env_1","server_timestamp":200,"delivered_to_count":2}""")

        val response =
            messagingApi.sendMessage(
                authorization = BEARER,
                request =
                    SendMessageRequestDto(
                        deviceId = "dev_phone",
                        conversationId = "conv_1",
                        ciphertext = "cipher",
                        envelopeId = "env_1",
                        envelopeType = 0,
                        orderingEpoch = 7,
                    ),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()?.deliveredToCount)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/messages/send", request.path)
    }

    @Test
    fun ackMessageEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            messagingApi.ackMessage(
                authorization = BEARER,
                request = AckEnvelopeRequestDto(deviceId = "dev_phone", envelopeId = "env_1"),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/messages/ack", request.path)
    }

    @Test
    fun uploadInitEndpoint_isCovered() {
        enqueueJson("""{"attachment_id":"att_1","blob_path":"/blob/att_1"}""")

        val response =
            attachmentsApi.initUpload(
                authorization = BEARER,
                request = UploadAttachmentInitRequestDto("conv_1", 123456, "image/jpeg"),
            ).execute()

        assertTrue(response.isSuccessful)
        assertEquals("att_1", response.body()?.attachmentId)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/attachments/upload-init", request.path)
    }

    @Test
    fun uploadChunkEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            attachmentsApi.uploadChunk(
                authorization = BEARER,
                attachmentId = "att_1",
                offset = 512,
                chunk = "cipher_chunk".toByteArray().toRequestBody("application/octet-stream".toMediaType()),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/attachments/att_1/chunk?offset=512", request.path)
        assertEquals("cipher_chunk", request.body.readUtf8())
    }

    @Test
    fun finalizeUploadEndpoint_isCovered() {
        enqueueJson("{}")

        val response =
            attachmentsApi.finalizeUpload(
                authorization = BEARER,
                attachmentId = "att_1",
                request = FinalizeAttachmentRequestDto("sha256:abc"),
            ).execute()

        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/attachments/att_1/finalize", request.path)
    }

    @Test
    fun downloadAttachmentEndpoint_isCovered() {
        val binary = byteArrayOf(1, 2, 3, 4)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(Buffer().write(binary)),
        )

        val response = attachmentsApi.downloadAttachment(BEARER, "att_1").execute()
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertArrayEquals(binary, response.body()!!.bytes())

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/attachments/att_1", request.path)
    }

    @Test
    fun adminStatsEndpoint_isCovered() {
        enqueueJson("""{"user_count":100,"device_count":200,"active_session_count":50,"conversation_count":300,"pending_envelope_count":12,"total_storage_bytes":1234}""")

        val response = adminApi.loadStats(ADMIN_TOKEN).execute()
        assertTrue(response.isSuccessful)
        assertEquals(100, response.body()?.userCount)

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/admin/stats", request.path)
        assertEquals(ADMIN_TOKEN, request.getHeader("X-Admin-Token"))
    }

    @Test
    fun adminDeleteUserEndpoint_isCovered() {
        enqueueJson("{}")

        val response = adminApi.deleteUser(ADMIN_TOKEN, "usr_dead").execute()
        assertTrue(response.isSuccessful)

        val request = takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/admin/users/usr_dead", request.path)
    }

    @Test
    fun apiErrorSchema_isCovered() {
        val error =
            gson.fromJson(
                """{"error":{"code":8,"message":"Too many requests"}}""",
                ApiErrorEnvelopeDto::class.java,
            )

        assertEquals(8, error.error.code)
        assertEquals("Too many requests", error.error.message)
    }

    private fun enqueueJson(
        json: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(json),
        )
    }

    private fun takeRequest(): RecordedRequest =
        server.takeRequest(2, TimeUnit.SECONDS)
            ?: error("No request captured by MockWebServer.")

    private fun requestJson(request: RecordedRequest): JsonObject =
        JsonParser.parseString(request.body.readUtf8()).asJsonObject

    private companion object {
        const val BEARER = "Bearer acc_token"
        const val ADMIN_TOKEN = "admin-token"
    }
}
