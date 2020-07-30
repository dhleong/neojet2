package io.neovim.apibuilder

import com.fasterxml.jackson.annotation.JsonFormat
import com.squareup.kotlinpoet.*
import io.neovim.rpc.Packet
import io.neovim.types.NeovimApiEvent

/**
 * @author dhleong
 */
fun createBaseEventType(name: ClassName) = TypeSpec.classBuilder(name).apply {
    addModifiers(KModifier.ABSTRACT)
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

    if (event.isDeprecated) {
        addAnnotation(event.createDeprecatedAnnotation())
    }

    if (event.parameters.isNotEmpty()) {
        // data classes must have at least one parameter
        addModifiers(KModifier.DATA)

        primaryConstructor(FunSpec.constructorBuilder().apply {
            for (param in event.parameters) {
                val paramName = param.name.toCamelCase()
                val paramType = event.typeOfParam(param)
                addParameter(paramName, paramType)
                addProperty(PropertySpec.builder(paramName, paramType).apply {
                    initializer(paramName)
                }.build())
            }
        }.build())
    }

}.build()

