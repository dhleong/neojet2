package org.neojet.facade

import com.intellij.ide.highlighter.JavaFileType
import com.nhaarman.mockitokotlin2.verifyBlocking
import org.neojet.NeojetTestCase
import org.neojet.util.buffer

/**
 * Tests for handling IntelliJ-generated text change events
 * @author dhleong
 */
class EditorTextChangeTests : NeojetTestCase() {

    // TODO
    fun skipNewLines() = doTest(JavaFileType.INSTANCE,
        before = """
            class Foo <caret>
        """.trimIndent(),

        typeText = "{\n",

        after = """
            class Foo {
            }
        """.trimIndent()
    ) {
        val buffer = facade.editor.buffer ?: throw IllegalStateException()
        verifyBlocking(buffer) {
            setLines(0, 2, true, listOf(
                "class Foo {",
                "}"
            ))
        }
    }

}
