package org.neojet.ext

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import org.neojet.util.virtualFile

/**
 * @author dhleong
 */
class EditorDataContext(
    private val editor: Editor
) : DataContext {
    override fun getData(dataId: String): Any? = when (dataId) {
        PlatformDataKeys.EDITOR.name -> editor
        PlatformDataKeys.PROJECT.name -> editor.project
        PlatformDataKeys.VIRTUAL_FILE.name -> editor.virtualFile
        else -> null
    }
}

fun Editor.createDataContext(): DataContext = EditorDataContext(this)