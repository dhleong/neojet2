package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.util.Disposer
import io.neovim.NeovimApi
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.rpc.channels.FallbackChannelFactory
import io.neovim.rpc.channels.SocketChannel
import org.neojet.events.DefaultEventDaemon
import org.neojet.events.EventDaemon
import org.neojet.util.enhanced
import java.awt.KeyboardFocusManager

/**
 * @author dhleong
 */
class NJCore : BaseComponent, Disposable {

    val nvim: NeovimApi
        get() = provider.api

    private var provider = providerFactory.create()
    private var events = eventsFactory.create()

    override fun initComponent() {
        super.initComponent()

        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorReleased(event: EditorFactoryEvent) {
                event.editor.enhanced?.let {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .removeKeyEventDispatcher(it.keyEventDispatcher)
                }
            }

            override fun editorCreated(event: EditorFactoryEvent) {
                // TODO we can probably get away with a single KeyEventDispatcher
                val facade = NeojetEnhancedEditorFacade.install(event.editor)
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(facade.keyEventDispatcher)
            }
        }, this)

        // start dispatching neovim events:
        events.start(provider.api)
    }

    override fun disposeComponent() {
        provider.close()
        events.stop()
        super.disposeComponent()
    }

    override fun dispose() {
        Disposer.dispose(this)
    }

    fun attach(editor: Editor, enhanced: NeojetEnhancedEditorFacade): NeovimApi {
        EditorManager.setActive(enhanced)

        return provider.attach(editor, enhanced)
    }

    companion object {
        val instance: NJCore
            get() = ApplicationManager.getApplication()
                .getComponent(NJCore::class.java)

        val defaultProviderFactory = DefaultNeovimProvider.Factory(
            FallbackChannelFactory(
                SocketChannel.Factory("localhost", 7777),
                EmbeddedChannel.Factory()
            )
        )

        var providerFactory: NeovimProvider.Factory = defaultProviderFactory
        var eventsFactory: EventDaemon.Factory = DefaultEventDaemon.Factory()
    }
}
