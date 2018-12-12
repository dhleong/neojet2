package io.neovim.rpc

/**
 * @author dhleong
 */
abstract class Packet {
    enum class Type {
        REQUEST,
        RESPONSE,
        NOTIFICATION
    }

    abstract val type: Type
}