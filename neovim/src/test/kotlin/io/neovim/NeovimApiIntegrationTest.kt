package io.neovim

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import io.neovim.events.NeovimEvent
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.types.Buffer
import io.neovim.types.IntPair
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class NeovimApiIntegrationTest {

    lateinit var api: NeovimApi
    lateinit var rpc: Rpc

    @Before fun setUp() {
        rpc = Rpc(EmbeddedChannel.Factory(
            args = listOf("nvim", "-u", "NONE")
        ).create())
        api = NeovimApi.create(rpc)
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test fun `Basic API calls work`() = runBlockingUnit {
        assertThat(api.getCurrentLine()).isInstanceOf(String::class.java)
    }

    @Test fun `Custom types get decoded`() = runBlockingUnit {
        assertThat(api.getCurrentBuf()).isInstanceOf(Buffer::class.java)
    }

    @Test fun `Custom types get encoded`() = runBlockingUnit {
        val buf = api.getCurrentBuf()
        assertThat(buf).isInstanceOf(Buffer::class.java)

        assertThat {
            runBlocking {
                api.setCurrentBuf(buf)
            }
        }.returnedValue { }
    }

    @Test fun `Instance methods on custom types work`() = runBlockingUnit {
        val buffer = api.getCurrentBuf()

        assertThat(buffer.isValid()).isTrue()
        assertThat(buffer.getName()).isEqualTo("")
        assertThat(buffer.getNumber()).isEqualTo(1L)
    }

    @Test fun `Arguments to methods on custom types work`() = runBlockingUnit {
        val win = api.getCurrentWin()

        assertThat(win.isValid()).isTrue()
        assertThat {
            runBlocking {
                win.setCursor(IntPair(1, 0))
            }
        }.returnedValue {  }
    }

    @Test fun `Read notifications`() = runBlockingUnit {
        api.uiAttach(10, 10, emptyMap())

        val nextEvent = api.nextEvent()
        assertThat(nextEvent).isNotNull().all {
            isInstanceOf(NeovimEvent::class)
        }
    }

    @Test(timeout = 1000) fun `Attach to buffer`() = runBlockingUnit {
        val buf = api.getCurrentBuf()

        // set some content to ensure the buffer is "loaded"
        buf.setLines(0, 0, false, listOf("Test"))

        val attached = buf.attach(false, emptyMap())
        assertThat(attached).isTrue()

//        rpc.send(RequestPacket(
//            requestId = 0,
//            method = "nvim_buf_attach",
//            args = listOf(0, false, emptyMap<Any, Any>())
//        ))
//        delay(1000)

//        val result = rpc.request("nvim_buf_attach", listOf(0, false, emptyMap<Any, Any>()), Boolean::class.java)
//        assert(result.result as Boolean).isTrue()
    }


//    @Test(timeout = 1000) fun `Busy open`() = runBlockingUnit {
//        api.command("setlocal nolist")
//
//        // set some content to ensure the buffer is "loaded"
//        buf.setLines(0, 0, false, listOf("Test"))
//
//        val attached = buf.attach(false, emptyMap())
//        assertThat(attached).isTrue()
//    }
}

