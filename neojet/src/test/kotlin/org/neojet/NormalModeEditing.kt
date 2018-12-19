package org.neojet

/**
 * @author dhleong
 */
class NormalModeEditing : NeojetTestCase() {
    fun `test Delete line`() = doTest(
        before = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent(),

        after = """
                void foo() {
                }
            }
        """.trimIndent()
    ) {
//        dispatchLinesEvent(0, 1, lines = emptyList())
        typeText(keys("ggdd"))
    }

    private fun dispatchLinesEvent(
        firstLine: Int,
        lastLine: Int,
        lines: List<String>
    ) {
//        facade.dispatch(BufLinesEvent(
//            buffer = Buffer.create(rpc, 1),
//            changedtick = 1L,
//            firstline = firstLine.toLong(),
//            lastline = lastLine.toLong(),
//            linedata = lines,
//            more = false
//        ))
    }
}