package io.neovim.rpc

import com.fasterxml.jackson.databind.ObjectMapper
import io.neovim.Rpc

/**
 * @author dhleong
 */
interface PacketsChannel : AutoCloseable {
    fun setRpc(rpc: Rpc)
    fun readPacket(): Packet?
    fun writePacket(packet: Packet)
}

internal class ObjectMapperPacketsChannel(
    private val channel: NeovimChannel
) : PacketsChannel, AutoCloseable by channel {
    private lateinit var mapper: ObjectMapper

    private val input by lazy { channel.getInputStream() }
    private val output by lazy { channel.getOutputStream() }

    override fun setRpc(rpc: Rpc) {
        mapper = createNeovimObjectMapper(rpc)
    }

    override fun readPacket(): Packet? =
        mapper.readValue(input, Packet::class.java)

    override fun writePacket(packet: Packet) {
        mapper.writeValue(output, packet)
    }

}