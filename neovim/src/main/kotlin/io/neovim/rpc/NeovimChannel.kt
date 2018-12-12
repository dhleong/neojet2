package io.neovim.rpc

import java.io.InputStream
import java.io.OutputStream

/**
 * @author dhleong
 */

interface NeovimChannel : AutoCloseable {
    interface Factory {
        fun create(): NeovimChannel
    }

    fun getInputStream(): InputStream
    fun getOutputStream(): OutputStream
}