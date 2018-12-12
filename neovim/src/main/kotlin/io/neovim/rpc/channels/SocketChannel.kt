package io.neovim.rpc.channels

import io.neovim.rpc.NeovimChannel
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * @author dhleong
 */
class SocketChannel(
    private val socket: Socket
) : NeovimChannel {

    override fun getInputStream(): InputStream = socket.getInputStream()

    override fun getOutputStream(): OutputStream = socket.getOutputStream()

    override fun close() {
        socket.close()
    }

    class Factory(
        private val address: InetSocketAddress
    ) : NeovimChannel.Factory {

        constructor(host: String, port: Int) : this(InetSocketAddress(host, port))

        override fun create(): NeovimChannel = SocketChannel(
            Socket(address.address, address.port)
        )
    }
}