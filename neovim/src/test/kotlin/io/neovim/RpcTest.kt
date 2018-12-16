package io.neovim

import assertk.assert
import io.neovim.rpc.ResponsePacket
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Interleaved responses go to the right requestor`() = runBlockingUnit {
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

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Interleaved responses go to the right requestor async`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 1))
        packets.enqueueIncoming(ResponsePacket(requestId = 0))

        coroutineScope {
            async {
                val response = rpc.request("test0")
                assert(response).hasRequestId(0)
            }

            async {
                val response = rpc.request("test1")
                assert(response).hasRequestId(1)
            }
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Packets buffer if there's nobody to consume`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 1))
        packets.enqueueIncoming(ResponsePacket(requestId = 0))

        coroutineScope {
            async {
                val response = rpc.request("test0")
                assert(response).hasRequestId(0)
            }

            // due to this delay, the test1 packet might
            // have been lost
            delay(50)

            async {
                val response = rpc.request("test1")
                assert(response).hasRequestId(1)
            }
        }
    }
}

