package io.neovim

import io.neovim.events.NeovimEvent
import io.neovim.impl.proxy
import io.neovim.types.Buffer
import io.neovim.types.NeovimApiInfo
import io.neovim.types.Tabpage
import io.neovim.types.Window

/**
 * Neovim functional interface
 *
 * Generated from Neovim v0.3.1
 *
 * @author dhleong
 */
@Suppress("unused")
interface NeovimApi {
    /**
     * Get the next Notification event received, such as [Redraw].
     *
     * Call [uiAttach] first to start receiving events.
     *
     * @return null when the connection is closed.
     */
    suspend fun nextEvent(): NeovimEvent?

    @ApiMethod("nvim_ui_attach", since = 1)
    suspend fun uiAttach(
        width: Long,
        height: Long,
        options: Map<String, Any>
    )

    @ApiMethod("nvim_ui_detach", since = 1)
    suspend fun uiDetach()

    @ApiMethod("nvim_ui_try_resize", since = 1)
    suspend fun uiTryResize(width: Long, height: Long)

    @ApiMethod("nvim_ui_set_option", since = 1)
    suspend fun uiSetOption(name: String, value: Any)

    @ApiMethod("nvim_command", since = 1)
    suspend fun command(command: String)

    @ApiMethod("nvim_get_hl_by_name", since = 3)
    suspend fun getHlByName(name: String, rgb: Boolean): Map<String, Any>

    @ApiMethod("nvim_get_hl_by_id", since = 3)
    suspend fun getHlById(hlId: Long, rgb: Boolean): Map<String, Any>

    @ApiMethod("nvim_feedkeys", since = 1)
    suspend fun feedkeys(
        keys: String,
        mode: String,
        escapeCsi: Boolean
    )

    @ApiMethod("nvim_input", since = 1)
    suspend fun input(keys: String): Long

    @ApiMethod("nvim_replace_termcodes", since = 1)
    suspend fun replaceTermcodes(
        str: String,
        fromPart: Boolean,
        doLt: Boolean,
        special: Boolean
    ): String

    @ApiMethod("nvim_command_output", since = 1)
    suspend fun commandOutput(command: String): String

    @ApiMethod("nvim_eval", since = 1)
    suspend fun eval(expr: String): Any

    @ApiMethod("nvim_execute_lua", since = 3)
    suspend fun executeLua(code: String, args: List<Any>): Any

    @ApiMethod("nvim_call_function", since = 1)
    suspend fun callFunction(fn: String, args: List<Any>): Any

    @ApiMethod("nvim_call_dict_function", since = 4)
    suspend fun callDictFunction(
        dict: Any,
        fn: String,
        args: List<Any>
    ): Any

    @ApiMethod("nvim_strwidth", since = 1)
    suspend fun strwidth(text: String): Long

    @ApiMethod("nvim_list_runtime_paths", since = 1)
    suspend fun listRuntimePaths(): List<String>

    @ApiMethod("nvim_set_current_dir", since = 1)
    suspend fun setCurrentDir(dir: String)

    @ApiMethod("nvim_get_current_line", since = 1)
    suspend fun getCurrentLine(): String

    @ApiMethod("nvim_set_current_line", since = 1)
    suspend fun setCurrentLine(line: String)

    @ApiMethod("nvim_del_current_line", since = 1)
    suspend fun delCurrentLine()

    @ApiMethod("nvim_get_var", since = 1)
    suspend fun getVar(name: String): Any

    @ApiMethod("nvim_set_var", since = 1)
    suspend fun setVar(name: String, value: Any)

    @ApiMethod("nvim_del_var", since = 1)
    suspend fun delVar(name: String)

    @ApiMethod("nvim_get_vvar", since = 1)
    suspend fun getVvar(name: String): Any

    @ApiMethod("nvim_get_option", since = 1)
    suspend fun getOption(name: String): Any

    @ApiMethod("nvim_set_option", since = 1)
    suspend fun setOption(name: String, value: Any)

    @ApiMethod("nvim_out_write", since = 1)
    suspend fun outWrite(str: String)

    @ApiMethod("nvim_err_write", since = 1)
    suspend fun errWrite(str: String)

    @ApiMethod("nvim_err_writeln", since = 1)
    suspend fun errWriteln(str: String)

    @ApiMethod("nvim_list_bufs", since = 1)
    suspend fun listBufs(): List<Buffer>

    @ApiMethod("nvim_get_current_buf", since = 1)
    suspend fun getCurrentBuf(): Buffer

    @ApiMethod("nvim_set_current_buf", since = 1)
    suspend fun setCurrentBuf(buffer: Buffer)

    @ApiMethod("nvim_list_wins", since = 1)
    suspend fun listWins(): List<Window>

    @ApiMethod("nvim_get_current_win", since = 1)
    suspend fun getCurrentWin(): Window

    @ApiMethod("nvim_set_current_win", since = 1)
    suspend fun setCurrentWin(window: Window)

    @ApiMethod("nvim_list_tabpages", since = 1)
    suspend fun listTabpages(): List<Tabpage>

    @ApiMethod("nvim_get_current_tabpage", since = 1)
    suspend fun getCurrentTabpage(): Tabpage

    @ApiMethod("nvim_set_current_tabpage", since = 1)
    suspend fun setCurrentTabpage(tabpage: Tabpage)

    @ApiMethod("nvim_subscribe", since = 1)
    suspend fun subscribe(event: String)

    @ApiMethod("nvim_unsubscribe", since = 1)
    suspend fun unsubscribe(event: String)

    @ApiMethod("nvim_get_color_by_name", since = 1)
    suspend fun getColorByName(name: String): Long

    @ApiMethod("nvim_get_color_map", since = 1)
    suspend fun getColorMap(): Map<String, Any>

    @ApiMethod("nvim_get_mode", since = 2)
    suspend fun getMode(): Map<String, Any>

    @ApiMethod("nvim_get_keymap", since = 3)
    suspend fun getKeymap(mode: String): List<Map<String, Any>>

    @ApiMethod("nvim_get_commands", since = 4)
    suspend fun getCommands(opts: Map<String, Any>): Map<String, Any>

    @ApiMethod("nvim_get_api_info", since = 1)
    suspend fun getApiInfo(): NeovimApiInfo

    @ApiMethod("nvim_set_client_info", since = 4)
    suspend fun setClientInfo(
        name: String,
        version: Map<String, Any>,
        type: String,
        methods: Map<String, Any>,
        attributes: Map<String, Any>
    )

    @ApiMethod("nvim_get_chan_info", since = 4)
    suspend fun getChanInfo(chan: Long): Map<String, Any>

    @ApiMethod("nvim_list_chans", since = 4)
    suspend fun listChans(): List<Any>

    @ApiMethod("nvim_call_atomic", since = 1)
    suspend fun callAtomic(calls: List<Any>): List<Any>

    @ApiMethod("nvim_parse_expression", since = 4)
    suspend fun parseExpression(
        expr: String,
        flags: String,
        highlight: Boolean
    ): Map<String, Any>

    @ApiMethod("nvim_list_uis", since = 4)
    suspend fun listUis(): List<Any>

    @ApiMethod("nvim_get_proc_children", since = 4)
    suspend fun getProcChildren(pid: Long): List<Any>

    @ApiMethod("nvim_get_proc", since = 4)
    suspend fun getProc(pid: Long): Any

    companion object {
        fun create(rpc: Rpc): NeovimApi = proxy(rpc)
    }
}
