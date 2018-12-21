package org.neojet.nvim

import io.neovim.NeovimApi
import org.neojet.util.toVimCode
import java.awt.event.KeyEvent

suspend fun NeovimApi.input(key: KeyEvent) {
    val code = key.toVimCode()
    println("input($code)")
    input(code)
}