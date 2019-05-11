package io.neovim.types

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.neovim.DummyPacketsChannel
import io.neovim.Rpc
import io.neovim.rpc.createNeovimObjectMapper
import org.junit.Test
import org.msgpack.core.MessagePack



/**
 * @author dhleong
 */
class IntPairTest {
    @Test fun `Serialization works as expected`() {
        val mapper = createNeovimObjectMapper(Rpc(DummyPacketsChannel()))
        val bytes = mapper.writeValueAsBytes(IntPair(42, 9001))

        val pack = MessagePack.newDefaultBufferPacker()
        pack.packArrayHeader(2)
        pack.packInt(42)
        pack.packInt(9001)

        assertThat(bytes.toList())
            .isEqualTo(pack.toByteArray().toList())
    }
}