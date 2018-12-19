package org.neojet

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import io.neovim.log
import kotlinx.coroutines.runBlocking
import org.neojet.nvim.NvimWrapper
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
abstract class NeojetTestCase : UsefulTestCase() {
    private lateinit var myFixture: CodeInsightTestFixture

    private val testDataPath: String
        get() = PathManager.getHomePath() + "/community/plugins/neojet/testData"

    protected lateinit var facade: NeojetEnhancedEditorFacade

//    protected lateinit var packetsChannel: DummyPacketsChannel
//    protected lateinit var rpc: Rpc
//    protected lateinit var responseIds: IdAllocator

    override fun setUp() {
        super.setUp()

//        responseIds = SimpleIdAllocator()
//        packetsChannel = DummyPacketsChannel()
//        rpc = Rpc(packetsChannel)
        enqueueResponse() // setlocal nolist
        enqueueResponse() // laststatus=0
        enqueueResponse() // uiAttach()
        enqueueResponse() // e! $file
        enqueueResponse(
            // getCurrentBuf
//            Buffer.create(rpc, 1)
        )
        enqueueResponse(true) // buffer.attach()

        // FIXME in test mode we load files that don't exist on the disk,
        // so without feeding the buffer to vim somehow (which we *could* do)
        // things explode with real vim...
        NJCore.isTestMode = true
//        NvimWrapper.rpcFactory = { rpc }

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor)
        val fixture = fixtureBuilder.fixture
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
            LightTempDirTestFixtureImpl(true)
        ).also {

            it.setUp()
            it.testDataPath = testDataPath
        }
    }

    protected fun enqueueResponse(result: Any? = null) {
//        packetsChannel.enqueueIncoming(ResponsePacket(
//            requestId = responseIds.next(),
//            result = result
//        ))
    }

    override fun tearDown() {
        log("tearDown")
        enqueueResponse(true) // buffer.detach
        enqueueResponse() // uiDetach
        runBlocking {
            NJCore.instance.nvim().command("bdelete!")
        }
        myFixture.tearDown()
        facade.dispose()

        NJCore.isTestMode = false
        NvimWrapper.rpcFactory = null

        super.tearDown()
    }

    protected fun typeTextInFile(keys: List<KeyStroke>, fileContents: String): Editor {
        configureByText(fileContents)
        return typeText(keys)
    }

    @Suppress("MemberVisibilityCanPrivate")
    protected fun configureByText(
        content: String,
        fileType: LanguageFileType = PlainTextFileType.INSTANCE
    ): Editor {
        myFixture.configureByText(fileType, content)
        facade = NeojetEnhancedEditorFacade.install(myFixture.editor)
        return myFixture.editor
    }

    protected fun configureByJavaText(content: String) =
        configureByText(content, JavaFileType.INSTANCE)

    protected fun configureByXmlText(content: String) =
        configureByText(content, XmlFileType.INSTANCE)

    @Suppress("MemberVisibilityCanPrivate")
    protected fun typeText(keys: List<KeyStroke>): Editor {
        val editor = myFixture.editor
        for (key in keys) {
            enqueueResponse(1)
            facade.dispatchTypedKey(KeyEvent(editor.component,
                0, 0, key.modifiers, key.keyCode, key.keyChar
            ))
        }

        // give neovim some time to send us events
        Thread.sleep(1000)

        val events = NJCore.instance.queuedEvents.toList()
        NJCore.instance.queuedEvents.clear()
        println("${events.size} queued events waiting")
        events.forEach {
            println("dispatch>> $it")
            facade.dispatch(it)
        }

        return editor
    }

    fun assertOffset(vararg expectedOffsets: Int) {
        val carets = myFixture.editor.caretModel.allCarets
        assertEquals("Wrong amount of carets", expectedOffsets.size, carets.size)
        for (i in expectedOffsets.indices) {
            assertEquals(expectedOffsets[i], carets[i].offset)
        }
    }

    fun assertSelection(expected: String?) {
        val selected = myFixture.editor.selectionModel.selectedText
        assertEquals(expected, selected)
    }

    fun doTest(keys: List<KeyStroke>, before: String, after: String) {
        doTest(before, after) {
            typeText(keys)
        }
    }

    fun doTest(before: String, after: String, block: () -> Unit) {
        log("doTest")
        configureByText(before)
        log("doTest:configured")
        CommandProcessor.getInstance().executeCommand(myFixture.project, {
            ApplicationManager.getApplication().runWriteAction {
                log("<< block")
                block()
                log(">> block")
            }
        }, "testCommand", "org.neojet")
        myFixture.checkResult(after)
    }

    fun keys(asTyped: String) = asTyped.map {
        KeyStroke.getKeyStroke(it)
    }
}

