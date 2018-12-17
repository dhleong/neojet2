package io.neovim.events.params

import com.fasterxml.jackson.annotation.JsonFormat

/**
 * @author dhleong
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class ContentInfo(
    val attrs: HighlightAttrs,
    val content: String
)