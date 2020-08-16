package io.neovim

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import io.neovim.events.NeovimEvent
import io.neovim.events.Redraw
import io.neovim.rpc.RequestPacket
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.types.Buffer
import io.neovim.types.IntPair
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.whileSelect
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class NeovimApiIntegrationTest {

    lateinit var api: NeovimApi
    lateinit var rpc: Rpc

    @Before fun setUp() {
        rpc = Rpc(EmbeddedChannel.Factory(
            args = listOf("-u", "NONE")
        ).create())
        api = NeovimApi.create(rpc)
    }

    @After fun tearDown() {
        rpc.close()
    }

    @Test fun `Basic API calls work`() = runBlockingUnit {
        assertThat(api.getCurrentLine()).isInstanceOf(String::class.java)
    }

    @Test fun `Custom types get decoded`() = runBlockingUnit {
        assertThat(api.getCurrentBuf()).isInstanceOf(Buffer::class.java)
    }

    @Test fun `Custom types get encoded`() = runBlockingUnit {
        val buf = api.getCurrentBuf()
        assertThat(buf).isInstanceOf(Buffer::class.java)

        assertThat {
            runBlocking {
                api.setCurrentBuf(buf)
            }
        }.returnedValue { }
    }

    @Test fun `Instance methods on custom types work`() = runBlockingUnit {
        val buffer = api.getCurrentBuf()

        assertThat(buffer.isValid()).isTrue()
        assertThat(buffer.getName()).isEqualTo("")
        assertThat(buffer.getNumber()).isEqualTo(1L)
    }

    @Test fun `Arguments to methods on custom types work`() = runBlockingUnit {
        val win = api.getCurrentWin()

        assertThat(win.isValid()).isTrue()
        assertThat {
            runBlocking {
                win.setCursor(IntPair(1, 0))
            }
        }.returnedValue {  }
    }

    @Test fun `Read notifications`() = runBlockingUnit {
        api.uiAttach(10, 10, emptyMap())

        val nextEvent = api.nextEvent()
        assertThat(nextEvent).isNotNull().all {
            isInstanceOf(NeovimEvent::class)
        }
    }

    @Test(timeout = 1000) fun `Attach to buffer`() = runBlockingUnit {
        val buf = api.getCurrentBuf()

        // set some content to ensure the buffer is "loaded"
        buf.setLines(0, 0, false, listOf("Test"))

        val attached = buf.attach(false, emptyMap())
        assertThat(attached).isTrue()
    }


//    @Test(timeout = 1000) fun `Busy open`() = runBlockingUnit {
//        api.command("setlocal nolist")
//
//        // set some content to ensure the buffer is "loaded"
//        buf.setLines(0, 0, false, listOf("Test"))
//
//        val attached = buf.attach(false, emptyMap())
//        assertThat(attached).isTrue()
//    }

    @Test(timeout = 5000) fun `Neojet Open`() = runBlockingUnit {
        api.command("setlocal nolist")
        api.command("set laststatus=0")
        api.uiAttach(60, 25, mapOf(
            "ext_popupmenu" to true,
            "ext_tabline" to true,
            "ext_cmdline" to true,
            "ext_wildmenu" to true
        ))

        val fileToEdit = File("build.gradle")
        coroutineScope {
            val events = async {
                while (isActive) {
                    val event = api.nextEvent()
                    assertThat(event).isNotNull()
                    println("event = $event")
                }
            }

            listOf(
                async {
                    api.command("noswapfile e! '${fileToEdit.absolutePath}'")
                    events.cancel()
                },

                async {
                    delay(100)
                    val output = api.commandOutput("echo 'test'")
                    println("OUTPUT = $output")
                }

            )
        }.awaitAll()
    }
}


