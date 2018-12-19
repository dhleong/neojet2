package org.neojet

/**
 * @author dhleong
 */
class NormalModeEditingIntegration : NeojetIntegrationTestCase() {
    fun `test Delete first line`() = doTest(
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

    fun `test Delete first line and undo`() = doTest(
        before = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent(),

        typeKeys = "ddu",

        after = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent()
    )

    fun `test Delete middle line and undo`() = doTest(
        before = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent(),

        typeKeys = "jddu",

        after = """
            class Test {
                void foo() {
                }
            }
        """.trimIndent()
    )
}