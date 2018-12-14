package io.neovim.types

import io.neovim.Rpc

/**
 * @author dhleong
 */
interface NeovimObject {
    val id: Long

    interface Factory {
        fun create(rpc: Rpc, id: Long): NeovimObject
    }
}