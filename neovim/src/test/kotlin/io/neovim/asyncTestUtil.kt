package io.neovim

import kotlinx.coroutines.runBlocking

/**
 * Convenience to suppress warnings about JUnit functions having to return Unit
 *
 * @author dhleong
 */
fun runBlockingUnit(block: suspend () -> Unit): Unit = runBlocking {
    block()
    Unit
}
