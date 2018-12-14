package io.neovim.apibuilder

import com.squareup.kotlinpoet.*
import io.neovim.ApiExtensionType
import io.neovim.Rpc
import io.neovim.types.NeovimApiMetadata
import io.neovim.types.NeovimApiTypeInfo
import io.neovim.types.NeovimObject

/**
 * @author dhleong
 */
fun createTypeInterface(
    apiInfo: NeovimApiMetadata,
    name: String,
    typeInfo: NeovimApiTypeInfo
): TypeSpec {
    val className = ClassName("io.neovim.types", name)

    return TypeSpec.interfaceBuilder(className).apply {

        addKdoc("""
            Interface to "$name" Neovim type

            @author dhleong

        """.trimIndent())

        suppressUnusedWarnings()
        addAnnotation(AnnotationSpec.builder(ApiExtensionType::class).apply {
            addMember("id = %L", typeInfo.id)
        }.build())

        addSuperinterface(NeovimObject::class.java)

        apiInfo.functions.filter {
            it.isMethod
                && it.name.startsWith(typeInfo.prefix)
        }.forEach { fn ->
            addFunction(fn.toFunSpec(prefix = typeInfo.prefix))
        }

        addType(
            TypeSpec.companionObjectBuilder().apply {
                addSuperinterface(NeovimObject.Factory::class)
                addFunction(
                    FunSpec.builder("create").apply {
                        addParameter("rpc", Rpc::class)
                        addParameter("id", LONG)
                        addModifiers(KModifier.OVERRIDE)
                        returns(className)

                        addStatement("""
                            return proxy(rpc, id)
                        """.trimIndent())
                    }.build()
                )
            }.build()
        )
    }.build()
}
