package org.neojet.events

import io.neovim.NeovimApi
import io.neovim.log
import kotlinx.coroutines.Job
import org.neojet.EditorManager
import org.neojet.util.On
import org.neojet.util.corun
import java.util.logging.Logger

/**
 * @author dhleong
 */
interface EventDaemon {
    interface Factory {
        fun create(): EventDaemon

        companion object {
            // SAM fakery
            operator fun invoke(factory: () -> EventDaemon) = object : Factory {
                override fun create(): EventDaemon = factory()
            }
        }
    }

    fun start(api: NeovimApi)
    fun stop()
}

class DefaultEventDaemon : EventDaemon {
    class Factory : EventDaemon.Factory {
        override fun create(): EventDaemon = DefaultEventDaemon()
    }

    private val logger: Logger = Logger.getLogger("neojet:EventDaemon")

    private lateinit var job: Job

    override fun start(api: NeovimApi) {
        job = corun(On.UI) {
            while (true) {
                val ev = api.nextEvent() ?: break

                val facade = EditorManager.getCurrent()
                log(">>> dispatch event($facade): $ev")

                facade?.dispatch(ev)
                    ?: logger.warning("No editor to receive $ev")
            }
            logger.info("Exit nvim event loop")
        }
    }

    override fun stop() {
        job.cancel()
    }

}