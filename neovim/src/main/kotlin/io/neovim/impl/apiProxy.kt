package io.neovim.impl

import io.neovim.ApiMethod
import io.neovim.Rpc
import io.neovim.events.NeovimEvent
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

private interface ProxiedType {
    val customTypeInstanceId: Long?
}

/**
 * @author dhleong
 */
inline fun <reified T : Any> proxy(
    rpc: Rpc,
    customTypeInstanceId: Long? = null
): T = proxy(T::class, rpc, customTypeInstanceId)

fun <T : Any> proxy(
    type: KClass<T>,
    rpc: Rpc,
    customTypeInstanceId: Long? = null
): T {

    // extract method info lazily
    val methodInfo = mutableMapOf<Method, ApiMethodInfo>()

    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        type.java.classLoader,
        arrayOf(type.java, ProxiedType::class.java)
    ) { _, method, args ->

        val handleResult = handleSimpleMethods(
            type.java, rpc, customTypeInstanceId, method, args
        )
        if (handleResult !== NOT_SIMPLE) {
            return@newProxyInstance handleResult
        }

        @Suppress("UNCHECKED_CAST")
        val continuation = args.last() as Continuation<Any>

        val info = methodInfo[method] ?: ApiMethodInfo.from(type, method).also {
            methodInfo[method] = it
        }

        val fullArgs: MutableList<Any> = if (customTypeInstanceId != null) {
            // for instance methods, the first argument is always
            // the instance
            ArrayList<Any>(args.size).also {
                it += customTypeInstanceId
            }
        } else {
            ArrayList(args.size - 1)
        }

        // always add whatever args were provided
        fullArgs.addAll(args.toList().dropLast(1))

        // we have to pull some tricks to properly implement suspending
        // from this proxy. This, unfortunately, involves hopping over to
        // Java land briefly
        ProxyHelper.request(
            rpc,
            info.name,
            fullArgs,
            info,
            continuation
        )
    } as T
}

// sentinel object indicating that handleSimpleMethods did not handle the method
private val NOT_SIMPLE: Any = Object()

private fun <T> handleSimpleMethods(
    type: Class<T>,
    rpc: Rpc,
    customTypeInstanceId: Long?,
    method: Method,
    args: Array<out Any?>?
): Any? = when (method.name) {
    "nextEvent" -> {
        // nextEvent is a special case for reading notifications
        @Suppress("UNCHECKED_CAST")
        ProxyHelper.nextEvent(rpc, args!![0] as Continuation<NeovimEvent>)
    }

    // NOTE the id property is implemented as a getId() method
    "getId" -> customTypeInstanceId
        ?: throw IllegalStateException(
            "No instance ID for $type to return for .id call"
        )

    // ProxiedType interface for implementing the below:

    "getCustomTypeInstanceId" -> customTypeInstanceId

    // normal object methods

    "equals" -> {
        val other = args!![0]
        other != null
            && type.isInstance(other)
            && other is ProxiedType
            && other.customTypeInstanceId == customTypeInstanceId
    }

    "hashCode" -> {
        val typeHash = type.hashCode()
        31 * typeHash + (customTypeInstanceId?.hashCode() ?: 0)
    }

    "toString" -> {
        if (customTypeInstanceId != null) {
            "${type.simpleName}[$customTypeInstanceId]"
        } else type.simpleName
    }

    else -> NOT_SIMPLE
}

data class ApiMethodInfo(
    val name: String,
    val sinceVersion: Int,
    val deprecatedSinceVersion: Int,
    val resultType: Class<*>
) {
    companion object {
        fun from(sourceClass: KClass<*>, method: Method): ApiMethodInfo {
            val annotation = method.getDeclaredAnnotation(ApiMethod::class.java)
            val kmethod = sourceClass.members.first {
                it.name == method.name
            }
            return ApiMethodInfo(
                name = annotation.name,
                sinceVersion = annotation.since,
                deprecatedSinceVersion = annotation.deprecatedSince,
                resultType = kmethod.returnType.jvmErasure.java
            )
        }
    }
}

