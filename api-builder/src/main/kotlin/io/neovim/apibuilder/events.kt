package io.neovim.apibuilder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import io.neovim.types.NeovimApiEvent

/**
 * @author dhleong
 */
fun createEvent(
    baseClass: ClassName,
    event: NeovimApiEvent
) = builderOf(event)(ClassName(
    "io.neovim.events",
    event.name.toCamelCase().capitalize()
)).apply {

    superclass(baseClass)
    addAnnotation(event.createApiMethodAnnotation())

    if (event.parameters.isNotEmpty()) {
        // data classes must have at least one parameter
        addModifiers(KModifier.DATA)

        primaryConstructor(FunSpec.constructorBuilder().apply {
            for (param in event.parameters) {
                addParameter(param.name, param.type.toTypeName())
                addProperty(PropertySpec.builder(param.name, param.type.toTypeName()).apply {
                    initializer(param.name)
                }.build())
            }
        }.build())
    }

}.build()

fun builderOf(event: NeovimApiEvent): (ClassName) -> TypeSpec.Builder =
    if (event.parameters.isEmpty()) TypeSpec.Companion::objectBuilder
    else TypeSpec.Companion::classBuilder

