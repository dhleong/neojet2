package io.neovim.events.params



/**
 * @author dhleong
 */
data class HighlightAttrs(
    val foreground: Int?,
    val background: Int?,
    val special: Int?,

    val reverse: Boolean?,
    val italic: Boolean?,
    val bold: Boolean?,
    val underline: Boolean?,
    val undercurl: Boolean?
) {

    /**
     * @return True if no values are provided
     */
    val isEmpty: Boolean
        get() = foreground == null
            && background == null
            && special == null
            && reverse == null
            && italic == null
            && bold == null
            && underline == null
            && undercurl == null

}