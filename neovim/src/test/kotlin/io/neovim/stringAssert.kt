package io.neovim

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show

fun Assert<String>.doesNotContain(substring: String) {
    if (!actual.contains(substring)) return
    expected("to NOT contain ${show(substring)} but was ${show(
        actual
    )}")
}