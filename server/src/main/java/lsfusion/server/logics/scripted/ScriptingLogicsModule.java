package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
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
import lsfusion.server.logics.property.Event;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.*;
import lsfusion.server.logics.property.actions.external.ExternalDBActionProperty;
import lsfusion.server.logics.property.actions.external.ExternalDBFActionProperty;
import lsfusion.server.logics.property.actions.external.ExternalHTTPActionProperty;
import lsfusion.server.logics.property.actions.file.ReadActionProperty;
import lsfusion.server.logics.property.actions.file.WriteActionProperty;
import lsfusion.server.logics.property.actions.flow.BreakActionProperty;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.actions.flow.ReturnActionProperty;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.logics.property.derived.AggregateGroupProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.resolving.ResolvingErrors;
import lsfusion.server.logics.resolving.ResolvingErrors.ResolvingError;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.mail.SendEmailActionProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.LocalNestedType;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.codehaus.janino.SimpleCompiler;

import javax.mail.Message;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.logics.PropertyUtils.*;
import static lsfusion.server.logics.scripted.AlignmentUtils.*;

public class ScriptingLogicsModule extends LogicsModule {

    private static final Logger systemLogger = ServerLoggers.systemLogger;

    protected final BusinessLogics BL;

    private String code = null;
    private String filename = null;
    private String path = null;
    private final ScriptingErrorLog errLog;
    private ScriptParser parser;
    private ScriptingLogicsModuleChecks checks;
    private List<String> warningList = new ArrayList<>();
    private Map<CalcProperty, String> alwaysNullProperties = new HashMap<>();

    private String lastOptimizedJPropSID = null;

    public enum ConstType { STATIC, INT, REAL, NUMERIC, STRING, LOGICAL, LONG, DATE, DATETIME, TIME, COLOR, NULL }
    public enum InsertPosition {IN, BEFORE, AFTER, FIRST}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, AGGR, EQUAL, LAST, NAGGR}

    public ScriptingLogicsModule(String filename, BaseLogicsModule baseModule, BusinessLogics BL) {
        this(baseModule, BL);
        this.filename = filename;
    }

    public ScriptingLogicsModule(InputStream stream, String path, BaseLogicsModule baseModule, BusinessLogics BL) throws IOException {
        this(stream, path, "utf-8", baseModule, BL);
    }

    public ScriptingLogicsModule(InputStream stream, String path, String charsetName, BaseLogicsModule baseModule, BusinessLogics BL) throws IOException {
        this(baseModule, BL);
        this.code = IOUtils.readStreamToString(stream, charsetName);
        this.path = path;
        errLog.setModuleId(getIdentifier());
    }

    public ScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL, String code) {
        this(baseModule, BL);
        this.code = code;
    }

    private ScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL) {
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
    public void initMetaAndClasses() throws RecognitionException {
        parseStep(ScriptParser.State.META_CLASS_TABLE);
    }

    @Override
    public void initTables() throws RecognitionException {
        addScriptedTables();
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        warningList.clear();
        
        parseStep(ScriptParser.State.MAIN);
        
        if (!parser.isInsideMetacode()) {
            showWarnings();
        }
    }

    @Override
    public void initIndexes() throws RecognitionException {
        for (TemporaryIndexInfo info : tempIndicies) {
            addIndex(info.keyNames, info.params);
        }
        tempIndicies.clear();
        
        for (LCP property : indexedProperties) {
            addIndex(property);
        }
        indexedProperties.clear();
    }

    public void initScriptingModule(String name, String namespace, List<String> requiredModules, List<String> namespacePriority) {
        setModuleName(name);
        setNamespace(namespace == null ? name : namespace);
        setDefaultNamespace(namespace == null);
        if (requiredModules.isEmpty() && !getName().equals("System")) {
            requiredModules.add("System");
        }
        setRequiredNames(new LinkedHashSet<>(requiredModules));
        setNamespacePriority(namespacePriority);
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
    }

    private void setModuleName(String moduleName) {
        setName(moduleName);
        errLog.setModuleId(getIdentifier());
    }
    
    private String getIdentifier() {
        String id = getName();
        if (id == null) {
            id = path;
        }
        return (id == null ? "" : id);
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

    public ImOrderSet<ObjectEntity> getMappingObjectsArray(FormEntity form, ImOrderSet<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MOrderExclSet<ObjectEntity> mObjects = SetFact.mOrderExclSet(mapping.size()); // throwing exception
        for (int i = 0; i < mapping.size(); i++) {
            mObjects.exclAdd(getNFObjectEntityByName(form, mapping.get(i)));
        }
        return mObjects.immutableOrder();
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

    public MappedProperty getPropertyWithMapping(FormEntity form, AbstractFormPropertyUsage pDrawUsage, Result<Pair<Property, String>> inherited) throws ScriptingErrorLog.SemanticErrorException {
        assert !(pDrawUsage instanceof FormPredefinedUsage);
        LP<?, ?> property;
        ImOrderSet<String> mapping;
        if(pDrawUsage instanceof FormActionOrPropertyUsage) {
            List<String> usageMapping = ((FormActionOrPropertyUsage<?>) pDrawUsage).mapping;
            LP usageProperty = findLPByActionOrPropertyUsage(((FormActionOrPropertyUsage) pDrawUsage).usage, form, usageMapping);
            
            ImList<String> uMapping = ListFact.fromJavaList(usageMapping);
            mapping = uMapping.toOrderSet();
            if(mapping.size() == usageMapping.size())
                property = usageProperty;
            else {
                final ImOrderSet<String> fMapping = mapping;
                ImList<Integer> indexMapping = uMapping.mapListValues(new GetValue<Integer, String>() {
                    public Integer getMapValue(String value) {
                        return fMapping.indexOf(value) + 1;
                    }
                });
                if(usageProperty instanceof LCP)
                    property = addJProp(false, LocalizedString.NONAME, (LCP)usageProperty, indexMapping.toArray(new Integer[uMapping.size()]));
                else
                    property = addJoinAProp((LAP)usageProperty, indexMapping.toArray(new Integer[uMapping.size()]));
                
                if(inherited != null) {
                    inherited.set(new Pair<>(usageProperty.property, usageProperty.property.isNamed() ? PropertyDrawEntity.createSID(usageProperty.property.getName(), usageMapping) : null));                    
                }
            }
        } else {
            property = ((FormLPUsage)pDrawUsage).lp;
            mapping = ((FormLPUsage<?>) pDrawUsage).mapping;
        }

//        if (property.property.interfaces.size() != mapping.size()) {
//            getErrLog().emitParamCountError(parser, property, mapping.size());
//        }
        return new MappedProperty(property, getMappingObjectsArray(form, mapping));
    }

    public LCP<?> findLCPByPropertyUsage(PropertyUsage pUsage, FormEntity form, List<String> mapping, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames != null)
            return findLCPByPropertyUsage(nullIfNotFound, pUsage);
        List<ResolveClassSet> classes = getMappingClassesArray(form, mapping);
        return findLCPByNameAndClasses(pUsage.name, pUsage.getSourceName(), classes, nullIfNotFound);
    }
    public LAP<?> findLAPByPropertyUsage(PropertyUsage pUsage, FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames != null)
            return findLAPByPropertyUsage(pUsage);
        List<ResolveClassSet> classes = getMappingClassesArray(form, mapping);
        return findLAPByNameAndClasses(pUsage.name, pUsage.getSourceName(), classes);
    }

    public LP<?, ?> findLPByActionOrPropertyUsage(ActionOrPropertyUsage orUsage) throws ScriptingErrorLog.SemanticErrorException {
        PropertyUsage pUsage = orUsage.property;
        if(orUsage instanceof CalcPropertyUsage) {
            return findLCPByPropertyUsage(pUsage);
        }
        if(orUsage instanceof ActionPropertyUsage) {
            return findLAPByPropertyUsage(pUsage);
        }
        assert orUsage instanceof PropertyElseActionUsage;
        LP<?, ?> result = findLCPByPropertyUsage(true, pUsage);
        if(result == null)
            result = findLAPByPropertyUsage(pUsage);
        return result;
    }
    public LP<?, ?> findLPByActionOrPropertyUsage(ActionOrPropertyUsage orUsage, FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyUsage pUsage = orUsage.property;
        if(orUsage instanceof CalcPropertyUsage) {
            return findLCPByPropertyUsage(pUsage, form, mapping, false);
        }
        if(orUsage instanceof ActionPropertyUsage) {
            return findLAPByPropertyUsage(pUsage, form, mapping);
        }
        assert orUsage instanceof PropertyElseActionUsage;
        LP<?, ?> result = findLCPByPropertyUsage(pUsage, form, mapping, true);
        if(result == null)
            result = findLAPByPropertyUsage(pUsage, form, mapping);
        return result;
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
        checks.checkDuplicateClass(className);
        checks.checkStaticClassConstraints(isAbstract, instNames, instCaptions);
        checks.checkClassParents(parentNames);

        LocalizedString caption = (captionStr == null ? LocalizedString.create(className) : captionStr);

        ImList<CustomClass> parents = BaseUtils.immutableCast(findClasses(parentNames));

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

            if(!instNames.isEmpty())
                cls.addParentClass(getBaseClass().staticObjectClass, version);
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
        PropertyCompoundNameParser parser = new PropertyCompoundNameParser(this, name);
        return findLAPByNameAndClasses(parser.propertyCompoundNameWithoutSignature(), name, parser.getSignature());
    }

    public LCP<?> findProperty(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyCompoundNameParser parser = new PropertyCompoundNameParser(this, name);
        return findLCPByNameAndClasses(parser.propertyCompoundNameWithoutSignature(), name, parser.getSignature());
    }

    public LCP<?>[] findProperties(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LCP<?>[] result = new LCP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    public LCP<?> findLCPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByNameAndClasses(name, sourceName, params, false);
    }
    public LCP<?> findLCPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByNameAndClasses(name, sourceName, params, false, false, nullIfNotFound);
    }
    public LCP<?> findLCPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByNameAndClasses(name, sourceName, params, onlyAbstract, prioritizeNotEqual, false);
    }
    public LCP<?> findLCPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        LCP<?> property = null;

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

        if(!nullIfNotFound)
            checks.checkProperty(property, sourceName == null ? name : sourceName);
        return property;
    }
    private LAP<?> findLAPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLAPByNameAndClasses(name, sourceName, params, false, false);
    }
    private LAP<?> findLAPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual) throws ScriptingErrorLog.SemanticErrorException {
        LAP<?> property = null;

        try {
            if (onlyAbstract) {
                property = resolveAbstractAction(name, params, prioritizeNotEqual);
            } else {
                property = resolveAction(name, params);
            }
        } catch (ResolvingErrors.ResolvingAmbiguousPropertyError e) {
            if (sourceName != null) {
                e.name = sourceName;
            }
            convertResolveError(e);
        } catch (ResolvingError e) {
            convertResolveError(e);
        }

        checks.checkAction(property, sourceName == null ? name : sourceName, params);
        return property;
    }

    public LCP<?> findLCPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByPropertyUsage(pUsage, false);
    }
    public LCP<?> findLCPByPropertyUsage(boolean nullIfNotFound, PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByPropertyUsage(pUsage, false, nullIfNotFound);
    }

    public LAP<?> findLAPByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLAPByPropertyUsage(pUsage, false);
    }

    public LCP<?> findLCPByPropertyUsage(PropertyUsage pUsage, boolean isAbstract) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByPropertyUsage(pUsage, isAbstract, false);
    }
    public LCP<?> findLCPByPropertyUsage(PropertyUsage pUsage, boolean isAbstract, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByNameAndClasses(pUsage.name, pUsage.getSourceName(), getParamClasses(pUsage), isAbstract, false, nullIfNotFound);
    }
    public LAP<?> findLAPByPropertyUsage(PropertyUsage pUsage, boolean isAbstract) throws ScriptingErrorLog.SemanticErrorException {
        return findLAPByNameAndClasses(pUsage.name, pUsage.getSourceName(), getParamClasses(pUsage), isAbstract, false);
    }

    public LAP<?> findLAPNoParamsByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames == null) {
            pUsage.classNames = Collections.emptyList();
        }
        LAP<?> lap = findLAPByPropertyUsage(pUsage);
        ValueClass[] paramClasses = lap.getInterfaceClasses(ClassType.signaturePolicy);
        if (paramClasses.length != 0) {
            errLog.emitPropertyWithParamsExpectedError(getParser(), pUsage.name, "[]");
        }
        return lap;
    }

    public LCP<?> findLCPNoParamsByPropertyUsage(PropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPParamByPropertyUsage(pUsage, ListFact.<ValueClass>EMPTY());
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
        return findForm(name, false);
    }
    public FormEntity findForm(String name, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        try {
            FormEntity form = resolveForm(name);
            if(form == null && nullIfNotFound)
                return null;
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

    public void addScriptedGroup(String groupName, LocalizedString captionStr, String integrationSID, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateGroup(groupName);
        LocalizedString caption = (captionStr == null ? LocalizedString.create(groupName) : captionStr);
        AbstractGroup parentGroup = (parentName == null ? null : findGroup(parentName));
        AbstractGroup group = addAbstractGroup(groupName, caption, parentGroup);
        group.setIntegrationSID(integrationSID);
    }

    public ScriptingFormEntity createScriptedForm(String formName, LocalizedString caption, DebugInfo.DebugPoint point, String icon,
                                                  ModalityType modalityType, int autoRefresh) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateForm(formName);
        caption = (caption == null ? LocalizedString.create(formName) : caption);

        String canonicalName = elementCanonicalName(formName);

        ScriptingFormEntity form = new ScriptingFormEntity(this, new FormEntity(canonicalName, point, caption, icon, getVersion()));
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
        formEntity.setDebugPoint(point);
    }

    public void finalizeScriptedForm(ScriptingFormEntity form) {
        form.getForm().finalizeInit(getVersion());
    }

    public ScriptingFormEntity getFormForExtending(String name) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(name);
        return new ScriptingFormEntity(this, form);
    }

    public LCP addScriptedDProp(String returnClass, List<String> paramClasses, boolean sessionProp, boolean innerProp, boolean isLocalScope, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoInline(innerProp);

        ValueClass value = findClass(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClass(paramClasses.get(i));
        }

        if (sessionProp) {
            return addSDProp(LocalizedString.NONAME, isLocalScope, value, nestedType, params);
        } else {
            assert nestedType == null;
            return addDProp(LocalizedString.NONAME, value, params);
        }
    }

    public LCP<?> addScriptedAbstractProp(CaseUnionProperty.Type type, String returnClass, List<String> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass value = findClass(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClass(paramClasses.get(i));
        }
        return addAUProp(null, false, isExclusive, isChecked, isLast, type, LocalizedString.NONAME, value, params);
    }

    public LAP addScriptedAbstractActionProp(ListCaseActionProperty.AbstractType type, List<String> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast) throws ScriptingErrorLog.SemanticErrorException {
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

    // todo [dale]: выделить общий код    
    public void addImplementationToAbstractAction(PropertyUsage abstractPropUsage, List<TypedParameter> context, LAPWithParams implement, LCPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        LAP abstractLP = findLAPByPropertyUsage(abstractPropUsage, context, true);
        checks.checkParamCount(abstractLP, context.size());
        checks.checkImplementIsNotMain(abstractLP, implement.getLP());

        List<LPWithParams> allProps = new ArrayList<>();
        allProps.add(implement);
        if (when != null) {
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        List<ResolveClassSet> signature = getClassesFromTypedParams(context);
        addImplementationToAbstractAction(abstractPropUsage.name, abstractLP, signature, when != null, params);
    }

    public void addImplementationToAbstractProp(PropertyUsage abstractPropUsage, List<TypedParameter> context, LCPWithParams implement, LCPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        LCP abstractLP = findLCPByPropertyUsage(abstractPropUsage, context, true);
        checks.checkParamCount(abstractLP, context.size());
        checks.checkImplementIsNotMain(abstractLP, implement.getLP());

        List<LPWithParams> allProps = new ArrayList<>();
        allProps.add(implement);
        if (when != null) {
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        List<ResolveClassSet> signature = getClassesFromTypedParams(context);
        addImplementationToAbstractProp(abstractPropUsage.name, abstractLP, signature, when != null, params);
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

    public void getParamIndices(List<TypedParameter> typedParams, List<TypedParameter> context, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        for(TypedParameter typedParam : typedParams) {
            getParamIndex(typedParam, context, dynamic, insideRecursion);
        }
    }

    public int getParamIndex(TypedParameter param, List<TypedParameter> context, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        String paramName = param.paramName;
        int index = indexOf(context, paramName);

        if (index < 0 && isRecursiveParam(paramName)) {
            if (param.cls != null) {
                errLog.emitParamClassNonDeclarationError(parser, paramName);
            } else if (!insideRecursion) {
                errLog.emitRecursiveParamsOutideRecursionError(parser, paramName);
            } else if (indexOf(context, paramName.substring(1)) < 0) {
                errLog.emitParamNotFoundError(parser, paramName.substring(1));
            }
        }

        if (index >= 0 && param.cls != null && context != null) {
            ValueClass existingParamClass = context.get(index).cls;
            if (existingParamClass != null) {
                errLog.emitParamClassRedefinitionError(parser, paramName, existingParamClass.getParsedName());
            } else {
                errLog.emitParamClassNonDeclarationError(parser, paramName);
            }
        }
        if (index < 0 && context != null && (dynamic || paramName.startsWith("$") && insideRecursion)) {
            if (isRecursiveParam(paramName) && insideRecursion) {
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

    private boolean isRecursiveParam(String paramName) {
        return paramName.startsWith("$");
    }

    public static abstract class LPWithParams {
        private final LP<?, ?> property; // nullable
        public final List<Integer> usedParams; // immutable ordered zero-based set

        public LPWithParams(LP<?, ?> property, List<Integer> usedParams) {
            this.property = property;
            this.usedParams = Collections.unmodifiableList(new ArrayList<>(usedParams));
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", property, usedParams);
        }

        public LP<?, ?> getLP() {
            return property;
        }
    }

    public static class LAPWithParams extends LPWithParams {
        public LAPWithParams(LAP<?> property, List<Integer> usedParams) {
            super(property, usedParams);
        }

        public LAP<?> getLP() {
            return (LAP<?>) super.getLP();
        }
    }

    public static class LCPWithParams extends LPWithParams {
        public LCPWithParams(LCP<?> property, List<Integer> usedParams) {
            super(property, usedParams);
        }

        public LCPWithParams(LCP<?> property, LPWithParams mapLP) {
            this(property, mapLP.usedParams);
        }

        public LCPWithParams(LCP<?> property, Integer usedParam) {
            this(property, Collections.singletonList(usedParam));
        }

        public LCPWithParams(Integer usedParam) {
            this(null, usedParam);
        }

        public LCPWithParams(LCP<?> property) {
            this(property, Collections.<Integer>emptyList());
        }

        public LCP<?> getLP() {
            return (LCP<?>) super.getLP();
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

    public static class LCPContextIndependent {
        public final LCP property;
        public final List<ResolveClassSet> signature;
        public final List<Integer> usedContext;

        public LCPContextIndependent(LCP property, List<ResolveClassSet> signature, List<Integer> usedContext) {
            this.property = property;
            this.signature = signature;
            this.usedContext = usedContext;
        }
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

    public ImList<ValueClass> getValueClassesFromTypedParams(List<TypedParameter> params) {
        MList<ValueClass> mResult = ListFact.mList(params.size());
        for (TypedParameter param : params) {
            mResult.add(param.cls);
        }
        return mResult.immutableList();
    }

    public FormLPUsage checkPropertyIsNew(FormLCPUsage property) {
        if(property.lp.property.getSID().equals(lastOptimizedJPropSID))
            property = new FormLCPUsage(addJProp(false, LocalizedString.NONAME, property.lp, BaseUtils.consecutiveList(property.lp.property.interfaces.size(), 1).toArray()), property.mapping, property.signature);
        return property;
    }

    public LP makeActionOrPropertyPublic(FormEntity form, String alias, FormLPUsage<?> lpUsage) {
        String name = "_FORM_" + form.getCanonicalName().replace('.', '_') + "_" + alias;
        LP property = lpUsage.lp;
        if (property != null && property instanceof LCP && propertyNeedsToBeWrapped((LCP)property)) {
            property = wrappedProperty((LCP)lpUsage.lp);
        }
        makeActionOrPropertyPublic(property, name, lpUsage.signature);
        return property;
    }

    public void addSettingsToActionOrProperty(LP property, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature,
                                      ActionOrPropertySettings ps) throws ScriptingErrorLog.SemanticErrorException {
        property.property.annotation = ps.annotation;

        List<String> paramNames = getParamNamesFromTypedParams(params);
        checks.checkDistinctParameters(paramNames);
        checks.checkNamedParams(property, paramNames);
        checks.checkParamsClasses(params, signature);

        String groupName = ps.groupName;
        AbstractGroup group = (groupName == null ? null : findGroup(groupName));
        property.property.caption = (caption == null ? LocalizedString.create(name) : caption);
        addPropertyToGroup(property.property, group);
    }

    public void addSettingsToAction(LAP property, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature, ActionSettings ps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateAction(name, signature);

        addSettingsToActionOrProperty(property, name, caption, params, signature, ps);

        makeActionPublic(property, name, signature);
    }

    public LCP addSettingsToProperty(LCP<?> baseProperty, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature,
                                      PropertySettings ps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateProperty(name, signature);

        LCP<?> property = baseProperty;
        if (propertyNeedsToBeWrapped(property)) {
            property = wrappedProperty(property);
        }

        addSettingsToActionOrProperty(property, name, caption, params, signature, ps);

        makePropertyPublic(property, name, signature);

        ImplementTable targetTable = null;
        String tableName = ps.table;
        if (tableName != null) {
            targetTable = findTable(tableName);
            if (targetTable.getMapKeysTable(property.property.getOrderTableInterfaceClasses(ClassType.storedPolicy)) == null) {
                errLog.emitWrongClassesForTableError(parser, name, tableName);
            }
        }
        if (property.property instanceof StoredDataProperty) {
            property.property.markStored(baseLM.tableFactory, targetTable);
        } else if (ps.isPersistent && (property.property instanceof AggregateProperty)) {
            addPersistent(property, targetTable);
        }

        if(ps.isComplex)
            property.property.complex = true;

        if(ps.noHint)
            property.property.noHint = true;

        BooleanDebug notNull = ps.notNull;
        if (notNull != null) {
            BooleanDebug notNullResolve = ps.notNullResolve;
            setNotNull(property, notNull.debugPoint, ps.notNullEvent,
                    notNullResolve != null ? ListFact.singleton(new PropertyFollowsDebug(false, true, notNullResolve.debugPoint)) :
                                             ListFact.<PropertyFollowsDebug>EMPTY());

            if(notNullResolve != null)
                property.property.setAggr(true);
        }

        if (Settings.get().isCheckAlwaysNull()) {
            checks.checkPropertyValue(property, alwaysNullProperties);
            if (!alwaysNullProperties.isEmpty()) {
                showAlwaysNullErrors();
            }
        }

//            if (Settings.get().isCheckClassWhere()) {
//                checks.checkClassWhere((LCP) property, name);
//            }
        makeLoggable(baseProperty, ps.isLoggable);
        return property;
    }

    /** Проверяет нужно ли обернуть свойство в join.
     *  Свойства нужно обернуть, если это не только что созданное свойство, а свойство, созданное ранее с уже установленными 
     *  параметрами (например, с установленным каноническим именем или debug point'ом). Такая ситуация возникает, если 
     *  была произведена какая-то оптимизация: кэширование (например, с помощью IdentityLazy) либо логика с lastOptimizedJPropSID.
     *  todo [dale]: Сейчас проверяются только основные частные случаи.
     */
    private boolean propertyNeedsToBeWrapped(LCP<?> property) {
        // Если объявление имеет вид f(x, y) = g(x, y), то нужно дополнительно обернуть свойство g в join
        return property.property.getSID().equals(lastOptimizedJPropSID)
                || property.property instanceof ValueProperty
                || property.property instanceof IsClassProperty;
    }

    private LCP<?> wrappedProperty(LCP<?> property) {
        return addJProp(false, LocalizedString.NONAME, property, BaseUtils.consecutiveList(property.property.interfaces.size(), 1).toArray());
    }

    private void showAlwaysNullErrors() throws ScriptingErrorLog.SemanticErrorException {
        StringBuilder errorMessage = new StringBuilder();
        for (CalcProperty property : alwaysNullProperties.keySet()) {
            if (errorMessage.length() > 0) {
                errorMessage.append("\n");
            }
            String location = alwaysNullProperties.get(property);
            errorMessage.append("[error]:\t" + location + " property '" + property.getName() + "' is always NULL");
        }
        alwaysNullProperties.clear();
        ScriptingErrorLog.emitSemanticError(errorMessage.toString(), new ScriptingErrorLog.SemanticErrorException(parser.getCurrentParser().input));
    }

    public void addToContextMenuFor(LAP onContextAction, LocalizedString contextMenuCaption, ActionOrPropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        LP<?, ?> mainProperty = findLPByActionOrPropertyUsage(mainPropertyUsage);
        onContextAction.addToContextMenuFor(mainProperty, contextMenuCaption);

        onContextAction.setAsEditActionFor(onContextAction.property.getSID(), mainProperty);
    }

    public void setAsEditActionFor(LAP onEditAction, String editActionSID, ActionOrPropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert mainPropertyUsage != null;

        LP<?, ?> mainProperty = findLPByActionOrPropertyUsage(mainPropertyUsage);
        onEditAction.setAsEditActionFor(editActionSID, mainProperty);
    }

    public void setForceViewType(LP property, ClassViewType viewType) {
        property.setForceViewType(viewType);
    }

    public void setFixedCharWidth(LP property, Integer fixedCharWidth) {
        if (fixedCharWidth != null && fixedCharWidth > 0)
            property.setFixedCharWidth(fixedCharWidth);
    }

    public void setCharWidth(LP property, Integer charWidth) {
        if (charWidth != null)
            property.setCharWidth(charWidth);
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

    public void setAutoset(LCP property, boolean autoset) {
        property.setAutoset(autoset);
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

    public void makeLoggable(LCP property, boolean isLoggable) {
        if (isLoggable && property != null) {
            property.makeLoggable(this, BL.systemEventsLM);
        }
    }

    public void setEchoSymbols(LP property) {
        property.setEchoSymbols(true);
    }

    public void setAggr(LP property) {
        ((CalcProperty)property.property).setAggr(true);
    }

    public void setScriptedEditAction(LP property, String actionType, LAPWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        property.property.setEditAction(actionType, actionImplements.get(0));
    }

    public void setScriptedContextMenuAction(LP property, LocalizedString contextMenuCaption, LAPWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = actionImplements.get(0);

        String actionSID = actionImplement.property.getSID();
        property.property.setContextMenuAction(actionSID, FormPropertyOptions.getContextMenuCaption(contextMenuCaption, actionImplement.property));
        property.property.setEditAction(actionSID, actionImplement);
    }

    public void setScriptedKeyPressAction(LP property, String key, LAPWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = actionImplements.get(0);

        String actionSID = actionImplement.property.getSID();
        property.property.setKeyAction(KeyStroke.getKeyStroke(key), actionSID);
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

    private List<ResolveClassSet> getParamClassesByParamProperties(List<LCPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = new ArrayList<>();
        for (LCPWithParams paramProp : paramProps) {
            if (paramProp.getLP() != null) {
                LCP lcp = paramProp.getLP();
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

    private LCP findLCPByPropertyUsage(PropertyUsage mainProp, List<LCPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLCPByPropertyUsage(mainProp);
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, context);
        return findLCPByNameAndClasses(mainProp.name, mainProp.getSourceName(), classes);
    }

    private LAP findLAPByPropertyUsage(PropertyUsage mainProp, List<LCPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLAPByPropertyUsage(mainProp);
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, context);
        return findLAPByNameAndClasses(mainProp.name, mainProp.getSourceName(), classes);
    }

    private LCP findLCPByPropertyUsage(PropertyUsage mainProp, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPByPropertyUsage(mainProp, params, false);
    }

    private LCP findLCPByPropertyUsage(PropertyUsage mainProp, List<TypedParameter> params, boolean onlyAbstract) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLCPByPropertyUsage(mainProp, onlyAbstract);
        return findLCPByNameAndClasses(mainProp.name, mainProp.getSourceName(), getClassesFromTypedParams(params), onlyAbstract, true);
    }

    private LAP findLAPByPropertyUsage(PropertyUsage mainProp, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLAPByPropertyUsage(mainProp, params, false);
    }

    private LAP findLAPByPropertyUsage(PropertyUsage mainProp, List<TypedParameter> params, boolean onlyAbstract) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLAPByPropertyUsage(mainProp, onlyAbstract);
        return findLAPByNameAndClasses(mainProp.name, mainProp.getSourceName(), getClassesFromTypedParams(params), onlyAbstract, true);
    }

    public LCPWithParams addScriptedJProp(boolean user, PropertyUsage pUsage, List<LCPWithParams> paramProps, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LCP mainProp = findLCPByPropertyUsage(pUsage, paramProps, params);
        return addScriptedJProp(user, mainProp, paramProps);
    }

    public LCPWithParams addScriptedJProp(LCP mainProp, List<LCPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(false, mainProp, paramProps);
    }

    public LCPWithParams addScriptedJProp(boolean user, LCP mainProp, List<LCPWithParams> paramProps, List<Integer> usedContext, boolean ci) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(user, mainProp, getAllGroupProps(usedContext, paramProps, ci));
    }

    public LCPWithParams addScriptedJProp(boolean user, LCP mainProp, List<LCPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LCP prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
            lastOptimizedJPropSID = mainProp.property.getSID();
        } else {
            prop = addJProp(user, LocalizedString.NONAME, mainProp, resultParams.toArray());
        }
        return new LCPWithParams(prop, mergeAllParams(paramProps));
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

    public LCPWithParams addScriptedEqualityProp(String op, LCPWithParams leftProp, LCPWithParams rightProp, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkComparisonCompatibility(leftProp, rightProp, context);
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LCPWithParams addScriptedRelationalProp(String op, LCPWithParams leftProp, LCPWithParams rightProp, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkComparisonCompatibility(leftProp, rightProp, context);
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LCPWithParams addScriptedOverrideProp(List<LCPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.EXCLUSIVE, properties, "EXCLUSIVE");
        } else {
            return addScriptedUProp(Union.OVERRIDE, properties, "OVERRIDE");
        }
    }

    public LCPWithParams addScriptedLikeProp(LCPWithParams leftProp, LCPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(baseLM.like2, asList(leftProp, rightProp));
    }

    public LCPWithParams addScriptedIfProp(List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LCPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            boolean[] notsArray = new boolean[properties.size() - 1];
            Arrays.fill(notsArray, false);
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LCPWithParams addScriptedOrProp(List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LCPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            List<LCPWithParams> logicalProperties = convertToLogical(properties);
            res = addScriptedUProp(Union.OVERRIDE, logicalProperties, "OR");
        }
        return res;
    }

    public LCPWithParams addScriptedXorProp(List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LCPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            List<LCPWithParams> logicalProperties = convertToLogical(properties);
            res = addScriptedUProp(Union.XOR, logicalProperties, "XOR");
        }
        return res;
    }

    public LCPWithParams addScriptedAndProp(List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LCPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            boolean[] notsArray = new boolean[properties.size() - 1];
            Arrays.fill(notsArray, false);

            LCPWithParams firstArgument = properties.get(0);
            if (!isLogical(firstArgument.getLP())) {
                properties.set(0, new LCPWithParams(toLogical(firstArgument).getLP(), firstArgument));
            }
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    private List<LCPWithParams> convertToLogical(List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        List<LCPWithParams> logicalProperties = new ArrayList<>();
        for (LCPWithParams prop : properties) {
            LCPWithParams logicalProp = prop;
            if (!isLogical(prop.getLP())) {
                logicalProp = new LCPWithParams(toLogical(prop).getLP(), prop);
            }
            logicalProperties.add(logicalProp);
        }
        return logicalProperties;
    }

    private LCPWithParams toLogical(LCPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(and(false), Arrays.asList(new LCPWithParams(baseLM.vtrue), property));
    }

    public LCPWithParams addScriptedIfElseUProp(LCPWithParams ifProp, LCPWithParams thenProp, LCPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
        List<LCPWithParams> lpParams = new ArrayList<>();
        lpParams.add(addScriptedJProp(and(false), asList(thenProp, ifProp)));
        if (elseProp != null) {
            lpParams.add(addScriptedJProp(and(true), asList(elseProp, ifProp)));
        }
        return addScriptedUProp(Union.EXCLUSIVE, lpParams, "IF");
    }

    public LCPWithParams addScriptedCaseUProp(List<LCPWithParams> whenProps, List<LCPWithParams> thenProps, LCPWithParams elseProp, boolean isExclusive) {
        assert whenProps.size() > 0 && whenProps.size() == thenProps.size();

        List<LCPWithParams> caseParamProps = new ArrayList<>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParamProps.add(whenProps.get(i));
            caseParamProps.add(thenProps.get(i));
        }
        if (elseProp != null) {
            caseParamProps.add(elseProp);
        }

        LCP caseProp = addCaseUProp(null, false, LocalizedString.NONAME, isExclusive, getParamsPlainList(caseParamProps).toArray());
        return new LCPWithParams(caseProp, mergeAllParams(caseParamProps));
    }

    public LCPWithParams addScriptedMultiProp(List<LCPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.CLASS, properties, "MULTI");
        } else {
            return addScriptedUProp(Union.CLASSOVERRIDE, properties, "MULTI");
        }
    }

    public LAP addScriptedCustomActionProp(String javaClassName, List<String> classes, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
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
            errLog.emitCreatingClassInstanceError(parser, e.getMessage(), javaClassName);
        }
        return null;
    }

    public LAP addScriptedCustomActionProp(String code, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
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
            errLog.emitCreatingClassInstanceError(parser, e.getMessage(), script);
        }
        return null;
    }


    public ImList<Type> getTypesByParamProperties(List<LCPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, params);
        MList<Type> mTypes = ListFact.mList(classes.size());
        for(int i=0,size=paramProps.size();i<size;i++) {
            Type type = null;

            ResolveClassSet paramClass = classes.get(i);
            if(paramClass != null)
                type = paramClass.getType();
            else {
                LCP<?> property = paramProps.get(i).getLP();
                if(property != null) {
                    ValueClass valueClass = property.property.getValueClass(ClassType.valuePolicy);
                    if (valueClass != null)
                        type = valueClass.getType();
                }
            }
            mTypes.add(type);
        }
        return mTypes.immutableList();
    }

    public ImList<ValueClass> getValueClassesByParamProperties(List<LCPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, params);
        MList<ValueClass> mValueClasses = ListFact.mList(classes.size());
        for(int i=0,size=paramProps.size();i<size;i++) {
            ValueClass valueClass = null;

            LCP<?> property = paramProps.get(i).getLP();
            if(property != null)
                valueClass = property.property.getValueClass(ClassType.valuePolicy);

            if(valueClass == null) {
                ResolveClassSet paramClass = classes.get(i);
                if(paramClass != null)
                    valueClass = paramClass.getCommonClass();
            }
            mValueClasses.add(valueClass);
        }
        return mValueClasses.immutableList();
    }

    public Type getTypeByParamProperty(LCPWithParams paramProp, List<TypedParameter> params) {
        return getTypesByParamProperties(Collections.singletonList(paramProp), params).single();
    }

    public ValueClass getValueClassByParamProperty(LCPWithParams paramProp, List<TypedParameter> params) {
        return getValueClassesByParamProperties(Collections.singletonList(paramProp), params).single();
    }

    public ImList<Type> getTypesForExternalProp(List<LCPWithParams> paramProps, List<TypedParameter> params) {
        return getTypesByParamProperties(paramProps, params);
    }

    public LAPWithParams addScriptedExternalJavaActionProp(List<LCPWithParams> params, List<TypedParameter> context, List<PropertyUsage> toPropertyUsageList) {
        throw new UnsupportedOperationException("EXTERNAL JAVA not supported");
    }

    public LAPWithParams addScriptedExternalDBActionProp(LCPWithParams connectionString, LCPWithParams exec, List<LCPWithParams> params, List<TypedParameter> context, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalDBActionProperty(getTypesForExternalProp(params, context), findLCPsNoParamsByPropertyUsage(toPropertyUsageList))),
                BaseUtils.mergeList(Arrays.asList(connectionString, exec), params));
    }

    public LAPWithParams addScriptedExternalDBFActionProp(LCPWithParams connectionString, String charset, List<LCPWithParams> params, List<TypedParameter> context, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalDBFActionProperty(getTypesForExternalProp(params, context), charset, findLCPsNoParamsByPropertyUsage(toPropertyUsageList))),
                BaseUtils.addList(connectionString, params));
    }

    public LAPWithParams addScriptedExternalHTTPActionProp(ExternalHttpMethod method, LCPWithParams connectionString, LCPWithParams bodyUrl, PropertyUsage headers, PropertyUsage headersTo,
                                                           List<LCPWithParams> params, List<TypedParameter> context, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        LCP headersProperty = headers != null ? findLCPStringParamByPropertyUsage(headers) : null;
        LCP headersToProperty = headersTo != null ? findLCPStringParamByPropertyUsage(headersTo) : null;
        return addScriptedJoinAProp(addAProp(new ExternalHTTPActionProperty(method != null ? method : ExternalHttpMethod.POST,
                        getTypesForExternalProp(params, context), findLCPsNoParamsByPropertyUsage(toPropertyUsageList), headersProperty, headersToProperty, bodyUrl != null)),
                bodyUrl != null ? BaseUtils.mergeList(Arrays.asList(connectionString, bodyUrl), params) : BaseUtils.addList(connectionString, params));
    }

    public LAPWithParams addScriptedExternalLSFActionProp(LCPWithParams connectionString, LCPWithParams actionLCP, boolean eval, boolean action, List<LCPWithParams> params, List<TypedParameter> context, List<PropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        String request = eval ? (action ? "eval/action" : "eval") : "/exec?action=$" + (params.size()+1);
        return addScriptedExternalHTTPActionProp(ExternalHttpMethod.POST,
                addScriptedJProp(getArithProp("+"), Arrays.asList(connectionString, new LCPWithParams(addCProp(StringClass.text, LocalizedString.create(request, false))))),
                null, null, null, BaseUtils.add(params, actionLCP), context, toPropertyUsageList);
    }

    private ImList<LCP> findLCPsNoParamsByPropertyUsage(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages == null)
            return ListFact.EMPTY();

        MList<LCP> mProps = ListFact.mList(propUsages.size());
        for (PropertyUsage propUsage : propUsages) {
            LCP<?> lcp = findLCPNoParamsByPropertyUsage(propUsage);
            mProps.add(lcp);
        }
        return mProps.immutableList();
    }

    public LAPWithParams addScriptedEmailProp(LCPWithParams fromProp,
                                             LCPWithParams subjProp,
                                             LCPWithParams bodyProp,
                                             List<Message.RecipientType> recipTypes,
                                             List<LCPWithParams> recipProps,
                                             List<LCPWithParams> attachFileNames,
                                             List<LCPWithParams> attachFiles) throws ScriptingErrorLog.SemanticErrorException {

        List<LPWithParams> allProps = new ArrayList<>();

        if (fromProp != null) {
            allProps.add(fromProp);
        }
        if(subjProp != null) {
            allProps.add(subjProp);
        }
        if(bodyProp != null) {
            allProps.add(bodyProp);
        }
        allProps.addAll(recipProps);

        for (int i = 0; i < attachFileNames.size(); i++) {
            if (attachFileNames.get(i) != null) {
                allProps.add(attachFileNames.get(i));
            }
            allProps.add(attachFiles.get(i));
        }

        Object[] allParams = getParamsPlainList(allProps).toArray();

        ImOrderSet<PropertyInterface> tempContext = genInterfaces(getIntNum(allParams));
        ValueClass[] eaClasses = CalcProperty.getCommonClasses(tempContext, readCalcImplements(tempContext, allParams).getCol());

        LAP<ClassPropertyInterface> eaPropLP = BL.emailLM.addEAProp(null, LocalizedString.NONAME, eaClasses);
        SendEmailActionProperty eaProp = (SendEmailActionProperty) eaPropLP.property;

        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> allImplements = readCalcImplements(eaPropLP.listInterfaces, allParams);

        int i = 0;
        
        if (fromProp != null) {
            eaProp.setFromAddressAccount(allImplements.get(i++));
        }

        if(subjProp != null) {
            eaProp.setSubject(allImplements.get(i++));
        }

        if(bodyProp != null) {
            eaProp.addInlineFile(allImplements.get(i++));
        }

        for (Message.RecipientType recipType : recipTypes) {
            eaProp.addRecipient(allImplements.get(i++), recipType);
        }

        for (LCPWithParams fileName : attachFileNames) {
            eaProp.addAttachmentFile(fileName != null ? allImplements.get(i++) : null, allImplements.get(i++));
        }

        return new LAPWithParams(eaPropLP, mergeAllParams(allProps));
    }

    public LCPWithParams addScriptedAdditiveOrProp(List<String> operands, List<LCPWithParams> properties) {
        assert operands.size() + 1 == properties.size();

        LCPWithParams res = properties.get(0);
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
            res = new LCPWithParams(addUProp(null, LocalizedString.NONAME, Union.SUM, null, coeffs, resultParams.toArray()), mergeAllParams(properties));
        }
        return res;
    }

    public LCPWithParams addScriptedAdditiveProp(List<String> operands, List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LCPWithParams sumLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            LCPWithParams currLP = properties.get(i);
            sumLP = addScriptedJProp(getArithProp(operands.get(i-1)), asList(sumLP, currLP));
        }
        return sumLP;
    }


    public LCPWithParams addScriptedMultiplicativeProp(List<String> operands, List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LCPWithParams curLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(getArithProp(op), asList(curLP, properties.get(i)));
        }
        return curLP;
    }

    public LCPWithParams addScriptedUnaryMinusProp(LCPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(baseLM.minus, Collections.singletonList(prop));
    }

    public LCPWithParams addScriptedNotProp(LCPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(not(), Collections.singletonList(prop));
    }

    public LCPWithParams addScriptedCastProp(String typeName, LCPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(typeName);

        //cls всегда будет DataClass из-за грамматики
        assert cls instanceof DataClass;

        return addScriptedJProp(addCastProp((DataClass) cls), Collections.singletonList(prop));
    }

    private boolean doesExtendContext(int contextSize, List<? extends LPWithParams> list, List<LCPWithParams> orders) {
        Set<Integer> listContext = new HashSet<>();
        for(int i=0;i<contextSize;i++)
            listContext.add(i);
        for(LPWithParams lp : list)
            if(lp.getLP() != null)
                listContext.addAll(lp.usedParams);
        return !listContext.containsAll(mergeAllParams(orders));
    }

    private List<Integer> mergeAllParams(List<? extends LPWithParams> lpList) {
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

    public LAPWithParams addScriptedListAProp(List<LAPWithParams> properties, List<LCP> localProps) {
        List<Object> resultParams = getParamsPlainList(properties);

        MExclSet<Pair<LCP, List<ResolveClassSet>>> mDebugLocals = null;
        if(debugger.isEnabled()) {
            mDebugLocals = SetFact.mExclSet(localProps.size());
        }
        MSet<SessionDataProperty> mLocals = SetFact.mSet();
        for (LCP<?> localProp : localProps) {
            if (mDebugLocals != null) {
                List<ResolveClassSet> localSignature = getLocalSignature(localProp);
                mDebugLocals.exclAdd(new Pair<LCP, List<ResolveClassSet>>(localProp, localSignature));
            }
            mLocals.add((SessionDataProperty) localProp.property);

            removeLocal(localProp);
        }

        LAP<?> listLP = addListAProp(mLocals.immutable(), resultParams.toArray());

        if(mDebugLocals != null) {
            listLP.property.setDebugLocals(mDebugLocals.immutable());
        }

        List<Integer> usedParams = mergeAllParams(properties);
        return new LAPWithParams(listLP, usedParams);
    }

    public LAPWithParams addScriptedNewSessionAProp(LAPWithParams action, List<PropertyUsage> migrateSessionProps, boolean migrateAllSessionProps,
                                                   boolean isNested, boolean singleApply, boolean newSQL) throws ScriptingErrorLog.SemanticErrorException {
        LAP<?> sessionLP = addNewSessionAProp(null, action.getLP(), isNested, singleApply, newSQL, getMigrateProps(migrateSessionProps, migrateAllSessionProps));
        return new LAPWithParams(sessionLP, action.usedParams);
    }

    public DataClass getInputDataClass(String paramName, List<TypedParameter> context, String typeId, LCPWithParams oldValue, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        DataClass requestDataClass;
        if(typeId != null) {
            requestDataClass = ClassCanonicalNameUtils.getScriptedDataClass(typeId);
        } else {
            ValueClass valueClass = getValueClassByParamProperty(oldValue, context);
            checks.checkInputDataClass(valueClass);
            requestDataClass = (DataClass) valueClass;
        }

        if(paramName != null)
            getParamIndex(new TypedParameter(requestDataClass, paramName), context, true, insideRecursion);
        return requestDataClass;
    }

    public LAPWithParams addScriptedInputAProp(DataClass requestDataClass, LCPWithParams oldValue, PropertyUsage targetProp, LAPWithParams doAction, LAPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean assign, LCPWithParams changeProp, DebugInfo.DebugPoint assignDebugPoint) throws ScriptingErrorLog.SemanticErrorException {
        assert targetProp == null;
        LCP<?> tprop = getInputProp(targetProp, requestDataClass, null);

        LAP property = addInputAProp(requestDataClass, tprop != null ? tprop.property : null);
        
        if (changeProp == null)
            changeProp = oldValue;

        // optimization. we don't use files on client side (see also DefaultChangeActionProperty.executeCustom()) 
        if (oldValue == null || getTypeByParamProperty(oldValue, oldContext) instanceof FileClass)
            oldValue = new LCPWithParams(baseLM.vnull);
        LAPWithParams inputAction = addScriptedJoinAProp(property, Collections.singletonList(oldValue));

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, ListFact.<LCP>singleton(tprop), inputAction,
                ListFact.singleton(assign ? new Pair<>(changeProp, assignDebugPoint) : null));
    }


    public LAPWithParams addScriptedRequestAProp(LAPWithParams requestAction, LAPWithParams doAction, LAPWithParams elseAction) {
        List<LPWithParams> propParams = new ArrayList<>();
        propParams.add(requestAction);
        propParams.add(doAction);
        if(elseAction != null)
            propParams.add(elseAction);

        List<Integer> allParams = mergeAllParams(propParams);
        LAP result = addRequestAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LAPWithParams(result, allParams);
    }

    public LAPWithParams addScriptedActiveFormAProp(String formName) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(formName);
        return new LAPWithParams(addAProp(null, new IsActiveFormActionProperty(LocalizedString.NONAME, form, baseLM.getIsActiveFormProperty())), new ArrayList<Integer>());
    }

    public LAPWithParams addScriptedActivateAProp(FormEntity form, ComponentView component) {
        return new LAPWithParams(addAProp(null, new ActivateActionProperty(LocalizedString.NONAME, form, component)), new ArrayList<Integer>());
    }

    public List<LCP<?>> addLocalDataProperty(List<String> names, String returnClassName, List<String> paramClassNames,
                                             LocalNestedType nestedType, DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {

        List<ResolveClassSet> signature = new ArrayList<>();
        for (String className : paramClassNames) {
            signature.add(findClass(className).getResolveSet());
        }

        List<LCP<?>> res = new ArrayList<>();
        for (String name : names) {
            LCP<?> lcp = addScriptedDProp(returnClassName, paramClassNames, true, false, true, nestedType);
            addLocal(lcp, new LocalPropertyData(name, signature));
            lcp.property.setDebugInfo(new CalcPropertyDebugInfo(point, false));
            res.add(lcp);
        }
        return res;
    }

    public LCP addWatchLocalDataProperty(LCP lp, List<ResolveClassSet> signature) {
        assert lp.property instanceof SessionDataProperty;
        addModuleLP(lp);
        propClasses.put(lp, signature);
        return lp;
    }

    public LAPWithParams addScriptedJoinAProp(PropertyUsage pUsage, List<LCPWithParams> properties, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LAP mainProp = findLAPByPropertyUsage(pUsage, properties, params);
        return addScriptedJoinAProp(mainProp, properties);
    }

    public LAPWithParams addScriptedJoinAProp(LAP mainProp, List<LCPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkParamCount(mainProp, properties.size());

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);
        LAP prop = addJoinAProp(null, LocalizedString.NONAME, mainProp, resultParams.toArray());
        return new LAPWithParams(prop, usedParams);
    }

    public LAPWithParams addScriptedConfirmProp(LCPWithParams msgProp, LAPWithParams doAction, LAPWithParams elseAction, boolean yesNo, List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        LCP targetProp = null;
        if(yesNo)
            targetProp = getInputProp(null, LogicalClass.instance, null);

        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addConfirmAProp("lsFusion", yesNo, targetProp, resultParams.toArray());
        LAPWithParams inputAction = new LAPWithParams(asyncLAP, msgProp.usedParams);

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, yesNo ? ListFact.singleton(targetProp) : ListFact.<LCP>EMPTY(), inputAction, yesNo ? ListFact.<Pair<LCPWithParams, DebugInfo.DebugPoint>>singleton(null) : ListFact.<Pair<LCPWithParams, DebugInfo.DebugPoint>>EMPTY());
    }

    public LAPWithParams addScriptedMessageProp(LCPWithParams msgProp, boolean noWait) {
        List<Object> resultParams = getParamsPlainList(singletonList(msgProp));
        LAP asyncLAP = addMAProp("lsFusion", noWait, resultParams.toArray());
        return new LAPWithParams(asyncLAP, msgProp.usedParams);
    }

    public LAPWithParams addScriptedAsyncUpdateProp(LCPWithParams asyncProp) {
        List<Object> resultParams = getParamsPlainList(singletonList(asyncProp));
        LAP asyncLAP = addAsyncUpdateAProp(resultParams.toArray());
        return new LAPWithParams(asyncLAP, asyncProp.usedParams);
    }

    private FormEntity getFormFromSeekObjectName(String formObjectName) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = formObjectName.lastIndexOf('.');
        assert pointPos > 0;

        String formName = formObjectName.substring(0, pointPos);
        return findForm(formName);
    }

    private ObjectEntity getSeekObject(FormEntity form, String formObjectName) {
        return form.getNFObject(getSeekObjectName(formObjectName), getVersion());
    }

    private GroupObjectEntity getSeekGroupObject(FormEntity form, String formObjectName) {
        return form.getNFGroupObject(getSeekObjectName(formObjectName), getVersion());
    }

    private String getSeekObjectName(String formObjectName) {
        int pointPos = formObjectName.lastIndexOf('.');
        assert pointPos > 0;

        return formObjectName.substring(pointPos + 1);
    }

    public LAPWithParams addScriptedObjectSeekProp(String name, LCPWithParams seekProp, UpdateType type) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        ObjectEntity object = getSeekObject(form, name);

        if (object != null) {
            List<Object> resultParams = getParamsPlainList(singletonList(seekProp));
            LAP lap = addOSAProp(object, type, resultParams.toArray());
            return new LAPWithParams(lap, seekProp.usedParams);
        } else {
            errLog.emitObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LAPWithParams addScriptedGroupObjectSeekProp(String name, List<String> objNames, List<LCPWithParams> values, UpdateType type) throws ScriptingErrorLog.SemanticErrorException {
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
            LAP lap = addGOSAProp(groupObject, objects, type, resultParams.toArray());
            return new LAPWithParams(lap, mergeAllParams(values));
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LAPWithParams addScriptedEvalActionProp(LCPWithParams property, List<LCPWithParams> params, List<TypedParameter> contextParams, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        Type exprType = getTypeByParamProperty(property, contextParams);
        if (!(exprType instanceof StringClass)) {
            errLog.emitEvalExpressionError(parser);
        }

        List<LCP<?>> paramsLCP = new ArrayList<>();
        Set<Integer> allParams = new TreeSet<>(property.usedParams);
        if (params != null) {
            for (LCPWithParams param : params) {
                paramsLCP.add(param.getLP());
                allParams.addAll(param.usedParams);
            }
        }

        LAP<?> res = addEvalAProp(property.getLP(), paramsLCP, action);
        return new LAPWithParams(res, new ArrayList<>(allParams));
    }

    public LAPWithParams addScriptedDrillDownActionProp(LCPWithParams property) {
        LAP<?> res = addDrillDownAProp(property.getLP());
        return new LAPWithParams(res, property.usedParams);
    }

    public LAPWithParams addScriptedAssignPropertyAProp(List<TypedParameter> context, PropertyUsage toPropertyUsage, List<LCPWithParams> toPropertyMapping, LCPWithParams fromProperty, LCPWithParams whereProperty, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        LCP toPropertyLP = findLCPByPropertyUsage(toPropertyUsage, toPropertyMapping, newContext);

        LCPWithParams toProperty = addScriptedJProp(toPropertyLP, toPropertyMapping);

        return addScriptedAssignAProp(context, fromProperty, whereProperty, toProperty);
    }

    private LAPWithParams addScriptedAssignAProp(List<TypedParameter> context, LCPWithParams fromProperty, LCPWithParams whereProperty, LCPWithParams toProperty) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAssignProperty(fromProperty, toProperty);

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, fromProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LCPWithParams(resI));
        }
        paramsList.add(toProperty);
        paramsList.add(fromProperty);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LAP result = addSetPropertyAProp(null, LocalizedString.NONAME, resultInterfaces.size(), whereProperty != null, resultParams.toArray());
        return new LAPWithParams(result, resultInterfaces);
    }

    public LAPWithParams addScriptedAddObjProp(List<TypedParameter> context, String className, PropertyUsage toPropUsage, List<LCPWithParams> toPropMapping, LCPWithParams whereProperty, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkAddActionsClass(cls, className);
        checks.checkAddObjTOParams(context.size(), toPropMapping);

        LCPWithParams toProperty = null;
        if (toPropUsage != null && toPropMapping != null) {
            toProperty = addScriptedJProp(findLCPByPropertyUsage(toPropUsage, toPropMapping, newContext), toPropMapping);
        }

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, whereProperty);

        List<LCPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LCPWithParams(resI));
        }
        if (toProperty != null) {
            paramsList.add(toProperty);
        } else if (whereProperty == null) {
            paramsList.add(new LCPWithParams(new LCP<>(baseLM.getAddedObjectProperty())));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LAP result = addAddObjAProp((CustomClass) cls, false, resultInterfaces.size(), whereProperty != null, toProperty != null || whereProperty == null, resultParams.toArray());
        return new LAPWithParams(result, resultInterfaces);
    }

    public LAPWithParams addScriptedDeleteAProp(int oldContextSize, List<TypedParameter> newContext, LCPWithParams param, LCPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        LAPWithParams res = addScriptedChangeClassAProp(oldContextSize, newContext, param, getBaseClass().unknown, whereProperty);
        setDeleteActionOptions(res.getLP());
        return res;
    }

    public LAPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LCPWithParams param, String className, LCPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkChangeClassActionClass(cls, className);
        return addScriptedChangeClassAProp(oldContextSize, newContext, param, (ConcreteCustomClass) cls, whereProperty);
    }

    private LAPWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LCPWithParams param, ConcreteObjectClass cls, LCPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
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
            paramsList.add(new LCPWithParams(resI));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);

        LAP<?> res = addChangeClassAProp(cls, resultInterfaces.size(), changedIndex, contextExtended, whereProperty != null, resultParams.toArray());
        return new LAPWithParams(res,  resultInterfaces);
    }

    public List<Integer> getResultInterfaces(int contextSize, LPWithParams... params) {
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

    public LAPWithParams addScriptedIfAProp(LCPWithParams condition, LAPWithParams trueAction, LAPWithParams falseAction) {
        List<LPWithParams> propParams = toList(condition, trueAction);
        if (falseAction != null) {
            propParams.add(falseAction);
        }
        List<Integer> allParams = mergeAllParams(propParams);
        LAP result = addIfAProp(null, LocalizedString.NONAME, false, getParamsPlainList(propParams).toArray());
        return new LAPWithParams(result, allParams);
    }

    public LAPWithParams addScriptedTryAProp(LAPWithParams tryAction, LAPWithParams catchAction, LAPWithParams finallyAction) {
        List<LPWithParams> propParams = new ArrayList<>();
        propParams.add(tryAction);
        if (catchAction != null) {
            propParams.add(catchAction);
        }if (finallyAction != null) {
            propParams.add(finallyAction);
        }

        List<Integer> allParams = mergeAllParams(propParams);
        LAP result = addTryAProp(null, LocalizedString.NONAME, catchAction != null, finallyAction != null, getParamsPlainList(propParams).toArray());
        return new LAPWithParams(result, allParams);
    }

    public LAPWithParams addScriptedCaseAProp(List<LCPWithParams> whenProps, List<LAPWithParams> thenActions, LAPWithParams elseAction, boolean isExclusive) {
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
        LAP result = addCaseAProp(isExclusive, getParamsPlainList(caseParams).toArray());
        return new LAPWithParams(result, allParams);
    }

    public LAPWithParams addScriptedMultiAProp(List<LAPWithParams> actions, boolean isExclusive) {
        List<Integer> allParams = mergeAllParams(actions);
        LAP result = addMultiAProp(isExclusive, getParamsPlainList(actions).toArray());
        return new LAPWithParams(result, allParams);

    }

    public LAPWithParams addScriptedApplyAProp(LAPWithParams action, boolean singleApply, List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps, boolean serializable)
            throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> propParams = Collections.<LPWithParams>singletonList(action);

        LAP result = addApplyAProp(null, LocalizedString.NONAME, (action != null && action.getLP() != null) ? action.getLP() : null, singleApply,
                getMigrateProps(keepSessionProps, keepAllSessionProps), serializable);

        return new LAPWithParams(result, mergeAllParams(propParams));
    }

    public LAPWithParams addScriptedCancelAProp(List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps)
            throws ScriptingErrorLog.SemanticErrorException {
        LAP result = addCancelAProp(null, LocalizedString.NONAME, getMigrateProps(keepSessionProps, keepAllSessionProps));
        return new LAPWithParams(result, new ArrayList<Integer>());
    }

    private FunctionSet<SessionDataProperty> getMigrateProps(List<PropertyUsage> keepSessionProps, boolean keepAllSessionProps) throws ScriptingErrorLog.SemanticErrorException {
        FunctionSet<SessionDataProperty> keepProps;
        if(keepAllSessionProps) {
            keepProps = DataSession.keepAllSessionProperties;
        } else {
            MExclSet<SessionDataProperty> mKeepProps = SetFact.mExclSet(keepSessionProps.size());
            for (PropertyUsage migratePropUsage : keepSessionProps) {
                LCP<?> prop = findLCPByPropertyUsage(migratePropUsage);
                checks.checkSessionProperty(prop);
                mKeepProps.exclAdd((SessionDataProperty) prop.property);
            }
            keepProps = mKeepProps.immutable();
        }
        return keepProps;
    }

    public LAPWithParams addScriptedNewAProp(List<TypedParameter> oldContext, LAPWithParams action, Integer addNum, String addClassName, Boolean autoSet) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedForAProp(oldContext, null, new ArrayList<LCPWithParams>(), action, null, addNum, addClassName, autoSet, false, false, new ArrayList<LCPWithParams>(), false);
    }

    public LAPWithParams addScriptedForAProp(List<TypedParameter> oldContext, LCPWithParams condition, List<LCPWithParams> orders, LAPWithParams action, LAPWithParams elseAction, Integer addNum, String addClassName, Boolean autoSet, boolean recursive, boolean descending, List<LCPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        boolean ordersNotNull = (condition != null ? doesExtendContext(oldContext.size(), singletonList(condition), orders) : !orders.isEmpty());

        List<LPWithParams> creationParams = new ArrayList<>();
        if (condition != null) {
            creationParams.add(condition);
        }
        creationParams.addAll(orders);
        if(addNum != null) {
            creationParams.add(new LCPWithParams(addNum));
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
            allCreationParams.add(new LCPWithParams(usedParam));
        }
        allCreationParams.addAll(creationParams);
        if(noInline==null) { // предполагается надо включить все кроме addNum
            noInline = new ArrayList<>();
            for (int extParam : extParams)
                if(addNum==null || !addNum.equals(extParam)) {
                    noInline.add(new LCPWithParams(extParam));
                }
        }
        allCreationParams.addAll(noInline);

        LAP result = addForAProp(LocalizedString.NONAME, !descending, ordersNotNull, recursive, elseAction != null, usedParams.size(),
                                addClassName != null ? (CustomClass) findClass(addClassName) : null, autoSet != null ? autoSet : false, condition != null, noInline.size(), forceInline,
                                getParamsPlainList(allCreationParams).toArray());
        return new LAPWithParams(result, usedParams);
    }

    public LAPWithParams getTerminalFlowActionProperty(boolean isBreak) {
        return new LAPWithParams(isBreak ? new LAP<>(new BreakActionProperty()) : new LAP<>(new ReturnActionProperty()), new ArrayList<Integer>());
    }

    private List<Integer> getParamsAssertList(List<LCPWithParams> list) {
        List<Integer> result = new ArrayList<>();
        for(LCPWithParams lp : list) {
            assert lp.getLP() == null;
            result.add(BaseUtils.single(lp.usedParams));
        }
        return result;
    }

    @SafeVarargs
    private final List<Object> getParamsPlainList(List<? extends LPWithParams>... mappedPropLists) {
        List<LP> props = new ArrayList<>();
        List<List<Integer>> usedParams = new ArrayList<>();
        for (List<? extends LPWithParams> mappedPropList : mappedPropLists) {
            for (LPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.getLP());
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

    public LCP addScriptedGProp(List<LCPWithParams> groupProps, GroupingType type, List<LCPWithParams> mainProps, List<LCPWithParams> orderProps, boolean ascending, LCPWithParams whereProp, List<ResolveClassSet> explicitInnerClasses) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkGPropOrderConsistence(type, orderProps.size());
        checks.checkGPropAggrConstraints(type, mainProps, groupProps);
        checks.checkGPropAggregateConsistence(type, mainProps.size());
        checks.checkGPropWhereConsistence(type, whereProp);
        checks.checkGPropSumConstraints(type, mainProps.get(0));

        List<LCPWithParams> whereProps = new ArrayList<>();
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LCPWithParams(mainProps.get(0).usedParams.get(0)));
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

        boolean ordersNotNull = doesExtendContext(0, mergeLists(mainProps, groupProps, whereProps), orderProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps, whereProps)).size();
        assert groupPropParamCount == explicitInnerClasses.size();
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

    public LCPContextIndependent addScriptedCIGProp(int oldContextSize, List<LCPWithParams> groupProps, GroupingType type, List<LCPWithParams> mainProps, List<LCPWithParams> orderProps,
                                  boolean ascending, LCPWithParams whereProp, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedCDIGProp(oldContextSize, groupProps, type, mainProps, orderProps, ascending, whereProp, newContext);
    }

    // ci - надо в дырки вставлять, от использованных, если не ci то в конце
    public List<LCPWithParams> getAllGroupProps(List<Integer> resultInterfaces, List<LCPWithParams> groupProps, boolean ci) {
        List<LCPWithParams> allGroupProps = new ArrayList<>();

        if(ci) {
            Set<Integer> usedInterfaces = new HashSet<>(resultInterfaces);
//        нужно groupProps в дырки вставить для context independent группировки
            int ra = 0, ga = 0;
            int groupSize = groupProps.size();
            for (int i = 0, size = resultInterfaces.size() + groupSize; i < size; i++) {
                LCPWithParams add;
                if (ga >= groupSize || usedInterfaces.contains(i))
                    add = new LCPWithParams(resultInterfaces.get(ra++));
                else
                    add = groupProps.get(ga++);
                allGroupProps.add(add);
            }
        } else {
            for (int resI : resultInterfaces) {
                allGroupProps.add(new LCPWithParams(resI));
            }
            allGroupProps.addAll(groupProps);
        }

        return allGroupProps;
    }

    // второй результат в паре использованные параметры из внешнего контекста (LCP на выходе имеет сначала эти использованные параметры, потом группировки)
    public LCPContextIndependent addScriptedCDIGProp(int oldContextSize, List<LCPWithParams> groupProps, GroupingType type, List<LCPWithParams> mainProps, List<LCPWithParams> orderProps,
                                  boolean ascending, LCPWithParams whereProp, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        List<LCPWithParams> lpWithParams = mergeLists(groupProps, mainProps, orderProps, Collections.singletonList(whereProp));
        List<Integer> resultInterfaces = getResultInterfaces(oldContextSize, lpWithParams.toArray(new LPWithParams[lpWithParams.size()]));

        List<LCPWithParams> allGroupProps = getAllGroupProps(resultInterfaces, groupProps, true);

        List<ResolveClassSet> explicitInnerClasses = getClassesFromTypedParams(oldContextSize, resultInterfaces, newContext);

        LCP gProp = addScriptedGProp(allGroupProps, type, mainProps, orderProps, ascending, whereProp, explicitInnerClasses);
        return new LCPContextIndependent(gProp, getParamClassesByParamProperties(allGroupProps, newContext), resultInterfaces);
    }

    public List<ResolveClassSet> getClassesFromTypedParams(int oldContextSize, List<Integer> resultInterfaces, List<TypedParameter> newContext) {
        List<TypedParameter> usedInnerInterfaces = new ArrayList<>();
        for (int resI : resultInterfaces)
            usedInnerInterfaces.add(newContext.get(resI));
        usedInnerInterfaces.addAll(newContext.subList(oldContextSize, newContext.size()));
        return getClassesFromTypedParams(usedInnerInterfaces);
    }

    public Pair<LCPWithParams, LCPContextIndependent> addScriptedCDGProp(int oldContextSize, List<LCPWithParams> groupProps, GroupingType type, List<LCPWithParams> mainProps, List<LCPWithParams> orderProps,
                                  boolean ascending, LCPWithParams whereProp, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if(groupProps == null)
            groupProps = Collections.emptyList();
        LCPContextIndependent ci = addScriptedCDIGProp(oldContextSize, groupProps, type, mainProps, orderProps, ascending, whereProp, newContext);
        if(groupProps.size() > 0)
            return new Pair<>(null, ci);
        else
            return new Pair<>(new LCPWithParams(ci.property, ci.usedContext), null);
    }

    public LCPContextIndependent addScriptedAGProp(List<TypedParameter> context, String aggClassName, LCPWithParams whereExpr, DebugInfo.DebugPoint classDebugPoint, DebugInfo.DebugPoint exprDebugPoint, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoInline(innerPD);

        ValueClass aggClass = findClass(aggClassName);
        checks.checkAggrClass(aggClass, aggClassName);
        checks.checkParamCount(whereExpr.getLP(), context.size());

//        prim1Object = DATA prim1Class (aggrClass) INDEXED;
//        prim2Object = DATA prim2Class (aggrClass) INDEXED;

        List<LCPWithParams> groupProps = new ArrayList<>();
        List<ResolveClassSet> resultSignature = new ArrayList<>();
        ResolveClassSet aggSignature = aggClass.getResolveSet();
        for (TypedParameter param : context) {
            LCP lp = addDProp(LocalizedString.NONAME, param.cls, aggClass);

            makePropertyPublic(lp, param.paramName, aggSignature);
            ((StoredDataProperty) lp.property).markStored(baseLM.tableFactory);

            groupProps.add(new LCPWithParams(lp, 0));
            resultSignature.add(param.cls.getResolveSet());
        }

//        aggrObject (prim1Object, prim2Object) =
//                GROUP AGGR aggrClass aggrObject
//        WHERE aggrObject IS aggrClass BY prim1Object(aggrObject), prim2Object(aggrObject);
        LCP lcp = addScriptedGProp(groupProps, GroupingType.AGGR, Collections.singletonList(new LCPWithParams(0)), Collections.<LCPWithParams>emptyList(), false,
                new LCPWithParams(is(aggClass), 0), Collections.singletonList(aggSignature));
        ((AggregateGroupProperty) lcp.property).isFullAggr = true;

//        aggrProperty(prim1Class prim1Object, prim2Class prim2Object) => aggrObject(prim1Object, prim2Object) RESOLVE LEFT; // добавление
        addScriptedFollows(whereExpr.getLP(), new LCPWithParams(lcp, whereExpr), Collections.singletonList(new PropertyFollowsDebug(true, classDebugPoint)), Event.APPLY, null);

//        aggrObject IS aggrClass => aggrProperty(prim1Object(aggrObject), prim2Object(aggrObject)) RESOLVE RIGHT; // удаление
        addScriptedFollows(is(aggClass), addScriptedJProp(whereExpr.getLP(), groupProps), Collections.singletonList(new PropertyFollowsDebug(false, exprDebugPoint)), Event.APPLY, null);

        return new LCPContextIndependent(lcp, resultSignature, Collections.<Integer>emptyList());
    }

    public LCPWithParams addScriptedMaxProp(List<LCPWithParams> paramProps, boolean isMin) throws ScriptingErrorLog.SemanticErrorException {
        if (isMin) {
            return addScriptedUProp(Union.MIN, paramProps, "MIN");
        } else {
            return addScriptedUProp(Union.MAX, paramProps, "MAX");
        }
    }

    private LCPWithParams addScriptedUProp(Union unionType, List<LCPWithParams> paramProps, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
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
        return new LCPWithParams(prop, mergeAllParams(paramProps));
    }

    public LCPWithParams addScriptedPartitionProp(PartitionType partitionType, PropertyUsage ungroupPropUsage, boolean strict, int precision, boolean isAscending,
                                                 boolean useLast, int groupPropsCnt, List<LCPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkPartitionWindowConsistence(partitionType, useLast);
        LCP ungroupProp = ungroupPropUsage != null ? findLCPByPropertyUsage(ungroupPropUsage, paramProps.subList(0, groupPropsCnt), context) : null;
        checks.checkPartitionUngroupConsistence(ungroupProp, groupPropsCnt);

        boolean ordersNotNull = doesExtendContext(0, paramProps.subList(0, groupPropsCnt + 1), paramProps.subList(groupPropsCnt + 1, paramProps.size()));

        List<Object> resultParams = getParamsPlainList(paramProps);
        List<Integer> usedParams = mergeAllParams(paramProps);
        LCP prop;
        if (partitionType == PartitionType.SUM || partitionType == PartitionType.PREVIOUS) {
            prop = addOProp(null, false, LocalizedString.NONAME, partitionType, isAscending, ordersNotNull, useLast, groupPropsCnt, resultParams.toArray());
        } else if (partitionType == PartitionType.DISTR_CUM_PROPORTION) {
            List<ResolveClassSet> contextClasses = getClassesFromTypedParams(context);// для не script - временный хак
            // может быть внешний context
            List<ResolveClassSet> explicitInnerClasses = new ArrayList<>();
            for(int usedParam : usedParams)
                explicitInnerClasses.add(contextClasses.get(usedParam)); // one-based;
            prop = addPGProp(null, false, precision, strict, LocalizedString.NONAME, usedParams.size(), explicitInnerClasses, isAscending, ordersNotNull, ungroupProp, resultParams.toArray());
        } else {
            prop = addUGProp(null, false, strict, LocalizedString.NONAME, usedParams.size(), isAscending, ordersNotNull, ungroupProp, resultParams.toArray());
        }
        return new LCPWithParams(prop, usedParams);
    }

    public LCPWithParams addScriptedCCProp(List<LCPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addCCProp(params.size()), params);
    }

    public LCPWithParams addScriptedConcatProp(String separator, List<LCPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addSFUProp(params.size(), separator), params);
    }

    public LCPWithParams addScriptedDCCProp(LCPWithParams ccProp, int index) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDeconcatenateIndex(ccProp, index);
        return addScriptedJProp(addDCCProp(index - 1), Collections.singletonList(ccProp));
    }

    public LCP addScriptedSFProp(String typeName, List<SQLSyntaxType> types, List<String> implTexts, boolean hasNotNull) throws ScriptingErrorLog.SemanticErrorException {
        assert types.size() == implTexts.size();
        checks.checkSingleImplementation(types);

        Set<Integer> params = findFormulaParameters(implTexts.get(0));

        for (String text : implTexts) {
            Set<Integer> formulaParams = findFormulaParameters(text);
            checks.checkFormulaParameters(formulaParams);
            if (formulaParams.size() != params.size()) {
                errLog.emitFormulaDifferentParamCountError(parser, implTexts.get(0), text);
            }
        }

        String defaultFormula = "";
        MExclMap<SQLSyntaxType, String> mSyntaxes = MapFact.mExclMap();
        for (int i = 0; i < types.size(); i++) {
            SQLSyntaxType type = types.get(i);
            String text = transformFormulaText(implTexts.get(i), StringFormulaProperty.getParamName("$1"));
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

    public static String transformFormulaText(String text, String textTo) { // так как $i не постфиксный (например $1 и $12)
        return text != null ? text.replaceAll("\\$(\\d+)", textTo) : null;
    }

    public LCPWithParams addScriptedRProp(List<TypedParameter> context, LCPWithParams zeroStep, LCPWithParams nextStep, Cycle cycleType) throws ScriptingErrorLog.SemanticErrorException {
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
        LCP res = addRProp(null, false, LocalizedString.NONAME, cycleType, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

        List<Integer> resUsedParams = new ArrayList<>();
        for (Integer usedParam : usedParams) {
            if (!context.get(usedParam).paramName.startsWith("$")) {
                resUsedParams.add(usedParam);
            }
        }
        return new LCPWithParams(res, resUsedParams);
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

    public int createScriptedInteger(String literalText) throws ScriptingErrorLog.SemanticErrorException {
        int res = 0;
        try {
            res = Integer.parseInt(literalText);
        } catch (NumberFormatException e) {
            errLog.emitIntegerValueError(parser, literalText);
        }
        return res;
    }

    public long createScriptedLong(String literalText) throws ScriptingErrorLog.SemanticErrorException {
        long res = 0;
        try {
            res = Long.parseLong(literalText);
        } catch (NumberFormatException e) {
            errLog.emitLongValueError(parser, literalText);
        }
        return res;
    }

    public double createScriptedDouble(String literalText) throws ScriptingErrorLog.SemanticErrorException {
        double res = 0;
        try {
            res = Double.parseDouble(literalText);
        } catch (NumberFormatException e) {
            errLog.emitDoubleValueError(parser, literalText);
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

    public <O extends ObjectSelector> LAPWithParams addScriptedShowFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                              Boolean syncType, WindowFormType windowType, ManageSessionType manageSession, FormSessionScope formSessionScope,
                                              boolean checkOnOk, Boolean noCancel, boolean readonly) throws ScriptingErrorLog.SemanticErrorException {
        List<O> allObjects = mapped.objects;
        MList<O> mObjects = ListFact.mList(allObjects.size());
        List<LCPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mList(allObjects.size());
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mObjects.add(object);
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
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

        LAP property = addIFAProp(null, LocalizedString.NONAME, mapped.form, mObjects.immutableList(), mNulls.immutableList(),
                                 manageSession, noCancel,
                                 syncType, windowType, false, checkOnOk,
                                 readonly);

        property = addSessionScopeAProp(formSessionScope, property);

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LAPWithParams(property, new ArrayList<Integer>());
        }
    }

    private LCP<?> getInputProp(PropertyUsage targetProp, ValueClass valueClass, Set<CalcProperty> usedProps) throws ScriptingErrorLog.SemanticErrorException {
        if(targetProp != null) {
            LCP<?> result = findLCPNoParamsByPropertyUsage(targetProp);
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

    public <O extends ObjectSelector> LAPWithParams addScriptedDialogFAProp(
                                                MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                                WindowFormType windowType, ManageSessionType manageSession, FormSessionScope scope,
                                                boolean checkOnOk, Boolean noCancel, boolean readonly, LAPWithParams doAction, LAPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {

        List<O> allObjects = mapped.objects;
        MList<O> mInputObjects = ListFact.mListMax(allObjects.size());
        MList<Boolean> mInputNulls = ListFact.mListMax(allObjects.size());
        MList<LCP> mInputProps = ListFact.mListMax(allObjects.size());

        MList<O> mObjects = ListFact.mListMax(allObjects.size());
        List<LCPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mListMax(allObjects.size());

        MList<Pair<LCPWithParams, DebugInfo.DebugPoint>> mAssignProps = ListFact.mListMax(allObjects.size());

        MList<O> mContextObjects = ListFact.mListMax(allObjects.size() + 1);
        MList<CalcProperty> mContextProps = ListFact.mListMax(allObjects.size() + 1);
        List<LCPWithParams> contextLPs = new ArrayList<>();

        Set<CalcProperty> usedProps = new HashSet<>();

        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            if (objectProp.in != null) {
                mObjects.add(object);
                mapping.add(objectProp.in);
                mNulls.add(objectProp.inNull);
            }
            if (objectProp.out) {
                mInputObjects.add(object);
                mInputNulls.add(objectProp.outNull);
                LCP<?> outProp = getInputProp(objectProp.outProp, mapped.form.getBaseClass(object), usedProps);
                mInputProps.add(outProp);

                LCPWithParams changeProp = null;
                if(objectProp.constraintFilter || objectProp.assign) {
                    changeProp = objectProp.changeProp;
                    if(changeProp == null)
                        changeProp = objectProp.in;
                    assert changeProp != null;
                }
                if(objectProp.constraintFilter) {
                    mContextObjects.add(object);
                    mContextProps.add(changeProp.getLP().property);
                    contextLPs.add(changeProp);
                }

                Pair<LCPWithParams, DebugInfo.DebugPoint> assignProp = null;
                if(objectProp.assign)
                    assignProp = new Pair<>(changeProp, objectProp.assignDebugPoint);
                mAssignProps.add(assignProp);
            }
        }
        ImList<O> inputObjects = mInputObjects.immutableList();
        ImList<Boolean> inputNulls = mInputNulls.immutableList();
        ImList<LCP> inputProps = mInputProps.immutableList();

        ImList<Pair<LCPWithParams, DebugInfo.DebugPoint>> assignProps = mAssignProps.immutableList();

        ImList<O> contextObjects = mContextObjects.immutableList();
        ImList<CalcProperty> contextProps = mContextProps.immutableList();

        if(windowType == null)
            windowType = WindowFormType.FLOAT;

        List<LCPWithParams> propParams = new ArrayList<>();
        propParams.addAll(contextLPs);
        List<Integer> allParams = mergeAllParams(propParams);

        ImList<O> objects = mObjects.immutableList();
        LAP property = addIFAProp(null, LocalizedString.NONAME, mapped.form, objects, mNulls.immutableList(),
                                 inputObjects, inputProps, inputNulls,
                                 manageSession, noCancel,
                                 contextObjects, contextProps,
                true, windowType, false, checkOnOk,
                readonly);

        property = addSessionScopeAProp(scope, property, inputProps.addList(baseLM.getRequestCanceledProperty()).getCol());

        LAPWithParams formAction;
        if (mapping.size() > 0) { // тут надо contextLPs просто в mapping закинуть по идее сразу
            for(LCPWithParams contextLP : contextLPs)
                for (int usedParam : contextLP.usedParams) {
                    mapping.add(new LCPWithParams(usedParam));
                }
            formAction = addScriptedJoinAProp(property, mapping);
        } else {
            formAction = new LAPWithParams(property, allParams);
        }

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, inputProps, formAction, assignProps);
    }

    private LAPWithParams proceedInputDoClause(LAPWithParams doAction, LAPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, ImList<LCP> inputParamProps, LAPWithParams proceedAction, ImList<Pair<LCPWithParams, DebugInfo.DebugPoint>> assignProps) throws ScriptingErrorLog.SemanticErrorException {
        if (doAction != null) {
            doAction = extendDoParams(doAction, newContext, oldContext.size(), false, inputParamProps, null, assignProps);
            return addScriptedRequestAProp(proceedAction, doAction, elseAction);
        }

        return proceedAction;
    }

    private LAPWithParams proceedImportDoClause(boolean noParams, LAPWithParams doAction, LAPWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, LCP<?> whereLCP, ImList<LCP> importParamProps, ImList<Boolean> nulls, LAPWithParams proceedAction) throws ScriptingErrorLog.SemanticErrorException {
        if (doAction != null) {
            assert nulls != null;

            int paramOld = oldContext.size() + (!noParams ? 1 : 0);
            if(paramOld == newContext.size()) // хак, потом можно будет красивее сделать
                importParamProps = SetFact.EMPTYORDER();

            List<LAPWithParams> actions = new ArrayList<>();
            actions.add(proceedAction);

            LAPWithParams fillNullsAction = fillImportNullsAction(noParams, paramOld, oldContext, newContext, whereLCP, importParamProps, nulls);
            if(fillNullsAction != null)
                actions.add(fillNullsAction);

            actions.add(extendImportDoAction(noParams, paramOld, oldContext, newContext, doAction, elseAction, whereLCP, importParamProps, nulls));

            LAPWithParams listAction = addScriptedListAProp(actions, Collections.<LCP>emptyList());
            // хак - в ifAProp оборачиваем что delegationType был AFTER_DELEGATE, а не BEFORE или null, вообще по хорошему надо delegationType в момент parsing'а проставлять, а не в самих свойствах
            return addScriptedIfAProp(new LCPWithParams(baseLM.vtrue), listAction, null);
        }
        return proceedAction;
    }

    private LAPWithParams extendImportDoAction(boolean noParams, int paramOld, List<TypedParameter> oldContext, List<TypedParameter> newContext, LAPWithParams doAction, LAPWithParams elseAction, LCP<?> whereLCP, ImList<LCP> importParamProps, ImList<Boolean> nulls) throws ScriptingErrorLog.SemanticErrorException {
        ImList<Pair<LCPWithParams, DebugInfo.DebugPoint>> assignProps = ListFact.toList(importParamProps.size(), new GetIndex<Pair<LCPWithParams, DebugInfo.DebugPoint>>() {
            public Pair<LCPWithParams, DebugInfo.DebugPoint> getMapValue(int i) {
                return null;
            }});
        doAction = extendDoParams(doAction, newContext, paramOld, !noParams, importParamProps, nulls, assignProps); // row parameter consider to be external (it will be proceeded separately)
        if(!noParams) { // adding row parameter
            modifyContextFlowActionPropertyDefinitionBodyCreated(doAction, BaseUtils.add(oldContext, newContext.get(oldContext.size())), oldContext, false);

            doAction = addScriptedForAProp(oldContext, new LCPWithParams(whereLCP, oldContext.size()), Collections.singletonList(new LCPWithParams(oldContext.size())), doAction,
                    elseAction, null, null, false, false, false, Collections.<LCPWithParams>emptyList(), false);
        }
        return doAction;
    }

    // filling null values if necessary
    private LAPWithParams fillImportNullsAction(boolean noParams, int paramOld, List<TypedParameter> oldContext, List<TypedParameter> newContext, LCP<?> whereLCP, ImList<LCP> importParamProps, ImList<Boolean> nulls) throws ScriptingErrorLog.SemanticErrorException {
        List<Integer> params = !noParams ? Collections.<Integer>singletonList(oldContext.size()) : Collections.<Integer>emptyList();
        List<TypedParameter> oldAndRowContext = newContext.subList(0, paramOld);
        List<LAPWithParams> fillNulls = null;
        for(int i=paramOld;i<newContext.size();i++) {
            int importIndex = i - paramOld;
            if (!nulls.get(importIndex)) { // no null
                if (fillNulls == null)
                    fillNulls = new ArrayList<>();

                LCPWithParams importProp = new LCPWithParams(importParamProps.get(importIndex), params);
                DataClass cls = (DataClass) newContext.get(i).cls;
                LCPWithParams defaultValueProp = new LCPWithParams(addCProp(cls, DerivedProperty.getValueForProp(cls.getDefaultValue(), cls)));
                // prop(row) <- defvalue WHERE NOT prop(row)
                fillNulls.add(addScriptedAssignAProp(oldAndRowContext, defaultValueProp, addScriptedNotProp(importProp), importProp));
            }
        }
        if(fillNulls != null) {
            LAPWithParams fillNullsAction = addScriptedListAProp(fillNulls, Collections.<LCP>emptyList());
            if(!noParams) // FOR where(row)
                fillNullsAction = addScriptedForAProp(oldContext, new LCPWithParams(noParams ? baseLM.vtrue : whereLCP, params), Collections.<LCPWithParams>emptyList(), fillNullsAction,
                    null, null, null, false, false, false, Collections.<LCPWithParams>emptyList(), false);
            return fillNullsAction;
        }
        return null;
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

    private LAPWithParams nullExec(LAPWithParams doAction, int param) throws ScriptingErrorLog.SemanticErrorException {
        List<LCPWithParams> params = new ArrayList<>();
        boolean found = false;
        for(int usedParam : doAction.usedParams)
            if(usedParam == param){
                found = true;
                params.add(new LCPWithParams(baseLM.vnull));
            } else
                params.add(new LCPWithParams(usedParam));

        if(!found) // не было использований
            return null;
        return addScriptedJoinAProp(doAction.getLP(), params);
    }

    // recursive
    private LAPWithParams extendDoParams(LAPWithParams doAction, List<TypedParameter> context, int paramOld, boolean isLastParamRow, ImList<LCP> resultProps, ImList<Boolean> nulls, ImList<Pair<LCPWithParams, DebugInfo.DebugPoint>> assignProps) throws ScriptingErrorLog.SemanticErrorException {
        assert context.size() - paramOld == resultProps.size();
        assert resultProps.size() == assignProps.size();

        List<TypedParameter> currentContext = new ArrayList<>(context);
        int paramNum;
        while((paramNum = currentContext.size() - 1) >= paramOld) {
            // remove'им параметр
            List<TypedParameter> removedContext = BaseUtils.remove(currentContext, paramNum);

            LCPWithParams paramLP = new LCPWithParams(paramNum);
            Pair<LCPWithParams, DebugInfo.DebugPoint> assignLP = assignProps.get(paramNum - paramOld);
            if(assignLP != null) {
                LAPWithParams assignAction = addScriptedAssignAProp(currentContext, paramLP, null, assignLP.first);

                ScriptingLogicsModule.setDebugInfo(null, assignLP.second, assignAction.getLP().property);

                doAction = addScriptedListAProp(Arrays.asList(assignAction, doAction), new ArrayList<LCP>());
            }

            boolean paramNoNull = nulls != null && !nulls.get(paramNum - paramOld);
            LAPWithParams nullExec = paramNoNull ? null : nullExec(doAction, paramNum); // передает NULL в качестве параметра
            if(paramNoNull || nullExec != null) { // нет параметра нет проблемы
                modifyContextFlowActionPropertyDefinitionBodyCreated(doAction, currentContext, removedContext, false);

                LCP resultProp = resultProps.get(paramNum - paramOld);
                LCPWithParams resultLP = isLastParamRow ? new LCPWithParams(resultProp, paramOld - 1) : new LCPWithParams(resultProp);

                doAction = addScriptedForAProp(removedContext, addScriptedEqualityProp("==", paramLP, resultLP, currentContext), new ArrayList<LCPWithParams>(), doAction,
                        nullExec, null, null, false, false, false, isLastParamRow ? Collections.<LCPWithParams>emptyList() : null, false);
            }

            currentContext = removedContext;
        }

        return doAction;
    }

    public <O extends ObjectSelector> LAPWithParams addScriptedPrintFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                           LCPWithParams printerProperty, FormPrintType printType, PropertyUsage propUsage,
                                               Boolean syncType, Integer selectTop, PropertyUsage sheetNamePropUsage, LCPWithParams passwordProperty) throws ScriptingErrorLog.SemanticErrorException {
        assert printType != null;
        List<O> allObjects = mapped.objects;
        MList<O> mObjects = ListFact.mList(allObjects.size());
        List<LCPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mList(allObjects.size());
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mObjects.add(object);
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        if(syncType == null)
            syncType = false;

        //использования printerProperty и passwordProperty не пересекаются, поэтому параметры не разделяем
        List<LCPWithParams> propParams = new ArrayList<>();
        if(printerProperty != null) {
            propParams.add(printerProperty);
        }
        if(passwordProperty != null) {
            propParams.add(passwordProperty);
        }
        List<Integer> allParams = mergeAllParams(propParams);

        LCP<?> targetProp = null;
        if(propUsage != null)
            targetProp = findLCPNoParamsByPropertyUsage(propUsage);

        LCP<?> sheetNameProperty = null;
        if(sheetNamePropUsage != null)
            sheetNameProperty = findLCPNoParamsByPropertyUsage(sheetNamePropUsage);

        LAP property = addPFAProp(null, LocalizedString.NONAME, mapped.form, mObjects.immutableList(), mNulls.immutableList(),
                printerProperty != null ? printerProperty.getLP().property : null, sheetNameProperty, printType, syncType, selectTop,
                passwordProperty != null ? passwordProperty.getLP().property : null, targetProp, false);

        if (mapping.size() > 0)  { // тут надо printerProperty просто в mapping закинуть по идее сразу
            if(printerProperty != null) {
                for (int usedParam : printerProperty.usedParams) {
                    mapping.add(new LCPWithParams(usedParam));
                }
            }
            if(passwordProperty != null) {
                for (int usedParam : passwordProperty.usedParams) {
                    mapping.add(new LCPWithParams(usedParam));
                }
            }
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LAPWithParams(property, allParams);
        }
    }

    public <O extends ObjectSelector> LAPWithParams addScriptedExportFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps, FormIntegrationType exportType,
                                                                            LCPWithParams rootProperty, LCPWithParams tagProperty, boolean attr,
                                                                            boolean noHeader, String separator, boolean noEscape, String charset, PropertyUsage propUsage,
                                                                            OrderedMap<GroupObjectEntity, PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(exportType == null)
            exportType = FormIntegrationType.JSON;

        List<O> allObjects = mapped.objects;
        MList<O> mObjects = ListFact.mList(allObjects.size());
        List<LCPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mList(allObjects.size());
        for (int i = 0; i < allObjects.size(); i++) {
            O object = allObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mObjects.add(object);
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }


        List<LPWithParams> propParams = new ArrayList<>();
        List<Integer> allParams = mergeAllParams(propParams);

        LCP<?> singleExportFile = null;
        MExclMap<GroupObjectEntity, LCP> exportFiles = MapFact.mExclMap();
        if(exportType.isPlain()) {
            if(propUsages != null) {
                for (Map.Entry<GroupObjectEntity, PropertyUsage> entry : propUsages.entrySet()) {
                    exportFiles.exclAdd(entry.getKey(), findLCPNoParamsByPropertyUsage(entry.getValue()));
                }
            } else if (propUsage != null) {
                errLog.emitSimpleError(parser, String.format("EXPORT %s TO single file not supported", exportType));
            } else {
                errLog.emitSimpleError(parser, "Output file(s) for export not specified");
            }
        } else {
            if(propUsages != null) {
                errLog.emitSimpleError(parser, String.format("EXPORT %s TO multiple files not supported", exportType));
            } else {
                singleExportFile = propUsage != null ? findLCPNoParamsByPropertyUsage(propUsage) : baseLM.exportFile;
            }
        }

        if(rootProperty != null) {
            errLog.emitSimpleError(parser, "EXPORT form with ROOT not supported");
        }
        if(tagProperty != null) {
            errLog.emitSimpleError(parser, "EXPORT form with TAG not supported");
        }
        if(attr) {
            errLog.emitSimpleError(parser, "EXPORT form with ATTR not supported");
        }

        LAP property = addEFAProp(null, LocalizedString.NONAME, mapped.form, mObjects.immutableList(), mNulls.immutableList(),
                exportType, noHeader, separator, noEscape, charset, null, null, singleExportFile, exportFiles.immutable());

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(property, mapping);
        } else {
            return new LAPWithParams(property, allParams);
        }
    }

    public GroupObjectEntity findGroupObjectEntity(FormEntity form, String objectName) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity result = form.getNFGroupObject(objectName, getVersion());
        if (result == null) {
            errLog.emitGroupObjectNotFoundError(parser, objectName);
        }
        return result;
    }

    public ObjectEntity findObjectEntity(FormEntity form, String objectName) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity result = form.getNFObject(objectName, getVersion());
        if (result == null) {
            errLog.emitObjectNotFoundError(parser, objectName);
        }
        return result;
    }

    public void addScriptedMetaCodeFragment(String name, List<String> params, List<String> tokens, List<Pair<Integer, Boolean>> metaTokens, String code, int lineNumber) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateMetaCodeFragment(name, params.size());
        checks.checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(elementCanonicalName(name), params, tokens, metaTokens, code, getName(), lineNumber);
        addMetaCodeFragment(fragment);
    }

    public void runMetaCode(String name, List<String> params, int lineNumber, boolean enabledMeta) throws RecognitionException {
        MetaCodeFragment metaCode = findMetaCodeFragment(name, params.size());
        checks.checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params);
        parser.runMetaCode(this, code, metaCode, MetaCodeFragment.metaCodeCallString(name, metaCode, params), lineNumber, enabledMeta);
    }

    public Pair<List<String>, List<Pair<Integer, Boolean>>> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        return parser.grabMetaCode(metaCodeName);
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

    public void throwAlreadyDefinePropertyDraw(FormEntity.AlreadyDefined alreadyDefined) throws ScriptingErrorLog.SemanticErrorException {
        getErrLog().emitAlreadyDefinedPropertyDrawError(getParser(), alreadyDefined.formCanonicalName, alreadyDefined.newSID, alreadyDefined.formPath);
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


    public LCP addScriptedReflectionProperty(ReflectionPropertyType type, ActionOrPropertyUsage propertyUsage, List<ResolveClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        switch (type) {
            case CANONICAL_NAME:
            default: return addCanonicalNameProp(propertyUsage);
        }
    }

    public LCP addCanonicalNameProp(ActionOrPropertyUsage propertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        return new LCP<>(new CanonicalNameProperty(findLPByActionOrPropertyUsage(propertyUsage)));
    }

    public LAPWithParams addScriptedFocusActionProp(PropertyDrawEntity property) {
        return new LAPWithParams(addFocusActionProp(property), new ArrayList<Integer>());
    }

    public LAPWithParams addScriptedReadActionProperty(LCPWithParams sourcePathProp, PropertyUsage propUsage, List<TypedParameter> params, boolean clientAction, boolean dialog) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass sourceProp = getValueClassByParamProperty(sourcePathProp, params);
        LCP<?> targetProp = propUsage == null ? baseLM.readFile : findLCPNoParamsByPropertyUsage(propUsage);
        return addScriptedJoinAProp(addAProp(new ReadActionProperty(sourceProp, targetProp, clientAction, dialog)),
                Collections.singletonList(sourcePathProp));
    }

    public LAPWithParams addScriptedWriteActionProperty(LCPWithParams sourceProp, LCPWithParams pathProp, List<TypedParameter> params, boolean clientAction, boolean dialog, boolean append) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new WriteActionProperty(getTypeByParamProperty(sourceProp, params),
                clientAction, dialog, append, getValueClassByParamProperty(sourceProp, params), getValueClassByParamProperty(pathProp, params))),
                Arrays.asList(sourceProp, pathProp));
    }

    public ImList<Type> getTypesForExportProp(List<LCPWithParams> paramProps, List<TypedParameter> params) {
        return getTypesByParamProperties(paramProps, params);
    }

    public LAPWithParams addScriptedExportActionProperty(List<TypedParameter> oldContext, FormIntegrationType type, final List<String> ids, List<Boolean> literals,
                                                         List<LCPWithParams> exprs, LCPWithParams whereProperty, PropertyUsage fileProp, LCPWithParams rootProperty, LCPWithParams tagProperty,
                                                         String separator, boolean noHeader, boolean noEscape, String charset, boolean attr,
                                                         List<LCPWithParams> orderProperties, List<Boolean> orderDirections) throws ScriptingErrorLog.SemanticErrorException {

        LCP<?> targetProp = fileProp != null ? findLCPNoParamsByPropertyUsage(fileProp) : null;
        if(targetProp == null)
            targetProp = baseLM.exportFile;

        List<String> exIds = new ArrayList<>(ids);
        List<Boolean> exLiterals = new ArrayList<>(literals);

        MOrderExclMap<String, Boolean> mOrders = MapFact.mOrderExclMap(orderProperties.size());
        for (int i = 0; i < orderProperties.size(); i++) {
            LCPWithParams orderProperty = orderProperties.get(i);
            exprs.add(orderProperty);
            String orderId = "order" + exIds.size();
            exIds.add(orderId);
            exLiterals.add(false);
            mOrders.exclAdd(orderId, orderDirections.get(i));
        }
        ImOrderMap<String, Boolean> orders = mOrders.immutableOrder();

        List<LCPWithParams> props = exprs;
        if(whereProperty != null)
            props = BaseUtils.add(exprs, whereProperty);

        List<Integer> resultInterfaces = getResultInterfaces(oldContext.size(), props.toArray(new LPWithParams[exprs.size()+1]));

        if(type == null)
            type = FormIntegrationType.JSON;
//            type = doesExtendContext(oldContext.size(), new ArrayList<LPWithParams>(), props) ? FormIntegrationType.JSON : FormIntegrationType.TABLE;

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LCPWithParams(resI));
        }
        paramsList.addAll(exprs);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        if(rootProperty != null) {
            paramsList.add(rootProperty);
        }
        if(tagProperty != null) {
            paramsList.add(tagProperty);
        }

//        ImList<Type> exprTypes = getTypesForExportProp(exprs, newContext);

        List<Object> resultParams = getParamsPlainList(paramsList);
        LAP result = null;
        try {
            result = addExportPropertyAProp(LocalizedString.NONAME, type, resultInterfaces.size(), exIds, exLiterals, orders, targetProp,
                    whereProperty != null, rootProperty != null ? rootProperty.getLP().property : null, tagProperty != null ? tagProperty.getLP().property : null,
                    separator, noHeader, noEscape, charset, attr, resultParams.toArray());
        } catch (FormEntity.AlreadyDefined alreadyDefined) {
            throwAlreadyDefinePropertyDraw(alreadyDefined);
        }
        return new LAPWithParams(result, resultInterfaces);
    }

    // always from LPWithParams where usedParams is ordered set
    public static ImOrderSet<String> getUsedNames(List<TypedParameter> context, List<Integer> usedParams) {
        MOrderExclSet<String> mResult = SetFact.mOrderExclSet(usedParams.size());
        for (int usedIndex : usedParams) {
            mResult.exclAdd(context.get(usedIndex).paramName);
        }
        return mResult.immutableOrder();
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

    public LAPWithParams addScriptedNewThreadActionProperty(LAPWithParams actionProp, LCPWithParams connectionProp, LCPWithParams periodProp, LCPWithParams delayProp) {
        List<LPWithParams> propParams = BaseUtils.<LPWithParams>toList(actionProp);
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
        return new LAPWithParams(property, allParams);
    }

    public LAPWithParams addScriptedNewExecutorActionProperty(LAPWithParams actionProp, LCPWithParams threadsProp) {
        List<LPWithParams> propParams = Arrays.asList(actionProp, threadsProp);
        List<Integer> allParams = mergeAllParams(propParams);
        LAP<?> property = addNewExecutorAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LAPWithParams(property, allParams);
    }

    private ImList<LCP> findLCPsForImport(List<PropertyUsage> propUsages, ImList<ValueClass> paramClasses) throws ScriptingErrorLog.SemanticErrorException {
        MList<LCP> mProps = ListFact.mList(propUsages.size());
        for (PropertyUsage propUsage : propUsages) {
            mProps.add(findLCPParamByPropertyUsage(propUsage, paramClasses));
        }
        return mProps.immutableList();
    }

    private ImList<LCP> genLCPsForImport(List<TypedParameter> oldContext, List<TypedParameter> newContext, ImList<ValueClass> paramClasses) throws ScriptingErrorLog.SemanticErrorException {
        int size=newContext.size() - oldContext.size() - paramClasses.size();

        MList<LCP> mResult = ListFact.mList(size);
        for(int i=size-1;i>=0;i--)
            mResult.add(new LCP<>(DerivedProperty.createImportDataProp(newContext.get(newContext.size() - 1 - i).cls, paramClasses)));
        return mResult.immutableList();
    }

    private ImList<LCP> findLCPsIntegerParamByPropertyUsage(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        MList<LCP> mProps = ListFact.mList(propUsages.size());
        for (PropertyUsage propUsage : propUsages) {
            mProps.add(findLCPIntegerParamByPropertyUsage(propUsage));
        }
        return mProps.immutableList();
    }

    private LCP findLCPIntegerParamByPropertyUsage(PropertyUsage propUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPParamByPropertyUsage(propUsage, ListFact.singleton((ValueClass) IntegerClass.instance));
    }

    private LCP findLCPStringParamByPropertyUsage(PropertyUsage propUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLCPParamByPropertyUsage(propUsage, ListFact.singleton((ValueClass) StringClass.text));
    }

    private LCP findLCPParamByPropertyUsage(PropertyUsage propUsage, ImList<ValueClass> valueClasses) throws ScriptingErrorLog.SemanticErrorException {
        if (propUsage.classNames == null) {
            propUsage.classNames = new ArrayList<>();
            for (ValueClass valueClass : valueClasses) {
                propUsage.classNames.add(valueClass.getParsedName());
            }
        }
        LCP<?> lcp = findLCPByPropertyUsage(propUsage);
        ValueClass[] paramClasses = lcp.getInterfaceClasses(ClassType.signaturePolicy);
        if (paramClasses.length != valueClasses.size()) {
            errLog.emitPropertyWithParamsExpectedError(getParser(), propUsage.name, getParamClasses(valueClasses));
        } else {
            for (int i = 0; i < paramClasses.length; i++) {
                ValueClass paramClass = paramClasses[i];
                ValueClass valueClass = valueClasses.get(i);
                if (!valueClass.isCompatibleParent(paramClass) && !paramClass.isCompatibleParent(valueClass)) {
                    errLog.emitPropertyWithParamsExpectedError(getParser(), propUsage.name, getParamClasses(valueClasses));
                }
            }
        }
        return lcp;
    }

    private String getParamClasses(ImList<ValueClass> valueClasses) {
        List<ResolveClassSet> signature = new ArrayList<>();
        for(ValueClass valueClass : valueClasses) {
            signature.add(valueClass.getResolveSet());
        }
        return PropertyCanonicalNameUtils.createSignature(signature);
    }

    private FormIntegrationType adjustImportFormatFromFileType(FormIntegrationType format, LCPWithParams fileProp, OrderedMap<GroupObjectEntity, LCPWithParams> fileProps, List<TypedParameter> context) {
        if(format == null) {
            if(fileProps != null && !fileProps.isEmpty())
                fileProp = fileProps.values().iterator().next();
            Type type = getTypeByParamProperty(fileProp, context);
            if(type instanceof StaticFormatFileClass)
                return ((StaticFormatFileClass)type).getIntegrationType();
        }
        return format;
    }

    public LAPWithParams addScriptedImportActionProperty(FormIntegrationType format, LCPWithParams fileProp, List<String> ids, List<Boolean> literals, List<PropertyUsage> propUsages,
                                                         List<Boolean> nulls, LAPWithParams doAction, LAPWithParams elseAction, List<TypedParameter> context, List<TypedParameter> newContext,
                                                         PropertyUsage wherePropertyUsage, LCPWithParams sheet, boolean sheetAll, String separator, boolean noHeader, boolean noEscape,
                                                         String charset, LCPWithParams root, List<TypedParameter> fieldParams, List<String> toParamClasses, boolean attr,
                                                         LCPWithParams whereProp, LCPWithParams memoProp) throws ScriptingErrorLog.SemanticErrorException {

        if(fileProp == null)
            fileProp = new LCPWithParams(baseLM.importFile);

        if(toParamClasses != null && toParamClasses.size() > 1) {
            errLog.emitSimpleError(parser, "IMPORT TO/FIELDS params with multiple classes not supported");
        }

        format = adjustImportFormatFromFileType(format, fileProp, null, context);

        ImList<LCP> props;
        ImList<ValueClass> paramClasses;
        if(fieldParams != null) { // FIELDS
            paramClasses = getValueClassesFromTypedParams(fieldParams);
            props = genLCPsForImport(context, newContext, paramClasses);
        } else { // TO
            paramClasses = findClasses(toParamClasses);
            props = findLCPsForImport(propUsages, paramClasses);
        }

        boolean noParams = paramClasses.isEmpty();

        LCP<?> whereLCP;
        if(fieldParams != null) { // FIELDS
            assert wherePropertyUsage == null;
            whereLCP = !noParams ? new LCP<>(DerivedProperty.createImportDataProp(LogicalClass.instance, paramClasses)) : null;
        } else { // TO
            if(wherePropertyUsage != null)
                whereLCP = findLCPByPropertyUsage(wherePropertyUsage);
            else
                whereLCP = findLCPByPropertyUsage(new PropertyUsage("imported", toParamClasses), false, true);
        }

        List<LCPWithParams> params = new ArrayList<>();
        params.add(fileProp);
        if(root != null)
            params.add(root);
        if(whereProp != null)
            params.add(whereProp);
        if(memoProp != null)
            params.add(memoProp);
        if(sheet != null)
            params.add(sheet);

        LAP importAction = null;
        try {
            importAction = addImportPropertyAProp(format, params.size(), ids, literals, paramClasses, whereLCP, separator, noHeader, noEscape, charset, sheetAll, attr, whereProp != null, getUParams(props.toArray(new LCP[props.size()])));
        } catch (FormEntity.AlreadyDefined alreadyDefined) {
            throwAlreadyDefinePropertyDraw(alreadyDefined);
        }
        return proceedImportDoClause(noParams, doAction, elseAction, context, newContext, whereLCP, props, nulls != null ? ListFact.fromJavaList(nulls) : null, addScriptedJoinAProp(importAction, params));
    }

    public ImList<ValueClass> findClasses(List<String> classNames) throws ScriptingErrorLog.SemanticErrorException {
        MList<ValueClass> mResult = ListFact.mList(classNames.size()); // exception 
        for(String className : classNames)
            mResult.add(findClass(className));
        return mResult.immutableList();
    }

    public LAPWithParams addScriptedImportFormActionProperty(FormIntegrationType format, List<TypedParameter> context, LCPWithParams fileProp, OrderedMap<GroupObjectEntity, LCPWithParams> fileProps,
                                                             FormEntity formEntity, LCPWithParams sheet, boolean sheetAll, boolean noHeader, boolean noEscape, boolean attr, String charset, String separator,
                                                             LCPWithParams rootProp, LCPWithParams whereProp,  LCPWithParams memoProp) throws ScriptingErrorLog.SemanticErrorException {
        format = adjustImportFormatFromFileType(format, fileProp, fileProps, context);

        List<LCPWithParams> params = new ArrayList<>();
        boolean hasFileProps = fileProps != null && !fileProps.isEmpty();
        boolean isPlain = format != null ? format.isPlain() : hasFileProps;
        if(isPlain) {
            if(hasFileProps) {
                for(LCPWithParams fProp : fileProps.values()) {
                    checks.checkImportFromFileExpression(fProp);
                    params.add(fProp);
                }
            } else if(fileProp != null) {
                errLog.emitSimpleError(parser, String.format("IMPORT %s FROM single file not supported", format));
            } else {
                errLog.emitSimpleError(parser, "Input file(s) for import not specified");
            }
        } else {
            if(hasFileProps) {
                errLog.emitSimpleError(parser, String.format("IMPORT %s FROM multiple files not supported", format));
            } else {
                if (fileProp == null) fileProp = new LCPWithParams(baseLM.importFile);
                checks.checkImportFromFileExpression(fileProp);
                params.add(fileProp);
            }
        }

        if(attr)
            errLog.emitSimpleError(parser, "IMPORT form with ATTR not supported");
        if(whereProp != null)
            errLog.emitSimpleError(parser, "IMPORT form with WHERE not supported");
        if(memoProp != null)
            errLog.emitSimpleError(parser, "IMPORT form with MEMO not supported");

        if(rootProp != null)
            params.add(rootProp);
        if(sheet != null)
            params.add(sheet);

        ImOrderSet<GroupObjectEntity> groupFiles = fileProps != null ? SetFact.fromJavaOrderSet(fileProps.keyList()) : SetFact.<GroupObjectEntity>EMPTYORDER();
        return addScriptedJoinAProp(addImportFAProp(format, formEntity, params.size(), groupFiles, sheetAll, separator, noHeader, noEscape, charset, whereProp != null), params);
    }

    public LCP addTypeProp(ValueClass valueClass, boolean bIs) {
        if (bIs) {
            return is(valueClass);
        } else {
            return object(valueClass);
        }
    }

    public LCPWithParams addScriptedTypeProp(LCPWithParams ccProp, String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addTypeProp(findClass(className), bIs), Collections.singletonList(ccProp));
    }

    public void addScriptedConstraint(LCP<?> property, Event event, boolean checked, List<PropertyUsage> propUsages, LCP<?> messageProperty, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (!property.property.checkAlwaysNull(true)) {
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
        addConstraint(property, messageProperty, type, checkedProps, event, this, debugPoint);
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

    public LCPWithParams addScriptedSessionProp(IncrementType type, LCPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkSessionPropertyParameter(property);
        LCP newProp;
        PrevScope scope = (type == null ? PrevScope.DB : (prevScope != null ? prevScope : PrevScope.EVENT)); // по сути оптимизация если scope известен использовать его
        if (type == null) {
            newProp = addOldProp(property.getLP(), scope);
        } else {
            newProp = addCHProp(property.getLP(), type, scope);
        }
        return new LCPWithParams(newProp, property);
    }

    public LCPWithParams addScriptedSignatureProp(LCPWithParams property) {
        LCP newProp = addClassProp(property.getLP());
        return new LCPWithParams(newProp, property);
    }

    public LCPWithParams addScriptedActiveTabProp(ComponentView component) {
        return new LCPWithParams(new LCP<>(component.getActiveTab().property));
    }

    public void addScriptedFollows(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, List<PropertyFollowsDebug> resolveOptions, LCPWithParams rightProp, Event event, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        LCP mainProp = findLCPByPropertyUsage(mainPropUsage, namedParams);
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        addScriptedFollows(mainProp, rightProp, resolveOptions, event, debugPoint);
    }

    private void addScriptedFollows(LCP mainProp, LCPWithParams rightProp, List<PropertyFollowsDebug> resolveOptions, Event event, DebugInfo.DebugPoint debugPoint) {
        Integer[] params = new Integer[rightProp.usedParams.size()];
        for (int j = 0; j < params.length; j++) {
            params[j] = rightProp.usedParams.get(j) + 1;
        }
        follows(mainProp, debugPoint, ListFact.fromJavaList(resolveOptions), event, rightProp.getLP(), params);
    }

    public void addScriptedWriteWhen(PropertyUsage mainPropUsage, List<TypedParameter> namedParams, LCPWithParams valueProp, LCPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        LCP mainProp = findLCPByPropertyUsage(mainPropUsage, namedParams);
        if (!(mainProp.property instanceof DataProperty)) {
            errLog.emitOnlyDataPropertyIsAllowedError(parser, mainPropUsage.name);
        }
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        mainProp.setEventChange(this, action, params.toArray());
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

    public void addScriptedEvent(LCPWithParams whenProp, LAPWithParams event, List<LCPWithParams> orders, boolean descending, Event baseEvent, List<LCPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint) {
        if(noInline==null) {
            noInline = new ArrayList<>();
            for(Integer usedParam : whenProp.usedParams)
                noInline.add(new LCPWithParams(usedParam));
        }
        List<Object> params = getParamsPlainList(asList(event, whenProp), orders, noInline);
        addEventAction(baseEvent, descending, false, noInline.size(), forceInline, debugPoint, params.toArray());
    }

    public void addScriptedGlobalEvent(LAPWithParams event, Event baseEvent, boolean single, ActionOrPropertyUsage showDep) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkEventNoParameters(event.getLP());
        ActionProperty action = event.getLP().property;
        if(showDep!=null)
            action.showDep = findLPByActionOrPropertyUsage(showDep).property;
        addBaseEvent(action, baseEvent, false, single);
    }

    public void addScriptedShowDep(ActionOrPropertyUsage property, ActionOrPropertyUsage propFrom) throws ScriptingErrorLog.SemanticErrorException {
        findLPByActionOrPropertyUsage(property).property.showDep = findLPByActionOrPropertyUsage(propFrom).property;
    }

    public void addScriptedAspect(PropertyUsage mainPropUsage, List<TypedParameter> mainPropParams, LAPWithParams actionProp, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        LAP mainProp = findLAPByPropertyUsage(mainPropUsage, mainPropParams);
        checks.checkParamCount(mainProp, mainPropParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(mainPropParams));

        LAP<PropertyInterface> mainActionLP = (LAP<PropertyInterface>) mainProp;

        List<Object> params = getParamsPlainList(Collections.singletonList(actionProp));
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(mainActionLP.listInterfaces, params.toArray());
        addAspectEvent(mainActionLP.property, actionImplements.get(0), before);
    }

    public void addScriptedTable(String name, List<String> classIds, boolean isFull, boolean isExplicit) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateTable(name);

        // todo [dale]: Hack. Class CustomObjectClass is created after all in InitObjectClassTask 
        boolean isCustomObjectClassTable = isCustomObjectClassTable(name, classIds);

        ValueClass[] classes = new ValueClass[classIds.size()];
        if (!isCustomObjectClassTable(name, classIds)) {
            for (int i = 0; i < classIds.size(); i++) {
                classes[i] = findClass(classIds.get(i));
            }
        }
        
        tempTables.add(new TemporaryTableInfo(name, classes, isFull, isExplicit, isCustomObjectClassTable));
    }

    private boolean isCustomObjectClassTable(String name, List<String> classIds) {
        return classIds.size() == 1 && classIds.get(0).equals("CustomObjectClass");
    }
    
    private void addScriptedTables() {
        for (TemporaryTableInfo info : tempTables) {
            ValueClass[] classes = info.classes;
            if (info.isCustomObjectClassTable) {
                classes = new ValueClass[] {baseLM.baseClass.objectClass};
            }
            addTable(info.name, info.isFull, info.isExplicit, classes);
        }
        tempTables.clear(); 
    } 
    
    private List<TemporaryTableInfo> tempTables = new ArrayList<>();
    
    private static class TemporaryTableInfo {
        public final String name;
        public final ValueClass[] classes;
        public final boolean isFull, isExplicit;
        public final boolean isCustomObjectClassTable;
        
        public TemporaryTableInfo(String name, ValueClass[] classes, boolean isFull, boolean isExplicit, boolean isCustomObjectClassTable) {
            this.name = name;
            this.classes = classes;
            this.isFull = isFull;
            this.isExplicit = isExplicit;
            this.isCustomObjectClassTable = isCustomObjectClassTable;  
        }
    }  
    
    private List<LCP> indexedProperties = new ArrayList<>();
    private List<TemporaryIndexInfo> tempIndicies = new ArrayList<>();
            
    public void addScriptedIndex(LCP lp) {
        indexedProperties.add(lp);

        ImSet<StoredDataProperty> fullAggrProps;
        if(lp.property instanceof AggregateGroupProperty && (fullAggrProps = ((AggregateGroupProperty) lp.property).getFullAggrProps()) != null) {
            for(StoredDataProperty fullAggrProp : fullAggrProps)
                indexedProperties.add(new LCP<>(fullAggrProp));
        }
    }

    public LCPWithParams findIndexProp(PropertyUsage toPropertyUsage, List<LCPWithParams> toPropertyMapping, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        LCP toPropertyLP = findLCPByPropertyUsage(toPropertyUsage, toPropertyMapping, context);
        return new LCPWithParams(toPropertyLP, getParamsAssertList(toPropertyMapping));
    }

    public void addScriptedIndex(List<TypedParameter> params, List<LCPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkIndexNecessaryProperty(lps);
        checks.checkStoredProperties(lps);
        checks.checkDistinctParametersList(lps);
        checks.checkIndexNumberOfParameters(params.size(), lps);
        ImOrderSet<String> keyNames = ListFact.fromJavaList(params).toOrderExclSet().mapOrderSetValues(new GetValue<String, TypedParameter>() {
            public String getMapValue(TypedParameter value) {
                return value.paramName;
            }});
        tempIndicies.add(new TemporaryIndexInfo(keyNames, getParamsPlainList(lps).toArray()));
    }

    private static class TemporaryIndexInfo {
        public ImOrderSet<String> keyNames;
        public Object[] params;
        
        public TemporaryIndexInfo(ImOrderSet<String> keyNames, Object[] params) {
            this.keyNames = keyNames;
            this.params = params;
        }
    }

    public void addScriptedLoggable(List<PropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        for (PropertyUsage propUsage : propUsages) {
            LCP lp = findLCPByPropertyUsage(propUsage);
            lp.makeLoggable(this, BL.systemEventsLM);
        }
    }

    public void addScriptedWindow(WindowType type, String name, LocalizedString captionStr, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateWindow(name);

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
            errLog.emitWindowPositionNotSpecifiedError(parser, name);
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
            errLog.emitWindowPositionConflictError(parser, name);
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
                                                           PropertyUsage actionUsage, String formName, boolean isAction) throws ScriptingErrorLog.SemanticErrorException {
        LAP<?> action = null;
        FormEntity form = null;
        if (formName != null) {
            form = findForm(formName);
        }
        if (actionUsage != null) {
            if(!isAction)
                form = findForm(actionUsage.name, true);
            if(form == null)
                action = findNavigatorAction(actionUsage);
        }

        if (name == null) {
            name = createDefaultNavigatorElementName(action, form);
        }

        checks.checkNavigatorElementName(name);
        checks.checkDuplicateNavigatorElement(name);

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
            return form.getName();
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

    private NavigatorElement createNavigatorElement(String canonicalName, LocalizedString caption, DebugInfo.DebugPoint point, LAP<?> action, FormEntity form) {
        NavigatorElement newElement;
        if (form != null) {
            newElement = addNavigatorForm(form, canonicalName, caption);
        } else if (action != null) {
            newElement = addNavigatorAction(action, canonicalName, caption);
        } else {
            newElement = addNavigatorFolder(canonicalName, caption);
        }
        newElement.setDebugPoint(point);
        return newElement;
    }

    private LAP<?> findNavigatorAction(PropertyUsage actionUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert actionUsage != null;
        if (actionUsage.classNames == null) {
            actionUsage.classNames = Collections.emptyList();
        }
        LAP<?> action = findLAPNoParamsByPropertyUsage(actionUsage);
        checks.checkNavigatorAction(action);
        return action;
    }

    public void setupNavigatorElement(NavigatorElement element, LocalizedString caption, NavigatorElement parentElement, NavigatorElementOptions options, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        if (caption != null) {
            element.caption = caption;
        }

        applyNavigatorElementOptions(element, parentElement, options, isEditOperation);
    }

    public void applyNavigatorElementOptions(NavigatorElement element, NavigatorElement parent, NavigatorElementOptions options, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        setNavigatorElementWindow(element, options.windowName);
        setNavigatorElementImage(element, parent, options.imagePath);

        if (parent != null && (!isEditOperation || options.position != InsertPosition.IN)) {
            moveElement(element, parent, options.position, options.anchor, isEditOperation);
        }
    }

    private void moveElement(NavigatorElement element, NavigatorElement parentElement, InsertPosition pos, NavigatorElement anchorElement, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();
        checks.checkNavigatorElementMoveOperation(element, parentElement, anchorElement, isEditOperation, version);

        switch (pos) {
            case IN:    parentElement.add(element, version); break;
            case BEFORE:parentElement.addBefore(element, anchorElement, version); break;
            case AFTER: parentElement.addAfter(element, anchorElement, version); break;
            case FIRST: parentElement.addFirst(element, version); break;
        }
    }

    public void setNavigatorElementWindow(NavigatorElement element, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        assert element != null;

        if (windowName != null) {
            AbstractWindow window = findWindow(windowName);

            if (window instanceof NavigatorWindow) {
                element.window = (NavigatorWindow) window;
            } else {
                errLog.emitAddToSystemWindowError(parser, element.getName(), windowName);
            }
        }
    }

    public void setNavigatorElementImage(NavigatorElement element, NavigatorElement parent, String imagePath) {
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

    public void actionPropertyDefinitionBodyCreated(LAPWithParams lpWithParams, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate) {
        if (lpWithParams.getLP() != null) {
            setDebugInfo(lpWithParams, startPoint, endPoint, modifyContext, needToCreateDelegate);
        }
    }

    public static void setDebugInfo(LAPWithParams lpWithParams, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate) {
        //noinspection unchecked
        LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.getLP();
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

    public void topContextActionPropertyDefinitionBodyCreated(LAPWithParams lpWithParams) {
        boolean isDebug = debugger.isEnabled();

        if(isDebug) {
            //noinspection unchecked
            LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.getLP();

            ActionProperty property = lAction.property;

            debugger.setNewDebugStack(property);
        }
    }

    public LAPWithParams modifyContextFlowActionPropertyDefinitionBodyCreated(LAPWithParams lpWithParams,
                                                                             List<TypedParameter> newContext, List<TypedParameter> oldContext,
                                                                             boolean needFullContext) {
        boolean isDebug = debugger.isEnabled();

        if(isDebug || needFullContext) {
            lpWithParams = patchExtendParams(lpWithParams, newContext, oldContext);
        }

        if (isDebug) {
            //noinspection unchecked
            LAP<PropertyInterface> lAction = (LAP<PropertyInterface>) lpWithParams.getLP();

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
    private LAPWithParams patchExtendParams(LAPWithParams lpWithParams, List<TypedParameter> newContext, List<TypedParameter> oldContext) {

        if(!lpWithParams.getLP().listInterfaces.isEmpty() && lpWithParams.usedParams.isEmpty()) {
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
            allCreationParams.add(new LCPWithParams(i));
        }

        List<Object> resultParams = getParamsPlainList(allCreationParams);
        LAP wrappedLAP = addListAProp(newContext.size() - oldContext.size(), resultParams.toArray());

        List<Integer> wrappedUsed = mergeAllParams(allCreationParams);
        return new LAPWithParams(wrappedLAP, wrappedUsed);
    }

    public void checkPropertyValue(LCP property) {
        checks.checkPropertyValue(property, alwaysNullProperties);
    }

    public void checkCIInExpr(LCPContextIndependent ci) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkCIInExpr(ci);
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
        for (String requiredModuleName : module.getRequiredNames()) {
            LogicsModule requiredModule = BL.getSysModule(requiredModuleName);
            assert requiredModule != null;
            if (!visitedModules.contains(requiredModule)) {
                initNamespacesToModules(requiredModule, visitedModules);
            }
        }
    }

    private void showWarnings() {
        for (String warningText : warningList) {
            systemLogger.warn("WARNING!" + warningText);
        }
    }

    public interface AbstractFormPropertyUsage {
    }

    public static abstract class FormPropertyUsage implements AbstractFormPropertyUsage {
        public List<String> mapping;

        public FormPropertyUsage(List<String> mapping) {
            this.mapping = mapping;
        }
        
        public void setMapping(List<String> mapping) { // need this because in formMappedProperty mapping is parsed after usage
            this.mapping = mapping;
        }
    }

    public interface AbstractFormCalcPropertyUsage extends AbstractFormPropertyUsage { // lcp or calc
    }

    public interface AbstractFormActionPropertyUsage extends AbstractFormPropertyUsage { // lap or calc
    }

    public static abstract class FormLPUsage<L extends LP> implements AbstractFormPropertyUsage {
        public final L lp;
        public final List<ResolveClassSet> signature;
        public final ImOrderSet<String> mapping;

        public FormLPUsage(L lp, ImOrderSet<String> mapping) {
            this(lp, mapping, null);            
        }
        public FormLPUsage(L lp, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            this.lp = lp;
            this.signature = signature;
            this.mapping = mapping;
        }
    }

    public static class FormLCPUsage extends FormLPUsage<LCP> implements AbstractFormCalcPropertyUsage {
        public FormLCPUsage(LCP lp, ImOrderSet<String> mapping) {
            super(lp, mapping);
        }

        public FormLCPUsage(LCP lp, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            super(lp, mapping, signature);
        }
    }

    public static class FormLAPUsage extends FormLPUsage<LAP> implements AbstractFormActionPropertyUsage {
        public FormLAPUsage(LAP lp, ImOrderSet<String> mapping) {
            super(lp, mapping);
        }

        public FormLAPUsage(LAP lp, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            super(lp, mapping, signature);
        }
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

    public static class FormPredefinedUsage extends FormPropertyUsage {
        public final PropertyUsage property;

        public FormPredefinedUsage(PropertyUsage property, List<String> mapping) {
            super(mapping);
            this.property = property;
        }
    }
    
    public abstract static class FormActionOrPropertyUsage<U extends ActionOrPropertyUsage> extends FormPropertyUsage {
        public final U usage;

        public FormActionOrPropertyUsage(U usage, List<String> mapping) {
            super(mapping);
            this.usage = usage;
        }
    }

    public static class FormCalcPropertyUsage extends FormActionOrPropertyUsage implements AbstractFormCalcPropertyUsage {
        public FormCalcPropertyUsage(PropertyUsage property, List<String> mapping) {
            this(new CalcPropertyUsage(property), mapping);
        }
        public FormCalcPropertyUsage(CalcPropertyUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public static class FormPropertyElseActionUsage extends FormActionOrPropertyUsage {
        public FormPropertyElseActionUsage(PropertyElseActionUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public static class FormActionPropertyUsage extends FormActionOrPropertyUsage implements AbstractFormActionPropertyUsage {
        public FormActionPropertyUsage(PropertyUsage property, List<String> mapping) {
            this(new ActionPropertyUsage(property), mapping);
        }
        public FormActionPropertyUsage(ActionPropertyUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public abstract static class ActionOrPropertyUsage {
        public final PropertyUsage property;

        public ActionOrPropertyUsage(PropertyUsage property) {
            this.property = property;
        }
        
        public abstract FormActionOrPropertyUsage createFormUsage(List<String> mapping);
    }

    public static class CalcPropertyUsage extends ActionOrPropertyUsage {
        public CalcPropertyUsage(PropertyUsage property) {
            super(property);
        }

        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormCalcPropertyUsage(this, mapping);
        }
    }

    public static class PropertyElseActionUsage extends ActionOrPropertyUsage {
        public PropertyElseActionUsage(PropertyUsage property) {
            super(property);
        }

        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormPropertyElseActionUsage(this, mapping);
        }
    }

    public static class ActionPropertyUsage extends ActionOrPropertyUsage {
        public ActionPropertyUsage(PropertyUsage property) {
            super(property);
        }

        @Override
        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormActionPropertyUsage(this, mapping);
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
        public final LCPWithParams in;
        public final Boolean inNull;

        public final boolean out;
        public final Integer outParamNum;
        public final Boolean outNull;
        public final PropertyUsage outProp;

        public final LCPWithParams changeProp;

        public final boolean assign;
        public final DebugInfo.DebugPoint assignDebugPoint;
        public final boolean constraintFilter;


        public FormActionProps(LCPWithParams in, Boolean inNull, boolean out, Integer outParamNum, Boolean outNull, PropertyUsage outProp, boolean constraintFilter, boolean assign, LCPWithParams changeProp, DebugInfo.DebugPoint changeDebugPoint) {
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
