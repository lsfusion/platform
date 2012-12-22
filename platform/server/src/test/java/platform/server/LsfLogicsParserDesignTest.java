package platform.server;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TestName;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.view.ComponentView;
import platform.server.form.view.ContainerView;
import platform.server.form.view.GroupObjectView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LP;
import platform.server.logics.scripted.ScriptingBusinessLogics;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingFormView;
import platform.server.logics.scripted.ScriptingLogicsModule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import static org.junit.Assert.*;

public class LsfLogicsParserDesignTest {
    private static final String SCRIPTS_FOLDER = "src/test/resources/testdesign/";

    @Rule
    public TestName name = new TestName();

    private File testScriptFile;

    private ScriptingBusinessLogics bl;

    private ScriptingLogicsModule LM;

    private FormEntity entity;

    private ScriptingFormView design;

    private GroupObjectView sGroup;
    private GroupObjectView aGroup;

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
        aGroup = null;
        sGroup = null;
    }

    @Test
    public void testEmptyStatements() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    GROUP(s) {\n" +
                  "        ;; tableRowsCount = 10;\n" +
                  "        needVerticalScroll = FALSE;\n" +
                  "    ;;};;\n" +
                  "\n" +
                  "    PROPERTY(bar) {\n" +
                  "        hide = TRUE;;;\n" +
                  "        ;;\n" +
                  "    };;\n" +
                  "\n" +
                  "}");
    }

    @Test
    public void testDefaultDesignCreation() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT;");

        assertNotNull(design.getComponentBySID("main"));

        assertNotNull(design.getComponentBySID("a.box"));
        assertNotNull(design.getComponentBySID("a.panel"));
        assertNotNull(design.getComponentBySID("a.grid"));
        assertNotNull(design.getComponentBySID("a.grid.box"));
        assertNotNull(design.getComponentBySID("a.showType"));
        assertNotNull(design.getComponentBySID("a.classChooser"));
        assertNotNull(design.getComponentBySID("a.controls"));
        assertNotNull(design.getComponentBySID("a.filters"));

        assertNotNull(design.getComponentBySID("a.base"));

        assertNotNull(design.getComponentBySID("functions.box"));

        assertNotNull(design.getComponentBySID("functions.print"));
        assertNotNull(design.getComponentBySID("functions.edit"));
        assertNotNull(design.getComponentBySID("functions.xls"));
        assertNotNull(design.getComponentBySID("functions.null"));
        assertNotNull(design.getComponentBySID("functions.apply"));
        assertNotNull(design.getComponentBySID("functions.cancel"));
        assertNotNull(design.getComponentBySID("functions.ok"));

        assertTrue(design.getComponentBySID("a.panel") instanceof ComponentView);
        assertFalse(design.getComponentBySID("a.grid") instanceof ContainerView);
    }

    @Test
    public void testSkipDefaultDesignCreation() throws Exception {
        setupTest("DESIGN storeArticle;", false);

        assertNotNull(design.getComponentBySID("main"));

        assertNotNull(design.getComponentBySID("a.grid"));
        assertNotNull(design.getComponentBySID("a.showType"));
        assertNotNull(design.getComponentBySID("a.classChooser"));

        assertNotNull(design.getComponentBySID("functions.print"));
        assertNotNull(design.getComponentBySID("functions.edit"));
        assertNotNull(design.getComponentBySID("functions.xls"));
        assertNotNull(design.getComponentBySID("functions.null"));
        assertNotNull(design.getComponentBySID("functions.apply"));
        assertNotNull(design.getComponentBySID("functions.cancel"));
        assertNotNull(design.getComponentBySID("functions.ok"));

        assertNull(design.getComponentBySID("functions.box", false));

        assertNull(design.getComponentBySID("a.box", false));
        assertNull(design.getComponentBySID("a.panel", false));
        assertNull(design.getComponentBySID("a.grid.box", false));
        assertNull(design.getComponentBySID("a.controls", false));
        assertNull(design.getComponentBySID("a.filters", false));
    }

    @Test
    public void testSetFormViewProperties() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    title='some2';\n" +
                  "    overridePageWidth=12;\n" +
                  "}");

        assertEquals(design.caption, "some2");
        assertEquals((long) design.overridePageWidth, 12);
    }

    @Test
    public void testSetGroupViewProperties() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    GROUP(s) {\n" +
                  "        tableRowsCount = 10;\n" +
                  "        needVerticalScroll = FALSE;\n" +
                  "    }\n" +
                  "}");

        assertEquals((long) sGroup.tableRowsCount, 10);
        assertEquals(sGroup.needVerticalScroll, false);
    }

    @Test(expected = RuntimeException.class)
    public void testSetUnknownProperties() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    caption23='some';\n" +
                  "}");
    }

    @Test
    public void testSetPropertyDrawViewProperties() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        autoHide = TRUE;\n" +
                  "        showTableFirst = TRUE;\n" +
                  "        editOnSingleClick = TRUE;\n" +
                  "        hide = TRUE;\n" +
                  "        regexp = '[\\d]+';\n" +
                  "        regexpMessage = 'regexpmsg';\n" +
                  "    }\n" +
                  "\n" +
                  "    PROPERTY(storeSizeName(s)) {\n" +
                  "        echoSymbols = FALSE;\n" +
                  "        minimumSize = (321, 123);" +
                  "    }\n" +
                  "\n" +
                  "    PROPERTY(name(a)) {\n" +
                  "        minimumCharWidth = 11;\n" +
                  "        maximumCharWidth = 12;\n" +
                  "        preferredCharWidth = 13;\n" +
                  "        showEditKey = TRUE;\n" +
                  "    }\n" +
                  "\n" +
                  "    PROPERTY(foo(s, a)) {\n" +
                  "        focusable = FALSE;\n" +
                  "        panelLabelAbove = TRUE;\n" +
                  "        caption = 'This is bar\\'s caption!';\n" +
                  "        clearText = TRUE;\n" +
                  "        childConstraints = TO THE LEFT;\n" +
                  "        insetsInside = (100, 223  , -  123, 123);\n" +
                  "    }\n" +
                  "}");

        PropertyDrawView barView = design.get(entity.getPropertyDraw(findLPBySID("bar")));
        PropertyDrawView storeSizeView = design.get(entity.getPropertyDraw(findLPBySID("storeSizeName")));
        PropertyDrawView nameView = design.get(entity.getPropertyDraw(findLPBySID("name"), 1));
        PropertyDrawView fooView = design.get(entity.getPropertyDraw(findLPBySID("foo")));

        assertTrue(barView.autoHide);
        assertTrue(barView.showTableFirst);
        assertTrue(barView.editOnSingleClick);
        assertTrue(barView.hide);
        assertEquals(barView.regexp, "[\\d]+");

        assertEquals(storeSizeView.minimumSize, new Dimension(321, 123));

        assertEquals(nameView.getMinimumCharWidth(), 11);
        assertEquals(nameView.getMaximumCharWidth(), 12);
        assertTrue(nameView.showEditKey);

        assertEquals(fooView.caption, "This is bar's caption!");
        assertEquals(fooView.constraints.childConstraints, DoNotIntersectSimplexConstraint.TOTHE_LEFT);
        assertEquals(fooView.constraints.insetsInside, new Insets(100, 223, -123, 123));
    }

    @Test
    public void testSetPropertiesWithCustomConverters() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        editKey = 'alt shift X';\n" +
                  "        headerFont = 'Tahoma bold 15';\n" +
                  "        font = '12 Tahoma italic bold';\n" +
                  "    }\n" +
                  "}");

        PropertyDrawView barView = design.get(entity.getPropertyDraw(findLPBySID("bar")));

        assertEquals(barView.editKey, KeyStroke.getKeyStroke("alt shift X"));
        assertEquals(barView.editKey, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
        assertEquals(barView.design.headerFont, new Font("Tahoma", Font.BOLD, 15));
        assertEquals(barView.design.font, new Font("Tahoma", Font.BOLD | Font.ITALIC, 12));
    }

    @Test
    public void testContainers1() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    ADD root {\n" +
                  "        title = 'The One';\n" +
                  "        ADD child1 {\n" +
                  "            background = #1232AC;\n" +
                  "        };\n" +
                  "        ADD child2 {\n" +
                  "            ADD child21 { title = 'sdf'; }\n" +
                  "        }\n" +
                  "        ADD child3 BEFORE child1 {\n" +
                  "            ADD child31 {\n" +
                  "                title = 'sdf';\n" +
                  "                ADD child311 { title = 'sdfsdf'; }\n" +
                  "            }\n" +
                  "            ADD child32 { title = 'sdf'; }\n" +
                  "            POSITION child1 TO NOT INTERSECT;\n" +
                  "        }\n" +
                  "\n" +
                  "        ADD functions.box AFTER child3;\n" +
                  "    }\n" +
                  "    REMOVE child2 CASCADE;\n" +
                  "    REMOVE child31;\n" +
                  "}");

        ContainerView root = getContainer("root");
        ContainerView child1 = getContainer("child1");
        ComponentView child2 = getComponent("child2");
        ComponentView child21 = getComponent("child21");
        ContainerView child3 = getContainer("child3");
        ComponentView child31 = getComponent("child31");
        ContainerView child311 = getContainer("child311");
        ContainerView child32 = getContainer("child32");

        assertNotNull(root);
        assertNotNull(child1);
        assertNull(child2);
        assertNull(child21);
        assertNotNull(child3);
        assertNull(child31);
        assertNotNull(child32);
        assertNotNull(child311);
        assertNotNull(child32);

        assertEquals(root.getChildren().size(), 3);
        assertSame(root.getChildren().get(0), child3);
        assertSame(root.getChildren().get(1), getComponent("functions.box"));
        assertSame(root.getChildren().get(2), child1);

        assertEquals(child3.constraints.intersects.get(child1), DoNotIntersectSimplexConstraint.DO_NOT_INTERSECT);
    }

    @Test
    public void testDesignForJavaForm() throws Exception {
        setupTest("DESIGN dictionariesForm FROM DEFAULT {\n" +
                  "    dict.panel {\n" +
                  "        ADD some {\n" +
                  "            caption = 'Some wrapper for the name';\n" +
                  "            ADD PROPERTY(commonName);\n" +
                  "        }\n" +
                  "    }\n" +
                  "}");

        FormEntity form = (FormEntity) LM.findNavigatorElementByName("dictionariesForm");

        ScriptingFormView design = (ScriptingFormView) form.getRichDesign();
        assertNotNull(design);

        ComponentView some = design.getComponentBySID("some");
        PropertyDrawView nameView = design.get(form.getPropertyDraw(findLPBySID("commonName")));

        assertNotNull(some);
        assertNotNull(nameView);

        assertSame(nameView.getContainer(), some);
    }

    @Test(expected = RuntimeException.class)
    public void testMoveToSubcontainerFails() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    main {\n" +
                  "        ADD outer {\n" +
                  "            title = '7+2+3';\n" +
                  "            ADD s.box;\n" +
                  "            ADD inner {\n" +
                  "                title = '2+3';\n" +
                  "                ADD a.box;\n" +
                  "                ADD s.box;\n" +
                  "                ADD outer;\n" +
                  "            }\n" +
                  "        }\n" +
                  "    }\n" +
                  "}");
    }

    @Test(expected = RuntimeException.class)
    public void testSetKeyStrokePropertyFails() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        editKey = 'alt shift Xdf';\n" +
                  "    }\n" +
                  "}");
    }

    @Test(expected = RuntimeException.class)
    public void testSetFontPropertyFails1() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        font = 'alt shift Xdf';\n" +
                  "    }\n" +
                  "}");
    }

    @Test(expected = RuntimeException.class)
    public void testSetFontPropertyFails2() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        font = '12 italic bold name1 name2';\n" +
                  "    }\n" +
                  "}");
    }

    @Test(expected = RuntimeException.class)
    public void testSetFontPropertyFails3() throws Exception {
        setupTest("DESIGN storeArticle FROM DEFAULT {\n" +
                  "    PROPERTY(bar) {\n" +
                  "        font = '-12 italic Tahoma';\n" +
                  "    }\n" +
                  "}");
    }

    private LP findLPBySID(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findLPByCompoundName(sid);
    }

    private ComponentView getComponent(String sid) throws Exception {
        return design.getComponentBySID(sid, false);
    }

    private ContainerView getContainer(String sid) throws Exception {
        ComponentView component = design.getComponentBySID(sid, false);
        assertTrue(component instanceof ContainerView);
        return (ContainerView) component;
    }

    private void setupTest(String testCode) throws Exception {
        setupTest(testCode, true);
    }

    private void setupTest(String testCode, boolean hasDefault) throws Exception {
        String fileContent = FileUtils.readFileToString(new File("src/test/resources/testdesign.lsf"), "UTF-8") + testCode;
        testScriptFile = new File(SCRIPTS_FOLDER + name.getMethodName() + ".lsf");
        FileUtils.writeStringToFile(testScriptFile, fileContent, "UTF-8");

        bl = new ScriptingBusinessLogics("scriptedLogicsUnitTest",
                                        new PostgreDataAdapter("scripted_logic_design_unittest", "localhost", "postgres", "11111", false),
                                        1234,
                                        testScriptFile.getAbsolutePath());
        bl.afterPropertiesSet();

        LM = (ScriptingLogicsModule) bl.getModule("testDesign");
        assertNotNull(LM);

        entity = (FormEntity) LM.findNavigatorElementByName("storeArticle");
        assertNotNull(entity);

        design = (ScriptingFormView) entity.getRichDesign();
        assertNotNull(design);

        if (hasDefault) {
            aGroup = design.getGroupObject("a");
            sGroup = design.getGroupObject("s");
        }
    }
}
