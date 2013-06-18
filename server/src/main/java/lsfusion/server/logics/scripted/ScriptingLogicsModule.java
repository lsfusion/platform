package lsfusion.server.logics.scripted;

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
import lsfusion.interop.ModalityType;
import lsfusion.server.ServerLoggers;
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
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.GroupObjectProp;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.window.*;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ScriptParsingException;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.Event;
import lsfusion.server.logics.property.actions.BaseEvent;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.mail.AttachmentFormat;
import lsfusion.server.mail.EmailActionProperty;
import lsfusion.server.mail.EmailActionProperty.FormStorageType;
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

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {

    private final static Logger scriptLogger = ServerLoggers.scriptLogger;

    private final CompoundNameResolver<LP<?, ?>> lpResolver = new CompoundNameResolver<LP<?, ?>>(new LPNameModuleFinder());
    private final CompoundNameResolver<AbstractGroup> groupResolver = new CompoundNameResolver<AbstractGroup>(new GroupNameModuleFinder());
    private final CompoundNameResolver<NavigatorElement> navigatorResolver = new CompoundNameResolver<NavigatorElement>(new NavigatorElementNameModuleFinder());
    private final CompoundNameResolver<AbstractWindow> windowResolver = new CompoundNameResolver<AbstractWindow>(new WindowNameModuleFinder());
    private final CompoundNameResolver<ImplementTable> tableResolver = new CompoundNameResolver<ImplementTable>(new TableNameModuleFinder());
    private final CompoundNameResolver<ValueClass> classResolver = new CompoundNameResolver<ValueClass>(new ClassNameModuleFinder());

    private final BusinessLogics<?> BL;

    private String code = null;
    private String filename = null;
    private List<String> namespacePriority;
    private final ScriptingErrorLog errLog;
    private ScriptParser parser;
    private List<String> warningList = new ArrayList<String>();
    private Map<Property, String> alwaysNullProperties = new HashMap<Property, String>();

    private String lastOpimizedJPropSID = null;

    private Map<String, LP<?, ?>> currentLocalProperties = new HashMap<String, LP<?, ?>>();

    private Map<String, List<LogicsModule>> namespaceToModules = new LinkedHashMap<String, List<LogicsModule>>();

    public enum ConstType { STATIC, INT, REAL, NUMERIC, STRING, LOGICAL, LONG, DATE, DATETIME, TIME, COLOR, NULL }
    public enum InsertPosition {IN, BEFORE, AFTER, FIRST}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, AGGR, EQUAL}

    private Map<String, DataClass> primitiveTypeAliases = BaseUtils.buildMap(
            asList("INTEGER", "DOUBLE", "LONG", "DATE", "BOOLEAN", "DATETIME", "TEXT", "TIME", "WORDFILE", "IMAGEFILE", "PDFFILE", "CUSTOMFILE", "EXCELFILE", "COLOR"),
            Arrays.<DataClass>asList(IntegerClass.instance, DoubleClass.instance, LongClass.instance, DateClass.instance, LogicalClass.instance,
                    DateTimeClass.instance, StringClass.text, TimeClass.instance, WordClass.get(false, false), ImageClass.get(false, false), PDFClass.get(false, false),
                    DynamicFormatFileClass.get(false, false), ExcelClass.get(false, false), ColorClass.instance)
    );

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

    public ScriptingLogicsModule(InputStream stream, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(stream, "utf-8", baseModule, BL);
    }

    public ScriptingLogicsModule(InputStream stream, String charsetName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(baseModule, BL);
        this.code = IOUtils.readStreamToString(stream, charsetName);
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

    public ScriptingErrorLog getErrLog() {
        return errLog;
    }

    public ScriptParser getParser() {
        return parser;
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

    private DataClass getPredefinedClass(String name) {
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

    public ObjectEntity getObjectEntityByName(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity obj = form.getObject(name);
        if (obj == null) {
            getErrLog().emitObjectNotFoundError(parser, name);
        }
        return obj;
    }

    public MappedProperty getPropertyWithMapping(FormEntity form, String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property = findLPByCompoundName(name);
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
        scriptLogger.info("extendClass(" + className + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        CustomClass cls = (CustomClass) findClassByCompoundName(className);
        boolean isAbstract = cls instanceof AbstractCustomClass;

        List<String> names = instNames;
        List<String> captions = instCaptions;
        if (!isAbstract) {
            ((ConcreteCustomClass) cls).addStaticObjects(instNames, instCaptions);
            names = ((ConcreteCustomClass) cls).getStaticObjectsNames();
            captions = ((ConcreteCustomClass) cls).getStaticObjectsCaptions();
        }

        checkStaticClassConstraints(isAbstract, names, captions);
        checkClassParents(parentNames);

        for (String parentName : parentNames) {
            CustomClass parentClass = (CustomClass) findClassByCompoundName(parentName);
            if (cls.parents.contains(parentClass)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            cls.addParentClass(parentClass);
        }
    }

    public AbstractGroup findGroupByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractGroup group = groupResolver.resolve(name);
        checkGroup(group, name);
        return group;
    }

    public LAP<?> findLAPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LAP<?>) findLPByCompoundName(name);
    }

    public LCP<?> findLCPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LCP<?>) findLPByCompoundName(name);
    }

    public LP<?, ?> findLPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (currentLocalProperties.containsKey(name)) {
            return currentLocalProperties.get(name);
        }

        LP<?, ?> property = lpResolver.resolve(name);
        checkProperty(property, name);
        return property;
    }

    public Set<String> copyCurrentLocalProperties() {
        return new HashSet<String>(currentLocalProperties.keySet());
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

    public Event createScriptedEvent(BaseEvent base, List<String> formIds) throws ScriptingErrorLog.SemanticErrorException {
        return new Event(base, formIds != null ? new SessionEnvEvent(SetFact.fromJavaSet(new HashSet<FormEntity>(findFormsByCompoundName(formIds)))) : SessionEnvEvent.ALWAYS);
    }

    public MetaCodeFragment findMetaCodeFragmentByCompoundName(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        CompoundNameResolver<MetaCodeFragment> resolver = new CompoundNameResolver<MetaCodeFragment>(new MetaCodeNameModuleFinder(paramCnt));
        MetaCodeFragment code = resolver.resolve(name);
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
        return new ScriptingFormEntity(this, new FormEntity(null, formName, caption, title, icon));
    }

    public ScriptingFormView createScriptedFormView(String formName, String caption, boolean applyDefault) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedFormView(" + formName + ", " + applyDefault + ");");

        FormEntity form = findFormByCompoundName(formName);
        FormView formView = applyDefault ? new DefaultFormView(form) : new FormView(form);
        ScriptingFormView scriptingView = new ScriptingFormView(formView, this);
        if (caption != null) {
            formView.caption = caption;
        }

        form.setRichDesign(formView);

        return scriptingView;
    }

    public ScriptingFormView getDesignForExtending(String formName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("getDesignForExtending(" + formName + ");");
        FormEntity form = findFormByCompoundName(formName);
        return new ScriptingFormView(form.getRichDesign(), this);
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedForm(" + form + ");");
        addFormEntity(form.getForm());
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

    public void addImplementationToAbstract(String abstractPropName, List<String> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addImplementationToAbstract(" + abstractPropName + ", " + context + ", " + implement + ", " + when + ");");

        LP abstractLP = findLPByCompoundName(abstractPropName);
        checkParamCount(abstractLP, context.size());

        List<LPWithParams> allProps = new ArrayList<LPWithParams>();
        allProps.add(implement);
        if (when != null) {
            checkCalculationProperty(when.property);
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        if (abstractLP instanceof LCP) {
            addImplementationToAbstractProp(abstractPropName, (LCP) abstractLP, when != null, params);
        } else {
            addImplementationToAbstractAction(abstractPropName, (LAP) abstractLP, when != null, params);
        }
    }

    private void addImplementationToAbstractProp(String propName, LCP abstractProp, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checkAbstractProperty(abstractProp, propName);
        CaseUnionProperty.Type type = ((CaseUnionProperty)abstractProp.property).getAbstractType();
        checkAbstractTypes(type == CaseUnionProperty.Type.CASE, isCase);

        try {
            abstractProp.addOperand(isCase, params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    private void addImplementationToAbstractAction(String actionName, LAP abstractAction, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checkAbstractAction(abstractAction, actionName);
        ListCaseActionProperty.AbstractType type = ((ListCaseActionProperty)abstractAction.property).getAbstractType();
        checkAbstractTypes(type == ListCaseActionProperty.AbstractType.CASE, isCase);

        try {
            abstractAction.addOperand(isCase, params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    public int getParamIndex(String param, List<String> namedParams, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        int index = -1;
        if (namedParams != null) {
            index = namedParams.indexOf(param);
        }
        if (index < 0 && param.startsWith("$")) {
            if (Character.isDigit(param.charAt(1))) {
                index = Integer.parseInt(param.substring(1)) - 1;
                if (index < 0 || !dynamic && namedParams != null && index >= namedParams.size()) {
                    errLog.emitParamIndexError(parser, index + 1, namedParams == null ? 0 : namedParams.size());
                }
            } else if (!insideRecursion) {
                errLog.emitRecursiveParamsOutideRecursionError(parser, param);
            } else if (namedParams != null && namedParams.indexOf(param.substring(1)) < 0 && !dynamic) {
                errLog.emitParamNotFoundError(parser, param.substring(1));
            }
        }
        if (index < 0 && namedParams != null && (dynamic || param.startsWith("$") && insideRecursion)) {
            index = namedParams.size();
            namedParams.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, param);
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

    public void addSettingsToProperty(LP property, String name, String caption, List<String> namedParams, String groupName, boolean isPersistent, boolean isComplex, String tableName, Boolean notNullResolve, Event notNullEvent) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addSettingsToProperty(" + property.property.getSID() + ", " + name + ", " + caption + ", " +
                           namedParams + ", " + groupName + ", " + isPersistent  + ", " + tableName + ");");
        checkDuplicateProperty(name);
        checkDistinctParameters(namedParams);
        checkNamedParams(property, namedParams);

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
            checkPropertyValue(property);
            if (!alwaysNullProperties.isEmpty()) {
                showAlwaysNullErrors();
            }

            checkClassWhere((LCP) property, name);
        }
        addNamedParams(property.property.getSID(), namedParams);
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

    public void addToContextMenuFor(LP onContextAction, String contextMenuCaption, String mainPropertySID) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertySID != null;

        checkActionProperty(onContextAction);

        LP<?, ?> mainProperty = findLPByCompoundName(mainPropertySID);
        LAP onContextLAP = (LAP) onContextAction;
        onContextLAP.addToContextMenuFor(mainProperty, contextMenuCaption);
    }

    public void setAsEditActionFor(LP onEditAction, String editActionSID, String mainPropertySID) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertySID != null;

        checkActionProperty(onEditAction);

        LP<?, ?> mainProperty = findLPByCompoundName(mainPropertySID);
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
        ImList<ActionPropertyMapImplement> actionImplements = readActionImplements(property.listInterfaces, params.toArray());
        property.property.setEditAction(actionType, actionImplements.get(0));
    }

    public void setEventId(LP property, String id) {
        property.property.eventID = id;
    }

    private <T extends LP> void changePropertyName(T lp, String name) {
        removeModuleLP(lp);
        setPropertySID(lp, name, false);
        lp.property.freezeSID();
        addModuleLP(lp);
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
        EmailActionProperty eaProp = (EmailActionProperty) eaPropLP.property;

        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> allImplements = readCalcImplements(eaPropLP.listInterfaces, allParams);

        int i = 0;
        if (fromProp != null) {
            eaProp.setFromAddress(allImplements.get(i++));
        } else {
            // по умолчанию используем стандартный fromAddress
            eaProp.setFromAddress(new CalcPropertyMapImplement((CalcProperty) BL.emailLM.fromAddress.property));
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

    public LPWithParams addScriptedListAProp(boolean newSession, boolean doApply, boolean singleApply, Set<String> upLocalNames, String used, List<LPWithParams> properties, List<String> localPropNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedListAProp(" + newSession + ", " + doApply + ", " + properties + ");");

        ImSet<SessionDataProperty> sessionUsed = used != null ? SetFact.singleton((SessionDataProperty) findLPByCompoundName(used).property) : SetFact.<SessionDataProperty>EMPTY();
        MExclSet<SessionDataProperty> mUpLocal = SetFact.mExclSet(upLocalNames.size()); // exception кидается
        for(String name : upLocalNames) {
            mUpLocal.exclAdd((SessionDataProperty) findLPByCompoundName(name).property);
        }

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);

        LAP<?> listLP = addListAProp(resultParams.toArray());
        for (String propName : localPropNames) {
            currentLocalProperties.remove(propName);
        }

        return !newSession
               ? new LPWithParams(listLP, usedParams)
               : new LPWithParams(addNewSessionAProp(null, genSID(), "", listLP, doApply, singleApply, mUpLocal.immutable(), sessionUsed), usedParams);
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
        checkLocalDataPropertyName(name);

        LCP res = addScriptedDProp(returnClassName, paramClassNames, true, false);
        currentLocalProperties.put(name, res);
        return res;
    }

    public LPWithParams addScriptedJoinAProp(LP mainProp, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedJoinAProp(" + mainProp + ", " + properties + ", " + ");");
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

        ObjectEntity object = form.getObject(objectName);
        if (object != null) {
            List<Object> resultParams = getParamsPlainList(singletonList(seekProp));
            LAP lap = addOSAProp(object, resultParams.toArray());
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

    public LPWithParams addScriptedAssignPropertyAProp(List<String> context, String toPropertyName, List<LPWithParams> toPropertyMapping, LPWithParams fromProperty, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAssignPropertyAProp(" + context + ", " + toPropertyName + ", " + fromProperty + ", " + whereProperty + ");");
        LP toPropertyLP = findLPByCompoundName(toPropertyName);

        if (!(toPropertyLP.property instanceof DataProperty || toPropertyLP.property instanceof CaseUnionProperty)) {
            errLog.emitOnlyDataCasePropertyIsAllowedError(parser, toPropertyName);
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

    public LPWithParams addScriptedAddObjProp(List<String> context, String className, String toPropName, List<LPWithParams> toPropMapping, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAddObjProp(" + className + ", " + toPropName + ", " + toPropMapping + ", " + whereProperty + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        checkAddObjTOParams(context.size(), toPropMapping);

        LPWithParams toProperty = null;
        if (toPropName != null && toPropMapping != null) {
            toProperty = addScriptedJProp(findLPByCompoundName(toPropName), toPropMapping);
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

    public LPWithParams addScriptedDeleteAProp(int oldContextSize, List<String> newContext, LPWithParams param, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = addScriptedChangeClassAProp(oldContextSize, newContext, param, baseClass.unknown, whereProperty);
        setDeleteActionOptions((LAP)res.property);
        return res;
    }

    public LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<String> newContext, LPWithParams param, String className, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClassByCompoundName(className);
        checkChangeClassActionClass(cls);
        return addScriptedChangeClassAProp(oldContextSize, newContext, param, (ConcreteCustomClass) cls, whereProperty);
    }

    private LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<String> newContext, LPWithParams param, ConcreteObjectClass cls, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
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

    public LPWithParams addScriptedForAProp(List<String> oldContext, LPWithParams condition, List<LPWithParams> orders, LPWithParams action, LPWithParams elseAction, Integer addNum, String addClassName, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
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
        if (type == GroupingType.AGGR) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(null, asList(mainProps.get(0).usedParams.get(0))));
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
        } else if (type == GroupingType.AGGR) {
            resultProp = addAGProp(null, false, genSID(), false, "", false, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.EQUAL) {
            resultProp = addCGProp(null, false, genSID(), false, "", null, groupPropParamCount, resultParams.toArray());
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

    public LPWithParams addScriptedPartitionProp(PartitionType partitionType, LP ungroupProp, boolean strict, int precision, boolean isAscending,
                                                 boolean useLast, int groupPropsCnt, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedPartitionProp(" + partitionType + ", " + ungroupProp + ", " + strict + ", " + precision + ", " +
                                                        isAscending + ", " + useLast + ", " + groupPropsCnt + ", " + paramProps + ");");
        checkPartitionWindowConsistence(partitionType, useLast);
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

    public LCP addScriptedSFProp(String typeName, String formulaText) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSFProp(" + typeName + ", " + formulaText + ");");
        Set<Integer> params = findFormulaParameters(formulaText);
        checkFormulaParameters(params);
        if (typeName != null) {
            ValueClass cls = findClassByCompoundName(typeName);
            checkFormulaClass(cls);
            return addSFProp(transformFormulaText(formulaText), (DataClass) cls, params.size());
        } else {
            return addSFProp(transformFormulaText(formulaText), params.size());
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

    public LPWithParams addScriptedRProp(List<String> context, LPWithParams zeroStep, LPWithParams nextStep, Cycle cycleType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedRProp(" + context + ", " + zeroStep + ", " + nextStep + ", " + cycleType + ");");

        List<Integer> usedParams = mergeAllParams(asList(zeroStep, nextStep));
        checkRecursionContext(context, usedParams);

        MOrderExclSet<Integer> mMainParams = SetFact.mOrderExclSetMax(usedParams.size());
        Map<Integer, Integer> usedToResult = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            if (!context.get(usedParams.get(i)).startsWith("$")) {
                mMainParams.exclAdd(i);
                usedToResult.put(usedParams.get(i), i);
            }
        }
        ImOrderSet<Integer> mainParams = mMainParams.immutableOrder();

        Map<Integer, Integer> mapPrev = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            String param = context.get(usedParams.get(i)); // usedParams и context orderSet / revMap'ы
            if (param.startsWith("$")) {
                mapPrev.put(i, usedToResult.get(context.indexOf(param.substring(1))));
            }
        }

        List<Object> resultParams = getParamsPlainList(Arrays.asList(zeroStep, nextStep));
        LP res = addRProp(null, genSID(), false, "", cycleType, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

        List<Integer> resUsedParams = new ArrayList<Integer>();
        for (Integer usedParam : usedParams) {
            if (!context.get(usedParam).startsWith("$")) {
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

    public LPWithParams addScriptedFAProp(String formName, List<String> objectNames, List<LPWithParams> mapping, ModalityType modalityType, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFAProp(" + formName + ", " + objectNames + ", " + mapping + ", " + modalityType + ", " + sessionScope + ");");

        FormEntity form = findFormByCompoundName(formName);

        ObjectEntity[] objects = new ObjectEntity[objectNames.size()];
        for (int i = 0; i < objectNames.size(); i++) {
            objects[i] = findObjectEntity(form, objectNames.get(i));
        }

        LPWithParams res = new LPWithParams(addFAProp(null, genSID(), "", form, objects, null, sessionScope, modalityType, checkOnOk, showDrop), new ArrayList<Integer>());
        if (mapping.size() > 0) {
            res = addScriptedJoinAProp(res.property, mapping);
        }
        return res;
    }

    public ObjectEntity findObjectEntity(FormEntity form, String objectName) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity result = form.getObject(objectName);
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
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos+1);
        LCP resultProp = null;

        ValueClass cls = findClassByCompoundName(className);
        if (cls instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteClass = (ConcreteCustomClass) cls;
            if (concreteClass.hasStaticObject(instanceName)) {
                resultProp = addCProp(concreteClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static оbject", instanceName);
            }
        } else {
            errLog.emitAbstractClassInstancesUseError(parser, className, instanceName);
        }
        return resultProp;
    }

    public LCP addScriptedGroupObjectProp(String name, GroupObjectProp prop) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);
        LCP resultProp = null;

        FormEntity form = findFormByCompoundName(formName);
        if(form == null) {
            errLog.emitNotFoundError(parser, "form", formName);
        }

        GroupObjectEntity groupObject = form.getGroupObject(objectName);
        if (groupObject != null) {
            resultProp = addGroupObjectProp(groupObject, prop);
        } else {
            errLog.emitNotFoundError(parser, "group оbject", objectName);
        }
        return resultProp;
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

    public void addScriptedConstraint(LP property, Event event, boolean checked, List<String> propNames, String message) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConstraint(" + property + ", " + checked + ", " + propNames + ", " + message + ");");
        if (!property.property.check()) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        property.property.caption = message;
        ImSet<CalcProperty<?>> checkedProps = null;
        CalcProperty.CheckType type = (checked ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO);
        if (checked && propNames != null) {
            MSet<CalcProperty<?>> mCheckedProps = SetFact.mSet();
            for (String propName : propNames) {
                mCheckedProps.add((CalcProperty<?>) findLPByCompoundName(propName).property);
            }
            type = CalcProperty.CheckType.CHECK_SOME;
            checkedProps = mCheckedProps.immutable();
        }
        addConstraint((LCP<?>) property, type, checkedProps, event, this);
    }

    public LPWithParams addScriptedSessionProp(IncrementType type, LPWithParams property) {
        scriptLogger.info("addScriptedSessionProp(" + type + ", " + property + ");");
        LCP newProp;
        if (type == null) {
            newProp = addOldProp((LCP) property.property);
        } else {
            newProp = addCHProp((LCP) property.property, type);
        }
        return new LPWithParams(newProp, property.usedParams);
    }

    public LPWithParams addScriptedSignatureProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSignatureProp(" + property + ");");
        checkCalculationProperty(property.property);
        LCP newProp = addClassProp((LCP) property.property);
        return new LPWithParams(newProp, property.usedParams);
    }

    public void addScriptedFollows(String mainPropName, List<String> namedParams, List<Integer> options, List<LPWithParams> props, List<Event> sessions) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFollows(" + mainPropName + ", " + namedParams + ", " + options + ", " + props + ", " + sessions + ");");
        LCP mainProp = (LCP) findLPByCompoundName(mainPropName);
        checkProperty(mainProp, mainPropName);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);

        for (int i = 0; i < props.size(); i++) {
            Integer[] params = new Integer[props.get(i).usedParams.size()];
            for (int j = 0; j < params.length; j++) {
                params[j] = props.get(i).usedParams.get(j) + 1;
            }
            follows(mainProp, options.get(i), sessions.get(i), (LCP) props.get(i).property, params);
        }
    }

    public void addScriptedWriteWhen(String mainPropName, List<String> namedParams, LPWithParams valueProp, LPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedWriteWhen(" + mainPropName + ", " + namedParams + ", " + valueProp + ", " + whenProp + ");");
        LP mainProp = findLPByCompoundName(mainPropName);
        if (!(mainProp.property instanceof DataProperty)) {
            errLog.emitOnlyDataPropertyIsAllowedError(parser, mainPropName);
        }
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);
        checkCalculationProperty(mainProp);

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        ((LCP)mainProp).setEventChange(this, action, params.toArray());
    }

    public Set<LCP> findLCPsByCompoundName(List<String> ids) throws ScriptingErrorLog.SemanticErrorException {
        if(ids==null)
            return null;

        Set<LCP> prevStart = new HashSet<LCP>(); // функционально из-за exception'а не сделаешь
        for(String id : ids)
            prevStart.add(findLCPByCompoundName(id));
        return prevStart;
    }

    public final static GetValue<CalcProperty, LCP> getProp = new GetValue<CalcProperty, LCP>() {
        public CalcProperty getMapValue(LCP value) {
            return ((LCP<?>)value).property;
        }};

    public void addScriptedEvent(LPWithParams whenProp, LPWithParams event, List<LPWithParams> orders, boolean descending, Event baseEvent, Set<LCP> prevStart, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEvent(" + whenProp + ", " + event + ", " + orders + ", " + descending + ", " + baseEvent + ");");
        checkActionProperty(event.property);
        if(noInline==null) {
            noInline = new ArrayList<LPWithParams>();
            for(Integer usedParam : whenProp.usedParams)
                noInline.add(new LPWithParams(null, asList(usedParam)));
        }
        List<Object> params = getParamsPlainList(asList(event, whenProp), orders, noInline);
        addEventAction(baseEvent, descending, false, prevStart == null ? null : SetFact.fromJavaSet(prevStart).mapSetValues(getProp), noInline.size(), forceInline, params.toArray());
    }

    public void addScriptedGlobalEvent(LPWithParams event, Event baseEvent, boolean single, String showDep, Set<LCP> prevStart) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGlobalEvent(" + event + ", " + baseEvent + ");");
        checkActionProperty(event.property);
        checkEventNoParameters(event.property);
        ActionProperty action = (ActionProperty) event.property.property;
        if(showDep!=null)
            action.showDep = findLPByCompoundName(showDep).property;
        addBaseEvent(action, baseEvent, false, single, SetFact.fromJavaSet(prevStart).mapSetValues(getProp));
    }

    public void addScriptedShowDep(String property, String propFrom) throws ScriptingErrorLog.SemanticErrorException {
        findLPByCompoundName(property).property.showDep = findLPByCompoundName(propFrom).property;
    }

    public void addScriptedAspect(String mainPropName, List<String> mainPropParams, LPWithParams actionProp, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAspect(" + mainPropName + ", " + mainPropParams + ", " + actionProp + ", " + before + ");");
        LP mainProp = findLPByCompoundName(mainPropName);
        checkParamCount(mainProp, mainPropParams.size());
        checkDistinctParameters(mainPropParams); // todo [dale]: надо, наверное, это вынести в отдельный метод
        checkActionProperty(actionProp.property);
        checkActionProperty(mainProp);

        List<Object> params = getParamsPlainList(Arrays.asList(actionProp));
        ImList<ActionPropertyMapImplement> actionImplements = readActionImplements(mainProp.listInterfaces, params.toArray());
        addAspectEvent((ActionProperty) mainProp.property, actionImplements.get(0), before);
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

    public void addScriptedIndex(List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIndex(" + propNames + ");");
        LCP[] lps = new LCP[propNames.size()];
        for (int i = 0; i < propNames.size(); i++) {
            lps[i] = (LCP) findLPByCompoundName(propNames.get(i));
        }
        addIndex(lps);
    }

    public void addScriptedLoggable(List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedLoggable(" + propNames + ");");

        for (String name : propNames) {
            LCP lp = (LCP) findLPByCompoundName(name);
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

        HAlign hAlign = options.getHAlign();
        VAlign vAlign = options.getVAlign();
        HAlign thAlign = options.getTextHAlign();
        VAlign tvAlign = options.getTextVAlign();
        if (hAlign != null) {
            window.alignmentX = hAlign.asToolbarAlign();
        }
        if (vAlign != null) {
            window.alignmentY = vAlign.asToolbarAlign();
        }
        if (thAlign != null) {
            window.horizontalTextPosition = thAlign.asTextPosition();
        }
        if (tvAlign != null) {
            window.verticalTextPosition = tvAlign.asTextPosition();
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

    public NavigatorElement createScriptedNavigatorElement(String name, String caption, NavigatorElement<?> parentElement, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName, String actionName, String icon) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedNavigatorElement(" + name + ", " + caption + ");");

        assert name != null && caption != null && parentElement != null;

        checkDuplicateNavigatorElement(name);

        NavigatorElement newElement;

        if (actionName != null) {
            LAP<?> actionProperty = (LAP<?>) findLPByCompoundName(actionName);
            checkActionProperty(actionProperty);

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
        if (anchorElement != null && !parentElement.equals(anchorElement.getParent())) {
            errLog.emitIllegalInsertBeforeAfterComponentElement(parser, element.getSID(), parentElement.getSID(), anchorElement.getSID());
        }

        if (element.isAncestorOf(parentElement)) {
            errLog.emitIllegalMoveNavigatorToSubnavigator(parser, element.getSID(), parentElement.getSID());
        }

        switch (pos) {
            case IN:
                parentElement.add(element);
                break;
            case BEFORE:
                parentElement.addBefore(element, anchorElement);
                break;
            case AFTER:
                parentElement.addAfter(element, anchorElement);
                break;
            case FIRST:
                parentElement.addFirst(element);
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
        if (property.property instanceof CalcProperty && !property.property.check() && !alwaysNullProperties.containsKey(property.property)) {
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

    private void checkDuplicateProperty(String propName) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingLP(getNamespace(), propName);
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
        if (type != GroupingType.CONCAT && orderParamsCnt > 0) {
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
        if (type == GroupingType.AGGR) {
            if (mainProps.get(0).property != null) {
                errLog.emitNonObjectAggrGPropError(parser);
            }
        }
    }

    private void checkGPropWhereConsistence(GroupingType type, LPWithParams where) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.AGGR && where != null) {
            errLog.emitWhereGPropError(parser, type);
        }
    }

    public void checkActionAllParamsUsed(List<String> context, LP property, boolean ownContext) throws ScriptingErrorLog.SemanticErrorException {
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

    public void checkActionLocalContext(List<String> oldContext, List<String> newContext) throws ScriptingErrorLog.SemanticErrorException {
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

    private void checkLocalDataPropertyName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (currentLocalProperties.containsKey(name)) {
            errLog.emitAlreadyDefinedError(parser, "local property", name);
        } else {
            checkDuplicateProperty(name);
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

    public void checkChangeClassWhere(boolean contextExtended, LPWithParams param, LPWithParams where, List<String> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (contextExtended && (where == null || !where.usedParams.contains(param.usedParams.get(0)))) {
            errLog.emitChangeClassWhereError(parser, newContext.get(newContext.size() - 1));
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
        if (!parser.isInsideMetacode()) {
            showWarnings();
        }
    }

    public void initScriptingModule(String name, String namespace, List<String> requiredModules, List<String> namespacePriority) {
        setModuleName(name);
        setNamespace(namespace == null ? name : namespace);
        setRequiredModules(requiredModules);
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

    public class CompoundNameResolver<T> {
        private ModuleFinder<T> finder;

        public CompoundNameResolver(ModuleFinder<T> finder) {
            this.finder = finder;
        }

        public T findInNamespace(String namespaceName, String name) {
            T result = null;
            for (LogicsModule module : namespaceToModules.get(namespaceName)) {
                if ((result = finder.resolveInModule(module, name)) != null) {
                    return result;
                }
            }
            return result;
        }

        private List<LogicsModule> findInRequiredModules(String name, List<String> namespaces) {
            List<LogicsModule> outModules = new ArrayList<LogicsModule>();

            for (String namespaceName : namespaces) {
                for (LogicsModule module : namespaceToModules.get(namespaceName)) {
                    if (finder.resolveInModule(module, name) != null) {
                        outModules.add(module);
                        return outModules;
                    }
                }
            }

            Set<String> checkedNamespaces = new HashSet<String>(namespaces);
            for (Map.Entry<String, List<LogicsModule>> e : namespaceToModules.entrySet()) {
                if (!checkedNamespaces.contains(e.getKey())) {
                    for (LogicsModule module : e.getValue()) {
                        if (finder.resolveInModule(module, name) != null) {
                            outModules.add(module);
                            break;
                        }
                    }
                }
            }
            return outModules;
        }

        public final T resolve(String name) throws ScriptingErrorLog.SemanticErrorException {
            T result = null;
            int dotPosition = name.indexOf('.');
            if (dotPosition > 0) {
                String namespaceName = name.substring(0, dotPosition);
                checkNamespace(namespaceName);
                result = findInNamespace(namespaceName, name.substring(dotPosition + 1));
            } else {
                result = finder.resolveInModule(ScriptingLogicsModule.this, name);
                if (result == null) {
                    List<String> namespaces = new ArrayList<String>();
                    namespaces.add(getNamespace());
                    namespaces.addAll(namespacePriority);
                    List<LogicsModule> containingModules = findInRequiredModules(name, namespaces);
                    if (containingModules.size() > 1) {
                        errLog.emitAmbiguousNameError(parser, containingModules, name);
                    } else if (containingModules.size() == 1) {
                        result = finder.resolveInModule(containingModules.get(0), name);
                    }
                }
            }
            return result;
        }
    }
}
