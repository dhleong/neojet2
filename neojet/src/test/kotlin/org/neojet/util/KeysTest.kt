package org.neojet.util

import assertk.Assert
import assertk.assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.junit.Test
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class KeysTest {
    @Test fun `Simple keys`() {
        assert("C").hasVimCode("c")
        assert("1").hasVimCode("1")
    }

    @Test fun `Keys with Modifiers`() {
        assert("control C").hasVimCode("<c-c>")
        assert("alt C").hasVimCode("<a-c>")
        assert("meta C").hasVimCode("<m-c>")
        assert("control shift C").hasVimCode("<c-s-c>")
        assert("alt shift C").hasVimCode("<a-s-c>")
        assert("control alt C").hasVimCode("<c-a-c>")
        assert("control alt shift C").hasVimCode("<c-a-s-c>")
        assert("meta control alt shift C").hasVimCode("<m-c-a-s-c>")
    }

    @Test fun `Special Keys`() {
        assert('<').hasVimCode("<lt>")
        assert('>').hasVimCode("<gt>")

        assert("UP").hasVimCode("<up>")
        assert("DOWN").hasVimCode("<down>")
        assert("LEFT").hasVimCode("<left>")
        assert("RIGHT").hasVimCode("<right>")

        assert("ENTER").hasVimCode("<cr>")
    }

    @Test fun `Backspace works`() {
        assert("BACK_SPACE").hasVimCode("<bs>")
        assert('\u007F').hasVimCode("<bs>")
    }
}

private fun Assert<Any>.hasVimCode(expectedVimCode: String) {
    val stroke = when (val it = actual) {
        is Char -> stroke(it)
        is String -> stroke(it)
        is KeyStroke -> it
        else -> throw IllegalArgumentException()
    }
    val actualCode = stroke.toVimCode()
    if (actualCode.toLowerCase() == expectedVimCode.toLowerCase()) return
    expected("${show(stroke)} (${show(stroke.keyCode)}) to have vim code `$expectedVimCode` but was `$actualCode`")
}

private fun stroke(char: Char) = KeyStroke.getKeyStroke(char)
private fun stroke(string: String) = KeyStroke.getKeyStroke(string)