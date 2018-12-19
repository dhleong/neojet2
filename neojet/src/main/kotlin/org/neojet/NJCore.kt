package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import io.neovim.NeovimApi
import io.neovim.events.NeovimEvent
import io.neovim.log
import io.neovim.rpc.channels.EmbeddedChannel
import io.neovim.rpc.channels.FallbackChannelFactory
import io.neovim.types.IntPair
import kotlinx.coroutines.Job
import org.neojet.nvim.NvimWrapper
import org.neojet.util.*
import java.awt.KeyboardFocusManager
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

/**
 * @author dhleong
 */
class NJCore : BaseComponent, Disposable {

    private val logger: Logger = Logger.getLogger("neojet:NJCore")

    private lateinit var job: Job
    var nvim = NvimWrapper(FallbackChannelFactory(
//        SocketChannel.Factory("localhost", 7777),
        EmbeddedChannel.Factory()
    ))

    private var refs = AtomicInteger(0)

    override fun initComponent() {
        super.initComponent()

        corun {
            val info = nvim().getApiInfo()
            logger.info("Connected to nvim ${info.apiMetadata.version}")
        }

        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorReleased(event: EditorFactoryEvent) {
                event.editor.getUserData(neojetEnhancedEditorKey)?.let {
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

        job = corun(On.UI, daemon = true) {
            while (true) {
                val ev = nvim().nextEvent() ?: break

                val facade = EditorManager.getCurrent()
                log(">>> dispatch event($facade): $ev")

                if (!isTestMode) {
                    facade?.dispatch(ev)
                        ?: logger.warning("No editor to receive $ev")
                } else {
                    queuedEvents.add(ev)
                }
            }
            logger.info("Exit nvim event loop")
        }
    }

    val queuedEvents: MutableList<NeovimEvent> = mutableListOf()

    override fun disposeComponent() {
        nvim.close()
        job.cancel()
        super.disposeComponent()
    }

    override fun dispose() {
        Disposer.dispose(this)
    }

    fun attach(editor: Editor, enhanced: NeojetEnhancedEditorFacade): NeovimApi {
        val nvim = nvim()

        EditorManager.setActive(enhanced)
        logger.info("attach($editor) on ${Thread.currentThread()}")
        corun {
            logger.info(" >> attach($editor) on ${Thread.currentThread()}")
            nvim.command("setlocal nolist")
            uiAttach(nvim, editor, editor.document.vFile, enhanced.cells)
            enhanced.setReady()
        }

        return nvim()
    }

    private suspend fun uiAttach(nvim: NeovimApi, editor: Editor, vFile: VirtualFile, windowSize: IntPair) {
        Disposer.register(editor.disposable, Disposable {
            corun {
                editor.buffer?.detach()
            }
            if (0 == refs.decrementAndGet()) {
                logger.info("detach last")
                corun {
                    nvim.uiDetach()
                }
            }
        })

        if (0 == refs.getAndIncrement()) {
            val (width, height) = windowSize
            logger.info("attach: $width, $height")

            nvim.command("set laststatus=0")
            nvim.uiAttach(width.toLong(), height.toLong(), mapOf(
                "ext_popupmenu" to true,
                "ext_tabline" to true,
                "ext_cmdline" to true,
                "ext_wildmenu" to true
            ))

            logger.info("attached")
        }

        // NOTE if a swap file exists for the file, nvim
        // will hang waiting for keyboard input...
        nvim.command("set noswapfile")

        val localFile = vFile.absoluteLocalFile
        val filePath = localFile.absolutePath
        nvim.command("e! $filePath")

        val buffer = nvim.getCurrentBuf()
        editor.buffer = buffer

        if (!localFile.exists()) {
            // we need to load the buffer manually
            val lines = vFile.inputStream.bufferedReader().useLines { it.toList()  }
            logger.info("sending ${lines.size} lines to nvim")
            buffer.setLines(0, 0, false, lines)
        }

        // attach to the buffer
        buffer.attach(false, emptyMap())
        logger.info("installed!")
    }

    companion object {
        val instance: NJCore
            get() = ApplicationManager.getApplication()
                .getComponent(NJCore::class.java)

        var isTestMode: Boolean = false
//        var isTestMode: Boolean = true
    }
}
