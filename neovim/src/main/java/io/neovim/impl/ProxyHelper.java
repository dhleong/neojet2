package io.neovim.impl;

import io.neovim.Rpc;
import io.neovim.events.NeovimEvent;
import kotlin.coroutines.Continuation;

import java.util.List;

/**
 * @author dhleong
 */
class ProxyHelper {
    static Object nextEvent(
        Rpc rpc,
        Continuation<? super NeovimEvent> continuation
    ) {
        return rpc.nextEvent(continuation);
    }

    static Object request(
        Rpc rpc,
        String method,
        List<Object> args,
        Class<?> resultType,
        Continuation<? super Object> continuation
    ) {
        final Object result = rpc.request(
            method, args, resultType,
            new ExceptionReportingContinuation(
                continuation,
                method,
                args
            )
        );

        // the suspend fn may choose to not suspend; in that case,
        // we directly unpack the result here:
        return ExceptionReportingContinuation.Companion.unpackCoroutineResultInline(
            result,
            method,
            args
        );
    }
}
