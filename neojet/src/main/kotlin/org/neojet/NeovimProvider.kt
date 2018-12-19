package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import io.neovim.NeovimApi
import io.neovim.rpc.NeovimChannel
import io.neovim.types.IntPair
import org.neojet.nvim.NvimWrapper
import org.neojet.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

/**
 * @author dhleong
 */
interface NeovimProvider : AutoCloseable {
    interface Factory {
        fun create(): NeovimProvider
    }

    val api: NeovimApi

    /**
     * Implementations MUST call [NeojetEnhancedEditorFacade.setReady]
     */
    fun attach(editor: Editor, facade: NeojetEnhancedEditorFacade): NeovimApi
}

class DefaultNeovimProvider(
    neovimChannelFactory: NeovimChannel.Factory
) : NeovimProvider {
    class Factory(
        private val neovimChannelFactory: NeovimChannel.Factory
    ) : NeovimProvider.Factory {
        override fun create() = DefaultNeovimProvider(neovimChannelFactory)
    }

    private val logger: Logger = Logger.getLogger("neojet:Provider")

    private val nvim = NvimWrapper(neovimChannelFactory)

    private var refs = AtomicInteger(0)

    override val api: NeovimApi
        get() = nvim.instance

    override fun attach(editor: Editor, facade: NeojetEnhancedEditorFacade): NeovimApi {
        val nvim = nvim()

        logger.info("attach($editor) on ${Thread.currentThread()}")
        corun {
            logger.info(" >> attach($editor) on ${Thread.currentThread()}")
            nvim.command("setlocal nolist")
            uiAttach(nvim, editor, editor.document.vFile, facade.cells)
            facade.setReady()
        }

        return nvim
    }

    private suspend fun uiAttach(
        nvim: NeovimApi,
        editor: Editor,
        vFile: VirtualFile,
        windowSize: IntPair
    ) {
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

    override fun close() {
        nvim.close()
    }
}