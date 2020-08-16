package io.neovim

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.neovim.events.BufChangedtickEvent
import io.neovim.events.BufDetachEvent
import io.neovim.events.BufLinesEvent
import io.neovim.events.NeovimEvent
import io.neovim.impl.ApiMethodInfo
import io.neovim.rpc.*
import io.neovim.types.Buffer
import io.neovim.types.IncompatibleApiException
import io.neovim.types.NeovimApiInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext

private fun coroutineName(): String {
    val full = Thread.currentThread().name
    val thread = full.substringBefore(" ")
    val coroutine = full.substringAfterLast(" ")
    return "$thread $coroutine"
}
fun log(message: String) = println("[${coroutineName()}] $message")

const val DEFAULT_TIMEOUT_MS = 1500L

const val NVIM_API_INFO_REQUEST_METHOD = "nvim_get_api_info"

/**
 * @author dhleong
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Rpc(
    private val channel: PacketsChannel,
    private val ids: IdAllocator = SimpleIdAllocator(),
    private val responseTimeoutMillis: Long = DEFAULT_TIMEOUT_MS
) : AutoCloseable, CoroutineScope {

    constructor(
        channel: NeovimChannel,
        customEventTypes: Map<String, Class<out NeovimEvent>> = emptyMap(),
        responseTimeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ) : this(
        ObjectMapperPacketsChannel(channel, customEventTypes),
        responseTimeoutMillis = responseTimeoutMillis
    )

    init {
        channel.setRpc(this)
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val allPackets = BroadcastChannel<Packet>(16)
    private val availablePackets = mutableSetOf<Packet>()
    private val availablePacketsLock = Mutex()
    private val availableResponses = mutableMapOf<Long, ResponsePacket>()
    private val outstandingRequests = mutableMapOf<Long, CompletableDeferred<ResponsePacket?>>()

    private val ioThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val packetProducer = launch(ioThread) {
        val input = this@Rpc.channel

        while (job.isActive) {
            val packet = try {
                println("read packet ${Thread.currentThread()}...")
                // NOTE: don't block our single io thread with this blocking read
                withContext(Dispatchers.IO) {
                    input.readPacket()
                } ?: continue // unknown packet; discard and try again
            } catch (e: IOException) {
                if (e is MismatchedInputException && !job.isActive) {
                    // the mapper is just upset that it got EOF before it
                    // could read an object
                    return@launch
                }

                e.printStackTrace()
                break
            }

            println("<< read $packet")

            if (packet is ResponsePacket) {
                val outstanding = outstandingRequests[packet.requestId]
                if (outstanding != null) {
                    outstanding.complete(packet)
                } else {
                    availableResponses[packet.requestId] = packet
                }
                continue
            }

            availablePacketsLock.withLock {
                availablePackets.add(packet)
            }
//            if (packet is NeovimEvent) {
//                allPackets.offer(packet)
//            } else {
//                allPackets.send(packet)
//            }
            allPackets.send(packet)
            println("sent $packet")
        }
    }

    private val outboundQueue = Channel<Packet>(capacity = 8)
    private val packetSender = launch(ioThread) {
        val output = this@Rpc.channel
        for (packet in outboundQueue) {
            println(">> send $packet")
            output.writePacket(packet)
        }
    }

    private val requestTypes = mutableMapOf<Long, Class<*>>()

    private val apiInfoMutex = Mutex()
    private val apiInfo by lazy {
        runBlocking {
            requestApiInfo()
        }
    }

    suspend fun request(
        method: String,
        args: Any? = emptyList<Any?>(),
        methodInfo: ApiMethodInfo? = null
    ): ResponsePacket {
        val requiredApiLevel = methodInfo?.sinceVersion ?: 1
        if (methodInfo != null && methodInfo.deprecatedSinceVersion > -1) {
            log("WARN: deprecated method use: $method is deprecated since API ${methodInfo.deprecatedSinceVersion}")
        }

        // NOTE: ensure we always fetch api info as the first request; this
        // seems to fix issues where responses to certain requests
        // (esp: ui_attach with ext_tabline:true) don't get sent.
        val info = apiInfoMutex.withLock {
            if (method != NVIM_API_INFO_REQUEST_METHOD) {
                apiInfo
            } else null
        }

        if (requiredApiLevel > 1) {
            info?.let {
                val version = it.apiMetadata.version
                if (version.apiLevel < requiredApiLevel) {
                    throw IncompatibleApiException(
                        method,
                        "API level $requiredApiLevel > current API level ${version.apiLevel}"
                    )
                } else if (version.apiCompatible > requiredApiLevel) {
                    throw IncompatibleApiException(
                        method,
                        "API compat level ${version.apiCompatible} > required API level $requiredApiLevel"
                    )
                }
            }
        }

        return performRequest(method, args, methodInfo)
    }

    private suspend fun performRequest(
        method: String,
        args: Any? = emptyList<Any?>(),
        methodInfo: ApiMethodInfo? = null
    ): ResponsePacket {

        val request = RequestPacket(
            requestId = ids.next(),
            method = method,
            args = args
        )

        if (methodInfo?.resultType != null) {
            requestTypes[request.requestId] = methodInfo.resultType
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

        println("start timer($responseTimeoutMillis) for $method @ ${request.requestId}")
        return withTimeoutOrNull(responseTimeoutMillis) {
            awaitResponseTo(method, request.requestId)
        } ?: throw TimeoutException(
            "Timed out awaiting response to #${request.requestId}: $method($args)"
        )
    }

    private suspend fun awaitResponseTo(
        method: String, requestId: Long
    ): ResponsePacket {
        // enqueue a deferred *first* in case the response comes in early
        val deferred = CompletableDeferred<ResponsePacket?>(job)
        deferred.invokeOnCompletion {
            outstandingRequests.remove(requestId)
        }

        outstandingRequests[requestId] = deferred

        val pending = availableResponses.remove(requestId)
        if (pending != null) {
            // if the response came in before we enqueued our deferred,
            //  handle it here
            deferred.complete(pending)
        }

        return deferred.await()
            ?: throw IllegalStateException("Never received response to $method")
    }

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
        ioThread.close()
        packetProducer.cancel()
        packetSender.cancel()
        channel.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend inline fun <reified T : Packet> firstPacketThat(
        crossinline matching: (packet: T) -> Boolean
    ): T? {
        // open a channel *first*, in case the packet is broadcast while
        // we're in the lock
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

    private suspend fun requestApiInfo(): NeovimApiInfo? =
        performRequest(NVIM_API_INFO_REQUEST_METHOD,
            methodInfo = ApiMethodInfo(
                name = NVIM_API_INFO_REQUEST_METHOD,
                sinceVersion = 1,
                deprecatedSinceVersion = -1,
                resultType = NeovimApiInfo::class.java
            )
        ).result as? NeovimApiInfo

    companion object {
        fun create(
            channelFactory: NeovimChannel.Factory,
            responseTimeoutMillis: Long = DEFAULT_TIMEOUT_MS
        ): Rpc = try {
            Rpc(
                channelFactory.create(),
                responseTimeoutMillis = responseTimeoutMillis
            )
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Unable to connect to Neovim via $channelFactory",
                e
            )
        }
    }
}