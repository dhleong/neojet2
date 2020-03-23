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
import kotlin.collections.Map

/**
 * Interface to "Buffer" Neovim type
 *
 * Generated from Neovim v0.4.3
 *
 * @author dhleong
 */
@Suppress("unused")
@ApiExtensionType(id = 0)
interface Buffer : NeovimObject {
    @ApiMethod("nvim_buf_line_count", since = 1)
    suspend fun lineCount(): Long

    @ApiMethod("nvim_buf_attach", since = 4)
    suspend fun attach(sendBuffer: Boolean, opts: Map<String, Any>): Boolean

    @ApiMethod("nvim_buf_detach", since = 4)
    suspend fun detach(): Boolean

    @ApiMethod("nvim_buf_get_lines", since = 1)
    suspend fun getLines(
        start: Long,
        end: Long,
        strictIndexing: Boolean
    ): List<String>

    @ApiMethod("nvim_buf_set_lines", since = 1)
    suspend fun setLines(
        start: Long,
        end: Long,
        strictIndexing: Boolean,
        replacement: List<String>
    )

    @ApiMethod("nvim_buf_get_offset", since = 5)
    suspend fun getOffset(index: Long): Long

    @ApiMethod("nvim_buf_get_var", since = 1)
    suspend fun getVar(name: String): Any

    @ApiMethod("nvim_buf_get_changedtick", since = 2)
    suspend fun getChangedtick(): Long

    @ApiMethod("nvim_buf_get_keymap", since = 3)
    suspend fun getKeymap(mode: String): List<Map<String, Any>>

    @ApiMethod("nvim_buf_set_keymap", since = 6)
    suspend fun setKeymap(
        mode: String,
        lhs: String,
        rhs: String,
        opts: Map<String, Any>
    )

    @ApiMethod("nvim_buf_del_keymap", since = 6)
    suspend fun delKeymap(mode: String, lhs: String)

    @ApiMethod("nvim_buf_get_commands", since = 4)
    suspend fun getCommands(opts: Map<String, Any>): Map<String, Any>

    @ApiMethod("nvim_buf_set_var", since = 1)
    suspend fun setVar(name: String, value: Any)

    @ApiMethod("nvim_buf_del_var", since = 1)
    suspend fun delVar(name: String)

    @ApiMethod("nvim_buf_get_option", since = 1)
    suspend fun getOption(name: String): Any

    @ApiMethod("nvim_buf_set_option", since = 1)
    suspend fun setOption(name: String, value: Any)

    @ApiMethod("nvim_buf_get_number", since = 1)
    suspend fun getNumber(): Long

    @ApiMethod("nvim_buf_get_name", since = 1)
    suspend fun getName(): String

    @ApiMethod("nvim_buf_set_name", since = 1)
    suspend fun setName(name: String)

    @ApiMethod("nvim_buf_is_loaded", since = 5)
    suspend fun isLoaded(): Boolean

    @ApiMethod("nvim_buf_is_valid", since = 1)
    suspend fun isValid(): Boolean

    @ApiMethod("nvim_buf_get_mark", since = 1)
    suspend fun getMark(name: String): IntPair

    @ApiMethod("nvim_buf_add_highlight", since = 1)
    suspend fun addHighlight(
        nsId: Long,
        hlGroup: String,
        line: Long,
        colStart: Long,
        colEnd: Long
    ): Long

    @ApiMethod("nvim_buf_clear_namespace", since = 5)
    suspend fun clearNamespace(
        nsId: Long,
        lineStart: Long,
        lineEnd: Long
    )

    @ApiMethod("nvim_buf_clear_highlight", since = 1)
    suspend fun clearHighlight(
        nsId: Long,
        lineStart: Long,
        lineEnd: Long
    )

    @ApiMethod("nvim_buf_set_virtual_text", since = 5)
    suspend fun setVirtualText(
        nsId: Long,
        line: Long,
        chunks: List<Any>,
        opts: Map<String, Any>
    ): Long

    companion object : NeovimObject.Factory {
        override fun create(rpc: Rpc, id: Long): Buffer = proxy(rpc, id)
    }
}
