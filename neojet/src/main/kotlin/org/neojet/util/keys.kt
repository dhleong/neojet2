package org.neojet.util

import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
private val specialKeyChars = mapOf(
    '\u007F' to "<BS>",
    '\n' to "<CR>",
    '<' to "<LT>",
    '>' to "<GT>"
)

private val specialKeyCodes = mapOf(
    8 to "<BS>",
    10 to "<CR>",
    37 to "<LEFT>",
    38 to "<UP>",
    39 to "<RIGHT>",
    40 to "<DOWN>"
)

// TODO special keys? modifiers?
fun KeyStroke.toVimCode() = specialKeyChars[keyChar]
    ?: specialKeyCodes[keyCode]
    ?: keyChar.toString()

fun KeyEvent.toVimCode() = KeyStroke.getKeyStrokeForEvent(this).toVimCode()
