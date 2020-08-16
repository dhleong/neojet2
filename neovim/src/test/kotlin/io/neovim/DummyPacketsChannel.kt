package io.neovim

import io.neovim.rpc.Packet
import io.neovim.rpc.PacketsChannel
import io.neovim.rpc.RequestPacket
import io.neovim.rpc.ResponsePacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking

/**
 * @author dhleong
 */
class DummyPacketsChannel : PacketsChannel {

    private val incomingQueue = Channel<Packet>(capacity = 10)
    private var hasApiInfoEnqueued = false

    @Suppress("MemberVisibilityCanBePrivate")
    val writtenPackets: List<Packet> = mutableListOf()

    override fun setRpc(rpc: Rpc) {
        // nop
    }

    override fun readPacket(): Packet? = runBlocking {
        incomingQueue.receive()
    }

    override fun writePacket(packet: Packet) {
        if (
            packet is RequestPacket
            && packet.requestId == 0L
            && packet.method == NVIM_API_INFO_REQUEST_METHOD
        ) {
            // ignore the required, automatic, initial api info request
            return
        }

        (writtenPackets as MutableList<Packet>).add(packet)
    }

    override fun close() {
        // nop
    }

    fun enqueueIncoming(packet: Packet) {
        if (!hasApiInfoEnqueued) {
            hasApiInfoEnqueued = true
            if (packet is ResponsePacket && packet.requestId > 0L) {
                // NOTE enqueue a fake response for the initial api info request
                enqueueIncoming(ResponsePacket(
                    requestId = 0,
                    result = apiInfo(currentLevel = 1)
                ))
            }
        }

        incomingQueue.sendBlocking(packet)
    }

}