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
import io.neovim.rpc.channels.EmbeddedChannel
import kotlinx.coroutines.runBlocking
import org.neojet.events.DefaultEventDaemon
import org.neojet.events.EventDaemon
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
abstract class NeojetIntegrationTestCase : UsefulTestCase() {
    private lateinit var myFixture: CodeInsightTestFixture

    private val testDataPath: String
        get() = PathManager.getHomePath() + "/community/plugins/neojet/testData"

    protected lateinit var facade: NeojetEnhancedEditorFacade

    private lateinit var events: TestableEventDaemon

    override fun setUp() {
        super.setUp()

        events = TestableEventDaemon()

        NJCore.isTestMode = true
        NJCore.eventsFactory = EventDaemon.Factory { events }
        NJCore.providerFactory = DefaultNeovimProvider.Factory(
            EmbeddedChannel.Factory(args = listOf("-u", "NONE"))
        )

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

    override fun tearDown() {
        runBlocking {
            NJCore.instance.nvim.command("bdelete!")
        }
        myFixture.tearDown()
        facade.dispose()

        NJCore.isTestMode = false
        NJCore.providerFactory = NJCore.defaultProviderFactory
        NJCore.eventsFactory = DefaultEventDaemon.Factory()

        super.tearDown()
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
            facade.dispatchTypedKey(KeyEvent(editor.component,
                0, 0, key.modifiers, key.keyCode, key.keyChar
            ))
        }

        events.collectAndDispatch()

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

    fun doTest(before: String, typeKeys: String, after: String) {
        doTest(before, keys(typeKeys), after)
    }

    fun doTest(before: String, type: List<KeyStroke>, after: String) {
        doTest(before, after) {
            typeText(type)
        }
    }

    fun doTest(before: String, after: String, block: () -> Unit) {
        configureByText(before)
        CommandProcessor.getInstance().executeCommand(myFixture.project, {
            ApplicationManager.getApplication().runWriteAction {
                // always ensure we start at the top of the file
                // TODO if we properly ensure the caret is sync'd *to* nvim on buffer open
                // this might be unnecessary
                typeText(keys("gg"))

                block()
            }
        }, "testCommand", "org.neojet")
        myFixture.checkResult(after)
    }

    fun keys(asTyped: String) = asTyped.map {
        KeyStroke.getKeyStroke(it)
    }
}

