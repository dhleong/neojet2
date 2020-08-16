package io.neovim.rpc

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.ObjectMapper
import io.neovim.DummyPacketsChannel
import io.neovim.Rpc
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.runBlockingUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class NeovimObjectMapperIntegrationTest {

    private lateinit var channel: NeovimChannel
    private lateinit var rpc: Rpc
    private lateinit var mapper: ObjectMapper
    private lateinit var idAllocator: IdAllocator

    private val readPackets = mutableListOf<Packet>()

    @Before fun setUp() {
        channel = EmbeddedChannel.Factory(
            args = listOf("-u", "NONE")
        ).create()
        rpc = Rpc(DummyPacketsChannel())
        mapper = createNeovimObjectMapper(rpc)
        idAllocator = SimpleIdAllocator()
        readPackets.clear()
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test(timeout = 3000) fun `Read with tabline`() {
        sendRequest("nvim_get_api_info")
        assertThat(nextPacket()).isNotNull()
            .isInstanceOf(ResponsePacket::class.java)

        sendRequest("nvim_command", "setlocal nolist")
        assertThat(nextPacket()).isNotNull()
            .isInstanceOf(ResponsePacket::class.java)

        sendRequest("nvim_command", "set laststatus=0")
        assertThat(nextPacket()).isNotNull()
            .isInstanceOf(ResponsePacket::class.java)

        sendRequest("nvim_ui_attach", 60, 25, mapOf<String, Any>(
            "ext_tabline" to true
        ))
        assertThat(nextPacket()).isNotNull()
            .isInstanceOf(ResponsePacket::class.java)

        val fileToEdit = File("build.gradle")
        val openRequestId = sendRequest("nvim_command",
            "noswapfile e! '${fileToEdit.absolutePath}'"
        )

        do {
            println("wait for response to $openRequestId")
            assertThat(nextPacket()).isNotNull()
        } while (readPackets.find { (it as? ResponsePacket)?.requestId == openRequestId } == null)

        println("done")
    }

    private fun sendRequest(
        method: String,
        vararg args: Any
    ): Long {
        val id = idAllocator.next()
        mapper.writeValue(channel.getOutputStream(), RequestPacket(
            requestId = id,
            method = method,
            args = args.toList()
        ))
        return id
    }

    private fun nextPacket() = mapper.reader()
        .readTree(channel.getInputStream())
        .let { root ->
            mapper.readerFor(Packet::class.java)
                .readValue<Packet>(root)
        }
        .also {
            readPackets += it
        }

}