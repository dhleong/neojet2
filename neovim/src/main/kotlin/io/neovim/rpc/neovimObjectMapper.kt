package io.neovim.rpc

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.neovim.Rpc
import io.neovim.rpc.impl.expectNext
import io.neovim.rpc.impl.nextLong
import io.neovim.rpc.impl.nextString
import io.neovim.rpc.impl.nextTypedValue
import org.msgpack.jackson.dataformat.MessagePackFactory

/**
 * @author dhleong
 */
fun createNeovimObjectMapper(
    rpc: Rpc
): ObjectMapper {

    val module = ObjectMapperModule(rpc)

    val factory = MessagePackFactory().apply {
        disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
        disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
    }

    installNeovimExtensionTypes(rpc, factory, module)

    return ObjectMapper(factory).apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        registerKotlinModule()
        registerModule(module)
    }
}

private class ObjectMapperModule(
    private val rpc: Rpc
) : SimpleModule() {
    init {
        addDeserializer(Packet::class.java, PacketDeserializer())
    }

    private inner class PacketDeserializer : JsonDeserializer<Packet>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Packet {
            p.expectNext(JsonToken.VALUE_NUMBER_INT)

            val type = Packet.Type.create(p.intValue)
            return when (type) {
                Packet.Type.REQUEST -> p.readRequest()
                Packet.Type.RESPONSE -> p.readResponse()
                Packet.Type.NOTIFICATION -> p.readNotification()
            }
        }

        private fun JsonParser.readRequest(): Packet = RequestPacket(
            requestId = nextLong(),
            method = nextString(),
            args = nextTypedValue()
        )

        private fun JsonParser.readResponse(): Packet {
            val requestId = nextLong()
            val error: Any? = nextTypedValue()

            val result: Any? = rpc.getExpectedTypeForRequest(requestId)?.let { type ->
                nextTypedValue(type)
            } ?: nextTypedValue()

            return ResponsePacket(
                requestId = requestId,
                error = error,
                result = result
            )
        }

        private fun JsonParser.readNotification(): Packet {
            TODO()
        }
    }
}

