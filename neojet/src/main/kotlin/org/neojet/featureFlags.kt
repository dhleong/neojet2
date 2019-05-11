package org.neojet

const val INTELLIJ_MANAGED_INSERT_MODE = false

@Suppress("ConstantConditionIf")
inline fun whenIntellijManagesInsertMode(block: () -> Unit) {
    if (INTELLIJ_MANAGED_INSERT_MODE) {
        block()
    }
}