package org.neojet

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.LanguageFileType
import io.neovim.rpc.channels.EmbeddedChannel
import kotlinx.coroutines.runBlocking
import org.neojet.events.DefaultEventDaemon
import org.neojet.nvim.input
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
abstract class NeojetIntegrationTestCase : AbstractNeojetTestCase() {

    override fun createNeovimProviderFactory(): NeovimProvider.Factory =
        DefaultNeovimProvider.Factory(
            EmbeddedChannel.Factory(args = listOf("-u", "NONE"))
        )

    override fun tearDown() {
        runBlocking {
            NJCore.instance.nvim.command("bdelete!")
        }

        super.tearDown()

        NJCore.providerFactory = NJCore.defaultProviderFactory
        NJCore.eventsFactory = DefaultEventDaemon.Factory()

        events.drain()
    }

    override fun configureByText(
        content: String,
        fileType: LanguageFileType
    ): Editor = super.configureByText(content, fileType).also {
        facade.dispatchTypedKey = { ev ->
            runBlocking {
                NJCore.instance.nvim.input(ev)
            }
        }

        // wait for the facade to be ready
        runBlocking {
            facade.awaitReady()
        }
    }

    @Suppress("MemberVisibilityCanPrivate")
    protected fun typeTextInFacade(keys: List<KeyStroke>): Editor {
        val editor = myFixture.editor
        for (key in keys) {
            facade.dispatchTypedKey(KeyEvent(editor.component,
                0, 0, key.modifiers, key.keyCode, key.keyChar
            ))
        }

        events.collectAndDispatch(timeoutMillis = 750)

        return editor
    }

    fun doTest(before: String, typeKeys: String, after: String) {
        doTest(before, keys(typeKeys), after)
    }

    fun doTest(before: String, type: List<KeyStroke>, after: String) {
        doTest(before = before, after = after) {
            typeTextInFacade(type)
        }
    }

    fun keys(asTyped: String) = asTyped.map {
        KeyStroke.getKeyStroke(it)
    }

    override fun onPreTest() {
        // always ensure we start at the top of the file
        // TODO if we properly ensure the caret is sync'd *to* nvim on buffer open
        // this might be unnecessary
        typeTextInFacade(keys("gg"))
    }
}

