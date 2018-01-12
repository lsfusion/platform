package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.*;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.DefaultIcon;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.navigator.NavigatorForm;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.window.*;
import lsfusion.server.logics.*;
import lsfusion.server.logics.debug.*;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.Event;
import lsfusion.server.logics.property.actions.*;
import lsfusion.server.logics.property.actions.external.ExternalActionProperty;
import lsfusion.server.logics.property.actions.external.ExternalDBActionProperty;
import lsfusion.server.logics.property.actions.external.ExternalDBFActionProperty;
import lsfusion.server.logics.property.actions.external.ExternalHTTPActionProperty;
import lsfusion.server.logics.property.actions.file.FileActionType;
import lsfusion.server.logics.property.actions.flow.BreakActionProperty;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.actions.flow.ReturnActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.csv.ImportCSVDataActionProperty;
import lsfusion.server.logics.property.actions.importing.csv.ImportFormCSVDataActionProperty;
import lsfusion.server.logics.property.actions.importing.dbf.ImportFormDBFDataActionProperty;
import lsfusion.server.logics.property.actions.importing.json.ImportFormJSONDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xml.ImportFormXMLDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xml.ImportXMLDataActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.resolving.ResolvingErrors;
import lsfusion.server.logics.resolving.ResolvingErrors.ResolvingError;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.mail.AttachmentFormat;
import lsfusion.server.mail.SendEmailActionProperty;
import lsfusion.server.mail.SendEmailActionProperty.FormStorageType;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.LocalNestedType;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.janino.SimpleCompiler;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.logics.PropertyUtils.*;
import static lsfusion.server.logics.scripted.AlignmentUtils.*;

public class ScriptingLogicsModule extends LogicsModule {

    private static final Logger scriptLogger = ServerLoggers.scriptLogger;
    
    protected final BusinessLogics<?> BL;

    private String code = null;
    private String filename = null;
    private String path = null;
    private final ScriptingErrorLog errLog;
    private ScriptParser parser;
    private ScriptingLogicsModuleChecks checks;
    private List<String> warningList = new ArrayList<>();
    private Map<Property, String> alwaysNullProperties = new HashMap<>();

    private String lastOptimizedJPropSID = null;

    public enum ConstType { STATIC, INT, REAL, NUMERIC, STRING, LOGICAL, LONG, DATE, DATETIME, TIME, COLOR, NULL }
    public enum InsertPosition {IN, BEFORE, AFTER, FIRST}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, AGGR, EQUAL, LAST, NAGGR}

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

    private ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        setBaseLogicsModule(baseModule);
        this.BL = BL;
        errLog = new ScriptingErrorLog("");
        parser = new ScriptParser(errLog);
        checks = new ScriptingLogicsModuleChecks(this);
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
        setDefaultNamespace(namespace == null);
        if (requiredModules.isEmpty() && !getName().equals("System")) {
            requiredModules.add("System");
        }
        setRequiredModules(new HashSet<>(requiredModules));
        setNamespacePriority(namespacePriority);
    }

    public void initAliases() {
        initBaseGroupAliases();
        initBaseClassAliases();
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
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
    
    public ScriptingLogicsModuleChecks getChecks() {
        return checks;
    }
    
    public String transformStringLiteral(String s) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return ScriptedStringUtils.transformStringLiteral(s);
        } catch (ScriptedStringUtils.TransformationError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
        return null;
    }

    public LocalizedString transformLocalizedStringLiteral(String s) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return ScriptedStringUtils.transformLocalizedStringLiteral(s);
        } catch (ScriptedStringUtils.TransformationError | LocalizedString.FormatError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
        return null;
    }
    
    public ObjectEntity[] getMappingObjectsArray(FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity[] objects = new ObjectEntity[mapping.size()];
        for (int i = 0; i < mapping.size(); i++) {
            objects[i] = getNFObjectEntityByName(form, mapping.get(i));
        }
        return objects;
    }

    public List<ResolveClassSet> getMappingClassesArray(FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        List<ResolveClassSet> classes = new ArrayList<>();
        for (String paramName : mapping) {
            ObjectEntity obj = getNFObjectEntityByName(form, paramName);
            classes.add(obj.getResolveClassSet());
        }
        return classes;
    }

    public ObjectEntity getObjectEntityByName(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity obj = form.getObject(name);
        if (obj == null) {
            getErrLog().emitObjectNotFoundError(parser, name);
        }
        return obj;
    }

    public ObjectEntity getNFObjectEntityByName(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity obj = form.getNFObject(name, getVersion());
        if (obj == null) {
            getErrLog().emitObjectNotFoundError(parser, name);
        }
        return obj;
    }

    public MappedProperty getPropertyWithMapping(FormEntity form, AbstractPropertyUsage pDrawUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property;
        if(pDrawUsage instanceof PropertyUsage) {
            PropertyUsage pUsage = (PropertyUsage) pDrawUsage;
            if (pUsage.classNames != null) {
                property = findLPByPropertyUsage(pUsage);
            } else {
                List<ResolveClassSet> classes = getMappingClassesArray(form, mapping);
                property = findLPByNameAndClasses(pUsage.name, pUsage.getSourceName(), classes);
            }
        } else {
            property = ((LPUsage)pDrawUsage).lp;
        }
        
        if (property.property.interfaces.size() != mapping.size()) {
            getErrLog().emitParamCountError(parser, property, mapping.size());
        }
        return new MappedProperty(property, getMappingObjectsArray(form, mapping));
    }

    private void convertResolveError(ResolvingError e) throws ScriptingErrorLog.SemanticErrorException {
        try {
            throw e;
        } catch (ResolvingErrors.ResolvingAmbiguousError re) {
            errLog.emitAmbiguousNameError(parser, re.modules, re.name);
        } catch (ResolvingErrors.ResolvingAmbiguousPropertyError re) {
            errLog.emitAmbiguousPropertyNameError(parser, re.foundItems, re.name);            
        } catch (ResolvingErrors.ResolvingNamespaceError re) {
            errLog.emitNamespaceNotFoundError(parser, re.namespaceName);
        } catch (ResolvingError re) {
            assert false;
        }
    }

    public ValueClass findClass(String name) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = ClassCanonicalNameUtils.getScriptedDataClass(name);
        if (valueClass == null) {
            try {
                valueClass = resolveClass(name);
            } catch (ResolvingError e) {
                convertResolveError(e);
            }
        }
        checks.checkClass(valueClass, name);
        return valueClass;
    }

    public void addScriptedClass(String className, LocalizedString captionStr, boolean isAbstract,
                                 List<String> instNames, List<LocalizedString> instCaptions, List<String> parentNames, boolean isComplex,
                                 DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateClass(className, BL);
        checks.checkStaticClassConstraints(isAbstract, instNames, instCaptions);
        checks.checkClassParents(parentNames);

        LocalizedString caption = (captionStr == null ? LocalizedString.create(className) : captionStr);

        CustomClass[] parents;
        if (parentNames.isEmpty()) {
            parents = new CustomClass[] {baseLM.baseClass};
        } else {
            parents = new CustomClass[parentNames.size()];
            for (int i = 0; i < parentNames.size(); i++) {
                String parentName = parentNames.get(i);
                parents[i] = (CustomClass) findClass(parentName);
            }
        }

        List<LocalizedString> captions = new ArrayList<>();
        for (int i = 0; i < instCaptions.size(); i++) {
            captions.add(instCaptions.get(i) == null ? LocalizedString.create(instNames.get(i)) : instCaptions.get(i));
        }

        CustomClass cls;
        if (isAbstract) {
            cls = addAbstractClass(className, caption, parents);
        } else {
            cls = addConcreteClass(className, caption, instNames, captions, parents);
        }
        cls.isComplex = isComplex;

        ClassDebugInfo debugInfo = new ClassDebugInfo(point);
        if (debugger.isEnabled() && point.needToCreateDelegate()) {
            debugger.addDelegate(debugInfo);
            cls.setDebugInfo(debugInfo);
        }
    }

    public void extendClass(String className, List<String> instNames, List<LocalizedString> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        CustomClass cls = (CustomClass) findClass(className);
        boolean isAbstract = cls instanceof AbstractCustomClass;

        List<String> names = instNames;
        List<LocalizedString> captions = instCaptions;
        if (!isAbstract) {
            ((ConcreteCustomClass) cls).addStaticObjects(instNames, instCaptions, version);
            names = ((ConcreteCustomClass) cls).getNFStaticObjectsNames(version);
            captions = ((ConcreteCustomClass) cls).getNFStaticObjectsCaptions(version);
        }

        checks.checkStaticClassConstraints(isAbstract, names, captions);
        checks.checkClassParents(parentNames);

        for (String parentName : parentNames) {
            CustomClass parentClass = (CustomClass) findClass(parentName);
            if (cls.containsNFParents(parentClass, version)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            cls.addParentClass(parentClass, version);
        }
    }

    public AbstractGroup findGroup(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            AbstractGroup group = resolveGroup(name);
            checks.checkGroup(group, name);
            return group;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public LAP<?> findAction(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LAP<?>) findLP(name);
    }

    public LCP<?> findProperty(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LCP<?>) findLP(name);
    }

    public LCP<?>[] findProperties(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LCP<?>[] result = new LCP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    private LP<?, ?> findLP(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyUsageParser parser = new PropertyUsageParser(this, name);
        LP<?, ?> property = null;
        try {
            property = findLPByNameAndClasses(parser.getCompoundName(), name, parser.getSignature());
        } catch (AbstractPropertyNameParser.ParseException e) {
            Throwables.propagate(e);
        }
        return property;
    }

    public LP<?, ?> findLPByNameAndClasses(String name, String sourceName, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        List<ResolveClassSet> classSets = new ArrayList<>();
        for (ValueClass cls : classes) {
            classSets.add(cls.getResolveSet());
        }
        return findLPByNameAndClasses(name, sourceName, classSets);
    }

    private LP<?, ?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(name, sourceName, params, false, false);
    }
    
    private LP<?, ?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property = null;
        
        try {
            if (onlyAbstract) {
                property = resolveAbstractProperty(name, params, prioritizeNotEqual);
            } else {
                property = resolveProperty(name, params);
            }
        } catch (ResolvingErrors.ResolvingAmbiguousPropertyError e) {
            if (sourceName != null) {
                e.name = sourceName;
            }
            convertResolveError(e);
        } catch (ResolvingError e) {
            convertResolveError(e);
        } 
        
        checks.checkProperty(property, sourceName == null ? name : sourceName, params);
        return property;
    }

    public LCP<?> findLCPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> lp = findLPByPropertyUsage(pUsage, false, false);
        checks.checkCalculationProperty(lp);
        return (LCP<?>) lp; 
    }

    public LAP<?> findLAPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> lp = findLPByPropertyUsage(pUsage, false, false);
        checks.checkActionProperty(lp);
        return (LAP<?>) lp;
    }

    public LP<?, ?> findLPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(pUsage, false, false);
    }

    public LP<?, ?> findLPByPropertyUsage(PropertyUsage pUsage, boolean isAbstract, boolean prioritizeNotEquals) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(pUsage.name, pUsage.getSourceName(), getParamClasses(pUsage), isAbstract, prioritizeNotEquals);
    }
    
    public AbstractWindow findWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            AbstractWindow window = resolveWindow(name);
            checks.checkWindow(window, name);
            return window;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public FormEntity findForm(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            FormEntity form = resolveForm(name);
            checks.checkForm(form, name);
            return form;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    private List<FormEntity> findForms(List<String> names) throws ScriptingErrorLog.SemanticErrorException {
        List<FormEntity> forms = new ArrayList<>();
        for (String name : names) {
            forms.add(findForm(name));
        }
        return forms;
    }

    public Event createScriptedEvent(BaseEvent base, List<String> formIds, List<PropertyUsage> afterIds) throws ScriptingErrorLog.SemanticErrorException {
        return new Event(base, formIds != null ? new SessionEnvEvent(SetFact.fromJavaSet(new HashSet<>(findForms(formIds)))) : SessionEnvEvent.ALWAYS, afterIds == null? null : SetFact.fromJavaSet(findPropsByPropertyUsages(afterIds)));
    }

    public MetaCodeFragment findMetaCodeFragment(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        try {
            MetaCodeFragment code = resolveMetaCodeFragment(name, paramCnt);
            checks.checkMetaCodeFragment(code, name);
            return code;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public NavigatorElement findNavigatorElement(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            NavigatorElement element = resolveNavigatorElement(name);
            checks.checkNavigatorElement(element, name);
            return element;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public ImplementTable findTable(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ImplementTable table = resolveTable(name);
            checks.checkTable(table, name);
            return table;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public void addScriptedGroup(String groupName, LocalizedString captionStr, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateGroup(groupName, BL);
        LocalizedString caption = (captionStr == null ? LocalizedString.create(groupName) : captionStr);
        AbstractGroup parentGroup = (parentName == null ? null : findGroup(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, LocalizedString caption, DebugInfo.DebugPoint point, String icon,
                                                  ModalityType modalityType, int autoRefresh) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateForm(formName, BL);
        caption = (caption == null ? LocalizedString.create(formName) : caption);

        String canonicalName = elementCanonicalName(formName);

        ScriptingFormEntity form = new ScriptingFormEntity(this, new FormEntity(canonicalName, point.toString(), caption, icon, getVersion()));
        form.setModalityType(modalityType);
        form.setAutoRefresh(autoRefresh);

        return form;
    }

    public ScriptingFormView getFormDesign(String formName, LocalizedString caption, boolean custom) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        FormEntity form = findForm(formName);
        FormView view;
        if (custom) {
            view = new FormView(form, version);
            form.setRichDesign(view, version);
        } else {
            view = form.getNFRichDesign(version);
        }
        
        if (view != null && caption != null) {
            view.setCaption(caption);
        }
        
        return new ScriptingFormView(view, this);
    }
    
    public void addScriptedForm(ScriptingFormEntity form, DebugInfo.DebugPoint point) {
        FormEntity formEntity = addFormEntity(form.getForm());
        formEntity.setCreationPath(point.toString());
    }

    public void finalizeScriptedForm(ScriptingFormEntity form) {
        form.getForm().finalizeInit(getVersion());
    }

    public ScriptingFormEntity getFormForExtending(String name) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(name);
        return new ScriptingFormEntity(this, form);
    }

    public LCP addScriptedDProp(String returnClass, List<String> paramClasses, boolean sessionProp, boolean innerProp, boolean isLocalScope, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass value = findClass(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClass(paramClasses.get(i));
        }

        if (sessionProp) {
            return addSDProp(LocalizedString.NONAME, isLocalScope, value, nestedType, params);
        } else {
            assert nestedType == null;
            if (innerProp) {
                return addDProp(LocalizedString.NONAME, value, params);
            } else {
                StoredDataProperty storedProperty = new StoredDataProperty(LocalizedString.NONAME, params, value);
                return addProperty(null, new LCP<>(storedProperty));
            }
        }
    }

    public LP<?, ?> addScriptedAbstractProp(CaseUnionProperty.Type type, String returnClass, List<String> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass value = findClass(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClass(paramClasses.get(i));
        }
        return addAUProp(null, false, isExclusive, isChecked, isLast, type, LocalizedString.NONAME, value, params);
    }

    public LP addScriptedAbstractActionProp(ListCaseActionProperty.AbstractType type, List<String> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClass(paramClasses.get(i));
        }
        LAP<?> result;
        if (type == ListCaseActionProperty.AbstractType.LIST) {
            result = addAbstractListAProp(isChecked, isLast, params);
        } else {
            result = addAbstractCaseAProp(type, isExclusive, isChecked, isLast, params);
        }
        return result;
    }

    public void addImplementationToAbstract(PropertyUsage abstractPropUsage, List<TypedParameter> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        LP abstractLP = findJoinMainProp(abstractPropUsage, context, true);
        checks.checkParamCount(abstractLP, context.size());
        checks.checkImplementIsNotMain(abstractLP, implement.property);
        
        List<LPWithParams> allProps = new ArrayList<>();
        allProps.add(implement);
        if (when != null) {
            checks.checkCalculationProperty(when.property);
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        List<ResolveClassSet> signature = getClassesFromTypedParams(context);
        if (abstractLP instanceof LCP) {
            checks.checkCalculationProperty(implement.property);
            addImplementationToAbstractProp(abstractPropUsage.name, (LCP) abstractLP, signature, when != null, params);
        } else {
            checks.checkActionProperty(implement.property);
            addImplementationToAbstractAction(abstractPropUsage.name, (LAP) abstractLP, signature, when != null, params);
        }
    }

    private void addImplementationToAbstractProp(String propName, LCP abstractProp, List<ResolveClassSet> signature, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAbstractProperty(abstractProp, propName);
        CaseUnionProperty.Type type = ((CaseUnionProperty)abstractProp.property).getAbstractType();
        checks.checkAbstractTypes(type == CaseUnionProperty.Type.CASE, isCase);

        try {
            abstractProp.addOperand(isCase, signature, getVersion(), params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    private void addImplementationToAbstractAction(String actionName, LAP abstractAction, List<ResolveClassSet> signature, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAbstractAction(abstractAction, actionName);
        ListCaseActionProperty.AbstractType type = ((ListCaseActionProperty)abstractAction.property).getAbstractType();
        checks.checkAbstractTypes(type == ListCaseActionProperty.AbstractType.CASE, isCase);

        try {
            abstractAction.addOperand(isCase, signature, getVersion(), params.toArray());
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
    
    public List<ResolveClassSet> createClassSetsFromClassNames(List<String> classNames) throws ScriptingErrorLog.SemanticErrorException {
        List<ResolveClassSet> params = new ArrayList<>();
        for (String className : classNames) {
            ValueClass cls = findClass(className);
            params.add(cls.getResolveSet());
        }
        return params;
    }
    
    public int getParamIndex(TypedParameter param, List<TypedParameter> context, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        String paramName = param.paramName;
        int index = indexOf(context, paramName);
        
        if (index < 0 && paramName.startsWith("$")) {
            if (param.cls != null) {
                errLog.emitParamClassNonDeclarationError(parser, paramName);
            } else if (!insideRecursion) {
                errLog.emitRecursiveParamsOutideRecursionError(parser, paramName);
            } else if (indexOf(context, paramName.substring(1)) < 0) {
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
            if (paramName.startsWith("$") && insideRecursion) {
                param.cls = context.get(indexOf(context, paramName.substring(1))).cls;            
            }
            index = context.size();
            context.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, paramName);
        }
        return index;
    }

    public static class LPWithParams {
        public LP property; // nullable
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
        List<String> paramNames = new ArrayList<>();
        for (TypedParameter param : params) {
            paramNames.add(param.paramName);
        }
        return paramNames;        
    }

    public List<ResolveClassSet> getClassesFromTypedParams(List<TypedParameter> params) {
        List<ResolveClassSet> paramClasses = new ArrayList<>();
        for (TypedParameter param : params) {
            if (param.cls == null) {
                paramClasses.add(null);
            } else {
                paramClasses.add(param.cls.getResolveSet());
            }
        }
        return paramClasses;
    }

    public LPUsage checkPropertyIsNew(LPUsage property) {
        if(property.lp.property.getSID().equals(lastOptimizedJPropSID))
            property = new LPUsage(addJProp(false, LocalizedString.NONAME, (LCP) property.lp, BaseUtils.consecutiveList(property.lp.property.interfaces.size(), 1).toArray()), property.signature);
        return property;
    }
    
    public void makePropertyPublic(FormEntity form, String alias, LPUsage lpUsage) {
        String name = "_FORM_" + form.getCanonicalName().replace('.', '_') + "_" + alias;
        makePropertyPublic(lpUsage.lp, name, lpUsage.signature);
    }
    
    public LP addSettingsToProperty(LP baseProperty, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature, 
                                      String groupName, boolean isPersistent, boolean isComplex, boolean noHint, String tableName, BooleanDebug notNull, 
                                      BooleanDebug notNullResolve, Event notNullEvent, String annotation, boolean isLoggable) throws ScriptingErrorLog.SemanticErrorException {
        LP property = baseProperty;
        checks.checkDuplicateProperty(name, signature, BL);

        property.property.annotation = annotation;

        List<String> paramNames = getParamNamesFromTypedParams(params);
        checks.checkDistinctParameters(paramNames);
        checks.checkNamedParams(property, paramNames);
        
        // Если объявление имеет вид f(x, y) = g(x, y), то нужно дополнительно обернуть свойство g в join
        if (property.property.getSID().equals(lastOptimizedJPropSID)) {
            property = addJProp(false, LocalizedString.NONAME, (LCP) property, BaseUtils.consecutiveList(property.property.interfaces.size(), 1).toArray());
        }
        
        makePropertyPublic(property, name, signature);
        
        AbstractGroup group = (groupName == null ? null : findGroup(groupName));
        property.property.caption = (caption == null ? LocalizedString.create(name) : caption);
        addPropertyToGroup(property.property, group);

        ImplementTable targetTable = null;
        if (tableName != null) {
            targetTable = findTable(tableName);
            if (!targetTable.equalClasses(((LCP<?>)property).property.getOrderTableInterfaceClasses(ClassType.storedPolicy))) {
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

        if(noHint)
            ((LCP<?>)property).property.noHint = true;

        if (notNull != null) {
            setNotNull((LCP)property, notNull.debugPoint, notNullEvent, 
                    notNullResolve != null ? ListFact.singleton(new PropertyFollowsDebug(false, true, notNullResolve.debugPoint)) :
                                             ListFact.<PropertyFollowsDebug>EMPTY());
            
            if(notNullResolve != null)
                ((LCP<?>)property).property.setAggr(true);
        }

        if (property.property instanceof CalcProperty) {
            
            if (Settings.get().isCheckAlwaysNull()) {
                checks.checkPropertyValue(property, alwaysNullProperties);
                if (!alwaysNullProperties.isEmpty()) {
                    showAlwaysNullErrors();
                }
            }

//            if (Settings.get().isCheckClassWhere()) {
//                checks.checkClassWhere((LCP) property, name);
//            }
        }
        makeLoggable(baseProperty, isLoggable);
        return property;
    }

    private void showAlwaysNullErrors() throws ScriptingErrorLog.SemanticErrorException {
        StringBuilder errorMessage = new StringBuilder();
        for (Property property : alwaysNullProperties.keySet()) {
            if (errorMessage.length() > 0) {
                errorMessage.append("\n");
            }
            String location = alwaysNullProperties.get(property);
            errorMessage.append("[error]:\t" + location + " property '" + property.getName() + "' is always NULL");
        }
        alwaysNullProperties.clear();
        ScriptingErrorLog.emitSemanticError(errorMessage.toString(), new ScriptingErrorLog.SemanticErrorException(parser.getCurrentParser().input));
    }

    public void addToContextMenuFor(LP onContextAction, LocalizedString contextMenuCaption, PropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        checks.checkActionProperty(onContextAction);

        LP<?, ?> mainProperty = findLPByPropertyUsage(mainPropertyUsage);
        LAP onContextLAP = (LAP) onContextAction;
        onContextLAP.addToContextMenuFor(mainProperty, contextMenuCaption);
        ((ActionProperty) onContextLAP.property).checkReadOnly = false;

        onContextLAP.setAsEditActionFor(onContextLAP.property.getSID(), mainProperty);
    }

    public void setAsEditActionFor(LP onEditAction, String editActionSID, PropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        checks.checkActionProperty(onEditAction);

        LP<?, ?> mainProperty = findLPByPropertyUsage(mainPropertyUsage);
        LAP onEditLAP = (LAP) onEditAction;
        onEditLAP.setAsEditActionFor(editActionSID, mainProperty);
    }

    public void setForceViewType(LP property, ClassViewType viewType) {
        property.setForceViewType(viewType);
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

    public void setDefaultCompare(LP property, String defaultCompare) {
        property.setDefaultCompare(defaultCompare);
    }

    public void setChangeKey(LP property, String code, Boolean showEditKey) {
        property.setChangeKey(KeyStroke.getKeyStroke(code));
        if (showEditKey != null)
            property.setShowChangeKey(showEditKey);
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

    public void makeLoggable(LP property, boolean isLoggable) throws ScriptingErrorLog.SemanticErrorException {
        if (isLoggable && property != null) {
            checks.checkCalculationProperty(property);
            ((LCP) property).makeLoggable(this, BL.systemEventsLM);
        }
    }

    public void setEchoSymbols(LP property) {
        property.setEchoSymbols(true);
    }

    public void setAggProp(LP property) {
        ((CalcProperty)property.property).aggProp = true;
    }

    public void setAggr(LP property) {
        ((CalcProperty)property.property).setAggr(true);
    }

    public void setScriptedEditAction(LP property, String actionType, LPWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        property.property.setEditAction(actionType, actionImplements.get(0));
    }

    public void setScriptedContextMenuAction(LP property, LocalizedString contextMenuCaption, LPWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = actionImplements.get(0);

        String actionSID = actionImplement.property.getSID();
        property.property.setContextMenuAction(actionSID, FormPropertyOptions.getContextMenuCaption(contextMenuCaption, actionImplement.property));
        actionImplement.property.checkReadOnly = false;

        property.property.setEditAction(actionSID, actionImplement);
    }

    public void setEventId(LP property, String id) {
        property.property.drawOptions.setEventID(id);
    }
    
    public List<ResolveClassSet> getParamClasses(PropertyUsage usage) throws ScriptingErrorLog.SemanticErrorException {
        List<ValueClass> valueClasses = getValueClasses(usage);
        if (valueClasses == null) {
            return null;
        }
        
        List<ResolveClassSet> classes = new ArrayList<>();
        for (ValueClass valueClass : valueClasses) {
            if (valueClass == null) {
                classes.add(null);
            } else {
                classes.add(valueClass.getResolveSet());
            }
        }
        return classes;
    }

    public List<ValueClass> getValueClasses(PropertyUsage usage) throws ScriptingErrorLog.SemanticErrorException {
        if (usage.classNames == null) {
            return null;
        }

        List<ValueClass> classes = new ArrayList<>();
        for (String className : usage.classNames) {
            if (className.equals(PropertyCanonicalNameUtils.UNKNOWNCLASS)) {
                classes.add(null);
            } else {
                ValueClass cls = findClass(className);
                classes.add(cls);
            }
        }
        return classes;
    }
    
    private List<ResolveClassSet> getParamClassesByParamProperties(List<LPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = new ArrayList<>();
        for (LPWithParams paramProp : paramProps) {
            if (paramProp.property != null) {
                LCP lcp = (LCP)paramProp.property;
                List<ResolveClassSet> usedClasses = getUsedClasses(params, paramProp.usedParams);
                classes.add(lcp.getResolveClassSet(usedClasses));
            } else {
                TypedParameter param = params.get(paramProp.usedParams.get(0));
                if (param.cls == null) {
                    classes.add(null);
                } else {
                    classes.add(param.cls.getResolveSet());
                }
            }
        }
        return classes;
    }
    
    public List<ResolveClassSet> getSignatureForGProp(List<LPWithParams> paramProps, List<TypedParameter> params) {
        return getParamClassesByParamProperties(paramProps, params);
    }

    private LP findJoinMainProp(String mainPropName, String sourceName, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, context);
        return findLPByNameAndClasses(mainPropName, sourceName, classes);
    }
    
    private LP findJoinMainProp(PropertyUsage mainProp, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null) {
            return findLPByPropertyUsage(mainProp);
        } else {
            return findJoinMainProp(mainProp.name, mainProp.getSourceName(), paramProps, context);
        }
    }

    private LP findJoinMainProp(PropertyUsage mainProp, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        return findJoinMainProp(mainProp, params, false);
    }
    
    private LP findJoinMainProp(PropertyUsage mainProp, List<TypedParameter> params, boolean onlyAbstract) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null) {
            return findLPByPropertyUsage(mainProp, onlyAbstract, false); 
        } else {
            return findLPByNameAndClasses(mainProp.name, mainProp.getSourceName(), getClassesFromTypedParams(params), onlyAbstract, true);
        }
    }
    
    public LPWithParams addScriptedJProp(boolean user, PropertyUsage pUsage, List<LPWithParams> paramProps, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findJoinMainProp(pUsage, paramProps, params);
        return addScriptedJProp(user, mainProp, paramProps);
    }
    
    public LPWithParams addScriptedJProp(LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(false, mainProp, paramProps);
    }
    
    public LPWithParams addScriptedJProp(boolean user, LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkCalculationProperty(mainProp);
        checks.checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LP prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
            lastOptimizedJPropSID = mainProp.property.getSID();
        } else {
            prop = addJProp(user, LocalizedString.NONAME, (LCP) mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    private LCP getRelationProp(String op) {
        switch (op) {
            case "==":
                return baseLM.equals2;
            case "=":
                return baseLM.equals2;
            case "!=":
                return baseLM.diff2;
            case ">":
                return baseLM.greater2;
            case "<":
                return baseLM.less2;
            case ">=":
                return baseLM.groeq2;
            case "<=":
                return baseLM.lsoeq2;
        }
        assert false;
        return null;
    }

    private LCP getArithProp(String op) {
        switch (op) {
            case "+":
                return baseLM.sum;
            case "-":
                return baseLM.subtract;
            case "*":
                return baseLM.multiply;
            case "/":
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
        if(property == null)
            return false;

        Type type = property.property.getType();
        return type != null && type.equals(LogicalClass.instance);
    }

    private LPWithParams toLogical(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(and(false), Arrays.asList(new LPWithParams(baseLM.vtrue, new ArrayList<Integer>()), property));
    }

    public LPWithParams addScriptedIfProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            boolean[] notsArray = new boolean[properties.size() - 1];
            Arrays.fill(notsArray, false);
            if (properties.get(0).property != null) {
                checks.checkCalculationProperty(properties.get(0).property);
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
                checks.checkCalculationProperty(properties.get(0).property);
            }
            if (!isLogical(properties.get(0).property)) {
                properties.get(0).property = toLogical(properties.get(0)).property;
            }
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedIfElseUProp(LPWithParams ifProp, LPWithParams thenProp, LPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
        assert !(thenProp.property instanceof LAP) && (elseProp == null || !(elseProp.property instanceof LAP));
        List<LPWithParams> lpParams = new ArrayList<>();
        lpParams.add(addScriptedJProp(and(false), asList(thenProp, ifProp)));
        if (elseProp != null) {
            lpParams.add(addScriptedJProp(and(true), asList(elseProp, ifProp)));
        }
        return addScriptedUProp(Union.EXCLUSIVE, lpParams, "IF");
    }

    public LPWithParams addScriptedCaseUProp(List<LPWithParams> whenProps, List<LPWithParams> thenProps, LPWithParams elseProp, boolean isExclusive) {
        assert whenProps.size() > 0 && whenProps.size() == thenProps.size();

        List<LPWithParams> caseParamProps = new ArrayList<>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParamProps.add(whenProps.get(i));
            caseParamProps.add(thenProps.get(i));
        }
        if (elseProp != null) {
            caseParamProps.add(elseProp);
        }

        LP caseProp = addCaseUProp(null, false, LocalizedString.NONAME, isExclusive, getParamsPlainList(caseParamProps).toArray());
        return new LPWithParams(caseProp, mergeAllParams(caseParamProps));
    }

    public LPWithParams addScriptedMultiProp(List<LPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.CLASS, properties, "MULTI");
        } else {
            return addScriptedUProp(Union.CLASSOVERRIDE, properties, "MULTI");
        }
    }

    public LPWithParams addScriptedFileAProp(FileActionType actionType, LPWithParams property, LPWithParams pathProp, boolean isAbsolutPath, boolean noDialog) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> params = new ArrayList<>();
        params.add(property);
        if(pathProp != null)
            params.add(pathProp);

        LAP<?> res;
        switch (actionType) {
            case OPEN:
                res = addOFAProp(property.property.property.getValueClass(ClassType.valuePolicy),
                        pathProp == null ? null : pathProp.property.property.getValueClass(ClassType.valuePolicy));
                break;
            default: // SAVE
                res = addSFAProp(property.property.property.getValueClass(ClassType.valuePolicy),
                        pathProp == null ? null : pathProp.property.property.getValueClass(ClassType.valuePolicy),
                        isAbsolutPath, noDialog);
        }
        return addScriptedJoinAProp(res, params);
    }

    public LP addScriptedCustomActionProp(String javaClassName, List<String> classes, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ActionProperty instance;
            if (classes == null || classes.isEmpty()) {
                instance = (ActionProperty) Class.forName(javaClassName).getConstructor(this.getClass()).newInstance(this);
            } else {
                ValueClass[] classList = new ValueClass[classes.size()];
                for (int i = 0; i < classes.size(); i++) {
                    classList[i] = findClass(classes.get(i));
                }
                instance = (ActionProperty) Class.forName(javaClassName).getConstructor(new Class[] {this.getClass(), ValueClass[].class}).newInstance(this, classList);
            }
            if (instance instanceof ExplicitActionProperty && allowNullValue) {
                ((ExplicitActionProperty) instance).allowNullValue = true;
            }
            return baseLM.addAProp(null, instance);
        } catch (ClassNotFoundException e) {
            errLog.emitClassNotFoundError(parser, javaClassName);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, javaClassName);
        }
        return null;
    }

    public LP addScriptedCustomActionProp(String code, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        String script = "";
        try {

            script = code.substring(1, code.length() - 1); //remove brackets

            String javaClass = "import lsfusion.server.data.SQLHandledException;\n" +
                    "import lsfusion.server.logics.property.ClassPropertyInterface;\n" +
                    "import lsfusion.server.logics.property.ExecutionContext;\n" +
                    "import lsfusion.server.logics.scripted.ScriptingActionProperty;\n" +
                    "import lsfusion.server.logics.scripted.ScriptingLogicsModule;\n" +
                    "\n" +
                    "import java.sql.SQLException;\n" +
                    "\n" +
                    "public class ExecuteActionProperty extends ScriptingActionProperty {\n" +
                    "\n" +
                    "    public ExecuteActionProperty(ScriptingLogicsModule LM) {\n" +
                    "        super(LM);\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {\n" +
                    "        try {\n" +
                    script +
                    "        } catch (Exception e) {\n" +
                    "            e.printStackTrace();\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(javaClass);
            Class<?> executeClass = sc.getClassLoader().loadClass("ExecuteActionProperty");

            ActionProperty instance = (ActionProperty) executeClass.getConstructor(ScriptingLogicsModule.class).newInstance(this);
            if (instance instanceof ExplicitActionProperty && allowNullValue) {
                ((ExplicitActionProperty) instance).allowNullValue = true;
            }
            return baseLM.addAProp(null, instance);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, script);
        }
        return null;
    }

    public LP addScriptedExternalJavaActionProp() {
        throw new UnsupportedOperationException("CUSTOM JAVA not supported");
    }

    private String transformExternalText(String text) {
        return transformFormulaText(text, ExternalActionProperty.getParamName("$1"));
    }
    public LP addScriptedExternalDBActionProp(String connectionString, String exec, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addAProp(new ExternalDBActionProperty(findFormulaParameters(connectionString + " " + exec).size(), transformExternalText(connectionString), transformExternalText(exec), findLCPsByPropertyUsage(toPropertyUsageList)));
    }

    public LP addScriptedExternalDBFActionProp(String connectionString, PropertyUsage queryFile, String charset, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addAProp(new ExternalDBFActionProperty(findFormulaParameters(connectionString).size(), transformExternalText(connectionString), findLCPByPropertyUsage(queryFile), charset, findLCPsByPropertyUsage(toPropertyUsageList)));
    }

    public LP addScriptedExternalHTTPActionProp(String connectionString, Integer bodyParamsCount, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addAProp(new ExternalHTTPActionProperty(findFormulaParameters(connectionString).size(), bodyParamsCount == null ? 0 : bodyParamsCount, transformExternalText(connectionString), findLCPsByPropertyUsage(toPropertyUsageList)));
    }

    public LP addScriptedExternalLSFActionProp(String action, Integer bodyParamsCount, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        String execQuery = String.format("http://localhost:%s/exec?action=%s", ThreadLocalContext.getRmiManager().getHttpPort(), transformExternalText(action));
        return addAProp(new ExternalHTTPActionProperty(findFormulaParameters(action).size(), getSignatureSize(action, bodyParamsCount), execQuery, findLCPsByPropertyUsage(toPropertyUsageList)));
    }

    private int getSignatureSize(String action, Integer bodyParamsCount) {
        if (bodyParamsCount == null) {
            int bracketPos = action.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
            if (bracketPos >= 0 && action.lastIndexOf(PropertyCanonicalNameUtils.signatureRBracket) == action.length() - 1)
                bodyParamsCount = StringUtils.countMatches(action.substring(bracketPos + 1, action.length() - 1), ",") + 1;
        }
        return bodyParamsCount == null ? 0 : bodyParamsCount;
    }

    private List<LCP> findLCPsByPropertyUsage(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = new ArrayList<>();
        for (PropertyUsage propUsage : propUsages) {
            LCP<?> lcp = findLCPByPropertyUsage(propUsage);
            props.add(lcp);
        }
        return props;
    }

    public LPWithParams addScriptedEmailProp(LPWithParams fromProp,
                                             LPWithParams subjProp,
                                             List<Message.RecipientType> recipTypes,
                                             List<LPWithParams> recipProps,
                                             List<String> forms,
                                             List<FormStorageType> formTypes,
                                             List<OrderedMap<String, LPWithParams>> mapObjects,
                                             List<LPWithParams> attachNames,
                                             List<AttachmentFormat> attachFormats,
                                             List<LPWithParams> attachFileNames,
                                             List<LPWithParams> attachFiles,
                                             List<LPWithParams> inlineTexts) throws ScriptingErrorLog.SemanticErrorException {

        List<LPWithParams> allProps = new ArrayList<>();

        if (fromProp != null) {
            allProps.add(fromProp);
        }
        allProps.add(subjProp);
        allProps.addAll(recipProps);

        List<ObjectEntity> formObjects = new ArrayList<>(Collections.<ObjectEntity>nCopies(allProps.size(), null)); 
        for (int i = 0; i < forms.size(); ++i) {
            FormEntity form = findForm(forms.get(i));
            for (Map.Entry<String, LPWithParams> e : mapObjects.get(i).entrySet()) {
                allProps.add(e.getValue());
                formObjects.add(findObjectEntity(form, e.getKey()));
            }
            
            if (formTypes.get(i) == FormStorageType.ATTACH && attachNames.get(i) != null) {
                allProps.add(attachNames.get(i));
                formObjects.add(null);
            }
        }
        
        for (int i = 0; i < attachFileNames.size(); i++) {
            if (attachFileNames.get(i) != null) {
                allProps.add(attachFileNames.get(i));
            }
            allProps.add(attachFiles.get(i));
        }

        allProps.addAll(inlineTexts);
        
        formObjects.addAll(Collections.<ObjectEntity>nCopies(allProps.size() - formObjects.size(), null));        

        Object[] allParams = getParamsPlainList(allProps).toArray();

        ImOrderSet<PropertyInterface> tempContext = genInterfaces(getIntNum(allParams));
        ValueClass[] eaClasses = CalcProperty.getCommonClasses(tempContext, readCalcImplements(tempContext, allParams).getCol(), formObjects);

        LAP<ClassPropertyInterface> eaPropLP = BL.emailLM.addEAProp(null, LocalizedString.NONAME, eaClasses, null, null);
        SendEmailActionProperty eaProp = (SendEmailActionProperty) eaPropLP.property;

        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> allImplements = readCalcImplements(eaPropLP.listInterfaces, allParams);

        int i = 0;
        if (fromProp != null) {
            eaProp.setFromAddressAccount(allImplements.get(i++));
        } else {
            // по умолчанию используем стандартный fromAddressAccount
            eaProp.setFromAddressAccount(new CalcPropertyMapImplement(BL.emailLM.findProperty("fromAddressDefaultNotificationAccount[]").property));
        }
        eaProp.setSubject(allImplements.get(i++));

        for (Message.RecipientType recipType : recipTypes) {
            eaProp.addRecipient(allImplements.get(i++), recipType);
        }

        for (int j = 0; j < forms.size(); ++j) {
            String formName = forms.get(j);
            FormStorageType formType = formTypes.get(j);
            FormEntity form = findForm(formName);

            Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements = new HashMap<>();
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

        for (LPWithParams fileName : attachFileNames) {
            CalcPropertyInterfaceImplement<ClassPropertyInterface> attachFileName = fileName != null ? allImplements.get(i++) : null;
            eaProp.addAttachmentFile(attachFileName, allImplements.get(i++));
        }

        for(int j = 0; j < inlineTexts.size(); j++)
            eaProp.addInlineText(allImplements.get(i++));

        return new LPWithParams(eaPropLP, mergeAllParams(allProps));
    }

    public LPWithParams addScriptedAdditiveOrProp(List<String> operands, List<LPWithParams> properties) {
        assert operands.size() + 1 == properties.size();
        
        LPWithParams res = properties.get(0);
        if (operands.size() > 0) {
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
            res = new LPWithParams(addUProp(null, LocalizedString.NONAME, Union.SUM, null, coeffs, resultParams.toArray()), mergeAllParams(properties));
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
        return addScriptedJProp(baseLM.minus, Collections.singletonList(prop));
    }

    public LPWithParams addScriptedNotProp(LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(not(), Collections.singletonList(prop));
    }

    public LPWithParams addScriptedCastProp(String typeName, LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(typeName);

        //cls всегда будет DataClass из-за грамматики
        assert cls instanceof DataClass;

        return addScriptedJProp(addCastProp((DataClass) cls), Collections.singletonList(prop));
    }

    private boolean doesExtendContext(int contextSize, List<LPWithParams> list, List<LPWithParams> orders) {
        Set<Integer> listContext = new HashSet<>();
        for(int i=0;i<contextSize;i++)
            listContext.add(i);
        for(LPWithParams lp : list)
            if(lp.property != null)
                listContext.addAll(lp.usedParams);
        return !listContext.containsAll(mergeAllParams(orders));
    }

    private List<Integer> mergeAllParams(List<LPWithParams> lpList) {
        Set<Integer> s = new TreeSet<>();
        for (LPWithParams mappedLP : lpList) {
            s.addAll(mappedLP.usedParams);
        }
        return new ArrayList<>(s);
    }

    private List<Integer> mergeIntLists(List<List<Integer>> lists) {
        Set<Integer> s = new TreeSet<>();
        for (List<Integer> list : lists) {
            s.addAll(list);
        }
        return new ArrayList<>(s);
    }

    public LPWithParams addScriptedListAProp(List<LPWithParams> properties, List<LP> localProps) {
        List<Object> resultParams = getParamsPlainList(properties);

        MExclSet<Pair<LP, List<ResolveClassSet>>> mDebugLocals = null;
        if(debugger.isEnabled()) {
            mDebugLocals = SetFact.mExclSet(localProps.size());
        }
        MSet<SessionDataProperty> mLocals = SetFact.mSet();
        for (LP<?, ?> localProp : localProps) {
            List<ResolveClassSet> localSignature = propClasses.remove(localProp);
            removeModuleProperty((LCP) localProp);
            
            if(mDebugLocals != null)
                mDebugLocals.exclAdd(new Pair<LP, List<ResolveClassSet>>(localProp, localSignature));

            mLocals.add((SessionDataProperty) localProp.property);
        }

        LAP<?> listLP = addListAProp(mLocals.immutable(), resultParams.toArray());

        if(mDebugLocals != null) {
            listLP.property.setDebugLocals(mDebugLocals.immutable());
        }

        List<Integer> usedParams = mergeAllParams(properties);
        return new LPWithParams(listLP, usedParams);
    }

    public LPWithParams addScriptedNewSessionAProp(LPWithParams action, List<PropertyUsage> migrateSessionProps, boolean migrateAllSessionProps,
                                                   boolean isNested, boolean singleApply, boolean newSQL) throws ScriptingErrorLog.SemanticErrorException {
        LAP<?> sessionLP = addNewSessionAProp(null, (LAP) action.property, isNested, singleApply, newSQL, getMigrateProps(migrateSessionProps, migrateAllSessionProps));
        return new LPWithParams(sessionLP, action.usedParams);
    }

    public DataClass getInputDataClass(String paramName, List<TypedParameter> context, String typeId, LPWithParams oldValue, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        DataClass requestDataClass;
        if(typeId != null) {
            requestDataClass = ClassCanonicalNameUtils.getScriptedDataClass(typeId);
        } else {
            ValueClass valueClass = oldValue.property.property.getValueClass(ClassType.valuePolicy);
            checks.checkInputDataClass(valueClass);
            requestDataClass = (DataClass) valueClass;
        }

        if(paramName != null)
            getParamIndex(new TypedParameter(requestDataClass, paramName), context, true, insideRecursion);
        return requestDataClass;
    }

    public LPWithParams addScriptedInputAProp(DataClass requestDataClass, LPWithParams oldValue, PropertyUsage targetProp, LPWithParams doAction, LPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean assign, LPWithParams changeProp, DebugInfo.DebugPoint assignDebugPoint) throws ScriptingErrorLog.SemanticErrorException {
        assert targetProp == null;
        LCP tprop = getInputProp(targetProp, requestDataClass, null);

        LAP property = addInputAProp(requestDataClass, (LCP<?>) tprop != null ? ((LCP<?>) tprop).property : null);

        if(oldValue == null)
            oldValue = new LPWithParams(baseLM.vnull, new ArrayList<Integer>());
        LPWithParams inputAction = addScriptedJoinAProp(property, Collections.singletonList(oldValue));
        
        if(changeProp == null)
            changeProp = oldValue;

        return proceedDoClause(doAction, elseAction, oldContext, newContext, ListFact.singleton(tprop), inputAction,
                ListFact.singleton(assign ? new Pair<>(changeProp, assignDebugPoint) : null));
    }


    public LPWithParams addScriptedRequestAProp(LPWithParams requestAction, LPWithParams doAction, LPWithParams elseAction) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> propParams = new ArrayList<>();
        propParams.add(requestAction);
        propParams.add(doAction);
        if(elseAction != null)
            propParams.add(elseAction);

        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addRequestAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedActiveFormAProp(String formName) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(formName);
        return new LPWithParams(addAProp(null, new IsActiveFormActionProperty(LocalizedString.NONAME, form, baseLM.getIsActiveFormProperty())), new ArrayList<Integer>());
    }

    public LPWithParams addScriptedActivateAProp(FormEntity form, ComponentView component) throws ScriptingErrorLog.SemanticErrorException {
        return new LPWithParams(addAProp(null, new ActivateActionProperty(LocalizedString.NONAME, form, component)), new ArrayList<Integer>());
    }

    public LCP addLocalDataProperty(String name, String returnClassName, List<String> paramClassNames, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
        List<ResolveClassSet> signature = new ArrayList<>();
        for (String className : paramClassNames) {
            signature.add(findClass(className).getResolveSet());
        }
        checks.checkDuplicateProperty(name, signature, BL);

        LCP res = addScriptedDProp(returnClassName, paramClassNames, true, false, true, nestedType);
        makePropertyPublic(res, name, signature);
        return res;
    }

    public LP addWatchLocalDataProperty(LP lp, List<ResolveClassSet> signature) {
        assert lp.property instanceof SessionDataProperty;
        addModuleLP(lp);
        propClasses.put(lp, signature);
        return lp; 
    }
    
    public LPWithParams addScriptedJoinAProp(PropertyUsage pUsage, List<LPWithParams> properties, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findJoinMainProp(pUsage, properties, params);    
        return addScriptedJoinAProp(mainProp, properties);                        
    }
    
    public LPWithParams addScriptedJoinAProp(LP mainProp, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkActionProperty(mainProp);
        checks.checkParamCount(mainProp, properties.size());

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);
        LP prop = addJoinAProp(null, LocalizedString.NONAME, (LAP<?>) mainProp, resultParams.toArray());
        return new LPWithParams(prop, usedParams);
    }

    public LPWithParams addScriptedConfirmProp(LPWithParams msgProp, LPWithParams doAction, LPWithParams elseAction, boolean yesNo, List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        LCP targetProp = null;
        if(yesNo)
            targetProp = getInputProp(null, LogicalClass.instance, null);
        
        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addConfirmAProp("lsFusion", yesNo, targetProp, resultParams.toArray());
        LPWithParams inputAction = new LPWithParams(asyncLAP, msgProp.usedParams);
                
        return proceedDoClause(doAction, elseAction, oldContext, newContext, yesNo ? ListFact.singleton(targetProp) : ListFact.<LCP>EMPTY(), inputAction, yesNo ? ListFact.<Pair<LPWithParams, DebugInfo.DebugPoint>>singleton(null) : ListFact.<Pair<LPWithParams, DebugInfo.DebugPoint>>EMPTY());
    }

    public LPWithParams addScriptedMessageProp(LPWithParams msgProp, boolean noWait) {
        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addMAProp("lsFusion", noWait, resultParams.toArray());
        return new LPWithParams(asyncLAP, msgProp.usedParams);
    }

    public LPWithParams addScriptedAsyncUpdateProp(LPWithParams asyncProp) {
        List<Object> resultParams = getParamsPlainList(singletonList(asyncProp));
        LAP asyncLAP = addAsyncUpdateAProp(resultParams.toArray());
        return new LPWithParams(asyncLAP, asyncProp.usedParams);
    }

    private FormEntity getFormFromSeekObjectName(String formObjectName) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = formObjectName.lastIndexOf('.');
        assert pointPos > 0;

        String formName = formObjectName.substring(0, pointPos);
        return findForm(formName);
    }

    private ObjectEntity getSeekObject(FormEntity form, String formObjectName) throws ScriptingErrorLog.SemanticErrorException {
        return form.getNFObject(getSeekObjectName(formObjectName), getVersion());
    }

    private GroupObjectEntity getSeekGroupObject(FormEntity form, String formObjectName) throws ScriptingErrorLog.SemanticErrorException {
        return form.getNFGroupObject(getSeekObjectName(formObjectName), getVersion());
    }

    private String getSeekObjectName(String formObjectName) {
        int pointPos = formObjectName.lastIndexOf('.');
        assert pointPos > 0;

        return formObjectName.substring(pointPos + 1);
    }

    public LPWithParams addScriptedObjectSeekProp(String name, LPWithParams seekProp, boolean last) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        ObjectEntity object = getSeekObject(form, name);
        
        if (object != null) {
            List<Object> resultParams = getParamsPlainList(singletonList(seekProp));
            LAP lap = addOSAProp(object, last, resultParams.toArray());
            return new LPWithParams(lap, seekProp.usedParams);
        } else {
            errLog.emitObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LPWithParams addScriptedGroupObjectSeekProp(String name, List<String> objNames, List<LPWithParams> values, boolean last) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        GroupObjectEntity groupObject = getSeekGroupObject(form, name);
        
        List<ObjectEntity> objects = new ArrayList<>();
        if (objNames != null) {
            for (String objName : objNames) {
                ObjectEntity obj = form.getNFObject(objName, getVersion());
                if (obj == null) {
                    errLog.emitObjectNotFoundError(parser, objName);
                } else if (obj.groupTo != groupObject) {
                    errLog.emitObjectOfGroupObjectError(parser, obj.getSID(), groupObject.getSID());                    
                }
                objects.add(obj);
            }
        }
        
        if (groupObject != null) {
            List<Object> resultParams = getParamsPlainList(values);
            LAP lap = addGOSAProp(groupObject, objects, last, resultParams.toArray());
            return new LPWithParams(lap, mergeAllParams(values));
        } else {
            errLog.emitNotFoundError(parser, "group оbject", getSeekObjectName(name));
            return null;
        }
    }

    public LPWithParams addScriptedEvalActionProp(LPWithParams property, List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        Type exprType = property.property.property.getType();
        if (!(exprType instanceof StringClass)) {
            errLog.emitEvalExpressionError(parser);
        }

        List<LCP<?>> paramsLCP = new ArrayList<>();
        Set<Integer> allParams = new TreeSet<>(property.usedParams);
        if (params != null) {
            for (LPWithParams param : params) {
                paramsLCP.add((LCP) param.property);
                allParams.addAll(param.usedParams);
            }
        }

        LAP<?> res = addEvalAProp((LCP) property.property, paramsLCP);
        return new LPWithParams(res, new ArrayList<>(allParams));
    }

    public LPWithParams addScriptedDrillDownActionProp(LPWithParams property) {
        LAP<?> res = addDrillDownAProp((LCP) property.property);
        return new LPWithParams(res, property.usedParams);
    }

    public LPWithParams addScriptedAssignPropertyAProp(List<TypedParameter> context, PropertyUsage toPropertyUsage, List<LPWithParams> toPropertyMapping, LPWithParams fromProperty, LPWithParams whereProperty, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        LP toPropertyLP = findJoinMainProp(toPropertyUsage, toPropertyMapping, newContext);

        LPWithParams toProperty = addScriptedJProp(toPropertyLP, toPropertyMapping);

        return addScriptedAssignAProp(context, fromProperty, whereProperty, toProperty);
    }

    private LPWithParams addScriptedAssignAProp(List<TypedParameter> context, LPWithParams fromProperty, LPWithParams whereProperty, LPWithParams toProperty) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAssignProperty(fromProperty, toProperty);

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, fromProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, Collections.singletonList(resI)));
        }
        paramsList.add(toProperty);
        paramsList.add(fromProperty);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LP result = addSetPropertyAProp(null, LocalizedString.NONAME, resultInterfaces.size(), whereProperty != null, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public LPWithParams addScriptedAddObjProp(List<TypedParameter> context, String className, PropertyUsage toPropUsage, List<LPWithParams> toPropMapping, LPWithParams whereProperty, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkAddActionsClass(cls);
        checks.checkAddObjTOParams(context.size(), toPropMapping);

        LPWithParams toProperty = null;
        if (toPropUsage != null && toPropMapping != null) {
            toProperty = addScriptedJProp(findJoinMainProp(toPropUsage, toPropMapping, newContext), toPropMapping);
        }

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, Collections.singletonList(resI)));
        }
        if (toProperty != null) {
            paramsList.add(toProperty);
        } else if (whereProperty == null) {
            paramsList.add(new LPWithParams(new LCP<>(baseLM.getAddedObjectProperty()), new ArrayList<Integer>()));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LAP result = addAddObjAProp((CustomClass) cls, false, resultInterfaces.size(), whereProperty != null, toProperty != null || whereProperty == null, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public LPWithParams addScriptedDeleteAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = addScriptedChangeClassAProp(oldContextSize, newContext, param, baseClass.unknown, whereProperty);
        setDeleteActionOptions((LAP) res.property);
        return res;
    }

    public LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, String className, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkChangeClassActionClass(cls);
        return addScriptedChangeClassAProp(oldContextSize, newContext, param, (ConcreteCustomClass) cls, whereProperty);
    }

    private LPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, ConcreteObjectClass cls, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> paramList = new ArrayList<>();
        paramList.add(param);
        if (whereProperty != null) {
            paramList.add(whereProperty);
        }
        List<Integer> allParams = mergeAllParams(paramList);
        int changedIndex = allParams.indexOf(param.usedParams.get(0));

        List<Integer> resultInterfaces = new ArrayList<>();
        for (int paramIndex : allParams) {
            if (paramIndex >= oldContextSize) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }
        boolean contextExtended = allParams.size() > resultInterfaces.size();

        checks.checkChangeClassWhere(contextExtended, param, whereProperty, newContext);

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, Collections.singletonList(resI)));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);

        LAP<?> res = addChangeClassAProp(cls, resultInterfaces.size(), changedIndex, contextExtended, whereProperty != null, resultParams.toArray());
        return new LPWithParams(res,  resultInterfaces);
    }

    private List<Integer> getResultInterfaces(int contextSize, LPWithParams... params) {
        List<LPWithParams> lpList = new ArrayList<>();
        for (LPWithParams lp : params) {
            if (lp != null) {
                lpList.add(lp);
            }
        }
        List<Integer> allParams = mergeAllParams(lpList);
        
        //все использованные параметры, которые были в старом контексте, идут на вход результирующего свойства
        List<Integer> resultInterfaces = new ArrayList<>();
        for (int paramIndex : allParams) {
            if (paramIndex >= contextSize) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }
        return resultInterfaces;
    }

    public LPWithParams addScriptedIfAProp(LPWithParams condition, LPWithParams trueAction, LPWithParams falseAction) {
        List<LPWithParams> propParams = toList(condition, trueAction);
        if (falseAction != null) {
            propParams.add(falseAction);
        }
        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addIfAProp(null, LocalizedString.NONAME, false, getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedTryAProp(LPWithParams tryAction, LPWithParams finallyAction) {
        List<LPWithParams> propParams = new ArrayList<>();
        propParams.add(tryAction);
        if (finallyAction != null) {
            propParams.add(finallyAction);
        }
        
        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addTryAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedCaseAProp(List<LPWithParams> whenProps, List<LPWithParams> thenActions, LPWithParams elseAction, boolean isExclusive) {
        assert whenProps.size() > 0 && whenProps.size() == thenActions.size();

        List<LPWithParams> caseParams = new ArrayList<>();
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
        List<Integer> allParams = mergeAllParams(actions);
        LP result = addMultiAProp(isExclusive, getParamsPlainList(actions).toArray());
        return new LPWithParams(result, allParams);

    }

    public LPWithParams addScriptedApplyAProp(LPWithParams action, boolean singleApply, List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps, boolean serializable) 
            throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> propParams = Collections.singletonList(action);

        LP result = addApplyAProp(null, LocalizedString.NONAME, (action != null && action.property instanceof LAP) ? (LAP) action.property : null, singleApply,
                getMigrateProps(keepSessionProps, keepAllSessionProps), serializable);

        return new LPWithParams(result, mergeAllParams(propParams));
    }

    public LPWithParams addScriptedCancelAProp(List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps)
            throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCancelAProp();");

        LP result = addCancelAProp(null, LocalizedString.NONAME, getMigrateProps(keepSessionProps, keepAllSessionProps));

        return new LPWithParams(result, new ArrayList<Integer>());
    }

    private FunctionSet<SessionDataProperty> getMigrateProps(List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps) throws ScriptingErrorLog.SemanticErrorException {
        FunctionSet<SessionDataProperty> keepProps;
        if(keepAllSessionProps) {
            keepProps = DataSession.keepAllSessionProperties;
        } else {
            MExclSet<SessionDataProperty> mKeepProps = SetFact.mExclSet(keepSessionProps.size());
            for (PropertyUsage migratePropUsage : keepSessionProps) {
                LP<?, ?> prop = findLPByPropertyUsage(migratePropUsage);
                checks.checkSessionProperty(prop);
                mKeepProps.exclAdd((SessionDataProperty) prop.property);
            }
            keepProps = mKeepProps.immutable();
        }
        return keepProps;
    }

    public LPWithParams addScriptedNewAProp(List<TypedParameter> oldContext, LPWithParams action, Integer addNum, String addClassName, Boolean autoSet) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedForAProp(oldContext, null, new ArrayList<LPWithParams>(), action, null, addNum, addClassName, autoSet, false, false, new ArrayList<LPWithParams>(), false);
    }
    
    public LPWithParams addScriptedForAProp(List<TypedParameter> oldContext, LPWithParams condition, List<LPWithParams> orders, LPWithParams action, LPWithParams elseAction, Integer addNum, String addClassName, Boolean autoSet, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        boolean ordersNotNull = (condition != null ? doesExtendContext(oldContext.size(), singletonList(condition), orders) : !orders.isEmpty());

        List<LPWithParams> creationParams = new ArrayList<>();
        if (condition != null) {
            creationParams.add(condition);
        }
        creationParams.addAll(orders);
        if(addNum != null) {
            creationParams.add(new LPWithParams(null, Collections.singletonList(addNum)));
        }
        if (elseAction != null) {
            creationParams.add(elseAction);
        }
        creationParams.add(action);
        List<Integer> allParams = mergeAllParams(creationParams);

        List<Integer> usedParams = new ArrayList<>();
        List<Integer> extParams = new ArrayList<>();
        for (int paramIndex : allParams) {
            if (paramIndex < oldContext.size()) {
                usedParams.add(paramIndex);
            } else {
                extParams.add(paramIndex);
            }
        }
        
        if(ActionPropertyDebugger.watchHack.get() != null && extParams.size() > 1) {
            ActionPropertyDebugger.watchHack.set(true);
        }

        checks.checkForActionPropertyConstraints(recursive, usedParams, allParams);

        List<LPWithParams> allCreationParams = new ArrayList<>();
        for (int usedParam : usedParams) {
            allCreationParams.add(new LPWithParams(null, Collections.singletonList(usedParam)));
        }
        allCreationParams.addAll(creationParams);
        if(noInline==null) { // предполагается надо включить все кроме addNum
            noInline = new ArrayList<>();
            for (int extParam : extParams)
                if(addNum==null || !addNum.equals(extParam)) {
                    noInline.add(new LPWithParams(null, Collections.singletonList(extParam)));
                }
        }
        allCreationParams.addAll(noInline);

        LP result = addForAProp(null, LocalizedString.NONAME, !descending, ordersNotNull, recursive, elseAction != null, usedParams.size(),
                                addClassName != null ? (CustomClass) findClass(addClassName) : null, autoSet != null ? autoSet : false, condition != null, noInline.size(), forceInline,
                                getParamsPlainList(allCreationParams).toArray());
        return new LPWithParams(result, usedParams);
    }

    public LPWithParams getTerminalFlowActionProperty(boolean isBreak) {
        return new LPWithParams(isBreak ? new LAP<>(new BreakActionProperty()) : new LAP<>(new ReturnActionProperty()), new ArrayList<Integer>());
    }

    private List<Integer> getParamsAssertList(List<LPWithParams> list) {
        List<Integer> result = new ArrayList<>();
        for(LPWithParams lp : list) {
            assert lp.property == null;
            result.add(BaseUtils.single(lp.usedParams));
        }
        return result;
    }

    @SafeVarargs
    private final List<Object> getParamsPlainList(List<LPWithParams>... mappedPropLists) {
        List<LP> props = new ArrayList<>();
        List<List<Integer>> usedParams = new ArrayList<>();
        for (List<LPWithParams> mappedPropList : mappedPropLists) {
            for (LPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.property);
                usedParams.add(mappedProp.usedParams);
            }
        }
        return getParamsPlainList(props, usedParams);
    }

    private List<Object> getParamsPlainList(List<LP> paramProps, List<List<Integer>> usedParams) {
        List<Integer> allUsedParams = mergeIntLists(usedParams);
        List<Object> resultParams = new ArrayList<>();

        for (int i = 0; i < paramProps.size(); i++) {
            LP property = paramProps.get(i);
            if (property != null) {
                resultParams.add(property);
                for (int paramIndex : usedParams.get(i)) {
                    int localParamIndex = allUsedParams.indexOf(paramIndex);
                    assert localParamIndex >= 0;
                    resultParams.add(localParamIndex + 1);
                }
            } else {
                int localParamIndex = allUsedParams.indexOf(usedParams.get(i).get(0));
                assert localParamIndex >= 0;
                resultParams.add(localParamIndex + 1);
            }
        }
        return resultParams;
    }

    public LCP addScriptedGProp(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps, List<LPWithParams> orderProps,
                                  boolean ascending, LPWithParams whereProp, List<TypedParameter> innerInterfaces) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkGPropOrderConsistence(type, orderProps.size());
        checks.checkGPropAggregateConsistence(type, mainProps.size());
        checks.checkGPropAggrConstraints(type, mainProps, groupProps);
        checks.checkGPropWhereConsistence(type, whereProp);
        checks.checkGPropSumConstraints(type, mainProps.get(0));

        List<LPWithParams> whereProps = new ArrayList<>();
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(null, Collections.singletonList(mainProps.get(0).usedParams.get(0))));
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

        boolean ordersNotNull = doesExtendContext(0, mergeLists(mainProps, groupProps), orderProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps)).size();
        List<ResolveClassSet> explicitInnerClasses = getClassesFromTypedParams(innerInterfaces);
        assert groupPropParamCount == explicitInnerClasses.size(); // в отличии скажем от Partition, тут внешнего контекста быть
        LocalizedString emptyCaption = LocalizedString.NONAME;
        LCP resultProp = null;
        if (type == GroupingType.SUM) {
            resultProp = addSGProp(null, false, false, emptyCaption, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.MAX || type == GroupingType.MIN) {
            resultProp = addMGProp(null, false, emptyCaption, type == GroupingType.MIN, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.CONCAT) {
            resultProp = addOGProp(null, false, emptyCaption, GroupType.STRING_AGG, orderProps.size(), ordersNotNull, !ascending, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            resultProp = addAGProp(null, false, false, emptyCaption, type == GroupingType.NAGGR, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.EQUAL) {
            resultProp = addCGProp(null, false, false, emptyCaption, null, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.LAST) {
            resultProp = addOGProp(null, false, emptyCaption, GroupType.LAST, orderProps.size(), ordersNotNull, !ascending, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
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
        checks.checkPropertyTypes(paramProps, errMsgPropType);

        int[] coeffs = null;
        if (unionType == Union.SUM) {
            coeffs = new int[paramProps.size()];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = 1;
            }
        }
        List<Object> resultParams = getParamsPlainList(paramProps);
        LCP prop = addUProp(null, LocalizedString.NONAME, unionType, null, coeffs, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    public LPWithParams addScriptedPartitionProp(PartitionType partitionType, PropertyUsage ungroupPropUsage, boolean strict, int precision, boolean isAscending,
                                                 boolean useLast, int groupPropsCnt, int groupPropsContextSize, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkPartitionWindowConsistence(partitionType, useLast);
        LP ungroupProp = ungroupPropUsage != null ? findJoinMainProp(ungroupPropUsage, paramProps.subList(1, groupPropsCnt + 1), context) : null;
        checks.checkPartitionUngroupConsistence(ungroupProp, groupPropsCnt);

        boolean ordersNotNull = doesExtendContext(groupPropsContextSize, paramProps.subList(0, groupPropsCnt + 1), paramProps.subList(groupPropsCnt + 1, paramProps.size()));

        List<Object> resultParams = getParamsPlainList(paramProps);
        List<Integer> usedParams = mergeAllParams(paramProps);
        LP prop;
        if (partitionType == PartitionType.SUM || partitionType == PartitionType.PREVIOUS) {
            prop = addOProp(null, false, LocalizedString.NONAME, partitionType, isAscending, ordersNotNull, useLast, groupPropsCnt, resultParams.toArray());
        } else if (partitionType == PartitionType.DISTR_CUM_PROPORTION) {
            List<ResolveClassSet> contextClasses = getClassesFromTypedParams(context);// для не script - временный хак
            // может быть внешний context
            List<ResolveClassSet> explicitInnerClasses = new ArrayList<>();
            for(int usedParam : usedParams)
                explicitInnerClasses.add(contextClasses.get(usedParam)); // one-based;
            prop = addPGProp(null, false, precision, strict, LocalizedString.NONAME, usedParams.size(), explicitInnerClasses, isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        } else {
            prop = addUGProp(null, false, strict, LocalizedString.NONAME, usedParams.size(), isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        }
        return new LPWithParams(prop, usedParams);
    }

    public LPWithParams addScriptedCCProp(List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addCCProp(params.size()), params);
    }

    public LPWithParams addScriptedConcatProp(String separator, List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addSFUProp(params.size(), separator), params);
    }

    public LPWithParams addScriptedDCCProp(LPWithParams ccProp, int index) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDeconcatenateIndex(ccProp, index);
        return addScriptedJProp(addDCCProp(index - 1), Collections.singletonList(ccProp));
    }

    public LCP addScriptedSFProp(String typeName, List<SQLSyntaxType> types, List<String> texts, boolean hasNotNull) throws ScriptingErrorLog.SemanticErrorException {
        assert types.size() == texts.size();
        checks.checkSingleImplementation(types);

        Set<Integer> params = findFormulaParameters(texts.get(0));
        
        for (String text : texts) {
            Set<Integer> formulaParams = findFormulaParameters(text);
            checks.checkFormulaParameters(formulaParams);
            if (formulaParams.size() != params.size()) {
                errLog.emitFormulaDifferentParamCountError(parser);
            }
        }
        
        String defaultFormula = "";
        MExclMap<SQLSyntaxType, String> mSyntaxes = MapFact.mExclMap();
        for (int i = 0; i < types.size(); i++) {
            SQLSyntaxType type = types.get(i);
            String text = transformFormulaText(texts.get(i), StringFormulaProperty.getParamName("$1"));
            if (type == null) {
                defaultFormula = text;
            } else {
                mSyntaxes.exclAdd(type, text);
            }
        }
        CustomFormulaSyntax formula = new CustomFormulaSyntax(defaultFormula, mSyntaxes.immutable());
        if (typeName != null) {
            ValueClass cls = findClass(typeName);
            checks.checkFormulaClass(cls);
            return addSFProp(formula, (DataClass) cls, params.size(), hasNotNull);
        } else {
            return addSFProp(formula, params.size(), hasNotNull);
        }
    }

    private Set<Integer> findFormulaParameters(String text) {
        Set<Integer> params = new HashSet<>();
        if(text != null) {
            Pattern pattern = Pattern.compile("\\$\\d+");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String group = matcher.group();
                int paramNumber = Integer.valueOf(group.substring(1));
                params.add(paramNumber);
            }
        }
        return params;
    }

    private String transformFormulaText(String text, String textTo) { // так как $i не постфиксный (например $1 и $12)
        return text.replaceAll("\\$(\\d+)", textTo);
    }

    public LPWithParams addScriptedRProp(List<TypedParameter> context, LPWithParams zeroStep, LPWithParams nextStep, Cycle cycleType) throws ScriptingErrorLog.SemanticErrorException {
        List<Integer> usedParams = mergeAllParams(asList(zeroStep, nextStep));
        checks.checkRecursionContext(getParamNamesFromTypedParams(context), usedParams);

        MOrderExclSet<Integer> mMainParams = SetFact.mOrderExclSetMax(usedParams.size());
        Map<Integer, Integer> usedToResult = new HashMap<>();
        for (int i = 0; i < usedParams.size(); i++) {
            if (!context.get(usedParams.get(i)).paramName.startsWith("$")) {
                mMainParams.exclAdd(i);
                usedToResult.put(usedParams.get(i), i);
            }
        }
        ImOrderSet<Integer> mainParams = mMainParams.immutableOrder();

        Map<Integer, Integer> mapPrev = new HashMap<>();
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
        LP res = addRProp(null, false, LocalizedString.NONAME, cycleType, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

        List<Integer> resUsedParams = new ArrayList<>();
        for (Integer usedParam : usedParams) {
            if (!context.get(usedParam).paramName.startsWith("$")) {
                resUsedParams.add(usedParam);
            }
        }
        return new LPWithParams(res, resUsedParams);
    }

    private static StringClass getStringConstClass(LocalizedString value) {
        if(value.needToBeLocalized())
            return StringClass.text;
        return StringClass.getv(new ExtInt(value.getSourceString().length()));
    }

    public LCP addConstantProp(ConstType type, Object value) throws ScriptingErrorLog.SemanticErrorException {
        switch (type) {
            case INT: return addUnsafeCProp(IntegerClass.instance, value);
            case LONG: return addUnsafeCProp(LongClass.instance, value);
            case NUMERIC: return addNumericConst((String) value);
            case REAL: return addUnsafeCProp(DoubleClass.instance, value);
            case STRING: return addUnsafeCProp(getStringConstClass((LocalizedString)value), value);
            case LOGICAL: return addUnsafeCProp(LogicalClass.instance, value);
            case DATE: return addUnsafeCProp(DateClass.instance, value);
            case DATETIME: return addUnsafeCProp(DateTimeClass.instance, value);
            case TIME: return addUnsafeCProp(TimeClass.instance, value);
            case STATIC: return addStaticClassConst((String) value);
            case COLOR: return addUnsafeCProp(ColorClass.instance, value);
            case NULL: return baseLM.vnull;
        }
        return null;
    }

    private LCP addNumericConst(String value) {
        return addUnsafeCProp(NumericClass.get(value.length(), value.length() - value.indexOf('.') - 1), new BigDecimal(value));
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
        checks.checkRange("year component", y, 1900, 9999);
        checks.checkRange("month component", m, 1, 12);
        checks.checkRange("day component", d, 1, 31);

        final List<Integer> longMonth = Arrays.asList(1, 3, 5, 7, 8, 10, 12);
        if (d == 31 && !longMonth.contains(m) ||
            d == 30 && m == 2 ||
            d == 29 && m == 2 && (y % 4 != 0 || y % 100 == 0 && y % 400 != 0))
        {
            errLog.emitDateDayError(parser, y, m, d);
        }

    }

    private void validateTime(int h, int m) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkRange("hour component", h, 0, 23);
        checks.checkRange("minute component", m, 0, 59);
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

    public <O extends ObjectSelector> LPWithParams addScriptedShowFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                              Boolean syncType, WindowFormType windowType, ManageSessionType manageSession, FormSessionScope formSessionScope, 
                                              boolean checkOnOk, Boolean noCancel, boolean readonly) throws ScriptingErrorLog.SemanticErrorException {
        List<O> objects = new ArrayList<>();
        List<LPWithParams> mapping = new ArrayList<>();
        List<Boolean> nulls = new ArrayList<>();

        List<O> allObjects = mapped.objects;
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            objects.add(object);
            mapping.add(objectProp.in);
            nulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        if(syncType == null)
            syncType = true;
        if(windowType == null) {
            if(syncType)
                windowType = WindowFormType.FLOAT;
            else
                windowType = WindowFormType.DOCKED;
        }
            
        List<LPWithParams> propParams = new ArrayList<>();
        List<Integer> allParams = mergeAllParams(propParams);

        LAP property = addIFAProp(null, LocalizedString.NONAME, mapped.form, objects, nulls,
                                 manageSession, noCancel,
                                 syncType, windowType, checkOnOk,
                                 readonly, getParamsPlainList(propParams).toArray());
        
        property = addSessionScopeAProp(formSessionScope, property);
        
        if (mapping.size() > 0) {
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LPWithParams(property, allParams);
        }
    }

    private LCP<?> getInputProp(PropertyUsage targetProp, ValueClass valueClass, Set<CalcProperty> usedProps) throws ScriptingErrorLog.SemanticErrorException {
        if(targetProp != null) {
            LCP<?> result = findLCPByPropertyUsage(targetProp);
            usedProps.add(result.property);
            return result;
        }

        if(valueClass instanceof DataClass) {
            LCP<?> requested = baseLM.getRequestedValueProperty().getLCP((DataClass)valueClass);
            if(usedProps == null || usedProps.add(requested.property))
                return requested;
        }
        // уже был или Object - генерим новое
        return new LCP<>(DerivedProperty.createInputDataProp(valueClass));
    }
    
    public <O extends ObjectSelector> LPWithParams addScriptedDialogFAProp(
                                                MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                                WindowFormType windowType, ManageSessionType manageSession, FormSessionScope scope,
                                                boolean checkOnOk, Boolean noCancel, boolean readonly, LPWithParams doAction, LPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {

        List<O> objects = new ArrayList<>();
        List<LPWithParams> mapping = new ArrayList<>();
        List<Boolean> nulls = new ArrayList<>();

        List<O> allObjects = mapped.objects;
        MList<O> mInputObjects = ListFact.mListMax(allObjects.size());
        MList<Boolean> mInputNulls = ListFact.mListMax(allObjects.size());
        MList<LCP> mInputProps = ListFact.mListMax(allObjects.size());
        
        MList<Pair<LPWithParams, DebugInfo.DebugPoint>> mAssignProps = ListFact.mListMax(allObjects.size());

        MList<O> mContextObjects = ListFact.mListMax(allObjects.size() + 1);
        MList<CalcProperty> mContextProps = ListFact.mListMax(allObjects.size() + 1);
        List<LPWithParams> contextLPs = new ArrayList<>();

        Set<CalcProperty> usedProps = new HashSet<>();

        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            if (objectProp.in != null) {
                objects.add(object);
                mapping.add(objectProp.in);
                nulls.add(objectProp.inNull);
            }
            if (objectProp.out) {
                mInputObjects.add(object);
                mInputNulls.add(objectProp.outNull);
                LCP<?> outProp = getInputProp(objectProp.outProp, mapped.form.getBaseClass(object), usedProps);
                mInputProps.add(outProp);

                LPWithParams changeProp = null;
                if(objectProp.constraintFilter || objectProp.assign) {
                    changeProp = objectProp.changeProp;
                    if(changeProp == null)
                        changeProp = objectProp.in;
                    assert changeProp != null;
                }
                if(objectProp.constraintFilter) {
                    mContextObjects.add(object);
                    mContextProps.add((CalcProperty)changeProp.property.property);
                    contextLPs.add(changeProp);
                }

                Pair<LPWithParams, DebugInfo.DebugPoint> assignProp = null;
                if(objectProp.assign)
                    assignProp = new Pair<>(changeProp, objectProp.assignDebugPoint);
                mAssignProps.add(assignProp);
            }
        }
        ImList<O> inputObjects = mInputObjects.immutableList();
        ImList<Boolean> inputNulls = mInputNulls.immutableList();
        ImList<LCP> inputProps = mInputProps.immutableList();

        ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> assignProps = mAssignProps.immutableList();

        ImList<O> contextObjects = mContextObjects.immutableList();
        ImList<CalcProperty> contextProps = mContextProps.immutableList();

        if(windowType == null) {
            if (!inputObjects.isEmpty())
                windowType = WindowFormType.DIALOG;
            else 
                windowType = WindowFormType.FLOAT;
        }

        List<LPWithParams> propParams = new ArrayList<>();
        for(LPWithParams contextLP : contextLPs) {
            propParams.add(contextLP);
            checks.checkCalculationProperty(contextLP.property);
        }
        List<Integer> allParams = mergeAllParams(propParams);

        LAP property = addIFAProp(null, LocalizedString.NONAME, mapped.form, objects, nulls,
                                 inputObjects, inputProps, inputNulls,
                                 manageSession, noCancel,
                                 contextObjects, contextProps,
                true, windowType, checkOnOk,
                readonly, getParamsPlainList(propParams).toArray());
        
        property = addSessionScopeAProp(scope, property, inputProps.addList(baseLM.getRequestCanceledProperty()).getCol());

        LPWithParams formAction;
        if (mapping.size() > 0) {
            for(LPWithParams contextLP : contextLPs)
                for (int usedParam : contextLP.usedParams) {
                    mapping.add(new LPWithParams(null, singletonList(usedParam)));
                }
            formAction = addScriptedJoinAProp(property, mapping);
        } else {
            formAction = new LPWithParams(property, allParams);
        }

        return proceedDoClause(doAction, elseAction, oldContext, newContext, inputProps, formAction, assignProps);
    }

    private LPWithParams proceedDoClause(LPWithParams doAction, LPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, ImList<LCP> inputParamProps, LPWithParams inputAction, ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> assignProps) throws ScriptingErrorLog.SemanticErrorException {
        assert newContext.size() - oldContext.size() == inputParamProps.size();
        assert inputParamProps.size() == assignProps.size();
        if (doAction != null) {
            doAction = extendDoParams(doAction, newContext, oldContext.size(), inputParamProps, assignProps);
            return addScriptedRequestAProp(inputAction, doAction, elseAction);
        } else {
            return inputAction;
        }
    }

//    private int findOldParam(List<TypedParameter> params, ImList<Integer> inputParams, Result<ImList<LCP>> rInputParamProps) throws ScriptingErrorLog.SemanticErrorException {
//        ImOrderSet<Integer> paramsSet = inputParams.toOrderExclSet();
//        MList<LCP> mInputParamProps = ListFact.mList(inputParams.size());
//        int paramOld = params.size() - inputParams.size();
//        for(int i = params.size()-1; i >= paramOld; i--) {
//            int paramIndex = paramsSet.indexOf(i);
//            if(paramIndex < 0) 
//                errLog.emitExtendParamUsage(parser, params.get(i).paramName);
//            
//            mInputParamProps.add(rInputParamProps.result.get(paramIndex));            
//        }
//        rInputParamProps.set(mInputParamProps.immutableList().reverseList());
//        return paramOld;
//    }
    
    private LPWithParams nullExec(LPWithParams doAction, int param) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> params = new ArrayList<>();
        boolean found = false;
        for(int usedParam : doAction.usedParams) 
            if(usedParam == param){
                found = true;
                params.add(new LPWithParams(baseLM.vnull, new ArrayList<Integer>()));
            } else
                params.add(new LPWithParams(null, Collections.singletonList(usedParam)));
            
        if(!found) // не было использований
            return null;
        return addScriptedJoinAProp(doAction.property, params);
    }
    
    // recursive
    private LPWithParams extendDoParams(LPWithParams doAction, List<TypedParameter> context, int paramOld, ImList<LCP> resultProps, ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> assignProps) throws ScriptingErrorLog.SemanticErrorException {
        
        List<TypedParameter> currentContext = new ArrayList<>(context);
        int paramNum;
        while((paramNum = currentContext.size() - 1) >= paramOld) {
            // remove'им параметр
            List<TypedParameter> removedContext = new ArrayList<>(currentContext);
            removedContext.remove(paramNum);

            LPWithParams paramLP = new LPWithParams(null, Collections.singletonList(paramNum));
            Pair<LPWithParams, DebugInfo.DebugPoint> assignLP = assignProps.get(paramNum - paramOld);
            if(assignLP != null) {
                LPWithParams assignAction = addScriptedAssignAProp(currentContext, paramLP, null, assignLP.first);
                
                ScriptingLogicsModule.setDebugInfo(null, assignLP.second, ((LAP<?>)assignAction.property).property);
                
                doAction = addScriptedListAProp(BaseUtils.toList(assignAction, doAction), new ArrayList<LP>());
            }

            LPWithParams nullExec = nullExec(doAction, paramNum); // передает NULL в качестве параметра
            if(nullExec != null) { // нет параметра нет проблемы
                modifyContextFlowActionPropertyDefinitionBodyCreated(doAction, currentContext, removedContext, false);

                LPWithParams resultLP = new LPWithParams(resultProps.get(paramNum - paramOld), new ArrayList<Integer>());

                doAction = addScriptedForAProp(removedContext, addScriptedEqualityProp("==", paramLP, resultLP), new ArrayList<LPWithParams>(), doAction,
                        nullExec, null, null, false, false, false, null, false);
            }

            currentContext = removedContext;
        }
        
        return doAction;
    }

    public <O extends ObjectSelector> LPWithParams addScriptedPrintFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                           LPWithParams printerProperty, FormPrintType printType, PropertyUsage propUsage,
                                               Boolean syncType, Integer selectTop) throws ScriptingErrorLog.SemanticErrorException {
        List<O> objects = new ArrayList<>();
        List<LPWithParams> mapping = new ArrayList<>();
        List<Boolean> nulls = new ArrayList<>();

        List<O> allObjects = mapped.objects;
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            objects.add(object);
            mapping.add(objectProp.in);
            nulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }
        
        if(syncType == null)
            syncType = false;

        List<LPWithParams> propParams = new ArrayList<>();
        if(printerProperty != null) {
            propParams.add(printerProperty);
            checks.checkCalculationProperty(printerProperty.property);
        }
        List<Integer> allParams = mergeAllParams(propParams);

        LCP<?> targetProp = null;
        if(propUsage != null)
            targetProp = findLCPByPropertyUsage(propUsage);

        LAP property = addPFAProp(null, LocalizedString.NONAME, mapped.form, objects, nulls,
                printerProperty != null, printType, syncType, selectTop, targetProp, false, getParamsPlainList(propParams).toArray());

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LPWithParams(property, allParams);
        }
    }

    public <O extends ObjectSelector> LPWithParams addScriptedExportFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                               FormExportType exportType, boolean noHeader, String separator, String charset, PropertyUsage propUsage) throws ScriptingErrorLog.SemanticErrorException {
        List<O> objects = new ArrayList<>();
        List<LPWithParams> mapping = new ArrayList<>();
        List<Boolean> nulls = new ArrayList<>();

        if(exportType == null)
            exportType = FormExportType.XML;

        List<O> allObjects = mapped.objects;
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            objects.add(object);
            mapping.add(objectProp.in);
            nulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }


        List<LPWithParams> propParams = new ArrayList<>();
        List<Integer> allParams = mergeAllParams(propParams);

        LCP<?> targetProp = null;
        if(propUsage != null)
            targetProp = findLCPByPropertyUsage(propUsage);

        LAP property = addEFAProp(null, LocalizedString.NONAME, mapped.form, objects, nulls,
                exportType, noHeader, separator, charset, targetProp, getParamsPlainList(propParams).toArray());

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LPWithParams(property, allParams);
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
        checks.checkDuplicateMetaCodeFragment(name, params.size(), BL);
        checks.checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(elementCanonicalName(name), params, tokens, code, getName(), lineNumber);
        addMetaCodeFragment(fragment);
    }

    public void runMetaCode(String name, List<String> params, int lineNumber, boolean enabledMeta) throws RecognitionException {
        MetaCodeFragment metaCode = findMetaCodeFragment(name, params.size());
        checks.checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params);
        parser.runMetaCode(this, code, metaCode, MetaCodeFragment.metaCodeCallString(name, metaCode, params), lineNumber, enabledMeta); 
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        return parser.grabMetaCode(metaCodeName);
    }

    public List<String> grabJavaCode() throws ScriptingErrorLog.SemanticErrorException {
        return parser.grabJavaCode();
    }

    private LCP addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos + 1);
        LCP resultProp = null;

        ValueClass cls = findClass(className);
        if (cls instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteClass = (ConcreteCustomClass) cls;
            if (concreteClass.hasStaticObject(instanceName)) { //, versionб так как отдельным шагом парсится
                resultProp = addCProp(concreteClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static оbject", instanceName);
            }
        } else {
            errLog.emitAbstractClassInstancesUseError(parser, className, instanceName);
        }
        return resultProp;
    }

    public LCP addScriptedGroupObjectProp(String name, GroupObjectProp prop, List<ResolveClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);
        LCP resultProp = null;

        FormEntity form = findForm(formName);

        GroupObjectEntity groupObject = form.getNFGroupObject(objectName, getVersion());
        if (groupObject != null) {
            for (ObjectEntity obj : groupObject.getOrderObjects()) {
                outClasses.add(obj.getResolveClassSet());
            }
            resultProp = addGroupObjectProp(groupObject, prop);
        } else {
            errLog.emitNotFoundError(parser, "group оbject", objectName);
        }
        return resultProp;
    }


    public LCP addScriptedReflectionProperty(ReflectionPropertyType type, PropertyUsage propertyUsage, List<ResolveClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        switch (type) {
            case CANONICAL_NAME:
            default: return addCanonicalNameProp(propertyUsage);
        }
    }

    public LCP addCanonicalNameProp(PropertyUsage propertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        return new LCP<>(new CanonicalNameProperty(findLPByPropertyUsage(propertyUsage)));
    }

    public LPWithParams addScriptedFocusActionProp(PropertyDrawEntity property) {
        return new LPWithParams(addFocusActionProp(property), new ArrayList<Integer>());
    }
    
    public LPWithParams addScriptedReadActionProperty(LPWithParams sourcePathProp, PropertyUsage propUsage, LPWithParams movePathProp, boolean delete) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass sourceProp = sourcePathProp.property.property.getValueClass(ClassType.valuePolicy);
        LCP<?> targetProp = findLCPByPropertyUsage(propUsage);
        ValueClass moveProp = movePathProp == null ? null : movePathProp.property.property.getValueClass(ClassType.valuePolicy);
        return addScriptedJoinAProp(addAProp(new ReadActionProperty(sourceProp, targetProp, moveProp, delete)),
                movePathProp == null ? Collections.singletonList(sourcePathProp) : Lists.newArrayList(sourcePathProp, movePathProp));
    }

    public LPWithParams addScriptedWriteActionProperty(LPWithParams sourcePathProp, LPWithParams sourceProp) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkCalculationProperty(sourceProp.property);
        return addScriptedJoinAProp(addAProp(new WriteActionProperty(sourceProp.property.property.getType(),
                sourcePathProp.property.property.getValueClass(ClassType.valuePolicy),
                sourceProp.property.property.getValueClass(ClassType.valuePolicy))), Arrays.asList(sourcePathProp, sourceProp));
    }

    public LPWithParams addScriptedImportDBFActionProperty(LPWithParams fileProp, LPWithParams whereProp, LPWithParams memoProp, List<String> ids, List<PropertyUsage> propUsages, String charset) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = findLPsForImport(propUsages);
        List<LPWithParams> params = new ArrayList<>();
        params.add(fileProp);
        if(whereProp != null)
            params.add(whereProp);
        if(memoProp != null)
            params.add(memoProp);
        int paramsCount = 1 + (whereProp != null ? 1 : 0) + (memoProp != null ? 1 : 0);
        return addScriptedJoinAProp(addAProp(ImportDataActionProperty.createDBFProperty(
                paramsCount, whereProp != null, memoProp != null,
                ids, props, charset, baseLM)), params);
    }

    public LPWithParams addScriptedImportActionProperty(ImportSourceFormat format, LPWithParams fileProp, List<String> ids, List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = findLPsForImport(propUsages);
        return addScriptedJoinAProp(addAProp(ImportDataActionProperty.createProperty(/*fileProp.property.property.getValueClass(ClassType.valuePolicy), */format, ids, props, baseLM)), Collections.singletonList(fileProp));
    }

    public LPWithParams addScriptedExportActionProperty(List<TypedParameter> oldContext, FormExportType type, final List<String> ids, List<LPWithParams> exprs, LPWithParams whereProperty,
                                                        PropertyUsage fileProp, String separator, boolean noHeader, String charset) throws ScriptingErrorLog.SemanticErrorException {
        
        LCP<?> targetProp = fileProp != null ? findLCPByPropertyUsage(fileProp) : BL.LM.formExportFile;

        List<LPWithParams> props = exprs;
        if(whereProperty != null)
            props = BaseUtils.add(exprs, whereProperty);

        List<Integer> resultInterfaces = getResultInterfaces(oldContext.size(), props.toArray(new LPWithParams[exprs.size()+1]));

        if(type == null)
            type = doesExtendContext(oldContext.size(), new ArrayList<LPWithParams>(), props) ? FormExportType.XML : FormExportType.LIST;

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, Collections.singletonList(resI)));
        }
        paramsList.addAll(exprs);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }

        ImOrderSet<String> idSet = SetFact.toOrderExclSet(ids.size(), new GetIndex<String>() {
            public String getMapValue(int i) {
                String id = ids.get(i);
                return id == null ? "expr" + i : id;
            }
        });

        List<Object> resultParams = getParamsPlainList(paramsList);
        LP result = addExportPropertyAProp(LocalizedString.NONAME, type, resultInterfaces.size(), idSet, targetProp, whereProperty != null, separator, noHeader, charset, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public static List<String> getUsedNames(List<TypedParameter> context, List<Integer> usedParams) {
        List<String> usedNames = new ArrayList<>();
        for (int usedIndex : usedParams) {
            usedNames.add(context.get(usedIndex).paramName);
        }
        return usedNames;
    }

    public static List<ResolveClassSet> getUsedClasses(List<TypedParameter> context, List<Integer> usedParams) {
        List<ResolveClassSet> usedClasses = new ArrayList<>();
        for (int usedIndex : usedParams) {
            ValueClass cls = context.get(usedIndex).cls;
            if(cls == null)
                usedClasses.add(null);
            else
                usedClasses.add(cls.getResolveSet());
        }
        return usedClasses;
    }

    public LPWithParams addScriptedNewThreadActionProperty(LPWithParams actionProp, LPWithParams connectionProp, LPWithParams periodProp, LPWithParams delayProp) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> propParams = toList(actionProp);
        if (periodProp != null) {
            propParams.add(periodProp);
        }
        if (delayProp != null) {
            propParams.add(delayProp);
        }
        if (connectionProp != null) {
            propParams.add(connectionProp);
        }
        List<Integer> allParams = mergeAllParams(propParams);
        LAP<?> property = addNewThreadAProp(null, LocalizedString.NONAME, connectionProp != null, periodProp != null, delayProp != null, getParamsPlainList(propParams).toArray());
        return new LPWithParams(property, allParams);
    }

    public LPWithParams addScriptedNewExecutorActionProperty(LPWithParams actionProp, LPWithParams threadsProp) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> propParams = toList(actionProp, threadsProp);
        List<Integer> allParams = mergeAllParams(propParams);
        LAP<?> property = addNewExecutorAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LPWithParams(property, allParams);
    }

    private List<LCP> findLPsForImport(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = new ArrayList<>();
        for (PropertyUsage propUsage : propUsages) {
            if (propUsage.classNames == null) {
                propUsage.classNames = Collections.singletonList("INTEGER"); // делаем так для лучшего сообщения об ошибке 
            } 
            LCP<?> lcp = findLCPByPropertyUsage(propUsage);
            ValueClass[] paramClasses = lcp.getInterfaceClasses(ClassType.signaturePolicy);
            if (paramClasses.length != 1 || paramClasses[0].getType() != ImportDataActionProperty.type) {
                errLog.emitPropertyWithParamsExpected(getParser(), propUsage.name, ImportDataActionProperty.type.getParsedName());
            }
            props.add(lcp);
        }
        return props;
    }

    public LPWithParams addScriptedImportExcelActionProperty(LPWithParams fileProp, List<String> ids, List<PropertyUsage> propUsages, LPWithParams sheetIndex) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = findLPsForImport(propUsages);
        return addScriptedJoinAProp(addAProp(new ImportXLSDataActionProperty(sheetIndex != null ? 2 : 1, ids, props, baseLM)), sheetIndex == null ? Collections.singletonList(fileProp) : Lists.newArrayList(fileProp, sheetIndex));
    }

    public LPWithParams addScriptedImportCSVActionProperty(LPWithParams fileProp, List<String> ids, List<PropertyUsage> propUsages, String separator, boolean noHeader, String charset) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = findLPsForImport(propUsages);
        return addScriptedJoinAProp(addAProp(new ImportCSVDataActionProperty(ids, props, separator, noHeader, charset, baseLM)), Collections.singletonList(fileProp));
    }

    public LPWithParams addScriptedImportXMLActionProperty(LPWithParams fileProp, List<String> ids, List<PropertyUsage> propUsages, LPWithParams rootProp, boolean attr) throws ScriptingErrorLog.SemanticErrorException {
        List<LCP> props = findLPsForImport(propUsages);
        List<LPWithParams> params = new ArrayList<>();
        params.add(fileProp);
        if(rootProp != null)
            params.add(rootProp);
        return addScriptedJoinAProp(addAProp(new ImportXMLDataActionProperty(params.size(), ids, props, attr, baseLM)), params);
    }

    public LPWithParams addScriptedImportFormCSVActionProperty(FormEntity formEntity, boolean noHeader, String charset, String separator) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ImportFormCSVDataActionProperty(formEntity, noHeader, charset, separator)), Collections.<LPWithParams>emptyList());
    }

    public LPWithParams addScriptedImportFormDBFActionProperty(FormEntity formEntity, String charset) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ImportFormDBFDataActionProperty(formEntity, charset)), Collections.<LPWithParams>emptyList());
    }

    public LPWithParams addScriptedImportFormXMLActionProperty(FormEntity formEntity, boolean attr) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ImportFormXMLDataActionProperty(formEntity, attr)), Collections.<LPWithParams>emptyList());
    }

    public LPWithParams addScriptedImportFormJSONActionProperty(FormEntity formEntity) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ImportFormJSONDataActionProperty(formEntity)), Collections.<LPWithParams>emptyList());
    }

    public LCP addScriptedTypeProp(String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        if (bIs) {
            return is(findClass(className));
        } else {
            return object(findClass(className));
        }
    }

    public LP addScriptedTypeExprProp(LP mainProp, LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(mainProp, Collections.singletonList(property)).property;
    }

    public void addScriptedConstraint(LP property, Event event, boolean checked, List<PropertyUsage> propUsages, LP messageProperty, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (!((LCP<?>)property).property.checkAlwaysNull(true)) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        ImSet<CalcProperty<?>> checkedProps = null;
        CalcProperty.CheckType type = (checked ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO);
        if (checked && propUsages != null) {
            MSet<CalcProperty<?>> mCheckedProps = SetFact.mSet();
            for (PropertyUsage propUsage : propUsages) {
                LCP<?> lcp = findLCPByPropertyUsage(propUsage);
                mCheckedProps.add(lcp.property);
            }
            type = CalcProperty.CheckType.CHECK_SOME;
            checkedProps = mCheckedProps.immutable();
        }
        addConstraint((LCP<?>) property, (LCP<?>) messageProperty, type, checkedProps, event, this, debugPoint);
    }

    private PrevScope prevScope = null;
    public void setPrevScope(Event event) {
        setPrevScope(event.getScope());
    }

    public void dropPrevScope(Event event) {
        dropPrevScope(event.getScope());
    }

    // по сути оптимизация - когда контекст глобального события использовать в операторах изменений PrevScope.DB
    public void setPrevScope(PrevScope scope) {
        assert prevScope == null;
        prevScope = scope;
    }

    public void dropPrevScope(PrevScope scope) {
        assert prevScope.equals(scope);
        prevScope = null;
    }

    public LPWithParams addScriptedSessionProp(IncrementType type, LPWithParams property) {
        LCP newProp;
        PrevScope scope = (type == null ? PrevScope.DB : (prevScope != null ? prevScope : PrevScope.EVENT)); // по сути оптимизация если scope известен использовать его
        if (type == null) {
            newProp = addOldProp((LCP) property.property, scope);
        } else {
            newProp = addCHProp((LCP) property.property, type, scope);
        }
        return new LPWithParams(newProp, property.usedParams);
    }

    public LPWithParams addScriptedSignatureProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkCalculationProperty(property.property);
        LCP newProp = addClassProp((LCP) property.property);
        return new LPWithParams(newProp, property.usedParams);
    }

    public LPWithParams addScriptedActiveTabProp(ComponentView component) throws ScriptingErrorLog.SemanticErrorException {
        return new LPWithParams(new LCP<>(component.getActiveTab().property), new ArrayList<Integer>());
    }

    public void addScriptedFollows(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, List<PropertyFollowsDebug> resolveOptions, LPWithParams rightProp, Event event, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        LCP mainProp = (LCP) findJoinMainProp(mainPropUsage, namedParams);
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        Integer[] params = new Integer[rightProp.usedParams.size()];
        for (int j = 0; j < params.length; j++) {
            params[j] = rightProp.usedParams.get(j) + 1;
        }
        follows(mainProp, debugPoint, ListFact.fromJavaList(resolveOptions), event, (LCP) rightProp.property, params);
    }

    public void addScriptedWriteWhen(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, LPWithParams valueProp, LPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findJoinMainProp(mainPropUsage, namedParams);
        if (!(mainProp.property instanceof DataProperty)) {
            errLog.emitOnlyDataPropertyIsAllowedError(parser, mainPropUsage.name);
        }
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        ((LCP)mainProp).setEventChange(this, action, params.toArray());
    }

    public Set<CalcProperty> findPropsByPropertyUsages(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages==null)
            return null;

        Set<CalcProperty> props = new HashSet<>(); // функционально из-за exception'а не сделаешь
        for (PropertyUsage usage : propUsages) {
            LCP<?> lp = findLCPByPropertyUsage(usage);
            props.add(lp.property); 
        }
        return props;
    }

    public void addScriptedEvent(LPWithParams whenProp, LPWithParams event, List<LPWithParams> orders, boolean descending, Event baseEvent, List<LPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkActionProperty(event.property);
        if(noInline==null) {
            noInline = new ArrayList<>();
            for(Integer usedParam : whenProp.usedParams)
                noInline.add(new LPWithParams(null, Collections.singletonList(usedParam)));
        }
        List<Object> params = getParamsPlainList(asList(event, whenProp), orders, noInline);
        addEventAction(baseEvent, descending, false, noInline.size(), forceInline, debugPoint, params.toArray());
    }

    public void addScriptedGlobalEvent(LPWithParams event, Event baseEvent, boolean single, PropertyUsage showDep) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkActionProperty(event.property);
        checks.checkEventNoParameters(event.property);
        ActionProperty action = (ActionProperty) event.property.property;
        if(showDep!=null)
            action.showDep = findLPByPropertyUsage(showDep).property;
        addBaseEvent(action, baseEvent, false, single);
    }

    public void addScriptedShowDep(PropertyUsage property, PropertyUsage propFrom) throws ScriptingErrorLog.SemanticErrorException {
        findLPByPropertyUsage(property).property.showDep = findLPByPropertyUsage(propFrom).property;
    }

    public void addScriptedAspect(PropertyUsage mainPropUsage, List<TypedParameter> mainPropParams, LPWithParams actionProp, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findJoinMainProp(mainPropUsage, mainPropParams);
        checks.checkParamCount(mainProp, mainPropParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(mainPropParams));
        checks.checkActionProperty(actionProp.property);
        checks.checkActionProperty(mainProp);

        LAP<PropertyInterface> mainActionLP = (LAP<PropertyInterface>) mainProp;

        List<Object> params = getParamsPlainList(Collections.singletonList(actionProp));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(mainActionLP.listInterfaces, params.toArray());
        addAspectEvent(mainActionLP.property, actionImplements.get(0), before);
    }

    public void addScriptedTable(String name, List<String> classIds, boolean isFull) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateTable(name, BL);

        ValueClass[] classes = new ValueClass[classIds.size()];
        for (int i = 0; i < classIds.size(); i++) {
            classes[i] = findClass(classIds.get(i));
        }
        addTable(name, isFull, classes);
    }

    public List<LCP> indexedProperties = new ArrayList<>();
    
    public void addScriptedIndex(LP property) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkCalculationProperty(property);
        indexedProperties.add((LCP) property);        
    }

    public LPWithParams findIndexProp(PropertyUsage toPropertyUsage, List<LPWithParams> toPropertyMapping, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        LP toPropertyLP = findJoinMainProp(toPropertyUsage, toPropertyMapping, context);
        return new LPWithParams(toPropertyLP, getParamsAssertList(toPropertyMapping));
    }
    
    public void addScriptedIndex(List<TypedParameter> params, List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkIndexNecessaryProperty(lps);
        checks.checkStoredProperties(lps);
        checks.checkDistinctParametersList(lps);
        checks.checkIndexNumberOfParameters(params.size(), lps);
        ImOrderSet<String> keyNames = ListFact.fromJavaList(params).toOrderExclSet().mapOrderSetValues(new GetValue<String, TypedParameter>() {
            public String getMapValue(TypedParameter value) {
                return value.paramName;
            }});
        addIndex(keyNames, getParamsPlainList(lps).toArray());
    }

    public void addScriptedLoggable(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        for (PropertyUsage propUsage : propUsages) {
            LCP lp = findLCPByPropertyUsage(propUsage);
            lp.makeLoggable(this, BL.systemEventsLM);
        }
    }

    public void addScriptedWindow(WindowType type, String name, LocalizedString captionStr, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateWindow(name, BL);

        LocalizedString caption = (captionStr == null ? LocalizedString.create(name) : captionStr);
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
                window = createTreeWindow(name, caption, options);
                break;
        }

        window.drawRoot = nvl(options.getDrawRoot(), false);
        window.drawScrollBars = nvl(options.getDrawScrollBars(), true);
        window.titleShown = nvl(options.getDrawTitle(), true);

        addWindow(window);
    }

    private MenuNavigatorWindow createMenuWindow(String name, LocalizedString caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        DockPosition dp = options.getDockPosition();
        if (dp == null) {
            errLog.emitWindowPositionNotSpecified(parser, name);
        } 
        assert dp != null;
        MenuNavigatorWindow window = new MenuNavigatorWindow(elementCanonicalName(name), caption, dp.x, dp.y, dp.width, dp.height);
        window.orientation = orientation.asMenuOrientation();

        return window;
    }

    private PanelNavigatorWindow createPanelWindow(String name, LocalizedString caption, NavigatorWindowOptions options) {
        Orientation orientation = options.getOrientation();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            orientation = Orientation.VERTICAL;
        }

        PanelNavigatorWindow window = new PanelNavigatorWindow(elementCanonicalName(name), caption, orientation.asToolbarOrientation());
        if (dockPosition != null) {
            window.setDockPosition(dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        }
        return window;
    }

    private ToolBarNavigatorWindow createToolbarWindow(String name, LocalizedString caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        BorderPosition borderPosition = options.getBorderPosition();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            orientation = Orientation.VERTICAL;
        }

        if (borderPosition != null && dockPosition != null) {
            errLog.emitWindowPositionConflict(parser, name);
        }

        ToolBarNavigatorWindow window;
        if (borderPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption, borderPosition.asLayoutConstraint());
        } else if (dockPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption, dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        } else {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption);
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

    private TreeNavigatorWindow createTreeWindow(String name, LocalizedString caption, NavigatorWindowOptions options) {
        TreeNavigatorWindow window = new TreeNavigatorWindow(elementCanonicalName(name), caption);
        DockPosition dp = options.getDockPosition();
        if (dp != null) {
            window.setDockPosition(dp.x, dp.y, dp.width, dp.height);
        }
        return window;
    }


    public void hideWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        findWindow(name).visible = false;
    }

    public static class NavigatorElementOptions {
        public String imagePath;
        public NavigatorElement anchor;
        public InsertPosition position;
        public String windowName;
    }

    public NavigatorElement createScriptedNavigatorElement(String name, LocalizedString caption, DebugInfo.DebugPoint point,
                                                           PropertyUsage actionUsage, String formName) throws ScriptingErrorLog.SemanticErrorException {
        LAP<?> action = null;
        FormEntity form = null;
        if (actionUsage != null) {
            action = findNavigatorAction(actionUsage);
        } else if (formName != null) {
            form = findForm(formName);
        }
        
        if (name == null) {
            name = createDefaultNavigatorElementName(action, form);
        }

        checks.checkNavigatorElementName(name);
        checks.checkDuplicateNavigatorElement(name, BL);
        
        if (caption == null) {
            caption = createDefaultNavigatorElementCaption(action, form);
            if (caption == null) {
                caption = LocalizedString.create(name);
            }
        }

        return createNavigatorElement(elementCanonicalName(name), caption, point, action, form);
    }
    
    private String createDefaultNavigatorElementName(LAP<?> action, FormEntity form) {
        if (action != null) {
            return action.property.getName();
        } else if (form != null) {
            String cn = form.getCanonicalName();
            return ElementCanonicalNameUtils.getName(cn); 
        }
        return null;
    }

    private LocalizedString createDefaultNavigatorElementCaption(LAP<?> action, FormEntity form) {
        if (action != null) {
            return action.property.caption;
        } else if (form != null) {
            return form.getCaption();
        }
        return null;
    }

    private NavigatorElement createNavigatorElement(String canonicalName, LocalizedString caption, DebugInfo.DebugPoint point, LAP<?> action, FormEntity form) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement newElement;
        if (form != null) {
            newElement = addNavigatorForm(form, canonicalName, caption);
        } else if (action != null) {
            newElement = addNavigatorAction(action, canonicalName, caption);
        } else {
            newElement = addNavigatorFolder(canonicalName, caption);
        }
        newElement.setCreationPath(point.toString());
        return newElement;   
    }
    
    private LAP<?> findNavigatorAction(PropertyUsage actionUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert actionUsage != null;
        if (actionUsage.classNames == null) {
            actionUsage.classNames = Collections.emptyList(); // делаем так для лучшего сообщения об ошибке
        }
        LAP<?> action = findLAPByPropertyUsage(actionUsage);
        checks.checkNavigatorAction(action);
        return action;
    }
    
    public NavigatorElement findOrCreateNavigatorElement(String name, DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {
        try {
            NavigatorElement ne = findNavigatorElement(name);
            if (ne instanceof NavigatorForm) {
                try {
                    FormEntity form = findForm(name);
                    if (!form.getCanonicalName().equals(((NavigatorForm)ne).getForm().getCanonicalName())) {
                        return createScriptedNavigatorElement(null, null, point, null, name);
                    }
                } catch (ScriptingErrorLog.SemanticErrorException e) {}
            }
            return ne;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return createScriptedNavigatorElement(null, null, point, null, name);
        }
    }
    
    public void setupNavigatorElement(NavigatorElement element, LocalizedString caption, NavigatorElement parentElement, NavigatorElementOptions options, boolean adding) throws ScriptingErrorLog.SemanticErrorException {
        if (caption != null) {
            element.caption = caption;
        }

        applyNavigatorElementOptions(element, parentElement, options, adding);
    }
    
    public void applyNavigatorElementOptions(NavigatorElement element, NavigatorElement parent, NavigatorElementOptions options, boolean adding) throws ScriptingErrorLog.SemanticErrorException {
        setNavigatorElementWindow(element, options.windowName);
        setNavigatorElementImage(element, parent, options.imagePath);
        
        if (parent != null && (adding || options.position != InsertPosition.IN)) {
            moveElement(element, parent, options.position, options.anchor, adding);
        }
    } 

    private void moveElement(NavigatorElement element, NavigatorElement parentElement, InsertPosition pos, NavigatorElement anchorElement, boolean adding) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();
        
        // если редактирование существующего элемента, и происходит перемещение элемента, то оно должно происходить только внутри своего уровня 
        if (!adding && !parentElement.equals(element.getNFParent(version))) {
            errLog.emitIllegalNavigatorElementMove(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }
        
        if (anchorElement != null && !parentElement.equals(anchorElement.getNFParent(version))) {
            errLog.emitIllegalInsertBeforeAfterElement(parser, element.getCanonicalName(), parentElement.getCanonicalName(), anchorElement.getCanonicalName());
        }

        if (element.isAncestorOf(parentElement, version)) {
            errLog.emitIllegalAddNavigatorToSubnavigator(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }

//        if (element.getNFParent(version) != null) {
//            System.out.println(String.format("MOVE element [%s] from [%s] to [%s] in module [%s]", element.getCanonicalName(), 
//                                            element.getNFParent(version).getCanonicalName(), parentElement, 
//                                            getParser().getGlobalDebugPoint(getName(), false)));
//        }
        
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
        assert element != null;
        
        if (windowName != null) {
            AbstractWindow window = findWindow(windowName);

            if (window instanceof NavigatorWindow) {
                element.window = (NavigatorWindow) window;
            } else {
                errLog.emitAddToSystemWindowError(parser, windowName);
            }
        }
    }

    public void setNavigatorElementImage(NavigatorElement element, NavigatorElement parent, String imagePath) throws ScriptingErrorLog.SemanticErrorException {
        if (imagePath != null) {
            element.setImage(imagePath);
        } else if (element.defaultIcon != null) {
            if (baseLM.root != null && parent != null && baseLM.root.equals(parent)) {
                element.setImage(element.defaultIcon == DefaultIcon.ACTION ? "/images/actionTop.png" :
                        element.defaultIcon == DefaultIcon.OPEN ? "/images/openTop.png" : "/images/formTop.png");
            }
            element.defaultIcon = null;
        }
    }
    
    public void propertyDefinitionCreated(LP property, DebugInfo.DebugPoint point) {
        if (property != null && property.property instanceof CalcProperty) {
            CalcProperty calcProp = (CalcProperty)property.property; 
            boolean needToCreateDelegate = debugger.isEnabled() && point.needToCreateDelegate() && calcProp instanceof DataProperty;
            if (calcProp.getDebugInfo() == null) { // при использовании в propertyExpression оптимизированных join свойств, не нужно им переустанавливать DebugInfo
                CalcPropertyDebugInfo debugInfo = new CalcPropertyDebugInfo(point, needToCreateDelegate);
                if (needToCreateDelegate) {
                    debugger.addDelegate(debugInfo);
                }
                calcProp.setDebugInfo(debugInfo);
            }
        }
    }

    public void actionPropertyDefinitionBodyCreated(LPWithParams lpWithParams, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate) throws ScriptingErrorLog.SemanticErrorException {
        if (lpWithParams.property != null) {
            checks.checkActionProperty(lpWithParams.property);
            setDebugInfo(lpWithParams, startPoint, endPoint, modifyContext, needToCreateDelegate);
        }
    }

    public static void setDebugInfo(LPWithParams lpWithParams, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate) {
        //noinspection unchecked
        LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.property;
        ActionProperty property = lAction.property;
        setDebugInfo(needToCreateDelegate, startPoint, endPoint, modifyContext, property);
    }

    public static void setDebugInfo(Boolean needToCreateDelegate, DebugInfo.DebugPoint point, ActionProperty property) {
        setDebugInfo(needToCreateDelegate, point, point, false, property);        
    }

    private static void setDebugInfo(Boolean needToCreateDelegate, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, ActionProperty property) {
        ActionDelegationType delegationType = property.getDelegationType(modifyContext);

        if(needToCreateDelegate == null)
            needToCreateDelegate = startPoint.needToCreateDelegate();

        if (debugger.isEnabled() && needToCreateDelegate && delegationType != null) {
            DebugInfo.DebugPoint typePoint = delegationType.getDebugPoint(startPoint, endPoint);
            ActionDebugInfo info = new ActionDebugInfo(startPoint, typePoint.line, typePoint.offset, delegationType);
            debugger.addDelegate(info);
            property.setDebugInfo(info);
        } else {
            property.setDebugInfo(new ActionDebugInfo(startPoint, delegationType, false));
        }
    }

    public void topContextActionPropertyDefinitionBodyCreated(LPWithParams lpWithParams) throws ScriptingErrorLog.SemanticErrorException {
        boolean isDebug = debugger.isEnabled();

        if(isDebug) {
            //noinspection unchecked
            LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.property;

            ActionProperty property = lAction.property;

            debugger.setNewDebugStack(property);
        }
    }

    public LPWithParams modifyContextFlowActionPropertyDefinitionBodyCreated(LPWithParams lpWithParams,
                                                                             List<TypedParameter> newContext, List<TypedParameter> oldContext,
                                                                             boolean needFullContext) throws ScriptingErrorLog.SemanticErrorException {
        boolean isDebug = debugger.isEnabled();
        
        if(isDebug || needFullContext) {
            lpWithParams = patchExtendParams(lpWithParams, newContext, oldContext);
        }            
        
        if (isDebug) {

            checks.checkActionProperty(lpWithParams.property);

            //noinspection unchecked
            LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.property;

            ActionProperty property = lAction.property;

            Map<String, PropertyInterface> paramsToInterfaces = new HashMap<>();
            Map<String, String> paramsToClassFQN = new HashMap<>();

            for (int i = 0; i < lpWithParams.usedParams.size(); i++) {
                int usedParam = lpWithParams.usedParams.get(i);
                if(usedParam >= oldContext.size()) { // если новый параметр
                    TypedParameter param = newContext.get(usedParam);

                    paramsToInterfaces.put(param.paramName, lAction.listInterfaces.get(i));
                    paramsToClassFQN.put(param.paramName, param.getParsedName());
                }
            }

            debugger.addParamInfo(property, paramsToInterfaces, paramsToClassFQN);
        }
        
        return lpWithParams;
    }

    // assert'им что newContext "расширяет" oldContext (во всяком случае такое предположение в addScriptedForAProp)
    private LPWithParams patchExtendParams(LPWithParams lpWithParams, List<TypedParameter> newContext, List<TypedParameter> oldContext) {

        if(!lpWithParams.property.listInterfaces.isEmpty() && lpWithParams.usedParams.isEmpty()) {
            return lpWithParams;
        }
        
        Set<Integer> usedExtendParams = new HashSet<>();
        for (int i = 0; i < lpWithParams.usedParams.size(); i++) {
            Integer usedParam = lpWithParams.usedParams.get(i);
            if(usedParam >= oldContext.size()) {
                usedExtendParams.add(usedParam);
            }
        }
        
        if(usedExtendParams.size() == (newContext.size() - oldContext.size())) { // все использованы
            return lpWithParams;
        }

        // по сути этот алгоритм эмулирует создание ListAction, с докидыванием в конец виртуального action'а который использует все extend параметры, однако само действие при этом не создает 
        List<LPWithParams> allCreationParams = new ArrayList<>();
        allCreationParams.add(lpWithParams);        
        for (int i = oldContext.size(); i < newContext.size(); i++) { // докидываем 
            allCreationParams.add(new LPWithParams(null, Collections.singletonList(i)));
        }

        List<Object> resultParams = getParamsPlainList(allCreationParams);
        LAP wrappedLAP = addListAProp(newContext.size() - oldContext.size(), resultParams.toArray());

        List<Integer> wrappedUsed = mergeAllParams(allCreationParams);
        return new LPWithParams(wrappedLAP, wrappedUsed);
    }

    public void checkPropertyValue(LP property) {
        checks.checkPropertyValue(property, alwaysNullProperties);        
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
            checks.checkNamespace(namespaceName);
        }

        for (String moduleName : requiredModules) {
            checks.checkModule(BL.getSysModule(moduleName), moduleName);
        }

        Set<String> prioritySet = new HashSet<>();
        for (String namespaceName : namespacePriority) {
            if (prioritySet.contains(namespaceName)) {
                errLog.emitNonUniquePriorityListError(parser, namespaceName);
            }
            prioritySet.add(namespaceName);
        }
    }

    public void setPropertyScriptInfo(LP property, String script, DebugInfo.DebugPoint point) {
        property.setCreationScript(script);
        property.setCreationPath(point.toString());
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
            LogicsModule requiredModule = BL.getSysModule(requiredModuleName);
            assert requiredModule != null;
            if (!visitedModules.contains(requiredModule)) {
                initNamespacesToModules(requiredModule, visitedModules);
            }
        }
    }

    private void showWarnings() {
        for (String warningText : warningList) {
            scriptLogger.warn("WARNING!" + warningText);
        }
    }

    public interface AbstractPropertyUsage {
        
    }     
    
    public static class LPUsage implements AbstractPropertyUsage {
        public final LP lp;
        public final List<ResolveClassSet> signature;

        public LPUsage(LP lp) {
            this(lp, null);
        }
        public LPUsage(LP lp, List<ResolveClassSet> signature) {
            this.lp = lp;
            this.signature = signature;
        }
    }
    
    public static class PropertyUsage implements AbstractPropertyUsage {
        public String name;
        public List<String> classNames;
        
        public PropertyUsage(String name) {
            this(name, null);
        }
        
        public PropertyUsage(String name, List<String> classNames) {
            this.name = name;
            this.classNames = classNames;
        }
        
        public String getSourceName() {
            String result = null;
            if (name != null) {
                result = name;
                if (classNames != null) {
                    result += "[";
                    for (String className : classNames) {
                        if (!result.endsWith("[")) {
                            result += ", ";
                        } 
                        result += className;
                    }
                    result += "]";
                }
            }
            return result;
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
                cls = findClass(cName);
            } else {
                cls = null;
            }                                
            paramName = pName;
        }
        
        public String getParsedName() {
            if(cls != null)
                return cls.getParsedName();
            return null;
        }
    }

    public static class FormActionProps {
        public final LPWithParams in;
        public final Boolean inNull;

        public final boolean out;
        public final Integer outParamNum; 
        public final Boolean outNull;
        public final PropertyUsage outProp;

        public final LPWithParams changeProp;

        public final boolean assign;
        public final DebugInfo.DebugPoint assignDebugPoint;
        public final boolean constraintFilter;


        public FormActionProps(LPWithParams in, Boolean inNull, boolean out, Integer outParamNum, Boolean outNull, PropertyUsage outProp, boolean constraintFilter, boolean assign, LPWithParams changeProp, DebugInfo.DebugPoint changeDebugPoint) {
            assert outProp == null;
            this.in = in;
            this.inNull = inNull;
            this.out = out;
            this.outParamNum = outParamNum;
            this.outNull = outNull;
            this.outProp = outProp;
            this.constraintFilter = constraintFilter;
            this.assign = assign;
            this.changeProp = changeProp;
            this.assignDebugPoint = changeDebugPoint;
        }
    }

}
