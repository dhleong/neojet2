package io.neovim.apibuilder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import io.neovim.Rpc
import io.neovim.types.NeovimApiMetadata

private val apiInterfaceClassName = ClassName("io.neovim", "NeovimApi")

/**
 * @author dhleong
 */
fun createApiInterface(
    info: NeovimApiMetadata
) = TypeSpec.interfaceBuilder(apiInterfaceClassName).apply {

    addKdoc("""
        Neovim functional interface

        Generated from Neovim v${info.version.major}.${info.version.minor}.${info.version.patch}

        @author dhleong

    """.trimIndent())

    suppressUnusedWarnings()

    for (fn in info.functions) {
        if (fn.isMethod || fn.isDeprecated) continue

        addFunction(fn.toFunSpec() )
    }

    addType(
        TypeSpec.companionObjectBuilder().apply {
            addFunction(
                FunSpec.builder("create").apply {
                    addParameter("rpc", Rpc::class)
                    returns(apiInterfaceClassName)

                    addStatement("""
                        return proxy(rpc)
                    """.trimIndent())
                }.build()
            )
        }.build()
    )

}.build()

