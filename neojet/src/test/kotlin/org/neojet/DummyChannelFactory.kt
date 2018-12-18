package org.neojet

import io.neovim.rpc.NeovimChannel
import java.io.InputStream
import java.io.OutputStream

class DummyChannel : NeovimChannel {
    class Factory : NeovimChannel.Factory {
        override fun create(): NeovimChannel = DummyChannel()
    }

    override fun getInputStream(): InputStream = object : InputStream() {
        override fun read(): Int = -1
    }

    override fun getOutputStream(): OutputStream = object : OutputStream() {
        override fun write(b: Int) { }
    }

    override fun close() { }

}
