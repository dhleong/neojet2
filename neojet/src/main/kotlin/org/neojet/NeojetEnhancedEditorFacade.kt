package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import io.neovim.NeovimApi
import io.neovim.events.*
import io.neovim.events.params.CursorShape
import io.neovim.events.params.ModeInfo
import io.neovim.types.Buffer
import io.neovim.types.IntPair
import io.neovim.types.height
import org.neojet.events.EventDispatchTarget
import org.neojet.events.EventDispatcher
import org.neojet.events.HandlesEvent
import org.neojet.nvim.input
import org.neojet.util.*
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.util.logging.Logger
import javax.swing.JComponent

/**
 * @author dhleong
 */

val neojetEnhancedEditorKey = Key<NeojetEnhancedEditorFacade>("org.neojet.enhancedEditor")
val neojetBufferKey = Key<Buffer>("org.neojet.buffer")

class NeojetEnhancedEditorFacade private constructor(
    val editor: Editor
) : Disposable, EventDispatchTarget {
    companion object {
        fun install(editor: Editor): NeojetEnhancedEditorFacade {
            if (!(editor is EditorImpl || editor is TextEditor)) {
                throw IllegalArgumentException("$editor is not an EditorEx or TextEditor")
            }

            val existing = editor.enhanced
            if (existing != null) {
                // idempotency
                return existing
            }

            val facade = NeojetEnhancedEditorFacade(editor)
            editor.enhanced = facade

            if (editor is EditorImpl) {
                Disposer.register(editor.disposable, facade)
            } else if (editor is TextEditor) {
                Disposer.register(editor, facade)
            }

            return facade
        }
    }

    val keyEventDispatcher: KeyEventDispatcher = KeyEventDispatcher {
        val isForOurComponent = it?.component?.belongsTo(editor.component) ?: false
        if (isForOurComponent && it.id == KeyEvent.KEY_TYPED) {
            dispatchTypedKey(it)
            true // consume
        } else if (isForOurComponent) {
            // TODO handle held keys, for example
            false
        } else {
            // not for our editor; ignore
            false
        }
    }

    private val caretMovedListener = object : CaretListener {
        override fun caretPositionChanged(ev: CaretEvent) {
            if (editor.buffer == null) return // not installed/buffer not loaded yet

            val line = ev.newPosition.line + 1 // 0-indexed to 1-indexed
            val column = ev.newPosition.column

            println("caret moved! notifying nvim")
            corun {
                nvim.getCurrentWin()
                    .setCursor(IntPair(line, column))
            }
        }
    }

    private val logger: Logger = Logger.getLogger("NeoJet:EditorFacade")

    // TODO handle resize
    var cells = editor.getTextCells()

    val nvim: NeovimApi = NJCore.instance.attach(editor, this)
    private val dispatcher = EventDispatcher(this)

    private lateinit var modes: List<ModeInfo>
    private var mode: ModeInfo? = null

    var editingDocumentFromVim = false
    private var cursorRow: Int = 0
    private var cursorCol: Int = 0

//    private val exBuffer = ExBuffer()

//    private var currentScrollRegion: SetScrollRegionEvent.ScrollRegion =
//        SetScrollRegionEvent.ScrollRegion()

    init {
        editor.caretModel.addCaretListener(caretMovedListener)
        editor.contentComponent.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                EditorManager.setActive(this@NeojetEnhancedEditorFacade)
                corun {
                    editor.buffer?.let {
                        nvim.setCurrentBuf(it)
                    }
                }
            }
        })

//        editor.document.addDocumentListener(object : DocumentListener {
//            override fun documentChanged(e: DocumentEvent) {
//                if (editingDocumentFromVim) return
//
//                // TODO: IntelliJ edited the document unexpectedly
//                System.out.println(
//                    "Document changed @${e.offset}: `${e.oldFragment}` -> `${e.newFragment}`")
//                System.out.println("${e.oldLength} -> ${e.newLength}")
//            }
//        }, this)
    }

    override fun dispose() {
        editor.caretModel.removeCaretListener(caretMovedListener)
    }

    fun dispatchTypedKey(e: KeyEvent) {
        corun {
            nvim.input(e)
        }
    }

    /*
     * Notifications from nvim
     */

    @HandlesEvent
    fun bufferLinesChanged(event: BufLinesEvent) {
        println(event)

        // this seems very wacky:
        val startOffset = editor.getLineStartOffset(event.firstline.toInt())
        val endOffset = editor.getLineStartOffset(event.lastline.toInt())
        if (event.firstline == event.lastline) {
            println("insert lines at $endOffset (${event.lastline})")
            editor.document.insertString(endOffset, event.linedata.joinToString(
                separator = "\n",
                prefix = "\n"
            ))
        } else {
            val replacement = event.linedata.joinToString("\n")
            println("replace from $startOffset (${event.firstline}) - $endOffset (${event.lastline}) with `$replacement`")
            editor.document.replaceString(
                startOffset,
                if (endOffset == startOffset) endOffset + 1
                else endOffset,
                replacement
            )
        }
    }

    @HandlesEvent
    fun cursorMoved(event: CursorGoto) {
        cursorRow = event.row.toInt()
        cursorCol = event.col.toInt()

        if (!cursorInDocument) {
            // nothing to see here
            return
        }

        withoutCaretNotifications {
            val newLogicalPosition = getLogicalPosition()

            val lineEndOffset = editor.getLineEndOffset(newLogicalPosition.line)
            val lineLength = lineEndOffset - editor.getLineStartOffset(newLogicalPosition.line)
            val endDiff = cursorCol - lineLength
            if (endDiff > 0) {
                // this implies inserting spaces
                editor.document.insertString(lineEndOffset, " ".repeat(endDiff))
            }

            editor.caretModel.primaryCaret.moveToLogicalPosition(newLogicalPosition)
        }
    }

    @HandlesEvent
    fun modeInfoSet(event: ModeInfoSet) {
        modes = event.cursorStyles
    }

    @HandlesEvent
    fun modeChange(event: ModeChange) {
        modes[event.modeIdx.toInt()].let {
            mode = it

            updateCursor(it)
        }
    }

    @HandlesEvent
    fun put(event: Put) {
        if (DumbService.getInstance(editor.project!!).isDumb) return

        if (!cursorInDocument) return

        // NOTE we handle changes to the document via the bufferLinesChanged
        // event handler

        ++cursorCol
    }

//    @HandlesEvent
//    fun scroll(event: ScrollEvent) {
//        val region = currentScrollRegion
//        for (scroll in event.value) {
//            val scrollAmount = scroll.value
//            System.out.println("scroll($region, $scrollAmount)")
//
//            if (hasDefaultScrollRegion && exBuffer.isActive) {
//                if (scrollAmount < 0) {
//                    exBuffer.clear()
//                } else {
//                    System.out.println("-->> suppressing for echo/ex")
//                    exBuffer.append("")
//                    continue
//                }
//            }
//
//            val range = editor.getTextRange(region, scroll)
//            val scrollRegionText = StringBuilder(editor.document.getText(range))
//
//            val dstTop = region.top
//            val dstBot = region.bottom
//
//            val dstTopOffset = editor.getLineStartOffset(dstTop)
//            val dstBotOffset = editor.getLineEndOffset(dstBot) + 1
//
//            if (scrollAmount > 0) {
//                // scrolling up; add lines below
//                scrollRegionText.append("\n".repeat(scrollAmount))
//            } else {
//                // scrolling down; insert lines above
//                scrollRegionText.insert(0, "\n".repeat(abs(scrollAmount)))
//            }
//
//            // move the scroll region
//            editor.document.replaceString(dstTopOffset, dstBotOffset, scrollRegionText)
//        }
//    }
//
//    @HandlesEvent
//    fun setScrollRegion(event: SetScrollRegionEvent) {
//        currentScrollRegion = event.value.last()
//        System.out.println("setScrollRegion($currentScrollRegion)")
//    }

    private val cursorInDocument: Boolean
        get() = cursorRow < cells.height - 1

    private val cursorOnStatusLine: Boolean
        get() = cursorRow == cells.height - 1

    private val cursorOnExLine: Boolean
        get() = cursorRow == cells.height - 1

//    private val hasDefaultScrollRegion: Boolean
//        get() = currentScrollRegion.isEmpty
//                || (currentScrollRegion.top == 0
//                && currentScrollRegion.left == 0
//                && currentScrollRegion.right == cells.width() - 1
//                && currentScrollRegion.bottom == cells.height() - 1)


    // Is this sufficient?
    private fun getLogicalPosition() = LogicalPosition(cursorRow, cursorCol)

    private fun updateCursor(mode: ModeInfo) {
        val useBlock = (mode.cursorShape == CursorShape.BLOCK)
        editor.settings.isBlockCursor = useBlock
    }

    fun dispatch(ev: NeovimEvent) {
        editDocumentFromVim {
            if (ev is Redraw) {
                for (child in ev.events) {
                    dispatchImpl(child)
                }
                return@editDocumentFromVim
            }

            dispatchImpl(ev)
        }
    }

    private fun dispatchImpl(ev: NeovimEvent) {
        try {
            dispatcher.dispatch(ev)
        } catch (e: Throwable) {
            logger.warning("Error dispatching $ev:\n${e.toStringWithStack()}")
        }
    }

    private inline fun editDocumentFromVim(crossinline edits: () -> Unit) {
        editingDocumentFromVim = true
        inWriteAction {
            runUndoTransparently {
                edits()
            }
        }
        editingDocumentFromVim = false
    }

    private inline fun withoutCaretNotifications(block: () -> Unit) {
        // just remove the caret listener while we adjust the position from nvim
        // to avoid infinite loops
        editor.caretModel.removeCaretListener(caretMovedListener)
        try {
            block()
        } finally {
            editor.caretModel.addCaretListener(caretMovedListener)
        }
    }
}

private fun Component.belongsTo(parentMaybe: JComponent): Boolean {

    var self: Component? = this
    do {
        if (self?.parent == parentMaybe) return true

        self = self?.parent
    } while (self != null)

    return false
}


