package io.neovim.apibuilder

import com.squareup.kotlinpoet.*
import io.neovim.ApiMethod
import io.neovim.types.NeovimApiCallable
import io.neovim.types.NeovimApiFunction
import io.neovim.types.NeovimApiInfo

fun NeovimApiFunction.toFunSpec(
    prefix: String? = null
) = FunSpec.builder(formatName(prefix)).apply {
    addAnnotation(createApiMethodAnnotation())

    addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)

    this@toFunSpec.parameters.forEachIndexed { index, param ->
        if (index == 0 && isMethod) {
            // the first argument to a method is the instance itself;
            // that doesn't need to be part of our signature
            return@forEachIndexed
        }

        addParameter(
            ParameterSpec.builder(
                param.name.toCamelCase(),
                param.type.toTypeName()
            ).build()
        )
    }

    returns(
        resultTypeForFn(name)
            ?: returnType.toTypeName()
    )
}.build()

fun NeovimApiCallable.createApiMethodAnnotation() =
    AnnotationSpec.builder(ApiMethod::class).apply {
        addMember("%S, since = %L", name, since)
    }.build()

/**
 * The API info returns a lot of "Any"; sometimes we can provide a
 * specific response type for a given method
 */
private fun resultTypeForFn(name: String): TypeName? = when (name) {
    "nvim_get_api_info" -> NeovimApiInfo::class.asClassName()

    else -> null
}

private fun NeovimApiFunction.formatName(prefix: String?) =
    name.removePrefix(prefix ?: "nvim_")
        .toCamelCase()

