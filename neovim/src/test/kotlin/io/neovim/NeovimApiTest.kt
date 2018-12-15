package io.neovim

import assertk.assert
import assertk.assertions.all
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import io.neovim.impl.proxy
import io.neovim.rpc.RequestPacket
import io.neovim.rpc.ResponsePacket
import io.neovim.types.Buffer
import io.neovim.types.Window
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

    @Test fun `Custom types support normal Object methods`() {
        val buffer = proxy<Buffer>(rpc, 1)
        val win = proxy<Window>(rpc, 1)

        assert(buffer.toString()) {
            doesNotContain("proxy")
            contains("Buffer")
            contains("${buffer.id}")
        }
        assert(buffer).isEqualTo(buffer)
        assert(buffer.hashCode()).isEqualTo(buffer.hashCode())

        assert(win.toString()) {
            doesNotContain("proxy")
            contains("Window")
            contains("${win.id}")
        }
        assert(win).isEqualTo(win)
        assert(win.hashCode()).isEqualTo(win.hashCode())

        assert(win).isNotEqualTo(buffer)
        assert(buffer).isNotEqualTo(win)
        assert(win.hashCode()).isNotEqualTo(buffer.hashCode())
    }

    @Test fun `Custom type identity works across instances`() {
        val buf1 = proxy<Buffer>(rpc, 1)
        val buf2 = proxy<Buffer>(rpc, 1)

        assert(buf1).isEqualTo(buf2)
        assert(buf2).isEqualTo(buf1)
        assert(buf1.hashCode()).isEqualTo(buf2.hashCode())
    }


}

