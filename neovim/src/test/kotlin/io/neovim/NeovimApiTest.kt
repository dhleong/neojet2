package io.neovim

import assertk.all
import assertk.assertThat
import assertk.assertions.*
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

    private lateinit var api: NeovimApi
    private lateinit var rpc: Rpc
    private lateinit var packets: DummyPacketsChannel

    @Before fun setUp() {
        packets = DummyPacketsChannel()
        rpc = Rpc(packets)
        api = NeovimApi.create(rpc)
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test fun `Methods proxy`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 1, result = "mreynolds"))

        assertThat(api.getCurrentLine()).isEqualTo("mreynolds")

        assertThat(packets.writtenPackets).each {
            it.isInstanceOf(RequestPacket::class.java).all {
                hasMethod("nvim_get_current_line")
            }
        }
    }

    @Test fun `Custom types support normal Object methods`() {
        val buffer = proxy<Buffer>(rpc, 1)
        val win = proxy<Window>(rpc, 1)

        assertThat(buffer.toString()).all {
            doesNotContain("proxy")
            contains("Buffer")
            contains("${buffer.id}")
        }
        assertThat(buffer).isEqualTo(buffer)
        assertThat(buffer.hashCode()).isEqualTo(buffer.hashCode())

        assertThat(win.toString()).all {
            doesNotContain("proxy")
            contains("Window")
            contains("${win.id}")
        }
        assertThat(win).isEqualTo(win)
        assertThat(win.hashCode()).isEqualTo(win.hashCode())

        assertThat(win).isNotEqualTo(buffer)
        assertThat(buffer).isNotEqualTo(win)
        assertThat(win.hashCode()).isNotEqualTo(buffer.hashCode())
    }

    @Test fun `Custom type identity works across instances`() {
        val buf1 = proxy<Buffer>(rpc, 1)
        val buf2 = proxy<Buffer>(rpc, 1)

        assertThat(buf1).isEqualTo(buf2)
        assertThat(buf2).isEqualTo(buf1)
        assertThat(buf1.hashCode()).isEqualTo(buf2.hashCode())
    }


}

