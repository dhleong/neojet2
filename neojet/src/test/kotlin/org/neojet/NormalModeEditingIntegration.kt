package org.neojet

/**
 * @author dhleong
 */
class NormalModeEditingIntegration : NeojetIntegrationTestCase() {
    fun `test Delete line`() = doTest(
        before = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent(),

        typeKeys = "dd",

        after = """
                void foo() {
                }
            }
        """.trimIndent()
    )
}