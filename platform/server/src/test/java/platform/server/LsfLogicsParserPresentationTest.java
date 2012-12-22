package platform.server;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TestName;
import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.CustomClass;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.scripted.ScriptingBusinessLogics;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingFormEntity;
import platform.server.logics.scripted.ScriptingLogicsModule;

import javax.swing.*;
import java.io.File;

import static java.awt.event.InputEvent.CTRL_MASK;
import static java.awt.event.InputEvent.SHIFT_MASK;
import static java.awt.event.KeyEvent.*;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static platform.base.BaseUtils.single;

public class LsfLogicsParserPresentationTest {
    private static final String SCRIPTS_FOLDER = "src/test/resources/testpresentation/";

    @Rule
    public TestName name = new TestName();

    private File testScriptFile;

    private ScriptingBusinessLogics bl;

    private ScriptingLogicsModule LM;

    private ScriptingFormEntity entity;

    private DefaultFormView design;

    private GroupObjectEntity sGroup;
    private GroupObjectEntity aGroup;

    private ObjectEntity sObject;
    private ObjectEntity aObject;

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
        aObject = null;
        sObject = null;
    }

    private LP findLPBySID(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findLPByCompoundName(sid);
    }

    private Property findPBySID(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return findLPBySID(sid).property;
    }

    private PropertyDrawEntity propDraw(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return entity.getPropertyDraw(findLPBySID(sid));
    }

    private void assertShowIF(PropertyDrawEntity propertyDraw, Property showIfProperty) throws Exception {
        assertEquals(getShowIfProperty(propertyDraw), showIfProperty);
    }

    private Property getShowIfProperty(PropertyDrawEntity propertyDraw) {
        assertNotNull(propertyDraw.propertyCaption);

        assertTrue(propertyDraw.propertyCaption.property instanceof JoinProperty);

        JoinProperty<AndFormulaProperty.Interface> captionProp = (JoinProperty) propertyDraw.propertyCaption.property;
        assertSame(captionProp.implement.property, bl.LM.and1.property);

        AndFormulaProperty andProp = (AndFormulaProperty) captionProp.implement.property;

        CalcPropertyInterfaceImplement<JoinProperty.Interface> andMappedImplement = captionProp.implement.mapping.get(andProp.andInterfaces.single());

        assertTrue(andMappedImplement instanceof CalcPropertyMapImplement);

        return ((CalcPropertyMapImplement) andMappedImplement).property;
    }

    @Test
    public void testObjectEntities() throws Exception {
        setupTest("FORM storeArticle 'По складам'\n" +
                  "OBJECTS s=store 'SSdd', a=article 'AAee', g1=(s1=store 'Some', a1=article 'Any'), a2=article\n"
        );

        ObjectEntity s1 = entity.getObject("s1");
        ObjectEntity a1 = entity.getObject("a1");
        ObjectEntity a2 = entity.getObject("a2");

        GroupObjectEntity g1 = entity.getGroupObject("g1");

        assertNotNull(g1);

        assertEquals(sObject.getCaption(), "SSdd");
        assertEquals(aObject.getCaption(), "AAee");

        assertEquals(s1.getCaption(), "Some");
        assertEquals(a1.getCaption(), "Any");

        assertEquals(a2.getCaption(), "Товар");
    }

    @Test
    public void testDefaultOrders() throws Exception {
        setupTest("FORM storeArticle 'По складам'\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES(s, a) foo, foo2, bar, foobar, bar2, baz, baz2\n" +
                  "ORDER BY foo, bar DESC, foo2 DESC, foobar ASC");

        PropertyDrawEntity fooProp = propDraw("foo");
        PropertyDrawEntity barProp = propDraw("bar");
        PropertyDrawEntity foo2Prop = propDraw("foo2");
        PropertyDrawEntity foobarProp = propDraw("foobar");

        assertEquals(entity.defaultOrders.size(), 4);

        assertEquals(entity.defaultOrders.get(fooProp), true);
        assertEquals(entity.defaultOrders.get(barProp), false);
        assertEquals(entity.defaultOrders.get(foo2Prop), false);
        assertEquals(entity.defaultOrders.get(foobarProp), true);
    }

    @Test
    public void testRegularFilters() throws Exception {
        setupTest("FORM storeArticle\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES(s, a) incomeQuantity COLUMNS (s), incomeQuantity2\n" +
                  "\n" +
                  "FILTERGROUP g1\n" +
                  "    FILTER '1й фильтр' 'F10' NOT NULL bar2(s, a)\n" +
                  "    FILTER 'Другой фильтр' 'ctrl shift F11' NOT NULL baz2(s, a)\n" +
                  "\n" +
                  "FILTERGROUP g2\n" +
                  "    FILTER 'Тоже фильтр' 'F12' NOT NULL storeIsHuge(s)\n" +
                  "    FILTER 'Тоже фильтр2' 'F9' NOT NULL name(a)"
        );

        RegularFilterGroupEntity g1 = entity.getRegularFilterGroup("g1");
        RegularFilterGroupEntity g2 = entity.getRegularFilterGroup("g2");

        assertNotNull(g1);
        assertNotNull(g2);

        assertEquals(g1.filters.size(), 2);
        assertEquals(g2.filters.size(), 2);

        RegularFilterEntity f11 = g1.filters.get(0);
        RegularFilterEntity f12 = g1.filters.get(1);
        RegularFilterEntity f21 = g2.filters.get(0);
        RegularFilterEntity f22 = g2.filters.get(1);

        assertEquals(f11.key, KeyStroke.getKeyStroke(VK_F10, 0));
        assertEquals(f12.key, KeyStroke.getKeyStroke(VK_F11, CTRL_MASK | SHIFT_MASK));
        assertEquals(f21.key, KeyStroke.getKeyStroke(VK_F12, 0));
        assertEquals(f22.key, KeyStroke.getKeyStroke(VK_F9, 0));

        assertEquals(f11.name, "1й фильтр");
        assertEquals(f22.name, "Тоже фильтр2");

        assertThat(f12.filter, instanceOf(NotNullFilterEntity.class));
        assertSame(((NotNullFilterEntity)f12.filter).property.property, findPBySID("baz2"));

        assertThat(f21.filter, instanceOf(NotNullFilterEntity.class));
        assertSame(((NotNullFilterEntity)f21.filter).property.property, findPBySID("storeIsHuge"));
    }

    @Test
    public void testSetPropertiesOnSingleEntities() throws Exception {
        setupTest("FORM storeArticle\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES SELECTION(s) READONLY, name(s) READONLY, OBJVALUE(a), name(a)\n" +
                  "PROPERTIES(s) storeSizeName BACKGROUND storeSize(s)\n" +
                  "PROPERTIES(s, a) incomeQuantity HEADER outcomeQuantity(s, a) READONLY, incomeQuantity2 FOOTER outcomeQuantity2(s, a) SHOWIF outcomeQuantity(s, a)"
        );

        PropertyDrawEntity nameProp = propDraw("name");
        PropertyDrawEntity incProp = propDraw("incomeQuantity");
        PropertyDrawEntity inc2Prop = propDraw("incomeQuantity2");
        PropertyDrawEntity sizeProp = propDraw("storeSizeName");

        assertTrue(nameProp.isReadOnly());
        assertTrue(incProp.isReadOnly());
        assertFalse(inc2Prop.isReadOnly());

        assertEquals(sizeProp.propertyBackground.property, findPBySID("storeSize"));
        assertEquals(incProp.propertyCaption.property, findPBySID("outcomeQuantity"));
        assertEquals(inc2Prop.propertyFooter.property, findPBySID("outcomeQuantity2"));
        //check if showif is ok
        assertShowIF(inc2Prop, findPBySID("outcomeQuantity"));
    }

    @Test
    public void testSetPropertiesOnMultipleEntities() throws Exception {
        setupTest("FORM storeArticle\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES READONLY SELECTION(s), name(s) EDITABLE, OBJVALUE(a), name(a)\n" +
                  "PROPERTIES(s, a) SHOWIF bar(s, a) BACKGROUND outcomeQuantity(s, a) READONLY incomeQuantity, incomeQuantity2 EDITABLE\n" +
                  "PROPERTIES HEADER incomeQuantity(s, a) FOOTER incomeQuantity2(s, a) outcomeQuantity(s, a), outcomeQuantity2(s, a) SHOWIF incomeQuantity2(s, a)"
        );

        PropertyDrawEntity nameProp = propDraw("name");
        PropertyDrawEntity name2Prop = entity.getPropertyDraw(findLPBySID("name"), 1);
        PropertyDrawEntity incProp = propDraw("incomeQuantity");
        PropertyDrawEntity inc2Prop = propDraw("incomeQuantity2");
        PropertyDrawEntity outProp = propDraw("outcomeQuantity");
        PropertyDrawEntity out2Prop = propDraw("outcomeQuantity2");

        //1я строка
        assertFalse(nameProp.isReadOnly());
        assertTrue(name2Prop.isReadOnly());
        assertTrue(incProp.isReadOnly());
        assertFalse(inc2Prop.isReadOnly());

        //2я строка
        //check if showif is ok
        assertShowIF(incProp, findPBySID("bar"));
        assertShowIF(inc2Prop, findPBySID("bar"));

        assertEquals(incProp.propertyBackground.property, findPBySID("outcomeQuantity"));
        assertEquals(inc2Prop.propertyBackground.property, findPBySID("outcomeQuantity"));

        //3я строка
        assertEquals(outProp.propertyCaption.property, findPBySID("incomeQuantity"));
        assertEquals(outProp.propertyFooter.property, findPBySID("incomeQuantity2"));
        assertEquals(out2Prop.propertyFooter.property, findPBySID("incomeQuantity2"));
        assertShowIF(out2Prop, findPBySID("incomeQuantity2"));
    }

    @Test
    public void testSetColumnsGroupObjects() throws Exception {
        setupTest("FORM storeArticle\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES(s, a) incomeQuantity COLUMNS (s), incomeQuantity2"
        );

        PropertyDrawEntity incProp = propDraw("incomeQuantity");
        PropertyDrawEntity inc2Prop = propDraw("incomeQuantity2");

        assertEquals(incProp.getColumnGroupObjects().size(), 1);
        assertSame(incProp.getColumnGroupObjects().single(), sGroup);

        assertEquals(inc2Prop.getColumnGroupObjects().size(), 0);
    }

    @Test
    public void testCustomClassForms() throws Exception {
        setupTest("FORM storeArticle\n" +
                  "OBJECTS s=store, a=article\n" +
                  "PROPERTIES(s, a) incomeQuantity COLUMNS (s), incomeQuantity2\n" +
                  "DIALOG article OBJECT a\n" +
                  "EDIT article OBJECT a\n" +
                  "LIST article OBJECT a\n" +
                  "DIALOG store OBJECT s\n" +
                  "EDIT store OBJECT s\n" +
                  "LIST store OBJECT s"
        );

        CustomClass article = (CustomClass) LM.findClassByCompoundName("article");
        CustomClass store = (CustomClass) LM.findClassByCompoundName("store");

        assertSame(article.getEditForm(LM.baseLM).form, entity);
        assertSame(article.getDialogForm(LM.baseLM).form, entity);
        assertSame(article.getListForm(LM.baseLM).form, entity);

        assertSame(store.getEditForm(LM.baseLM).form, entity);
        assertSame(store.getDialogForm(LM.baseLM).form, entity);
        assertSame(store.getListForm(LM.baseLM).form, entity);


        assertSame(article.getEditForm(LM.baseLM).object, aObject);
        assertSame(article.getDialogForm(LM.baseLM).object, aObject);
        assertSame(article.getListForm(LM.baseLM).object, aObject);

        assertSame(store.getEditForm(LM.baseLM).object, sObject);
        assertSame(store.getDialogForm(LM.baseLM).object, sObject);
        assertSame(store.getListForm(LM.baseLM).object, sObject);
    }

    private void setupTest(String testCode) throws Exception {
        String fileContent = FileUtils.readFileToString(new File("src/test/resources/testpresentation.lsf"), "UTF-8") + testCode + "\n;";
        testScriptFile = new File(SCRIPTS_FOLDER + name.getMethodName() + ".lsf");
        FileUtils.writeStringToFile(testScriptFile, fileContent, "UTF-8");

        bl = new ScriptingBusinessLogics("scriptedLogicsUnitTest",
                                        new PostgreDataAdapter("scripted_logic_presentation_unittest", "localhost", "postgres", "11111", false),
                                        1234,
                                        testScriptFile.getAbsolutePath());
        bl.afterPropertiesSet();

        LM = (ScriptingLogicsModule) bl.getModule("testPresentation");
        assertNotNull(LM);

        entity = (ScriptingFormEntity) LM.findNavigatorElementByName("storeArticle");
        assertNotNull(entity);

        design = (DefaultFormView) entity.richDesign;
        assertNotNull(design);

        aGroup = entity.getGroupObject("a");
        sGroup = entity.getGroupObject("s");

        aObject = entity.getGroupObject("a").objects.iterator().next();
        sObject = entity.getGroupObject("s").objects.iterator().next();
    }
}
