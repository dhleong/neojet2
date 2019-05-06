package org.neojet.facade

import com.intellij.ide.highlighter.JavaFileType
import org.neojet.NeojetTestCase
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

        after = """
            class Foo {
            }
        """.trimIndent()
    ) {
        val buffer = facade.editor.buffer ?: throw IllegalStateException()
        // TODO
//        verifyBlocking(buffer) {
//            setLines(0, 2, true, listOf(
//                "class Foo {",
//                "}"
//            ))
//        }
    }

}
