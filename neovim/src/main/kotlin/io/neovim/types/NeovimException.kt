package io.neovim.types

/**
 * @author dhleong
 */
class NeovimException(
    message: String,
    val method: String,
    val args: List<Any>
) : RuntimeException("Invoking $method($args): $message")