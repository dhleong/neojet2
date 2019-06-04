package org.neojet.util

import assertk.Assert
import assertk.assertThat
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class KeysTest {
    @Test fun `Simple keys`() {
        assertThat("C").hasVimCode("c")
        assertThat("1").hasVimCode("1")
    }

    @Test fun `Keys with Modifiers`() {
        assertThat("control C").hasVimCode("<c-c>")
        assertThat("alt C").hasVimCode("<a-c>")
        assertThat("meta C").hasVimCode("<m-c>")
        assertThat("control shift C").hasVimCode("<c-s-c>")
        assertThat("alt shift C").hasVimCode("<a-s-c>")
        assertThat("control alt C").hasVimCode("<c-a-c>")
        assertThat("control alt shift C").hasVimCode("<c-a-s-c>")
        assertThat("meta control alt shift C").hasVimCode("<m-c-a-s-c>")
    }

    @Test fun `Pressed keys with Modifiers`() {
        val ctrlAEvent = keyPressedEvent(65, modifiers = KeyEvent.CTRL_DOWN_MASK)
        assertThat(ctrlAEvent).hasVimCode("<c-a>")
    }

    @Test fun `Symbol keys`() {
        val bracketEvent = keyPressedEvent('{', modifiers = KeyEvent.SHIFT_DOWN_MASK)
        assertThat(bracketEvent).hasVimCode("{")

        val parenEvent = keyPressedEvent('(', modifiers = KeyEvent.SHIFT_DOWN_MASK)
        assertThat(parenEvent).hasVimCode("(")
    }

    @Test fun `Special Keys`() {
        assertThat('<').hasVimCode("<lt>")
        assertThat('>').hasVimCode("<gt>")

        assertThat("UP").hasVimCode("<up>")
        assertThat("DOWN").hasVimCode("<down>")
        assertThat("LEFT").hasVimCode("<left>")
        assertThat("RIGHT").hasVimCode("<right>")

        assertThat("ENTER").hasVimCode("<cr>")
    }

    @Test fun `Backspace works`() {
        assertThat("BACK_SPACE").hasVimCode("<bs>")
        assertThat('\u007F').hasVimCode("<bs>")
    }
}

private fun Assert<Any>.hasVimCode(expectedVimCode: String) = given { actual ->
    val stroke = when (val it = actual) {
        is Char -> stroke(it)
        is String -> stroke(it)
        is KeyStroke -> it
        is KeyEvent -> it.toKeyStroke()
        else -> throw IllegalArgumentException()
    }
    val actualCode = stroke.toVimCode()
    if (actualCode.toLowerCase() == expectedVimCode.toLowerCase()) return
    expected("${show(stroke)} (${show(stroke.keyCode)}) to have vim code `$expectedVimCode` but was `$actualCode`")
}

private fun stroke(char: Char) = KeyStroke.getKeyStroke(char)
private fun stroke(string: String) = KeyStroke.getKeyStroke(string)

private fun keyPressedEvent(keyChar: Char, modifiers: Int): Any {
    val source = mock<Component> {  }
    return KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, modifiers, KeyEvent.VK_UNDEFINED, keyChar)
}

private fun keyPressedEvent(keyCode: Int, modifiers: Int): Any {
    val source = mock<Component> {  }
    return KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, modifiers, keyCode, 1.toChar())
}
