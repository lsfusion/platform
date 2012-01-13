package platform.server;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TestName;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.scripted.ScriptedBusinessLogics;
import platform.server.logics.scripted.ScriptingFormEntity;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LsfLogicsParserNavigatorTest {
    private static final String SCRIPTS_FOLDER = "src/test/resources/testnavigator/";

    @Rule
    public TestName name = new TestName();

    private File testScriptFile;

    private ScriptedBusinessLogics bl;

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
                  "    ADD element1 'Element 1' AFTER userPolicyForm;\n" +
                  "\n" +
                  "    ADD element2 'Element 2' BEFORE userPolicyForm;\n" +
                  "    ADD storeArticle IN element2;\n" +
                  "\n" +
                  "    ADD outer 'Outer element' IN element1 TO leftWindow {\n" +
                  "        ADD dictionariesForm 'Dictionaries' {\n" +
                  "            ADD underForm 'under form element';\n" +
                  "        }\n" +
                  "\n" +
                  "        ADD element2;\n" +
                  "    }\n" +
                  "\n" +
                  "    element2 {\n" +
                  "        ADD element3;\n" +
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
                  "    ADD element1 'Element 1' AFTER userPolicyForm;\n" +
                  "    ADD adminElement IN element1;\n" +
                  "}"
        );
    }

    private NavigatorElement getElement(String sid) {
        return bl.LM.baseElement.getNavigatorElement(sid);
    }

    private FormEntity getForm(String sid) {
        NavigatorElement element = bl.LM.baseElement.getNavigatorElement(sid);
        assertTrue(element instanceof FormEntity);
        return (FormEntity) element;
    }

    private void setupTest(String testCode) throws Exception {
        String fileContent = FileUtils.readFileToString(new File("src/test/resources/testnavigator.lsf"), "UTF-8") + testCode + "\n";
        testScriptFile = new File(SCRIPTS_FOLDER + name.getMethodName() + ".lsf");
        FileUtils.writeStringToFile(testScriptFile, fileContent, "UTF-8");

        bl = new ScriptedBusinessLogics("scriptedLogicsUnitTest",
                                        new PostgreDataAdapter("scripted_logic_navigator_unittest", "localhost", "postgres", "11111", false),
                                        1234,
                                        testScriptFile.getAbsolutePath());
        bl.afterPropertiesSet();

        LM = (ScriptingLogicsModule) bl.findModule("testNavigator");
        assertNotNull(LM);

        entity = (ScriptingFormEntity) bl.LM.baseElement.getNavigatorElement("storeArticle");
        assertNotNull(entity);

        design = (DefaultFormView) entity.richDesign;
        assertNotNull(design);
    }
}
