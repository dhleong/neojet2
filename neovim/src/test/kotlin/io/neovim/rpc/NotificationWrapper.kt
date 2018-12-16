package io.neovim.rpc

class NotificationWrapper(
    override val type: Packet.Type = Packet.Type.NOTIFICATION,
    val name: String,
    var args: List<Any>
) : Packet
