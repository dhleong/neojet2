package org.neojet.util

import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
private val specialKeyChars = mapOf(
    '\u238b' to "ESC",
    '\u007F' to "BS",
    '\n' to "CR",
    '<' to "LT",
    '>' to "GT"
)

private val specialKeyCodes = mapOf(
    8 to "BS",
    10 to "CR",
    27 to "ESC",
    37 to "LEFT",
    38 to "UP",
    39 to "RIGHT",
    40 to "DOWN"
)

fun KeyInfo.toVimCode(): String {
    val builder = StringBuilder(8)

    if (keyCode != KeyEvent.VK_UNDEFINED) {
        // don't add modifiers to keyChar-based strokes (?)
        if ((modifiers and KeyEvent.META_MASK) != 0) {
            builder.append("m-")
        }
        if ((modifiers and KeyEvent.CTRL_MASK) != 0) {
            builder.append("c-")
        }
        if ((modifiers and KeyEvent.ALT_MASK) != 0) {
            builder.append("a-")
        }
        if ((modifiers and KeyEvent.SHIFT_MASK) != 0) {
            builder.append("s-")
        }
    }

    val base = specialKeyChars[keyChar]
        ?: specialKeyCodes[keyCode]
        ?: if (keyChar != KeyEvent.CHAR_UNDEFINED) keyChar.toString()
            else null
        ?: keyCode.codeToVkKeyName()
//        ?: if (keyCode.)
        ?: ""

    return when {
        builder.isEmpty() && base.length == 1 -> base

        builder.isEmpty() -> "<$base>"

        else -> "<$builder$base>"
    }
}

data class KeyInfo(
    val keyCode: Int = KeyEvent.VK_UNDEFINED,
    val keyChar: Char = KeyEvent.CHAR_UNDEFINED,
    val modifiers: Int = 0
)

fun KeyEvent.toVimCode() = toKeyInfo().toVimCode()

fun KeyEvent.toKeyInfo(): KeyInfo = KeyInfo(
    keyCode = keyCode,
    keyChar = keyChar,
    modifiers = modifiers
)

fun KeyStroke.toEventFor(component: JComponent) = KeyEvent(
    component,
    0, 0,
    modifiers, keyCode, keyChar
)

private val vkKeyCodeToNameMap by lazy {
    // this is apparently how the AWTKeyStroke handles its toString
    KeyEvent::class.java.declaredFields
        .asSequence()
        .filter { it.name.startsWith("VK_") && it.type == Int::class.java }
        .fold(mutableMapOf<Int, String>()) { m, keyField ->
            val keyCode = keyField.getInt(null)
            m.also {
                m[keyCode] = keyField.name.substring("VK_".length).toLowerCase()
            }
        }
}

private fun Int.codeToVkKeyName(): String? =
    if (this != KeyEvent.VK_UNDEFINED) {
        vkKeyCodeToNameMap[this]
    } else null

