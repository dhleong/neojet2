package org.neojet

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.neovim.NeovimApi
import io.neovim.events.BufLinesEvent
import io.neovim.events.CursorGoto
import io.neovim.types.Window
import org.neojet.ext.createDataContext
import org.neojet.util.enhanced
import java.awt.event.KeyEvent

/**
 * @author dhleong
 */
abstract class NeojetTestCase : AbstractNeojetTestCase() {

    protected lateinit var nvim: NeovimApi
    protected val dispatchedKeys = mutableListOf<KeyEvent>()

    override fun setUp() {
        val window = mock<Window> {  }
        nvim = mock {
            onBlocking { getCurrentWin() } doReturn window
        }
        dispatchedKeys.clear()

        super.setUp()
    }

    override fun createNeovimProviderFactory(): NeovimProvider.Factory =
        object : NeovimProvider.Factory {
            override fun create() = object : NeovimProvider {
                override val api: NeovimApi
                    get() = nvim

                override fun attach(editor: Editor, facade: NeojetEnhancedEditorFacade): NeovimApi {
                    editor.enhanced = facade
                    facade.dispatchTypedKey = { ev -> dispatchedKeys.add(ev) }
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
    ) = facade.dispatch(BufLinesEvent(
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
    ) = facade.dispatch(CursorGoto(row.toLong(), col.toLong()))

    protected fun doTest(
        fileType: LanguageFileType = PlainTextFileType.INSTANCE,
        before: String,
        typeText: String,
        after: String,
        block: () -> Unit
    ) = doTest(
        fileType,
        before = before,
        after = after,
        block = {
            val handler = EditorActionManager.getInstance().typedAction.rawHandler
            val editor = facade.editor
            val context = editor.createDataContext()
            for (charTyped in typeText) {
                handler.execute(editor, charTyped, context)
            }
        },
        extraAsserts = block
    )

}