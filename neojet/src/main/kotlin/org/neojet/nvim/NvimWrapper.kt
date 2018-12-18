package org.neojet.nvim

import io.neovim.NeovimApi
import io.neovim.Rpc
import io.neovim.rpc.NeovimChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * @author dhleong
 */

class NvimWrapper(
    private val channelFactory: NeovimChannel.Factory
) {

    @Suppress("EXPERIMENTAL_API_USAGE")
    val instance: NeovimApi
        get() = myInstance ?: open()

    private var myInstance: NeovimApi? = null
    private var myRpc: Rpc? = null

    val isConnected: Boolean
        get() = myRpc != null

    @ExperimentalCoroutinesApi
    private fun open(): NeovimApi {
        if (myInstance != null) throw IllegalStateException()

        val rpc = createRpc(channelFactory).also {
            myRpc = it
        }
        return NeovimApi.create(rpc).also {
            myInstance = it
        }
    }

    fun close() {
        myRpc?.close()
        myRpc = null
        myInstance = null
    }

    operator fun invoke(): NeovimApi = instance

    private fun createRpc(channelFactory: NeovimChannel.Factory) =
        rpcFactory?.invoke(channelFactory)
            ?: Rpc(channelFactory.create())

    companion object {
        /**
         * For injecting a custom [Rpc] factory in tests
         */
        var rpcFactory: ((NeovimChannel.Factory) -> Rpc)? = null
    }
}