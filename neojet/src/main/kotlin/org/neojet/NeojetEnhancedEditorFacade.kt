package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
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
import kotlinx.coroutines.channels.Channel
import org.neojet.events.EventDispatchTarget
import org.neojet.events.EventDispatcher
import org.neojet.events.HandlesEvent
import org.neojet.ext.VimShortcutKeyAction
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

            // suppress default handling of special keys so we
            // can let Vim handle it (at least... in normal mode)
            VimShortcutKeyAction.registerCustomShortcutSet(
                editor.contentComponent,
                facade
            )

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
        if (!isForOurComponent) {
            // not for our editor; ignore
            return@KeyEventDispatcher false
        }

        when (it.id) {
            KeyEvent.KEY_TYPED -> {
                if (it.modifiers == 0) {
                    // ONLY handle 0-modifier keys typed *here*
                    dispatchTypedKey(it)
                }
                true // consume
            }

            KeyEvent.KEY_PRESSED -> {
                // if there are no modifiers it should be handled by the
                // KEY_TYPED branch above (?)
                if (it.modifiers == 0) return@KeyEventDispatcher true

                dispatchTypedKey(it)
                true
            }

            else -> true
        }
    }

    // var so it can be replaced in integration tests
    var dispatchTypedKey: (KeyEvent) -> Unit = { ev ->
        corun {
            nvim.input(ev)
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
    private val readyChannel = Channel<Unit>(Channel.CONFLATED)

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

        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) {
                if (editingDocumentFromVim) return

                // IntelliJ did some extra edits to the document
                println("Document changed @${e.offset}: `${e.oldFragment}` -> `${e.newFragment}`")
                val startLine = e.document.getLineNumber(e.offset)
                val inputEndLine = e.document.getLineNumber(e.offset + e.oldLength)
                val outputEndLine = e.document.getLineNumber(e.offset + e.newLength)
                println("${e.oldLength} -> ${e.newLength} @[$startLine, $inputEndLine] -> [$startLine, $outputEndLine]")

                val lines = e.document.getLines(startLine, outputEndLine)
                val buffer = editor.buffer ?: throw IllegalStateException("No buffer set")
                corun {
                    buffer.setLines(
                        start = startLine.toLong(),
                        end = when {
                            e.oldLength == 0 -> inputEndLine.toLong()
                            else -> inputEndLine.toLong() + 1
                        },
                        strictIndexing = false,
                        replacement = lines
                    )
                }
            }
        }, this)
    }

    override fun dispose() {
        editor.caretModel.removeCaretListener(caretMovedListener)
    }

    /*
     * Notifications from nvim
     */

    @HandlesEvent
    fun bufferLinesChanged(event: BufLinesEvent) {
//        val linesRaw = event.linedata.map { "'$it'" }.toString()
//        println(event)
//        println(" -> lines: $linesRaw")

        val replacement = StringBuilder().apply {
            for (line in event.linedata) {
                append(line)
                append("\n")
            }
        }.toString()

        // this seems very wacky:
        val startOffset = editor.getLineStartOffset(event.firstline.toInt())
        val endOffset = maxOf(
            startOffset,
            editor.getLineStartOffset(event.lastline.toInt()) - 1
        )
        val isInsert = event.firstline == event.lastline

        when {
            isInsert && event.firstline.toInt() == editor.lineCount -> {
                editor.document.insertString(editor.document.textLength, replacement)
            }

            isInsert -> {
                editor.document.insertString(endOffset, replacement)
            }

            else -> {
                val atEnd = event.lastline >= editor.lineCount.toLong()
                val atStart = event.firstline == 0L
                val replaceStart =
                    // if we're deleting at the end of the document, start one earlier
                    // to clear the newline
                    if (atEnd && !atStart) startOffset - 1
                    else startOffset
                val replaceEnd =
                    if (atEnd) editor.document.textLength
                    else endOffset + 1

                editor.document.replaceString(
                    maxOf(0, replaceStart),
                    replaceEnd,

                    when {
                        atEnd -> replacement.substringBeforeLast("\n")
                        else -> replacement
                    }
                )
            }
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

//            val lineEndOffset = editor.getLineEndOffset(newLogicalPosition.line)
//            val lineLength = lineEndOffset - editor.getLineStartOffset(newLogicalPosition.line)
//            val endDiff = cursorCol - lineLength
//            if (endDiff > 0) {
//                // this implies inserting spaces
//                editor.document.insertString(lineEndOffset, " ".repeat(endDiff))
//            }

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

    fun setReady() {
        readyChannel.offer(Unit)
    }

    /**
     * Mostly used by tests to wait for this facade to be ready to use...
     */
    suspend fun awaitReady() = readyChannel.receive()

    private fun dispatchImpl(ev: NeovimEvent) {
        try {
            dispatcher.dispatch(ev)
        } catch (e: Throwable) {
            logger.warning("Error dispatching $ev:\n${e.toStringWithStack()}")
        }
    }

    private inline fun editDocumentFromVim(crossinline edits: () -> Unit) {
        inWriteAction {
            editingDocumentFromVim = true
            runUndoTransparently {
                edits()
            }
            editingDocumentFromVim = false
        }
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


