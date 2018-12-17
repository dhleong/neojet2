package io.neovim.events

import io.neovim.ApiMethod
import io.neovim.types.Buffer

/*
 * Buffer events are not currently provided by the API for some reason
 * @author dhleong
 */

@ApiMethod("nvim_buf_lines_event", since = 3)
data class BufLinesEvent(
    val buffer: Buffer,
    val changedtick: Long?,
    val firstline: Long,
    val lastline: Long,
    val linedata: List<String>,
    val more: Boolean
) : NeovimEvent()

@ApiMethod("nvim_buf_changedtick_event", since = 3)
data class BufChangedtickEvent(
    val buffer: Buffer,
    val changedtick: Long?
) : NeovimEvent()


@ApiMethod("nvim_buf_detach_event", since = 3)
data class BufDetachEvent(
    val buffer: Buffer
) : NeovimEvent()

