package com.bialger.voxclient.data.datasource.remote

import com.bialger.voxclient.data.dto.VoxWebSocketEventDto
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VoxWebSocketServiceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun connectWithBearer_sendsAuthorizationHeader() {
        val serverOpenLatch = CountDownLatch(1)
        val clientOpenLatch = CountDownLatch(1)

        server.enqueue(
            MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        serverOpenLatch.countDown()
                        webSocket.close(1000, "done")
                    }
                },
            ),
        )

        val service = VoxWebSocketService(server.url("/").toString())
        service.connectWithBearer(
            accessToken = "acc_1",
            listener =
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        clientOpenLatch.countDown()
                    }
                },
        )

        assertTrue(serverOpenLatch.await(2, TimeUnit.SECONDS))
        assertTrue(clientOpenLatch.await(2, TimeUnit.SECONDS))

        val request = server.takeRequest(2, TimeUnit.SECONDS) ?: error("No handshake request captured.")
        assertEquals("Bearer acc_1", request.getHeader("Authorization"))
    }

    @Test
    fun connectWithDeferredAuth_sendsAuthFrame() {
        val messageLatch = CountDownLatch(1)
        var receivedMessage: String? = null

        server.enqueue(
            MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        receivedMessage = text
                        messageLatch.countDown()
                        webSocket.close(1000, "done")
                    }
                },
            ),
        )

        val service = VoxWebSocketService(server.url("/").toString())
        service.connectWithDeferredAuth(
            accessToken = "acc_2",
            listener = object : WebSocketListener() {},
        )

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS))
        assertEquals("""{"type":"auth","access_token":"acc_2"}""", receivedMessage)

        val request = server.takeRequest(2, TimeUnit.SECONDS) ?: error("No handshake request captured.")
        assertEquals(null, request.getHeader("Authorization"))
    }

    @Test
    fun parseEvent_coversAllDocumentedEventTypes() {
        val service = VoxWebSocketService("https://vox.example/")

        val envelope =
            service.parseEvent(
                """{"type":"envelope","envelope_id":"env_1","conversation_id":"conv_1","sender_device_id":"dev_remote","ciphertext":"c","server_timestamp":1,"envelope_type":0,"ordering_epoch":7}""",
            )
        assertTrue(envelope is VoxWebSocketEventDto.EnvelopeEvent)
        assertEquals("env_1", (envelope as VoxWebSocketEventDto.EnvelopeEvent).envelopeId)

        val membership =
            service.parseEvent(
                """{"type":"conversation_membership_changed","conversation_id":"conv_1","membership_version":4}""",
            )
        assertTrue(membership is VoxWebSocketEventDto.ConversationMembershipChangedEvent)

        val userDevices =
            service.parseEvent(
                """{"type":"user_devices_changed","user_id":"usr_bob"}""",
            )
        assertTrue(userDevices is VoxWebSocketEventDto.UserDevicesChangedEvent)

        val syncRecord =
            service.parseEvent(
                """{"type":"sync_record_changed","collection":"contacts","record_id":"contact_usr_bob","version":8}""",
            )
        assertTrue(syncRecord is VoxWebSocketEventDto.SyncRecordChangedEvent)
    }

    @Test
    fun parseEvent_returnsUnknownForUnexpectedOrInvalidPayload() {
        val service = VoxWebSocketService("https://vox.example/")

        val unknown = service.parseEvent("""{"type":"not_known","x":1}""")
        assertTrue(unknown is VoxWebSocketEventDto.UnknownEvent)
        assertEquals("not_known", (unknown as VoxWebSocketEventDto.UnknownEvent).type)

        val invalid = service.parseEvent("""not-json""")
        assertTrue(invalid is VoxWebSocketEventDto.UnknownEvent)
        assertFalse((invalid as VoxWebSocketEventDto.UnknownEvent).rawPayload.isEmpty())
    }
}
