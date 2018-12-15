package io.neovim.apibuilder

import com.fasterxml.jackson.annotation.JsonFormat
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import io.neovim.rpc.Packet
import io.neovim.types.NeovimApiEvent

/**
 * @author dhleong
 */
fun createBaseEventType(name: ClassName) = TypeSpec.classBuilder(name).apply {
    addModifiers(KModifier.SEALED)
    addAnnotation(AnnotationSpec.builder(JsonFormat::class).apply {
        addMember("shape = JsonFormat.Shape.ARRAY")
    }.build())
    addSuperinterface(Packet::class)

    addProperty(PropertySpec.builder("type", Packet.Type::class, KModifier.OVERRIDE).apply {
        initializer("Packet.Type.NOTIFICATION")
    }.build())
}.build()

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

