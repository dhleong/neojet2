package io.neovim

/**
 * @author dhleong
 */
interface NeovimApi {

    @ApiMethod("nvim_get_current_line", since = 1)
    suspend fun getCurrentLine(): String

    companion object {
        fun create(rpc: Rpc): NeovimApi = proxy(rpc)
    }
}