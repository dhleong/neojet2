package io.neovim.apibuilder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.neovim.ApiMethod
import io.neovim.NeovimApi
import io.neovim.Rpc
import io.neovim.apibuilder.builtins.bufferEventTypes
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.types.NeovimApiMetadata
import java.io.File
import kotlin.reflect.full.findAnnotation

const val INDENT = "    "

/**
 * @author dhleong
 */
suspend fun main(args: Array<String>) = timing("generate all interfaces") {

    val outputRoot = findOutputRoot(args)
    println("Writing to $outputRoot")

    val info = timing("load API info") { loadInfo() }

    timing("wrote API interface") {
        val apiFile = FileSpec.builder("io.neovim", "NeovimApi").apply {
            indent(INDENT)
            addProxyImport()
            addType(createApiInterface(info))
        }.build()
        apiFile.writeTo(outputRoot)
    }

    info.types.forEach { (typeName, typeInfo) -> timing("wrote $typeName") {
        val typeFile = FileSpec.builder("io.neovim.types", typeName).apply {
            indent(INDENT)
            addProxyImport()
            addType(createTypeInterface(info, typeName, typeInfo))
        }.build()

        typeFile.writeTo(outputRoot)
    } }

    val baseEventName = ClassName("io.neovim.events", "NeovimEvent")
    timing("wrote events") {
        val eventsFile = FileSpec.builder("io.neovim.events", "events").apply {
            indent(INDENT)

            addComment("""
                Implementations of core Neovim event types.

                ${info.formatGenerated()}

                @author dhleong

            """.trimIndent())

            addType(createBaseEventType(baseEventName))

            addType(TypeSpec.classBuilder(ClassName("io.neovim.events", "Redraw")).apply {
                addModifiers(KModifier.DATA)
                superclass(baseEventName)
                addKdoc("""
                    Redraw is an aggregate event that is not directly generated by the
                    API info since its format is non-standard

                """.trimIndent())

                primaryConstructor(FunSpec.constructorBuilder().apply {
                    val eventsType = List::class.asTypeName().parameterizedBy(baseEventName)
                    addParameter("events", eventsType)
                    addProperty(PropertySpec.builder("events", eventsType).apply {
                        initializer("events")
                    }.build())
                }.build())
            }.build())

            info.uiEvents.filter {
                !it.isDeprecated
            }.forEach { event ->
                addType(createEvent(baseEventName, event))
            }
        }.build()

        eventsFile.writeTo(outputRoot)
    }

    timing("wrote events map") {
        val eventsMapFile = FileSpec.builder("io.neovim.events", "map").apply {
            indent(INDENT)
            addFunction(FunSpec.builder("createEventTypesMap").apply {
                addModifiers(KModifier.INTERNAL)
                addKdoc("""
                    Creates a map of event name -> event type for use deserializing

                    ${info.formatGenerated()}

                    @author dhleong

                """.trimIndent())


                val returnType = ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                    String::class.asTypeName(),
                    Class::class.asTypeName().parameterizedBy(
                        WildcardTypeName.producerOf(
                            baseEventName
                        )
                    )
                )
                returns(returnType)

                addStatement("val map: %T = mutableMapOf()", returnType)

                allBuiltinEventTypes(info).forEach { (name, type) ->
                    addStatement(
                        "map[%S] = %T::class.java",
                        name,
                        type
                    )
                }

                addStatement("return map")
            }.build())
        }.build()

        eventsMapFile.writeTo(outputRoot)
    }
}

private fun allBuiltinEventTypes(info: NeovimApiMetadata) = (
    info.uiEvents.asSequence().filter {
        !it.isDeprecated
    }.map {
        it.name to ClassName(
            "io.neovim.events",
            it.name.toCamelCase().capitalize()
        )
    }

    +

    bufferEventTypes.map {
        it.findAnnotation<ApiMethod>()!!.name to it.asClassName()
    }
)

private fun findOutputRoot(args: Array<String>): File {
    if (args.isNotEmpty()) {
        return File(args[0])
    }

    val neovimGradle = File("neovim/build.gradle")
    if (!neovimGradle.isFile) {
        throw IllegalArgumentException("Not in project root; unsure where to output files")
    }

    val packageRoot = File("neovim/src/main/kotlin/").absoluteFile
    if (!packageRoot.isDirectory) {
        throw IllegalArgumentException("Package root does not exist: $packageRoot")
    }

    return packageRoot
}

private fun FileSpec.Builder.addProxyImport() {
    addImport("io.neovim.impl", "proxy")
}

suspend fun loadInfo(): NeovimApiMetadata {
    val rpc = Rpc(EmbeddedChannel.Factory().create())
    val api = NeovimApi.create(rpc)
    return rpc.use {
        val (_, apiInfo) = api.getApiInfo()
        apiInfo
    }
}

inline fun <R> timing(task: String, block: () -> R): R {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()

    println("[$task] in ${end - start}ms")
    return result
}