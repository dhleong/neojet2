package io.neovim.apibuilder

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import io.neovim.events.params.*
import kotlin.reflect.KClass

/**
 * @author dhleong
 */
val fixedParamTypes = mapOf(
    "cmdline_show" to mapOf(
        "content" to listOfType(ContentInfo::class)
    ),
    "cmdline_block_show" to mapOf(
        "lines" to listOfType(ContentInfo::class)
    ),
    "cmdline_block_append" to mapOf(
        "line" to ContentInfo::class.asClassName(),
        "lines" to listOfType(ContentInfo::class)
    ),

    "highlight_set" to mapOf(
        "attrs" to HighlightAttrs::class.asClassName()
    ),

    "mode_info_set" to mapOf(
        "cursor_styles" to listOfType(ModeInfo::class)
    ),

    "popupmenu_show" to mapOf(
        "items" to listOfType(CompletionItem::class)
    ),

    "tabline_update" to mapOf(
        "tabs" to listOfType(TabInfo::class)
    ),

    "wildmenu_show" to mapOf(
        "items" to listOfType(CompletionItem::class)
    )
)

private fun listOfType(type: KClass<*>) = List::class.asClassName().parameterizedBy(
    type.asClassName()
)