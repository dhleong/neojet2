package io.neovim

import io.neovim.events.NeovimEvent
import io.neovim.rpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

private fun coroutineName(): String {
    val full = Thread.currentThread().name
    val thread = full.substringBefore(" ")
    val coroutine = full.substringAfterLast(" ")
    return "$thread $coroutine"
}
fun log(message: String) = println("[${coroutineName()}] $message")

/**
 * @author dhleong
 */
class Rpc(
    private val channel: PacketsChannel,
    private val ids: IdAllocator = SimpleIdAllocator()
) : AutoCloseable, CoroutineScope {

    constructor(
        channel: NeovimChannel,
        customEventTypes: Map<String, Class<out NeovimEvent>> = emptyMap()
    ) : this(
        ObjectMapperPacketsChannel(channel, customEventTypes)
    )

    init {
        channel.setRpc(this)
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @ExperimentalCoroutinesApi
    private val allPackets = BroadcastChannel<Packet>(16)
    private val availablePackets = mutableSetOf<Packet>()
    private val availablePacketsLock = ReentrantLock()

    @ExperimentalCoroutinesApi
    private val packetProducer = launch(Dispatchers.IO) {
        val input = this@Rpc.channel

        while (job.isActive) {
            val packet = try {
                log("await packet")
                input.readPacket()
                    ?: continue // unknown packet; discard and try again
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }

            log("packet << $packet")
            availablePacketsLock.withLock {
                availablePackets.add(packet)
            }
            allPackets.send(packet)
            log("dispatched << $packet")
        }
        log("quit loop")
    }

    private val outboundQueue = Channel<Packet>(capacity = 8)
    private val packetSender = launch(Dispatchers.IO) {
        val output = this@Rpc.channel
        for (packet in outboundQueue) {
            log("<< write $packet")
            output.writePacket(packet)
            log(">> write $packet")
        }
    }

    private val requestTypes = mutableMapOf<Long, Class<*>>()

    suspend fun request(
        method: String,
        args: Any? = emptyList<Any?>(),
        resultType: Class<*>? = null
    ): ResponsePacket {

        val request = RequestPacket(
            requestId = ids.next(),
            method = method,
            args = args
        )

        if (resultType != null) {
            requestTypes[request.requestId] = resultType
        }

        log("sending $request")
        send(request)

        log("await response to $request")
        return firstPacketThat {
            log("check(${request.requestId}) $it")
            it.requestId == request.requestId
        } ?: throw IllegalStateException("Never received response to $method")
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun send(packet: Packet) {
        outboundQueue.send(packet)
    }

    suspend fun nextEvent() = firstPacketThat<NeovimEvent> { true }

    override fun close() {
        job.cancel()
        @Suppress("EXPERIMENTAL_API_USAGE")
        packetProducer.cancel()
        packetSender.cancel()
        channel.close()
    }

    private suspend inline fun <reified T : Packet> firstPacketThat(
        matching: (packet: T) -> Boolean
    ): T? {
        // check the available buffered packets
        availablePacketsLock.withLock {
            val iterator = availablePackets.iterator()
            while (iterator.hasNext()) {
                val packet = iterator.next()
                if (packet is T && matching(packet)) {
                    iterator.remove()
                    return packet
                }
            }
        }

        log("await packet from subscription")
        @Suppress("EXPERIMENTAL_API_USAGE")
        val channel = allPackets.openSubscription()
        for (packet in channel) {
            if (packet is T && matching(packet)) {
                channel.cancel()
                availablePacketsLock.withLock {
                    availablePackets.remove(packet)
                }
                return packet
            }

            // make sure someone else has a chance to consume
            yield()

            // request another packet, because ours isn't here yet
        }

        channel.cancel()
        return null
    }

    internal fun getExpectedTypeForRequest(requestId: Long): Class<*>? =
        requestTypes.remove(requestId)

    companion object {
        fun create(channelFactory: NeovimChannel.Factory): Rpc {
            try {
                return Rpc(channelFactory.create())
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Unable to connect to Neovim via $channelFactory",
                    e
                )
            }
        }
    }
}