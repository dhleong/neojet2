package io.neovim.rpc.channels

import io.neovim.rpc.NeovimChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/**
 * @author dhleong
 */
class EmbeddedChannel(
    private val process: Process
) : NeovimChannel {

    private val job = Job()

    init {
        CoroutineScope(job).launch {
            process.errorStream.bufferedReader().lines().forEach {
                println("ERROR: $it")
            }
        }
    }

    override fun getInputStream(): InputStream = process.inputStream

    override fun getOutputStream(): OutputStream = process.outputStream

    override fun close() {
        job.cancel()
        process.destroy()
    }

    class Factory(
        private val path: String = "/usr/bin/env",
        private val args: List<String> = listOf("nvim")
    ) : NeovimChannel.Factory {

        override fun create(): NeovimChannel {
            val invocation = mutableListOf(path)
            invocation += args
            if ("--embed" !in invocation) {
                invocation += "--embed"
            }

            return EmbeddedChannel(
                ProcessBuilder(invocation).start()
            )
        }

    }
}