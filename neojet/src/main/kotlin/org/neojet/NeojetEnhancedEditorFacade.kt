package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import io.neovim.NeovimApi
import io.neovim.events.CursorGoto
import io.neovim.events.NeovimEvent
import io.neovim.events.Redraw
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

            val facade = NeojetEnhancedEditorFacade(editor)
            editor.putUserData(neojetEnhancedEditorKey, facade)

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
            if (!movingCursor) {
                val line = ev.newPosition.line + 1 // 0-indexed to 1-indexed
                val column = ev.newPosition.column

                corun {
                    nvim.getCurrentWin()
                        .setCursor(IntPair(line, column))
                }
            }
        }
    }

    private val logger: Logger = Logger.getLogger("NeoJet:EditorFacade")

    var cells = editor.getTextCells()

    val nvim: NeovimApi = NJCore.instance.attach(editor, this)
    private val dispatcher = EventDispatcher(this)

//    private lateinit var modes: List<ModeInfo>
//    private var mode: ModeInfo? = null

    var editingDocumentFromVim = false
    var movingCursor = false
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

//        subs.add(
//            nvim.bufferedRedrawEvents()
//                .subscribe(this::dispatchRedrawEvents)
//        )
    }

    override fun dispose() {
//        subs.dispose()

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

//    @Suppress("UNUSED_PARAMETER")
//    @HandlesEvent
//    fun clearToEol(event: EolClearEvent) {
//        if (DumbService.getInstance(editor.project!!).isDumb) return
//
//        if (cursorOnStatusLine) {
//            // TODO deal with status line?
//            System.out.println("Drop CEOL on status")
//            return
//        } else if (cursorOnExLine) {
//            // ??
//            System.out.println("Clear EX to EOL after $cursorCol")
//            exBuffer.deleteAfter(cursorCol)
//            showStatusMessage(
//                exBuffer.getLines().joinToString("\n")
//            )
//            return
//        }
//
//        val logicalPosition = getLogicalPosition()
//        val start = editor.logicalPositionToOffset(logicalPosition)
//        val lineEndOffset = editor.getLineEndOffset(logicalPosition.line)
//        val end = minOf(
//            editor.document.textLength - 1,
//            lineEndOffset
//        )
//
//        if (end < start) {
//            // usually for drawing status line, etc.
//            return
//        }
//
//        System.out.println("ClearEOL after $cursorCol @${logicalPosition.line}")
//        editor.document.deleteString(start, end)
//    }

    @HandlesEvent
    fun cursorMoved(event: CursorGoto) {
        movingCursor = true

        cursorRow = event.row.toInt()
        cursorCol = event.col.toInt()
        System.out.println("CursorGoto($cursorRow, $cursorCol)")

        if (cursorInDocument) {
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

        movingCursor = false
    }

//    @HandlesEvent
//    fun modeInfoSet(event: ModeInfoSet) {
//        modes = event.value[0].modes
//    }

//    @HandlesEvent
//    fun modeChange(event: ModeChange) {
//        event.mode
//        modes[event.value[0].modeIndex].let {
//            mode = it
//
//            updateCursor(it)
//        }
//    }

//    @HandlesEvent
//    fun put(event: PutEvent) {
//        if (DumbService.getInstance(editor.project!!).isDumb) return
//
//        val line = cursorRow
//        val lineText = event.bytesToCharSequence()
//        if (cursorOnStatusLine) {
//            // TODO deal with status line?
//            System.out.println("Drop put @$line (cells.height=${cells.height()})")
//            return
//        } else if (cursorOnExLine) {
//            // ??
//            System.out.println("EX put @$cursorCol: $lineText")
//            exBuffer.put(cursorCol, lineText)
//            cursorCol += lineText.length
//            showStatusMessage(
//                exBuffer.getLines().joinToString("\n")
//            )
//            return
//        }
//
//        exBuffer.isActive = false
//        if (line > editor.lastLine
//            && event.value.none { it.value !in setOf('~', '\n') }) {
//            System.out.println("Ignore 'no line' placeholders")
//            return
//        }
//
//        val lineEndOffset = editor.getLineEndOffset(line, clamp = false)
//        val start = editor.logicalPositionToOffset(LogicalPosition(line, cursorCol))
//        val delta = lineEndOffset - start
//        val end = minOf(
//            editor.document.textLength - 1,
//            start + minOf(event.value.size, delta)
//        )
//
//        if (lineText.matches(Regex("~[ ]+$"))) {
//            // delete start - 1 to get the \n
//            System.out.println("DELETE LINE @$line")
//            editor.document.deleteString(maxOf(0, start - 1), end)
//            return
//        }
//
//        if (lineEndOffset < start) {
//            // usually for drawing status line, etc.
//            System.out.println("Ignore put @$line,$cursorCol: `$lineText`")
//            return
//        }
//
//        // TODO better checking for deletes at end of document
//        if (start == end) {
//            System.out.println("INSERT($start) <- `$lineText`")
//            editor.document.insertString(start, lineText)
//        } else {
//            System.out.println("REPLACE($start, $end) <- $lineText")
//            editor.document.replaceString(start, end, lineText)
//        }
//        cursorCol += event.value.size
//    }
//
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
        get() = cursorRow < cells.height - 2

    private val cursorOnStatusLine: Boolean
        get() = cursorRow == cells.height - 2

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

//    private fun updateCursor(mode: ModeInfo) {
//        val useBlock = (mode.cursorShape == ModeInfo.CursorShape.BLOCK)
//        editor.settings.isBlockCursor = useBlock
//    }

//    private fun dispatchRedrawEvents(events: List<RedrawSubEvent<*>>) {
//        editDocumentFromVim {
//            events.forEach(dispatcher::dispatch)
//        }
//    }

    private inline fun editDocumentFromVim(crossinline edits: () -> Unit) {
        editingDocumentFromVim = true
        inWriteAction {
            runUndoTransparently {
                edits()
            }
        }
        editingDocumentFromVim = false
    }

    fun dispatch(ev: NeovimEvent) {
        editDocumentFromVim {
            if (ev is Redraw) {
                for (child in ev.events) {
                    dispatch(child)
                }
                return@editDocumentFromVim
            }

            try {
                dispatcher.dispatch(ev)
            } catch (e: Throwable) {
                logger.warning("Error dispatching $ev:\n${e.toStringWithStack()}")
            }
        }
    }
}


//private fun PutEvent.bytesToCharSequence(): CharSequence {
//    return value.fold(StringBuilder(value.size), { buffer, value ->
//        buffer.append(value.value)
//    })
//}

private fun Component.belongsTo(parentMaybe: JComponent): Boolean {

    var self: Component? = this
    do {
        if (self?.parent == parentMaybe) return true

        self = self?.parent
    } while (self != null)

    return false
}


