package io.neovim.apibuilder

import com.fasterxml.jackson.annotation.JsonFormat
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.neovim.NeovimApi
import io.neovim.Rpc
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.types.NeovimApiMetadata
import java.io.File

/**
 * @author dhleong
 */
suspend fun main(args: Array<String>) = timing("generate all interfaces") {

    val outputRoot = findOutputRoot(args)
    println("Writing to $outputRoot")

    val info = timing("load API info") { loadInfo() }

    timing("wrote API interface") {
        val apiFile = FileSpec.builder("io.neovim", "NeovimApi").apply {
            addProxyImport()
            addType(createApiInterface(info))
        }.build()
        apiFile.writeTo(outputRoot)
    }

    info.types.forEach { typeName, typeInfo -> timing("wrote $typeName") {
        val typeFile = FileSpec.builder("io.neovim.types", typeName).apply {
            addProxyImport()
            addType(createTypeInterface(info, typeName, typeInfo))
        }.build()

        typeFile.writeTo(outputRoot)
    } }

    timing("wrote events") {
        val eventsFile = FileSpec.builder("io.neovim.events", "events").apply {

            addComment("""
                Sealed class implementations of Neovim event types, so you can
                use an exhaustive `when ()` on a bare instance.

                ${info.formatGenerated()}

                @author dhleong

            """.trimIndent())

            val baseEventName = ClassName("io.neovim.events", "NeovimEvent")
            val baseEventType = TypeSpec.classBuilder(baseEventName).apply {
                addModifiers(KModifier.SEALED)
                addAnnotation(AnnotationSpec.builder(JsonFormat::class).apply {
                    addMember("shape = JsonFormat.Shape.ARRAY")
                }.build())
            }.build()

            addType(baseEventType)

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
            addFunction(FunSpec.builder("createEventTypesMap").apply {
                addModifiers(KModifier.INTERNAL)
                addKdoc("""
                    Creates a map of event name -> event type for use deserializing

                    ${info.formatGenerated()}

                    @author dhleong

                """.trimIndent())

                val returnType = ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                    String::class.asTypeName(),
                    Class::class.asTypeName().parameterizedBy(STAR)
                )
                returns(returnType)

                addStatement("val map: %T = mutableMapOf()", returnType)

                info.uiEvents.filter {
                    !it.isDeprecated
                }.forEach { event ->
                    addStatement(
                        "map[%S] = %T::class.java",
                        event.name,
                        ClassName("io.neovim.events", event.name.toCamelCase().capitalize())
                    )
                }

                addStatement("return map")
            }.build())
        }.build()

        eventsMapFile.writeTo(outputRoot)
    }
}

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