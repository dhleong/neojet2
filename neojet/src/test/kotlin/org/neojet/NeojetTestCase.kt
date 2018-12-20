package org.neojet

import com.intellij.openapi.editor.Editor
import com.nhaarman.mockito_kotlin.mock
import io.neovim.NeovimApi
import io.neovim.events.BufLinesEvent
import io.neovim.events.CursorGoto
import org.neojet.util.enhanced

/**
 * @author dhleong
 */
abstract class NeojetTestCase : AbstractNeojetTestCase() {

    protected lateinit var nvim: NeovimApi

    override fun setUp() {
        nvim = mock {  }

        super.setUp()
    }

    override fun createNeovimProviderFactory(): NeovimProvider.Factory =
        object : NeovimProvider.Factory {
            override fun create() = object : NeovimProvider {
                override val api: NeovimApi
                    get() = nvim

                override fun attach(editor: Editor, facade: NeojetEnhancedEditorFacade): NeovimApi {
                    editor.enhanced = facade
                    facade.setReady()
                    return api
                }

                override fun close() { }
            }
        }

    /*
        Utils
     */

    /**
     * In case you want the caret as a visual cue for whitespace,
     * but don't actually want to verify its position
     */
    protected fun String.removeCaret() = replace("<caret>", "")

    protected fun dispatchBufferLinesChanged(
        firstLine: Long,
        lastLine: Long,
        lines: List<String>
    ) = facade.bufferLinesChanged(BufLinesEvent(
        mock {},
        changedtick = 1,
        firstline = firstLine,
        lastline = lastLine,
        linedata = lines,
        more = false
    ))

    protected fun dispatchCursorGoto(
        row: Int,
        col: Int
    ) = facade.cursorMoved(CursorGoto(row.toLong(), col.toLong()))

}