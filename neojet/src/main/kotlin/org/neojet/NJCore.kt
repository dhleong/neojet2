package org.neojet

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.util.Disposer
import io.neovim.rpc.channels.FallbackChannelFactory
import io.neovim.rpc.channels.SocketChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.neojet.nvim.NvimWrapper
import java.util.logging.Logger

/**
 * @author dhleong
 */
class NJCore : BaseComponent, Disposable {

    val logger = Logger.getLogger("neojet:NJCore")

    var nvim = NvimWrapper(FallbackChannelFactory(
        SocketChannel.Factory("localhost", 7777)
    ))

    override fun getComponentName(): String = COMPONENT_NAME

    override fun initComponent() {
        super.initComponent()

        GlobalScope.launch {
            val line = nvim().getCurrentLine()
            logger.info("Current line = $line")
        }
    }

    override fun disposeComponent() {
        nvim.close()
        super.disposeComponent()
    }

    override fun dispose() {
        Disposer.dispose(this)
    }

    companion object {
        const val COMPONENT_NAME = "NJCore"

        val instance: NJCore
            get() = ApplicationManager.getApplication()
                .getComponent(NJCore::class.java)

        var isTestMode: Boolean = false
    }
}
