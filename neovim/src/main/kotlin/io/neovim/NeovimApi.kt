package io.neovim

import io.neovim.impl.proxy
import io.neovim.types.NeovimApiInfo

/**
 * @author dhleong
 */
interface NeovimApi {

    @ApiMethod("nvim_get_current_line", since = 1)
    suspend fun getCurrentLine(): String

    @ApiMethod("nvim_get_api_info", since = 1)
    suspend fun getApiInfo(): NeovimApiInfo

    companion object {
        fun create(rpc: Rpc): NeovimApi = proxy(rpc)
    }
}