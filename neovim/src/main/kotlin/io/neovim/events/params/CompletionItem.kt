package io.neovim.events.params

import com.fasterxml.jackson.annotation.JsonFormat

/**
 * @author dhleong
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class CompletionItem(
    val word: String,
    val kind: String = "",
    val menu: String = "",
    val info: String = ""
)