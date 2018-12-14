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

/**
 * Interface to "Window" Neovim type
 *
 * @author dhleong
 */
@Suppress("unused")
@ApiExtensionType(id = 1)
interface Window : NeovimObject {
    @ApiMethod("nvim_win_get_buf", since = 1)
    suspend fun getBuf(): Buffer

    @ApiMethod("nvim_win_get_cursor", since = 1)
    suspend fun getCursor(): IntPair

    @ApiMethod("nvim_win_set_cursor", since = 1)
    suspend fun setCursor(pos: IntPair)

    @ApiMethod("nvim_win_get_height", since = 1)
    suspend fun getHeight(): Long

    @ApiMethod("nvim_win_set_height", since = 1)
    suspend fun setHeight(height: Long)

    @ApiMethod("nvim_win_get_width", since = 1)
    suspend fun getWidth(): Long

    @ApiMethod("nvim_win_set_width", since = 1)
    suspend fun setWidth(width: Long)

    @ApiMethod("nvim_win_get_var", since = 1)
    suspend fun getVar(name: String): Any

    @ApiMethod("nvim_win_set_var", since = 1)
    suspend fun setVar(name: String, value: Any)

    @ApiMethod("nvim_win_del_var", since = 1)
    suspend fun delVar(name: String)

    @ApiMethod("nvim_win_get_option", since = 1)
    suspend fun getOption(name: String): Any

    @ApiMethod("nvim_win_set_option", since = 1)
    suspend fun setOption(name: String, value: Any)

    @ApiMethod("nvim_win_get_position", since = 1)
    suspend fun getPosition(): IntPair

    @ApiMethod("nvim_win_get_tabpage", since = 1)
    suspend fun getTabpage(): Tabpage

    @ApiMethod("nvim_win_get_number", since = 1)
    suspend fun getNumber(): Long

    @ApiMethod("nvim_win_is_valid", since = 1)
    suspend fun isValid(): Boolean

    companion object : NeovimObject.Factory {
        override fun create(rpc: Rpc, id: Long): Window = proxy(rpc, id)
    }
}
