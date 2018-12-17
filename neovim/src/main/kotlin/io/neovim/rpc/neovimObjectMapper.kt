package io.neovim.rpc

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.neovim.Rpc
import io.neovim.events.NeovimEvent
import io.neovim.events.Redraw
import io.neovim.events.createEventTypesMap
import io.neovim.rpc.impl.*
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.io.IOException

/**
 * @author dhleong
 */
fun createNeovimObjectMapper(
    rpc: Rpc,
    customEventTypes: Map<String, Class<out NeovimEvent>> = emptyMap()
): ObjectMapper {

    val module = ObjectMapperModule(rpc, customEventTypes)

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
    private val rpc: Rpc,
    customEventTypes: Map<String, Class<out NeovimEvent>>
) : SimpleModule() {

    private val eventsMap = createEventTypesMap() + customEventTypes
    private val instancesMap = mutableMapOf<Class<out NeovimEvent>, NeovimEvent?>()

    init {
        addDeserializer(Packet::class.java, PacketDeserializer())
    }

    private inner class PacketDeserializer : JsonDeserializer<Packet>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Packet? {
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
            val error: ErrorResponse? = nextTypedValue()

            val result: Any? = rpc.getExpectedTypeForRequest(requestId)?.let { type ->
                nextTypedValue(type)
            } ?: nextTypedValue()

            return ResponsePacket(
                requestId = requestId,
                error = error,
                result = result
            )
        }

        private fun JsonParser.readNotification(): Packet? {
            val name = nextString()

            if (name == "redraw") {
                // redraw is a special case; it has a single argument that is
                // an array of `[eventType, [argTuple, ...]]` tuples
                return readRedraw()
            }

            val type = eventsMap[name]
                ?: return skipUnknownPacket()

            nextToken()
            return readEventValue(type) as Packet
        }

        private fun JsonParser.skipUnknownPacket(): Packet? {
            expectNext(JsonToken.START_ARRAY)
            skipChildren()
            nextToken()
            return null
        }

        private fun JsonParser.readRedraw(): Packet {
            expectNext(JsonToken.START_ARRAY)

            val contents = mutableListOf<NeovimEvent>()

            while (nextToken() == JsonToken.START_ARRAY) {
                val name = nextString()
                val type = eventsMap[name]
                if (type == null) {
                    // skip unknown events
                    while (nextToken() == JsonToken.START_ARRAY) {
                        skipChildren()
                    }
                    continue
                }

                while (nextToken() != JsonToken.END_ARRAY) {
                    expect(JsonToken.START_ARRAY)
                    val subEvent = readEventValue(type) ?: break // no more
                    contents.add(subEvent)
                }
                expect(JsonToken.END_ARRAY)
            }

            expect(JsonToken.END_ARRAY)
            return Redraw(contents)
        }

        private fun JsonParser.readEventValue(
            type: Class<out NeovimEvent>
        ): NeovimEvent? {
            //NOTE: you would think that if reading it as a Tree
            // and then parsing from that works, we could just read it
            // directly without problems... but you'd be wrong.
            // Reading directly causes:
            //   Cannot deserialize instance of `java.util.ArrayList` out
            //   of VALUE_EMBEDDED_OBJECT token
            // when parsing eg: TablineUpdate (see NeovimObjectMapperTest)
            val tree = readValueAsTree<TreeNode>()
            return codec.treeAsTokens(tree)
                .inflateNextEventValue(type)
//            return inflateNextEventValue(type) as Packet
        }

        private fun JsonParser.inflateNextEventValue(
            type: Class<out NeovimEvent>
        ): NeovimEvent? = try {
            val instance = instancesMap[type]
                ?: type.findInstance().also {
                    instancesMap[type] = it
                }

            if (instance !== NotSingleInstance) {
                // skip the empty array...
                expectNext(JsonToken.START_ARRAY)
                skipChildren()
                nextToken()

                // ... and use the singleton
                instance
            } else {
                nextTypedValue(type)
            }
        } catch (e: JsonProcessingException) {
            throw IOException(
                "Unable to deserialize $type",
                e
            )
        }
    }

    companion object {
        private val NotSingleInstance = Redraw(emptyList())
    }

    private fun <T : NeovimEvent> Class<T>.findInstance(): NeovimEvent =
        kotlin.objectInstance ?: NotSingleInstance
}


