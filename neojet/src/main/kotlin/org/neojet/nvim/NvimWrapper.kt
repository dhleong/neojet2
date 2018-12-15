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

    val rpc: Rpc
        get() = myRpc ?: open().let { myRpc!! }

    private var myInstance: NeovimApi? = null
    private var myRpc: Rpc? = null

    @ExperimentalCoroutinesApi
    private fun open(): NeovimApi {
        if (myInstance != null) throw IllegalStateException()

        val connection = channelFactory.create()

        val rpc = Rpc(connection).also {
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
}