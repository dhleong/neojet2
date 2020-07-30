package io.neovim

import io.neovim.events.NeovimEvent
import io.neovim.impl.proxy
import io.neovim.types.Buffer
import io.neovim.types.NeovimApiInfo
import io.neovim.types.Tabpage
import io.neovim.types.Window
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Neovim functional interface
 *
 * Generated from Neovim v0.4.3
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

    @ApiMethod("buffer_get_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferGetLine(buffer: Buffer, index: Long): String

    @ApiMethod("buffer_set_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferSetLine(
        buffer: Buffer,
        index: Long,
        line: String
    )

    @ApiMethod("buffer_del_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferDelLine(buffer: Buffer, index: Long)

    @ApiMethod("buffer_get_line_slice", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferGetLineSlice(
        buffer: Buffer,
        start: Long,
        end: Long,
        includeStart: Boolean,
        includeEnd: Boolean
    ): List<String>

    @ApiMethod("buffer_set_line_slice", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferSetLineSlice(
        buffer: Buffer,
        start: Long,
        end: Long,
        includeStart: Boolean,
        includeEnd: Boolean,
        replacement: List<String>
    )

    @ApiMethod("buffer_set_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferSetVar(
        buffer: Buffer,
        name: String,
        value: Any
    ): Any

    @ApiMethod("buffer_del_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferDelVar(buffer: Buffer, name: String): Any

    @ApiMethod("buffer_insert", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun bufferInsert(
        buffer: Buffer,
        lnum: Long,
        lines: List<String>
    )

    @ApiMethod("nvim_ui_attach", since = 1)
    suspend fun uiAttach(
        width: Long,
        height: Long,
        options: Map<String, Any>
    )

    @ApiMethod("ui_attach", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun uiAttach(
        width: Long,
        height: Long,
        enableRgb: Boolean
    )

    @ApiMethod("nvim_ui_detach", since = 1)
    suspend fun uiDetach()

    @ApiMethod("nvim_ui_try_resize", since = 1)
    suspend fun uiTryResize(width: Long, height: Long)

    @ApiMethod("nvim_ui_set_option", since = 1)
    suspend fun uiSetOption(name: String, value: Any)

    @ApiMethod("nvim_ui_try_resize_grid", since = 6)
    suspend fun uiTryResizeGrid(
        grid: Long,
        width: Long,
        height: Long
    )

    @ApiMethod("nvim_ui_pum_set_height", since = 6)
    suspend fun uiPumSetHeight(height: Long)

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

    @ApiMethod("nvim_input_mouse", since = 6)
    suspend fun inputMouse(
        button: String,
        action: String,
        modifier: String,
        grid: Long,
        row: Long,
        col: Long
    )

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

    @ApiMethod("vim_set_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetVar(name: String, value: Any): Any

    @ApiMethod("vim_del_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimDelVar(name: String): Any

    @ApiMethod("nvim_get_vvar", since = 1)
    suspend fun getVvar(name: String): Any

    @ApiMethod("nvim_set_vvar", since = 6)
    suspend fun setVvar(name: String, value: Any)

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

    @ApiMethod("nvim_create_buf", since = 6)
    suspend fun createBuf(listed: Boolean, scratch: Boolean): Buffer

    @ApiMethod("nvim_open_win", since = 6)
    suspend fun openWin(
        buffer: Buffer,
        enter: Boolean,
        config: Map<String, Any>
    ): Window

    @ApiMethod("nvim_list_tabpages", since = 1)
    suspend fun listTabpages(): List<Tabpage>

    @ApiMethod("nvim_get_current_tabpage", since = 1)
    suspend fun getCurrentTabpage(): Tabpage

    @ApiMethod("nvim_set_current_tabpage", since = 1)
    suspend fun setCurrentTabpage(tabpage: Tabpage)

    @ApiMethod("nvim_create_namespace", since = 5)
    suspend fun createNamespace(name: String): Long

    @ApiMethod("nvim_get_namespaces", since = 5)
    suspend fun getNamespaces(): Map<String, Any>

    @ApiMethod("nvim_paste", since = 6)
    suspend fun paste(
        data: String,
        crlf: Boolean,
        phase: Long
    ): Boolean

    @ApiMethod("nvim_put", since = 6)
    suspend fun put(
        lines: List<String>,
        type: String,
        after: Boolean,
        follow: Boolean
    )

    @ApiMethod("nvim_subscribe", since = 1)
    suspend fun subscribe(event: String)

    @ApiMethod("nvim_unsubscribe", since = 1)
    suspend fun unsubscribe(event: String)

    @ApiMethod("nvim_get_color_by_name", since = 1)
    suspend fun getColorByName(name: String): Long

    @ApiMethod("nvim_get_color_map", since = 1)
    suspend fun getColorMap(): Map<String, Any>

    @ApiMethod("nvim_get_context", since = 6)
    suspend fun getContext(opts: Map<String, Any>): Map<String, Any>

    @ApiMethod("nvim_load_context", since = 6)
    suspend fun loadContext(dict: Map<String, Any>): Any

    @ApiMethod("nvim_get_mode", since = 2)
    suspend fun getMode(): Map<String, Any>

    @ApiMethod("nvim_get_keymap", since = 3)
    suspend fun getKeymap(mode: String): List<Map<String, Any>>

    @ApiMethod("nvim_set_keymap", since = 6)
    suspend fun setKeymap(
        mode: String,
        lhs: String,
        rhs: String,
        opts: Map<String, Any>
    )

    @ApiMethod("nvim_del_keymap", since = 6)
    suspend fun delKeymap(mode: String, lhs: String)

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

    @ApiMethod("nvim_select_popupmenu_item", since = 6)
    suspend fun selectPopupmenuItem(
        item: Long,
        insert: Boolean,
        finish: Boolean,
        opts: Map<String, Any>
    )

    @ApiMethod("window_set_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun windowSetVar(
        window: Window,
        name: String,
        value: Any
    ): Any

    @ApiMethod("window_del_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun windowDelVar(window: Window, name: String): Any

    @ApiMethod("vim_command", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimCommand(command: String)

    @ApiMethod("vim_feedkeys", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimFeedkeys(
        keys: String,
        mode: String,
        escapeCsi: Boolean
    )

    @ApiMethod("vim_input", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimInput(keys: String): Long

    @ApiMethod("vim_replace_termcodes", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimReplaceTermcodes(
        str: String,
        fromPart: Boolean,
        doLt: Boolean,
        special: Boolean
    ): String

    @ApiMethod("vim_command_output", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimCommandOutput(command: String): String

    @ApiMethod("vim_eval", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimEval(expr: String): Any

    @ApiMethod("vim_call_function", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimCallFunction(fn: String, args: List<Any>): Any

    @ApiMethod("vim_strwidth", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimStrwidth(text: String): Long

    @ApiMethod("vim_list_runtime_paths", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimListRuntimePaths(): List<String>

    @ApiMethod("vim_change_directory", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimChangeDirectory(dir: String)

    @ApiMethod("vim_get_current_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetCurrentLine(): String

    @ApiMethod("vim_set_current_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetCurrentLine(line: String)

    @ApiMethod("vim_del_current_line", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimDelCurrentLine()

    @ApiMethod("vim_get_var", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetVar(name: String): Any

    @ApiMethod("vim_get_vvar", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetVvar(name: String): Any

    @ApiMethod("vim_get_option", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetOption(name: String): Any

    @ApiMethod("vim_set_option", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetOption(name: String, value: Any)

    @ApiMethod("vim_out_write", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimOutWrite(str: String)

    @ApiMethod("vim_err_write", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimErrWrite(str: String)

    @ApiMethod("vim_report_error", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimReportError(str: String)

    @ApiMethod("vim_get_buffers", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetBuffers(): List<Buffer>

    @ApiMethod("vim_get_current_buffer", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetCurrentBuffer(): Buffer

    @ApiMethod("vim_set_current_buffer", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetCurrentBuffer(buffer: Buffer)

    @ApiMethod("vim_get_windows", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetWindows(): List<Window>

    @ApiMethod("vim_get_current_window", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetCurrentWindow(): Window

    @ApiMethod("vim_set_current_window", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetCurrentWindow(window: Window)

    @ApiMethod("vim_get_tabpages", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetTabpages(): List<Tabpage>

    @ApiMethod("vim_get_current_tabpage", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetCurrentTabpage(): Tabpage

    @ApiMethod("vim_set_current_tabpage", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSetCurrentTabpage(tabpage: Tabpage)

    @ApiMethod("vim_subscribe", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimSubscribe(event: String)

    @ApiMethod("vim_unsubscribe", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimUnsubscribe(event: String)

    @ApiMethod("vim_name_to_color", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimNameToColor(name: String): Long

    @ApiMethod("vim_get_color_map", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetColorMap(): Map<String, Any>

    @ApiMethod("vim_get_api_info", since = 0, deprecatedSince = 1)
    @Deprecated("Deprecated since 1")
    suspend fun vimGetApiInfo(): List<Any>

    companion object {
        fun create(rpc: Rpc): NeovimApi = proxy(rpc)
    }
}
