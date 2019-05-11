package org.neojet.ext

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import org.neojet.NeojetEnhancedEditorFacade
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * This Action is registered on every Enhanced editor in order
 * to suppress the default handling of certain keys that don't
 * respect Normal mode
 *
 * @author dhleong
 */
class VimShortcutKeyAction : AnAction() {

    private var facade: NeojetEnhancedEditorFacade? = null

    init {
        shortcutSet = CustomShortcutSet(
            shortcutFromCode(KeyEvent.VK_BACK_SPACE),
            shortcutFromCode(KeyEvent.VK_ENTER),
            shortcutFromCode(KeyEvent.VK_TAB),
            shortcutFromCode(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        // NOTE: right now we just suppress the event so that the default handling
        // won't change the document when in normal mode (for example) and let the
        // facade's keyEventDispatcher do its thing normally.
        // TODO In the future, it might be nice to let the default handling go
        //  through when we're in insert mode....
    }

    fun install(facade: NeojetEnhancedEditorFacade) {
        if (this.facade === facade) return // already installed
        uninstall()

        this.facade = facade

        val contentComponent = facade.editor.contentComponent
        registerCustomShortcutSet(contentComponent, facade)
    }

    fun uninstall() {
        val facade = facade ?: return // already uninstalled
        this.facade = null

        unregisterCustomShortcutSet(facade.editor.contentComponent)
    }

    fun setInstalled(facade: NeojetEnhancedEditorFacade, isInstalled: Boolean) {
        if (isInstalled) install(facade)
        else uninstall()
    }

}

private fun shortcutFromCode(keyCode: Int, modifiers: Int = 0) =
    KeyboardShortcut(KeyStroke.getKeyStroke(keyCode, modifiers), null)

