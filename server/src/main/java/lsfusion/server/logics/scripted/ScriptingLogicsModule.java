package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.base.IOUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.UpClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.AbstractClassWhere;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.window.*;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.Event;
import lsfusion.server.logics.property.actions.BaseEvent;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.mail.AttachmentFormat;
import lsfusion.server.mail.SendEmailActionProperty;
import lsfusion.server.mail.SendEmailActionProperty.FormStorageType;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.logics.PropertyUtils.*;
import static lsfusion.server.logics.NamespaceElementFinder.FoundItem;
import static lsfusion.server.logics.scripted.AlignmentUtils.*;
import static lsfusion.server.logics.scripted.ScriptingFormEntity.getPropertyDraw;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {

    private final static Logger scriptLogger = ServerLoggers.scriptLogger;

    private final CompoundNameResolver<LP<?, ?>, List<AndClassSet>> lpResolver = new LPResolver(new SoftLPNameModuleFinder());
    private final CompoundNameResolver<LP<?, ?>, ?> lpOldResolver = new LPResolver(new OldLPNameModuleFinder());
    private final CompoundNameResolver<AbstractGroup, ?> groupResolver = new CompoundNameResolver<AbstractGroup, Object>(new GroupNameModuleFinder());
    private final CompoundNameResolver<NavigatorElement, ?> navigatorResolver = new CompoundNameResolver<NavigatorElement, Object>(new NavigatorElementNameModuleFinder());
    private final CompoundNameResolver<AbstractWindow, ?> windowResolver = new CompoundNameResolver<AbstractWindow, Object>(new WindowNameModuleFinder());
    private final CompoundNameResolver<ImplementTable, ?> tableResolver = new CompoundNameResolver<ImplementTable, Object>(new TableNameModuleFinder());
    private final CompoundNameResolver<CustomClass, ?> classResolver = new CompoundNameResolver<CustomClass, Object>(new ClassNameModuleFinder());

    private final BusinessLogics<?> BL;

    private String code = null;
    private String filename = null;
    private String path = null;
    private List<String> namespacePriority;
    private final ScriptingErrorLog errLog;
    private ScriptParser parser;
    private List<String> warningList = new ArrayList<String>();
    private Map<Property, String> alwaysNullProperties = new HashMap<Property, String>();

    private String lastOpimizedJPropSID = null;

    private Set<LP<?, ?>> currentLocalProperties = new HashSet<LP<?, ?>>();

    private Map<String, List<LogicsModule>> namespaceToModules = new LinkedHashMap<String, List<LogicsModule>>();

    public enum ConstType { STATIC, INT, REAL, NUMERIC, STRING, LOGICAL, LONG, DATE, DATETIME, TIME, COLOR, NULL }
    public enum InsertPosition {IN, BEFORE, AFTER, FIRST}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, AGGR, EQUAL, LAST, NAGGR}

    private static Map<String, DataClass> primitiveTypeAliases = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("DATE", DateClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("DATETIME", DateTimeClass.instance);
        put("TEXT", StringClass.text);
        put("RICHTEXT", StringClass.richText);
        put("TIME", TimeClass.instance);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get(false, false));
        put("IMAGEFILE", ImageClass.get(false, false));
        put("PDFFILE", PDFClass.get(false, false));
        put("CUSTOMFILE", DynamicFormatFileClass.get(false, false));
        put("EXCELFILE", ExcelClass.get(false, false));
        put("COLOR", ColorClass.instance);
    }};

    private ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        setBaseLogicsModule(baseModule);
        this.BL = BL;
        errLog = new ScriptingErrorLog("");
        parser = new ScriptParser(errLog);
    }

    public ScriptingLogicsModule(String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        this(baseModule, BL);
        this.filename = filename;
    }

    public ScriptingLogicsModule(InputStream stream, String path, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(stream, path, "utf-8", baseModule, BL);
    }

    public ScriptingLogicsModule(InputStream stream, String path, String charsetName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(baseModule, BL);
        this.code = IOUtils.readStreamToString(stream, charsetName);
        this.path = path;
    }

    public ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL, String code) {
        this(baseModule, BL);
        this.code = code;
    }

    protected DataSession createSession() throws SQLException {
        return ThreadLocalContext.getDbManager().createSession();
    }

    private void setModuleName(String moduleName) {
        setName(moduleName);
        errLog.setModuleName(moduleName);
    }

    private CharStream createStream() throws IOException {
        if (code != null) {
            return new ANTLRStringStream(code);
        } else {
            return new ANTLRFileStream(filename, "UTF-8");
        }
    }

    @Override
    @IdentityLazy
    public int getModuleComplexity() {
        try {
            return createStream().size();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public ScriptingErrorLog getErrLog() {
        return errLog;
    }

    public ScriptParser getParser() {
        return parser;
    }

    public String getPath() {
        return path;
    }

    public String getCode() {
        return code;
    }
    
    protected LogicsModule findModule(String name) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModule(name);
        checkModule(module, name);
        return module;
    }

    public String transformStringLiteral(String s) throws ScriptingErrorLog.SemanticErrorException {
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                assert i+2 < s.length();
                char nextCh = s.charAt(i+1);
                switch (nextCh) {
                    case '\\': b.append('\\'); break;
                    case '\'': b.append('\''); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    default: errLog.emitStrLiteralEscapeSequenceError(parser, nextCh);
                }
                ++i;
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();
    }

    static public DataClass getPredefinedClass(String name) {
        if (primitiveTypeAliases.containsKey(name)) {
            return primitiveTypeAliases.get(name);
        } else if (name.startsWith("STRING[")) {
            name = name.substring("STRING[".length(), name.length() - 1);
            return StringClass.get(new ExtInt(Integer.parseInt(name)));
        } else if (name.startsWith("ISTRING[")) {
            name = name.substring("ISTRING[".length(), name.length() - 1);
            return StringClass.geti(new ExtInt(Integer.parseInt(name)));
        } else if (name.startsWith("VARSTRING[")) {
            name = name.substring("VARSTRING[".length(), name.length() - 1);
            return StringClass.getv(new ExtInt(Integer.parseInt(name)));
        } else if (name.startsWith("VARISTRING[")) {
            name = name.substring("VARISTRING[".length(), name.length() - 1);
            return StringClass.getvi(new ExtInt(Integer.parseInt(name)));
        } else if (name.startsWith("NUMERIC[")) {
            String length = name.substring("NUMERIC[".length(), name.indexOf(","));
            String precision = name.substring(name.indexOf(",") + 1, name.length() - 1);
            return NumericClass.get(Integer.parseInt(length), Integer.parseInt(precision));
        }
        return null;
    }

    private Type getPredefinedType(String name) {
        if ("OBJECT".equals(name)) {
            return ObjectType.instance;
        } else {
            return getPredefinedClass(name);
        }
    }

    public ObjectEntity[] getMappingObjectsArray(FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity[] objects = new ObjectEntity[mapping.size()];
        for (int i = 0; i < mapping.size(); i++) {
            objects[i] = getObjectEntityByName(form, mapping.get(i));
        }
        return objects;
    }

    public List<AndClassSet> getMappingClassesArray(FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> classes = new ArrayList<AndClassSet>();
        for (String paramName : mapping) {
            ObjectEntity obj = getObjectEntityByName(form, paramName);
            classes.add(obj.getAndClassSet());
        }
        return classes;
    }
    
    public ObjectEntity getObjectEntityByName(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity obj = form.getNFObject(name, getVersion());
        if (obj == null) {
            getErrLog().emitObjectNotFoundError(parser, name);
        }
        return obj;
    }

    public MappedProperty getPropertyWithMapping(FormEntity form, PropertyUsage pUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property;
        if (pUsage.classNames != null) {
            property = findLPByPropertyUsage(pUsage);            
        } else {
            List<AndClassSet> classes = getMappingClassesArray(form, mapping);
            property = findLPByNameAndClasses(pUsage.name, classes);            
        }
        
        if (property.property.interfaces.size() != mapping.size()) {
            getErrLog().emitParamCountError(parser, property, mapping.size());
        }
        return new MappedProperty(property, getMappingObjectsArray(form, mapping));
    }

    public ValueClass findClassByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = getPredefinedClass(name);
        if (valueClass == null) {
            valueClass = classResolver.resolve(name);
        }
        checkClass(valueClass, name);
        return valueClass;
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract,
                                 List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedClass(" + className + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        checkDuplicateClass(className);
        checkStaticClassConstraints(isAbstract, instNames, instCaptions);
        checkClassParents(parentNames);

        String caption = (captionStr == null ? className : captionStr);

        CustomClass[] parents;
        if (parentNames.isEmpty()) {
            parents = new CustomClass[] {baseLM.baseClass};
        } else {
            parents = new CustomClass[parentNames.size()];
            for (int i = 0; i < parentNames.size(); i++) {
                String parentName = parentNames.get(i);
                parents[i] = (CustomClass) findClassByCompoundName(parentName);
            }
        }

        List<String> captions = new ArrayList<String>();
        for (String instCaption : instCaptions) {
            captions.add(instCaption == null ? null : instCaption);
        }

        if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, instNames, captions, parents);
        }
    }

    public void extendClass(String className, List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        scriptLogger.info("extendClass(" + className + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        CustomClass cls = (CustomClass) findClassByCompoundName(className);
        boolean isAbstract = cls instanceof AbstractCustomClass;

        List<String> names = instNames;
        List<String> captions = instCaptions;
        if (!isAbstract) {
            ((ConcreteCustomClass) cls).addStaticObjects(instNames, instCaptions, version);
            names = ((ConcreteCustomClass) cls).getNFStaticObjectsNames(version);
            captions = ((ConcreteCustomClass) cls).getNFStaticObjectsCaptions(version);
        }

        checkStaticClassConstraints(isAbstract, names, captions);
        checkClassParents(parentNames);

        for (String parentName : parentNames) {
            CustomClass parentClass = (CustomClass) findClassByCompoundName(parentName);
            if (cls.containsNFParents(parentClass, version)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            cls.addParentClass(parentClass, version);
        }
    }

    public AbstractGroup findGroupByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractGroup group = groupResolver.resolve(name);
        checkGroup(group, name);
        return group;
    }

    public LAP<?> findLAPByCompoundOldName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LAP<?>) findLPByCompoundOldName(name);
    }

    public LCP<?> findLCPByCompoundOldName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LCP<?>) findLPByCompoundOldName(name);
    }

    public LP<?, ?> findLPByCompoundOldName(String name) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property = lpOldResolver.resolve(name);
        checkProperty(property, name);
        return property;
    }

    public LP<?, ?> findLPByNameAndClasses(String name, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> classSets = new ArrayList<AndClassSet>();
        for (ValueClass cls : classes) {
            classSets.add(cls.getUpSet());
        }
        return findLPByNameAndClasses(name, classSets);
    }
    
    private LP<?, ?> findLPByNameAndClasses(String name, List<AndClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property = lpResolver.resolve(name, params);
        checkProperty(property, name);
        return property;
    }
    
    public LP<?, ?> findLPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return  findLPByNameAndClasses(pUsage.name, getParamClasses(pUsage));
    }
    
    public AbstractWindow findWindowByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractWindow window = windowResolver.resolve(name);
        checkWindow(window, name);
        return window;
    }

    public FormEntity findFormByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement navigator = navigatorResolver.resolve(name);
        checkForm(navigator, name);
        return (FormEntity) navigator;
    }

    public List<FormEntity> findFormsByCompoundName(List<String> names) throws ScriptingErrorLog.SemanticErrorException {
        List<FormEntity> forms = new ArrayList<FormEntity>();
        for (String name : names) {
            forms.add(findFormByCompoundName(name));
        }
        return forms;
    }

    public Event createScriptedEvent(BaseEvent base, List<String> formIds, List<PropertyUsage> afterIds) throws ScriptingErrorLog.SemanticErrorException {
        return new Event(base, formIds != null ? new SessionEnvEvent(SetFact.fromJavaSet(new HashSet<FormEntity>(findFormsByCompoundName(formIds)))) : SessionEnvEvent.ALWAYS, afterIds == null? null : SetFact.fromJavaSet(findPropsByPropertyUsages(afterIds)));
    }

    public MetaCodeFragment findMetaCodeFragmentByCompoundName(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        CompoundNameResolver<MetaCodeFragment, Integer> resolver = new CompoundNameResolver<MetaCodeFragment, Integer>(new MetaCodeNameModuleFinder());
        MetaCodeFragment code = resolver.resolve(name, paramCnt);
        checkMetaCodeFragment(code, name);
        return code;
    }

    public NavigatorElement findNavigatorElementByName(String name) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement element = navigatorResolver.resolve(name);
        checkNavigatorElement(element, name);
        return element;
    }

    public ImplementTable findTableByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        ImplementTable table = tableResolver.resolve(name);
        checkTable(table, name);
        return table;
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGroup(" + groupName + ", " + (captionStr==null ? "" : captionStr) + ", " + (parentName == null ? "null" : parentName) + ");");
        checkDuplicateGroup(groupName);
        String caption = (captionStr == null ? groupName : captionStr);
        AbstractGroup parentGroup = (parentName == null ? null : findGroupByCompoundName(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, String caption, String title, String icon) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedForm(" + formName + ", " + caption + ", " + title + ");");
        checkDuplicateNavigatorElement(formName);
        caption = (caption == null ? formName : caption);
        return new ScriptingFormEntity(this, new FormEntity(formName, caption, title, icon, getVersion()));
    }

    public ScriptingFormView createScriptedFormView(String formName, String caption, boolean applyDefault) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();
        
        scriptLogger.info("createScriptedFormView(" + formName + ", " + applyDefault + ");");

        FormEntity form = findFormByCompoundName(formName);
        FormView formView = applyDefault ? new DefaultFormView(form, version) : new FormView(form, version);
        ScriptingFormView scriptingView = new ScriptingFormView(formView, this);
        if (caption != null) {
            formView.caption = caption;
        }

        form.setRichDesign(formView, version);

        return scriptingView;
    }

    public ScriptingFormView getDesignForExtending(String formName) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        scriptLogger.info("getDesignForExtending(" + formName + ");");
        FormEntity form = findFormByCompoundName(formName);
        return new ScriptingFormView(form.getNFRichDesign(version), this);
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedForm(" + form + ");");
        addFormEntity(form.getForm()).finalizeInit(getVersion());
    }

    public ScriptingFormEntity getFormForExtending(String name) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findFormByCompoundName(name);
        return new ScriptingFormEntity(this, form);
    }

    public LCP addScriptedDProp(String returnClass, List<String> paramClasses, boolean sessionProp, boolean innerProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDProp(" + returnClass + ", " + paramClasses + ", " + innerProp + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }

        if (sessionProp) {
            return addSDProp(genSID(), "", value, params);
        } else {
            if (innerProp) {
                return addDProp(genSID(), "", value, params);
            } else {
                StoredDataProperty storedProperty = new StoredDataProperty(genSID(), "", params, value);
                return addProperty(null, new LCP<ClassPropertyInterface>(storedProperty));
            }
        }
    }

    public LP<?, ?> addScriptedAbstractProp(CaseUnionProperty.Type type, String returnClass, List<String> paramClasses, boolean isExclusive, boolean isChecked) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAbstractProp(" + type + ", " + returnClass + ", " + paramClasses + ", " + isExclusive + ", " + isChecked + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }
        return addAUProp(null, genSID(), false, isExclusive, isChecked, type, "", value, params);
    }

    public LP addScriptedAbstractActionProp(ListCaseActionProperty.AbstractType type, List<String> paramClasses, boolean isExclusive, boolean isChecked) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAbstractActionProp(" + type + ", " + paramClasses + ", " + isExclusive + ", " + isChecked + ");");
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }
        if (type == ListCaseActionProperty.AbstractType.LIST) {
            return addAbstractListAProp(isChecked, params);
        } else {
            return addAbstractCaseAProp(type, isExclusive, isChecked, params);
        }
    }

    public void addImplementationToAbstract(PropertyUsage abstractPropUsage, List<TypedParameter> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addImplementationToAbstract(" + abstractPropUsage + ", " + context + ", " + implement + ", " + when + ");");

        LP abstractLP = findLPByPropertyUsage(abstractPropUsage);
        checkParamCount(abstractLP, context.size());

        List<LPWithParams> allProps = new ArrayList<LPWithParams>();
        allProps.add(implement);
        if (when != null) {
            checkCalculationProperty(when.property);
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        if (abstractLP instanceof LCP) {
            addImplementationToAbstractProp(abstractPropUsage.name, (LCP) abstractLP, when != null, params);
        } else {
            addImplementationToAbstractAction(abstractPropUsage.name, (LAP) abstractLP, when != null, params);
        }
    }

    private void addImplementationToAbstractProp(String propName, LCP abstractProp, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checkAbstractProperty(abstractProp, propName);
        CaseUnionProperty.Type type = ((CaseUnionProperty)abstractProp.property).getAbstractType();
        checkAbstractTypes(type == CaseUnionProperty.Type.CASE, isCase);

        try {
            abstractProp.addOperand(isCase, getVersion(), params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    private void addImplementationToAbstractAction(String actionName, LAP abstractAction, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checkAbstractAction(abstractAction, actionName);
        ListCaseActionProperty.AbstractType type = ((ListCaseActionProperty)abstractAction.property).getAbstractType();
        checkAbstractTypes(type == ListCaseActionProperty.AbstractType.CASE, isCase);

        try {
            abstractAction.addOperand(isCase, getVersion(), params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    private int indexOf(List<TypedParameter> params, String paramName) {
        if (params == null) return -1;        
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).paramName.equals(paramName)) {
                return i;    
            }
        }
        return -1;
    }
    
    public List<AndClassSet> createClassSetsFromClassNames(List<String> classNames) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> params = new ArrayList<AndClassSet>();
        for (String className : classNames) {
            ValueClass cls = findClassByCompoundName(className);
            params.add(cls.getUpSet());
        }
        return params;
    }
    
    public int getParamIndex(TypedParameter param, List<TypedParameter> context, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        String paramName = param.paramName;
        int index = indexOf(context, paramName);
        
        if (index < 0 && paramName.startsWith("$")) {
            if (param.cls != null) {
                errLog.emitParamClassNonDeclarationError(parser, paramName);
            }
            if (Character.isDigit(paramName.charAt(1))) {
                index = Integer.parseInt(paramName.substring(1)) - 1;
                if (index < 0 || !dynamic && context != null && index >= context.size()) {
                    errLog.emitParamIndexError(parser, index + 1, context == null ? 0 : context.size());
                }
            } else if (!insideRecursion) {
                errLog.emitRecursiveParamsOutideRecursionError(parser, paramName);
            } else if (indexOf(context, paramName.substring(1)) < 0 && !dynamic) {
                errLog.emitParamNotFoundError(parser, paramName.substring(1));
            }
        }
        
        if (index >= 0 && param.cls != null && context != null) {
            if (context.get(index).cls != null) {
                errLog.emitParamClassRedefinitionError(parser, paramName);
            } else {
                errLog.emitParamClassNonDeclarationError(parser, paramName);
            }
        }
        if (index < 0 && context != null && (dynamic || paramName.startsWith("$") && insideRecursion)) {
            index = context.size();
            context.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, paramName);
        }
        return index;
    }

    public static class LPWithParams {
        public LP property;
        public List<Integer> usedParams;

        public LPWithParams(LP property, List<Integer> usedParams) {
            this.property = property;
            this.usedParams = usedParams;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", property, usedParams);
        }
    }

    private boolean isTrivialParamList(List<Object> paramList) {
        int index = 1;
        for (Object param : paramList) {
            if (!(param instanceof Integer) || ((Integer)param) != index) return false;
            ++index;
        }
        return true;
    }

    private List<String> getParamNamesFromTypedParams(List<TypedParameter> params) {
        List<String> paramNames = new ArrayList<String>();
        for (TypedParameter param : params) {
            paramNames.add(param.paramName);
        }
        return paramNames;        
    }

    public List<AndClassSet> getClassesFromTypedParams(List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> paramClasses = new ArrayList<AndClassSet>();
        for (TypedParameter param : params) {
            if (param.cls == null) {
                paramClasses.add(null);
            } else {
                paramClasses.add(param.cls.getUpSet());
            }
        }
        return paramClasses;
    }
    
    private List<ValueClass> extractClasses(List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        List<ValueClass> classes = new ArrayList<ValueClass>();
        for (TypedParameter param : params) {
            classes.add(param.cls);
        }
        return classes;
    } 
    
    public void addSettingsToProperty(LP property, String name, String caption, List<TypedParameter> params, List<AndClassSet> signature, String groupName, boolean isPersistent, boolean isComplex, String tableName, Boolean notNullResolve, Event notNullEvent) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addSettingsToProperty(" + property.property.getSID() + ", " + name + ", " + caption + ", " +
                           params + ", " + groupName + ", " + isPersistent  + ", " + tableName + ");");
        checkDuplicateProperty(name, signature);
       
        List<String> paramNames = getParamNamesFromTypedParams(params);
        checkDistinctParameters(paramNames);
        checkNamedParams(property, paramNames);

        // Если объявление имеет вид f(x, y) = g(x, y), то нужно дополнительно обернуть свойство g в join
        if (property.property.getSID().equals(lastOpimizedJPropSID)) {
            property = addJProp("", (LCP) property, BaseUtils.consecutiveList(property.property.interfaces.size(), 1).toArray());
        }
        changePropertyName(property, name); // должно идти первым

        AbstractGroup group = (groupName == null ? null : findGroupByCompoundName(groupName));
        property.property.caption = (caption == null ? name : caption);
        addPropertyToGroup(property.property, group);

        ImplementTable targetTable = null;
        if (tableName != null) {
            targetTable = findTableByCompoundName(tableName);
            if (!targetTable.equalClasses(((LCP<?>)property).property.getInterfaceClasses(ClassType.ASSERTFULL))) {
                // todo : проверка неправильная - должна быть на ClassWhere
                //errLog.emitWrongClassesForTable(parser, name, tableName);
            }
        }
        if (property.property instanceof StoredDataProperty) {
            ((StoredDataProperty)property.property).markStored(baseLM.tableFactory, targetTable);
        } else if (isPersistent && (property.property instanceof AggregateProperty)) {
            addPersistent((LCP) property, targetTable);
        }

        if(isComplex)
            ((LCP<?>)property).property.complex = true;

        if (notNullResolve != null) {
            setNotNull((LCP)property, notNullEvent, notNullResolve ? PropertyFollows.RESOLVE_FALSE : PropertyFollows.RESOLVE_NOTHING);
        }

        if (property.property instanceof CalcProperty) {
            
            if (Settings.get().isCheckAlwaysNull()) {
                checkPropertyValue(property);
                if (!alwaysNullProperties.isEmpty()) {
                    showAlwaysNullErrors();
                }
            }

            if (Settings.get().isCheckClassWhere()) {
                checkClassWhere((LCP) property, name);
            }
        }
        addNamedParams(property.property.getSID(), paramNames);
        propClasses.put(property, signature);
    }

    private void showAlwaysNullErrors() throws ScriptingErrorLog.SemanticErrorException {
        String errorMessage = "";
        for (Property property : alwaysNullProperties.keySet()) {
            if (!errorMessage.isEmpty()) {
                errorMessage += "\n";
            }
            String location = alwaysNullProperties.get(property);
            errorMessage += "[error]:\t" + location + " property '" + property.getName() + "' is always NULL";
        }
        alwaysNullProperties.clear();
        errLog.emitSemanticError(errorMessage, new ScriptingErrorLog.SemanticErrorException(parser.getCurrentParser().input));
    }

    public void addToContextMenuFor(LP onContextAction, String contextMenuCaption, PropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        checkActionProperty(onContextAction);

        LP<?, ?> mainProperty = findLPByPropertyUsage(mainPropertyUsage);
        LAP onContextLAP = (LAP) onContextAction;
        onContextLAP.addToContextMenuFor(mainProperty, contextMenuCaption);
    }

    public void setAsEditActionFor(LP onEditAction, String editActionSID, PropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        checkActionProperty(onEditAction);

        LP<?, ?> mainProperty = findLPByPropertyUsage(mainPropertyUsage);
        LAP onEditLAP = (LAP) onEditAction;
        onEditLAP.setAsEditActionFor(editActionSID, mainProperty);
    }

    public void setDrawToToolbar(LP property) {
        property.setDrawToToolbar(true);
    }

    public void setFixedCharWidth(LP property, Integer fixedCharWidth) {
        if (fixedCharWidth != null && fixedCharWidth > 0)
            property.setFixedCharWidth(fixedCharWidth);
    }

    public void setMinCharWidth(LP property, Integer minCharWidth) {
        if (minCharWidth != null)
            property.setMinimumCharWidth(minCharWidth);
    }

    public void setMaxCharWidth(LP property, Integer maxCharWidth) {
        if (maxCharWidth != null)
            property.setMaximumCharWidth(maxCharWidth);
    }

    public void setPrefCharWidth(LP property, Integer prefCharWidth) {
        if (prefCharWidth != null)
            property.setPreferredCharWidth(prefCharWidth);
    }

    public void setImage(LP property, String path) {
        property.setImage(path);
    }

    public void setEditKey(LP property, String code, Boolean showEditKey) {
        property.setEditKey(KeyStroke.getKeyStroke(code));
        if (showEditKey != null)
            property.setShowEditKey(showEditKey);
    }

    public void setAutoset(LP property, boolean autoset) {
        ((LCP)property).setAutoset(autoset);
    }

    public void setAskConfirm(LP property, boolean askConfirm) {
        property.setAskConfirm(askConfirm);
    }

    public void setRegexp(LP property, String regexp, String regexpMessage) {
        property.setRegexp(regexp);
        if (regexpMessage != null) {
            property.setRegexpMessage(regexpMessage);
        }
    }

    public void makeLoggable(LP property, Boolean isLoggable) {
        if (isLoggable != null && isLoggable && property != null)
            ((LCP)property).makeLoggable(BL.systemEventsLM);
    }

    public void setEchoSymbols(LP property) {
        property.setEchoSymbols(true);
    }

    public void setAggProp(LP property) {
        ((CalcProperty)property.property).aggProp = true;
    }

    public void setScriptedEditAction(LP property, String actionType, LPWithParams action) {
        List<Object> params = getParamsPlainList(Arrays.asList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        property.property.setEditAction(actionType, actionImplements.get(0));
    }

    public void setEventId(LP property, String id) {
        property.property.eventID = id;
    }

    public void setPropertyOldName(LP property, String oldName) {
        property.property.setOldName(oldName); 
    }
    
    private <T extends LP> void changePropertyName(T lp, String name) {
        removeModuleLP(lp);
        setPropertySID(lp, name, lp.property.getOldName(), false);
        lp.property.freezeSID();
        addModuleLP(lp);
    }

    // Для local свойств будем устанавливать сгенерированные автоматически sid для того, чтобы они были уникальными
    private <T extends LP> void changeLocalPropertyName(T lp, String name) {
        removeModuleLP(lp);
        String oldSID = lp.property.getSID();
        setPropertySID(lp, name, lp.property.getOldName(), false);
        lp.property.setSID("local" + oldSID);
        lp.property.freezeSID();
        addModuleLP(lp);
    }
    
    private List<AndClassSet> getParamClasses(PropertyUsage usage) throws ScriptingErrorLog.SemanticErrorException {
        if (usage.classNames == null) {
            return null;
        }
        
        List<AndClassSet> classes = new ArrayList<AndClassSet>();
        for (String className : usage.classNames) {
            ValueClass cls = findClassByCompoundName(className);
            classes.add(cls == null ? null : cls.getUpSet());
        }
        return classes;
    }
    
    private List<AndClassSet> getParamClassesByParamProperties(List<LPWithParams> paramProps, List<TypedParameter> params) {
        List<AndClassSet> classes = new ArrayList<AndClassSet>();
        for (LPWithParams paramProp : paramProps) {
            if (paramProp.property != null) {
                CalcProperty lcp = (CalcProperty) paramProp.property.property;
                classes.add(lcp.getValueClassSet());
            } else {
                TypedParameter param = params.get(paramProp.usedParams.get(0));
                if (param.cls == null) {
                    classes.add(null);
                } else {
                    classes.add(param.cls.getUpSet());
                }
            }
        }
        return classes;
    }
    
    public List<AndClassSet> getSignatureForGProp(List<LPWithParams> paramProps, List<TypedParameter> params) {
        return getParamClassesByParamProperties(paramProps, params);
    }
    
    private LP findJoinMainProp(String mainPropName, List<LPWithParams> paramProps, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> classes = getParamClassesByParamProperties(paramProps, params);
        LP mainProp = findLPByNameAndClasses(mainPropName, classes);
        return mainProp;
    }
    
    public LPWithParams addScriptedJProp(PropertyUsage pUsage, List<LPWithParams> paramProps, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp;
        if (pUsage.classNames != null) {
            mainProp = findLPByPropertyUsage(pUsage);
        } else {
            mainProp = findJoinMainProp(pUsage.name, paramProps, params);
        }
        return addScriptedJProp(mainProp, paramProps);
    }
    
    public LPWithParams addScriptedJProp(LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        //  checkCalculationProperty(mainProp);
        if (mainProp instanceof LAP)    // todo [dale]: Это нужно убирать, пока оставил для += действий.
            return addScriptedJoinAProp(mainProp, paramProps);

        checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LP prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
            lastOpimizedJPropSID = mainProp.property.getSID();
        } else {
            scriptLogger.info("addScriptedJProp(" + mainProp.property.getSID() + ", " + resultParams + ");");
            prop = addJProp("", (LCP) mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    private LCP getRelationProp(String op) {
        if (op.equals("==")) {
            return baseLM.equals2;
        } else if (op.equals("!=")) {
            return baseLM.diff2;
        } else if (op.equals(">")) {
            return baseLM.greater2;
        } else if (op.equals("<")) {
            return baseLM.less2;
        } else if (op.equals(">=")) {
            return baseLM.groeq2;
        } else if (op.equals("<=")) {
            return baseLM.lsoeq2;
        }
        assert false;
        return null;
    }

    private LCP getArithProp(String op) {
        if (op.equals("+")) {
            return baseLM.sum;
        } else if (op.equals("-")) {
            return baseLM.subtract;
        } else if (op.equals("*")) {
            return baseLM.multiply;
        } else if (op.equals("/")) {
            return baseLM.divide;
        }
        assert false;
        return null;
    }

    public LPWithParams addScriptedEqualityProp(String op, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedRelationalProp(String op, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedOverrideProp(List<LPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.EXCLUSIVE, properties, "EXCLUSIVE");
        } else {
            return addScriptedUProp(Union.OVERRIDE, properties, "OVERRIDE");
        }
    }

    private boolean isLogical(LP property) {
        return property != null && property.property.getValueClass() != null && property.property.getValueClass().equals(LogicalClass.instance);
    }

    private LPWithParams toLogical(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(and(false), Arrays.<LPWithParams>asList(new LPWithParams(baseLM.vtrue, new ArrayList<Integer>()), property));
    }

    public LPWithParams addScriptedIfProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            boolean[] notsArray = new boolean[properties.size() - 1];
            Arrays.fill(notsArray, false);
            if (properties.get(0).property != null) {
                checkCalculationProperty(properties.get(0).property);
            }
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedOrProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            for (LPWithParams prop : properties) {
                if (!isLogical(prop.property)) {
                    prop.property = toLogical(prop).property;
                }
            }
            res = addScriptedUProp(Union.OVERRIDE, properties, "OR");
        }
        return res;
    }

    public LPWithParams addScriptedXorProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            for (LPWithParams prop : properties) {
                if (!isLogical(prop.property)) {
                    prop.property = toLogical(prop).property;
                }
            }
            res = addScriptedUProp(Union.XOR, properties, "XOR");
        }
        return res;
    }

    public LPWithParams addScriptedAndProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            boolean[] notsArray = new boolean[properties.size() - 1];
            Arrays.fill(notsArray, false);
            if (properties.get(0).property != null) {
                checkCalculationProperty(properties.get(0).property);
            }
            if (!isLogical(properties.get(0).property)) {
                properties.get(0).property = toLogical(properties.get(0)).property;
            }
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedIfElseUProp(LPWithParams ifProp, LPWithParams thenProp, LPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIfElseUProp(" + ifProp + ", " + thenProp + ", " + elseProp + ");");
        assert !(thenProp.property instanceof LAP) && (elseProp == null || !(elseProp.property instanceof LAP));
        List<LPWithParams> lpParams = new ArrayList<LPWithParams>();
        lpParams.add(addScriptedJProp(and(false), asList(thenProp, ifProp)));
        if (elseProp != null) {
            lpParams.add(addScriptedJProp(and(true), asList(elseProp, ifProp)));
        }
        return addScriptedUProp(Union.EXCLUSIVE, lpParams, "IF");
    }

    public LPWithParams addScriptedCaseUProp(List<LPWithParams> whenProps, List<LPWithParams> thenProps, LPWithParams elseProp, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCaseUProp(" + whenProps  + "->" + thenProps + ");");

        assert whenProps.size() > 0 && whenProps.size() == thenProps.size();

        List<LPWithParams> caseParamProps = new ArrayList<LPWithParams>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParamProps.add(whenProps.get(i));
            caseParamProps.add(thenProps.get(i));
        }
        if (elseProp != null) {
            caseParamProps.add(elseProp);
        }

        LP caseProp = addCaseUProp(null, genSID(), false, "", isExclusive, getParamsPlainList(caseParamProps).toArray());
        return new LPWithParams(caseProp, mergeAllParams(caseParamProps));
    }

    public LPWithParams addScriptedMultiProp(List<LPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.CLASS, properties, "MULTI");
        } else {
            return addScriptedUProp(Union.CLASSOVERRIDE, properties, "MULTI");
        }
    }

    public LPWithParams addScriptedFileAProp(boolean loadFile, LPWithParams property) {
        scriptLogger.info("addScriptedFileAProp(" + loadFile + ", " + property + ");");
        LAP<?> res;
        if (loadFile) {
            res = addLFAProp((LCP) property.property);
        } else {
            res = addOFAProp((LCP) property.property);
        }
        return new LPWithParams(res, property.usedParams);
    }

    public LP addScriptedCustomActionProp(String javaClassName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCustomActionProp(" + javaClassName + ");");
        try {
            return baseLM.addAProp(null, (ActionProperty) Class.forName(javaClassName).getConstructor(this.getClass()).newInstance(this));
        } catch (ClassNotFoundException e) {
            errLog.emitClassNotFoundError(parser, javaClassName);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, javaClassName);
        }
        return null;
    }

    public LPWithParams addScriptedEmailProp(LPWithParams fromProp,
                                             LPWithParams subjProp,
                                             List<Message.RecipientType> recipTypes,
                                             List<LPWithParams> recipProps,
                                             List<String> forms,
                                             List<FormStorageType> formTypes,
                                             List<OrderedMap<String, LPWithParams>> mapObjects,
                                             List<LPWithParams> attachNames,
                                             List<AttachmentFormat> attachFormats) throws ScriptingErrorLog.SemanticErrorException {

        List<LPWithParams> allProps = new ArrayList<LPWithParams>();

        if (fromProp != null) {
            allProps.add(fromProp);
        }
        allProps.add(subjProp);
        allProps.addAll(recipProps);

        for (int i = 0; i < forms.size(); ++i) {
            allProps.addAll(mapObjects.get(i).values());
            if (formTypes.get(i) == FormStorageType.ATTACH && attachNames.get(i) != null) {
                allProps.add(attachNames.get(i));
            }
        }

        Object[] allParams = getParamsPlainList(allProps).toArray();

        ImOrderSet<PropertyInterface> tempContext = genInterfaces(getIntNum(allParams));
        ValueClass[] eaClasses = CalcProperty.getCommonClasses(tempContext, readCalcImplements(tempContext, allParams).getCol());

        LAP<ClassPropertyInterface> eaPropLP = BL.emailLM.addEAProp(null, "", "", eaClasses, null, null);
        SendEmailActionProperty eaProp = (SendEmailActionProperty) eaPropLP.property;

        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> allImplements = readCalcImplements(eaPropLP.listInterfaces, allParams);

        int i = 0;
        if (fromProp != null) {
            eaProp.setFromAddressAccount(allImplements.get(i++));
        } else {
            // по умолчанию используем стандартный fromAddressAccount
            eaProp.setFromAddressAccount(new CalcPropertyMapImplement((CalcProperty) BL.emailLM.getLCPByOldName("fromAddressDefaultNotificationAccount").property));
        }
        eaProp.setSubject(allImplements.get(i++));

        for (Message.RecipientType recipType : recipTypes) {
            eaProp.addRecipient(allImplements.get(i++), recipType);
        }

        for (int j = 0; j < forms.size(); ++j) {
            String formName = forms.get(j);
            FormStorageType formType = formTypes.get(j);
            FormEntity form = findFormByCompoundName(formName);

            Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements = new HashMap<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>>();
            for (Map.Entry<String, LPWithParams> entry : mapObjects.get(j).entrySet()) {
                objectsImplements.put(findObjectEntity(form, entry.getKey()), allImplements.get(i++));
            }

            if (formType == FormStorageType.ATTACH) {
                CalcPropertyInterfaceImplement<ClassPropertyInterface> attachNameProp = attachNames.get(j) != null ? allImplements.get(i++) : null;
                eaProp.addAttachmentForm(form, attachFormats.get(j), objectsImplements, attachNameProp);
            } else {
                eaProp.addInlineForm(form, objectsImplements);
            }
        }

        return new LPWithParams(eaPropLP, mergeAllParams(allProps));
    }

    public LPWithParams addScriptedAdditiveOrProp(List<String> operands, List<LPWithParams> properties) {
        assert operands.size() + 1 == properties.size();
        
        LPWithParams res = properties.get(0);
        if (operands.size() > 0) {
            scriptLogger.info("addScriptedAdditiveOrProp(" + operands + ", " + properties + ");");
            List<Object> resultParams;
            int[] coeffs = new int[properties.size()];
            for (int i = 0; i < coeffs.length; i++) {
                if (i == 0 || operands.get(i-1).equals("(+)")) {
                    coeffs[i] = 1;
                } else {
                    coeffs[i] = -1;
                }
            }
            resultParams = getParamsPlainList(properties);
            res = new LPWithParams(addUProp(null, "", Union.SUM, null, coeffs, resultParams.toArray()), mergeAllParams(properties));
        }
        return res;    
    }
    
    public LPWithParams addScriptedAdditiveProp(List<String> operands, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LPWithParams sumLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            LPWithParams currLP = properties.get(i);
            sumLP = addScriptedJProp(getArithProp(operands.get(i-1)), asList(sumLP, currLP));
        }
        return sumLP;
    }


    public LPWithParams addScriptedMultiplicativeProp(List<String> operands, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(getArithProp(op), asList(curLP, properties.get(i)));
        }
        return curLP;
    }

    public LPWithParams addScriptedUnaryMinusProp(LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(baseLM.minus, asList(prop));
    }

    public LPWithParams addScriptedNotProp(LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(not(), asList(prop));
    }

    public LPWithParams addScriptedCastProp(String typeName, LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClassByCompoundName(typeName);

        //cls всегда будет DataClass из-за грамматики
        assert cls instanceof DataClass;

        return addScriptedJProp(addCastProp((DataClass) cls), asList(prop));
    }

    private boolean doesExtendContext(List<LPWithParams> list, List<LPWithParams> orders) {
        Set<Integer> listContext = new HashSet<Integer>();
        for(LPWithParams lp : list)
            if(lp.property != null)
                listContext.addAll(lp.usedParams);
        return !listContext.containsAll(mergeAllParams(orders));
    }

    private List<Integer> mergeAllParams(List<LPWithParams> lpList) {
        Set<Integer> s = new TreeSet<Integer>();
        for (LPWithParams mappedLP : lpList) {
            s.addAll(mappedLP.usedParams);
        }
        return new ArrayList<Integer>(s);
    }

    private List<Integer> mergeIntLists(List<List<Integer>> lists) {
        Set<Integer> s = new TreeSet<Integer>();
        for (List<Integer> list : lists) {
            s.addAll(list);
        }
        return new ArrayList<Integer>(s);
    }

    public LPWithParams addScriptedListAProp(boolean newSession, boolean doApply, boolean singleApply, PropertyUsage used, List<LPWithParams> properties, List<LP> localProps, boolean newThread, long delay, Long period) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedListAProp(" + newSession + ", " + doApply + ", " + properties + ");");

        ImSet<SessionDataProperty> sessionUsed = used != null ? SetFact.singleton((SessionDataProperty) findLPByPropertyUsage(used).property) : SetFact.<SessionDataProperty>EMPTY();

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);

        LAP<?> listLP = addListAProp(resultParams.toArray());
        
        currentLocalProperties.removeAll(localProps);
        for (LP<?, ?> localProp : localProps) {
            propClasses.remove(localProp);
            removeModuleLP(localProp);
        }
        
        MExclSet<SessionDataProperty> mUpLocal = SetFact.mExclSet(currentLocalProperties.size()); // exception кидается
        for(LP<?, ?> local : currentLocalProperties) {
            mUpLocal.exclAdd((SessionDataProperty) local.property);
        }

        if(newSession)
            listLP = addNewSessionAProp(null, genSID(), "", listLP, doApply, singleApply, mUpLocal.immutable(), sessionUsed); 

        if(newThread) {
            assert newSession;
            listLP = addNewThreadAProp(null, genSID(), "", listLP, delay, period);
        }

        return new LPWithParams(listLP, usedParams);
    }

    public LPWithParams addScriptedRequestUserInputAProp(String typeId, String chosenKey, LPWithParams action) throws ScriptingErrorLog.SemanticErrorException {
        Type requestValueType = getPredefinedType(typeId);

        LPWithParams prop;
        if (action == null) {
            if (!(requestValueType instanceof DataClass)) {
                errLog.emitRequestUserInputDataTypeError(parser, typeId);
            }

            prop = new LPWithParams(addRequestUserDataAProp(null, genSID(), "", (DataClass) requestValueType), new ArrayList<Integer>());
        } else {
            prop = new LPWithParams(addRequestUserInputAProp(null, genSID(), "", (LAP<?>) action.property, requestValueType, chosenKey), newArrayList(action.usedParams));
        }
        return prop;
    }

    public LCP addLocalDataProperty(String name, String returnClassName, List<String> paramClassNames) throws ScriptingErrorLog.SemanticErrorException {
        List<AndClassSet> paramClasses = new ArrayList<AndClassSet>();
        for (String className : paramClassNames) {
            paramClasses.add(findClassByCompoundName(className).getUpSet());
        }
//        checkDuplicateProperty(name, paramClasses);

        LCP res = addScriptedDProp(returnClassName, paramClassNames, true, false);
        changeLocalPropertyName(res, name);
        List<AndClassSet> outParams = createClassSetsFromClassNames(paramClassNames);
        propClasses.put(res, outParams);
        currentLocalProperties.add(res);
        return res;
    }

    public LPWithParams addScriptedJoinAProp(PropertyUsage pUsage, List<LPWithParams> properties, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp;    
        if (pUsage.classNames != null) { 
            mainProp = findLPByPropertyUsage(pUsage);
        } else {
            mainProp = findJoinMainProp(pUsage.name, properties, params);
        }
        return addScriptedJoinAProp(mainProp, properties);                        
    }
    
    public LPWithParams addScriptedJoinAProp(LP mainProp, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedJoinAProp(" + mainProp + ", " + properties + ", " + ");");
        checkActionProperty(mainProp);
        checkParamCount(mainProp, properties.size());

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);
        LP prop = addJoinAProp(null, genSID(), "", (LAP<?>) mainProp, resultParams.toArray());
        return new LPWithParams(prop, usedParams);
    }

    public LP addScriptedAddFormAction(String className, boolean session) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAddFormAction(" + className + ", " + session + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        return getScriptAddFormAction((CustomClass) cls, session);
    }

    public LP addScriptedEditFormAction(String className, boolean session) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEditFormAction(" + className + ", " + session + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        return getScriptEditFormAction((CustomClass) cls, session);
    }

    public LPWithParams addScriptedConfirmProp(LPWithParams msgProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConfirmProp(" + msgProp + ");");
        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addConfirmAProp("lsFusion", resultParams.toArray());
        return new LPWithParams(asyncLAP, msgProp.usedParams);
    }

    public LPWithParams addScriptedMessageProp(LPWithParams msgProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedMessageProp(" + msgProp + ");");
        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addMAProp("lsFusion", resultParams.toArray());
        return new LPWithParams(asyncLAP, msgProp.usedParams);
    }

    public LPWithParams addScriptedAsyncUpdateProp(LPWithParams asyncProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAsyncUpdateProp(" + asyncProp + ");");
        List<Object> resultParams = getParamsPlainList(singletonList(asyncProp));
        LAP asyncLAP = addAsyncUpdateAProp(resultParams.toArray());
        return new LPWithParams(asyncLAP, asyncProp.usedParams);
    }

    public LPWithParams addScriptedObjectSeekProp(String name, LPWithParams seekProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedObjectSeekProp(" + name + "," + seekProp + ");");

        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);

        FormEntity form = findFormByCompoundName(formName);
        if(form == null) {
            errLog.emitNotFoundError(parser, "form", formName);
        }

        ObjectEntity object = form.getNFObject(objectName, getVersion());
        if (object != null) {
            List<Object> resultParams = getParamsPlainList(singletonList(seekProp));
            LAP lap = addOSAProp(form, object, resultParams.toArray());
            return new LPWithParams(lap, seekProp.usedParams);
        } else {
            errLog.emitNotFoundError(parser, "оbject", objectName);
            return null;
        }
    }

    public LPWithParams addScriptedEvalActionProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEvalActionProp(" + property + ")");
        Type exprType = property.property.property.getType();
        if (!(exprType instanceof StringClass)) {
            errLog.emitEvalExpressionError(parser);
        }
        LAP<?> res = addEvalAProp((LCP) property.property);
        return new LPWithParams(res, property.usedParams);
    }

    public LPWithParams addScriptedDrillDownActionProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDrillDownActionProp(" + property + ")");
        LAP<?> res = addDrillDownAProp((LCP) property.property);
        return new LPWithParams(res, property.usedParams);
    }

    public LPWithParams addScriptedAssignPropertyAProp(List<TypedParameter> context, PropertyUsage toPropertyUsage, List<LPWithParams> toPropertyMapping, LPWithParams fromProperty, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAssignPropertyAProp(" + context + ", " + toPropertyUsage + ", " + fromProperty + ", " + whereProperty + ");");
        LP toPropertyLP = findLPByPropertyUsage(toPropertyUsage);

        if (!(toPropertyLP.property instanceof DataProperty || toPropertyLP.property instanceof CaseUnionProperty)) {
            errLog.emitOnlyDataCasePropertyIsAllowedError(parser, toPropertyUsage.name); // todo [dale]: изменить формат сообщения об ошибке
        }

        if (fromProperty.property != null && fromProperty.property.property.getType() != null &&
                toPropertyLP.property.getType().getCompatible(fromProperty.property.property.getType()) == null) {
            errLog.emitIncompatibleTypes(parser, "ASSIGN");
        }

        LPWithParams toProperty = addScriptedJProp(toPropertyLP, toPropertyMapping);

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, fromProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<LPWithParams>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, asList(resI)));
        }
        paramsList.add(toProperty);
        paramsList.add(fromProperty);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LP result = addSetPropertyAProp(null, genSID(), "", resultInterfaces.size(), whereProperty != null, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public LPWithParams addScriptedAddObjProp(List<TypedParameter> context, String className, PropertyUsage toPropUsage, List<LPWithParams> toPropMapping, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAddObjProp(" + className + ", " + toPropUsage + ", " + toPropMapping + ", " + whereProperty + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        checkAddObjTOParams(context.size(), toPropMapping);

        LPWithParams toProperty = null;
        if (toPropUsage != null && toPropMapping != null) {
            toProperty = addScriptedJProp(findLPByPropertyUsage(toPropUsage), toPropMapping);
        }

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<LPWithParams>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, asList(resI)));
        }
        if (toProperty != null) {
            paramsList.add(toProperty);
        } else if (whereProperty == null) {
            paramsList.add(new LPWithParams(new LCP(baseLM.getAddedObjectProperty()), new ArrayList<Integer>()));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LAP result = getScriptAddObjectAction((CustomClass) cls, false, resultInterfaces.size(), whereProperty != null, toProperty != null || whereProperty == null, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public LPWithParams addScriptedDeleteAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = addScriptedChangeClassAProp(oldContextSize, newContext, param, baseClass.unknown, whereProperty);
        setDeleteActionOptions((LAP)res.property);
        return res;
    }

    public LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, String className, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClassByCompoundName(className);
        checkChangeClassActionClass(cls);
        return addScriptedChangeClassAProp(oldContextSize, newContext, param, (ConcreteCustomClass) cls, whereProperty);
    }

    private LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, ConcreteObjectClass cls, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedChangeClassAProp(" + oldContextSize + ", " + newContext + ", " + param + ", " + cls + ", " + whereProperty + ")");

        List<LPWithParams> paramList = new ArrayList<LPWithParams>();
        paramList.add(param);
        if (whereProperty != null) {
            paramList.add(whereProperty);
        }
        List<Integer> allParams = mergeAllParams(paramList);
        int changedIndex = allParams.indexOf(param.usedParams.get(0));

        List<Integer> resultInterfaces = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex >= oldContextSize) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }
        boolean contextExtended = allParams.size() > resultInterfaces.size();

        checkChangeClassWhere(contextExtended, param, whereProperty, newContext);

        List<LPWithParams> paramsList = new ArrayList<LPWithParams>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, asList(resI)));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);

        LAP<?> res = addChangeClassAProp(genSID(), cls, resultInterfaces.size(), changedIndex, contextExtended, whereProperty != null, resultParams.toArray());
        return new LPWithParams(res,  resultInterfaces);
    }

    private List<Integer> getResultInterfaces(int contextSize, LPWithParams... params) {
        List<LPWithParams> lpList = new ArrayList<LPWithParams>();
        for (LPWithParams lp : params) {
            if (lp != null) {
                lpList.add(lp);
            }
        }
        List<Integer> allParams = mergeAllParams(lpList);

        //все использованные параметры, которые были в старом контексте, идут на вход результирующего свойства
        List<Integer> resultInterfaces = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex >= contextSize) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }
        return resultInterfaces;
    }

    public LPWithParams addScriptedIfAProp(LPWithParams condition, LPWithParams trueAction, LPWithParams falseAction) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIfAProp(" + condition + ", " + trueAction + ", " + falseAction + ");");
        List<LPWithParams> propParams = toList(condition, trueAction);
        if (falseAction != null) {
            propParams.add(falseAction);
        }
        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addIfAProp(null, genSID(), "", false, getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedTryAProp(LPWithParams tryAction, LPWithParams finallyAction) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedTryAProp(" + tryAction + ", " + finallyAction + ");");
        List<LPWithParams> propParams = new ArrayList<LPWithParams>();
        if(tryAction != null)
            propParams.add(tryAction);
        if (finallyAction != null)
            propParams.add(finallyAction);
        
        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addTryAProp(null, genSID(), "", getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedCaseAProp(List<LPWithParams> whenProps, List<LPWithParams> thenActions, LPWithParams elseAction, boolean isExclusive) {
        scriptLogger.info("addScriptedCaseAProp(" + whenProps + ", " + thenActions + ", " + elseAction + ", " + isExclusive + ");");
        assert whenProps.size() > 0 && whenProps.size() == thenActions.size();

        List<LPWithParams> caseParams = new ArrayList<LPWithParams>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParams.add(whenProps.get(i));
            caseParams.add(thenActions.get(i));
        }
        if (elseAction != null) {
            caseParams.add(elseAction);
        }

        List<Integer> allParams = mergeAllParams(caseParams);
        LP result = addCaseAProp(isExclusive, getParamsPlainList(caseParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedMultiAProp(List<LPWithParams> actions, boolean isExclusive) {
        scriptLogger.info("addScriptedMultiAProp(" + actions + ", " + isExclusive + ");");

        List<Integer> allParams = mergeAllParams(actions);
        LP result = addMultiAProp(isExclusive, getParamsPlainList(actions).toArray());
        return new LPWithParams(result, allParams);

    }

    public LPWithParams addScriptedApplyAProp(LPWithParams action, boolean singleApply) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedApplyAProp(" + action + ");");
        List<LPWithParams> propParams = new ArrayList<LPWithParams>();
        if(action != null)
            propParams.add(action);
        
        LP result = addApplyAProp(null, genSID(), "", (action != null && action.property instanceof LAP) ? (LAP) action.property : null, singleApply);
        return new LPWithParams(result, mergeAllParams(propParams));
    }

    public LPWithParams addScriptedForAProp(List<TypedParameter> oldContext, LPWithParams condition, List<LPWithParams> orders, LPWithParams action, LPWithParams elseAction, Integer addNum, String addClassName, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedForAProp(" + oldContext + ", " + condition + ", " + orders + ", " + action + ", " + elseAction + ", " + recursive + ", " + descending + ");");

        boolean ordersNotNull = (condition != null ? doesExtendContext(singletonList(condition), orders) : !orders.isEmpty());

        List<LPWithParams> creationParams = new ArrayList<LPWithParams>();
        if (condition != null) {
            creationParams.add(condition);
        }
        creationParams.addAll(orders);
        if(addNum != null) {
            creationParams.add(new LPWithParams(null, asList(addNum)));
        }
        if (elseAction != null) {
            creationParams.add(elseAction);
        }
        creationParams.add(action);
        List<Integer> allParams = mergeAllParams(creationParams);

        List<Integer> usedParams = new ArrayList<Integer>();
        List<Integer> extParams = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex < oldContext.size()) {
                usedParams.add(paramIndex);
            } else
                extParams.add(paramIndex);
        }

        checkForActionPropertyConstraints(recursive, usedParams, allParams);

        List<LPWithParams> allCreationParams = new ArrayList<LPWithParams>();
        for (int usedParam : usedParams) {
            allCreationParams.add(new LPWithParams(null, asList(usedParam)));
        }
        allCreationParams.addAll(creationParams);
        if(noInline==null) { // предполагается надо включить все кроме addNum
            noInline = new ArrayList<LPWithParams>();
            for (int usedParam : extParams)
                if(addNum==null || !addNum.equals(usedParam)) {
                    noInline.add(new LPWithParams(null, asList(usedParam)));
                }
        }
        allCreationParams.addAll(noInline);

        LP result = addForAProp(null, genSID(), "", !descending, ordersNotNull, recursive, elseAction != null, usedParams.size(), 
                addClassName != null ? (CustomClass)findClassByCompoundName(addClassName) : null, condition!=null, noInline.size(), forceInline, getParamsPlainList(allCreationParams).toArray());
        return new LPWithParams(result, usedParams);
    }

    public LPWithParams getTerminalFlowActionProperty(boolean isBreak) {
        return new LPWithParams(isBreak ? baseLM.flowBreak : baseLM.flowReturn, new ArrayList<Integer>());
    }

    private List<Object> getCoeffParamsPlainList(List<LPWithParams> mappedPropsList, Integer[] coeffs) {
        List<LP> props = new ArrayList<LP>();
        List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
        for (LPWithParams mappedProp : mappedPropsList) {
            props.add(mappedProp.property);
            usedParams.add(mappedProp.usedParams);
        }
        return getCoeffParamsPlainList(props, usedParams, coeffs);
    } 
    
    private List<Object> getParamsPlainList(List<LPWithParams>... mappedPropLists) {
        List<LP> props = new ArrayList<LP>();
        List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
        for (List<LPWithParams> mappedPropList : mappedPropLists) {
            for (LPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.property);
                usedParams.add(mappedProp.usedParams);
            }
        }
        return getCoeffParamsPlainList(props, usedParams, null);
    }

    private List<Object> getCoeffParamsPlainList(List<LP> paramProps, List<List<Integer>> usedParams, Integer[] coeffs) {
        assert coeffs == null || paramProps.size() == coeffs.length;
        List<Integer> allUsedParams = mergeIntLists(usedParams);
        List<Object> resultParams = new ArrayList<Object>();

        for (int i = 0; i < paramProps.size(); i++) {
            LP property = paramProps.get(i);
            if (property != null) {
                if (coeffs != null) {
                    resultParams.add(coeffs[i]);
                }
                resultParams.add(property);
                for (int paramIndex : usedParams.get(i)) {
                    int localParamIndex = allUsedParams.indexOf(paramIndex);
                    assert localParamIndex >= 0;
                    resultParams.add(localParamIndex + 1);
                }
            } else {
                if (coeffs != null) {
                    resultParams.add(coeffs[i]);
                }
                int localParamIndex = allUsedParams.indexOf(usedParams.get(i).get(0));
                assert localParamIndex >= 0;
                resultParams.add(localParamIndex + 1);
            }
        }
        return resultParams;
    }

    public LCP addScriptedGProp(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps, List<LPWithParams> orderProps,
                                  boolean ascending, LPWithParams whereProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGProp(" + type + ", " + mainProps + ", " + groupProps + ", " + orderProps + ", " +
                                            ascending + ", " + whereProp + ");");

        checkGPropOrderConsistence(type, orderProps.size());
        checkGPropAggregateConsistence(type, mainProps.size());
        checkGPropAggrConstraints(type, mainProps, groupProps);
        checkGPropWhereConsistence(type, whereProp);

        List<LPWithParams> whereProps = new ArrayList<LPWithParams>();
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(null, asList(mainProps.get(0).usedParams.get(0))));
            }
        }
        if (type == GroupingType.LAST) {
            if (whereProp != null) {
                mainProps.add(0, whereProp);
            } else {
                mainProps.add(mainProps.get(0));
            }
        }
        List<Object> resultParams = getParamsPlainList(mainProps, whereProps, orderProps, groupProps);

        boolean ordersNotNull = doesExtendContext(mergeLists(mainProps, groupProps), orderProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps)).size();
        LCP resultProp = null;
        if (type == GroupingType.SUM) {
            resultProp = addSGProp(null, genSID(), false, false, "", groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.MAX || type == GroupingType.MIN) {
            resultProp = addMGProp(null, genSID(), false, "", type == GroupingType.MIN, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.CONCAT) {
            resultProp = addOGProp(null, genSID(), false, "", GroupType.STRING_AGG, orderProps.size(), ordersNotNull, !ascending, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            resultProp = addAGProp(null, false, genSID(), false, "", type == GroupingType.NAGGR, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.EQUAL) {
            resultProp = addCGProp(null, false, genSID(), false, "", null, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.LAST) {
            resultProp = addOGProp(null, genSID(), false, "", GroupType.LAST, orderProps.size(), ordersNotNull, !ascending, groupPropParamCount, resultParams.toArray());
        }
        return resultProp;
    }

    public LPWithParams addScriptedMaxProp(List<LPWithParams> paramProps, boolean isMin) throws ScriptingErrorLog.SemanticErrorException {
        if (isMin) {
            return addScriptedUProp(Union.MIN, paramProps, "MIN");
        } else {
            return addScriptedUProp(Union.MAX, paramProps, "MAX");
        }
    }

    private LPWithParams addScriptedUProp(Union unionType, List<LPWithParams> paramProps, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedUProp(" + unionType + ", " + paramProps + ");");
        checkPropertyTypes(paramProps, errMsgPropType);

        int[] coeffs = null;
        if (unionType == Union.SUM) {
            coeffs = new int[paramProps.size()];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = 1;
            }
        }
        List<Object> resultParams = getParamsPlainList(paramProps);
        LCP prop = addUProp(null, "", unionType, null, coeffs, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    public LPWithParams addScriptedPartitionProp(PartitionType partitionType, PropertyUsage ungroupPropUsage, boolean strict, int precision, boolean isAscending,
                                                 boolean useLast, int groupPropsCnt, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedPartitionProp(" + partitionType + ", " + ungroupPropUsage + ", " + strict + ", " + precision + ", " +
                                                        isAscending + ", " + useLast + ", " + groupPropsCnt + ", " + paramProps + ");");
        checkPartitionWindowConsistence(partitionType, useLast);
        LP ungroupProp = ungroupPropUsage != null ? findLPByPropertyUsage(ungroupPropUsage) : null;
        checkPartitionUngroupConsistence(ungroupProp, groupPropsCnt);

        boolean ordersNotNull = doesExtendContext(paramProps.subList(0, groupPropsCnt + 1), paramProps.subList(groupPropsCnt + 1, paramProps.size()));

        List<Object> resultParams = getParamsPlainList(paramProps);
        List<Integer> usedParams = mergeAllParams(paramProps);
        LP prop;
        if (partitionType == PartitionType.SUM || partitionType == PartitionType.PREVIOUS) {
            prop = addOProp(null, genSID(), false, "", partitionType, isAscending, ordersNotNull, useLast, groupPropsCnt, resultParams.toArray());
        } else if (partitionType == PartitionType.DISTR_CUM_PROPORTION) {
            prop = addPGProp(null, genSID(), false, precision, strict, "", usedParams.size(), isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        } else {
            prop = addUGProp(null, genSID(), false, strict, "", usedParams.size(), isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        }
        return new LPWithParams(prop, usedParams);
    }

    public LPWithParams addScriptedCCProp(List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCCProp(" + params + ");");
        return addScriptedJProp(addCCProp(params.size()), params);
    }

    public LPWithParams addScriptedConcatProp(String separator, List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConcatProp(" + separator + ", " + params + ");");
        return addScriptedJProp(addSFUProp(params.size(), separator), params);
    }

    public LPWithParams addScriptedDCCProp(LPWithParams ccProp, int index) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDCCProp(" + ccProp + ", " + index + ");");
        checkDeconcatenateIndex(ccProp, index);
        return addScriptedJProp(addDCCProp(index - 1), Arrays.asList(ccProp));
    }

    public LCP addScriptedSFProp(String typeName, String formulaText, boolean hasNotNull) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSFProp(" + typeName + ", " + formulaText + ");");
        Set<Integer> params = findFormulaParameters(formulaText);
        checkFormulaParameters(params);
        if (typeName != null) {
            ValueClass cls = findClassByCompoundName(typeName);
            checkFormulaClass(cls);
            return addSFProp(transformFormulaText(formulaText), (DataClass) cls, params.size(), hasNotNull);
        } else {
            return addSFProp(transformFormulaText(formulaText), params.size(), hasNotNull);
        }
    }

    private Set<Integer> findFormulaParameters(String text) {
        Set<Integer> params = new HashSet<Integer>();
        Pattern pattern = Pattern.compile("\\$\\d+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String group = matcher.group();
            int paramNumber = Integer.valueOf(group.substring(1));
            params.add(paramNumber);
        }
        return params;
    }

    private String transformFormulaText(String text) {
        return text.replaceAll("\\$(\\d+)", "prm$1");
    }

    public LPWithParams addScriptedRProp(List<TypedParameter> context, LPWithParams zeroStep, LPWithParams nextStep, Cycle cycleType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedRProp(" + context + ", " + zeroStep + ", " + nextStep + ", " + cycleType + ");");

        List<Integer> usedParams = mergeAllParams(asList(zeroStep, nextStep));
        checkRecursionContext(getParamNamesFromTypedParams(context), usedParams);

        MOrderExclSet<Integer> mMainParams = SetFact.mOrderExclSetMax(usedParams.size());
        Map<Integer, Integer> usedToResult = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            if (!context.get(usedParams.get(i)).paramName.startsWith("$")) {
                mMainParams.exclAdd(i);
                usedToResult.put(usedParams.get(i), i);
            }
        }
        ImOrderSet<Integer> mainParams = mMainParams.immutableOrder();

        Map<Integer, Integer> mapPrev = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            String param = context.get(usedParams.get(i)).paramName; // usedParams и context orderSet / revMap'ы
            if (param.startsWith("$")) {
                int index = 0;
                for (TypedParameter tparam : context)  {
                    if (tparam.paramName.equals(param.substring(1))) {
                        break;
                    }
                    ++index;
                }
                mapPrev.put(i, usedToResult.get(index));
            }
        }

        List<Object> resultParams = getParamsPlainList(Arrays.asList(zeroStep, nextStep));
        LP res = addRProp(null, genSID(), false, "", cycleType, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

        List<Integer> resUsedParams = new ArrayList<Integer>();
        for (Integer usedParam : usedParams) {
            if (!context.get(usedParam).paramName.startsWith("$")) {
                resUsedParams.add(usedParam);
            }
        }
        return new LPWithParams(res, resUsedParams);
    }

    public LCP addConstantProp(ConstType type, Object value) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addConstantProp(" + type + ", " + value + ");");

        switch (type) {
            case INT: return addCProp(IntegerClass.instance, value);
            case LONG: return addCProp(LongClass.instance, value);
            case NUMERIC: return addNumericConst((String) value);
            case REAL: return addCProp(DoubleClass.instance, value);
            case STRING: return addCProp(StringClass.getv(new ExtInt(((String) value).length())), value);
            case LOGICAL: return addCProp(LogicalClass.instance, value);
            case DATE: return addCProp(DateClass.instance, value);
            case DATETIME: return addCProp(DateTimeClass.instance, value);
            case TIME: return addCProp(TimeClass.instance, value);
            case STATIC: return addStaticClassConst((String) value);
            case COLOR: return addCProp(ColorClass.instance, value);
            case NULL: return baseLM.vnull;
        }
        return null;
    }

    private LCP addNumericConst(String value) {
        return addCProp(NumericClass.get(value.length(), value.length() - value.indexOf('.') - 1), new BigDecimal(value));
    }

    public Color createScriptedColor(int r, int g, int b) throws ScriptingErrorLog.SemanticErrorException {
        if (r > 255 || g > 255 || b > 255) {
            errLog.emitColorComponentValueError(parser);
        }
        return new Color(r, g, b);
    }

    public int createScriptedInteger(String s) throws ScriptingErrorLog.SemanticErrorException {
        int res = 0;
        try {
            res = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            errLog.emitIntegerValueError(parser);
        }
        return res;
    }

    public long createScriptedLong(String s) throws ScriptingErrorLog.SemanticErrorException {
        long res = 0;
        try {
            res = Long.parseLong(s);
        } catch (NumberFormatException e) {
            errLog.emitLongValueError(parser);
        }
        return res;
    }

    public double createScriptedDouble(String s) throws ScriptingErrorLog.SemanticErrorException {
        double res = 0;
        try {
            res = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            errLog.emitDoubleValueError(parser);
        }
        return res;
    }

    private void validateDate(int y, int m, int d) throws ScriptingErrorLog.SemanticErrorException {
        checkRange("year component", y, 1900, 9999);
        checkRange("month component", m, 1, 12);
        checkRange("day component", d, 1, 31);

        final List<Integer> longMonth = Arrays.asList(1, 3, 5, 7, 8, 10, 12);
        if (d == 31 && !longMonth.contains(m) ||
            d == 30 && m == 2 ||
            d == 29 && m == 2 && (y % 4 != 0 || y % 100 == 0 && y % 400 != 0))
        {
            errLog.emitDateDayError(parser, y, m, d);
        }

    }

    private void validateTime(int h, int m) throws ScriptingErrorLog.SemanticErrorException {
        checkRange("hour component", h, 0, 23);
        checkRange("minute component", m, 0, 59);
    }

    private void validateDateTime(int y, int m, int d, int h, int mn) throws ScriptingErrorLog.SemanticErrorException {
        validateDate(y, m, d);
        validateTime(h, mn);
    }

    public java.sql.Date dateLiteralToDate(String text) throws ScriptingErrorLog.SemanticErrorException {
        int y = Integer.parseInt(text.substring(0, 4));
        int m = Integer.parseInt(text.substring(5, 7));
        int d = Integer.parseInt(text.substring(8, 10));
        validateDate(y, m, d);
        return new java.sql.Date(y - 1900, m - 1, d);
    }

    public Timestamp dateTimeLiteralToTimestamp(String text) throws ScriptingErrorLog.SemanticErrorException {
        int y = Integer.parseInt(text.substring(0, 4));
        int m = Integer.parseInt(text.substring(5, 7));
        int d = Integer.parseInt(text.substring(8, 10));
        int h = Integer.parseInt(text.substring(11, 13));
        int mn = Integer.parseInt(text.substring(14, 16));
        validateDateTime(y, m, d, h, mn);
        return new Timestamp(y - 1900, m - 1, d, h, mn, 0, 0);
    }

    public Time timeLiteralToTime(String text) throws ScriptingErrorLog.SemanticErrorException {
        int h = Integer.parseInt(text.substring(0, 2));
        int m = Integer.parseInt(text.substring(3, 5));
        validateTime(h, m);
        return new Time(h, m, 0);
    }

    public LPWithParams addScriptedFAProp(String formName, List<String> objectNames, List<LPWithParams> mapping,
                                          String contextObjectName, LPWithParams contextProperty,
                                          String initFilterPropertyName, List<String> initFilterPropertyMapping,
                                          ModalityType modalityType, FormSessionScope sessionScope,
                                          boolean checkOnOk, boolean showDrop, FormPrintType printType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFAProp(" + formName + ", " + objectNames + ", " + mapping + ", " + modalityType + ", " + sessionScope + ");");

        if (contextProperty != null) {
            checkCalculationProperty(contextProperty.property);
        }

        FormEntity form = findFormByCompoundName(formName);

        ObjectEntity[] objects = new ObjectEntity[objectNames.size()];
        for (int i = 0; i < objectNames.size(); i++) {
            objects[i] = findObjectEntity(form, objectNames.get(i));
        }

        ObjectEntity contextObject = contextObjectName == null ? null : findObjectEntity(form, contextObjectName);

        Version version = getVersion();
        PropertyDrawEntity initFilterProperty = null;
        if (initFilterPropertyName != null) {
            initFilterProperty = initFilterPropertyMapping == null
                                 ? getPropertyDraw(this, form, initFilterPropertyName, version)
                                 : getPropertyDraw(this, form, PropertyDrawEntity.createSID(initFilterPropertyName, initFilterPropertyMapping), version);
        }

        LAP property = addFAProp(null, genSID(), "", form, objects, null, contextObject,
                                 contextProperty == null ? null : (CalcProperty)contextProperty.property.property,
                                 initFilterProperty,
                                 sessionScope, modalityType, checkOnOk, showDrop, printType);

        if (mapping.size() > 0) {
            if (contextProperty != null) {
                for (int usedParam : contextProperty.usedParams) {
                    mapping.add(new LPWithParams(null, singletonList(usedParam)));
                }
            }
            return addScriptedJoinAProp(property, mapping);
        } else {
            List<Integer> usedParams = contextProperty == null ? new ArrayList<Integer>() : contextProperty.usedParams;
            return new LPWithParams(property, usedParams);
        }
    }

    public ObjectEntity findObjectEntity(FormEntity form, String objectName) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity result = form.getNFObject(objectName, getVersion());
        if (result == null) {
            errLog.emitObjectNotFoundError(parser, objectName);
        }
        return result;
    }

    public void addScriptedMetaCodeFragment(String name, List<String> params, List<String> tokens, String code, int lineNumber) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedMetaCodeFragment(" + name + ", " + params + ", " + tokens + ", " + lineNumber + ");");

        checkDuplicateMetaCodeFragment(name, params.size());
        checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(params, tokens, code, getName(), lineNumber);
        addMetaCodeFragment(name, fragment);
    }

    public void runMetaCode(String name, List<String> params, int lineNumber) throws RecognitionException {
        MetaCodeFragment metaCode = findMetaCodeFragmentByCompoundName(name, params.size());
        checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params);
        parser.runMetaCode(this, code, metaCode, metaCodeCallString(name, metaCode, params), lineNumber);
    }

    private String metaCodeCallString(String name, MetaCodeFragment metaCode, List<String> actualParams) {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(name);
        builder.append("(");
        for (int i = 0; i < actualParams.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(metaCode.getParameters().get(i));
            builder.append("=");
            builder.append(actualParams.get(i));
        }
        builder.append(")");
        return builder.toString();
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        return parser.grabMetaCode(metaCodeName);
    }

    private LCP addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos+1);
        LCP resultProp = null;

        ValueClass cls = findClassByCompoundName(className);
        if (cls instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteClass = (ConcreteCustomClass) cls;
            if (concreteClass.hasNFStaticObject(instanceName, version)) {
                resultProp = addCProp(concreteClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static оbject", instanceName);
            }
        } else {
            errLog.emitAbstractClassInstancesUseError(parser, className, instanceName);
        }
        return resultProp;
    }

    public LCP addScriptedGroupObjectProp(String name, GroupObjectProp prop, List<AndClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);
        LCP resultProp = null;

        FormEntity form = findFormByCompoundName(formName);

        GroupObjectEntity groupObject = form.getNFGroupObject(objectName, getVersion());
        if (groupObject != null) {
            for (ObjectEntity obj : groupObject.getOrderObjects()) {
                outClasses.add(obj.baseClass.getUpSet());
            }
            resultProp = addGroupObjectProp(groupObject, prop);
        } else {
            errLog.emitNotFoundError(parser, "group оbject", objectName);
        }
        return resultProp;
    }

    public LPWithParams addScriptedFocusActionProp(PropertyDrawEntity property) throws ScriptingErrorLog.SemanticErrorException {
        return new LPWithParams(addFocusActionProp(property.getID()), new ArrayList<Integer>());
    }

    public LCP addScriptedTypeProp(String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addTypeProp(" + className + ", " + (bIs ? "IS" : "AS") + ");");
        if (bIs) {
            return is(findClassByCompoundName(className));
        } else {
            return object(findClassByCompoundName(className));
        }
    }

    public LP addScriptedTypeExprProp(LP mainProp, LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(mainProp, asList(property)).property;
    }

    public void addScriptedConstraint(LP property, Event event, boolean checked, List<PropertyUsage> propUsages, String message) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConstraint(" + property + ", " + checked + ", " + propUsages + ", " + message + ");");
        if (!property.property.check(true)) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        property.property.caption = message;
        ImSet<CalcProperty<?>> checkedProps = null;
        CalcProperty.CheckType type = (checked ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO);
        if (checked && propUsages != null) {
            MSet<CalcProperty<?>> mCheckedProps = SetFact.mSet();
            for (PropertyUsage propUsage : propUsages) {
                mCheckedProps.add((CalcProperty<?>) findLPByPropertyUsage(propUsage).property);
            }
            type = CalcProperty.CheckType.CHECK_SOME;
            checkedProps = mCheckedProps.immutable();
        }
        addConstraint((LCP<?>) property, type, checkedProps, event, this);
    }

    private PrevScope prevScope = null;
    public void setPrevScope(Event event) {
        assert prevScope == null;
        prevScope = event.getScope();
    }

    public void dropPrevScope(Event event) {
        assert prevScope.equals(event.getScope());
        prevScope = null;
    }

    public LPWithParams addScriptedSessionProp(IncrementType type, LPWithParams property) {
        scriptLogger.info("addScriptedSessionProp(" + type + ", " + property + ");");
        LCP newProp;
        PrevScope scope = (prevScope == null ? PrevScope.DB : prevScope);
        if (type == null) {
            newProp = addOldProp((LCP) property.property, scope);
        } else {
            newProp = addCHProp((LCP) property.property, type, scope);
        }
        return new LPWithParams(newProp, property.usedParams);
    }

    public LPWithParams addScriptedSignatureProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSignatureProp(" + property + ");");
        checkCalculationProperty(property.property);
        LCP newProp = addClassProp((LCP) property.property);
        return new LPWithParams(newProp, property.usedParams);
    }

    public void addScriptedFollows(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, List<Integer> options, List<LPWithParams> props, List<Event> sessions) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFollows(" + mainPropUsage + ", " + namedParams + ", " + options + ", " + props + ", " + sessions + ");");
        LCP mainProp = (LCP) findLPByPropertyUsage(mainPropUsage);
        checkProperty(mainProp, mainPropUsage.name);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        for (int i = 0; i < props.size(); i++) {
            Integer[] params = new Integer[props.get(i).usedParams.size()];
            for (int j = 0; j < params.length; j++) {
                params[j] = props.get(i).usedParams.get(j) + 1;
            }
            follows(mainProp, options.get(i), sessions.get(i), (LCP) props.get(i).property, params);
        }
    }

    public void addScriptedWriteWhen(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, LPWithParams valueProp, LPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedWriteWhen(" + mainPropUsage + ", " + namedParams + ", " + valueProp + ", " + whenProp + ");");
        LP mainProp = findLPByPropertyUsage(mainPropUsage);
        if (!(mainProp.property instanceof DataProperty)) {
            errLog.emitOnlyDataPropertyIsAllowedError(parser, mainPropUsage.name);
        }
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(getParamNamesFromTypedParams(namedParams));
        checkCalculationProperty(mainProp);

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        ((LCP)mainProp).setEventChange(this, action, params.toArray());
    }

    public Set<CalcProperty> findPropsByPropertyUsages(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages==null)
            return null;

        Set<CalcProperty> props = new HashSet<CalcProperty>(); // функционально из-за exception'а не сделаешь
        for (PropertyUsage usage : propUsages) {
            props.add(((LCP<?>)findLPByPropertyUsage(usage)).property); // todo [dale]: добавить семантическую ошибку
        }
        return props;
    }

    public final static GetValue<CalcProperty, LCP> getProp = new GetValue<CalcProperty, LCP>() {
        public CalcProperty getMapValue(LCP value) {
            return ((LCP<?>)value).property;
        }};

    public void addScriptedEvent(LPWithParams whenProp, LPWithParams event, List<LPWithParams> orders, boolean descending, Event baseEvent, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEvent(" + whenProp + ", " + event + ", " + orders + ", " + descending + ", " + baseEvent + ");");
        checkActionProperty(event.property);
        if(noInline==null) {
            noInline = new ArrayList<LPWithParams>();
            for(Integer usedParam : whenProp.usedParams)
                noInline.add(new LPWithParams(null, asList(usedParam)));
        }
        List<Object> params = getParamsPlainList(asList(event, whenProp), orders, noInline);
        addEventAction(baseEvent, descending, false, noInline.size(), forceInline, params.toArray());
    }

    public void addScriptedGlobalEvent(LPWithParams event, Event baseEvent, boolean single, PropertyUsage showDep) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGlobalEvent(" + event + ", " + baseEvent + ");");
        checkActionProperty(event.property);
        checkEventNoParameters(event.property);
        ActionProperty action = (ActionProperty) event.property.property;
        if(showDep!=null)
            action.showDep = findLPByPropertyUsage(showDep).property;
        addBaseEvent(action, baseEvent, false, single);
    }

    public void addScriptedShowDep(PropertyUsage property, PropertyUsage propFrom) throws ScriptingErrorLog.SemanticErrorException {
        findLPByPropertyUsage(property).property.showDep = findLPByPropertyUsage(propFrom).property;
    }

    public void addScriptedAspect(PropertyUsage mainPropUsage, List<TypedParameter> mainPropParams, LPWithParams actionProp, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAspect(" + mainPropUsage + ", " + mainPropParams + ", " + actionProp + ", " + before + ");");
        LP mainProp = findLPByPropertyUsage(mainPropUsage);
        checkParamCount(mainProp, mainPropParams.size());
        checkDistinctParameters(getParamNamesFromTypedParams(mainPropParams)); // todo [dale]: надо, наверное, это вынести в отдельный метод
        checkActionProperty(actionProp.property);
        checkActionProperty(mainProp);

        LAP<PropertyInterface> mainActionLP = (LAP<PropertyInterface>) mainProp;

        List<Object> params = getParamsPlainList(Arrays.asList(actionProp));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(mainActionLP.listInterfaces, params.toArray());
        addAspectEvent((ActionProperty) mainActionLP.property, actionImplements.get(0), before);
    }

    public void addScriptedTable(String name, List<String> classIds) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedTable(" + name + ", " + classIds + ");");
        checkDuplicateTable(name);

        ValueClass[] classes = new ValueClass[classIds.size()];
        for (int i = 0; i < classIds.size(); i++) {
            classes[i] = findClassByCompoundName(classIds.get(i));
        }
        addTable(name, classes);
    }

    public List<LCP> indexedProperties = new ArrayList<LCP>();
    
    public void addScriptedIndex(LP property) throws ScriptingErrorLog.SemanticErrorException {
        checkCalculationProperty(property);
        indexedProperties.add((LCP) property);        
    } 
    
    
    public void addScriptedIndex(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIndex(" + propUsages + ");");
        LCP[] lps = new LCP[propUsages.size()];
        for (int i = 0; i < propUsages.size(); i++) {
            lps[i] = (LCP) findLPByPropertyUsage(propUsages.get(i));
        }
        addIndex(lps);
    }

    public void addScriptedLoggable(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedLoggable(" + propUsages + ");");

        for (PropertyUsage propUsage : propUsages) {
            LCP lp = (LCP) findLPByPropertyUsage(propUsage);
            lp.makeLoggable(BL.systemEventsLM);
        }
    }

    public void addScriptedWindow(WindowType type, String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        if (scriptLogger.isInfoEnabled()) {
            scriptLogger.info("addScriptedWindow(" + name + ", " + type + ", " + caption + ", " + options + ");");
        }

        checkDuplicateWindow(name);

        NavigatorWindow window = null;
        switch (type) {
            case MENU:
                window = createMenuWindow(name, caption, options);
                break;
            case PANEL:
                window = createPanelWindow(name, caption, options);
                break;
            case TOOLBAR:
                window = createToolbarWindow(name, caption, options);
                break;
            case TREE:
                window = createTreeWindow(caption, options);
                break;
        }

        window.drawRoot = nvl(options.getDrawRoot(), false);
        window.drawScrollBars = nvl(options.getDrawScrollBars(), true);
        window.titleShown = nvl(options.getDrawTitle(), true);

        addWindow(name, window);
    }

    private MenuNavigatorWindow createMenuWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        DockPosition dp = options.getDockPosition();
        if (dp == null) {
            errLog.emitWindowPositionNotSpecified(parser, name);
        }

        MenuNavigatorWindow window = new MenuNavigatorWindow(null, caption, dp.x, dp.y, dp.width, dp.height);
        window.orientation = orientation.asMenuOrientation();

        return window;
    }

    private PanelNavigatorWindow createPanelWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            errLog.emitWindowOrientationNotSpecified(parser, name);
        }

        PanelNavigatorWindow window = new PanelNavigatorWindow(orientation.asToolbarOrientation(), null, caption);
        if (dockPosition != null) {
            window.setDockPosition(dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        }
        return window;
    }

    private ToolBarNavigatorWindow createToolbarWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        BorderPosition borderPosition = options.getBorderPosition();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            errLog.emitWindowOrientationNotSpecified(parser, name);
        }

        if (borderPosition != null && dockPosition != null) {
            errLog.emitWindowPositionConflict(parser, name);
        }

        ToolBarNavigatorWindow window;
        if (borderPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption, borderPosition.asLayoutConstraint());
        } else if (dockPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption, dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        } else {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption);
        }

        Alignment hAlign = options.getHAlign();
        Alignment vAlign = options.getVAlign();
        Alignment thAlign = options.getTextHAlign();
        Alignment tvAlign = options.getTextVAlign();
        if (hAlign != null) {
            window.alignmentX = asHorizontalToolbarAlign(hAlign);
        }
        if (vAlign != null) {
            window.alignmentY = asVerticalToolbarAlign(vAlign);
        }
        if (thAlign != null) {
            window.horizontalTextPosition = asHorizontalTextPosition(thAlign);
        }
        if (tvAlign != null) {
            window.verticalTextPosition = asVerticalTextPosition(tvAlign);
        }
        return window;
    }

    private TreeNavigatorWindow createTreeWindow(String caption, NavigatorWindowOptions options) {
        TreeNavigatorWindow window = new TreeNavigatorWindow(null, caption);
        DockPosition dp = options.getDockPosition();
        if (dp != null) {
            window.setDockPosition(dp.x, dp.y, dp.width, dp.height);
        }
        return window;
    }


    public void hideWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        findWindowByCompoundName(name).visible = false;
    }

    public NavigatorElement createScriptedNavigatorElement(String name, String caption, NavigatorElement<?> parentElement, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName, PropertyUsage actionUsage, String icon) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedNavigatorElement(" + name + ", " + caption + ");");

        assert name != null && caption != null;

        checkDuplicateNavigatorElement(name);

        NavigatorElement newElement;

        if (actionUsage != null) {
            LP findResult = findLPByPropertyUsage(actionUsage);
            checkActionProperty(findResult);
            LAP<?> actionProperty = (LAP<?>) findResult;

            newElement = addNavigatorAction(name, caption, actionProperty, icon);
        } else {
            newElement = addNavigatorElement(name, caption, icon);
        }

        setupNavigatorElement(newElement, caption, parentElement, pos, anchorElement, windowName);

        return newElement;
    }

    public void setupNavigatorElement(NavigatorElement<?> element, String caption, NavigatorElement<?> parentElement, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("setupNavigatorElement(" + element.getSID() + ", " + caption + ", " + parentElement + ", " + pos + ", " + anchorElement + ", " + windowName + ");");

        assert element != null;

        if (caption != null) {
            element.caption = caption;
        }

        if (windowName != null) {
            setNavigatorElementWindow(element, windowName);
        }

        if (parentElement != null) {
            moveElement(element, parentElement, pos, anchorElement);
        }
    }

    private void moveElement(NavigatorElement element, NavigatorElement parentElement, InsertPosition pos, NavigatorElement anchorElement) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();
        
        if (anchorElement != null && !parentElement.equals(anchorElement.getNFParent(version))) {
            errLog.emitIllegalInsertBeforeAfterComponentElement(parser, element.getSID(), parentElement.getSID(), anchorElement.getSID());
        }

        if (element.isAncestorOf(parentElement, version)) {
            errLog.emitIllegalMoveNavigatorToSubnavigator(parser, element.getSID(), parentElement.getSID());
        }

        switch (pos) {
            case IN:
                parentElement.add(element, version);
                break;
            case BEFORE:
                parentElement.addBefore(element, anchorElement, version);
                break;
            case AFTER:
                parentElement.addAfter(element, anchorElement, version);
                break;
            case FIRST:
                parentElement.addFirst(element, version);
                break;
        }
    }

    public void setNavigatorElementWindow(NavigatorElement element, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        assert element != null && windowName != null;

        AbstractWindow window = findWindowByCompoundName(windowName);
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, windowName);
        }

        if (window instanceof NavigatorWindow) {
            element.window = (NavigatorWindow) window;
        } else {
            errLog.emitAddToSystemWindowError(parser, windowName);
        }
    }

    private void checkGroup(AbstractGroup group, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (group == null) {
            errLog.emitGroupNotFoundError(parser, name);
        }
    }

    private void checkClass(ValueClass cls, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (cls == null) {
            errLog.emitClassNotFoundError(parser, name);
        }
    }

    private void checkProperty(LP lp, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, name);
        }
    }

    private void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
        }
    }

    private void checkNamespace(String namespaceName) throws ScriptingErrorLog.SemanticErrorException {
        if (!namespaceToModules.containsKey(namespaceName)) {
            errLog.emitNamespaceNotFoundError(parser, namespaceName);
        }
    }

    private void checkWindow(AbstractWindow window, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, name);
        }
    }

    private void checkNavigatorElement(NavigatorElement element, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (element == null) {
            errLog.emitNavigatorElementNotFoundError(parser, name);
        }
    }

    private void checkTable(ImplementTable table, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (table == null) {
            errLog.emitTableNotFoundError(parser, name);
        }
    }

    private void checkForm(NavigatorElement navElement, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (!(navElement instanceof FormEntity)) {
            errLog.emitFormNotFoundError(parser, name);
        }
    }

    private void checkMetaCodeFragment(MetaCodeFragment code, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (code == null) {
            errLog.emitMetaCodeFragmentNotFoundError(parser, name);
        }
    }

    private void checkParamCount(LP mainProp, int paramCount) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramCount) {
            errLog.emitParamCountError(parser, mainProp, paramCount);
        }
    }

    public void checkPropertyValue(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property instanceof CalcProperty && !property.property.check(false) && !alwaysNullProperties.containsKey(property.property)) {
            String path = parser.getCurrentScriptPath(getName(), parser.getCurrentParserLineNumber(), "\n\t\t\t");
            String location = path + ":" + (parser.getCurrentParser().input.LT(1).getCharPositionInLine() + 1);
            alwaysNullProperties.put(property.property, location);
        }
    }

    private void checkDuplicateClass(String className) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingClass(getNamespace(), className);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "class", className, module.getName());
        }
    }

    private void checkDuplicateGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingGroup(getNamespace(), groupName);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "group", groupName, module.getName());
        }
    }

    private void checkDuplicateProperty(String propName, List<AndClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingLP(getNamespace(), propName, signature);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "property", propName, module.getName());
        }
    }

    private void checkDuplicateWindow(String windowName) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingWindow(getNamespace(), windowName);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "window", windowName, module.getName());
        }
    }

    private void checkDuplicateNavigatorElement(String name) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingNavigatorElement(getNamespace(), name);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "form or navigator", name, module.getName());
        }
    }

    private void checkDuplicateMetaCodeFragment(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingMetaCode(getNamespace(), name, paramCnt);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "meta code", name, module.getName());
        }
    }

    private void checkDuplicateTable(String name) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingTable(getNamespace(), name);
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "table", name, module.getName());
        }
    }

    private void checkPropertyTypes(List<LPWithParams> properties, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        Property prop1 = properties.get(0).property.property;
        for (int i = 1; i < properties.size(); i++) {
            Property prop2 = properties.get(i).property.property;
            if (prop1.getType() != null && prop2.getType() != null && prop1.getType().getCompatible(prop2.getType()) == null) {
                errLog.emitIncompatibleTypes(parser, errMsgPropType);
            }
        }
    }

    private void checkStaticClassConstraints(boolean isAbstract, List<String> instNames, List<String> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isAbstract && !instNames.isEmpty()) {
            errLog.emitAbstractClassInstancesDefError(parser);
        }

        Set<String> names = new HashSet<String>();
        for (String name : instNames) {
            if (names.contains(name)) {
                errLog.emitAlreadyDefinedError(parser, "instance", name);
            }
            names.add(name);
        }
    }

    private void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        Set<ValueClass> parentsSet = new HashSet<ValueClass>();
        for (String parentName : parents) {
            ValueClass valueClass = findClassByCompoundName(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }

            if (parentsSet.contains(valueClass)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            parentsSet.add(valueClass);
        }
    }

    private void checkFormulaClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormulaReturnClassError(parser);
        }
    }

    private void checkFormDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormDataClassError(parser);
        }
    }

    private void checkChangeClassActionClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof ConcreteCustomClass)) {
            errLog.emitChangeClassActionClassError(parser);
        }
    }

    private void checkFormulaParameters(Set<Integer> params) throws ScriptingErrorLog.SemanticErrorException {
        for (int param : params) {
            if (param == 0 || param > params.size()) {
                errLog.emitParamIndexError(parser, param, params.size());
            }
        }
    }

    private void checkNamedParams(LP property, List<String> namedParams) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() != namedParams.size() && !namedParams.isEmpty()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    private void checkDistinctParameters(List<String> params) throws ScriptingErrorLog.SemanticErrorException {
        Set<String> paramsSet = new HashSet<String>(params);
        if (paramsSet.size() < params.size()) {
            errLog.emitDistinctParamNamesError(parser);
        }
    }

    private void checkMetaCodeParamCount(MetaCodeFragment code, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (code.parameters.size() != paramCnt) {
            errLog.emitParamCountError(parser, code.parameters.size(), paramCnt);
        }
    }

    private void checkGPropOrderConsistence(GroupingType type, int orderParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && type != GroupingType.LAST && orderParamsCnt > 0) {
            errLog.emitRedundantOrderGPropError(parser, type);
        }
    }

    private void checkGPropAggregateConsistence(GroupingType type, int aggrParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && aggrParamsCnt > 1) {
            errLog.emitMultipleAggrGPropError(parser, type);
        }
        if (type == GroupingType.CONCAT && aggrParamsCnt != 2) {
            errLog.emitConcatAggrGPropError(parser);
        }
    }

    private void checkGPropAggrConstraints(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (mainProps.get(0).property != null) {
                errLog.emitNonObjectAggrGPropError(parser);
            }
        }
    }

    private void checkGPropWhereConsistence(GroupingType type, LPWithParams where) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.AGGR && type != GroupingType.NAGGR && type != GroupingType.LAST && where != null) {
            errLog.emitWhereGPropError(parser, type);
        }
    }

    public void checkActionAllParamsUsed(List<TypedParameter> context, LP property, boolean ownContext) throws ScriptingErrorLog.SemanticErrorException {
        if (ownContext && context.size() > property.property.interfaces.size()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    public void checkActionProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LAP<?>)) {
            errLog.emitNotActionPropertyError(parser);
        }
    }

    public void checkAddActionsClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitAddActionsClassError(parser);
        }
    }

    public void checkCalculationProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LCP<?>)) {
            errLog.emitNotCalculationPropertyError(parser);
        }
    }

    public void checkActionLocalContext(List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (oldContext.size() != newContext.size()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    private void checkForActionPropertyConstraints(boolean isRecursive, List<Integer> oldContext, List<Integer> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (!isRecursive && oldContext.size() == newContext.size()) {
            errLog.emitForActionSameContextError(parser);
        }
    }

    private void checkRecursionContext(List<String> context, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        for (String param : context) {
            if (param.startsWith("$")) {
                int indexPlain = context.indexOf(param.substring(1));
                if (indexPlain < 0) {
                    errLog.emitParamNotFoundError(parser, param.substring(1));
                }
                if (!usedParams.contains(indexPlain)) {
                    errLog.emitParameterNotUsedInRecursionError(parser, param.substring(1));
                }
            }
        }
    }

    public void checkNecessaryProperty(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property == null) {
            errLog.emitNecessaryPropertyError(parser);
        }
    }

    public void checkDeconcatenateIndex(LPWithParams property, int index) throws ScriptingErrorLog.SemanticErrorException {
        Type propType = property.property.property.getType();
        if (propType instanceof ConcatenateType) {
            int concatParts = ((ConcatenateType) propType).getPartsCount();
            if (index <= 0 || index > concatParts) {
                errLog.emitDeconcatIndexError(parser, index, concatParts);
            }
        } else {
            errLog.emitDeconcatError(parser);
        }
    }

    private void checkPartitionWindowConsistence(PartitionType partitionType, boolean useLast) throws ScriptingErrorLog.SemanticErrorException {
        if (!useLast && (partitionType != PartitionType.SUM && partitionType != PartitionType.PREVIOUS)) {
            errLog.emitIllegalWindowPartitionError(parser);
        }
    }

    private void checkPartitionUngroupConsistence(LP ungroupProp, int groupPropCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (ungroupProp != null && ungroupProp.property.interfaces.size() != groupPropCnt) {
            errLog.emitUngroupParamsCntPartitionError(parser, groupPropCnt);
        }
    }

    private void checkClassWhere(LCP<?> property, String name) {
        ClassWhere<Integer> classWhere = property.getClassWhere(ClassType.ASIS);
        boolean needWarning = false;
        if (classWhere.wheres.length > 1) {
            needWarning = true;
        } else {
            AbstractClassWhere.And<Integer> where = classWhere.wheres[0];
            for (int i = 0; i < where.size(); ++i) {
                AndClassSet acSet = where.getValue(i);
                if (acSet instanceof UpClassSet && ((UpClassSet)acSet).wheres.length > 1 ||
                    acSet instanceof OrObjectClassSet && ((OrObjectClassSet)acSet).up.wheres.length > 1) {

                    needWarning = true;
                    break;
                }
            }
        }
        if (needWarning) {
            warningList.add(" Property " + name + " has class where " + classWhere);
        }
    }

    public void checkAbstractProperty(LCP property, String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property.property instanceof CaseUnionProperty && ((CaseUnionProperty)property.property).isAbstract())) {
            errLog.emitNotAbstractPropertyError(parser, propName);
        }
    }

    public void checkAbstractAction(LAP action, String actionName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(action.property instanceof ListCaseActionProperty && ((ListCaseActionProperty)action.property).isAbstract())) {
            errLog.emitNotAbstractActionError(parser, actionName);
        }
    }

    public void checkEventNoParameters(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() > 0) {
            errLog.emitEventNoParametersError(parser);
        }
    }

    public void checkChangeClassWhere(boolean contextExtended, LPWithParams param, LPWithParams where, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (contextExtended && (where == null || !where.usedParams.contains(param.usedParams.get(0)))) {
            errLog.emitChangeClassWhereError(parser, newContext.get(newContext.size() - 1).paramName);
        }
    }

    public void checkAddObjTOParams(int contextSize, List<LPWithParams> toPropMapping) throws ScriptingErrorLog.SemanticErrorException {
        if (toPropMapping != null) {
            for (LPWithParams param : toPropMapping) {
                if (param.usedParams.get(0) < contextSize) {
                    errLog.emitAddObjToPropertyError(parser);
                }
            }
        }
    }

    public void checkAbstractTypes(boolean isCase, boolean implIsCase) throws ScriptingErrorLog.SemanticErrorException {
        if (isCase && !implIsCase) {
            errLog.emitAbstractCaseImplError(parser);
        }
        if (!isCase && implIsCase) {
            errLog.emitAbstractNonCaseImplError(parser);
        }
    }

    public void checkRange(String valueType, int value, int lbound, int rbound) throws ScriptingErrorLog.SemanticErrorException {
        if (value < lbound || value > rbound) {
            errLog.emitOutOfRangeError(parser, valueType, lbound, rbound);
        }
    }

    public void initModulesAndNamespaces(List<String> requiredModules, List<String> namespacePriority) throws ScriptingErrorLog.SemanticErrorException {
        initNamespacesToModules(this, new HashSet<LogicsModule>());

        if (getNamespace().contains("_")) {
            errLog.emitNamespaceNameError(parser, getNamespace());
        }

        if (namespacePriority.contains(getNamespace())) {
            errLog.emitOwnNamespacePriorityError(parser, getNamespace());
        }

        for (String namespaceName : namespacePriority) {
            checkNamespace(namespaceName);
        }

        for (String moduleName : requiredModules) {
            checkModule(BL.getModule(moduleName), moduleName);
        }

        Set<String> prioritySet = new HashSet<String>();
        for (String namespaceName : namespacePriority) {
            if (prioritySet.contains(namespaceName)) {
                errLog.emitNonUniquePriorityListError(parser, namespaceName);
            }
            prioritySet.add(namespaceName);
        }
    }

    public boolean semicolonNeeded() {
        return parser.semicolonNeeded();
    }

    public void setPropertyScriptInfo(LP property, String script, int lineNumber) {
        property.setCreationScript(script);
        property.setCreationPath(parser.getCurrentScriptPath(getName(), lineNumber, "\n"));
    }

    private void parseStep(ScriptParser.State state) throws RecognitionException {
        try {
            parser.initParseStep(this, createStream(), state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initNamespacesToModules(LogicsModule module, Set<LogicsModule> visitedModules) {
        visitedModules.add(module);
        String namespaceName = module.getNamespace();
        if (!namespaceToModules.containsKey(namespaceName)) {
            namespaceToModules.put(namespaceName, BaseUtils.toList(module));
        } else {
            namespaceToModules.get(namespaceName).add(module);
        }
        for (String requiredModuleName : module.getRequiredModules()) {
            LogicsModule requiredModule = BL.getModule(requiredModuleName);
            assert requiredModule != null;
            if (!visitedModules.contains(requiredModule)) {
                initNamespacesToModules(requiredModule, visitedModules);
            }
        }
    }

    @Override
    public void initModuleDependencies() throws RecognitionException {
        parseStep(ScriptParser.State.PRE);
    }

    @Override
    public void initModule() throws RecognitionException {
        parseStep(ScriptParser.State.INIT);
    }

    @Override
    public void initClasses() throws RecognitionException {
        initBaseClassAliases();
        parseStep(ScriptParser.State.CLASS);
    }

    @Override
    public void initTables() throws RecognitionException {
        parseStep(ScriptParser.State.TABLE);
    }

    @Override
    public void initGroups() throws RecognitionException {
        initBaseGroupAliases();
        parseStep(ScriptParser.State.GROUP);
    }

    @Override
    public void initProperties() throws RecognitionException {
        warningList.clear();
        parseStep(ScriptParser.State.PROP);
    }

    @Override
    public void initIndexes() throws RecognitionException {
        parseStep(ScriptParser.State.INDEX);
        for (LCP property : indexedProperties) {
            addIndex(property);
        }
        indexedProperties.clear();
        if (!parser.isInsideMetacode()) {
            showWarnings();
        }
    }

    public void initScriptingModule(String name, String namespace, List<String> requiredModules, List<String> namespacePriority) {
        setModuleName(name);
        setNamespace(namespace == null ? name : namespace);
        setRequiredModules(new HashSet<String>(requiredModules));
        this.namespacePriority = namespacePriority;
    }

    public void initAliases() {
        initBaseGroupAliases();
        initBaseClassAliases();
    }

    private void showWarnings() {
        for (String warningText : warningList) {
            scriptLogger.warn("WARNING!" + warningText);
        }
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
    }

    @Override
    public String getNamePrefix() {
        return getNamespace();
    }

    public static class PropertyUsage {
        public String name;
        public List<String> classNames;
        
        public PropertyUsage(String name) {
            this(name, null);
        }
        
        public PropertyUsage(String name, List<String> classNames) {
            this.name = name;
            this.classNames = classNames;
        }
    }
    
    public class TypedParameter {
        public ValueClass cls;
        public String paramName;
        
        public TypedParameter(ValueClass cls, String name) {
            this.cls = cls;
            paramName = name;
        }  
        
        public TypedParameter(String cName, String pName) throws ScriptingErrorLog.SemanticErrorException {
            if (cName != null) {
                cls = findClassByCompoundName(cName);
            } else {
                cls = null;
            }                                
            paramName = pName;
        }
    }
    
    public class CompoundNameResolver<T, P> {
        private ModuleFinder<T, P> finder;

        public CompoundNameResolver(ModuleFinder<T, P> finder) {
            this.finder = finder;
        }

        protected List<FoundItem<T>> findInNamespace(String namespaceName, String name, P param) throws ScriptingErrorLog.SemanticErrorException {
            NamespaceElementFinder<T, P> nsFinder = new NamespaceElementFinder<T, P>(finder, namespaceToModules.get(namespaceName));
            return finalizeNamespaceResult(nsFinder.findInNamespace(namespaceName, name, param), name, param);
        }

        protected List<FoundItem<T>> finalizeNamespaceResult(List<FoundItem<T>> result, String name, P param) throws ScriptingErrorLog.SemanticErrorException {
            FoundItem<T> finalRes = finalizeResult(result, name, param);
            return finalRes.value == null ? new ArrayList<FoundItem<T>>() : Collections.singletonList(finalRes); 
        } 
        
        // реализация по умолчанию, предполагающая, что не может быть более одного подходящего объекта
        protected FoundItem<T> finalizeResult(List<FoundItem<T>> result, String name, P param) throws ScriptingErrorLog.SemanticErrorException {
            if (result.isEmpty()) return new FoundItem<T>(null, null);
            if (result.size() > 1) {
                List<LogicsModule> resModules = new ArrayList<LogicsModule>();
                for (FoundItem<T> item : result) {
                    resModules.add(item.module);
                }
                errLog.emitAmbiguousNameError(parser, resModules, name);    
            }
            return result.get(0);
        } 
        
        private T findInRequiredModules(String name, P param, List<String> namespaces) throws ScriptingErrorLog.SemanticErrorException {
            for (String namespaceName : namespaces) {
                List<FoundItem<T>> result = findInNamespace(namespaceName, name, param);
                if (!result.isEmpty()) {
                    return finalizeResult(result, name, param).value;
                }
            }

            Set<String> checkedNamespaces = new HashSet<String>(namespaces);
            List<FoundItem<T>> resultList = new ArrayList<FoundItem<T>>();
            for (Map.Entry<String, List<LogicsModule>> e : namespaceToModules.entrySet()) {
                if (!checkedNamespaces.contains(e.getKey())) {
                    for (LogicsModule module : e.getValue()) {
                        List<T> moduleResult = finder.resolveInModule(module, name, param);
                        for (T obj : moduleResult) {
                            resultList.add(new FoundItem<T>(obj, module));
                        }
                    }
                }
            }
            return finalizeResult(resultList, name, param).value;
        }

        public final T resolve(String name) throws ScriptingErrorLog.SemanticErrorException {
            return resolve(name, null);
        } 
        
        public final T resolve(String name, P param) throws ScriptingErrorLog.SemanticErrorException {
            T result;
            int dotPosition = name.indexOf('.');
            if (dotPosition > 0) {
                String namespaceName = name.substring(0, dotPosition);
                checkNamespace(namespaceName);
                List<FoundItem<T>> foundItems = findInNamespace(namespaceName, name.substring(dotPosition + 1), param);
                return finalizeResult(foundItems, name, param).value;
            } else {
                List<String> namespaces = new ArrayList<String>();
                namespaces.add(getNamespace());
                namespaces.addAll(namespacePriority);
                result = findInRequiredModules(name, param, namespaces);
            }
            return result;
        }
    }
    
    public class LPResolver extends CompoundNameResolver<LP<?, ?>, List<AndClassSet>> {
        public LPResolver(ModuleFinder<LP<?, ?>, List<AndClassSet>> finder) {
            super(finder);
        }

        @Override
        protected List<FoundItem<LP<?, ?>>> finalizeNamespaceResult(List<FoundItem<LP<?, ?>>> result, String name, List<AndClassSet> param) throws ScriptingErrorLog.SemanticErrorException {
            List<FoundItem<LP<?, ?>>> nsResult = new ArrayList<FoundItem<LP<?, ?>>>();
            for (FoundItem<LP<?, ?>> item : result) {
                if (match(item.module.getParamClasses(item.value), param, false)) {
                    nsResult.add(item);    
                }
            }
            if (nsResult.isEmpty()) {
                nsResult = result;
            }
            return nsResult;
        }


        @Override
        protected FoundItem<LP<?, ?>> finalizeResult(List<FoundItem<LP<?, ?>>> result, String name, List<AndClassSet> param) throws ScriptingErrorLog.SemanticErrorException {
            List<LogicsModule> errorModules = new ArrayList<LogicsModule>();
            FoundItem<LP<?, ?>> finalItem = new FoundItem<LP<?, ?>>(null, null);

            List<FoundItem<LP<?, ?>>> directResults = new ArrayList<FoundItem<LP<?, ?>>>();
            List<FoundItem<LP<?, ?>>> indirectResults = new ArrayList<FoundItem<LP<?, ?>>>();
            for (FoundItem<LP<?, ?>> item : result) {
                if (match(item.module.getParamClasses(item.value), param, false)) {
                    directResults.add(item);
                } else {
                    indirectResults.add(item);
                }
            }
            
            if (!directResults.isEmpty()) {
                List<FoundItem<LP<?, ?>>> filteredDirectResults = NamespacePropertyFinder.filterFoundProperties(directResults);
                if (filteredDirectResults.size() > 1) {
                    for (FoundItem<LP<?, ?>> item : filteredDirectResults) {
                        errorModules.add(item.module);
                    }
                } else if (filteredDirectResults.size() == 1) {
                    finalItem = filteredDirectResults.get(0);
                }
                
            } else {
                if (indirectResults.size() > 1) {
                    for (FoundItem<LP<?, ?>> item : indirectResults) {
                        errorModules.add(item.module);
                    }
                } else if (indirectResults.size() == 1) {
                    finalItem = indirectResults.get(0);
                }
            }
            if (errorModules.size() > 1) {
                errLog.emitAmbiguousNameError(parser, errorModules, name); // todo [dale]: сделать нормальную ошибку                    
            }
            return finalItem;
        }
    }
}
