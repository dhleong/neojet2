package io.neovim.rpc

import com.fasterxml.jackson.annotation.JsonFormat

/**
 * @author dhleong
 */
data class ResponsePacket(
    override val type: Packet.Type = Packet.Type.RESPONSE,
    val requestId: Long,
    val error: ErrorResponse? = null,
    val result: Any? = null
) : Packet


@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class ErrorResponse(
    val type: Int,
    val message: String
)