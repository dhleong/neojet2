package org.neojet.facade

import com.intellij.ide.highlighter.JavaFileType
import com.nhaarman.mockitokotlin2.verifyBlocking
import org.neojet.NeojetTestCase
import org.neojet.NvimMode
import org.neojet.util.buffer

/**
 * Tests for handling IntelliJ-generated text change events
 * @author dhleong
 */
class EditorTextChangeTests : NeojetTestCase() {

    fun `test auto-closing brackets`() = doTest(JavaFileType.INSTANCE,
        before = """
            class Foo <caret>
        """.trimIndent(),

        typeText = "{\n",
        inMode = NvimMode.INSERT,

        after = """
            class Foo {
            }
        """.trimIndent()
    ) {
        val buffer = facade.editor.buffer ?: throw IllegalStateException()

        verifyBlocking(buffer) {
            setLines(0, 1, false, listOf(
                "class Foo {"
            ))
        }

        verifyBlocking(buffer) {
            setLines(0, 1, false, listOf(
                "class Foo {}"
            ))
        }

        verifyBlocking(buffer) {
            setLines(0, 2, false, listOf(
                "class Foo {",
                "}"
            ))
        }
    }

}
