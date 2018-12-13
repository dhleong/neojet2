package io.neovim.rpc

import assertk.assert
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import io.neovim.DummyPacketsChannel
import io.neovim.Rpc
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class NeovimObjectMapperTest {

    lateinit var mapper: ObjectMapper

    @Before fun setUp() {
        mapper = createNeovimObjectMapper(Rpc(DummyPacketsChannel()))
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
}
