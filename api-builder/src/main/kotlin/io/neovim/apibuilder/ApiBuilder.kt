package io.neovim.apibuilder

import com.squareup.kotlinpoet.FileSpec
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

    val info = loadInfo()

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
}

fun findOutputRoot(args: Array<String>): File {
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

suspend fun loadInfo(): NeovimApiMetadata = timing("loadInfo") {
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