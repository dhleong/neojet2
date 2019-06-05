package org.neojet.nvim

import io.neovim.NeovimApi
import org.neojet.util.toKeyStroke
import org.neojet.util.toVimCode
import java.awt.event.KeyEvent

suspend fun NeovimApi.input(key: KeyEvent) {
    val code = key.toVimCode()
    if (code.isEmpty()) return

    println("input($code) <- (char#${key.keyChar.toInt()}) $key (stroke=${key.toKeyStroke()})")
    input(code)
}
