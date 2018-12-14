package io.neovim.types

import com.fasterxml.jackson.annotation.JsonFormat

/**
 * @author dhleong
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class IntPair(
    val first: Int,
    val second: Int
)