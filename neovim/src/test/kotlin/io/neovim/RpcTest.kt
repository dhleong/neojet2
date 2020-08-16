package io.neovim

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import io.neovim.impl.ApiMethodInfo
import io.neovim.rpc.ResponsePacket
import io.neovim.types.IncompatibleApiException
import io.neovim.types.NeovimApiInfo
import io.neovim.types.NeovimApiMetadata
import io.neovim.types.NeovimVersion
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
        packets.enqueueIncoming(ResponsePacket(requestId = 2))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))
        val response = rpc.request("test")
        assertThat(response).hasRequestId(1)
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Interleaved responses go to the right requestor`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 2))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        coroutineScope {
            val response = rpc.request("test1")
            assertThat(response).hasRequestId(1)
        }

        coroutineScope {
            val response = rpc.request("test2")
            assertThat(response).hasRequestId(2)
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 1000) fun `Interleaved responses go to the right requestor async`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 2))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        coroutineScope {
            async {
                val response = rpc.request("test1")
                assertThat(response).hasRequestId(1)
            }

            async {
                val response = rpc.request("test2")
                assertThat(response).hasRequestId(2)
            }
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Packets buffer if there's nobody to consume`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(requestId = 2))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        coroutineScope {
            async {
                val response = rpc.request("test1")
                assertThat(response).hasRequestId(1)
            }

            // due to this delay, the test1 packet might
            // have been lost
            delay(50)

            async {
                val response = rpc.request("test2")
                assertThat(response).hasRequestId(2)
            }
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Throw using future API level`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(
            requestId = 0,
            result = apiInfo(currentLevel = 5)
        ))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        assertThat {
            runBlocking {
                rpc.request(
                    "nvim_fancy_future",
                    methodInfo = requiredApiLevel(99)
                )
            }
        }.thrownError {
            isInstanceOf(IncompatibleApiException::class)
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 200) fun `Throw using ancient API level`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(
            requestId = 0,
            result = apiInfo(currentLevel = 20, compatLevel = 20)
        ))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        assertThat {
            runBlocking {
                rpc.request(
                    "nvim_ancient",
                    methodInfo = requiredApiLevel(2)
                )
            }
        }.thrownError {
            isInstanceOf(IncompatibleApiException::class)
        }
    }

    @Suppress("DeferredResultUnused")
    @Test(timeout = 500) fun `Allow using current API level`() = runBlockingUnit {
        packets.enqueueIncoming(ResponsePacket(
            requestId = 0,
            result = apiInfo(currentLevel = 5)
        ))
        packets.enqueueIncoming(ResponsePacket(requestId = 1))

        val response = rpc.request(
            "nvim_fancy_future",
            methodInfo = requiredApiLevel(5)
        )
        assertThat(response).isNotNull()
    }
}

fun apiInfo(currentLevel: Int, compatLevel: Int = 1) = NeovimApiInfo(
    0,
    NeovimApiMetadata(
        version = NeovimVersion(
            0, 3, 4,
            apiLevel = currentLevel,
            apiCompatible = compatLevel,
            apiPrerelease = false
        ),
        functions = emptyList(),
        types = emptyMap(),
        uiEvents = emptyList(),
        uiOptions = emptyList()
    )
)

fun requiredApiLevel(level: Int) = ApiMethodInfo(
    name = "",
    sinceVersion = level,
    resultType = Unit::class.java
)