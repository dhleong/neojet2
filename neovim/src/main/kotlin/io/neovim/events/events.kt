// Sealed class implementations of Neovim event types, so you can
// use an exhaustive `when ()` on a bare instance.
//
// Generated from Neovim v0.3.1
//
// @author dhleong
//
package io.neovim.events

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.neovim.ApiMethod
import io.neovim.rpc.Packet
import io.neovim.types.Tabpage

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
sealed class NeovimEvent : Packet {
    @JsonIgnore
    final override val type: Packet.Type = Packet.Type.NOTIFICATION
}

/**
 * Redraw is an aggregate event that is not directly generated by the
 * API info since its format is non-standard
 */
data class Redraw(val events: List<NeovimEvent>) : NeovimEvent()

@ApiMethod("resize", since = 3)
data class Resize(val width: Long, val height: Long) : NeovimEvent()

@ApiMethod("clear", since = 3)
object Clear : NeovimEvent()

@ApiMethod("eol_clear", since = 3)
object EolClear : NeovimEvent()

@ApiMethod("cursor_goto", since = 3)
data class CursorGoto(val row: Long, val col: Long) : NeovimEvent()

@ApiMethod("mode_info_set", since = 3)
data class ModeInfoSet(val enabled: Boolean, val cursor_styles: List<Any>) : NeovimEvent()

@ApiMethod("update_menu", since = 3)
object UpdateMenu : NeovimEvent()

@ApiMethod("busy_start", since = 3)
object BusyStart : NeovimEvent()

@ApiMethod("busy_stop", since = 3)
object BusyStop : NeovimEvent()

@ApiMethod("mouse_on", since = 3)
object MouseOn : NeovimEvent()

@ApiMethod("mouse_off", since = 3)
object MouseOff : NeovimEvent()

@ApiMethod("mode_change", since = 3)
data class ModeChange(val mode: String, val mode_idx: Long) : NeovimEvent()

@ApiMethod("set_scroll_region", since = 3)
data class SetScrollRegion(
    val top: Long,
    val bot: Long,
    val left: Long,
    val right: Long
) : NeovimEvent()

@ApiMethod("scroll", since = 3)
data class Scroll(val count: Long) : NeovimEvent()

@ApiMethod("highlight_set", since = 3)
data class HighlightSet(val attrs: Map<String, Any>) : NeovimEvent()

@ApiMethod("put", since = 3)
data class Put(val str: String) : NeovimEvent()

@ApiMethod("bell", since = 3)
object Bell : NeovimEvent()

@ApiMethod("visual_bell", since = 3)
object VisualBell : NeovimEvent()

@ApiMethod("flush", since = 3)
object Flush : NeovimEvent()

@ApiMethod("update_fg", since = 3)
data class UpdateFg(val fg: Long) : NeovimEvent()

@ApiMethod("update_bg", since = 3)
data class UpdateBg(val bg: Long) : NeovimEvent()

@ApiMethod("update_sp", since = 3)
data class UpdateSp(val sp: Long) : NeovimEvent()

@ApiMethod("default_colors_set", since = 4)
data class DefaultColorsSet(
    val rgb_fg: Long,
    val rgb_bg: Long,
    val rgb_sp: Long,
    val cterm_fg: Long,
    val cterm_bg: Long
) : NeovimEvent()

@ApiMethod("suspend", since = 3)
object Suspend : NeovimEvent()

@ApiMethod("set_title", since = 3)
data class SetTitle(val title: String) : NeovimEvent()

@ApiMethod("set_icon", since = 3)
data class SetIcon(val icon: String) : NeovimEvent()

@ApiMethod("option_set", since = 4)
data class OptionSet(val name: String, val value: Any) : NeovimEvent()

@ApiMethod("popupmenu_show", since = 3)
data class PopupmenuShow(
    val items: List<Any>,
    val selected: Long,
    val row: Long,
    val col: Long
) : NeovimEvent()

@ApiMethod("popupmenu_hide", since = 3)
object PopupmenuHide : NeovimEvent()

@ApiMethod("popupmenu_select", since = 3)
data class PopupmenuSelect(val selected: Long) : NeovimEvent()

@ApiMethod("tabline_update", since = 3)
data class TablineUpdate(val current: Tabpage, val tabs: List<Any>) : NeovimEvent()

@ApiMethod("cmdline_show", since = 3)
data class CmdlineShow(
    val content: List<Any>,
    val pos: Long,
    val firstc: String,
    val prompt: String,
    val indent: Long,
    val level: Long
) : NeovimEvent()

@ApiMethod("cmdline_pos", since = 3)
data class CmdlinePos(val pos: Long, val level: Long) : NeovimEvent()

@ApiMethod("cmdline_special_char", since = 3)
data class CmdlineSpecialChar(
    val c: String,
    val shift: Boolean,
    val level: Long
) : NeovimEvent()

@ApiMethod("cmdline_hide", since = 3)
data class CmdlineHide(val level: Long) : NeovimEvent()

@ApiMethod("cmdline_block_show", since = 3)
data class CmdlineBlockShow(val lines: List<Any>) : NeovimEvent()

@ApiMethod("cmdline_block_append", since = 3)
data class CmdlineBlockAppend(val lines: List<Any>) : NeovimEvent()

@ApiMethod("cmdline_block_hide", since = 3)
object CmdlineBlockHide : NeovimEvent()

@ApiMethod("wildmenu_show", since = 3)
data class WildmenuShow(val items: List<Any>) : NeovimEvent()

@ApiMethod("wildmenu_select", since = 3)
data class WildmenuSelect(val selected: Long) : NeovimEvent()

@ApiMethod("wildmenu_hide", since = 3)
object WildmenuHide : NeovimEvent()
