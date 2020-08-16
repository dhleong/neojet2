package io.neovim

import io.neovim.rpc.Packet
import io.neovim.rpc.PacketsChannel
import io.neovim.rpc.ResponsePacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking

/**
 * @author dhleong
 */
class DummyPacketsChannel : PacketsChannel {

    private val incomingQueue = Channel<Packet>(capacity = 10)

    @Suppress("MemberVisibilityCanBePrivate")
    val writtenPackets: List<Packet> = mutableListOf()

    override fun setRpc(rpc: Rpc) {
        // nop
    }

    override fun readPacket(): Packet? = runBlocking {
        incomingQueue.receive()
    }

    override fun writePacket(packet: Packet) {
        (writtenPackets as MutableList<Packet>).add(packet)
    }

    override fun close() {
        // nop
    }

    fun enqueueIncoming(packet: Packet) {
        incomingQueue.sendBlocking(packet)
    }

}