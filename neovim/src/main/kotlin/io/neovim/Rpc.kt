package io.neovim

import io.neovim.events.BufChangedtickEvent
import io.neovim.events.BufDetachEvent
import io.neovim.events.BufLinesEvent
import io.neovim.events.NeovimEvent
import io.neovim.rpc.*
import io.neovim.types.Buffer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select
import java.io.IOException
import java.util.concurrent.TimeoutException
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
    private val ids: IdAllocator = SimpleIdAllocator(),
    private val responseTimeoutMillis: Long = 1500
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
                input.readPacket()
                    ?: continue // unknown packet; discard and try again
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }

            println("<< read $packet")
            availablePacketsLock.withLock {
                availablePackets.add(packet)
            }
            allPackets.send(packet)
        }
    }

    private val outboundQueue = Channel<Packet>(capacity = 8)
    private val packetSender = launch(Dispatchers.IO) {
        val output = this@Rpc.channel
        for (packet in outboundQueue) {
            output.writePacket(packet)
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

        send(request)

        // see: https://github.com/neovim/neovim/issues/8634
        when (method) {
            "nvim_buf_attach" -> {
                val argsList = args as List<Any?>
                val bufferId = argsList[0].toBufferId()
                return fakeBufAttachResponse(
                    method, request.requestId,
                    bufferId = bufferId,
                    returnBuffer = argsList[1] as Boolean
                )
            }

            "nvim_buf_detach" -> {
                val argsList = args as List<Any?>
                val bufferId = argsList[0].toBufferId()
                return fakeBufDetachResponse(
                    method, request.requestId,
                    bufferId = bufferId
                )
            }
        }

        return withTimeoutOrNull(responseTimeoutMillis) {
            awaitResponseTo(method, request.requestId)
        } ?: throw TimeoutException(
            "Timed out awaiting response to #${request.requestId}: $method($args)"
        )
    }

    private suspend fun awaitResponseTo(
        method: String, requestId: Long
    ) = firstPacketThat<ResponsePacket> {
        it.requestId == requestId
    } ?: throw IllegalStateException("Never received response to $method")

    private fun Any?.toBufferId() =
        (this as? Buffer)?.id
            ?: (this as? Long)
            ?: (this as? Int)?.toLong()
            ?: throw IllegalArgumentException("No buffer id? got: $this")

    private suspend fun fakeBufAttachResponse(
        method: String,
        requestId: Long,
        returnBuffer: Boolean,
        bufferId: Long
    ): ResponsePacket = select {
        // prefer a proper response...
        scopedAsync {
            awaitResponseTo(method, requestId)
        }.onAwait { it }

        // ... but also accept we see nvim_buf_changed_tick
        if (!returnBuffer) {
            scopedAsync {
                awaitMatchingEvent<BufChangedtickEvent> {
                    bufferId == 0L // "current buffer"
                            || it.buffer.id == bufferId
                }
            }.onAwait { ResponsePacket(requestId = requestId, result = true) }
        }

        // ... or nvim_buf_lines_event
        if (returnBuffer) {
            scopedAsync {
                awaitMatchingEvent<BufLinesEvent> {
                    bufferId == 0L // "current buffer"
                        || it.buffer.id == bufferId
                }
            }.onAwait { ResponsePacket(requestId = requestId, result = true) }
        }
    }

    private suspend fun fakeBufDetachResponse(
        method: String,
        requestId: Long,
        bufferId: Long
    ): ResponsePacket = select {
        // prefer a proper response...
        scopedAsync {
            awaitResponseTo(method, requestId)
        }.onAwait { it }

        // ... but also accept we see nvim_buf_detached
        scopedAsync {
            awaitMatchingEvent<BufDetachEvent> {
                bufferId == 0L // "current buffer"
                    || it.buffer.id == bufferId
            }
        }.onAwait { ResponsePacket(requestId = requestId, result = true) }
    }

    /**
     * Simple util wrapper to disambiguate the coroutineScope for async {}
     */
    private inline fun <T> scopedAsync(
        crossinline block: suspend () -> T
    ): Deferred<T> = async { block() }

    private suspend inline fun <reified T : NeovimEvent> awaitMatchingEvent(
        crossinline matcher: (T) -> Boolean
    ): T {
        val event = firstPacketThat(matcher)
            ?: throw IllegalStateException()

        // wait a bit in case a real response comes in
        delay(200)

        // and, if we're still around, return the event:
        return event
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
        crossinline matching: (packet: T) -> Boolean
    ): T? {
        // open a channel *first*, in case the packet is broadcast while
        // we're in the lock
        @Suppress("EXPERIMENTAL_API_USAGE")
        val channel = allPackets.openSubscription()
        return scopedAsync {
            channel.firstPacketThat(matching)
        }.also {
            it.invokeOnCompletion { channel.cancel() }
        }.await()
    }

    private suspend inline fun <reified T : Packet> ReceiveChannel<Packet?>.firstPacketThat(
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

        for (packet in this) {
            if (packet is T && matching(packet)) {
                availablePacketsLock.withLock {
                    availablePackets.remove(packet)
                }
                return packet
            }

            // make sure someone else has a chance to consume
            yield()

            // request another packet, because ours isn't here yet
        }

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