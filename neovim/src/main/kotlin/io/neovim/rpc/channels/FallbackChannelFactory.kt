package io.neovim.rpc.channels

import io.neovim.rpc.NeovimChannel

/**
 * Tries each [NeovimChannel.Factory] in order and returns
 * the first [NeovimChannel] that could be successfully created
 *
 * @author dhleong
 */
class FallbackChannelFactory(
    private vararg val candidates: NeovimChannel.Factory
): NeovimChannel.Factory {
    override fun create(): NeovimChannel {
        var lastException: Exception? = null
        for (candidate in candidates) {
            try {
                return candidate.create()
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw IllegalArgumentException("Unable to create any channel", lastException)
    }

    override fun toString(): String {
        return "FallbackChannelFactory{\n${candidates.joinToString("\n")}}"
    }
}