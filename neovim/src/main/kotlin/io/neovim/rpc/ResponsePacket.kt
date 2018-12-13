package io.neovim.rpc

/**
 * @author dhleong
 */
data class ResponsePacket(
    override val type: Packet.Type = Packet.Type.RESPONSE,
    val requestId: Long,
    val error: Any? = null,
    val result: Any? = null
) : Packet