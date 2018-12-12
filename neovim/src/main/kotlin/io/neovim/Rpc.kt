package io.neovim

import io.neovim.rpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
class Rpc internal constructor(
    private val channel: PacketsChannel,
    private val ids: IdAllocator = SimpleIdAllocator()
) : AutoCloseable, CoroutineScope {

    constructor(channel: NeovimChannel) : this(
        ObjectMapperPacketsChannel(channel)
    )

    init {
        channel.setRpc(this)
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @ExperimentalCoroutinesApi
    private val packetProducer = produce {
        val input = this@Rpc.channel

        while (job.isActive) {
            val packet = input.readPacket() ?: break

            send(packet)
        }
    }
    private val discardedPackets = Channel<Packet>(capacity = 16)

    @ExperimentalCoroutinesApi
    private val allPackets = produce {
        while (job.isActive) {
            val selected = select<Packet> {
                packetProducer.onReceive { it }
                discardedPackets.onReceive { it }
            }
            send(selected)
        }
    }

    private val outboundQueue = Channel<Packet>(capacity = 8)
    private val packetSender = launch {
        val output = this@Rpc.channel
        for (packet in outboundQueue) {
            output.writePacket(packet)
        }
    }

    suspend fun request(
        method: String,
        args: Any? = null
    ): ResponsePacket {
        val request = RequestPacket(
            requestId = ids.next(),
            method = method,
            args = args
        )
        send(request)

        return firstPacketThat {
            it.requestId == request.requestId
        }
    }

    suspend fun send(packet: Packet) {
        outboundQueue.send(packet)
    }

    override fun close() {
        job.cancel()
        packetSender.cancel()
        channel.close()
    }

    private suspend inline fun <reified T : Packet> firstPacketThat(
        matching: (packet: T) -> Boolean
    ): T {

        var lastDiscarded: Packet? = null

        @Suppress("EXPERIMENTAL_API_USAGE")
        for (packet in allPackets) {
            if (packet is T && matching(packet)) {
                return packet
            }

            @Suppress("LiftReturnOrAssignment")
            if (packet != lastDiscarded) {
                discardedPackets.send(packet)
                lastDiscarded = packet
            } else {
                // if we got it again, chances are we're the only
                // ones listening and/or nobody else wants it,
                // so just actually discard it
                lastDiscarded = null
            }

            yield()
        }

        throw IllegalStateException("Never received a matching packet")
    }
}