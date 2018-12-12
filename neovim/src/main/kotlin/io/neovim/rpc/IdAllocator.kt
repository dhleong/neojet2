package io.neovim.rpc

import java.util.concurrent.atomic.AtomicLong

interface IdAllocator {
    fun next(): Long
}

class SimpleIdAllocator : IdAllocator {

    private val nextId = AtomicLong(0)

    override fun next(): Long = nextId.getAndIncrement()

}
