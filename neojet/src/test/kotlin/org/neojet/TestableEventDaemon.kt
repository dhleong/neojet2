package org.neojet

import io.neovim.NeovimApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.neojet.events.EventDaemon

/**
 * This EventDaemon does not dispatch any events when [started][start],
 * but instead provides an additional [collectAndDispatch] method that
 * drains and dispatches events until none have arrived within [timeoutMillis]
 */
class TestableEventDaemon(
    private val timeoutMillis: Long = 100
) : EventDaemon {

    private lateinit var nvim: NeovimApi

    override fun start(api: NeovimApi) {
        nvim = api
    }

    override fun stop() {
        // nop
    }

    fun collectAndDispatch() {
        drain(requireEditor = true)
    }

    fun drain(requireEditor: Boolean = false) {
        runBlocking {
            launch {
                while (true) {
                    val event = withTimeoutOrNull(timeoutMillis) {
                        nvim.nextEvent()
                    } ?: break

                    val editor = EditorManager.getCurrent()
                    if (editor == null) {
                        if (requireEditor) {
                            throw IllegalStateException("No editor to dispatch events to")
                        }
                        continue
                    }

                    editor.dispatch(event)
                }
            }.join()
        }
    }
}
