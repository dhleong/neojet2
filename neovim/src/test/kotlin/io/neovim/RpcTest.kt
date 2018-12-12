package io.neovim

import assertk.assert
import io.neovim.rpc.ResponsePacket
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class RpcTest {

    lateinit var rpc: Rpc
    lateinit var packets: DummyPacketsChannel

    @Before fun setUp() {
        packets = DummyPacketsChannel()
        rpc = Rpc(packets)
    }

    @Test fun `Request waits for correct response`() = runBlocking {
        packets.enqueueIncoming(ResponsePacket(requestId = 1))
        packets.enqueueIncoming(ResponsePacket(requestId = 0))
        val response = rpc.request("test")
        assert(response).hasRequestId(0)
    }

    @Test(timeout = 200) fun `Interleaved responses go to the right requestor`() = runBlocking {
        packets.enqueueIncoming(ResponsePacket(requestId = 1))
        packets.enqueueIncoming(ResponsePacket(requestId = 0))

        coroutineScope {
            val response = rpc.request("test0")
            assert(response).hasRequestId(0)
        }

        coroutineScope {
            val response = rpc.request("test1")
            assert(response).hasRequestId(1)
        }
    }
}

