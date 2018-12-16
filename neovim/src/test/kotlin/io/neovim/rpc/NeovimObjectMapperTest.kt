package io.neovim.rpc

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fasterxml.jackson.databind.ObjectMapper
import io.neovim.DummyPacketsChannel
import io.neovim.Rpc
import io.neovim.events.*
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
        assert(reconstituted).isEqualTo(packet)
    }

    @Test fun `Response Packet Round trip`() {
        val packet = ResponsePacket(
            requestId = 42,
            result = "serenity"
        )
        val serialized = mapper.writeValueAsBytes(packet)

        val reconstituted = mapper.readValue(serialized, Packet::class.java)
        assert(reconstituted).isEqualTo(packet)
    }

    @Test fun `Parse Notification with arguments`() {
        val read = readNotificationFromJson("""
            "resize", [4, 2]
        """.trimIndent())

        assert(read).isEqualTo(Resize(4, 2))
    }

    @Test fun `Parse Notification without arguments`() {
        val read = readNotificationFromJson("""
            "bell", []
        """.trimIndent())

        assert(read).isEqualTo(Bell)
    }

    @Test fun `Parse Unknown Notification without arguments`() {
        val read = readNotificationFromJson("""
            "docking_request", []
        """.trimIndent())

        assert(read).isNull()
    }

    @Test fun `Parse Unknown Notification with arguments`() {
        val read = readNotificationFromJson("""
            "docking_request", ["serenity"]
        """.trimIndent())

        assert(read).isNull()
    }

    @Test fun `Parse Redraw notification`() {
        val read = readNotificationFromJson("""
            "redraw", [
                ["put", ["z"], ["o"], ["e"]],
                ["flush", []]
            ]
        """.trimIndent())

        assert(read).isEqualTo(Redraw(listOf(
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

        assert(read).isEqualTo(Redraw(listOf(
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

        assert(read).isEqualTo(Redraw(listOf(
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
        assert(reconstituted).isNotNull {
            it.isInstanceOf(TablineUpdate::class.java)
        }
        assert((reconstituted as TablineUpdate).current.id).isEqualTo(42L)
    }

    private fun readNotificationFromJson(json: String): Packet? =
        readFromJson("[2, $json]")

    private fun readFromJson(json: String): Packet? =
        mapper.readValue(
            mapper.factory.createParser(json),
            Packet::class.java
        )
}
