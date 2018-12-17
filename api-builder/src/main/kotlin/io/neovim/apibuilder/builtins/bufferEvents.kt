package io.neovim.apibuilder.builtins

import io.neovim.events.BufChangedtickEvent
import io.neovim.events.BufDetachEvent
import io.neovim.events.BufLinesEvent

/**
 * @author dhleong
 */
val bufferEventTypes = listOf(
    BufLinesEvent::class,
    BufChangedtickEvent::class,
    BufDetachEvent::class
)
