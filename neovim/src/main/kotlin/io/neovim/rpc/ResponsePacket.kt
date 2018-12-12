package io.neovim.rpc

/**
 * @author dhleong
 */
class ResponsePacket(
    override val type: Type = Type.RESPONSE,
    val requestId: Long,
    val error: Any? = null,
    val result: Any? = null
) : Packet()