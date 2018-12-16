package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.concurrent.atomic.AtomicReference

/**
 * @author dhleong
 */
object EditorManager {

    private var currentEditor = AtomicReference<NeojetEnhancedEditorFacade?>()

    fun getCurrent(): NeojetEnhancedEditorFacade? = currentEditor.get()

    fun setActive(editorFacade: NeojetEnhancedEditorFacade) {
        Disposer.register(editorFacade, Disposable {
            currentEditor.compareAndSet(editorFacade, null)
        })

        currentEditor.set(editorFacade)
    }
}