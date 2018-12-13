package io.neovim

import assertk.assert
import assertk.assertions.isInstanceOf
import io.neovim.rpc.channels.EmbeddedChannel
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
}
