package io.neovim.types

/**
 * @author dhleong
 */
class IncompatibleApiException(
    val method: String,
    reason: String
) : RuntimeException("Unable to invoke $method: $reason")
