package io.neovim.rpc

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import io.neovim.ApiExtensionType
import io.neovim.Rpc
import io.neovim.rpc.impl.nextLong
import io.neovim.types.Buffer
import io.neovim.types.NeovimObject
import io.neovim.types.Tabpage
import io.neovim.types.Window
import org.msgpack.core.MessagePack
import org.msgpack.jackson.dataformat.ExtensionTypeCustomDeserializers
import org.msgpack.jackson.dataformat.MessagePackExtensionType
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.msgpack.jackson.dataformat.MessagePackGenerator
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation

fun installNeovimExtensionTypes(
    rpc: Rpc,
    factory: MessagePackFactory,
    module: SimpleModule
) {
    val deserializers = ExtensionTypeCustomDeserializers()

    register<Buffer>(rpc, factory, module, deserializers)
    register<Window>(rpc, factory, module, deserializers)
    register<Tabpage>(rpc, factory, module, deserializers)

    factory.setExtTypeCustomDesers(deserializers)
}

private inline fun <reified T : NeovimObject> register(
    rpc: Rpc,
    factory: MessagePackFactory,
    module: SimpleModule,
    deserializers: ExtensionTypeCustomDeserializers
) {
    val info = T::class.findAnnotation<ApiExtensionType>()
        ?: throw IllegalStateException()
    val typeIdByte = info.id.toByte()

    val objectFactory = T::class.companionObjectInstance as NeovimObject.Factory

    module.addSerializer(T::class.java, object : JsonSerializer<T>() {
        val packer = MessagePack.newDefaultBufferPacker()

        override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
            packer.clear()
            packer.packLong(value.id)

            val mpGen = gen as MessagePackGenerator
            mpGen.writeExtensionType(MessagePackExtensionType(
                typeIdByte,
                packer.toByteArray()
            ))
        }
    })

    val deserializer = object : JsonDeserializer<T>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): T {
            return p.embeddedObject as T
        }
    }
    module.addDeserializer(T::class.java, deserializer)

    deserializers.addCustomDeser(typeIdByte) { bytes ->
        val parser = factory.createParser(bytes)
        val id = parser.nextLong()
        objectFactory.create(rpc, id)
    }
}
