package io.neovim

annotation class ApiMethod(
    val name: String,
    val since: Int,
    val deprecatedSince: Int = -1
)
