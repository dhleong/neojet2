package io.neovim

import assertk.assert
import assertk.assertions.all
import assertk.assertions.isEqualTo
import io.neovim.rpc.RequestPacket
import io.neovim.rpc.ResponsePacket
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class NeovimApiTest {

    lateinit var api: NeovimApi
    lateinit var rpc: Rpc
    lateinit var packets: DummyPacketsChannel

    @Before fun setUp() {
        packets = DummyPacketsChannel()
        rpc = Rpc(packets)
        api = NeovimApi.create(rpc)
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test fun `Methods proxy`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 0, result = "mreynolds"))

        assert(api.getCurrentLine()).isEqualTo("mreynolds")

        assert(packets.writtenPackets).all {
            assert(it.actual as RequestPacket).hasMethod("nvim_get_current_line")
        }
    }
}

