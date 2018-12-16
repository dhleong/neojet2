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

// aliases as extension properties to avoid incorrect serialization

val IntPair.width: Int
    get() = first

val IntPair.height: Int
    get() = second
