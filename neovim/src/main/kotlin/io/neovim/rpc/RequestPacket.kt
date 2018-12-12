package io.neovim.rpc

/**
 * @author dhleong
 */
data class RequestPacket(
    override val type: Type = Type.REQUEST,
    val requestId: Long,
    val method: String,
    val args: Any? = null
) : Packet()