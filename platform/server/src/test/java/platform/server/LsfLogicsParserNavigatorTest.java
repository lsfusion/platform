package platform.server;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TestName;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.window.MenuNavigatorWindow;
import platform.server.form.window.PanelNavigatorWindow;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.scripted.ScriptingBusinessLogics;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingFormEntity;
import platform.server.logics.scripted.ScriptingLogicsModule;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static org.junit.Assert.*;

public class LsfLogicsParserNavigatorTest {
    private static final String SCRIPTS_FOLDER = "src/test/resources/testnavigator/";

    @Rule
    public TestName name = new TestName();

    private File testScriptFile;

    private ScriptingBusinessLogics bl;

    private ScriptingLogicsModule LM;

    private ScriptingFormEntity entity;

    private DefaultFormView design;

    @BeforeClass
    public static void setUpTests() throws Exception {
        Settings.instance = new Settings();
        File scriptsFolder = new File(SCRIPTS_FOLDER);
        if (scriptsFolder.exists()) {
            FileUtils.cleanDirectory(scriptsFolder);
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
//        if (testScriptFile != null && testScriptFile.exists()) {
//            testScriptFile.delete();
//        }

        testScriptFile = null;
        bl = null;
        design = null;
    }

    @Test
    public void testNavigatorsHierarchy() throws Exception {
        setupTest("NAVIGATOR {\n" +
                  "    NEW element1 'Element 1' AFTER BaseLogicsModule.userPolicyForm;\n" +
                  "\n" +
                  "    NEW element2 'Element 2' BEFORE userPolicyForm;\n" +
                  "    ADD storeArticle IN element2;\n" +
                  "\n" +
                  "    NEW outer 'Outer element' IN element1 {\n" +
                  "        ADD dictionariesForm 'Dictionaries' {\n" +
                  "            NEW underForm 'under form element';\n" +
                  "        }\n" +
                  "\n" +
                  "        ADD element2 'modif';\n" +
                  "    }\n" +
                  "\n" +
                  "    element2 {\n" +
                  "        NEW element3 'e3';\n" +
                  "    }\n" +
                  "}"
        );

        NavigatorElement elem1 = getElement("element1");
        NavigatorElement elem2 = getElement("element2");
        NavigatorElement elem3 = getElement("element3");
        NavigatorElement storeArticle = getElement("storeArticle");
        NavigatorElement outer = getElement("outer");
        NavigatorElement dictionariesForm = getElement("dictionariesForm");
        NavigatorElement underForm = getElement("underForm");

        assertNotNull(elem1);
        assertNotNull(elem2);
        assertNotNull(elem3);
        assertNotNull(storeArticle);
        assertNotNull(outer);
        assertNotNull(dictionariesForm);
        assertNotNull(underForm);

        assertEquals(dictionariesForm.caption, "Dictionaries");

        assertEquals(elem1.caption, "Element 1");
        assertEquals(elem2.caption, "modif");
        assertEquals(storeArticle.getParent(), elem2);
        assertEquals(elem3.getParent(), elem2);

        assertEquals(underForm.getParent(), dictionariesForm);

        assertEquals(outer.getParent(), elem1);
        assertEquals(dictionariesForm.getParent(), outer);
        assertEquals(elem2.getParent(), outer);
    }

    @Test(expected = RuntimeException.class)
    public void testMoveToSubnavigatorFails() throws Exception {
        setupTest("NAVIGATOR {\n" +
                  "    NEW element1 'Element 1' AFTER userPolicyForm;\n" +
                  "    ADD administration IN element1;\n" +
                  "}"
        );
    }

    @Test
    public void testCreateWindows() throws Exception {
        setupTest("WINDOW PANEL panel1 'Панель 1' HIDETITLE VERTICAL DRAWROOT HIDESCROLLBARS;\n" +
                  "WINDOW PANEL panel2 'Панель 2' HORIZONTAL HIDESCROLLBARS;\n" +
                  "\n" +
                  "WINDOW TREE tree1 'Объекты' HIDESCROLLBARS HIDETITLE ;\n" +
                  "WINDOW TREE tree2 'Объекты' POSITION(56, 23, 200, 123) HIDETITLE;\n" +
                  "\n" +
                  "WINDOW TOOLBAR toolbar1 'Навигатор' VERTICAL POSITION(12, 324, 45, 652) HALIGN(LEFT) VALIGN(TOP) TEXTHALIGN(LEFT) TEXTVALIGN(TOP) DRAWROOT HIDESCROLLBARS;\n" +
                  "WINDOW TOOLBAR toolbar2 'Навигатор' VERTICAL LEFT HALIGN(CENTER) VALIGN(CENTER) HIDETITLE;\n" +
                  "\n" +
                  "WINDOW MENU menu1 'Меню' POSITION(12, 23, 56, 7) VERTICAL LEFT HALIGN(CENTER) VALIGN(CENTER) HIDETITLE;"
        );

        PanelNavigatorWindow panel1 = (PanelNavigatorWindow) LM.getWindowByName("panel1");
        PanelNavigatorWindow panel2 = (PanelNavigatorWindow) LM.getWindowByName("panel2");

        TreeNavigatorWindow tree1 = (TreeNavigatorWindow) LM.getWindowByName("tree1");
        TreeNavigatorWindow tree2 = (TreeNavigatorWindow) LM.getWindowByName("tree2");

        ToolBarNavigatorWindow toolbar1 = (ToolBarNavigatorWindow) LM.getWindowByName("toolbar1");
        ToolBarNavigatorWindow toolbar2 = (ToolBarNavigatorWindow) LM.getWindowByName("toolbar2");

        MenuNavigatorWindow menu1 = (MenuNavigatorWindow) LM.getWindowByName("menu1");

        assertEquals(panel1.orientation, SwingConstants.VERTICAL);
        assertEquals(panel1.caption, "Панель 1");
        assertTrue(panel1.drawRoot);
        assertFalse(panel1.drawScrollBars);
        assertFalse(panel1.titleShown);

        assertEquals(panel2.orientation, SwingConstants.HORIZONTAL);

        assertFalse(tree1.drawScrollBars);
        assertFalse(tree1.titleShown);
        assertFalse(tree2.titleShown);
        assertEquals(tree2.x, 56);
        assertEquals(tree2.y, 23);
        assertEquals(tree2.width, 200);
        assertEquals(tree2.height, 123);

        assertTrue(toolbar1.drawRoot);
        assertFalse(toolbar1.drawScrollBars);
        assertEquals(toolbar1.x, 12);
        assertEquals(toolbar1.y, 324);
        assertEquals(toolbar1.width, 45);
        assertEquals(toolbar1.height, 652);
        assertTrue(toolbar1.alignmentX == JToolBar.LEFT_ALIGNMENT);
        assertTrue(toolbar1.alignmentY == JToolBar.TOP_ALIGNMENT);
        assertTrue(toolbar1.verticalTextPosition == SwingConstants.TOP);
        assertTrue(toolbar1.horizontalTextPosition == SwingConstants.LEADING);

        assertEquals(toolbar2.borderConstraint, BorderLayout.WEST);

        assertEquals(menu1.x, 12);
        assertEquals(menu1.y, 23);
        assertEquals(menu1.width, 56);
        assertEquals(menu1.height, 7);
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateWindowSIDFails() throws Exception {
        setupTest("WINDOW PANEL panel1 'Панель 1' HIDETITLE VERTICAL DRAWROOT HIDESCROLLBARS;\n" +
                  "WINDOW PANEL panel1 'Панель 1' HORIZONTAL HIDESCROLLBARS;"
        );
    }

    @Test(expected = RuntimeException.class)
    public void testToolbarWindowPositionConflictFails() throws Exception {
        setupTest("WINDOW TOOLBAR tb1 'Объекты' VERTICAL LEFT POSITION(12, 23, 32, 45) HIDESCROLLBARS HIDETITLE;");
    }

    @Test
    public void testSetNavigatorWindows() throws Exception {
        setupTest("WINDOW PANEL panel1 'Панель 1' HIDETITLE VERTICAL DRAWROOT HIDESCROLLBARS;\n" +
                  "WINDOW PANEL panel2 'Панель 2' HORIZONTAL HIDESCROLLBARS;\n" +
                  "WINDOW PANEL panel3 'Панель 3' HORIZONTAL HIDESCROLLBARS;\n" +
                  "\n" +
                  "NAVIGATOR {\n" +
                  "    NEW element1 'Element 1' AFTER userPolicyForm TO panel1;\n" +
                  "    NEW element2 'Element 2' TO panel1;\n" +
                  "    ADD storeArticle IN element2;\n" +
                  "\n" +
                  "    storeArticle TO panel3 {\n" +
                  "        NEW underForm 'some';\n" +
                  "    };\n" +
                  "\n" +
                  "    element2 TO panel2;" +
                  "}"
        );

        PanelNavigatorWindow panel1 = (PanelNavigatorWindow) LM.getWindowByName("panel1");
        PanelNavigatorWindow panel2 = (PanelNavigatorWindow) LM.getWindowByName("panel2");
        PanelNavigatorWindow panel3 = (PanelNavigatorWindow) LM.getWindowByName("panel3");

        NavigatorElement elem1 = getElement("element1");
        NavigatorElement elem2 = getElement("element2");
        NavigatorElement underForm = getElement("underForm");
        NavigatorElement storeArticle = getElement("storeArticle");

        assertSame(elem1.window, panel1);
        assertSame(elem2.window, panel2);
        assertSame(storeArticle.window, panel3);

        assertSame(underForm.getParent(), storeArticle);
        assertNull(underForm.window);
    }

    @Test(expected = RuntimeException.class)
    public void testAddNavigatorToSystemWindowFails() throws Exception {
        setupTest("NAVIGATOR {\n" +
                  "    NEW element1 'Element 1' AFTER userPolicyForm TO status;\n" +
                  "}"
        );
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateNavigatorFails() throws Exception {
        setupTest("NAVIGATOR {\n" +
                  "    NEW element1 'Element 1' AFTER userPolicyForm;\n" +
                  "    NEW element1 'Some';" +
                  "}"
        );
    }

    @Test(expected = RuntimeException.class)
    public void testMissingNavigatorsFails() throws Exception {
        setupTest("NAVIGATOR {\n" +
                  "    ADD element1 'Element 1';\n" +
                  "}"
        );
    }

    @Test
    public void testHideWindows() throws Exception {
        setupTest("HIDE WINDOW BaseLogicsModule.status;\n" +
                  "HIDE WINDOW log;"
        );

        assertFalse(LM.baseLM.windows.status.visible);
        assertFalse(LM.baseLM.windows.log.visible);
    }

    private NavigatorElement getElement(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findNavigatorElementByName(name);
    }

    private void setupTest(String testCode) throws Exception {
        String fileContent = FileUtils.readFileToString(new File("src/test/resources/testnavigator.lsf"), "UTF-8") + testCode + "\n";
        testScriptFile = new File(SCRIPTS_FOLDER + name.getMethodName() + ".lsf");
        FileUtils.writeStringToFile(testScriptFile, fileContent, "UTF-8");

        bl = new ScriptingBusinessLogics("scriptedLogicsUnitTest",
                                         new PostgreDataAdapter("scripted_logic_navigator_unittest", "localhost", "postgres", "11111", false),
                                         1234,
                                         testScriptFile.getAbsolutePath());
        bl.afterPropertiesSet();

        LM = (ScriptingLogicsModule) bl.getModule("testNavigator");
        assertNotNull(LM);

        entity = (ScriptingFormEntity) getElement("storeArticle");
        assertNotNull(entity);

        design = (DefaultFormView) entity.richDesign;
        assertNotNull(design);
    }
}
