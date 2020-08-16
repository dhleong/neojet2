package io.neovim.rpc

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.readValues
import io.neovim.Rpc
import io.neovim.events.NeovimEvent
import java.io.BufferedInputStream
import java.io.IOException

/**
 * @author dhleong
 */
interface PacketsChannel : AutoCloseable {
    fun setRpc(rpc: Rpc)
    fun readPacket(): Packet?
    fun writePacket(packet: Packet)
}

internal class ObjectMapperPacketsChannel(
    private val channel: NeovimChannel,
    private val customEventTypes: Map<String, Class<out NeovimEvent>>
) : PacketsChannel, AutoCloseable by channel {
    private lateinit var mapper: ObjectMapper

    private val input by lazy { BufferedInputStream(channel.getInputStream()) }
    private val output by lazy { channel.getOutputStream() }

    private val reader by lazy {
        mapper.readerFor(Packet::class.java)
    }

    override fun setRpc(rpc: Rpc) {
        mapper = createNeovimObjectMapper(rpc, customEventTypes)
    }

    override fun readPacket(): Packet? = synchronized(this) {
        println("readPacket...")
        return mapper.reader().readTree(input).let { root ->
            println("read; parsing...")
            reader.readValue(root)
        }
    }

//        reader.readValue(input)
//        mapper.reader().readValue(input)
//        mapper.readValue(parser)
            // NOTE: if the parser didn't clear this, MappingIterator
            // will think that it's at EOF for... SOME reason!?
//            with (iterator.parser) {
//                if (currentToken() == null) nextToken()
//                if (currentToken() == JsonToken.END_ARRAY) {
//                    nextToken()
//                }
//            }
//        mapper.readValue(input, Packet::class.java)

    override fun writePacket(packet: Packet) {
        mapper.writer().writeValue(output, packet)
//        mapper.writeValue(output, packet)
        output.flush()
    }

}

