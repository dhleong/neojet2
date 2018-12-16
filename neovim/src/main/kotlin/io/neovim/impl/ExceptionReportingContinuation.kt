package io.neovim.impl

import io.neovim.rpc.ResponsePacket
import io.neovim.types.NeovimException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author dhleong
 */
class ExceptionReportingContinuation(
    private val base: Continuation<Any?>,
    private val methodName: String,
    private val methodArgs: List<Any>
) : Continuation<ResponsePacket> {

    override val context: CoroutineContext
        get() = base.context

    override fun resumeWith(result: Result<ResponsePacket>) {
        val packet = result.getOrThrow()
        if (packet.error != null) {
            base.resumeWithException(toException(packet, methodName, methodArgs))
        } else {
            base.resume(unpack(packet))
        }
    }

    companion object {
        private fun unpack(packet: ResponsePacket) = packet.result ?: Unit

        private fun toException(
            packet: ResponsePacket,
            methodName: String,
            methodArgs: List<Any>
        ) = NeovimException(
            // TODO include error type?
            packet.error!!.message,
            method = methodName,
            args = methodArgs
        )

        fun unpackCoroutineResultInline(
            result: Any?,
            methodName: String,
            methodArgs: List<Any>
        ): Any? = when {
            result is ResponsePacket && result.error != null ->
                throw toException(result, methodName, methodArgs)

            result is ResponsePacket -> unpack(result)

            // suspended coroutine
            else -> result
        }
    }
}