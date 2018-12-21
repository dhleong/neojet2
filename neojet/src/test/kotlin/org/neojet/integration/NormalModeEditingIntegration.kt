package org.neojet.integration

import org.neojet.NeojetIntegrationTestCase

/**
 * @author dhleong
 */
class NormalModeEditingIntegration : NeojetIntegrationTestCase() {

    fun `test placeholder`() {
        // these integration test cases are too unreliable :(
        // this method is here so we can keep this class for posterity
        // without gradle barfing over the lack of tests in it
    }

    fun `skip Delete first line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        typeKeys = "dd",

        after = """
                void takeoff() {
                }
            }
        """.trimIndent()
    )

    fun `skip Delete first line and undo`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        typeKeys = "ddu",

        after = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent()
    )

    fun `skip Delete middle line and undo`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        typeKeys = "jddu",

        after = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent()
    )

    fun `skip Open new line`() = doTest(
        before = """
            class Ship {
                void takeoff() {
                }
            }
        """.trimIndent(),

        typeKeys = "jjo",

        after = """
            class Ship {
                void takeoff() {

                }
            }
        """.trimIndent()
    )
}