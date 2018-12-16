package org.neojet.nvim

import io.neovim.NeovimApi
import java.awt.event.KeyEvent

private val specialKeyChars = mapOf(
    '\n' to "<CR>",
    '<' to "<LT>"
)

private val specialKeyCodes = mapOf(
    37 to "<LEFT>",
    38 to "<UP>",
    39 to "<RIGHT>",
    40 to "<DOWN>"
)

// TODO tests please
// TODO special keys? modifiers?
internal fun KeyEvent.toVimCode() = specialKeyChars[keyChar]
    ?: specialKeyCodes[keyCode]
    ?: keyChar.toString()

suspend fun NeovimApi.input(key: KeyEvent) {
    input(key.toVimCode())
}