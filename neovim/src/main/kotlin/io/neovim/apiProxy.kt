package io.neovim

import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation

/**
 * @author dhleong
 */
inline fun <reified T> proxy(rpc: Rpc): T {

    val methodInfo = mutableMapOf<Method, ApiMethodInfo>()

    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, method, args ->
        @Suppress("UNCHECKED_CAST")
        val continuation = args.last() as Continuation<Any>

        val info = methodInfo[method] ?: ApiMethodInfo.from(method).also {
            methodInfo[method] = it
        }

        // TODO verify method supported on current API version

        // NOTE using runBlocking() here seems to pretty consistently pop over
        // to another coroutine *thread*. We could work around this by using a
        // Java class to pass our continuation object directly to the Java interface
        // to Rpc#request, but this is simpler... for now

        runBlocking(continuation.context) {
            val response = rpc.request(
                method = info.name,
                args = args.toList().dropLast(1)
            )
            when {
                response.error != null -> {
                    throw RuntimeException(response.error.toString())
                }
                else -> response.result ?: Unit
            }
        }
    } as T
}

data class ApiMethodInfo(
    val name: String,
    val sinceVersion: Int
) {
    companion object {
        fun from(method: Method): ApiMethodInfo {
            val annotation = method.getDeclaredAnnotation(ApiMethod::class.java)
            return ApiMethodInfo(annotation.name, annotation.since)
        }
    }
}
