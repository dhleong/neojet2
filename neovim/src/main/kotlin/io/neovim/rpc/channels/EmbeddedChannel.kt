package io.neovim.rpc.channels

import io.neovim.rpc.NeovimChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UncheckedIOException

/**
 * @author dhleong
 */
class EmbeddedChannel(
    private val process: Process
) : NeovimChannel {

    private val job = Job()

    init {
        GlobalScope.launch(job) {
            try {
                process.errorStream.bufferedReader().lines().forEach {
                    println("ERROR: $it")
                }
            } catch (e: UncheckedIOException) {
                // ignore
            } catch (e: IOException) {
                // ignore
            }
        }
    }

    override fun getInputStream(): InputStream = process.inputStream

    override fun getOutputStream(): OutputStream = process.outputStream

    override fun close() {
        job.cancel()
        process.destroy()
    }

    data class Factory(
        private val path: String = "/usr/bin/env",
        private val args: List<String> = emptyList()
    ) : NeovimChannel.Factory {

        override fun create(): NeovimChannel {
            val invocation = mutableListOf(path)

            // using the `env` path, the first argument must be `nvim`.
            // this simplifies the `args` usage to just be any args
            // to pass to nvim
            if (
                path == "/usr/bin/env"
                && (args.isEmpty()
                    || args[0] != "nvim")
            ) {
                invocation += "nvim"
            }

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