package io.neovim.types

import io.neovim.ApiExtensionType
import io.neovim.ApiMethod
import io.neovim.Rpc
import io.neovim.impl.proxy
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

/**
 * Interface to "Tabpage" Neovim type
 *
 * @author dhleong
 */
@Suppress("unused")
@ApiExtensionType(id = 2)
interface Tabpage : NeovimObject {
    @ApiMethod("nvim_tabpage_list_wins", since = 1)
    suspend fun listWins(): List<Window>

    @ApiMethod("nvim_tabpage_get_var", since = 1)
    suspend fun getVar(name: String): Any

    @ApiMethod("nvim_tabpage_set_var", since = 1)
    suspend fun setVar(name: String, value: Any)

    @ApiMethod("nvim_tabpage_del_var", since = 1)
    suspend fun delVar(name: String)

    @ApiMethod("nvim_tabpage_get_win", since = 1)
    suspend fun getWin(): Window

    @ApiMethod("nvim_tabpage_get_number", since = 1)
    suspend fun getNumber(): Long

    @ApiMethod("nvim_tabpage_is_valid", since = 1)
    suspend fun isValid(): Boolean

    companion object : NeovimObject.Factory {
        override fun create(rpc: Rpc, id: Long): Tabpage = proxy(rpc, id)
    }
}
