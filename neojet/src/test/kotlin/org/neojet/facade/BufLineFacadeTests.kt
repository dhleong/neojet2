package org.neojet.facade

import org.neojet.NeojetTestCase

/**
 * @author dhleong
 */
class BufLineFacadeTests : NeojetTestCase() {

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

    fun `test Insert middle line`() = doTest(
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

    fun `test Insert last line`() {

    }
}