package io.neovim.rpc

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fasterxml.jackson.databind.ObjectMapper
import io.neovim.DummyPacketsChannel
import io.neovim.Rpc
import io.neovim.events.*
import io.neovim.types.Buffer
import io.neovim.types.Tabpage
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class NeovimObjectMapperTest {

    private val rpc = Rpc(DummyPacketsChannel())
    lateinit var mapper: ObjectMapper

    @Before fun setUp() {
        mapper = createNeovimObjectMapper(rpc)
    }

    @Test fun `Request Packet Round trip`() {
        val packet = RequestPacket(
            requestId = 42,
            method = "nvim_test",
            args = listOf("test")
        )
        val serialized = mapper.writeValueAsBytes(packet)

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assertThat(reconstituted).isEqualTo(packet)
    }

    @Test fun `Response Packet Round trip`() {
        val packet = ResponsePacket(
            requestId = 42,
            result = "serenity"
        )
        val serialized = mapper.writeValueAsBytes(packet)

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assertThat(reconstituted).isEqualTo(packet)
    }

    @Test fun `Parse Notification with arguments`() {
        val read = readNotificationFromJson("""
            "resize", [4, 2]
        """.trimIndent())

        assertThat(read).isEqualTo(Resize(4, 2))
    }

    @Test fun `Parse Notification without arguments`() {
        val read = readNotificationFromJson("""
            "bell", []
        """.trimIndent())

        assertThat(read).isEqualTo(Bell)
    }

    @Test fun `Parse Unknown Notification without arguments`() {
        val read = readNotificationFromJson("""
            "docking_request", []
        """.trimIndent())

        assertThat(read).isNull()
    }

    @Test fun `Parse Unknown Notification with arguments`() {
        val read = readNotificationFromJson("""
            "docking_request", ["serenity"]
        """.trimIndent())

        assertThat(read).isNull()
    }

    @Test fun `Parse Redraw notification`() {
        val read = readNotificationFromJson("""
            "redraw", [
                ["put", ["z"], ["o"], ["e"]],
                ["flush", []]
            ]
        """.trimIndent())

        assertThat(read).isEqualTo(Redraw(listOf(
            Put("z"),
            Put("o"),
            Put("e"),
            Flush
        )))
    }

    @Test fun `Parse order-flipped Redraw notification`() {
        val read = readNotificationFromJson("""
            "redraw", [
                ["flush", []],
                ["put", ["z"], ["o"], ["e"]]
            ]
        """.trimIndent())

        assertThat(read).isEqualTo(Redraw(listOf(
            Flush,
            Put("z"),
            Put("o"),
            Put("e")
        )))
    }

    @Test fun `Parse Redraw notification with unknown types`() {
        val read = readNotificationFromJson("""
            "redraw", [
                ["flush", []],
                ["docking_request", []],
                ["dock", [0, 1], [2, 3, 4]],
                ["wave", ["message1"], ["message2"]],
                ["put", ["z"], ["o"], ["e"]]
            ]
        """.trimIndent())

        assertThat(read).isEqualTo(Redraw(listOf(
            Flush,
            Put("z"),
            Put("o"),
            Put("e")
        )))
    }

    @Test fun `Parse embedded objects in notifications`() {
        val serialized = mapper.writeValueAsBytes(NotificationWrapper(
            name = "tabline_update",
            args = listOf(
                Tabpage.create(rpc, 42),
                listOf(
                    mapOf(
                        "tab" to Tabpage.create(rpc, 9001),
                        "name" to "serenity"
                    )
                )
            )
        ))

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assertThat(reconstituted).isNotNull().all {
            isInstanceOf(TablineUpdate::class.java)
        }
        assertThat((reconstituted as TablineUpdate).current.id).isEqualTo(42L)
    }

    @Test fun `Parse embedded objects in redraw notifications`() {
        val serialized = mapper.writeValueAsBytes(NotificationWrapper(
            name = "redraw",
            args = listOf(
                listOf(
                    "tabline_update",
                    listOf(
                        Tabpage.create(rpc, 42),
                        listOf(
                            mapOf(
                                "tab" to Tabpage.create(rpc, 9001),
                                "name" to "serenity"
                            )
                        )
                    )
                )
            )
        ))

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assertThat(reconstituted).isNotNull().all {
            isInstanceOf(Redraw::class.java)
        }
        val update = (reconstituted as Redraw).events[0]
        assertThat(update).isNotNull().all {
            isInstanceOf(TablineUpdate::class.java)
        }
        assertThat((update as TablineUpdate).current.id).isEqualTo(42L)
    }

    @Test fun `Handle custom notification types`() {
        mapper = createNeovimObjectMapper(rpc, mapOf(
            "docking_request" to DockingRequest::class.java
        ))

        val event = readNotificationFromJson("""
            "docking_request", ["ariel"]
        """.trimIndent())
        assertThat(event).isNotNull().all {
            isInstanceOf(DockingRequest::class.java)
        }
    }

    @Test fun `Handle manual-builtin notification types`() {
        val original = NotificationWrapper(
            name = "nvim_buf_detach_event",
            args = listOf(
                Buffer.create(rpc, 42)
            )
        )
        val serialized = mapper.writeValueAsBytes(original)

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assertThat(reconstituted).isNotNull().all {
            isInstanceOf(BufDetachEvent::class.java)
        }
        assertThat((reconstituted as BufDetachEvent).buffer.id).isEqualTo(42L)
    }

    private fun readNotificationFromJson(json: String): Packet? =
        readFromJson("[2, $json]")

    private fun readFromJson(json: String): Packet? =
        mapper.readValue(
            mapper.factory.createParser(json),
            Packet::class.java
        )
}

private class DockingRequest(
    val station: String
) : NeovimEvent()
