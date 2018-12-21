package org.neojet.facade

import org.neojet.NeojetTestCase

/**
 * @author dhleong
 */
class BufLineFacadeTests : NeojetTestCase() {

    /*
        Delete lines
     */

    fun `test Delete first line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
                void takeoff() {
                }
            }
        """.trimIndent()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 0,
            lastLine = 1,
            lines = listOf()
        )
    }

    fun `test Delete middle line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                }
            }
        """.trimIndent()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 1,
            lastLine = 2,
            lines = listOf()
        )
    }

    fun `test Delete empty line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
            <caret>
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 2,
            lastLine = 3,
            lines = listOf()
        )
    }

    fun `test Delete last line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                void takeoff() {
                }
        """.trimIndent()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 3,
            lastLine = 4,
            lines = listOf()
        )
    }

    /*
        Insert lines
     */

    fun `test Insert first line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            package io.firefly
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 0,
            lastLine = 0,
            lines = listOf("package io.firefly")
        )
    }

    fun `test Insert middle line simple`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                void takeoff() {
                    <caret>
                }
            }
        """.trimIndent().removeCaret()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 2,
            lastLine = 2,
            lines = listOf("        ")
        )
    }

    fun `test Insert middle line real`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                void takeoff() {
                    <caret>
                }
            }
        """.trimIndent().removeCaret()
    ) {
        // this is how nvim actually does it
        dispatchBufferLinesChanged(
            firstLine = 2,
            lastLine = 2,
            lines = listOf("")
        )
        dispatchBufferLinesChanged(
            firstLine = 2,
            lastLine = 3,
            lines = listOf("        ")
        )
    }

    fun `test Insert last line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        after = """
            class Ship {
                void takeoff() {
                }
            }
            <caret>
        """.trimIndent().removeCaret()
    ) {
        dispatchBufferLinesChanged(
            firstLine = 4,
            lastLine = 4,
            lines = listOf("")
        )
    }
}