package io.neovim

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import io.neovim.rpc.RequestPacket
import io.neovim.rpc.ResponsePacket

fun Assert<ResponsePacket>.hasRequestId(id: Long) = given { actual ->
    if (actual.requestId == id) return
    expected("request id = ${show(id)}; was ${show(actual.requestId)}")
}

fun Assert<RequestPacket>.hasMethod(method: String) = given { actual ->
    if (actual.method == method) return
    expected("request method ${show(method)}; was ${show(actual.method)}")
}
