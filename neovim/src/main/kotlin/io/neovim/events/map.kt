package io.neovim.events

import java.lang.Class
import kotlin.String
import kotlin.collections.MutableMap

/**
 * Creates a map of event name -> event type for use deserializing
 *
 * Generated from Neovim v0.3.4
 *
 * @author dhleong
 */
internal fun createEventTypesMap(): MutableMap<String, Class<out NeovimEvent>> {
    val map: MutableMap<String, Class<out NeovimEvent>> = mutableMapOf()
    map["bell"] = Bell::class.java
    map["busy_start"] = BusyStart::class.java
    map["busy_stop"] = BusyStop::class.java
    map["clear"] = Clear::class.java
    map["cmdline_block_append"] = CmdlineBlockAppend::class.java
    map["cmdline_block_hide"] = CmdlineBlockHide::class.java
    map["cmdline_block_show"] = CmdlineBlockShow::class.java
    map["cmdline_hide"] = CmdlineHide::class.java
    map["cmdline_pos"] = CmdlinePos::class.java
    map["cmdline_show"] = CmdlineShow::class.java
    map["cmdline_special_char"] = CmdlineSpecialChar::class.java
    map["cursor_goto"] = CursorGoto::class.java
    map["default_colors_set"] = DefaultColorsSet::class.java
    map["eol_clear"] = EolClear::class.java
    map["flush"] = Flush::class.java
    map["grid_clear"] = GridClear::class.java
    map["grid_cursor_goto"] = GridCursorGoto::class.java
    map["grid_line"] = GridLine::class.java
    map["grid_resize"] = GridResize::class.java
    map["grid_scroll"] = GridScroll::class.java
    map["highlight_set"] = HighlightSet::class.java
    map["hl_attr_define"] = HlAttrDefine::class.java
    map["mode_change"] = ModeChange::class.java
    map["mode_info_set"] = ModeInfoSet::class.java
    map["mouse_off"] = MouseOff::class.java
    map["mouse_on"] = MouseOn::class.java
    map["option_set"] = OptionSet::class.java
    map["popupmenu_hide"] = PopupmenuHide::class.java
    map["popupmenu_select"] = PopupmenuSelect::class.java
    map["popupmenu_show"] = PopupmenuShow::class.java
    map["put"] = Put::class.java
    map["resize"] = Resize::class.java
    map["scroll"] = Scroll::class.java
    map["set_icon"] = SetIcon::class.java
    map["set_scroll_region"] = SetScrollRegion::class.java
    map["set_title"] = SetTitle::class.java
    map["suspend"] = Suspend::class.java
    map["tabline_update"] = TablineUpdate::class.java
    map["update_bg"] = UpdateBg::class.java
    map["update_fg"] = UpdateFg::class.java
    map["update_menu"] = UpdateMenu::class.java
    map["update_sp"] = UpdateSp::class.java
    map["visual_bell"] = VisualBell::class.java
    map["wildmenu_hide"] = WildmenuHide::class.java
    map["wildmenu_select"] = WildmenuSelect::class.java
    map["wildmenu_show"] = WildmenuShow::class.java
    map["nvim_buf_lines_event"] = BufLinesEvent::class.java
    map["nvim_buf_changedtick_event"] = BufChangedtickEvent::class.java
    map["nvim_buf_detach_event"] = BufDetachEvent::class.java
    return map
}
