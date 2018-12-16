package org.neojet.events

import io.neovim.events.NeovimEvent

/**
 * Marker interface for classes that can receive event dispatches
 */
interface EventDispatchTarget

/**
 * Marker annotation for functions that handle an event
 */
annotation class HandlesEvent

internal typealias EventHandler = (event: NeovimEvent) -> Unit

/**
 * @author dhleong
 */
class EventDispatcher(
    private val target: EventDispatchTarget,
    private val warnUnhandled: Boolean = true
) {

    private val handlers: Map<Class<*>, EventHandler> =
        target.javaClass.declaredMethods
            .filter { it.getAnnotation(HandlesEvent::class.java) != null }
            .fold(mutableMapOf()) { handlers, method ->
                val parameterTypes = method.parameterTypes
                if (method.parameterTypes.size != 1) {
                    throw IllegalArgumentException(
                        "On @HandlesEvent method $method; incorrect " +
                                "number of parameters (${method.parameterTypes.size})")
                }

                val parameterType = parameterTypes[0]
                if (!NeovimEvent::class.java.isAssignableFrom(parameterType)) {
                    throw IllegalArgumentException(
                        "On @HandlesEvent method $method; parameter " +
                                "type $parameterType is not an Event")
                }

                handlers.also {
                    it[parameterType] = { event ->
                        method.invoke(target, event)
                    }
                }
            }

    fun <T : NeovimEvent> dispatch(event: T) {
        handlers[event.javaClass]?.let {
            it.invoke(event)
            return
        }

        if (warnUnhandled) {
            System.err.println("WARN: Unhandled event on $target: $event")
        }
    }
}