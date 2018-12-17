package io.neovim.events.params

import io.neovim.types.Tabpage

/**
 * @author dhleong
 */
data class TabInfo(
    val name: String,
    val tab: Tabpage
)