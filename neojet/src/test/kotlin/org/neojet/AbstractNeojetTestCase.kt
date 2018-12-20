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
import org.neojet.events.EventDaemon

/**
 * @author dhleong
 */
abstract class AbstractNeojetTestCase : UsefulTestCase() {
    companion object {
        // we use a singleton event daemon because it's stateless anyway,
        // and NJCore is initialized as a singleton across this suite too....
        internal val events = TestableEventDaemon()
    }

    protected lateinit var myFixture: CodeInsightTestFixture

    private val testDataPath: String
        get() = PathManager.getHomePath() + "/community/plugins/neojet/testData"

    protected lateinit var facade: NeojetEnhancedEditorFacade
    private var facadeCreated = false

    override fun setUp() {
        super.setUp()

        facadeCreated = false
        NJCore.eventsFactory = EventDaemon.Factory { events }
        NJCore.providerFactory = createNeovimProviderFactory()

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
        myFixture.tearDown()
        if (facadeCreated) {
            facade.dispose()
        }
    }

    @Suppress("MemberVisibilityCanPrivate")
    protected open fun configureByText(
        content: String,
        fileType: LanguageFileType = PlainTextFileType.INSTANCE
    ): Editor {
        myFixture.configureByText(fileType, content)
        facade = NeojetEnhancedEditorFacade.install(myFixture.editor)
        facadeCreated = true
        return myFixture.editor
    }

    protected fun configureByJavaText(content: String) =
        configureByText(content, JavaFileType.INSTANCE)

    protected fun configureByXmlText(content: String) =
        configureByText(content, XmlFileType.INSTANCE)

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

    fun doTest(before: String, after: String, block: () -> Unit) {
        configureByText(before)

        CommandProcessor.getInstance().executeCommand(myFixture.project, {
            ApplicationManager.getApplication().runWriteAction {
                onPreTest()
                block()
                onPostTest()
            }
        }, "testCommand", "org.neojet")
        myFixture.checkResult(after)
    }

    protected open fun onPreTest() { }
    protected open fun onPostTest() { }

    abstract fun createNeovimProviderFactory(): NeovimProvider.Factory
}
