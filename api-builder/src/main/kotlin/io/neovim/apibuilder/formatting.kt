package io.neovim.apibuilder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.neovim.types.*

/**
 * @author dhleong
 */
fun NeovimApiType.toTypeName(): TypeName = when (this) {
    "Array" -> List::class.asClassName().parameterizedBy(ANY)
    "ArrayOf(Integer, 2)" -> IntPair::class.asClassName()
    "Boolean" -> BOOLEAN
    "Dictionary" -> Map::class.asClassName().parameterizedBy(
        String::class.asClassName(),
        ANY
    )
    "Float" -> DOUBLE
    "Integer" -> LONG
    "Object" -> ANY
    "String" -> String::class.asClassName()
    "void" -> UNIT

    // custom extension types
    "Buffer" -> ClassName("io.neovim.types", "Buffer")
    "Window" -> ClassName("io.neovim.types", "Window")
    "Tabpage" -> ClassName("io.neovim.types", "Tabpage")

    else -> {
        if (startsWith("ArrayOf")) {
            val type = substringAfter("ArrayOf(").dropLast(1)
            List::class.asClassName().parameterizedBy(type.toTypeName())
        } else ClassName("io.neovim", this)
    }
}

fun NeovimApiCallable.typeOfParam(param: NeovimApiParameter): TypeName =
    fixedParamTypes[name]?.let { eventParams ->
        eventParams[param.name]
    } ?: param.type.toTypeName()

fun builderOf(event: NeovimApiEvent): (ClassName) -> TypeSpec.Builder =
    if (event.parameters.isEmpty()) TypeSpec.Companion::objectBuilder
    else TypeSpec.Companion::classBuilder

fun String.toCamelCase() = StringBuilder().also { builder ->
    split("_").forEachIndexed { index, part ->
        builder.append(
            if (index == 0) part
            else part.capitalize()
        )
    }
}.toString()

fun NeovimApiMetadata.formatGenerated(): String =
    "Generated from Neovim v${version.major}.${version.minor}.${version.patch}"

private fun createSuppressUnusedAnnotation() =
    AnnotationSpec.builder(Suppress::class).apply {
        addMember("%S", "unused")
    }.build()

fun TypeSpec.Builder.suppressUnusedWarnings() {
    addAnnotation(createSuppressUnusedAnnotation())
}

fun FileSpec.Builder.suppressUnusedWarnings() {
    addAnnotation(createSuppressUnusedAnnotation())
}
