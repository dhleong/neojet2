package io.neovim

import assertk.assert
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
        rpc = Rpc(EmbeddedChannel.Factory().create())
        api = NeovimApi.create(rpc)
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test fun `Basic API calls work`() = runBlockingUnit {
        assert(api.getCurrentLine()).isInstanceOf(String::class.java)
    }

    @Test fun `Custom types get decoded`() = runBlockingUnit {
        assert(api.getCurrentBuf()).isInstanceOf(Buffer::class.java)
    }

    @Test fun `Custom types get encoded`() = runBlockingUnit {
        val buf = api.getCurrentBuf()
        assert(buf).isInstanceOf(Buffer::class.java)

        assert {
            runBlocking {
                api.setCurrentBuf(buf)
            }
        }.returnedValue { }
    }

    @Test fun `Instance methods on custom types work`() = runBlockingUnit {
        val buffer = api.getCurrentBuf()

        assert(buffer.isValid()).isTrue()
        assert(buffer.lineCount()).isEqualTo(1L)
    }

    @Test fun `Arguments to methods on custom types work`() = runBlockingUnit {
        val win = api.getCurrentWin()

        assert(win.isValid()).isTrue()
        assert {
            runBlocking {
                win.setCursor(IntPair(1, 0))
            }
        }.returnedValue {  }
    }

    @Test fun `Read notifications`() = runBlockingUnit {
        api.uiAttach(10, 10, emptyMap())

        val nextEvent = api.nextEvent()
        assert(nextEvent).isNotNull {
            it.isInstanceOf(NeovimEvent::class)
        }
    }
}

