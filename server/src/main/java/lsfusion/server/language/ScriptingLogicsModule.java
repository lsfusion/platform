package lsfusion.server.language;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.action.MessageClientType;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.navigator.NavigatorScheduler;
import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.ResourceUtils;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.table.IndexType;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.action.ActionSettings;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.converters.KeyStrokeConverter;
import lsfusion.server.language.form.FormPropertyOptions;
import lsfusion.server.language.form.ScriptingFormEntity;
import lsfusion.server.language.form.design.ScriptingFormView;
import lsfusion.server.language.metacode.MetaCodeFragment;
import lsfusion.server.language.navigator.window.BorderPosition;
import lsfusion.server.language.navigator.window.DockPosition;
import lsfusion.server.language.navigator.window.NavigatorWindowOptions;
import lsfusion.server.language.navigator.window.Orientation;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.PropertySettings;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.language.property.oraction.MappedActionOrProperty;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.ExplicitAction;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.*;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseContainerAction;
import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseType;
import lsfusion.server.logics.form.interactive.action.focus.ActivateAction;
import lsfusion.server.logics.form.interactive.action.focus.IsActiveFormAction;
import lsfusion.server.logics.form.interactive.action.input.InputContextAction;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputPropertyListEntity;
import lsfusion.server.logics.form.interactive.action.lifecycle.CloseFormAction;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.open.MappedForm;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.FormSelectTop;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.CCCContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.Union;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.data.FormulaJoinProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.AggregateGroupProperty;
import lsfusion.server.logics.property.set.Cycle;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.interpreter.action.EvalAction;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.ReflectionPropertyType;
import lsfusion.server.physics.admin.reflection.property.CanonicalNameProperty;
import lsfusion.server.physics.dev.debug.*;
import lsfusion.server.physics.dev.debug.action.ShowRecDepAction;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.ClassCanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.PropertyCompoundNameParser;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors.ResolvingError;
import lsfusion.server.physics.dev.integration.external.to.*;
import lsfusion.server.physics.dev.integration.external.to.file.ReadAction;
import lsfusion.server.physics.dev.integration.external.to.file.WriteAction;
import lsfusion.server.physics.dev.integration.external.to.mail.SendEmailAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.janino.SimpleCompiler;
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.language.ScriptedStringUtils.*;
import static lsfusion.server.language.navigator.window.AlignmentUtils.*;
import static lsfusion.server.logics.classes.data.StringClass.getv;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.*;

public class ScriptingLogicsModule extends LogicsModule {

    private static final Logger systemLogger = ServerLoggers.systemLogger;

    protected final BusinessLogics BL;

    private final String code;
    private String path = null;
    protected final ScriptingErrorLog errLog;
    protected ScriptParser parser;
    protected ScriptingLogicsModuleChecks checks;
    private final List<String> warningList = new ArrayList<>();
    private final Map<Property<?>, String> alwaysNullProperties = new HashMap<>();

    private String lastOptimizedJPropSID = null;

    public enum ConstType { STATIC, INT, REAL, NUMERIC, STRING, RSTRING, LOGICAL, TLOGICAL, LONG, DATE, DATETIME, TIME, COLOR, NULL }
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE, NATIVE}
    public static class GroupingType {
        public static final GroupingType SUM = new GroupingType();
        public static final GroupingType MAX = new GroupingType();
        public static final GroupingType MIN = new GroupingType();
        public static final GroupingType CONCAT = new GroupingType();
        public static final GroupingType AGGR = new GroupingType();
        public static final GroupingType EQUAL = new GroupingType();
        public static final GroupingType LAST = new GroupingType();
        public static final GroupingType NAGGR = new GroupingType();

        public int getSkipWhereIndex() {
            return this == LAST ? 1 : -1;
        }
    }
    public static class CustomGroupingType extends GroupingType {
        public final String aggrFunc;
        public final boolean setOrdered;

        public final DataClass dataClass;
        public final boolean valueNull;

        public CustomGroupingType(String aggrFunc, boolean setOrdered, DataClass dataClass, boolean valueNull) {
            this.aggrFunc = aggrFunc;
            this.setOrdered = setOrdered;

            this.dataClass = dataClass;
            this.valueNull = valueNull;
        }
    }

   public ScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL, String lsfPath) {
        this(ResourceUtils.findResourceAsString(lsfPath, false, false, null, null), baseModule, BL);
        this.path = lsfPath;
    }

    protected ScriptingLogicsModule(String code, BaseLogicsModule baseModule, BusinessLogics BL) {
        assert code != null;
        
        setBaseLogicsModule(baseModule);
        this.BL = BL;
        this.code = code;
        
        errLog = new ScriptingErrorLog();
        errLog.setModuleId(getIdentifier());
        
        parser = new ScriptParser();
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
    public void initTables(DBNamingPolicy namingPolicy) throws RecognitionException {
        addScriptedTables(namingPolicy);
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
    public void initIndexes(DBManager dbManager) throws RecognitionException {
        for (TemporaryIndexInfo info : tempIndexes) {
            checkIndexDifferentTables(info.params);
            dbManager.addIndex(info.keyNames, info.dbName, info.indexType, info.params);
        }
        tempIndexes.clear();

        for (int i = 0; i < indexedProperties.size(); i++) {
            dbManager.addIndex(indexedProperties.get(i), indexNames.get(i), indexTypes.get(i));
        }
        indexedProperties.clear();
    }

    public void checkIndexDifferentTables(Object[] params) throws ScriptingErrorLog.SemanticErrorException {
        ImplementTable table = null;
        String firstProperty = null;
        for (Object param : params) {
            if (param instanceof LP) {
                Property<?> property = ((LP)param).property;
                String name = property.toString();
                if (table == null) {
                    table = property.mapTable.table;
                    firstProperty = property.toString();
                } else if (table != property.mapTable.table) {
                    errLog.emitIndexPropertiesDifferentTablesError(parser, firstProperty, name);
                }
            }
        }
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

    public String getErrorsDescription() {
        return errLog.toString();
    }

    public interface InitRunnable {
        void run(ScriptingLogicsModule module) throws RecognitionException, FileNotFoundException;
    }

    public void runInit(InitRunnable runnable) {
        try {
            runnable.run(this);
        } catch (Exception e) {
            String errString = getErrorsDescription();
            if (e instanceof RecognitionException || !errString.isEmpty())
                throw new ScriptParsingException(errString + e.getMessage());
            throw Throwables.propagate(e);
        }

        // in theory when there are syntax errors, they can be recovered and there will be no exception 
        String errString = getErrorsDescription();
        if(!errString.isEmpty())
            throw new ScriptParsingException(errString);
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

    private CharStream createStream() {
        return new ANTLRStringStream(code);
    }

    @Override
    @IdentityLazy
    public int getModuleComplexity() {
        return createStream().size();
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

    public static String removeCarriageReturn(String s) {
        return s.replace("\r\n", "\n");
    }

    public String transformStringLiteral(String s) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return ScriptedStringUtils.transformStringLiteral(s, BL.getIdFromReversedI18NDictionaryMethod());
        } catch (ScriptedStringUtils.TransformationError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
        return null;
    }

    public String getRawStringLiteralText(String literalText) {
        if (literalText.charAt(1) == '\'') {
            return unquote(literalText.substring(1));
        } else {
            return unquote(literalText.substring(2, literalText.length() - 1));
        }
    }
    
    public LocalizedString getRawLocalizedStringLiteralText(String literalText) {
        return LocalizedString.create(getRawStringLiteralText(literalText), false);
    }
    
    public LocalizedString transformLocalizedStringLiteral(String s) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return ScriptedStringUtils.transformLocalizedStringLiteral(s, BL.getIdFromReversedI18NDictionaryMethod(), BL::appendEntryToBundle);
        } catch (TransformationError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        } catch (LocalizedString.FormatError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
        return null;
    }

    private String transformToResourceName(String s) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return ScriptedStringUtils.removeEscaping(s);
        } catch (ScriptedStringUtils.TransformationError e) {
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

    public MappedActionOrProperty getPropertyWithMapping(FormEntity form, AbstractFormActionOrPropertyUsage pDrawUsage, Result<Pair<ActionOrProperty, List<String>>> inherited) throws ScriptingErrorLog.SemanticErrorException {
        assert !(pDrawUsage instanceof FormPredefinedUsage);
        LAP<?, ?> property;
        ImOrderSet<String> mapping;
        if(pDrawUsage instanceof FormActionOrPropertyUsage) {
            List<String> usageMapping = ((FormActionOrPropertyUsage<?>) pDrawUsage).mapping;
            property = findLAPByActionOrPropertyUsage(((FormActionOrPropertyUsage) pDrawUsage).usage, form, usageMapping);

            ImList<String> uMapping = ListFact.fromJavaList(usageMapping);
            mapping = uMapping.toOrderSet();
            if(mapping.size() != usageMapping.size()) { // if we have "repeating" objects we're wrapping it into the JOIN / EXEC property
                if(inherited != null) {
                    assert property.getActionOrProperty().isNamed();
                    inherited.set(new Pair<>(property.getActionOrProperty(), usageMapping));
                }

                final ImOrderSet<String> fMapping = mapping;
                ImList<Integer> indexMapping = uMapping.mapListValues((String value) -> fMapping.indexOf(value) + 1);
                if(property instanceof LP)
                    property = addJProp((LP)property, indexMapping.toArray(new Integer[uMapping.size()]));
                else
                    property = addJoinAProp((LA)property, indexMapping.toArray(new Integer[uMapping.size()]));
            }
        } else {
            property = ((FormLAPUsage)pDrawUsage).lp;
            mapping = ((FormLAPUsage<?>) pDrawUsage).mapping;
//            assert inherited == null || !property.getActionOrProperty().isNamed();
        }

//        if (property.property.interfaces.size() != mapping.size()) {
//            getErrLog().emitParamCountError(parser, property, mapping.size());
//        }
        return new MappedActionOrProperty(property, getMappingObjectsArray(form, mapping));
    }

    public LP<?> findLPByPropertyUsage(NamedPropertyUsage pUsage, FormEntity form, List<String> mapping, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames != null)
            return findLPByPropertyUsage(nullIfNotFound, pUsage);
        List<ResolveClassSet> classes = getMappingClassesArray(form, mapping);
        return findLPByNameAndClasses(pUsage.name, pUsage.getSourceName(), classes, nullIfNotFound);
    }
    public LA<?> findLAByPropertyUsage(NamedPropertyUsage pUsage, FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByPropertyUsage(pUsage, form, mapping, false);
    }
    public LA<?> findLAByPropertyUsage(NamedPropertyUsage pUsage, FormEntity form, List<String> mapping, boolean orPropertyMessage) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames != null)
            return findLAByPropertyUsage(orPropertyMessage, pUsage);
        List<ResolveClassSet> classes = getMappingClassesArray(form, mapping);
        return findLAByNameAndClasses(pUsage.name, pUsage.getSourceName(), classes, orPropertyMessage);
    }

    public LAP<?, ?> findLAPByActionOrPropertyUsage(ActionOrPropertyUsage orUsage) throws ScriptingErrorLog.SemanticErrorException {
        NamedPropertyUsage pUsage = orUsage.property;
        if(orUsage instanceof PropertyUsage) {
            return findLPByPropertyUsage(pUsage);
        }
        if(orUsage instanceof ActionUsage) {
            return findLAByPropertyUsage(pUsage);
        }
        assert orUsage instanceof PropertyElseActionUsage;
        LAP<?, ?> result = findLPByPropertyUsage(true, pUsage);
        if(result == null)
            result = findLAByPropertyUsage(true, pUsage);
        return result;
    }
    public LAP<?, ?> findLAPByEventOrPropertyUsage(ActionOrPropertyUsage orUsage) throws ScriptingErrorLog.SemanticErrorException {
        NamedPropertyUsage pUsage = orUsage.property;
        if(orUsage instanceof PropertyUsage) {
            return findLPByPropertyUsage(pUsage);
        }
        if(orUsage instanceof ActionUsage) {
            return findLANoParamsByPropertyUsage(pUsage);
        }
        assert orUsage instanceof PropertyElseActionUsage;
        LAP<?, ?> result = findLPByPropertyUsage(true, pUsage);
        if(result == null)
            result = findLANoParamsByPropertyUsage(true, pUsage);
        return result;
    }
    public LAP<?, ?> findLAPByActionOrPropertyUsage(ActionOrPropertyUsage orUsage, FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        NamedPropertyUsage pUsage = orUsage.property;
        if(orUsage instanceof PropertyUsage) {
            return findLPByPropertyUsage(pUsage, form, mapping, false);
        }
        if(orUsage instanceof ActionUsage) {
            return findLAByPropertyUsage(pUsage, form, mapping);
        }
        assert orUsage instanceof PropertyElseActionUsage;
        LAP<?, ?> result = findLPByPropertyUsage(pUsage, form, mapping, true);
        if(result == null)
            result = findLAByPropertyUsage(pUsage, form, mapping, true);
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

    public void addScriptedClass(String className, LocalizedString captionStr, String image, boolean isAbstract,
                                 List<String> instNames, List<LocalizedString> instCaptions, List<String> images, List<String> parentNames, boolean isComplex,
                                 DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateClass(className);
        checks.checkStaticClassConstraints(isAbstract, instNames, instCaptions);
        checks.checkClassParents(parentNames);

        if(captionStr == null)
            captionStr = LocalizedString.create(BaseUtils.humanize(className));
        LocalizedString caption = captionStr;

        ImList<CustomClass> parents = BaseUtils.immutableCast(findClasses(parentNames));

        List<LocalizedString> captions = new ArrayList<>();
        for (int i = 0; i < instCaptions.size(); i++) {
            captions.add(instCaptions.get(i) == null ? LocalizedString.create(instNames.get(i)) : instCaptions.get(i));
        }

        CustomClass cls;
        if (isAbstract) {
            cls = addAbstractClass(className, caption, image, parents);
        } else {
            cls = addConcreteClass(className, caption, image, instNames, captions, images, parents);
        }
        cls.isComplex = isComplex;

        ClassDebugInfo debugInfo = new ClassDebugInfo(point);
        if (debugger.isEnabled() && point.needToCreateDelegate()) {
            debugger.addDelegate(debugInfo);
            cls.setDebugInfo(debugInfo);
        }
    }

    public void extendClass(String className, List<String> instNames, List<LocalizedString> instCaptions, List<String> images, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();

        CustomClass cls = (CustomClass) findClass(className);
        boolean isAbstract = cls instanceof AbstractCustomClass;

        List<String> names = instNames;
        List<LocalizedString> captions = instCaptions;
        if (!isAbstract) {
            ((ConcreteCustomClass) cls).addStaticObjects(instNames, instCaptions, images, version);

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

    public Group findGroup(String name) throws ScriptingErrorLog.SemanticErrorException {
        try {
            Group group = resolveGroup(name);
            checks.checkGroup(group, name);
            return group;
        } catch (ResolvingError e) {
            convertResolveError(e);
        }
        return null;
    }

    public LA<?> findAction(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyCompoundNameParser parser = new PropertyCompoundNameParser(this, name);
        return findLAByNameAndClasses(parser.propertyCompoundNameWithoutSignature(), name, parser.getSignature());
    }

    public LP<?> findProperty(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyCompoundNameParser parser = new PropertyCompoundNameParser(this, name);
        return findLPByNameAndClasses(parser.propertyCompoundNameWithoutSignature(), name, parser.getSignature());
    }

    public LP<?>[] findProperties(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LP<?>[] result = new LP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    public LP<?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(name, sourceName, params, false);
    }
    public LP<?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(name, sourceName, params, false, false, nullIfNotFound);
    }
    public LP<?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(name, sourceName, params, onlyAbstract, prioritizeNotEqual, false);
    }
    public LP<?> findLPByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> property = null;

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
    private LA<?> findLAByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByNameAndClasses(name, sourceName, params, false);
    }
    private LA<?> findLAByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean orPropertyMessage) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByNameAndClasses(name, sourceName, params, false, false, orPropertyMessage);
    }
    private LA<?> findLAByNameAndClasses(String name, String sourceName, List<ResolveClassSet> params, boolean onlyAbstract, boolean prioritizeNotEqual, boolean orPropertyMessage) throws ScriptingErrorLog.SemanticErrorException {
        LA<?> action = null;

        try {
            if (onlyAbstract) {
                action = resolveAbstractAction(name, params, prioritizeNotEqual);
            } else {
                action = resolveAction(name, params);
            }
        } catch (ResolvingErrors.ResolvingAmbiguousPropertyError e) {
            if (sourceName != null) {
                e.name = sourceName;
            }
            convertResolveError(e);
        } catch (ResolvingError e) {
            convertResolveError(e);
        }

        checks.checkAction(action, sourceName == null ? name : sourceName, params, orPropertyMessage);
        return action;
    }

    public LP<?> findLPByPropertyUsage(NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(pUsage, false);
    }
    public LP<?> findLPByPropertyUsage(boolean nullIfNotFound, NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(pUsage, false, nullIfNotFound);
    }

    public LA<?> findLAByPropertyUsage(NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByPropertyUsage(false, pUsage);
    }
    public LA<?> findLAByPropertyUsage(boolean orPropertyMessage, NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByPropertyUsage(pUsage, false, orPropertyMessage);
    }

    public LP<?> findLPByPropertyUsage(NamedPropertyUsage pUsage, boolean isAbstract) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(pUsage, isAbstract, false);
    }
    public LP<?> findLPByPropertyUsage(NamedPropertyUsage pUsage, boolean isAbstract, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByNameAndClasses(pUsage.name, pUsage.getSourceName(), getParamClasses(pUsage), isAbstract, false, nullIfNotFound);
    }
    public LA<?> findLAByPropertyUsage(NamedPropertyUsage pUsage, boolean isAbstract, boolean orPropertyMessage) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByNameAndClasses(pUsage.name, pUsage.getSourceName(), getParamClasses(pUsage), isAbstract, false, orPropertyMessage);
    }

    public LA<?> findLANoParamsByPropertyUsage(NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLANoParamsByPropertyUsage(false, pUsage);
    }
    public LA<?> findLANoParamsByPropertyUsage(boolean orPropertyMessage, NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        if (pUsage.classNames == null) {
            pUsage.classNames = Collections.emptyList();
        }
        LA<?> LA = findLAByPropertyUsage(orPropertyMessage, pUsage);
        ValueClass[] paramClasses = LA.getInterfaceClasses(ClassType.signaturePolicy);
        if (paramClasses.length != 0) {
            errLog.emitPropertyWithParamsExpectedError(getParser(), pUsage.name, "[]");
        }
        return LA;
    }

    public LP<?> findLPNoParamsByPropertyUsage(NamedPropertyUsage pUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPParamByPropertyUsage(pUsage, ListFact.EMPTY());
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

    private ImSet<FormEntity> findForms(List<String> names) throws ScriptingErrorLog.SemanticErrorException {
        MSet<FormEntity> forms = SetFact.mSet();
        for (String name : names) {
            forms.add(findForm(name));
        }
        return forms.immutable();
    }

    public Event createScriptedEvent(String name, BaseEvent base, List<String> formIds, List<ActionOrPropertyUsage> afterIds) throws ScriptingErrorLog.SemanticErrorException {
        return new Event(name, base, formIds != null ? new SessionEnvEvent(findForms(formIds)) : SessionEnvEvent.ALWAYS, afterIds == null? null : findEventActionsOrPropsByUsages(afterIds));
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

    public void addScriptedGroup(String groupName, LocalizedString captionStr, String integrationSID, String parentName, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateGroup(groupName);
        LocalizedString caption = (captionStr == null ? LocalizedString.create(groupName) : captionStr);
        Group parentGroup = (parentName == null ? null : findGroup(parentName));
        Group group = addAbstractGroup(groupName, caption, parentGroup);
        group.setIntegrationSID(integrationSID);
        group.setDebugPoint(debugPoint);
    }

    public ScriptingFormEntity createScriptedForm(String formName, LocalizedString caption, DebugInfo.DebugPoint point, String icon,
                                                  boolean localAsync) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateForm(formName);
        
        if(caption == null)
            caption = LocalizedString.create(BaseUtils.humanize(formName));

        String canonicalName = elementCanonicalName(formName);

        FormEntity formEntity = new FormEntity(canonicalName, point, caption, icon, getVersion());
        addFormEntity(formEntity);
                
        ScriptingFormEntity form = new ScriptingFormEntity(this, formEntity);
        form.setLocalAsync(localAsync);

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

    public void finalizeScriptedForm(ScriptingFormEntity form) {
        form.getForm().finalizeInit(getVersion());
    }

    public ScriptingFormEntity getFormForExtending(String name) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(name);
        return new ScriptingFormEntity(this, form);
    }

    public LP addScriptedDProp(String returnClass, ImList<ValueClass> paramClasses, boolean sessionProp, boolean innerProp, boolean isLocalScope, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoInline(innerProp);

        ValueClass value = findClass(returnClass);
        ValueClass[] params = getParams(paramClasses, true);

        if (sessionProp) {
            return addSDProp(LocalizedString.NONAME, isLocalScope, value, nestedType, params);
        } else {
            assert nestedType == null;
            return addDProp(LocalizedString.NONAME, value, params);
        }
    }

    public LP<?> addScriptedAbstractProp(CaseUnionProperty.Type type, String returnClass, ImList<ValueClass> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass value = findClass(returnClass);
        ValueClass[] params = getParams(paramClasses, true);
        return addAUProp(null, false, isExclusive, isChecked, isLast, type, LocalizedString.NONAME, value, params);
    }

    public LA addScriptedAbstractAction(ListCaseAction.AbstractType type, ImList<ValueClass> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass[] params = getParams(paramClasses, true);
        LA<?> result;
        if (type == ListCaseAction.AbstractType.LIST) {
            result = addAbstractListAProp(isChecked, isLast, params);
        } else {
            result = addAbstractCaseAProp(type, isExclusive, isChecked, isLast, params);
        }
        return result;
    }

    private ValueClass[] getParams(ImList<ValueClass> paramClasses, boolean checkSignature) throws ScriptingErrorLog.SemanticErrorException {
        if(checkSignature)
            for (int i = 0; i < paramClasses.size(); i++)
                checks.checkSignatureParam(paramClasses.get(i));
        return paramClasses.toArray(new ValueClass[paramClasses.size()]);
    }

    // todo [dale]: выделить общий код    
    public void addImplementationToAbstractAction(NamedPropertyUsage abstractPropUsage, List<TypedParameter> context, LAWithParams implement, LPWithParams when, boolean optimisticAsync) throws ScriptingErrorLog.SemanticErrorException {
        LA abstractLA = findLAByPropertyUsage(abstractPropUsage, context, true);
        checks.checkParamCount(abstractLA, context.size());
        checks.checkImplementIsNotMain(abstractLA, implement.getLP());

        List<LAPWithParams> allProps = new ArrayList<>();
        allProps.add(implement);
        if (when != null) {
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        List<ResolveClassSet> signature = getClassesFromTypedParams(context);
        addImplementationToAbstractAction(abstractPropUsage.name, abstractLA, signature, when != null, params, optimisticAsync);
    }

    public void addImplementationToAbstractProp(NamedPropertyUsage abstractPropUsage, List<TypedParameter> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        LP abstractLP = findLPByPropertyUsage(abstractPropUsage, context, true);
        checks.checkParamCount(abstractLP, context.size());
        checks.checkImplementIsNotMain(abstractLP, implement.getLP());

        List<LAPWithParams> allProps = new ArrayList<>();
        allProps.add(implement);
        if (when != null) {
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);

        List<ResolveClassSet> signature = getClassesFromTypedParams(context);
        addImplementationToAbstractProp(abstractPropUsage.name, abstractLP, signature, when != null, params);
    }

    private void addImplementationToAbstractProp(String propName, LP abstractProp, List<ResolveClassSet> signature, boolean isCase, List<Object> params) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAbstractProperty(abstractProp, propName);
        CaseUnionProperty.Type type = ((CaseUnionProperty)abstractProp.property).getAbstractType();
        checks.checkAbstractTypes(type == CaseUnionProperty.Type.CASE, isCase);

        try {
            abstractProp.addOperand(isCase, signature, getVersion(), params.toArray());
        } catch (ScriptParsingException e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
    }

    private void addImplementationToAbstractAction(String actionName, LA abstractAction, List<ResolveClassSet> signature, boolean isCase, List<Object> params, boolean optimisticAsync) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAbstractAction(abstractAction, actionName);
        ListCaseAction.AbstractType type = ((ListCaseAction)abstractAction.action).getAbstractType();
        checks.checkAbstractTypes(type == ListCaseAction.AbstractType.CASE, isCase);

        try {
            abstractAction.addOperand(isCase, signature, optimisticAsync, getVersion(), params.toArray());
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
        if (index < 0 && context != null && (dynamic || isRecursiveParam(paramName) && insideRecursion)) {
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

    public LAWithParams addScriptedForAProp(List<TypedParameter> oldContext, LPWithParams condition, List<LPWithParams> orders, LAWithParams action, LAWithParams elseAction, Integer addNum, String addClassName, Boolean autoSet, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedForAProp(oldContext, condition, orders, false, null, action, elseAction, addNum, addClassName, autoSet, recursive, descending, noInline, forceInline);
    }

    public static class LAWithParams extends LAPWithParams {
        public LAWithParams(LA<?> action, List<Integer> usedParams) {
            super(action, usedParams);
        }

        public LAWithParams(LA<?> action, LAPWithParams mapLP) {
            this(action, mapLP.usedParams);
        }

        public LA<?> getLP() {
            return (LA<?>) super.getLP();
        }
    }

    public static class LPWithParams extends LAPWithParams {
        public LPWithParams(LP<?> property, List<Integer> usedParams) {
            super(property, usedParams);
        }

        public LPWithParams(LP<?> property, LAPWithParams mapLP) {
            this(property, mapLP.usedParams);
        }

        public LPWithParams(LP<?> property, Integer usedParam) {
            this(property, Collections.singletonList(usedParam));
        }

        public LPWithParams(Integer usedParam) {
            this(null, usedParam);
        }

        public LPWithParams(LP<?> property) {
            this(property, Collections.emptyList());
        }

        public LPWithParams(LP<?> property, boolean notEmpty) {
            this(property, consecutiveList(property.listInterfaces.size(), 0));
            assert notEmpty;
        }

        public LP<?> getLP() {
            return (LP<?>) super.getLP();
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
    
    public static abstract class LPNotExpr {
    }
    public static class LPTrivialLA extends LPNotExpr {
        public final FormActionOrPropertyUsage action;
        public final List<TypedParameter> mapParams;

        public LPTrivialLA(FormActionOrPropertyUsage action, List<TypedParameter> mapParams) {
            this.action = action;
            this.mapParams = mapParams;
        }
    }
    public static class LPContextIndependent extends LPNotExpr {
        public final LP property;
        public final List<ResolveClassSet> signature;
        public final List<Integer> usedContext;

        public LPContextIndependent(LP property, List<ResolveClassSet> signature, List<Integer> usedContext) {
            this.property = property;
            this.signature = signature;
            this.usedContext = usedContext;
        }
    }

    public static class LPLiteral extends LPNotExpr {
        public final Object value;

        public LPLiteral(Object value) {
            this.value = value;
        }
    }

    public static class LPCompoundID extends LPNotExpr {
        public final String name;
        public final ScriptingErrorLog.SemanticErrorException error;

        public LPCompoundID(String name, ScriptingErrorLog.SemanticErrorException error) {
            this.name = name;
            this.error = error;
        }
    }

    public List<String> getParamNamesFromTypedParams(List<String> paramNames, List<TypedParameter> params, boolean innerPD) {
        List<String> defaultParamNames = !innerPD ? getParamNamesFromTypedParams(params) : null;
        if(paramNames == null)
            return defaultParamNames;

        List<String> result = new ArrayList<>();
        for (int i = 0, paramsSize = paramNames.size(); i < paramsSize; i++) {
            String paramName = paramNames.get(i);
            if(paramName == null && defaultParamNames != null)
                paramName = defaultParamNames.get(i);

            result.add(paramName);
        }
        return result;
    }

    private List<String> getParamNamesFromTypedParams(List<TypedParameter> params) {
        List<String> paramNames = new ArrayList<>();
        for (TypedParameter param : params) {
            paramNames.add(param.paramName);
        }
        return paramNames;
    }

    public List<ResolveClassSet> getClassesFromTypedParams(List<TypedParameter> params) {
        return getParamClasses(getValueClassesFromTypedParams(params));
    }

    public ImList<ValueClass> getValueClassesFromTypedParams(List<TypedParameter> params) {
        MList<ValueClass> mResult = ListFact.mList(params.size());
        for (TypedParameter param : params) {
            mResult.add(param.cls);
        }
        return mResult.immutableList();
    }

    public void makeActionOrPropertyPublic(FormEntity form, String alias, FormLAPUsage<?> lpUsage) {
        LAP property = lpUsage.lp;
        // sort of !propertyNeedToBeWrapped
        if(property.getActionOrProperty().isNamed() || (property instanceof LP && isLazy((LP)property)))
            return;
        
        String name = "_FORM_" + form.getCanonicalName().replace('.', '_') + "_" + alias;
        makeSystemActionOrPropertyPublic(property, name, lpUsage.signature);
    }

    public void addSettingsToActionOrProperty(LAP property, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature,
                                              ActionOrPropertySettings ps) throws ScriptingErrorLog.SemanticErrorException {
        makeActionOrPropertyPublic(property, name, signature);

        ActionOrProperty actionOrProperty = property.getActionOrProperty();
        ActionOrProperty.DrawOptions drawOptions = actionOrProperty.drawOptions;

        actionOrProperty.annotations = ps.annotations;

        List<String> paramNames = getParamNamesFromTypedParams(params);
        checks.checkDistinctParameters(paramNames);
        checks.checkNamedParams(property, paramNames);
        checks.checkParamsClasses(params, signature);
        actionOrProperty.paramNames = paramNames.isEmpty() ? MapFact.EMPTYREV() : property.listInterfaces.mapOrderRevValues(paramNames::get);

        if(caption == null)
            caption = LocalizedString.create(BaseUtils.humanize(name));
        actionOrProperty.caption = caption;

        String groupName = ps.groupName;
        Group group = (groupName == null ? null : findGroup(groupName));
        addPropertyToGroup(actionOrProperty, group);

        if (ps.viewType != null)
            drawOptions.setViewType(ps.viewType);
        if (ps.customRenderFunction != null)
            actionOrProperty.setCustomRenderFunction(ps.customRenderFunction);
        if (ps.customEditorFunction != null)
            drawOptions.setCustomEditorFunction(ps.customEditorFunction);
        if (ps.flex != null)
            drawOptions.setValueFlex(ps.flex);
        if (ps.charWidth != null)
            drawOptions.setCharWidth(ps.charWidth);
        if (ps.changeKey != null) {
            drawOptions.setChangeKey(ps.changeKey);
            if (ps.showChangeKey != null)
                drawOptions.setShowChangeKey(ps.showChangeKey);
        }
        if (ps.changeMouse != null) {
            drawOptions.setChangeMouse(ps.changeMouse);
            if (ps.showChangeMouse != null)
                drawOptions.setShowChangeMouse(ps.showChangeMouse);
        }
        if (ps.sticky != null)
            drawOptions.setSticky(ps.sticky);
        if (ps.sync != null)
            drawOptions.setSync(ps.sync);
        if (ps.image != null)
            actionOrProperty.setImage(ps.image);
        if (ps.extId != null)
            actionOrProperty.setExtId(ps.extId);

        if (ps.keyPressKey != null)
            setScriptedKeyPressAction(property, ps.keyPressKey, ps.keyPressAction);
        if (ps.contextMenuEventAction != null)
            setScriptedContextMenuAction(property, ps.contextMenuEventCaption, ps.contextMenuEventAction);
        if(ps.editEventActionType != null)
            setScriptedEventAction(property, ps.editEventActionType, ps.editEventBefore, ps.editEventAction);

    }

    public void addSettingsToAction(LA action, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature, ActionSettings as) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateAction(name, signature);

        addSettingsToActionOrProperty(action, name, caption, params, signature, as);

        ActionOrProperty actionOrProperty = action.getActionOrProperty();
        ActionOrProperty.DrawOptions drawOptions = actionOrProperty.drawOptions;

        if(as.contextMenuForMainPropertyUsage != null) {
            LAP<?, ?> mainProperty = findLAPByActionOrPropertyUsage(as.contextMenuForMainPropertyUsage);
            action.addToContextMenuFor(mainProperty, as.contextMenuForCaption);

            action.setAsEventActionFor(action.action.getSID(), mainProperty);
        }
        if(as.eventActionSID != null)
            setAsEventActionFor(action, as.eventActionSID, as.eventActionBefore, as.eventActionMainPropertyUsage);
        if(as.askConfirm != null)
            drawOptions.setAskConfirm(as.askConfirm);
    }

    public <K extends PropertyInterface> void addSettingsToProperty(LP<K> property, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature,
                                                                    PropertySettings ps, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateProperty(name, signature);

        if(ps.isMaterialized) // it is also sort of iteration (however it breaks the invariant that materialized and not materialized should behave the same, but it is not important for now)
            property = patchExtendParams(property, params, debugPoint); // but if remove this patch then getOrderTableInterfaceClasses should use patchType

        addSettingsToActionOrProperty(property, name, caption, params, signature, ps);

        ImplementTable targetTable = null;
        String tableName = ps.table;
        if (tableName != null) {
            targetTable = findTable(tableName);
            if (targetTable.getMapKeysTable(property.property.getOrderTableInterfaceClasses()) == null) {
                errLog.emitWrongClassesForTableError(parser, name, tableName);
            }
        }
        if (property.property instanceof StoredDataProperty || (ps.isMaterialized && (property.property instanceof AggregateProperty))) {
            property.property.markStored(targetTable);
        }
        property.property.mapDbName = ps.field;

        if(ps.indexType != null) {
            addScriptedIndex(property, ps.indexName, ps.indexType);
        }

        if(ps.isComplex != null)
            property.property.setComplex(ps.isComplex);
        if(ps.isPreread)
            property.property.setPreread(ps.isPreread);
        if(ps.isHint != null)
            property.property.setHint(ps.isHint);

        BooleanDebug notNull = ps.notNull;
        if (notNull != null) {
            BooleanDebug notNullResolve = ps.notNullResolve;
            Event notNullEvent = ps.notNullEvent;
            Event notNullResolveEvent = ps.notNullResolveEvent;
            setNotNull(property.property, notNull.debugPoint,
                    notNullResolve != null ? ListFact.singleton(new PropertyFollowsDebug(notNullResolveEvent, false, true, notNullResolve.debugPoint,
                        LocalizedString.concatList(LocalizedString.create("{logics.property} "), property.property.caption, " [" + property.property.getSID(), LocalizedString.create("]. {logics.property.not.defined.resolve}")))) : ListFact.EMPTY(),
                    // not sure that this is needed, but this way it works closer to the AGGR mechanism
                    notNullResolve != null && useOptResolve ? ListFact.singleton(property.property.getImplement().mapChanged(IncrementType.DROP, (notNullResolveEvent != null ? notNullResolveEvent : notNullEvent).getScope())) : null,
                    notNullEvent);

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
//                checks.checkClassWhere((LP) property, name);
//            }

        ActionOrProperty actionOrProperty = property.getActionOrProperty();
        ActionOrProperty.DrawOptions drawOptions = actionOrProperty.drawOptions;
        if(ps.defaultCompare != null)
            drawOptions.setDefaultCompare(ps.defaultCompare);
        if(ps.autoset != null)
            property.setAutoset(ps.autoset);
        if(ps.pattern != null)
            drawOptions.setPattern(ps.pattern);
        if(ps.regexp != null)
            drawOptions.setRegexp(ps.regexp);
        if(ps.regexpMessage != null)
            drawOptions.setRegexpMessage(ps.regexpMessage);
        if(ps.echoSymbols != null)
            drawOptions.setEchoSymbols(ps.echoSymbols);
        if(ps.aggr != null)
            property.property.setAggr(ps.aggr);
        if(ps.eventId != null)
            drawOptions.setEventID(ps.eventId);
        if(ps.lazy != null)
            property.property.setLazy(ps.lazy, ps.debugPoint);
    }

    /** Проверяет нужно ли обернуть свойство в join.
     *  Свойства нужно обернуть, если это не только что созданное свойство, а свойство, созданное ранее с уже установленными 
     *  параметрами (например, с установленным каноническим именем или debug point'ом). Такая ситуация возникает, если 
     *  была произведена какая-то оптимизация: кэширование (например, с помощью IdentityLazy) либо логика с lastOptimizedJPropSID.
     *  todo [dale]: Сейчас проверяются только основные частные случаи.
     */
    private boolean propertyNeedsToBeWrapped(LP<?> property) {
        // Если объявление имеет вид f(x, y) = g(x, y), то нужно дополнительно обернуть свойство g в join
        return isLastOptimized(property) || isLazy(property);
    }

    private boolean isLastOptimized(LP<?> property) {
        return property.property.getSID().equals(lastOptimizedJPropSID);
    }

    // it seems that there are a lot more lazy properties (for example AndFormulaProperty instances in BaseLogicsModule - object, and1, etc.)
    // but so far it doesn't seem to be that critical
    private boolean isLazy(LP<?> property) {
        return property.property instanceof ValueProperty || property.property instanceof IsClassProperty || property.property instanceof FormulaJoinProperty || property == baseLM.object; // == baseLM.object since it is used in checkSingleParam
    }

    public List<LPWithParams> checkSingleParams(List<LPWithParams> properties) {
        for(int i = 0; i < properties.size(); i++) {
            properties.set(i, checkSingleParam(properties.get(i)));
        }
        return properties;
    }

    public LPWithParams checkSingleParam(LPWithParams property) {
        if (property.getLP() == null) {
            property = new LPWithParams(baseLM.object, property);
        }
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

    public void addToContextMenuFor(ActionSettings as, LocalizedString contextMenuCaption, ActionOrPropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        as.contextMenuForCaption = contextMenuCaption;
        as.contextMenuForMainPropertyUsage = mainPropertyUsage;
    }

    public void setAsEventActionFor(LA eventAction, String eventActionSID, Boolean before, ActionOrPropertyUsage mainPropertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        if(before != null) {
            throw new UnsupportedOperationException("ASON CHANGE BEFORE|AFTER is not supported");
        }

        assert mainPropertyUsage != null;

        LAP<?, ?> mainProperty = findLAPByActionOrPropertyUsage(mainPropertyUsage);
        eventAction.setAsEventActionFor(eventActionSID, mainProperty);
    }

    public void setPivotOptions(LAP property, PivotOptions pivotOptions) {
        property.setPivotOptions(pivotOptions);
    }

    public void setFlexCharWidth(ActionOrPropertySettings ps, Integer charWidth, Boolean flex) {
        if (charWidth != null && charWidth > 0) {
            ps.flex = flex;
            ps.charWidth = charWidth;
        }
    }

    public void setLazy(PropertySettings ps, Property.Lazy lazy, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        ps.lazy = lazy;
        ps.debugPoint = debugPoint;
    }

    public void setChangeKey(ActionOrPropertySettings ps, String code, Boolean showChangeKey) {
        ps.changeKey = KeyStrokeConverter.parseInputBindingEvent(code, false);
        if (showChangeKey != null)
            ps.showChangeKey = showChangeKey;
    }

    public void setChangeMouse(ActionOrPropertySettings ps, String code, Boolean showChangeMouse) {
        ps.changeMouse = KeyStrokeConverter.parseInputBindingEvent(code, true);
        if (showChangeMouse != null)
            ps.showChangeMouse = showChangeMouse;
    }

    private static List<String> supportedBindings = Arrays.asList("preview", "dialog", "window", "group", "editing", "showing", "panel", "cell");
    private static Map<String, BindingMode> getBindingModesMap(Map<String, String> optionsMap) {
        Map<String, BindingMode> bindingModes = new HashMap<>();
        for(Map.Entry<String, String> option : optionsMap.entrySet()) {
            if(supportedBindings.contains(option.getKey())) {
                BindingMode bindingMode;
                switch (option.getValue()) {
                    case "all":
                        bindingMode = BindingMode.ALL;
                        break;
                    case "only":
                        bindingMode = BindingMode.ONLY;
                        break;
                    case "no":
                        bindingMode = BindingMode.NO;
                        break;
                    case "input":
                        bindingMode = BindingMode.INPUT;
                        break;
                    default:
                        bindingMode = BindingMode.AUTO;
                        break;
                }
                bindingModes.put(option.getKey(), bindingMode);
            }
        }
        return bindingModes;
    }

    private static Integer getPriority(Map<String, String> optionsMap) {
        String priority = optionsMap.get("priority");
        if(priority != null) {
            try {
                return Integer.parseInt(optionsMap.getOrDefault("priority", null));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Map<String, String> getOptionsMap(String values) {
        Map<String, String> options = new HashMap<>();
        Matcher m = Pattern.compile("([^=;]*)=([^=;]*)").matcher(values);
        while(m.find()) {
            options.put(m.group(1), m.group(2));
        }
        return options;
    }

    public void setScriptedEventAction(LAP property, String actionType, Boolean before, LAWithParams action) {
        if(before != null) {
            throw new UnsupportedOperationException("ON CHANGE BEFORE|AFTER is not supported");
        }
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LAP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        property.getActionOrProperty().setEventAction(actionType, actionImplements.get(0));
    }

    public void setScriptedContextMenuAction(LAP property, LocalizedString contextMenuCaption, LAWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LAP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        ActionMapImplement<?, PropertyInterface> actionImplement = actionImplements.get(0);

        String actionSID = actionImplement.action.getSID();
        property.getActionOrProperty().setContextMenuAction(actionSID, FormPropertyOptions.getContextMenuCaption(contextMenuCaption, actionImplement.action));
        property.getActionOrProperty().setEventAction(actionSID, actionImplement);
    }

    public void setScriptedKeyPressAction(LAP property, String key, LAWithParams action) {
        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(((LAP<PropertyInterface, ?>)property).listInterfaces, params.toArray());
        ActionMapImplement<?, PropertyInterface> actionImplement = actionImplements.get(0);

        String actionSID = actionImplement.action.getSID();
        property.getActionOrProperty().setKeyAction(KeyStroke.getKeyStroke(key), actionSID);
        property.getActionOrProperty().setEventAction(actionSID, actionImplement);
    }

    public List<ResolveClassSet> getParamClasses(NamedPropertyUsage usage) throws ScriptingErrorLog.SemanticErrorException {
        List<ValueClass> valueClasses = getValueClasses(usage);
        if (valueClasses == null) {
            return null;
        }

        return getParamClasses(valueClasses);
    }

    public List<ResolveClassSet> getParamClasses(LAP lap, ImList<ValueClass> valueClasses, boolean innerPD) {
        return innerPD || valueClasses.isEmpty() ? Collections.nCopies(lap.listInterfaces.size(), null) : getParamClasses(valueClasses);
    }

    private List<ResolveClassSet> getParamClasses(Iterable<ValueClass> valueClasses) {
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

    public List<ValueClass> getValueClasses(NamedPropertyUsage usage) throws ScriptingErrorLog.SemanticErrorException {
        if (usage.classNames == null) {
            return null;
        }

        List<ValueClass> classes = new ArrayList<>();
        for (String className : usage.classNames) {
            if (className.equals(PropertyCanonicalNameUtils.UNKNOWNCLASS)) {
                classes.add(null);
            } else {
                classes.add(findClass(className));
            }
        }
        return classes;
    }

    private List<ResolveClassSet> getParamClassesByParamProperties(List<LPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = new ArrayList<>();
        for (LPWithParams paramProp : paramProps) {
            if (paramProp.getLP() != null) {
                LP lcp = paramProp.getLP();
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

    private LP findLPByPropertyUsage(NamedPropertyUsage mainProp, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(mainProp, paramProps, context, false);
    }
    private LP findLPByPropertyUsage(NamedPropertyUsage mainProp, List<LPWithParams> paramProps, List<TypedParameter> context, boolean nullIfNotFound) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLPByPropertyUsage(nullIfNotFound, mainProp);
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, context);
        return findLPByNameAndClasses(mainProp.name, mainProp.getSourceName(), classes, nullIfNotFound);
    }

    private LA findLAByPropertyUsage(NamedPropertyUsage mainProp, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByPropertyUsage(mainProp, paramProps, context, false);
    }
    private LA findLAByPropertyUsage(NamedPropertyUsage mainProp, List<LPWithParams> paramProps, List<TypedParameter> context, boolean orPropertyMessage) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLAByPropertyUsage(orPropertyMessage, mainProp);
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, context);
        return findLAByNameAndClasses(mainProp.name, mainProp.getSourceName(), classes, orPropertyMessage);
    }

    private LP findLPByPropertyUsage(NamedPropertyUsage mainProp, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLPByPropertyUsage(mainProp, params, false);
    }

    private LP findLPByPropertyUsage(NamedPropertyUsage mainProp, List<TypedParameter> params, boolean onlyAbstract) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLPByPropertyUsage(mainProp, onlyAbstract);
        return findLPByNameAndClasses(mainProp.name, mainProp.getSourceName(), getClassesFromTypedParams(params), onlyAbstract, true);
    }

    private LA findLAByPropertyUsage(NamedPropertyUsage mainProp, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        return findLAByPropertyUsage(mainProp, params, false);
    }

    private LA findLAByPropertyUsage(NamedPropertyUsage mainProp, List<TypedParameter> params, boolean onlyAbstract) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.classNames != null)
            return findLAByPropertyUsage(mainProp, onlyAbstract, false);
        return findLAByNameAndClasses(mainProp.name, mainProp.getSourceName(), getClassesFromTypedParams(params), onlyAbstract, true, false);
    }

    public Pair<LPWithParams, LPTrivialLA> addScriptedJProp(boolean user, NamedPropertyUsage pUsage, List<LPWithParams> paramProps, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findLPByPropertyUsage(pUsage, paramProps, params, true);

        LPWithParams result = null;

        if(mainProp != null) {
            result = addScriptedJProp(user, mainProp, paramProps);
        } else {
            findLAByPropertyUsage(pUsage, paramProps, params, true);
        }                

        // this whole thing is needed for PROPERTIES f(a,b) block in form (to keep LL(*) parser there, just like with GROUP BY)
        LPTrivialLA la = null;
        if(result == null || result.getLP() != mainProp) { // optimization if it was lastoptimized, we can use this property (it will 
            List<TypedParameter> jParams = getJParams(params, paramProps);
            if (jParams != null) {
                List<String> paramNames = getParamNamesFromTypedParams(jParams);
                FormActionOrPropertyUsage formUsage;
                if (mainProp == null) { // action hack
                    formUsage = new FormActionUsage(pUsage, paramNames);
                } else {
                    formUsage = new FormPropertyUsage(pUsage, paramNames);
                }
                la = new LPTrivialLA(formUsage, jParams);
            } else if (result == null ) {
                errLog.emitPropertyNotFoundError(parser, pUsage.getSourceName());
            }
        }
        
        return new Pair<>(result, la);
    }

    public LPWithParams addScriptedJProp(LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(false, mainProp, paramProps);
    }

    public LPWithParams addScriptedJProp(boolean user, LP mainProp, List<LPWithParams> paramProps, List<Integer> usedContext, boolean ci) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(user, mainProp, getAllGroupProps(usedContext, paramProps, ci));
    }

    public LPWithParams addScriptedJProp(boolean user, LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LP prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
            lastOptimizedJPropSID = mainProp.property.getSID();
        } else {
            prop = addJProp(user, mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    private LP getRelationProp(String op) {
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

    private LP getArithProp(String op) {
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

    public LPWithParams addScriptedEqualityProp(String op, LPWithParams leftProp, LPWithParams rightProp, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkComparisonCompatibility(leftProp, rightProp, context);
        return addScriptedEqualityProp(op, leftProp, rightProp);
    }
    public LPWithParams addScriptedEqualityProp(String op, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedRelationalProp(String op, LPWithParams leftProp, LPWithParams rightProp, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkComparisonCompatibility(leftProp, rightProp, context);
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedOverrideProp(List<LPWithParams> properties, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        if (isExclusive) {
            return addScriptedUProp(Union.EXCLUSIVE, properties, "EXCLUSIVE");
        } else {
            return addScriptedUProp(Union.OVERRIDE, properties, "OVERRIDE");
        }
    }

    public LPWithParams addScriptedLikeProp(boolean match, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkMatchLikeType(match, leftProp, rightProp);
        return addScriptedJProp(match ? baseLM.match2 : baseLM.like2, asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedIfProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            curLP = addScriptedJProp(and(properties.size()), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedOrProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            List<LPWithParams> logicalProperties = convertToLogical(properties);
            res = addScriptedUProp(Union.OVERRIDE, logicalProperties, "OR");
        }
        return res;
    }

    public LPWithParams addScriptedXorProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            List<LPWithParams> logicalProperties = convertToLogical(properties);
            res = addScriptedUProp(Union.XOR, logicalProperties, "XOR");
        }
        return res;
    }

    public LPWithParams addScriptedAndProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams curLP = properties.get(0);
        if (properties.size() > 1) {
            LPWithParams firstArgument = properties.get(0);
            if (!isLogical(firstArgument.getLP())) {
                properties.set(0, new LPWithParams(toLogical(firstArgument).getLP(), firstArgument));
            }
            curLP = addScriptedJProp(and(properties.size()), properties);
        }
        return curLP;
    }

    private List<LPWithParams> convertToLogical(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> logicalProperties = new ArrayList<>();
        for (LPWithParams prop : properties) {
            LPWithParams logicalProp = prop;
            if (!isLogical(prop.getLP())) {
                logicalProp = new LPWithParams(toLogical(prop).getLP(), prop);
            }
            logicalProperties.add(logicalProp);
        }
        return logicalProperties;
    }

    private LPWithParams toLogical(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(and(false), Arrays.asList(new LPWithParams(baseLM.vtrue), property));
    }

    public static boolean useExclusiveIfElse = false; // not sure why exclusiveness was used (it doesn't change anything except moving prevExpr to the end of CaseExpr)

    public LPWithParams addScriptedIfElseUProp(LPWithParams ifProp, LPWithParams thenProp, LPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
//        List<LPWithParams> lpParams = new ArrayList<>();
//        lpParams.add(addScriptedJProp(and(false), asList(thenProp, ifProp)));
//        if (elseProp != null) {
//            lpParams.add(addScriptedJProp(and(true), asList(elseProp, ifProp)));
//        }
//        return addScriptedUProp(Union.EXCLUSIVE, lpParams, "IF");

        if(elseProp == null)
            return addScriptedIfProp(BaseUtils.toList(thenProp, ifProp));

        if(elseProp != null && useExclusiveIfElse)
            return addScriptedCaseUProp(BaseUtils.toList(ifProp, addScriptedNotProp(ifProp)), BaseUtils.toList(thenProp, elseProp), null, true);
        else
            return addScriptedCaseUProp(Collections.singletonList(ifProp), Collections.singletonList(thenProp), elseProp, false);
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

    public LA addScriptedInternalClientAction(String resourceName, ImList<ValueClass> paramClasses, boolean syncType) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass[] params = getParams(paramClasses, false);

        boolean isFile = resourceName.contains(".js") || resourceName.contains(".css");
        if (isFile && params.length > 0) {
            errLog.emitInternalClientActionHasParamsOnFileCallingError(parser, resourceName);
        }

        return addInternalClientAction(resourceName, params, syncType);
    }

    public LAWithParams addScriptedShowRecDepAction(List<ActionOrPropertyUsage> ids, boolean showRec, boolean global) throws ScriptingErrorLog.SemanticErrorException {
        return new LAWithParams(addAProp(null, new ShowRecDepAction(showRec, ids != null ? findEventActionsOrPropsByUsages(ids).getSet() : SetFact.EMPTY(), global)), new ArrayList<>());
    }

    public LA addScriptedInternalAction(String javaClassName, ImList<ValueClass> paramClasses, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        try {
            Object instanceObject = null;
            Class<?> javaClass = Class.forName(javaClassName);

            ValueClass[] params = getParams(paramClasses, false);

            if (params.length == 0) {
                try {
                    instanceObject = javaClass.getConstructor(this.getClass()).newInstance(this);
                } catch (NoSuchMethodException ignored) {
                }
            }

            if(instanceObject == null)
                instanceObject = javaClass.getConstructor(new Class[] {this.getClass(), ValueClass[].class}).newInstance(this, params);

            Action instance = (Action)instanceObject;
            if (instance instanceof ExplicitAction && allowNullValue) {
                ((ExplicitAction) instance).allowNullValue = true;
            }
            return baseLM.addAProp(null, instance);
        } catch (ClassNotFoundException e) {
            errLog.emitClassNotFoundError(parser, javaClassName);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, e.getMessage(), javaClassName);
        }
        return null;
    }

    public LA addScriptedInternalAction(String code, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        String script = "";
        try {

            script = code.substring(1, code.length() - 1); //remove brackets

            String javaClass = "import lsfusion.server.data.sql.exception.SQLHandledException;\n" +
                    "import lsfusion.server.logics.property.classes.ClassPropertyInterface;\n" +
                    "import lsfusion.server.logics.action.controller.context.ExecutionContext;\n" +
                    "import lsfusion.server.physics.dev.integration.internal.to.InternalAction;\n" +
                    "import lsfusion.server.language.ScriptingLogicsModule;\n" +
                    "\n" +
                    "import java.sql.SQLException;\n" +
                    "\n" +
                    "public class ExecuteAction extends InternalAction {\n" +
                    "\n" +
                    "    public ExecuteAction(ScriptingLogicsModule LM) {\n" +
                    "        super(LM);\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {\n" +
                    "        try {\n" +
                    script +
                    "        } catch (Exception e) {\n" +
                    "            e.printStackTrace();\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(javaClass);
            Class<?> executeClass = sc.getClassLoader().loadClass("ExecuteAction");

            Action instance = (Action) executeClass.getConstructor(ScriptingLogicsModule.class).newInstance(this);
            if (instance instanceof ExplicitAction && allowNullValue) {
                ((ExplicitAction) instance).allowNullValue = true;
            }
            return baseLM.addAProp(null, instance);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, e.getMessage(), script);
        }
        return null;
    }


    public ImList<Type> getTypesByParamProperties(List<LPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, params);
        MList<Type> mTypes = ListFact.mList(classes.size());
        for(int i=0,size=paramProps.size();i<size;i++) {
            Type type = null;

            ResolveClassSet paramClass = classes.get(i);
            if(paramClass != null)
                type = paramClass.getType();
            else {
                LP<?> property = paramProps.get(i).getLP();
                if(property != null)
                    type = property.property.getType();
            }
            mTypes.add(type);
        }
        return mTypes.immutableList();
    }

    public ImList<ValueClass> getValueClassesByParamProperties(List<LPWithParams> paramProps, List<TypedParameter> params) {
        List<ResolveClassSet> classes = getParamClassesByParamProperties(paramProps, params);
        MList<ValueClass> mValueClasses = ListFact.mList(classes.size());
        for(int i=0,size=paramProps.size();i<size;i++) {
            ValueClass valueClass = null;

            LP<?> property = paramProps.get(i).getLP();
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

    public Type getTypeByParamProperty(LPWithParams paramProp, List<TypedParameter> params) {
        return getTypesByParamProperties(Collections.singletonList(paramProp), params).single();
    }

    public ValueClass getValueClassByParamProperty(LPWithParams paramProp, List<TypedParameter> params) {
        return getValueClassesByParamProperties(Collections.singletonList(paramProp), params).single();
    }

    public ImList<Type> getTypesForEvalAction(List<LPWithParams> paramProps, List<TypedParameter> params) {
        return getTypesByParamProperties(paramProps, params);
    }
    
    public ImList<Type> getTypesForExternalAction(List<LPWithParams> paramProps, List<TypedParameter> params) {
        return getTypesByParamProperties(paramProps, params);
    }

    public LAWithParams addScriptedExternalJavaAction(List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) {
        throw new UnsupportedOperationException("EXTERNAL JAVA not supported");
    }

    public LAWithParams addScriptedInternalDBAction(LPWithParams exec, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new InternalDBAction(getTypesForExternalAction(params, context), findLPsNoParamsByPropertyUsage(toPropertyUsageList))),
                BaseUtils.addList(exec, params));
    }

    public LAWithParams addScriptedInternalClientAction(LPWithParams exec, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList, boolean syncType) throws ScriptingErrorLog.SemanticErrorException {
        if (toPropertyUsageList.size() > 1) {
            errLog.emitInternalClientActionHasTooMuchToPropertiesError(parser);
        }

        return addScriptedJoinAProp(addAProp(new InternalClientAction(getTypesForExternalAction(params, context), findLPsNoParamsByPropertyUsage(toPropertyUsageList), syncType || !toPropertyUsageList.isEmpty())), BaseUtils.addList(exec, params));
    }

    public LAWithParams addScriptedExternalDBAction(LPWithParams connectionString, LPWithParams exec, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalDBAction(getTypesForExternalAction(params, context), findLPsNoParamsByPropertyUsage(toPropertyUsageList))),
                BaseUtils.mergeList(Arrays.asList(connectionString, exec), params));
    }

    public LAWithParams addScriptedExternalDBFAction(LPWithParams connectionString, String charset, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalDBFAction(getTypesForExternalAction(params, context), charset, findLPsNoParamsByPropertyUsage(toPropertyUsageList))),
                BaseUtils.addList(connectionString, params));
    }

    public LAWithParams addScriptedExternalTCPAction(boolean clientAction, LPWithParams connectionString, List<LPWithParams> params, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalTCPAction(clientAction, getTypesForExternalAction(params, context))), BaseUtils.addList(connectionString, params));
    }

    public LAWithParams addScriptedExternalUDPAction(boolean clientAction, LPWithParams connectionString, List<LPWithParams> params, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalUDPAction(clientAction, getTypesForExternalAction(params, context))), BaseUtils.addList(connectionString, params));
    }

    public LAWithParams addScriptedExternalHTTPAction(boolean clientAction, ExternalHttpMethod method, LPWithParams connectionString, LPWithParams bodyUrl,
                                                      List<LPWithParams> bodyParamNames, List<NamedPropertyUsage> bodyParamHeadersList,
                                                      NamedPropertyUsage headers, NamedPropertyUsage cookies, NamedPropertyUsage headersTo, NamedPropertyUsage cookiesTo,
                                                      boolean noEncode, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList)
            throws ScriptingErrorLog.SemanticErrorException {
        LP headersProperty = headers != null ? findLPStringParamByPropertyUsage(headers) : null;
        LP cookiesProperty = cookies != null ? findLPStringParamByPropertyUsage(cookies) : null;
        LP headersToProperty = headersTo != null ? findLPStringParamByPropertyUsage(headersTo) : null;
        LP cookiesToProperty = cookiesTo != null ? findLPStringParamByPropertyUsage(cookiesTo) : null;
        boolean hasBodyUrl = bodyUrl != null;
        List<LPWithParams> properties = hasBodyUrl ? mergeLists(asList(connectionString, bodyUrl), bodyParamNames, params) :
                mergeLists(singletonList(connectionString), bodyParamNames, params);

        return addScriptedJoinAProp(addAProp(new ExternalHTTPAction(clientAction, method != null ? method : ExternalHttpMethod.POST,
                        getTypesForExternalAction(params, context), findLPsNoParamsByPropertyUsage(toPropertyUsageList), bodyParamNames.size(),
                        findLPsStringParamByPropertyUsage(bodyParamHeadersList), headersProperty, cookiesProperty, headersToProperty, cookiesToProperty,
                        noEncode, hasBodyUrl)),
                properties);
    }

    public LAWithParams addScriptedExternalLSFAction(LPWithParams connectionString, LPWithParams actionString, boolean eval, boolean action, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new ExternalLSFAction(getTypesForExternalAction(params, context), findLPsNoParamsByPropertyUsage(toPropertyUsageList), eval, action)), BaseUtils.addList(connectionString, actionString, params));
    }

    private ImList<LP> findLPsNoParamsByPropertyUsage(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages == null)
            return ListFact.EMPTY();

        MList<LP> mProps = ListFact.mList(propUsages.size());
        for (NamedPropertyUsage propUsage : propUsages) {
            LP<?> lcp = findLPNoParamsByPropertyUsage(propUsage);
            mProps.add(lcp);
        }
        return mProps.immutableList();
    }

    public LAWithParams addScriptedEmailProp(LPWithParams fromProp,
                                             LPWithParams subjProp,
                                             LPWithParams bodyProp,
                                             List<Message.RecipientType> recipTypes,
                                             List<LPWithParams> recipProps,
                                             List<LPWithParams> attachFileNames,
                                             List<LPWithParams> attachFiles,
                                             List<NamedPropertyUsage> attachFileNameProps,
                                             List<NamedPropertyUsage> attachFileProps,
                                             Boolean syncType) throws ScriptingErrorLog.SemanticErrorException {

        List<LAPWithParams> allProps = new ArrayList<>();

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

        if (syncType == null) {
            syncType = true;
        }

        LA<PropertyInterface> eaLA = BL.emailLM.addEAProp(null, LocalizedString.NONAME, getIntNum(allParams), syncType);
        SendEmailAction eaProp = (SendEmailAction) eaLA.action;

        ImList<PropertyInterfaceImplement<PropertyInterface>> allImplements = readCalcImplements(eaLA.listInterfaces, allParams);

        int i = 0;
        
        if (fromProp != null) {
            eaProp.setFromAddress(allImplements.get(i++));
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

        for (LPWithParams fileName : attachFileNames) {
            eaProp.addAttachmentFile(fileName != null ? allImplements.get(i++) : null, allImplements.get(i++));
        }

        for (int j = 0; j < attachFileNameProps.size(); j++) {
            NamedPropertyUsage fileNamePropUsage = attachFileNameProps.get(j);
            NamedPropertyUsage filePropUsage = attachFileProps.get(j);
            eaProp.addAttachmentFileProp(fileNamePropUsage != null ? findLPParamByPropertyUsage(fileNamePropUsage, ListFact.singleton(IntegerClass.instance)) : null,
                    filePropUsage != null ? findLPParamByPropertyUsage(filePropUsage, ListFact.singleton(IntegerClass.instance)) : null);
        }

        return new LAWithParams(eaLA, mergeAllParams(allProps));
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
            res = new LPWithParams(addUProp(null, LocalizedString.NONAME, Union.SUM, coeffs, resultParams.toArray()), mergeAllParams(properties));
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

    private boolean doesExtendContext(int contextSize, List<? extends LAPWithParams> list, List<LPWithParams> orders) {
        Set<Integer> listContext = new HashSet<>();
        for(int i=0;i<contextSize;i++)
            listContext.add(i);
        for(LAPWithParams lp : list)
            if(lp.getLP() != null)
                listContext.addAll(lp.usedParams);
        return !listContext.containsAll(mergeAllParams(orders));
    }

    private List<Integer> mergeAllParams(Iterable<? extends LAPWithParams> lpList) {
        Set<Integer> s = new TreeSet<>();
        for (LAPWithParams mappedLP : lpList)
            s.addAll(mappedLP.usedParams);
        return new ArrayList<>(s);
    }
    private List<Integer> mergeNullableAllParams(LAPWithParams... lpList) {
        Set<Integer> s = new TreeSet<>();
        for (LAPWithParams mappedLP : lpList)
            if(mappedLP != null)
                s.addAll(mappedLP.usedParams);
        return new ArrayList<>(s);
    }

    private List<Integer> mergeIntLists(List<List<Integer>> lists) {
        Set<Integer> s = new TreeSet<>();
        for (List<Integer> list : lists) {
            s.addAll(list);
        }
        return new ArrayList<>(s);
    }

    public LAWithParams addScriptedListAProp(List<LAWithParams> properties, List<LP> localProps) {
        List<Object> resultParams = getParamsPlainList(properties);

        MExclSet<Pair<LP, List<ResolveClassSet>>> mDebugLocals = null;
        if(debugger.isEnabled()) {
            mDebugLocals = SetFact.mExclSet(localProps.size());
        }
        MSet<SessionDataProperty> mLocals = SetFact.mSet();
        for (LP<?> localProp : localProps) {
            if (mDebugLocals != null) {
                List<ResolveClassSet> localSignature = getLocalSignature(localProp);
                mDebugLocals.exclAdd(new Pair<>(localProp, localSignature));
            }
            mLocals.add((SessionDataProperty) localProp.property);

            removeLocal(localProp);
        }

        LA<?> listLA = addListAProp(mLocals.immutable(), resultParams.toArray());

        if(mDebugLocals != null) {
            listLA.action.setDebugLocals(mDebugLocals.immutable());
        }

        return new LAWithParams(listLA, mergeAllParams(properties));
    }

    public LAWithParams addScriptedNewSessionAProp(LAWithParams action, List<NamedPropertyUsage> migrateSessionProps, boolean migrateAllSessionProps,
                                                   boolean migrateClasses, boolean isNested, boolean singleApply, boolean newSQL, List<String> formIds) throws ScriptingErrorLog.SemanticErrorException {
        ImSet<FormEntity> fixedForms = formIds != null ? findForms(formIds) : null;
        LA<?> sessionLA = addNewSessionAProp(null, action.getLP(), isNested, singleApply, newSQL,
                getMigrateProps(migrateSessionProps, migrateAllSessionProps), migrateClasses, fixedForms);
        return new LAWithParams(sessionLA, action.usedParams);
    }

    public ValueClass getInputValueClass(String paramName, List<TypedParameter> context, String classId, LPWithParams oldValue, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass requestValueClass;
        if(classId != null) {
            requestValueClass = findClass(classId);
        } else {
            ValueClass valueClass = getValueClassByParamProperty(oldValue, context);
//            checks.checkInputDataClass(valueClass);
            requestValueClass = valueClass;
        }

        if(paramName != null)
            getParamIndex(new TypedParameter(requestValueClass, paramName), context, true, insideRecursion);
        return requestValueClass;
    }

    // similar to Context Filter Entity
    private static class ILEWithParams {
        public final ImOrderSet<Integer> usedParams;
        public final ImOrderSet<PropertyInterface> orderInterfaces;
        public final InputListEntity<?, PropertyInterface, ?> list;
        public final InputFilterEntity<?, PropertyInterface> where;
        public final ImList<InputContextAction<?, PropertyInterface>> contextActions;

        public ILEWithParams(ImOrderSet<Integer> usedParams, ImOrderSet<PropertyInterface> orderInterfaces,
                             InputListEntity<?, PropertyInterface, ?> list, InputFilterEntity<?, PropertyInterface> where, ImList<InputContextAction<?, PropertyInterface>> contextActions) {
            this.usedParams = usedParams;
            this.orderInterfaces = orderInterfaces;
            assert usedParams.size() == orderInterfaces.size();
            this.list = list;
            this.where = where;
            this.contextActions = contextActions;
        }
    }
    private ScriptingLogicsModule.ILEWithParams getContextListEntity(int contextSize, ScriptingLogicsModule.LAPWithParams list, ScriptingLogicsModule.LPWithParams where,
                                                                     List<String> actionImages, List<String> keyStrokes, List<List<QuickAccess>> quickAccesses, List<LAWithParams> actions) {
//        if(list == null) // optimization
//            return new ILEWithParams(SetFact.EMPTYORDER(), SetFact.EMPTYORDER(), null, null, ListFact.EMPTY());

        List<LAPWithParams> props = new ArrayList<>();
        if(list != null)
            props.add(list);
        if(where != null)
            props.add(where);
        props.addAll(actions);

        // actually list / where and actions parameters are different (however it's not that important)
        ImOrderSet<Integer> usedContextParams = SetFact.fromJavaOrderSet(getResultInterfaces(contextSize, mergeAllParams(props)));
        ImOrderSet<PropertyInterface> orderInterfaces = genInterfaces(usedContextParams.size());
        ImRevMap<Integer, PropertyInterface> usedInterfaces = usedContextParams.mapSet(orderInterfaces);

        return new ScriptingLogicsModule.ILEWithParams(usedContextParams, orderInterfaces,
                list != null ? getInputListEntity(contextSize, list, usedInterfaces) : null,
                where != null ? getInputFilterEntity(contextSize, where, usedInterfaces) : null,
                getInputContextActions(contextSize, actionImages, keyStrokes, quickAccesses, actions, usedInterfaces));
    }

    private InputFilterEntity<?, PropertyInterface> getInputFilterEntity(int contextSize, LPWithParams where, ImRevMap<Integer, PropertyInterface> usedInterfaces) {
        return splitParams(where, contextSize, usedInterfaces, value -> 0, (property, mapValues, mapExternal) -> new InputFilterEntity<>(property, mapValues));
    }

    private InputListEntity<?, PropertyInterface, ?> getInputListEntity(int contextSize, LAPWithParams list, ImRevMap<Integer, PropertyInterface> usedInterfaces) {
        return splitAPParams(list, contextSize, usedInterfaces, value -> 0, (property, mapValues, mapExternal) -> InputListEntity.create(property, mapValues));
    }

    private <O extends ObjectSelector> ContextFilterEntity<PropertyInterface, PropertyInterface, O> getContextFilterEntity(int contextSize, ImOrderSet<O> objectsContext, LPWithParams contextFilter, ImRevMap<Integer, PropertyInterface> usedInterfaces) {
        return splitParams(contextFilter, contextSize, usedInterfaces, objectsContext::get, ContextFilterEntity::new);
    }

    private InputContextAction<?, PropertyInterface> getInputContextAction(int contextSize, List<List<QuickAccess>> quickAccesses, ImRevMap<Integer, PropertyInterface> usedInterfaces, int i, String actionImage, LAWithParams action, KeyStrokeOptions options) {
        return splitAPParams(action, contextSize, usedInterfaces, value -> 0, (property, mapValues, mapExternal) ->
                            // not sure that null or constant id won't do
                            new InputContextAction(actionImage, getFileName(actionImage), options.keyStroke, options.bindingModesMap, options.priority, ListFact.fromJavaList(quickAccesses.get(i)), action.getLP().action, mapValues));
    }

    private ImList<InputContextAction<?, PropertyInterface>> getInputContextActions(int contextSize, List<String> actionImages, List<String> keyStrokes, List<List<QuickAccess>> quickAccesses, List<LAWithParams> actions, ImRevMap<Integer, PropertyInterface> usedInterfaces) {
        return ListFact.fromJavaList(actionImages).mapListValues((i, actionImage) -> {
            LAWithParams action = actions.get(i);
            KeyStrokeOptions options = parseKeyStrokeOptions(keyStrokes.get(i));
            return getInputContextAction(contextSize, quickAccesses, usedInterfaces, i, actionImage, action, options);
        });
    }

    private <O extends ObjectSelector> ImList<ContextFilterEntity<PropertyInterface, PropertyInterface, O>> getContextFilterEntities(int contextSize, ImOrderSet<O> objectsContext, ImList<LPWithParams> contextFilters, ImRevMap<Integer, PropertyInterface> usedInterfaces) {
        return contextFilters.mapListValues((LPWithParams contextFilter) ->
                getContextFilterEntity(contextSize, objectsContext, contextFilter, usedInterfaces));
    }


    public static KeyStrokeOptions parseKeyStrokeOptions(String code) {
        Matcher m = Pattern.compile("([^;]*);(.*)").matcher(code);
        if(m.matches()) {
            Map<String, String> optionsMap = getOptionsMap(m.group(2));
            return new KeyStrokeOptions(nullEmpty(m.group(1)), getBindingModesMap(optionsMap), getPriority(optionsMap));
        } else {
            return new KeyStrokeOptions(nullEmpty(code), null, null);
        }
    }

    public static class KeyStrokeOptions {
        public String keyStroke;
        public Map<String, BindingMode> bindingModesMap;
        public Integer priority;

        public KeyStrokeOptions(String keyStroke, Map<String, BindingMode> bindingModesMap, Integer priority) {
            this.keyStroke = keyStroke;
            this.bindingModesMap = bindingModesMap;
            this.priority = priority;
        }
    }

    public LAWithParams addScriptedInputAProp(ValueClass requestValueClass, LPWithParams oldValue, NamedPropertyUsage targetProp, LAWithParams doAction, LAWithParams elseAction,
                                              List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean assign, boolean constraintFilter, LPWithParams changeProp,
                                              LAPWithParams listProp, LPWithParams whereProp, List<String> actionImages, List<String> keyStrokes, List<List<QuickAccess>> quickAccesses, List<LAWithParams> actions,
                                              DebugInfo.DebugPoint assignDebugPoint, FormSessionScope listScope, String customEditorFunction) throws ScriptingErrorLog.SemanticErrorException {

        if(listScope == null)
            listScope = FormSessionScope.OLDSESSION;

//        assert targetProp == null;
        LP<?> tprop = getInputProp(targetProp, requestValueClass, null);

        if (changeProp == null)
            changeProp = oldValue;

        if(assign && doAction == null) // we will need to add change props, temporary will make this action empty to avoid extra null checks 
            doAction = new LAWithParams(baseLM.getEmpty(), new ArrayList<Integer>());

        boolean notNull = false;

        LA action;
        ImOrderSet<Integer> usedParams;
        if(requestValueClass instanceof CustomClass) {
            ClassFormSelector classForm = new ClassFormSelector((CustomClass) requestValueClass, false);

            ImList<CCCF<ClassFormSelector.VirtualObject>> cccfs = ListFact.EMPTY();
            if(constraintFilter)
                cccfs = ListFact.singleton(new CCCF<>(changeProp, classForm.virtualObject, oldContext.size())); // assuming that there is only one parameter

            if (listProp instanceof LAWithParams)
                errLog.emitNotPrimitiveTypeInListError(parser);
            
            CFEWithParams<ClassFormSelector.VirtualObject> contextEntities = getContextFilterAndListAndActionsEntities(oldContext.size(), SetFact.singletonOrder(classForm.virtualObject),
                    whereProp != null ? ListFact.singleton(whereProp) : ListFact.EMPTY(), (LPWithParams) listProp, cccfs, actionImages, keyStrokes, quickAccesses, actions);
            usedParams = contextEntities.usedParams;

            action = addDialogInputAProp(classForm, tprop, classForm.virtualObject, oldValue != null, contextEntities.orderInterfaces, listScope, contextEntities.list, contextEntities.filters, contextEntities.contextActions, customEditorFunction, notNull);
        } else {
            // optimization. we don't use files on client side (see also DefaultChangeAction.executeCustom())
            if (oldValue != null && requestValueClass instanceof FileClass)
                oldValue = null;

            ILEWithParams contextEntity = getContextListEntity(oldContext.size(), listProp, whereProp, actionImages, keyStrokes, quickAccesses, actions);
            usedParams = contextEntity.usedParams;

            action = addDataInputAProp((DataClass) requestValueClass, tprop, oldValue != null, oldValue != null ? oldValue.getLP().getActionOrProperty() : null, contextEntity.orderInterfaces, contextEntity.list, contextEntity.where, listScope, contextEntity.contextActions, customEditorFunction, notNull);
        }
        
        List<LPWithParams> mapping = new ArrayList<>();

        if(oldValue != null) {
            mapping.add(oldValue);
        }

        for (int usedParam : usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        LAWithParams inputAction;
        if (mapping.size() > 0)
            inputAction = addScriptedJoinAProp(action, mapping);
        else
            inputAction = new LAWithParams(action, Collections.emptyList());

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, ListFact.singleton(tprop), inputAction,
                ListFact.singleton(assign ? new Pair<>(changeProp, assignDebugPoint) : null));
    }

    public LAWithParams addScriptedRequestAProp(LAWithParams requestAction, LAWithParams doAction, LAWithParams elseAction) {
        List<LAPWithParams> propParams = new ArrayList<>();
        propParams.add(requestAction);
        if(doAction != null)
            propParams.add(doAction);
        if(elseAction != null)
            propParams.add(elseAction);

        LA result = addRequestAProp(null, doAction != null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LAWithParams(result, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedActiveFormAProp(String formName) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findForm(formName);
        return new LAWithParams(addAProp(null, new IsActiveFormAction(LocalizedString.NONAME, form, baseLM.getIsActiveFormProperty())), new ArrayList<>());
    }

    public LAWithParams addScriptedActivateAProp(FormEntity form, ComponentView component) {
        return new LAWithParams(addAProp(null, new ActivateAction(LocalizedString.NONAME, form, component)), new ArrayList<>());
    }

    public LAWithParams addScriptedCloseFormAProp(String formId) {
        return new LAWithParams(addAProp(null, new CloseFormAction(LocalizedString.NONAME, formId)), new ArrayList<>());
    }

    public LAWithParams addScriptedCollapseExpandAProp(ComponentView component, boolean collapse) {
        return new LAWithParams(addAProp(null, new ExpandCollapseContainerAction(LocalizedString.NONAME, component, collapse)), new ArrayList<>());
    }

    public List<LP<?>> addLocalDataProperty(List<String> names, String returnClassName, List<String> paramClassNames,
                                            LocalNestedType nestedType, DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {


        List<LP<?>> res = new ArrayList<>();
        ImList<ValueClass> paramClasses = findClasses(paramClassNames);
        List<ResolveClassSet> signature = getParamClasses(paramClasses);
        for (String name : names) {
            LP<?> lcp = addScriptedDProp(returnClassName, paramClasses, true, false, true, nestedType);

            addLocal(lcp, new LocalPropertyData(name, signature));
            lcp.property.setDebugInfo(new PropertyDebugInfo(point, false));
            res.add(lcp);
        }
        return res;
    }

    public LP addWatchLocalDataProperty(LP lp, List<ResolveClassSet> signature) {
        assert lp.property instanceof SessionDataProperty;
        addModuleLAP(lp);
        propClasses.put(lp, signature);
        return lp;
    }

    public LAWithParams addScriptedJoinAProp(NamedPropertyUsage pUsage, List<LPWithParams> properties, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        LA mainAction = findLAByPropertyUsage(pUsage, properties, params);
        return addScriptedJoinAProp(mainAction, properties);
    }

    public LAWithParams addScriptedJoinAProp(LA mainAction, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkParamCount(mainAction, properties.size());

        List<Object> resultParams = getParamsPlainList(properties);
        LA action = addJoinAProp(null, LocalizedString.NONAME, mainAction, resultParams.toArray());
        return new LAWithParams(action, mergeAllParams(properties));
    }

    public LAWithParams addScriptedConfirmProp(LPWithParams messageProp, LPWithParams headerProp, LAWithParams doAction, LAWithParams elseAction, boolean yesNo, List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        LP targetProp = yesNo ? getInputProp(null, LogicalClass.instance, null) : null;

        List<LPWithParams> properties = new ArrayList<>();
        properties.add(messageProp);
        if(headerProp != null) {
            properties.add(headerProp);
        }
        LAWithParams inputAction = new LAWithParams(addConfirmAProp(headerProp != null, yesNo, targetProp,
                getParamsPlainList(properties).toArray()), mergeAllParams(properties));

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, yesNo ? ListFact.singleton(targetProp) : ListFact.EMPTY(), inputAction, yesNo ? ListFact.singleton(null) : ListFact.EMPTY());
    }

    public LAWithParams addScriptedMessageProp(LPWithParams messageProp, LPWithParams headerProp, boolean noWait, MessageClientType type) {
        List<LPWithParams> properties = new ArrayList<>();
        properties.add(messageProp);
        if(headerProp != null) {
            properties.add(headerProp);
        }
        return new LAWithParams(addMAProp(headerProp != null, noWait, type,
                getParamsPlainList(properties).toArray()), mergeAllParams(properties));
    }

    public LAWithParams addScriptedAsyncUpdateProp(LPWithParams asyncProp) {
        List<Object> resultParams = getParamsPlainList(singletonList(asyncProp));
        LA asyncLA = addAsyncUpdateAProp(resultParams.toArray());
        return new LAWithParams(asyncLA, asyncProp.usedParams);
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

    private RegularFilterGroupEntity getSeekFilterGroup(FormEntity form, String formFilterGroupName) {
        return form.getNFRegularFilterGroup(getSeekObjectName(formFilterGroupName), getVersion());
    }

    private String getSeekObjectName(String formObjectName) {
        int pointPos = formObjectName.lastIndexOf('.');
        assert pointPos > 0;

        return formObjectName.substring(pointPos + 1);
    }

    public LAWithParams addScriptedObjectSeekProp(String name, LPWithParams seekProp, UpdateType type) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        ObjectEntity object = getSeekObject(form, name);

        if (object != null) {
            List<Object> resultParams = getParamsPlainList(singletonList(seekProp));
            LA LA = addOSAProp(object, type, resultParams.toArray());
            return new LAWithParams(LA, seekProp.usedParams);
        } else {
            errLog.emitObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LAWithParams addScriptedGroupObjectSeekProp(String name, List<String> objNames, List<LPWithParams> values, UpdateType type) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        GroupObjectEntity groupObject = getSeekGroupObject(form, name);

        ImOrderSet<ObjectEntity> objects = SetFact.EMPTYORDER();
        if (objNames != null) {
            MOrderExclSet<ObjectEntity> mObjects = SetFact.mOrderExclSet(objNames.size());
            for (String objName : objNames) {
                ObjectEntity obj = form.getNFObject(objName, getVersion());
                if (obj == null) {
                    errLog.emitObjectNotFoundError(parser, objName);
                } else if (obj.groupTo != groupObject) {
                    errLog.emitObjectOfGroupObjectError(parser, obj.getSID(), groupObject.getSID());
                }
                mObjects.exclAdd(obj);
            }
            objects = mObjects.immutableOrder();
        }

        if (groupObject != null) {
            List<Object> resultParams = getParamsPlainList(values);
            LA LA = addGOSAProp(null, LocalizedString.NONAME, groupObject, objects, type, resultParams.toArray());
            return new LAWithParams(LA, mergeAllParams(values));
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LAWithParams addScriptedGroupObjectExpandProp(String name, List<String> objNames, List<LPWithParams> values, ExpandCollapseType type, boolean expand) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(name);
        GroupObjectEntity expandGroupObject = getSeekGroupObject(form, name);

        List<ObjectEntity> objects = new ArrayList<>();
        if (objNames != null) {
            for (String objName : objNames) {
                ObjectEntity obj = form.getNFObject(objName, getVersion());
                if (obj == null) {
                    errLog.emitObjectNotFoundError(parser, objName);
                }
                objects.add(obj);
            }
        }

        if (expandGroupObject != null) {
            List<Object> resultParams = getParamsPlainList(values);
            LA LA = addExpandCollapseAProp(expandGroupObject, objects, type, expand, resultParams.toArray());
            return new LAWithParams(LA, mergeAllParams(values));
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(name));
            return null;
        }
    }

    public LAWithParams addScriptedOrderProp(String goName, LPWithParams from, List<TypedParameter> context)  throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity go = getSeekGroupObject(getFormFromSeekObjectName(goName), goName);

        if (go != null) {
            if(from == null) {
                from = new LPWithParams(BL.userEventsLM.orders);
            }
            return addScriptedJoinAProp(baseLM.addOrderAProp(go, (DataClass) getTypeByParamProperty(from, context)), Collections.singletonList(from));
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(goName));
            return null;
        }
    }

    public LAWithParams addScriptedReadOrdersProp(String goName, NamedPropertyUsage propertyUsage)  throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity go = getSeekGroupObject(getFormFromSeekObjectName(goName), goName);

        if (go != null) {
            LP<?> targetProp = propertyUsage != null ? findLPNoParamsByPropertyUsage(propertyUsage) : null;
            return new LAWithParams(baseLM.addReadOrdersAProp(go, targetProp), Collections.emptyList());
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(goName));
            return null;
        }
    }

    public LAWithParams addScriptedFilterProp(String goName, LPWithParams from, List<TypedParameter> context)  throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = getFormFromSeekObjectName(goName);
        GroupObjectEntity go = getSeekGroupObject(form, goName);

        if (go != null) {
            if(from == null) {
                from = new LPWithParams(BL.userEventsLM.filters);
            }
            return addScriptedJoinAProp(baseLM.addFilterAProp(go, (DataClass) getTypeByParamProperty(from, context)), Collections.singletonList(from));
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(goName));
            return null;
        }
    }

    public LAWithParams addScriptedReadFiltersProp(String goName, NamedPropertyUsage propertyUsage)  throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity go = getSeekGroupObject(getFormFromSeekObjectName(goName), goName);

        if (go != null) {
            LP<?> targetProp = propertyUsage != null ? findLPNoParamsByPropertyUsage(propertyUsage) : null;
            return new LAWithParams(baseLM.addReadFiltersAProp(go, targetProp), Collections.emptyList());
        } else {
            errLog.emitGroupObjectNotFoundError(parser, getSeekObjectName(goName));
            return null;
        }
    }

    public LAWithParams addScriptedFilterGroupProp(String fgName, LPWithParams from, List<TypedParameter> context)  throws ScriptingErrorLog.SemanticErrorException {
        RegularFilterGroupEntity filterGroup = getSeekFilterGroup(getFormFromSeekObjectName(fgName), fgName);
        if (filterGroup != null) {
            if(from == null) {
                from = new LPWithParams(BL.userEventsLM.filterGroups);
            }
            return addScriptedJoinAProp(baseLM.addFilterGroupAProp(filterGroup.getID(), (DataClass) getTypeByParamProperty(from, context)), Collections.singletonList(from));
        } else {
            errLog.emitFilterGroupNotFoundError(parser, getSeekObjectName(fgName));
            return null;
        }
    }

    public LAWithParams addScriptedReadFilterGroupsProp(String fgName, NamedPropertyUsage propertyUsage)  throws ScriptingErrorLog.SemanticErrorException {
        RegularFilterGroupEntity filterGroup = getSeekFilterGroup(getFormFromSeekObjectName(fgName), fgName);
        if (filterGroup != null) {
            LP<?> targetProp = propertyUsage != null ? findLPNoParamsByPropertyUsage(propertyUsage) : null;
            return new LAWithParams(baseLM.addReadFilterGroupsAProp(filterGroup.getID(), targetProp), Collections.emptyList());
        } else {
            errLog.emitFilterGroupNotFoundError(parser, getSeekObjectName(fgName));
            return null;
        }
    }

    public LAWithParams addScriptedFilterPropertyProp(PropertyDrawEntity property, LPWithParams from, List<TypedParameter> context)  throws ScriptingErrorLog.SemanticErrorException {
        if(from == null) {
            from = new LPWithParams(BL.userEventsLM.filtersProperty);
        }
        return addScriptedJoinAProp(baseLM.addFilterPropertyAProp(property, (DataClass) getTypeByParamProperty(from, context)), Collections.singletonList(from));
    }

    public LAWithParams addScriptedReadFiltersPropertyProp(PropertyDrawEntity property, NamedPropertyUsage propertyUsage)  throws ScriptingErrorLog.SemanticErrorException {
        LP<?> targetProp = propertyUsage != null ? findLPNoParamsByPropertyUsage(propertyUsage) : null;
        return new LAWithParams(baseLM.addReadFiltersPropertyAProp(property, targetProp), Collections.emptyList());
    }

    public LAWithParams addScriptedEvalAction(LPWithParams property, List<LPWithParams> params, List<TypedParameter> contextParams, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        if(params == null)
            params = Collections.emptyList();

        Type exprType = getTypeByParamProperty(property, contextParams);
        if (!(exprType instanceof StringClass)) {
            errLog.emitEvalExpressionError(parser);
        }
        ImList<Type> paramTypes = getTypesForEvalAction(params, contextParams);

        return addScriptedJoinAProp(addAProp(new EvalAction(paramTypes, action)), BaseUtils.addList(property, params));
    }

    public LAWithParams addScriptedChangePropertyAProp(List<TypedParameter> context, NamedPropertyUsage toPropertyUsage, List<LPWithParams> toPropertyMapping, LPWithParams fromProperty, LPWithParams whereProperty, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        Result<Integer> exParams = new Result<>();
        Pair<LPWithParams, LPWithParams> identityChange = getIdentityLPPropertyUsageWithWhere(context, toPropertyUsage, toPropertyMapping, whereProperty, newContext, exParams);
        return addScriptedChangeAProp(context, fromProperty, identityChange.second, identityChange.first, newContext, exParams.result, debugPoint);
    }

    public LAWithParams addScriptedRecalculatePropertyAProp(List<TypedParameter> context, NamedPropertyUsage propertyUsage, List<LPWithParams> propertyMapping, LPWithParams whereProperty, Boolean classes, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        Pair<LPWithParams, LPWithParams> identityRecalc = getIdentityLPPropertyUsageWithWhere(context, propertyUsage, propertyMapping, whereProperty, newContext, null);
        return addScriptedRecalculateAProp(context, identityRecalc.first, identityRecalc.second, classes);
    }

    public Pair<LPWithParams, LPWithParams> getIdentityLPPropertyUsageWithWhere(List<TypedParameter> context, NamedPropertyUsage propertyUsage, List<LPWithParams> propertyMapping, LPWithParams whereProperty, List<TypedParameter> newContext, Result<Integer> rExParams) throws ScriptingErrorLog.SemanticErrorException {
        LP toPropertyLP = findLPByPropertyUsage(propertyUsage, propertyMapping, newContext);

        // to make change operator work with join property, we need identity parameters : 
        // we have to run through all toPropertyMapping if there is lp, or duplicate parameter, add virtual parameter to newContext with equals this parameter and AND it with whereProperty
        propertyMapping = new ArrayList<>(propertyMapping);
        Set<Integer> usedParams = new HashSet<>();
        int oldContextSize = context.size();
        int exParams = 0;
        for (int i = 0; i < propertyMapping.size(); i++) {
            LPWithParams toParam = propertyMapping.get(i);
            if (toParam.getLP() != null || !usedParams.add(toParam.usedParams.get(0))) {
                Result<LPWithParams> rWhereProperty = new Result<>(whereProperty);
                propertyMapping.set(i, addVirtualParam(newContext.size() + exParams, toParam, rWhereProperty, oldContextSize, newContext));
                whereProperty = rWhereProperty.result;
                exParams++;
            } else {
                Integer param = toParam.usedParams.get(0);
                if(param >= oldContextSize) { // adding new parameters to context
                    assert param == oldContextSize;
                    oldContextSize++;
                }
            }
        }

        LPWithParams toProperty = addScriptedJProp(toPropertyLP, propertyMapping);

        if(rExParams != null)
            rExParams.set(exParams);
        return new Pair<>(toProperty, whereProperty);
    }

    private LPWithParams addVirtualParam(int exParams, LPWithParams toParam, Result<LPWithParams> rWhereProperty, int oldContextSize, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        checkNoExtendContext(toParam.usedParams, oldContextSize, newContext);
                
        LPWithParams newToParam = new LPWithParams(exParams);
        LPWithParams paramWhere = addScriptedEqualityProp("=", newToParam, toParam);
        if(rWhereProperty.result != null)
            rWhereProperty.set(addScriptedJProp(and(false), asList(paramWhere, rWhereProperty.result)));
        else 
            rWhereProperty.set(paramWhere);
        return newToParam;
    }

    private LAWithParams addScriptedChangeAProp(List<TypedParameter> oldContext, LPWithParams fromProperty, LPWithParams whereProperty, LPWithParams toProperty, List<TypedParameter> newContext, int exParams, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkAssignProperty(fromProperty, toProperty);

        List<Integer> allParams = mergeNullableAllParams(toProperty, fromProperty, whereProperty);
        List<Integer> resultInterfaces = getResultInterfaces(oldContext.size(), allParams);
        List<Integer> extendInterfaces = getExtendInterfaces(oldContext.size(), allParams);

        if (!extendInterfaces.isEmpty()) {
            if (fromProperty.getLP() != null && !fromProperty.getLP().property.isExplicitNotNull() &&
                    !(whereProperty != null && getExtendInterfaces(oldContext.size(), whereProperty.usedParams).size() == extendInterfaces.size() && whereProperty.getLP().isFull(extendInterfaces.size()))) {
                // if there are extend params + and where does not have all full params
                LPWithParams orProperty = addScriptedOverrideProp(toList(toProperty, fromProperty), false);
                if (whereProperty == null)
                    whereProperty = orProperty;
                else
                    whereProperty = addScriptedIfProp(toList(whereProperty, orProperty));
            }

            if(exParams > 0)
                newContext = BaseUtils.mergeList(newContext, BaseUtils.toList(exParams, index -> new TypedParameter((ValueClass)null, "v" + index)));

            whereProperty = patchExtendParams(whereProperty, BaseUtils.toList(addScriptedChangeClassProp(toProperty), addScriptedChangeValueClassProp(toProperty, fromProperty)), newContext, oldContext.size(), debugPoint);
        }

        List<LAPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(resI));
        }
        paramsList.add(toProperty);
        paramsList.add(fromProperty);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        
        LA result = addSetPropertyAProp(null, LocalizedString.NONAME, resultInterfaces.size(), whereProperty != null, getParamsPlainList(paramsList).toArray());
        return new LAWithParams(result, resultInterfaces);
    }

    // the same as change but without from
    public LAWithParams addScriptedRecalculateAProp(List<TypedParameter> context, LPWithParams recalcProperty, LPWithParams whereProperty, Boolean classes) {
        List<Integer> resultInterfaces = getResultInterfaces(context.size(), recalcProperty, whereProperty);

        List<LAPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(resI));
        }
        paramsList.add(recalcProperty);
        if(whereProperty != null) {
            paramsList.add(whereProperty);
        }

        LA result = addRecalculatePropertyAProp(resultInterfaces.size(), whereProperty != null, classes, getParamsPlainList(paramsList).toArray());
        return new LAWithParams(result, resultInterfaces);
    }

    public LAWithParams addScriptedAddObjProp(List<TypedParameter> context, String className, NamedPropertyUsage toPropUsage, List<LPWithParams> toPropMapping, LPWithParams whereProperty, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkAddActionsClass(cls, className);
        checks.checkAddObjTOParams(context.size(), toPropMapping);

        LPWithParams toProperty = null;
        if (toPropUsage != null && toPropMapping != null) {
            toProperty = addScriptedJProp(findLPByPropertyUsage(toPropUsage, toPropMapping, newContext), toPropMapping);
        }

        if(whereProperty != null)
            whereProperty = patchExtendParams(whereProperty, newContext, context.size(), debugPoint);

        List<Integer> resultInterfaces = getResultInterfaces(context.size(), toProperty, whereProperty);

        List<LPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(resI));
        }
        if (toProperty != null) {
            paramsList.add(toProperty);
        } else if (whereProperty == null) {
            paramsList.add(new LPWithParams(new LP<>(baseLM.getAddedObjectProperty())));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LA result = addAddObjAProp((CustomClass) cls, false, resultInterfaces.size(), whereProperty != null, toProperty != null || whereProperty == null, resultParams.toArray());
        return new LAWithParams(result, resultInterfaces);
    }

    public LAWithParams addScriptedDeleteAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, LPWithParams whereProperty, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        LAWithParams res = addScriptedChangeClassAProp(oldContextSize, newContext, param, getBaseClass().unknown, whereProperty, debugPoint);
        setDeleteActionOptions(res.getLP());
        return res;
    }

    public LAWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, String className, LPWithParams whereProperty, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass cls = findClass(className);
        checks.checkChangeClassActionClass(cls, className);
        return addScriptedChangeClassAProp(oldContextSize, newContext, param, (ConcreteCustomClass) cls, whereProperty, debugPoint);
    }

    private LAWithParams addScriptedChangeClassAProp(int oldContextSize, List<TypedParameter> newContext, LPWithParams param, ConcreteObjectClass cls, LPWithParams whereProperty, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkChangeClassWhere(oldContextSize, param, whereProperty, newContext);
        
        if(param.getLP() != null) {
            Result<LPWithParams> rWhereProperty = new Result<>(whereProperty);
            param = addVirtualParam(oldContextSize, param, rWhereProperty, oldContextSize, newContext);
            whereProperty = rWhereProperty.result;
        } else
            whereProperty = patchExtendParams(whereProperty, newContext, oldContextSize, debugPoint);
        return addScriptedChangeClassAProp(oldContextSize, param, cls, whereProperty);
    }
    private LAWithParams addScriptedChangeClassAProp(int oldContextSize, LPWithParams param, ConcreteObjectClass cls, LPWithParams whereProperty) {
        
        List<Integer> allParams = mergeNullableAllParams(param, whereProperty);
        int changedIndex = allParams.indexOf(param.usedParams.get(0));

        List<Integer> resultInterfaces = getResultInterfaces(oldContextSize, allParams);
        boolean contextExtended = allParams.size() > resultInterfaces.size();

        List<LAPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(resI));
        }
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);

        LA<?> res = addChangeClassAProp(cls, resultInterfaces.size(), changedIndex, contextExtended, whereProperty != null, resultParams.toArray());
        return new LAWithParams(res,  resultInterfaces);
    }

    public List<Integer> getResultInterfaces(int contextSize, LAPWithParams... params) {
        return getResultInterfaces(contextSize, mergeNullableAllParams(params));
    }
    private List<Integer> getResultInterfaces(int contextSize, List<Integer> allParams) {
        List<Integer> resultInterfaces = new ArrayList<>();
        for (int paramIndex : allParams) {
            if (paramIndex >= contextSize) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }
        return resultInterfaces;
    }
    private List<Integer> getExtendInterfaces(int contextSize, List<Integer> allParams) {
        List<Integer> resultInterfaces = new ArrayList<>();
        for (int paramIndex : allParams)
            if (paramIndex >= contextSize)
                resultInterfaces.add(paramIndex);
        return resultInterfaces;
    }

    public LAWithParams addScriptedIfAProp(LPWithParams condition, LAWithParams trueAction, LAWithParams falseAction) {
        List<LAPWithParams> propParams = toList(condition, trueAction);
        if (falseAction != null) {
            propParams.add(falseAction);
        }
        LA result = addIfAProp(null, LocalizedString.NONAME, false, getParamsPlainList(propParams).toArray());
        return new LAWithParams(result, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedTryAProp(LAWithParams tryAction, LAWithParams catchAction, LAWithParams finallyAction) {
        List<LAPWithParams> propParams = new ArrayList<>();
        propParams.add(tryAction);
        if (catchAction != null) {
            propParams.add(catchAction);
        }if (finallyAction != null) {
            propParams.add(finallyAction);
        }

        LA result = addTryAProp(null, LocalizedString.NONAME, catchAction != null, finallyAction != null, getParamsPlainList(propParams).toArray());
        return new LAWithParams(result, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedCaseAProp(List<LPWithParams> whenProps, List<LAWithParams> thenActions, LAWithParams elseAction, boolean isExclusive) {
        assert whenProps.size() > 0 && whenProps.size() == thenActions.size();

        List<LAPWithParams> caseParams = new ArrayList<>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParams.add(whenProps.get(i));
            caseParams.add(thenActions.get(i));
        }
        if (elseAction != null) {
            caseParams.add(elseAction);
        }

        LA result = addCaseAProp(isExclusive, getParamsPlainList(caseParams).toArray());
        return new LAWithParams(result, mergeAllParams(caseParams));
    }

    public LAWithParams addScriptedMultiAProp(List<LAWithParams> actions, boolean isExclusive) {
        LA result = addMultiAProp(isExclusive, getParamsPlainList(actions).toArray());
        return new LAWithParams(result, mergeAllParams(actions));

    }

    public LAWithParams addScriptedApplyAProp(LAWithParams action, boolean singleApply, List<NamedPropertyUsage> keepSessionProps, boolean keepAllSessionProps, boolean serializable)
            throws ScriptingErrorLog.SemanticErrorException {
        List<LAPWithParams> propParams = Collections.singletonList(action);

        LA result = addApplyAProp(null, LocalizedString.NONAME, (action != null && action.getLP() != null) ? action.getLP() : null, singleApply,
                getMigrateProps(keepSessionProps, keepAllSessionProps), serializable);

        return new LAWithParams(result, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedCancelAProp(List<NamedPropertyUsage> keepSessionProps, boolean keepAllSessionProps)
            throws ScriptingErrorLog.SemanticErrorException {
        LA result = addCancelAProp(null, LocalizedString.NONAME, getMigrateProps(keepSessionProps, keepAllSessionProps));
        return new LAWithParams(result, new ArrayList<>());
    }

    private FunctionSet<SessionDataProperty> getMigrateProps(List<NamedPropertyUsage> keepSessionProps, boolean keepAllSessionProps) throws ScriptingErrorLog.SemanticErrorException {
        FunctionSet<SessionDataProperty> keepProps;
        if(keepAllSessionProps) {
            keepProps = DataSession.keepAllSessionProperties;
        } else {
            MExclSet<SessionDataProperty> mKeepProps = SetFact.mExclSet(keepSessionProps.size());
            for (NamedPropertyUsage migratePropUsage : keepSessionProps) {
                LP<?> prop = findLPByPropertyUsage(migratePropUsage);
                checks.checkSessionProperty(prop);
                mKeepProps.exclAdd((SessionDataProperty) prop.property);
            }
            keepProps = mKeepProps.immutable();
        }
        return keepProps;
    }

    public LAWithParams addScriptedNewAProp(List<TypedParameter> oldContext, LAWithParams action, Integer addNum, String addClassName, Boolean autoSet) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedForAProp(oldContext, null, new ArrayList<>(), action, null, addNum, addClassName, autoSet, false, false, new ArrayList<>(), false);
    }

    public LAWithParams addScriptedForAProp(List<TypedParameter> oldContext, List<TypedParameter> newContext, LPWithParams condition, List<LPWithParams> orders, SelectTop<LPWithParams> selectTop, LAWithParams action, LAWithParams elseAction, Integer addNum, String addClassName, Boolean autoSet, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        boolean ordersNotNull = (condition != null ? doesExtendContext(oldContext.size(), singletonList(condition), orders) : !orders.isEmpty());

        condition = patchExtendParams(condition, orders, ordersNotNull, BaseUtils.removeList(newContext, addNum != null ? addNum : -1), oldContext.size(), debugPoint);

        return addScriptedForAProp(oldContext, condition, orders, ordersNotNull, selectTop, action, elseAction, addNum, addClassName, autoSet, recursive, descending, noInline, forceInline);
    }

    public LAWithParams addScriptedForAProp(List<TypedParameter> oldContext, LPWithParams condition, List<LPWithParams> orders, boolean ordersNotNull, SelectTop<LPWithParams> selectTop, LAWithParams action, LAWithParams elseAction, Integer addNum, String addClassName, Boolean autoSet, boolean recursive, boolean descending, List<LPWithParams> noInline, boolean forceInline) throws ScriptingErrorLog.SemanticErrorException {
        if(selectTop == null)
            selectTop = SelectTop.NULL();

        List<LAPWithParams> creationParams = new ArrayList<>();
        if (condition != null) {
            creationParams.add(condition);
        }
        creationParams.addAll(orders);
        if(addNum != null) {
            creationParams.add(new LPWithParams(addNum));
        }
        if (elseAction != null) {
            creationParams.add(elseAction);
        }
        creationParams.add(action);
        List<Integer> allParams = mergeAllParams(creationParams);
        List<Integer> usedParams = getResultInterfaces(oldContext.size(), allParams);
        List<Integer> extParams = getExtendInterfaces(oldContext.size(), allParams);

        if(ActionDebugger.watchHack.get() != null && extParams.size() > 1) {
            ActionDebugger.watchHack.set(true);
        }

        checks.checkForActionConstraints(recursive, usedParams, allParams);

        List<LAPWithParams> allCreationParams = new ArrayList<>();
        for (int usedParam : usedParams) {
            allCreationParams.add(new LPWithParams(usedParam));
        }
        allCreationParams.addAll(creationParams);
        if(noInline==null) { // предполагается надо включить все кроме addNum
            noInline = new ArrayList<>();
            for (int extParam : extParams)
                if(addNum==null || !addNum.equals(extParam)) {
                    noInline.add(new LPWithParams(extParam));
                }
        }
        allCreationParams.addAll(noInline);

        LA result = addForAProp(LocalizedString.NONAME, descending, ordersNotNull, selectTop.CONST(), recursive, elseAction != null, usedParams.size(),
                                addClassName != null ? (CustomClass) findClass(addClassName) : null, autoSet != null ? autoSet : false, condition != null, noInline.size(), forceInline,
                                getParamsPlainList(allCreationParams).toArray());

        if(!selectTop.isEmpty()) { // optimization
            List<LPWithParams> mapping = new ArrayList<>();
            for(int resultInterface : usedParams)
                mapping.add(new LPWithParams(resultInterface));

            mapping.addAll(selectTop.getParams());

            LAWithParams lgp = addScriptedJoinAProp(result, mapping);

            result = lgp.getLP();
            usedParams = lgp.usedParams;
        }

        return new LAWithParams(result, usedParams);
    }

    public LP addScriptedGProp(List<LPWithParams> groupProps, GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> orderProps, boolean ordersNotNull, SelectTop<Integer> selectTop, boolean descending, Supplier<ConstraintData> constraintData, LPWithParams whereProp, List<ResolveClassSet> explicitInnerClasses) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkGPropAggrConstraints(type, mainProps, groupProps);
        checks.checkGPropAggregateConsistence(type, mainProps);
        checks.checkGPropWhereConsistence(type, whereProp);
        checks.checkGPropSumConstraints(type, mainProps.isEmpty() ? null : mainProps.get(0));

        List<LPWithParams> whereProps = new ArrayList<>();
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR || (type == GroupingType.CONCAT && whereProp != null)) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(mainProps.get(0).usedParams.get(0)));
            }
        }
        List<Object> resultParams = getParamsPlainList(mainProps, whereProps, orderProps, groupProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps, whereProps)).size();
        assert groupPropParamCount == explicitInnerClasses.size();
        LocalizedString emptyCaption = LocalizedString.NONAME;
        LP resultProp;
        if ((type == GroupingType.LAST || type == GroupingType.CONCAT || type instanceof CustomGroupingType) || !selectTop.isEmpty()) {
            resultProp = addOGProp(null, false, emptyCaption, getGroupType(type), whereProp != null, mainProps.size(), orderProps.size(), ordersNotNull, selectTop, descending, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.SUM) {
            resultProp = addSGProp(null, false, false, emptyCaption, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.MAX || type == GroupingType.MIN) {
            resultProp = addMGProp(null, emptyCaption, type == GroupingType.MIN, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            resultProp = addAGProp(null, false, false, emptyCaption, type == GroupingType.NAGGR, constraintData, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else if (type == GroupingType.EQUAL) {
            resultProp = addCGProp(null, false, false, emptyCaption, constraintData, null, groupPropParamCount, explicitInnerClasses, resultParams.toArray());
        } else
            throw new UnsupportedOperationException();
        return resultProp;
    }

    public LAWithParams getTerminalFlowAction(ChangeFlowActionType type) {
        ChangeFlowAction action = null;
        switch (type) {
            case BREAK:
                action = new BreakAction();
                break;
            case CONTINUE:
                action = new ContinueAction();
                break;
            case RETURN:
                action = new ReturnAction();
                break;
        }
        return new LAWithParams(new LA<>(action), new ArrayList<>());
    }

    private List<Integer> getParamsAssertList(List<LPWithParams> list) {
        List<Integer> result = new ArrayList<>();
        for(LPWithParams lp : list) {
            assert lp.getLP() == null;
            result.add(BaseUtils.single(lp.usedParams));
        }
        return result;
    }

    @SafeVarargs
    private final List<Object> getParamsPlainList(List<? extends LAPWithParams>... mappedPropLists) {
        List<LAP> props = new ArrayList<>();
        List<List<Integer>> usedParams = new ArrayList<>();
        for (List<? extends LAPWithParams> mappedPropList : mappedPropLists) {
            for (LAPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.getLP());
                usedParams.add(mappedProp.usedParams);
            }
        }
        return getParamsPlainList(props, usedParams);
    }

    private List<Object> getParamsPlainList(List<LAP> paramProps, List<List<Integer>> usedParams) {
        List<Integer> allUsedParams = mergeIntLists(usedParams);
        List<Object> resultParams = new ArrayList<>();

        for (int i = 0; i < paramProps.size(); i++) {
            LAP property = paramProps.get(i);
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

    @NotNull
    private static GroupType getGroupType(GroupingType type) {
        GroupType groupType;
        if(type == GroupingType.LAST)
            groupType = GroupType.LAST;
        else if (type == GroupingType.CONCAT)
            groupType = GroupType.CONCAT;
        else if (type == GroupingType.SUM)
            groupType = GroupType.SUM;
        else if (type == GroupingType.MAX)
            groupType = GroupType.MAX;
        else if (type == GroupingType.MIN)
            groupType = GroupType.MIN;
        else {
            CustomGroupingType customType = (CustomGroupingType) type;
            groupType = GroupType.CUSTOM(customType.aggrFunc, customType.setOrdered, customType.dataClass, customType.valueNull);
        }
        return groupType;
    }

    // второй результат в паре использованные параметры из внешнего контекста (LP на выходе имеет сначала эти использованные параметры, потом группировки)
    public LPContextIndependent addScriptedCDIGProp(int oldContextSize, List<LPWithParams> groupProps, GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> orderProps,
                                                    boolean descending, LPWithParams whereProp, SelectTop<LPWithParams> selectTop, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.LAST) {
            if (whereProp != null) {
                mainProps.add(0, whereProp);
            } else {
                mainProps.add(mainProps.get(0));
            }
            whereProp = null;
        }

        List<LPWithParams> whereProps = whereProp != null ? singletonList(whereProp) : Collections.emptyList();

        List<LPWithParams> lpWithParams = mergeLists(groupProps, mainProps, orderProps, whereProps);
        List<Integer> resultInterfaces = getResultInterfaces(oldContextSize, lpWithParams.toArray(new LAPWithParams[lpWithParams.size()]));

        List<LPWithParams> allGroupProps = getAllGroupProps(resultInterfaces, groupProps, true);

        List<ResolveClassSet> explicitInnerClasses = getClassesFromTypedParams(oldContextSize, resultInterfaces, newContext);

        List<LPWithParams> notNullParams = mergeLists(whereProps, removeList(mainProps, type.getSkipWhereIndex()), groupProps);
        boolean ordersNotNull = doesExtendContext(0, notNullParams, orderProps);

        LPWithParams patchedWhere = patchExtendParams(notNullParams, orderProps, ordersNotNull, newContext, oldContextSize, debugPoint);
        if(whereProp != null || (type == GroupingType.AGGR || type == GroupingType.NAGGR))
            whereProp = patchedWhere;
        else
            mainProps.set(0, patchedWhere);

        Supplier<ConstraintData> constraintCaption = () -> getConstraintData("{logics.property.derived.violate.property.uniqueness.for.objects}", allGroupProps, debugPoint);

        LP gProp = addScriptedGProp(allGroupProps, type, mainProps, orderProps, ordersNotNull, selectTop.CONST(), descending, constraintCaption, whereProp, explicitInnerClasses);

        if(!selectTop.isEmpty()) { // optimization
            List<LPWithParams> mapping = new ArrayList<>();
            for(int resultInterface : resultInterfaces)
                mapping.add(new LPWithParams(resultInterface));

            mapping.addAll(selectTop.getParams());

            LPWithParams lgp = addScriptedJProp(gProp, mapping);

            gProp = lgp.getLP();
            resultInterfaces = lgp.usedParams;
        }

        return new LPContextIndependent(gProp, getParamClassesByParamProperties(allGroupProps, newContext), resultInterfaces);
    }

    // ci - надо в дырки вставлять, от использованных, если не ci то в конце
    public List<LPWithParams> getAllGroupProps(List<Integer> resultInterfaces, List<LPWithParams> groupProps, boolean ci) {
        List<LPWithParams> allGroupProps = new ArrayList<>();

        if(ci) {
            Set<Integer> usedInterfaces = new HashSet<>(resultInterfaces);
//        нужно groupProps в дырки вставить для context independent группировки
            int ra = 0, ga = 0;
            int groupSize = groupProps.size();
            for (int i = 0, size = resultInterfaces.size() + groupSize; i < size; i++) {
                LPWithParams add;
                if (ga >= groupSize || usedInterfaces.contains(i))
                    add = new LPWithParams(resultInterfaces.get(ra++));
                else
                    add = groupProps.get(ga++);
                allGroupProps.add(add);
            }
        } else {
            for (int resI : resultInterfaces) {
                allGroupProps.add(new LPWithParams(resI));
            }
            allGroupProps.addAll(groupProps);
        }

        return allGroupProps;
    }

    public Pair<LPWithParams, LPContextIndependent> addScriptedCDGProp(int oldContextSize, List<LPWithParams> groupProps, GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> orderProps,
                                                                       boolean descending, LPWithParams whereProp, SelectTop<LPWithParams> selectTop, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if(selectTop == null)
            selectTop = SelectTop.NULL();
        if(groupProps == null)
            groupProps = Collections.emptyList();
        LPContextIndependent ci = addScriptedCDIGProp(oldContextSize, groupProps, type, mainProps, orderProps, descending, whereProp, selectTop, newContext, debugPoint);
        if(groupProps.size() > 0)
            return new Pair<>(null, ci);
        else
            return new Pair<>(new LPWithParams(ci.property, ci.usedContext), null);
    }

    public List<ResolveClassSet> getClassesFromTypedParams(int oldContextSize, List<Integer> resultInterfaces, List<TypedParameter> newContext) {
        List<TypedParameter> usedInnerInterfaces = new ArrayList<>();
        for (int resI : resultInterfaces)
            usedInnerInterfaces.add(newContext.get(resI));
        usedInnerInterfaces.addAll(newContext.subList(oldContextSize, newContext.size()));
        return getClassesFromTypedParams(usedInnerInterfaces);
    }
    public ImList<ValueClass> getValueClassesFromTypedParams(int oldContextSize, List<Integer> resultInterfaces, List<TypedParameter> newContext) {
        List<TypedParameter> usedInnerInterfaces = new ArrayList<>();
        for (int resI : resultInterfaces)
            usedInnerInterfaces.add(newContext.get(resI));
        usedInnerInterfaces.addAll(newContext.subList(oldContextSize, newContext.size()));
        return getValueClassesFromTypedParams(usedInnerInterfaces);
    }

    public <T extends PropertyInterface> LPContextIndependent addScriptedAGProp(List<TypedParameter> context, String aggClassName, LPWithParams whereExpr, Event aggrEvent, DebugInfo.DebugPoint aggrDebugPoint, Event newEvent, DebugInfo.DebugPoint newDebugPoint, Event deleteEvent, DebugInfo.DebugPoint deleteDebugPoint, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoInline(innerPD);

        whereExpr = patchExtendParams(whereExpr, context, 0, aggrDebugPoint);

        LP<?> whereLP = whereExpr.getLP();

        ValueClass aggClass = findClass(aggClassName);
        checks.checkAggrClass(aggClass, aggClassName);
        checks.checkParamCount(whereLP, context.size());

//        prim1Object = DATA prim1Class (aggrClass) INDEXED;
//        prim2Object = DATA prim2Class (aggrClass) INDEXED;

        List<LPWithParams> groupProps = new ArrayList<>();
        List<ResolveClassSet> resultSignature = new ArrayList<>();
        ResolveClassSet aggSignature = aggClass.getResolveSet();
        Object[] prevGroupProps = new Object[context.size() * 2];
        for (int i = 0, contextSize = context.size(); i < contextSize; i++) {
            TypedParameter param = context.get(i);
            LP lp = addDProp(LocalizedString.NONAME, param.cls, aggClass);

            makePropertyPublic(lp, param.paramName, aggSignature);
            lp.property.markStored(null);

            groupProps.add(new LPWithParams(lp, 0));
            prevGroupProps[2 * i] = lp.getOld((deleteEvent != null ? deleteEvent : aggrEvent.onlyScope()).getScope());
            prevGroupProps[2 * i + 1] = 1;
            resultSignature.add(param.cls.getResolveSet());
        }

//        aggrObject (prim1Object, prim2Object) =
//                GROUP AGGR aggrClass aggrObject
//        WHERE aggrObject IS aggrClass BY prim1Object(aggrObject), prim2Object(aggrObject);
        LP<T> aggrObjectLP = addScriptedGProp(groupProps, GroupingType.AGGR, Collections.singletonList(new LPWithParams(0)), Collections.emptyList(), false, SelectTop.NULL(), false,
                () -> getConstraintData("{logics.property.violated.aggr.unique}", aggClass, whereLP, aggrDebugPoint), new LPWithParams(is(aggClass), 0), Collections.singletonList(aggSignature));
        ((AggregateGroupProperty) aggrObjectLP.property).isFullAggr = true;

        // RESOLVING
        List<PropertyFollowsDebug> resolveNew, resolveDelete;

        resolveNew = singletonList(new PropertyFollowsDebug(newEvent, true, false, newDebugPoint, LocalizedString.concatList(LocalizedString.create("{logics.property.violated.aggr.new.resolve}"), ": " + aggrDebugPoint.toString())));
        resolveDelete = singletonList(new PropertyFollowsDebug(deleteEvent, false, false, deleteDebugPoint, LocalizedString.concatList(LocalizedString.create("{logics.property.violated.aggr.delete.resolve}"), ": " + aggrDebugPoint.toString())));

        // this provides simpler events, with the more relevant event order (because we eliminate a lot of recursive links, especially in DELETE event)
        // WHEN SET(aggrExpr) DO SETNOTNULL lcp
        ImList<LP> optResNewConds = useOptResolve ? ListFact.singleton(whereLP.getChanged(IncrementType.SET, (newEvent != null ? newEvent : aggrEvent.onlyScope()).getScope())) : null;
        // WHEN DROPPED(aggrExpr)(PREV(prim1Object(aggrObject)), PREV(prim2Object(aggrObject)) DO DELETE aggrObject // we need prev because the param object in aggrExpr can be deleted => aggobject will be changed to null
        ImList<LP> optResDeleteConds = useOptResolve ? ListFact.singleton(addJProp(whereLP.getChanged(IncrementType.DROP, (deleteEvent != null ? deleteEvent : aggrEvent.onlyScope()).getScope()), prevGroupProps)) : null;

//        aggrProperty(prim1Class prim1Object, prim2Class prim2Object) => aggrObject(prim1Object, prim2Object) RESOLVE LEFT; // new
        addScriptedFollows(whereLP, new LPWithParams(aggrObjectLP, whereExpr), resolveNew, optResNewConds, aggrEvent, getConstraintData("{logics.property.violated.aggr.new}", aggClass, whereLP, aggrDebugPoint).noUseDebugPoint());

//        aggrObject IS aggrClass => aggrProperty(prim1Object(aggrObject), prim2Object(aggrObject)) RESOLVE RIGHT; // delete
        addScriptedFollows(is(aggClass), addScriptedJProp(whereLP, groupProps), resolveDelete, optResDeleteConds, aggrEvent, getConstraintData("{logics.property.violated.aggr.delete}", aggClass, whereLP, aggrDebugPoint).noUseDebugPoint());

        return new LPContextIndependent(aggrObjectLP, resultSignature, Collections.emptyList());
    }

    public LPWithParams addScriptedPartitionProp(PartitionType partitionType, NamedPropertyUsage ungroupPropUsage, boolean strict, int precision, boolean descending, SelectTop<LPWithParams> selectTop,
                                                 int exprCnt, int groupPropsCnt, List<LPWithParams> paramProps, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
//        checks.checkPartitionWindowConsistence(partitionType, useLast);
        if(selectTop == null)
            selectTop = SelectTop.NULL();

        LP ungroupProp = ungroupPropUsage != null ? findLPByPropertyUsage(ungroupPropUsage, paramProps.subList(0, groupPropsCnt), context) : null;
        checks.checkPartitionUngroupConsistence(ungroupProp, groupPropsCnt);

        Pair<LP, List<Integer>> result = addScriptedPartitionProp(partitionType, strict, precision, descending, selectTop.CONST(), exprCnt, groupPropsCnt, paramProps, context, ungroupProp);

        if(!selectTop.isEmpty()) { // optimization
            List<LPWithParams> mapping = new ArrayList<>();
            for(int resultInterface : result.second)
                mapping.add(new LPWithParams(resultInterface));

            mapping.addAll(selectTop.getParams());

            return addScriptedJProp(result.first, mapping);
        }

        return new LPWithParams(result.first, result.second);
    }

    public static boolean useOptResolve = true;

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
        LP prop = addUProp(null, LocalizedString.NONAME, unionType, coeffs, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    @NotNull
    private Pair<LP, List<Integer>> addScriptedPartitionProp(PartitionType partitionType, boolean strict, int precision, boolean descending, SelectTop<Integer> selectTop, int exprCnt, int groupPropsCnt, List<LPWithParams> paramProps, List<TypedParameter> context, LP ungroupProp) {
        boolean ordersNotNull = doesExtendContext(0, paramProps.subList(0, groupPropsCnt + exprCnt), paramProps.subList(groupPropsCnt + exprCnt, paramProps.size()));

        List<Object> resultParams = getParamsPlainList(paramProps);
        List<Integer> usedParams = mergeAllParams(paramProps);
        LP prop;
        if (partitionType == PartitionType.sum() || partitionType == PartitionType.previous() || partitionType == PartitionType.select() || partitionType instanceof PartitionType.Custom) {
            prop = addOProp(null, false, LocalizedString.NONAME, partitionType, descending, ordersNotNull, selectTop, exprCnt, groupPropsCnt, resultParams.toArray());
        } else if (partitionType == PartitionType.distrCumProportion()) {
            assert exprCnt == 1;
            List<ResolveClassSet> contextClasses = getClassesFromTypedParams(context);// для не script - временный хак
            // может быть внешний context
            List<ResolveClassSet> explicitInnerClasses = new ArrayList<>();
            for(int usedParam : usedParams)
                explicitInnerClasses.add(contextClasses.get(usedParam)); // one-based;
            prop = addPGProp(null, false, precision, strict, LocalizedString.NONAME, usedParams.size(), explicitInnerClasses, descending, ordersNotNull, ungroupProp, resultParams.toArray());
        } else {
            assert exprCnt == 1;
            prop = addUGProp(null, false, strict, LocalizedString.NONAME, usedParams.size(), descending, ordersNotNull, ungroupProp, resultParams.toArray());
        }
        return new Pair<>(prop, usedParams);
    }

    public <O extends ObjectSelector> LAWithParams addScriptedPrintFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps, FormPrintType printType,
                                                                          boolean server, boolean autoPrint, NamedPropertyUsage propUsage, Boolean syncType,
                                                                          MessageClientType messageType, FormSelectTop<LPWithParams> selectTop,
                                                                          LPWithParams printerProperty, LPWithParams sheetNameProperty, LPWithParams passwordProperty,
                                                                          List<TypedParameter> objectsContext, List<LPWithParams> contextFilters, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {
        assert printType != null;

        if(selectTop == null)
            selectTop = FormSelectTop.NULL();

        ImList<O> mappedObjects = mapped.objects;
        ImOrderSet<O> contextObjects = getMappingObjectsArray(mapped, objectsContext);

        List<LPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mList(mappedObjects.size());

        for (int i = 0; i < mappedObjects.size(); i++) {
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        if(syncType == null)
            syncType = false;

        LP<?> targetProp = null;
        if(propUsage != null)
            targetProp = findLPNoParamsByPropertyUsage(propUsage);

        ValueClass printer = printerProperty != null ? getValueClassByParamProperty(printerProperty, params) : null;
        ValueClass sheetName = sheetNameProperty != null ? getValueClassByParamProperty(sheetNameProperty, params) : null;
        ValueClass password = passwordProperty != null ? getValueClassByParamProperty(passwordProperty, params) : null;

        CFEWithParams<O> contextEntities = getContextFilterEntities(params.size(), contextObjects, ListFact.fromJavaList(contextFilters));

        LA action = addPFAProp(null, LocalizedString.NONAME, mapped.form, mappedObjects, mNulls.immutableList(),
                contextEntities.orderInterfaces, contextEntities.filters, printType, server, autoPrint, syncType,
                messageType, targetProp, false, selectTop.mapValues(this, params), printer, sheetName, password);

        for (int usedParam : contextEntities.usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        mapping.addAll(selectTop.getParams());
        if(printerProperty != null) {
            mapping.add(printerProperty);
        }
        if(sheetNameProperty != null) {
            mapping.add(sheetNameProperty);
        }
        if(passwordProperty != null) {
            mapping.add(passwordProperty);
        }

        if (mapping.size() > 0)  {
            return addScriptedJoinAProp(action, mapping);
        } else {
            return new LAWithParams(action, Collections.emptyList());
        }
    }

    public LPWithParams addScriptedCCProp(List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addCCProp(params.size()), params);
    }

    public LPWithParams addScriptedConcatProp(String separatorValue, LPWithParams separatorProperty, List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        if(separatorValue != null) {
            return addScriptedJProp(addSFUProp(separatorValue, params.size()), params);
        } else {
            List<LPWithParams> resultParams = new ArrayList<>();
            resultParams.add(addScriptedJProp(addSFUProp(BaseUtils.impossibleString, params.size()), params));
            resultParams.add(new LPWithParams(baseLM.impossibleString));
            resultParams.add(separatorProperty);
            return addScriptedJProp(baseLM.replace, resultParams);
        }
    }

    public LPWithParams addScriptedDCCProp(LPWithParams ccProp, int index) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDeconcatenateIndex(ccProp, index);
        return addScriptedJProp(addDCCProp(index - 1), Collections.singletonList(ccProp));
    }

    public LP addScriptedSFProp(String valueClassName, String valueName, ImList<ValueClass> paramClasses, List<String> paramNames, List<SQLSyntaxType> types, List<String> implTexts, boolean valueNull, boolean paramsNull) throws ScriptingErrorLog.SemanticErrorException {
        ParsedFormula parsed = parseFormula(types, implTexts, paramNames);

        ValueClass cls = null;
        if (valueClassName != null) {
            cls = findClass(valueClassName);
            checks.checkFormulaClass(cls);
        }

        return addSFProp(parsed.formula, (DataClass) cls, valueName, BaseUtils.immutableCast(paramClasses), parsed.paramNames, valueNull, paramsNull);
    }

    private static class ParsedSingleFormula {
        public final String formula;
        public final ImOrderSet<String> paramNames; // translated params
        public final ImSet<String> usedParams;

        public ParsedSingleFormula(String formula, ImOrderSet<String> paramNames, ImSet<String> usedParams) {
            this.formula = formula;
            this.usedParams = usedParams;
            this.paramNames = paramNames;
        }
    }
    private ParsedSingleFormula parseSingleFormula(String formula, List<String> fieldNames) throws ScriptingErrorLog.SemanticErrorException {
        MSet<String> mUsedParams = SetFact.mSet();

        boolean explicitFields = fieldNames != null;
        fieldNames = explicitFields ? new ArrayList<>(fieldNames) : new ArrayList<>(); // we have to copy the array because we'll change it

        // building search pattern + filling missing parameters
        String patternString = "\\$\\d+"; // looking for $i
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            if (fieldName != null)
                patternString += "|\\$" + fieldName;
            else
                fieldNames.set(i, i + "i");
        }
        patternString = "(" + patternString + ")\\b"; // word boundaries added to be able to match prm10+

        // converting $i -> $paramName + getting used params + filling missing ones
        StringBuffer result = new StringBuffer();
        Matcher m = Pattern.compile(patternString).matcher(formula);
        while (m.find()) {
            String param = m.group().substring(1);

            if(!fieldNames.contains(param)) { // can be index or fieldName
                int paramIndex = Integer.parseInt(param) - 1;

                if(paramIndex >= fieldNames.size()) {
//                    if(explicitFields) have to emit error
                    for(int i = fieldNames.size(); i <= paramIndex; i++)
                        fieldNames.add(i + "i");
                }

                param = fieldNames.get(paramIndex);
            }

            m.appendReplacement(result, CallAction.getParamName(param)); // we have to replace to param name,
            mUsedParams.add(param); // mark that param is used
        }
        m.appendTail(result);
        ImSet<String> usedParams = mUsedParams.immutable();

        // adding $ to the used params
        MOrderExclSet<String> mParamNames = SetFact.mOrderExclSet(fieldNames.size());
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            if(usedParams.contains(fieldName))
                fieldName = CallAction.getParamName(fieldName);
            mParamNames.exclAdd(fieldName);
        }
        usedParams = usedParams.mapSetValues(CallAction::getParamName);
        ImOrderSet<String> paramNames = mParamNames.immutableOrder();

        return new ParsedSingleFormula(result.toString(), paramNames, usedParams);
    }

    private static class ParsedFormula {
        public final CustomFormulaSyntax formula;
        public final ImOrderSet<String> paramNames; // all params

        public ParsedFormula(CustomFormulaSyntax formula, ImOrderSet<String> paramNames) {
            this.formula = formula;
            this.paramNames = paramNames;
        }
    }
    private ParsedFormula parseFormula(List<SQLSyntaxType> types, List<String> implTexts, List<String> fieldNames) throws ScriptingErrorLog.SemanticErrorException {
        assert types.size() == implTexts.size();
        checks.checkSingleImplementation(types);

        String defaultFormula = "";
        MExclMap<SQLSyntaxType, String> mSyntaxes = MapFact.mExclMap();
        ImOrderSet<String> paramNames = null;
        ImSet<String> usedParams = null;

        for (int i = 0; i < types.size(); i++) {
            SQLSyntaxType type = types.get(i);
            String implText = implTexts.get(i);
            ParsedSingleFormula singleFormula = parseSingleFormula(implText, fieldNames);
            String text = singleFormula.formula;

            if (type == null)
                defaultFormula = text;
            else
                mSyntaxes.exclAdd(type, text);

            if(paramNames == null) {
                paramNames = singleFormula.paramNames;
                usedParams = singleFormula.usedParams;
            } else {
                if(!paramNames.equals(singleFormula.paramNames) && !usedParams.equals(singleFormula.usedParams))
                    errLog.emitFormulaDifferentParamCountError(parser, implTexts.get(0), implText);
            }
        }
        ImMap<SQLSyntaxType, String> syntaxes = mSyntaxes.immutable();

        return new ParsedFormula(new CustomFormulaSyntax(defaultFormula, syntaxes, usedParams), paramNames);
    }

    public static String transformFormulaText(String text, String textTo) {
        return text != null ? text.replaceAll("\\$(\\d+)", textTo) : null;
    }

    public LPWithParams addScriptedRProp(List<TypedParameter> context, LPWithParams zeroStep, LPWithParams nextStep, Cycle cycleType, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
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
        LP res = addRProp(null, false, LocalizedString.NONAME, cycleType, debugPoint, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

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
            return getv(false, ExtInt.UNLIMITED);
        return getv(new ExtInt(value.getSourceString().length()));
    }

    public Pair<LPWithParams, LPNotExpr> addConstantProp(ConstType type, Object value, int lineNumber, List<TypedParameter> context, boolean dynamic) throws RecognitionException {
        LP lp = null;
        switch (type) {
            case INT: lp = addUnsafeCProp(IntegerClass.instance, value); break;
            case LONG: lp =  addUnsafeCProp(LongClass.instance, value); break;
            case NUMERIC: lp =  addNumericConst((BigDecimal) value); break;
            case REAL: lp =  addUnsafeCProp(DoubleClass.instance, value); break;
            case RSTRING:
                LocalizedString rlstr = getRawLocalizedStringLiteralText((String) value);
                lp = addUnsafeCProp(getStringConstClass(rlstr), rlstr);
                return Pair.create(new LPWithParams(lp), new LPLiteral(rlstr));
                
            case STRING:
                String str = unquote((String) value);
                if (isInlineSequence(str)) {
                    return Pair.create(addStringInlineProp(str, lineNumber, context, dynamic), null);
                } else if (containsSpecialSequence(str)) {
                    return Pair.create(addStringInterpolateProp(str, lineNumber, context, dynamic), null);
                } else {
                    LocalizedString lstr = transformLocalizedStringLiteral((String) value);
                    lp = addUnsafeCProp(getStringConstClass(lstr), lstr);
                    return Pair.create(new LPWithParams(lp), new LPLiteral(lstr));
                }
            case LOGICAL: lp =  addUnsafeCProp(LogicalClass.instance, value); break;
            case TLOGICAL: lp =  addUnsafeCProp(LogicalClass.threeStateInstance, value); break;
            case DATE: lp =  addUnsafeCProp(DateClass.instance, value); break;
            case DATETIME: lp =  addUnsafeCProp(DateTimeClass.instance, value); break;
            case TIME: lp =  addUnsafeCProp(TimeClass.instance, value); break;
            case STATIC: return addStaticClassConst((String) value);
            case COLOR: lp =  addUnsafeCProp(ColorClass.instance, value); break;
            case NULL: lp =  baseLM.vnull; break;
        }
        return Pair.create(new LPWithParams(lp), new LPLiteral(value));
    }

    protected LPWithParams addStringInlineProp(String source, int lineNumber, List<TypedParameter> context, boolean dynamic) throws ScriptingErrorLog.SemanticErrorException {
        String resourceName = transformToResourceName(source.substring(INLINE_PREFIX.length(), source.length() - 1));
        String code = parseStringInlineProp(resourceName);
        code = quote(escapeInlineContent(code));
        return parser.runStringInterpolateCode(this, code, resourceName, lineNumber, context, dynamic);
    }

    private String parseStringInlineProp(String resourceName) throws ScriptingErrorLog.SemanticErrorException {
        String result = ResourceUtils.findResourceAsString(resourceName, true, false, null, "web");
        if (result == null)
            errLog.emitNotFoundError(parser, "file", resourceName);

        return result;
    }

    protected LPWithParams addStringInterpolateProp(String source, int lineNumber, List<TypedParameter> context, boolean dynamic) throws ScriptingErrorLog.SemanticErrorException {
        String code = null;
        try {
            code = StringUtils.join(ScriptedStringUtils.parseStringInterpolateProp(source), " + ");
        } catch (TransformationError e) {
            errLog.emitSimpleError(parser, e.getMessage());
        }
        return parser.runStringInterpolateCode(this, code, null, lineNumber, context, dynamic);
    }

    private LP addNumericConst(BigDecimal value) {
        //precision() of bigDecimal 0.x is incorrect
        int precision = value.abs().compareTo(BigDecimal.ONE) < 1 ? (value.scale() + 1) : value.precision();
        return addUnsafeCProp(NumericClass.get(precision, value.scale()), value);
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

    public BigDecimal createScriptedNumeric(String literalText) throws ScriptingErrorLog.SemanticErrorException {
        BigDecimal res = BigDecimal.ZERO;
        try {
            res = new BigDecimal(literalText);
        } catch (NumberFormatException e) {
            errLog.emitNumericValueError(parser, literalText);
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

    private void validateTime(int h, int m, int s) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkRange("hour component", h, 0, 23);
        checks.checkRange("minute component", m, 0, 59);
        checks.checkRange("seconds component", s, 0, 59);
    }

    private void validateDateTime(int y, int m, int d, int h, int mn, int s) throws ScriptingErrorLog.SemanticErrorException {
        validateDate(y, m, d);
        validateTime(h, mn, s);
    }

    public LocalDate dateLiteralToDate(String text) throws ScriptingErrorLog.SemanticErrorException {
        int y = Integer.parseInt(text.substring(0, 4));
        int m = Integer.parseInt(text.substring(5, 7));
        int d = Integer.parseInt(text.substring(8, 10));
        validateDate(y, m, d);
        return LocalDate.of(y, m, d);
    }

    public LocalDateTime dateTimeLiteralToTimestamp(String text) throws ScriptingErrorLog.SemanticErrorException {
        int y = Integer.parseInt(text.substring(0, 4));
        int m = Integer.parseInt(text.substring(5, 7));
        int d = Integer.parseInt(text.substring(8, 10));
        int h = Integer.parseInt(text.substring(11, 13));
        int mn = Integer.parseInt(text.substring(14, 16));
        int s = text.length() == 19 ? Integer.parseInt(text.substring(17, 19)) : 0;
        validateDateTime(y, m, d, h, mn, s);
        return LocalDateTime.of(y, m, d, h, mn, s);
    }

    public LocalTime timeLiteralToTime(String text) throws ScriptingErrorLog.SemanticErrorException {
        int h = Integer.parseInt(text.substring(0, 2));
        int m = Integer.parseInt(text.substring(3, 5));
        int s = text.length() == 8 ? Integer.parseInt(text.substring(6, 8)) : 0;
        validateTime(h, m, s);
        return LocalTime.of(h, m, s);
    }

    public boolean tBooleanToBoolean(String text) {
        return text.equals("TTRUE");
    }

    public <O extends ObjectSelector> LAWithParams addScriptedShowFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps,
                                                                         Boolean syncType, WindowFormType windowType, ManageSessionType manageSession, FormSessionScope formSessionScope,
                                                                         boolean checkOnOk, Boolean noCancel, boolean readonly,
                                                                         List<TypedParameter> objectsContext, List<LPWithParams> contextFilters, List<TypedParameter> oldContext,
                                                                         String formId) throws ScriptingErrorLog.SemanticErrorException {
        ImList<O> mappedObjects = mapped.objects;
        ImOrderSet<O> contextObjects = getMappingObjectsArray(mapped, objectsContext);

        MList<O> mObjects = ListFact.mListMax(mappedObjects.size());
        List<LPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mListMax(mappedObjects.size());

        for (int i = 0; i < mappedObjects.size(); i++) {
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mObjects.add(mappedObjects.get(i));
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        CFEWithParams<O> contextEntities = getContextFilterEntities(oldContext.size(), contextObjects, ListFact.fromJavaList(contextFilters));

        ImList<O> objects = mObjects.immutableList();
        LA action = addIFAProp(null, LocalizedString.NONAME, mapped.form, objects, mNulls.immutableList(),
                formSessionScope, manageSession, noCancel,
                contextEntities.orderInterfaces, contextEntities.filters,
                syncType, windowType, false, checkOnOk,
                readonly, formId);

        for (int usedParam : contextEntities.usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(action, mapping);
        } else {
            return new LAWithParams(action, Collections.emptyList());
        }
    }

    private LP<?> getInputProp(NamedPropertyUsage targetProp, ValueClass valueClass, Set<Property> usedProps) throws ScriptingErrorLog.SemanticErrorException {
        if(targetProp != null) {
            LP<?> result = findLPNoParamsByPropertyUsage(targetProp);
            if(usedProps != null)
                usedProps.add(result.property);
            return result;
        }

        // having the same property for different objects, we make implicit links between DIALOG / INPUT operators in the ACTIONS and INPUT operator itself (so DIALOG in ACTIONS will automatically transfer result to INPUT)
        LP requested = getRequestedValueProperty(valueClass);
        if(usedProps == null || usedProps.add(requested.property))
            return requested;

        return new LP<>(PropertyFact.createInputDataProp(valueClass));
    }

    public <O extends ObjectSelector> List<TypedParameter> getTypedObjectsNames(MappedForm<O> mapped) {
        FormEntity staticForm = mapped.form.getNFStaticForm();
        if(staticForm == null) // can be only mapped objects
            return Collections.singletonList(new TypedParameter(mapped.form.getBaseClass(mapped.objects.single()),"object"));

        return ScriptingFormEntity.getTypedObjectsNames(this, staticForm, getVersion());
    }

    private <O extends ObjectSelector> ImOrderSet<O> getMappingObjectsArray(MappedForm<O> mapped, List<TypedParameter> objectsContext) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity staticForm = mapped.form.getNFStaticForm();
        if(staticForm == null) // can be only mapped objects
            return mapped.objects;
        
        return BaseUtils.immutableCast(getMappingObjectsArray(staticForm, SetFact.fromJavaOrderSet(objectsContext).mapOrderSetValues(value -> value.paramName)));
    }
    
    // Constraint Change Context Filter
    private static class CCCF<O extends ObjectSelector> {
        public final LPWithParams change;
        public final O object;
        public final int objectParam;

        public CCCF(LPWithParams change, O object, int objectParam) {
            this.change = change;
            this.object = object;
            this.objectParam = objectParam;
        }
    }

    // Context Filter Entity
    private static class CFEWithParams<O extends ObjectSelector> {
        public final ImOrderSet<Integer> usedParams;
        public final ImOrderSet<PropertyInterface> orderInterfaces;
        public final ImSet<ContextFilterSelector<PropertyInterface, O>> filters;
        public final InputPropertyListEntity<?, PropertyInterface> list;
        public final ImList<InputContextAction<?, PropertyInterface>> contextActions;

        public CFEWithParams(ImOrderSet<Integer> usedParams, ImOrderSet<PropertyInterface> orderInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> filters, InputPropertyListEntity<?, PropertyInterface> list, ImList<InputContextAction<?, PropertyInterface>> contextActions) {
            this.usedParams = usedParams;
            this.orderInterfaces = orderInterfaces;
            assert usedParams.size() == orderInterfaces.size();
            this.filters = filters;
            this.list = list;
            this.contextActions = contextActions;
        }
    }
    private LPWithParams remap(LPWithParams property, int fromParam, int toParam) {
        List<Integer> remappedList = new ArrayList<Integer>();
        for(int usedParam : property.usedParams) {
            if (usedParam == fromParam)
                usedParam = toParam;
            remappedList.add(usedParam);
        }
        return new LPWithParams(property.getLP(), remappedList);
    }
    private interface ThreeFunction<A, B, C, R> {
        R apply(A param1, B param2, C param3);        
    }
    private <T extends PropertyInterface, E, R> R splitParams(LPWithParams lpWithParams, int contextSize, ImRevMap<Integer, PropertyInterface> usedInterfaces, IntFunction<E> external, ThreeFunction<Property<T>, ImRevMap<T, PropertyInterface>, ImRevMap<T, E>, R> result) {
        return splitAPParams(lpWithParams, contextSize, usedInterfaces, external, result);
    }
    private <T extends PropertyInterface, P extends ActionOrProperty<T>, E, R> R splitAPParams(LAPWithParams lpWithParams, int contextSize, ImRevMap<Integer, PropertyInterface> usedInterfaces, IntFunction<E> external, ThreeFunction<P, ImRevMap<T, PropertyInterface>, ImRevMap<T, E>, R> result) {
        LAP<T, P> lp = (LAP<T, P>) lpWithParams.getLP();

        int size = lp.listInterfaces.size();
        MRevMap<T, PropertyInterface> mMapValues = MapFact.mRevMapMax(size);
        MRevMap<T, E> mMapObjects = MapFact.mRevMapMax(size);
        for(int i = 0; i<size; i++) {
            T pi = lp.listInterfaces.get(i);
            Integer usedParam = lpWithParams.usedParams.get(i);
            PropertyInterface ii = usedInterfaces.get(usedParam);
            if(ii != null) // either context parameter
                mMapValues.revAdd(pi, ii);
            else // either object parameter
                mMapObjects.revAdd(pi, external.apply(usedParam - contextSize));
        }
        return result.apply(lp.getActionOrProperty(), mMapValues.immutableRev(), mMapObjects.immutableRev());
    }
    
    private <O extends ObjectSelector, T extends PropertyInterface, X extends PropertyInterface> CFEWithParams<O> getContextFilterEntities(int contextSize, ImOrderSet<O> objectsContext, ImList<LPWithParams> contextFilters) {
        return getContextFilterAndListEntities(contextSize, objectsContext, contextFilters, null, ListFact.EMPTY());
    }
    private <O extends ObjectSelector, T extends PropertyInterface, X extends PropertyInterface> CFEWithParams<O> getContextFilterAndListEntities(int contextSize, ImOrderSet<O> objectsContext, ImList<LPWithParams> contextFilters, LPWithParams list, ImList<CCCF<O>> cccfs) {
        return getContextFilterAndListAndActionsEntities(contextSize, objectsContext, contextFilters, list, cccfs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
    private <O extends ObjectSelector, T extends PropertyInterface, X extends PropertyInterface> CFEWithParams<O> getContextFilterAndListAndActionsEntities(int contextSize, ImOrderSet<O> objectsContext, ImList<LPWithParams> contextFilters, LPWithParams list, ImList<CCCF<O>> cccfs,
                                                                                                                                                  List<String> actionImages, List<String> keyStrokes, List<List<QuickAccess>> quickAccesses, List<LAWithParams> actions) {
        List<LAPWithParams> props = new ArrayList<>();
        ListFact.addJavaAll(contextFilters, props);
        ListFact.addJavaAll(cccfs.mapListValues((CCCF<O> cccf) -> cccf.change), props);
        ListFact.addJavaAll(cccfs.mapListValues((CCCF<O> cccf) -> new LPWithParams(null, cccf.objectParam)), props);
        if(list != null)
            props.add(list);
        props.addAll(actions);

        // actually action input param has different type (list type in theory), but it doesn't matter actually
        ImOrderSet<Integer> usedContextParams = SetFact.fromJavaOrderSet(getResultInterfaces(contextSize, mergeAllParams(props)));
        ImOrderSet<PropertyInterface> orderInterfaces = genInterfaces(usedContextParams.size());
        ImRevMap<Integer, PropertyInterface> usedInterfaces = usedContextParams.mapSet(orderInterfaces);

        return new CFEWithParams<O>(usedContextParams, orderInterfaces, ListFact.add(
            getContextFilterEntities(contextSize, objectsContext, contextFilters, usedInterfaces),
            cccfs.mapListValues((CCCF<O> cccf) -> {
                LP<X> lp = (LP<X>) cccf.change.getLP();
                return new CCCContextFilterEntity<>(lp.getImplement(SetFact.fromJavaOrderSet(cccf.change.usedParams).mapOrder(usedInterfaces)), cccf.object);
            })).toOrderExclSet().getSet(),
            list != null ? (InputPropertyListEntity<?, PropertyInterface>) getInputListEntity(contextSize, list, usedInterfaces) : null,
            getInputContextActions(contextSize, actionImages, keyStrokes, quickAccesses, actions, usedInterfaces));
    }

    public <O extends ObjectSelector> LAWithParams addScriptedDialogFAProp(
            MappedForm<O> mapped, List<FormActionProps> allObjectProps,
            WindowFormType windowType, ManageSessionType manageSession, FormSessionScope scope,
            boolean checkOnOk, Boolean noCancel, boolean readonly, LAWithParams doAction, LAWithParams elseAction,
            List<TypedParameter> objectsContext, List<LPWithParams> contextFilters,
            List<TypedParameter> oldContext, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {

        ImList<O> mappedObjects = mapped.objects;
        ImOrderSet<O> contextObjects = getMappingObjectsArray(mapped, objectsContext);

        MList<O> mInputObjects = ListFact.mListMax(mappedObjects.size());
        MList<Boolean> mInputNulls = ListFact.mListMax(mappedObjects.size());
        MList<LP> mInputProps = ListFact.mListMax(mappedObjects.size());

        MList<O> mObjects = ListFact.mListMax(mappedObjects.size());
        List<LPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mListMax(mappedObjects.size());

        LPWithParams list = null;
        
        MList<Pair<LPWithParams, DebugInfo.DebugPoint>> mChangeProps = ListFact.mListMax(mappedObjects.size());
        MList<CCCF<O>> mConstraintContextFilters = ListFact.mList();

        Set<Property> usedProps = new HashSet<>();

        for (int i = 0; i < mappedObjects.size(); i++) {
            O object = mappedObjects.get(i);
            FormActionProps objectProp = allObjectProps.get(i);
            if (objectProp.in != null) {
                mObjects.add(object);
                mapping.add(objectProp.in);
                mNulls.add(objectProp.inNull);
            }
            if (objectProp.out) {
                mInputObjects.add(object);
                mInputNulls.add(objectProp.outNull);
                LP<?> outProp = getInputProp(objectProp.outProp, mapped.form.getBaseClass(object), usedProps);
                mInputProps.add(outProp);
                
                if(objectProp.listProp != null) {
                    if(list != null)
                        errLog.emitSimpleError(parser, "LIST option can be specified only once");
                    list = remap(objectProp.listProp, objectProp.outParamNum, oldContext.size() + contextObjects.indexOf(object));
                }                    

                LPWithParams changeProp = objectProp.changeProp;
                if(changeProp == null)
                    changeProp = objectProp.in;

                if(objectProp.constraintFilter)
                    mConstraintContextFilters.add(new CCCF<O>(changeProp, object, oldContext.size() + contextObjects.indexOf(object)));

                Pair<LPWithParams, DebugInfo.DebugPoint> assignProp = null;
                if(objectProp.assign) {
                    assignProp = new Pair<>(changeProp, objectProp.assignDebugPoint);
                    if(doAction == null) // we will need to add change props, temporary will make this action empty to avoid extra null checks 
                        doAction = new LAWithParams(baseLM.getEmpty(), new ArrayList<Integer>());
                }
                mChangeProps.add(assignProp);
            }
        }
        ImList<O> objects = mObjects.immutableList();
        ImList<O> inputObjects = mInputObjects.immutableList();
        ImList<Boolean> inputNulls = mInputNulls.immutableList();
        ImList<LP> inputProps = mInputProps.immutableList();

        ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> changeProps = mChangeProps.immutableList();

        CFEWithParams<O> contextEntities = getContextFilterAndListEntities(oldContext.size(), contextObjects, ListFact.fromJavaList(contextFilters), list, mConstraintContextFilters.immutableList());

        boolean syncType = doAction != null || elseAction != null; // optimization

        LA action = addDialogInputAProp(mapped.form, objects, mNulls.immutableList(),
                                 inputObjects, inputProps, inputNulls, scope, contextEntities.list,
                                 manageSession, noCancel,
                                 contextEntities.orderInterfaces, contextEntities.filters,
                                 contextEntities.contextActions, syncType, windowType, checkOnOk,
                                 readonly, null, false);

        for (int usedParam : contextEntities.usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        LAWithParams formAction;
        if (mapping.size() > 0) { // optimization
            formAction = addScriptedJoinAProp(action, mapping);
        } else {
            formAction = new LAWithParams(action, Collections.emptyList());
        }

        return proceedInputDoClause(doAction, elseAction, oldContext, newContext, inputProps, formAction, changeProps);
    }

    private LAWithParams proceedInputDoClause(LAWithParams doAction, LAWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, ImList<LP> inputParamProps, LAWithParams proceedAction, ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> changeProps) throws ScriptingErrorLog.SemanticErrorException {
        if (doAction != null)
            doAction = extendDoParams(doAction, newContext, oldContext.size(), false, inputParamProps, null, changeProps);
        return addScriptedRequestAProp(proceedAction, doAction, elseAction);
    }

    private LAWithParams proceedImportDoClause(boolean noParams, LAWithParams doAction, LAWithParams elseAction, List<TypedParameter> oldContext, List<TypedParameter> newContext, LP<?> whereLCP, ImList<LP> importParamProps, ImList<Boolean> nulls, LAWithParams proceedAction) throws ScriptingErrorLog.SemanticErrorException {
        LAWithParams fillNullsAction = null;
        int paramOld = oldContext.size() + (!noParams ? 1 : 0);
        if(nulls != null) {
            if(paramOld == newContext.size()) // хак, потом можно будет красивее сделать
                importParamProps = SetFact.EMPTYORDER();
            fillNullsAction = fillImportNullsAction(noParams, paramOld, oldContext, newContext, whereLCP, importParamProps, nulls);
        }
            
        if (doAction != null || fillNullsAction != null) {
            List<LAWithParams> actions = new ArrayList<>();
            actions.add(proceedAction);

            if(fillNullsAction != null)
                actions.add(fillNullsAction);

            if(doAction != null)
                actions.add(extendImportDoAction(noParams, paramOld, oldContext, newContext, doAction, elseAction, whereLCP, importParamProps, nulls));

            LAWithParams listAction = addScriptedListAProp(actions, Collections.emptyList());
            // хак - в ifAProp оборачиваем что delegationType был AFTER_DELEGATE, а не BEFORE или null, вообще по хорошему надо delegationType в момент parsing'а проставлять, а не в самих свойствах
            return addScriptedIfAProp(new LPWithParams(baseLM.vtrue), listAction, null);
        }
        return proceedAction;
    }

    private LAWithParams extendImportDoAction(boolean noParams, int paramOld, List<TypedParameter> oldContext, List<TypedParameter> newContext, LAWithParams doAction, LAWithParams elseAction, LP<?> whereLCP, ImList<LP> importParamProps, ImList<Boolean> nulls) throws ScriptingErrorLog.SemanticErrorException {
        ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> changeProps = ListFact.toList(importParamProps.size(), i -> null);
        doAction = extendDoParams(doAction, newContext, paramOld, !noParams, importParamProps, nulls, changeProps); // row parameter consider to be external (it will be proceeded separately)
        if(!noParams) { // adding row parameter
            modifyContextFlowActionDefinitionBodyCreated(doAction, BaseUtils.add(oldContext, newContext.get(oldContext.size())), oldContext);

            doAction = addScriptedForAProp(oldContext, new LPWithParams(whereLCP, oldContext.size()), Collections.singletonList(new LPWithParams(oldContext.size())), doAction,
                    elseAction, null, null, false, false, false, Collections.emptyList(), false);
        }
        return doAction;
    }

    // filling null values if necessary
    private LAWithParams fillImportNullsAction(boolean noParams, int paramOld, List<TypedParameter> oldContext, List<TypedParameter> newContext, LP<?> whereLCP, ImList<LP> importParamProps, ImList<Boolean> nulls) throws ScriptingErrorLog.SemanticErrorException {
        List<Integer> params = !noParams ? Collections.singletonList(oldContext.size()) : Collections.emptyList();
        List<TypedParameter> oldAndRowContext = newContext.subList(0, paramOld);
        List<LAWithParams> fillNulls = null;
        for(int i=paramOld;i<newContext.size();i++) {
            int importIndex = i - paramOld;
            if (!nulls.get(importIndex)) { // no null
                if (fillNulls == null)
                    fillNulls = new ArrayList<>();

                LPWithParams importProp = new LPWithParams(importParamProps.get(importIndex), params);
                DataClass cls = (DataClass) newContext.get(i).cls;
                LPWithParams defaultValueProp = new LPWithParams(addCProp(cls, StaticValueExpr.getStaticValue(cls.getDefaultValue(), cls)));
                // prop(row) <- defvalue WHERE NOT prop(row)
                fillNulls.add(addScriptedChangeAProp(oldAndRowContext, defaultValueProp, addScriptedNotProp(importProp), importProp, oldAndRowContext, 1, null));
            }
        }
        if(fillNulls != null) {
            LAWithParams fillNullsAction = addScriptedListAProp(fillNulls, Collections.emptyList());
            if(!noParams) // FOR where(row)
                fillNullsAction = addScriptedForAProp(oldContext, new LPWithParams(noParams ? baseLM.vtrue : whereLCP, params), Collections.emptyList(), fillNullsAction,
                    null, null, null, false, false, false, Collections.emptyList(), false);
            return fillNullsAction;
        }
        return null;
    }

//    private int findOldParam(List<TypedParameter> params, ImList<Integer> inputParams, Result<ImList<LP>> rInputParamProps) throws ScriptingErrorLog.SemanticErrorException {
//        ImOrderSet<Integer> paramsSet = inputParams.toOrderExclSet();
//        MList<LP> mInputParamProps = ListFact.mList(inputParams.size());
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

    private LAWithParams nullExec(LAWithParams doAction, int param) throws ScriptingErrorLog.SemanticErrorException {
        List<LPWithParams> params = new ArrayList<>();
        boolean found = false;
        for(int usedParam : doAction.usedParams)
            if(usedParam == param){
                found = true;
                params.add(new LPWithParams(baseLM.vnull));
            } else
                params.add(new LPWithParams(usedParam));

        if(!found) // не было использований
            return null;
        return addScriptedJoinAProp(doAction.getLP(), params);
    }

    // recursive
    private LAWithParams extendDoParams(LAWithParams doAction, List<TypedParameter> context, int paramOld, boolean isLastParamRow, ImList<LP> resultProps, ImList<Boolean> nulls, ImList<Pair<LPWithParams, DebugInfo.DebugPoint>> changeProps) throws ScriptingErrorLog.SemanticErrorException {
        assert context.size() - paramOld == resultProps.size();
        assert resultProps.size() == changeProps.size();

        List<TypedParameter> currentContext = new ArrayList<>(context);
        int paramNum;
        while((paramNum = currentContext.size() - 1) >= paramOld) {
            // remove'им параметр
            List<TypedParameter> removedContext = BaseUtils.remove(currentContext, paramNum);

            LPWithParams paramLP = new LPWithParams(paramNum);
            Pair<LPWithParams, DebugInfo.DebugPoint> assignLP = changeProps.get(paramNum - paramOld);
            if(assignLP != null) {
                LAWithParams assignAction = addScriptedChangeAProp(currentContext, paramLP, null, assignLP.first, currentContext, 0, null);

                ScriptingLogicsModule.setDebugInfo(null, assignLP.second, assignAction.getLP().action);

                doAction = addScriptedListAProp(Arrays.asList(assignAction, doAction), new ArrayList<>());
            }

            boolean paramNoNull = nulls != null && !nulls.get(paramNum - paramOld);
            LAWithParams nullExec = paramNoNull ? null : nullExec(doAction, paramNum); // передает NULL в качестве параметра
            if(paramNoNull || nullExec != null) { // нет параметра нет проблемы
                modifyContextFlowActionDefinitionBodyCreated(doAction, currentContext, removedContext);

                LP resultProp = resultProps.get(paramNum - paramOld);
                LPWithParams resultLP = isLastParamRow ? new LPWithParams(resultProp, paramOld - 1) : new LPWithParams(resultProp);

                doAction = addScriptedForAProp(removedContext, addScriptedEqualityProp("==", paramLP, resultLP, currentContext), new ArrayList<>(), doAction,
                        nullExec, null, null, false, false, false, isLastParamRow ? Collections.emptyList() : null, false);
            }

            currentContext = removedContext;
        }

        return doAction;
    }

    public <O extends ObjectSelector> LPWithParams addScriptedJSONFormProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps, List<TypedParameter> objectsContext,
                                                                           List<LPWithParams> contextFilters, List<TypedParameter> params,
                                                                           FormSelectTop<LPWithParams> selectTop, boolean returnString) throws ScriptingErrorLog.SemanticErrorException {

        if(selectTop == null)
            selectTop = FormSelectTop.NULL();

        ImList<O> mappedObjects = mapped.objects;
        ImOrderSet<O> contextObjects = getMappingObjectsArray(mapped, objectsContext);

        List<LPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mListMax(mappedObjects.size());

        for (int i = 0; i < mappedObjects.size(); i++) {
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        CFEWithParams<O> contextEntities = getContextFilterEntities(params.size(), contextObjects, ListFact.fromJavaList(contextFilters));

        LP property = addJSONFormProp(null, LocalizedString.NONAME, mapped.form, mappedObjects, mNulls.immutableList(),
                contextEntities.orderInterfaces, contextEntities.filters, selectTop.mapValues(this, params), returnString);

        for (int usedParam : contextEntities.usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        mapping.addAll(selectTop.getParams());

        if (mapping.size() > 0) {
            return addScriptedJProp(property, mapping);
        } else {
            return new LPWithParams(property, Collections.emptyList());
        }
    }

    public <O extends ObjectSelector> LAWithParams addScriptedExportFAProp(MappedForm<O> mapped, List<FormActionProps> allObjectProps, FormIntegrationType exportType,
                                                                           LPWithParams sheetNameProperty, LPWithParams rootProperty, LPWithParams tagProperty, boolean attr, Boolean hasHeader,
                                                                           String separator, boolean noEscape, FormSelectTop<LPWithParams> selectTop, String charset, NamedPropertyUsage propUsage,
                                                                           OrderedMap<GroupObjectEntity, NamedPropertyUsage> propUsages, List<TypedParameter> objectsContext,
                                                                           List<LPWithParams> contextFilters, List<TypedParameter> params) throws ScriptingErrorLog.SemanticErrorException {

        if(selectTop == null)
            selectTop = FormSelectTop.NULL();

        ImList<O> mappedObjects = mapped.objects;
        ImOrderSet<O> contextObjects = getMappingObjectsArray(mapped, objectsContext);

        List<LPWithParams> mapping = new ArrayList<>();
        MList<Boolean> mNulls = ListFact.mListMax(mappedObjects.size());

        for (int i = 0; i < mappedObjects.size(); i++) {
            FormActionProps objectProp = allObjectProps.get(i);
            assert objectProp.in != null;
            mapping.add(objectProp.in);
            mNulls.add(objectProp.inNull);
            assert !objectProp.out && !objectProp.constraintFilter;
        }

        if (rootProperty != null) {
            errLog.emitSimpleError(parser, "EXPORT form with ROOT not supported");
        }
        if (tagProperty != null) {
            errLog.emitSimpleError(parser, "EXPORT form with TAG not supported");
        }
        if (attr) {
            errLog.emitSimpleError(parser, "EXPORT form with ATTR not supported");
        }

        LP<?> singleExportFile = null;
        MExclMap<GroupObjectEntity, LP> exportFiles = MapFact.mExclMap();
        if (propUsages != null) {
            for (Map.Entry<GroupObjectEntity, NamedPropertyUsage> entry : propUsages.entrySet()) {
                LP<?> propertyUsage = findLPNoParamsByPropertyUsage(entry.getValue());
                exportFiles.exclAdd(entry.getKey(), propertyUsage);
                checks.checkExportFromFileExpression(propertyUsage);
            }
        } else {
            singleExportFile = propUsage != null ? findLPNoParamsByPropertyUsage(propUsage) : baseLM.exportFile;
            checks.checkExportFromFileExpression(singleExportFile);
        }

        exportType = adjustExportFormatFromFileType(exportType, singleExportFile, exportFiles.immutable().values().toJavaCol());

        if (exportType.isPlain()) {
            if (exportFiles.isEmpty()) {
                if (singleExportFile != null) {
                    errLog.emitSimpleError(parser, String.format("EXPORT %s TO single file not supported", exportType));
                } else {
                    errLog.emitSimpleError(parser, "Output file(s) for export not specified");
                }
            }
        } else if (!exportFiles.isEmpty()) {
            errLog.emitSimpleError(parser, String.format("EXPORT %s TO multiple files not supported", exportType));
        }

        ValueClass sheetName = sheetNameProperty != null ? getValueClassByParamProperty(sheetNameProperty, params) : null;
        ValueClass root = rootProperty != null ? getValueClassByParamProperty(rootProperty, params) : null;
        ValueClass tag = tagProperty != null ? getValueClassByParamProperty(tagProperty, params) : null;

        CFEWithParams<O> contextEntities = getContextFilterEntities(params.size(), contextObjects, ListFact.fromJavaList(contextFilters));

        LA action = addEFAProp(null, LocalizedString.NONAME, mapped.form, mappedObjects, mNulls.immutableList(),
                contextEntities.orderInterfaces, contextEntities.filters, exportType, hasHeader, separator, noEscape, selectTop.mapValues(this, params), charset, singleExportFile, exportFiles.immutable(), sheetName, root, tag);

        for (int usedParam : contextEntities.usedParams) {
            mapping.add(new LPWithParams(usedParam));
        }

        mapping.addAll(selectTop.getParams());

        if(sheetNameProperty != null)
            mapping.add(sheetNameProperty);

        if(rootProperty != null)
            mapping.add(rootProperty);

        if(tagProperty != null)
            mapping.add(tagProperty);

        if (mapping.size() > 0) {
            return addScriptedJoinAProp(action, mapping);
        } else {
            return new LAWithParams(action, Collections.emptyList());
        }
    }

    public LPWithParams addScriptedJSONProperty(List<TypedParameter> oldContext, final List<String> ids, List<Boolean> literals,
                                                List<LPWithParams> exprs, List<LPTrivialLA> propUsages,
                                                LPWithParams whereProperty,
                                                List<LPWithParams> orderProperties, List<Boolean> orderDescendings, SelectTop<LPWithParams> selectTop, boolean returnString,
                                                List<TypedParameter> newContext)
            throws ScriptingErrorLog.SemanticErrorException {

        if(selectTop == null)
            selectTop = SelectTop.NULL();

        List<LPWithParams> exExprs = new ArrayList<>(exprs);
        MList<IntegrationPropUsage> mExPropUsages = ListFact.mList();
        for (int i = 0, size = exprs.size(); i < size; i++) {
            Pair<ActionOrProperty, List<String>> exPropUsage = null;
            LPTrivialLA propUsage = propUsages.get(i);
            if (propUsage != null) { // we need property to get the correct integrationSID
                FormActionOrPropertyUsage<?> pDrawUsage = propUsage.action;
                LAP usageProperty = findLPByPropertyUsage(pDrawUsage.usage.property, propUsage.mapParams);
                exPropUsage = new Pair<>(usageProperty.getActionOrProperty(), pDrawUsage.mapping);
            }
            mExPropUsages.add(new IntegrationPropUsage(ids.get(i), literals.get(i), exprs.get(i), exPropUsage));
        }

        MOrderExclMap<String, Boolean> mOrders = MapFact.mOrderExclMap(orderProperties.size());
        for (int i = 0, size = orderProperties.size(); i < size; i++) {
            LPWithParams orderProperty = orderProperties.get(i);
            exExprs.add(orderProperty);
            String orderId = "order" + mExPropUsages.size();
            mExPropUsages.add(new IntegrationPropUsage(orderId, false, orderProperty, null));
            mOrders.exclAdd(orderId, orderDescendings.get(i));
        }
        ImOrderMap<String, Boolean> orders = mOrders.immutableOrder();
        ImList<IntegrationPropUsage> exPropUsages = mExPropUsages.immutableList();

        // technically it's a mixed operator with exec technics (root, tag like SHOW / DIALOG) and operator technics (exprs, where like CHANGE)
        List<LPWithParams> props = exExprs;
        if(whereProperty != null)
            props = BaseUtils.add(exExprs, whereProperty);
        /*for(LPWithParams windowProp : selectTop.getParams()) {
            props = BaseUtils.add(props, windowProp);
        }*/
        List<Integer> resultInterfaces = getResultInterfaces(oldContext.size(), props.toArray(new LAPWithParams[props.size()]));

        List<LAPWithParams> paramsList = new ArrayList<>();
        for (int resI : resultInterfaces)
            paramsList.add(new LPWithParams(resI));
        paramsList.addAll(exExprs);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        //paramsList.addAll(selectTop.getParams());
//        ImList<Type> exprTypes = getTypesForExportProp(exprs, newContext);
        List<Object> resultParams = getParamsPlainList(paramsList);

        // we need explicit inner classes, because json property is lazy, and for it integration form classes are important (will be null otherwise)
        ImList<ValueClass> explicitInnerClasses = getValueClassesFromTypedParams(oldContext.size(), resultInterfaces, newContext);

        LP result = null;
        try {
            result = addJSONProp(LocalizedString.NONAME, resultInterfaces.size(), explicitInnerClasses, exPropUsages, orders,
                    whereProperty != null, selectTop.mapValues(this, newContext), returnString, resultParams.toArray());
        } catch (FormEntity.AlreadyDefined alreadyDefined) {
            throwAlreadyDefinePropertyDraw(alreadyDefined);
        }

        if(!selectTop.isEmpty()) { // optimization
            List<LPWithParams> mapping = new ArrayList<>();
            for (int resI : resultInterfaces)
                mapping.add(new LPWithParams(resI));

            mapping.addAll(selectTop.getParams());

            LPWithParams lgp = addScriptedJProp(result, mapping);
            result = lgp.getLP();
            resultInterfaces = lgp.usedParams;
        }

        return new LPWithParams(result, resultInterfaces);
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

    public void addScriptedMetaCodeFragment(String name, List<String> params, List<Pair<String, Boolean>> tokens, int lineNumber) throws RecognitionException {
        checks.checkDuplicateMetaCodeFragment(name, params.size());
        checks.checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(elementCanonicalName(name), params, tokens, getName(), lineNumber);
        addMetaCodeFragment(fragment);
    }

    public void runMetaCode(String name, List<String> params, int lineNumberBefore, int lineNumberAfter, boolean enabledMeta) throws RecognitionException {
        MetaCodeFragment metaCode = findMetaCodeFragment(name, params.size());
        checks.checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params, BL.getIdFromReversedI18NDictionaryMethod(), BL::appendEntryToBundle);
        parser.runMetaCode(this, code, metaCode.getLineNumber(), metaCode.getModuleName(), 
                MetaCodeFragment.metaCodeCallString(name, metaCode, params), lineNumberBefore, lineNumberAfter, enabledMeta);
    }

    private Pair<LPWithParams, LPNotExpr> addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos + 1);
        LP resultProp = null;

        boolean isCompoundID = className.indexOf('.') < 0;
        ScriptingErrorLog.SemanticErrorException semanticError = null;
        try {
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
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            if(isCompoundID)
                semanticError = e;
            else
                throw e;
        }
        return new Pair<>(new LPWithParams(resultProp), isCompoundID ? new LPCompoundID(name, semanticError) : null);
    }

    public Pair<LPWithParams, LPNotExpr> addSingleParameter(TypedParameter param, List<TypedParameter> context, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        String name = param.paramName;
        boolean isCompoundID = param.cls == null && !isRecursiveParam(name);
        ScriptingErrorLog.SemanticErrorException semanticError = null;
        int paramIndex;
        try {
            paramIndex = getParamIndex(param, context, dynamic, insideRecursion);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            if(isCompoundID) {
                paramIndex = -1;
                semanticError = e;
            } else
                throw e;
        }
        return new Pair<>(new LPWithParams(paramIndex), isCompoundID ? new LPCompoundID(name, semanticError) : null);
    }

    public void throwAlreadyDefinePropertyDraw(FormEntity.AlreadyDefined alreadyDefined) throws ScriptingErrorLog.SemanticErrorException {
        getErrLog().emitAlreadyDefinedPropertyDrawError(getParser(), alreadyDefined.formCanonicalName, alreadyDefined.newSID, alreadyDefined.formPath);
    }

    public LP addScriptedGroupObjectProp(String name, GroupObjectProp prop, List<ResolveClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);
        LP resultProp = null;

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

    public LPWithParams addScriptedValueObjectProp(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.lastIndexOf('.');
        assert pointPos > 0;

        String formName = name.substring(0, pointPos);
        String objectName = name.substring(pointPos+1);
        LPWithParams resultProp = null;

        ObjectEntity object = findForm(formName).getNFObject(objectName, getVersion());
        if (object != null) {
            resultProp = new LPWithParams(addValueObjectProp(object));
        } else  {
            errLog.emitNotFoundError(parser, "оbject", objectName);
        }
        return resultProp;
    }


    public LP addScriptedReflectionProperty(ReflectionPropertyType type, ActionOrPropertyUsage propertyUsage, List<ResolveClassSet> outClasses) throws ScriptingErrorLog.SemanticErrorException {
        switch (type) {
            case CANONICAL_NAME:
            default: return addCanonicalNameProp(propertyUsage);
        }
    }

    public LP addCanonicalNameProp(ActionOrPropertyUsage propertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        return new LP<>(new CanonicalNameProperty(findLAPByActionOrPropertyUsage(propertyUsage)));
    }

    public LAWithParams addScriptedFocusAction(PropertyDrawEntity property) {
        return new LAWithParams(addFocusAction(property), new ArrayList<>());
    }

    public LAWithParams addScriptedReadAction(LPWithParams sourcePathProp, NamedPropertyUsage propUsage, List<TypedParameter> params, boolean clientAction, boolean dialog) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass sourceProp = getValueClassByParamProperty(sourcePathProp, params);
        LP<?> targetProp = propUsage == null ? baseLM.readFile : findLPNoParamsByPropertyUsage(propUsage);
        return addScriptedJoinAProp(addAProp(new ReadAction(targetProp, clientAction, dialog)),
                Collections.singletonList(sourcePathProp));
    }

    public LAWithParams addScriptedWriteAction(LPWithParams sourceProp, LPWithParams pathProp, List<TypedParameter> params, boolean clientAction, boolean dialog, boolean append) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJoinAProp(addAProp(new WriteAction(getTypeByParamProperty(sourceProp, params),
                clientAction, dialog, append)),
                Arrays.asList(sourceProp, pathProp));
    }

    public ImList<Type> getTypesForExportProp(List<LPWithParams> paramProps, List<TypedParameter> params) {
        return getTypesByParamProperties(paramProps, params);
    }

    public static class IntegrationPropUsage<P extends PropertyInterface> {
        public final String alias;

        public final boolean literal;

        public final ImOrderSet<P> listInterfaces;

        public final Pair<ActionOrProperty, List<String>> inherited;

        public final Group group;

        public IntegrationPropUsage(String alias, Boolean literal, LPWithParams lp, Pair<ActionOrProperty, List<String>> inherited) {
            this(alias, literal, lp.getLP(), inherited);
        }
        public IntegrationPropUsage(String alias, Boolean literal, LP lp, Pair<ActionOrProperty, List<String>> inherited) {
            this(alias, literal, lp, inherited, Group.NOGROUP);
        }
        public IntegrationPropUsage(String alias, Boolean literal, LP lp, Pair<ActionOrProperty, List<String>> inherited, Group group) {
            this(alias, literal, lp != null ? (ImOrderSet<P>) lp.listInterfaces : null, inherited, group);
        }
        public IntegrationPropUsage(String alias, Boolean literal, ImOrderSet<P> listInterfaces, Pair<ActionOrProperty, List<String>> inherited, Group group) {
            this.alias = alias;
            this.literal = literal != null && literal;
            this.listInterfaces = listInterfaces;
            this.inherited = inherited;
            this.group = group;
        }
    }

    public LAWithParams addScriptedExportAction(List<TypedParameter> oldContext, FormIntegrationType type, final List<String> ids, List<Boolean> literals,
                                                List<LPWithParams> exprs, List<LPTrivialLA> propUsages, LPWithParams whereProperty, NamedPropertyUsage fileProp,
                                                LPWithParams sheetNameProperty, LPWithParams rootProperty, LPWithParams tagProperty,
                                                String separator, Boolean hasHeader, boolean noEscape, SelectTop<LPWithParams> selectTop, String charset, boolean attr,
                                                List<LPWithParams> orderProperties, List<Boolean> orderDescendings, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {

        if(selectTop == null)
            selectTop = SelectTop.NULL();

        LP<?> targetProp = fileProp != null ? findLPNoParamsByPropertyUsage(fileProp) : null;
        if(targetProp == null)
            targetProp = baseLM.exportFile;

        checks.checkExportFromFileExpression(targetProp);

        List<LPWithParams> exExprs = new ArrayList<>(exprs);

        MList<IntegrationPropUsage> mExPropUsages = ListFact.mList();
        for (int i = 0, size = exprs.size(); i < size; i++) {
            Pair<ActionOrProperty, List<String>> exPropUsage = null;
            LPTrivialLA propUsage = propUsages.get(i);
            if (propUsage != null) { // we need property to get the correct integrationSID
                FormActionOrPropertyUsage<?> pDrawUsage = propUsage.action;
                LAP usageProperty = findLPByPropertyUsage(pDrawUsage.usage.property, propUsage.mapParams);
                exPropUsage = new Pair<>(usageProperty.getActionOrProperty(), pDrawUsage.mapping);
            }
            mExPropUsages.add(new IntegrationPropUsage(ids.get(i), literals.get(i), exprs.get(i), exPropUsage));
        }

        MOrderExclMap<String, Boolean> mOrders = MapFact.mOrderExclMap(orderProperties.size());
        for (int i = 0; i < orderProperties.size(); i++) {
            LPWithParams orderProperty = orderProperties.get(i);
            exExprs.add(orderProperty);
            String orderId = "order" + mExPropUsages.size();
            mExPropUsages.add(new IntegrationPropUsage(orderId, false, orderProperty, null));
            mOrders.exclAdd(orderId, orderDescendings.get(i));
        }
        ImOrderMap<String, Boolean> orders = mOrders.immutableOrder();
        ImList<IntegrationPropUsage> exPropUsages = mExPropUsages.immutableList();

        type = adjustExportFormatFromFileType(type, targetProp, null);
//            type = doesExtendContext(oldContext.size(), new ArrayList<LAPWithParams>(), props) ? FormIntegrationType.JSON : FormIntegrationType.TABLE;

        // technically it's a mixed operator with exec technics (root, tag like SHOW / DIALOG) and operator technics (exprs, where like CHANGE)
        List<LPWithParams> props = exExprs;
        if(whereProperty != null)
            props = BaseUtils.add(props, whereProperty);
        List<Integer> resultInterfaces = getResultInterfaces(oldContext.size(), props.toArray(new LAPWithParams[props.size()]));
        List<LPWithParams> mapping = new ArrayList<>();
        for (int resI : resultInterfaces) {
            mapping.add(new LPWithParams(resI));
        }

        List<LAPWithParams> paramsList = new ArrayList<>();
        paramsList.addAll(mapping);
        paramsList.addAll(exExprs);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
//        ImList<Type> exprTypes = getTypesForExportProp(exprs, newContext);
        List<Object> resultParams = getParamsPlainList(paramsList);

        ValueClass sheetName = sheetNameProperty != null ? getValueClassByParamProperty(sheetNameProperty, oldContext) : null;
        ValueClass root = rootProperty != null ? getValueClassByParamProperty(rootProperty, oldContext) : null;
        ValueClass tag = tagProperty != null ? getValueClassByParamProperty(tagProperty, oldContext) : null;

        ImList<ValueClass> explicitInnerClasses = getValueClassesFromTypedParams(oldContext.size(), resultInterfaces, newContext);

        LA result = null;
        try {
            result = addExportPropertyAProp(LocalizedString.NONAME, type, resultInterfaces.size(), explicitInnerClasses, exPropUsages, orders, targetProp,
                    whereProperty != null, sheetName, root, tag, separator, hasHeader, noEscape, selectTop.mapValues(this, oldContext), charset, attr, resultParams.toArray());
        } catch (FormEntity.AlreadyDefined alreadyDefined) {
            throwAlreadyDefinePropertyDraw(alreadyDefined);
        }

        mapping.addAll(selectTop.getParams());
        if(sheetNameProperty != null)
            mapping.add(sheetNameProperty);
        if(rootProperty != null)
            mapping.add(rootProperty);
        if(tagProperty != null)
            mapping.add(tagProperty);

        if(mapping.size() > resultInterfaces.size()) { // optimization
            return addScriptedJoinAProp(result, mapping);
        } else
            return new LAWithParams(result, resultInterfaces);
    }

    private FormIntegrationType adjustExportFormatFromFileType(FormIntegrationType format, LP fileProp, Collection<LP> fileProps) {
        if (format == null) {
            if (fileProps != null && !fileProps.isEmpty())
                fileProp = fileProps.iterator().next();
            Type type = fileProp.property.getType();
            if (type instanceof StaticFormatFileClass)
                return ((StaticFormatFileClass) type).getIntegrationType();
            else
                return FormIntegrationType.JSON;
        }
        return format;
    }

    public static abstract class LAPWithParams {
        private final LAP<?, ?> property; // nullable
        public final List<Integer> usedParams; // immutable zero-based set always ordered by value

        public LAPWithParams(LAP<?, ?> property, List<Integer> usedParams) {
            this.property = property;
            this.usedParams = Collections.unmodifiableList(new ArrayList<>(usedParams));
            assert property == null || property.listInterfaces.size() == usedParams.size();
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", property, usedParams);
        }

        public LAP<?, ?> getLP() {
            return property;
        }
    }

    // always from LAPWithParams where usedParams is ordered set
    public static ImOrderSet<String> getUsedNames(List<TypedParameter> context, List<Integer> usedParams) {
        MOrderExclSet<String> mResult = SetFact.mOrderExclSet(usedParams.size());
        for (int usedIndex : usedParams) {
            mResult.exclAdd(context.get(usedIndex).paramName);
        }
        return mResult.immutableOrder();
    }

    public static List<TypedParameter> getJParams(List<TypedParameter> context, List<LPWithParams> params) {
        List<TypedParameter> result = new ArrayList<>();
        for (LPWithParams param : params) {
            if(param.getLP() != null)
                return null;
            else
                result.add(context.get(param.usedParams.get(0)));
        }
        return result;
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

    public LAWithParams addScriptedNewThreadAction(LAWithParams action, LPWithParams connectionProp, LPWithParams periodProp, LPWithParams delayProp, NamedPropertyUsage toProp) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> targetProp = toProp != null ? findLPNoParamsByPropertyUsage(toProp) : null;

        List<LAPWithParams> propParams = BaseUtils.toList(action);
        if (periodProp != null) {
            propParams.add(periodProp);
        }
        if (delayProp != null) {
            propParams.add(delayProp);
        }
        if (connectionProp != null) {
            propParams.add(connectionProp);
        }
        LA<?> newAction = addNewThreadAProp(null, LocalizedString.NONAME, connectionProp != null, periodProp != null, delayProp != null, targetProp, getParamsPlainList(propParams).toArray());
        return new LAWithParams(newAction, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedNewExecutorAction(LAWithParams action, LPWithParams threadsProp, Boolean sync) {
        List<LAPWithParams> propParams = Arrays.asList(action, threadsProp);
        LA<?> newAction = addNewExecutorAProp(null, LocalizedString.NONAME, sync, getParamsPlainList(propParams).toArray());
        return new LAWithParams(newAction, mergeAllParams(propParams));
    }

    public LAWithParams addScriptedNewConnectionAction(LAWithParams action) {
        List<LAPWithParams> propParams = singletonList(action);
        LA<?> newAction = addNewConnectionAProp(null, LocalizedString.NONAME, getParamsPlainList(propParams).toArray());
        return new LAWithParams(newAction, mergeAllParams(propParams));
    }

    private ImList<LP> findLPsForImport(List<NamedPropertyUsage> propUsages, ImList<ValueClass> paramClasses) throws ScriptingErrorLog.SemanticErrorException {
        MList<LP> mProps = ListFact.mList(propUsages.size());
        for (NamedPropertyUsage propUsage : propUsages) {
            mProps.add(findLPParamByPropertyUsage(propUsage, paramClasses));
        }
        return mProps.immutableList();
    }

    private ImList<LP> genLPsForImport(List<TypedParameter> oldContext, List<TypedParameter> newContext, ImList<ValueClass> paramClasses) {
        int size=newContext.size() - oldContext.size() - paramClasses.size();

        MList<LP> mResult = ListFact.mList(size);
        for(int i=size-1;i>=0;i--)
            mResult.add(new LP<>(PropertyFact.createImportDataProp(newContext.get(newContext.size() - 1 - i).cls, paramClasses)));
        return mResult.immutableList();
    }

    private ImList<LP> findLPsIntegerParamByPropertyUsage(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        MList<LP> mProps = ListFact.mList(propUsages.size());
        for (NamedPropertyUsage propUsage : propUsages) {
            mProps.add(findLPIntegerParamByPropertyUsage(propUsage));
        }
        return mProps.immutableList();
    }

    private LP findLPIntegerParamByPropertyUsage(NamedPropertyUsage propUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPParamByPropertyUsage(propUsage, ListFact.singleton(IntegerClass.instance));
    }

    private ImList<LP> findLPsStringParamByPropertyUsage(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages == null)
            return ListFact.EMPTY();
        
        MList<LP> mProps = ListFact.mList(propUsages.size());
        for (NamedPropertyUsage propUsage : propUsages) {
            mProps.add(findLPStringParamByPropertyUsage(propUsage));
        }
        return mProps.immutableList();
    }

    private LP findLPStringParamByPropertyUsage(NamedPropertyUsage propUsage) throws ScriptingErrorLog.SemanticErrorException {
        return findLPParamByPropertyUsage(propUsage, ListFact.singleton(StringClass.text));
    }

    private LP findLPParamByPropertyUsage(NamedPropertyUsage propUsage, ImList<ValueClass> valueClasses) throws ScriptingErrorLog.SemanticErrorException {
        if (propUsage.classNames == null) {
            propUsage.classNames = new ArrayList<>();
            for (ValueClass valueClass : valueClasses) {
                propUsage.classNames.add(valueClass.getParsedName());
            }
        }
        LP<?> lcp = findLPByPropertyUsage(propUsage);
        ValueClass[] paramClasses = lcp.getInterfaceClasses(ClassType.signaturePolicy);
        if (paramClasses.length != valueClasses.size()) {
            errLog.emitPropertyWithParamsExpectedError(getParser(), propUsage.name, getSignature(valueClasses));
        } else {
            for (int i = 0; i < paramClasses.length; i++) {
                ValueClass paramClass = paramClasses[i];
                ValueClass valueClass = valueClasses.get(i);
                if (!valueClass.isCompatibleParent(paramClass) && !paramClass.isCompatibleParent(valueClass)) {
                    errLog.emitPropertyWithParamsExpectedError(getParser(), propUsage.name, getSignature(valueClasses));
                }
            }
        }
        return lcp;
    }

    private String getSignature(ImList<ValueClass> valueClasses) {
        return PropertyCanonicalNameUtils.createSignature(getParamClasses(valueClasses));
    }

    private FormIntegrationType adjustImportFormatFromFileType(FormIntegrationType format, LPWithParams fileProp, OrderedMap<GroupObjectEntity, LPWithParams> fileProps, List<TypedParameter> context) {
        if(format == null) {
            if(fileProps != null && !fileProps.isEmpty())
                fileProp = fileProps.values().iterator().next();
            Type type = getTypeByParamProperty(fileProp, context);
            if(type instanceof StaticFormatFileClass)
                return ((StaticFormatFileClass) type).getIntegrationType();
            else if (type instanceof AJSONClass)
                return FormIntegrationType.JSON;
        }
        return format;
    }

    public LAWithParams addScriptedImportAction(FormIntegrationType format, LPWithParams fileProp, List<String> ids, List<Boolean> literals, List<NamedPropertyUsage> propUsages,
                                                List<Boolean> nulls, LAWithParams doAction, LAWithParams elseAction, List<TypedParameter> context, List<TypedParameter> newContext,
                                                NamedPropertyUsage wherePropertyUsage, LPWithParams sheet, boolean sheetAll, String separator, boolean noHeader, boolean noEscape,
                                                String charset, LPWithParams root, List<TypedParameter> fieldParams, List<String> toParamClasses, boolean attr,
                                                LPWithParams whereProp, LPWithParams memoProp) throws ScriptingErrorLog.SemanticErrorException {

        if(fileProp == null)
            fileProp = new LPWithParams(baseLM.importFile);

        if(toParamClasses != null && toParamClasses.size() > 1) {
            errLog.emitSimpleError(parser, "IMPORT TO/FIELDS params with multiple classes not supported");
        }

        format = adjustImportFormatFromFileType(format, fileProp, null, context);

        ImList<LP> props;
        ImList<ValueClass> paramClasses;
        if(fieldParams != null) { // FIELDS
            paramClasses = getValueClassesFromTypedParams(fieldParams);
            props = genLPsForImport(context, newContext, paramClasses);
        } else { // TO
            assert doAction == null;
            paramClasses = findClasses(toParamClasses);
            props = findLPsForImport(propUsages, paramClasses);
        }

        ImList<IntegrationPropUsage> exPropUsages = ListFact.toList(props.size(), i -> new IntegrationPropUsage(ids.get(i), literals.get(i), props.get(i), null));

        boolean noParams = paramClasses.isEmpty();

        LP<?> whereLCP;
        if(fieldParams != null) { // FIELDS
            assert wherePropertyUsage == null;
            whereLCP = !noParams ? new LP<>(PropertyFact.createImportDataProp(LogicalClass.instance, paramClasses)) : null;
        } else { // TO
            if(wherePropertyUsage != null)
                whereLCP = findLPByPropertyUsage(wherePropertyUsage);
            else
                whereLCP = findLPByPropertyUsage(new NamedPropertyUsage("imported", toParamClasses), false, true);
        }

        boolean hasRoot = root != null;
        boolean hasWhere = whereProp != null;

        List<LPWithParams> params = new ArrayList<>();
        params.add(fileProp);
        if(hasRoot)
            params.add(root);
        if(hasWhere)
            params.add(whereProp);
        if(memoProp != null)
            params.add(memoProp);
        if(sheet != null)
            params.add(sheet);

        LA importAction = null;
        try {
            importAction = addImportPropertyAProp(format, params.size(), exPropUsages, paramClasses, whereLCP, separator,
                    noHeader, noEscape, charset, sheetAll, attr, hasRoot, hasWhere, getUParams(props.toArray(new LP[props.size()])));
        } catch (FormEntity.AlreadyDefined alreadyDefined) {
            throwAlreadyDefinePropertyDraw(alreadyDefined);
        }
        return proceedImportDoClause(noParams, doAction, elseAction, context, newContext, whereLCP, props, nulls != null ? ListFact.fromJavaList(nulls) : null, addScriptedJoinAProp(importAction, params));
    }

    public ImList<ValueClass> findClasses(List<String> classNames, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        return classNames == null ? getValueClassesFromTypedParams(context) : findClasses(classNames);
    }

    public ImList<ValueClass> findClasses(List<String> classNames) throws ScriptingErrorLog.SemanticErrorException {
        MList<ValueClass> mResult = ListFact.mList(classNames.size()); // exception
        for(String className : classNames)
            mResult.add(findClass(className));
        return mResult.immutableList();
    }

    public LAWithParams addScriptedImportFormAction(FormIntegrationType format, List<TypedParameter> context, LPWithParams fileProp, OrderedMap<GroupObjectEntity, LPWithParams> fileProps,
                                                    FormEntity formEntity, LPWithParams sheet, boolean sheetAll, boolean noHeader, boolean noEscape, boolean attr, String charset, String separator,
                                                    LPWithParams rootProp, LPWithParams whereProp, LPWithParams memoProp) throws ScriptingErrorLog.SemanticErrorException {
        format = adjustImportFormatFromFileType(format, fileProp, fileProps, context);

        List<LPWithParams> params = new ArrayList<>();
        boolean hasFileProps = fileProps != null && !fileProps.isEmpty();
        boolean isPlain = format != null ? format.isPlain() : hasFileProps;
        if(isPlain) {
            if(hasFileProps) {
                for(LPWithParams fProp : fileProps.values()) {
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
                if (fileProp == null) fileProp = new LPWithParams(baseLM.importFile);
                checks.checkImportFromFileExpression(fileProp);
                params.add(fileProp);
            }
        }

        boolean hasRoot = rootProp != null;
        boolean hasWhere = whereProp != null;

        if(attr)
            errLog.emitSimpleError(parser, "IMPORT form with ATTR not supported");
        if(hasWhere)
            errLog.emitSimpleError(parser, "IMPORT form with WHERE not supported");
        if(memoProp != null)
            errLog.emitSimpleError(parser, "IMPORT form with MEMO not supported");

        if(hasRoot)
            params.add(rootProp);
        if(sheet != null)
            params.add(sheet);

        ImOrderSet<GroupObjectEntity> groupFiles = fileProps != null ? SetFact.fromJavaOrderSet(fileProps.keyList()) : SetFact.EMPTYORDER();
        return addScriptedJoinAProp(addImportFAProp(format, formEntity, params.size(), groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasRoot, hasWhere, !groupFiles.isEmpty()), params);
    }

    public LP addTypeProp(ValueClass valueClass, boolean bIs) {
        if (bIs) {
            return is(valueClass);
        } else {
            return object(valueClass);
        }
    }

    public LPWithParams addScriptedTypeProp(LPWithParams ccProp, String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(addTypeProp(findClass(className), bIs), Collections.singletonList(ccProp));
    }

    <T extends PropertyInterface> void addScriptedConstraint(LP<T> property, List<TypedParameter> context, Event event, boolean checked, List<NamedPropertyUsage> propUsages, LP<?> messageProperty, List<LPWithParams> properties, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        property = patchExtendParams(property, context, debugPoint);

        if (!property.property.checkAlwaysNull(true)) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        ImSet<Property<?>> checkedProps = null;
        Property.CheckType type = (checked ? Property.CheckType.CHECK_ALL : Property.CheckType.CHECK_NO);
        if (checked && propUsages != null) {
            MSet<Property<?>> mCheckedProps = SetFact.mSet();
            for (NamedPropertyUsage propUsage : propUsages) {
                LP<?> lcp = findLPByPropertyUsage(propUsage);
                mCheckedProps.add(lcp.property);
            }
            type = Property.CheckType.CHECK_SOME;
            checkedProps = mCheckedProps.immutable();
        }

        properties = checkSingleParams(properties);
        ImList<PropertyMapImplement<?, T>> mapImplements = (ImList<PropertyMapImplement<?, T>>) (ImList<?>) readCalcImplements(property.listInterfaces, getParamsPlainList(properties).toArray());
        addConstraint(property, messageProperty, mapImplements, type, checkedProps, event, this, debugPoint);
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

    public LPWithParams addScriptedSessionProp(IncrementType type, LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkSessionPropertyParameter(property);
        LP newProp;
        PrevScope scope = (type == null ? PrevScope.DB : (prevScope != null ? prevScope : PrevScope.EVENT)); // по сути оптимизация если scope известен использовать его
        if (type == null) {
            newProp = addOldProp(property.getLP(), scope);
        } else {
            newProp = addCHProp(property.getLP(), type, scope);
        }
        return new LPWithParams(newProp, property);
    }

    public LPWithParams addScriptedSignatureProp(LPWithParams property) {
        LP newProp = addClassProp(property.getLP());
        return new LPWithParams(newProp, property);
    }

    public LPWithParams addScriptedChangeClassProp(LPWithParams property) {
        LP newProp = addChangeClassProp(property.getLP());
        return new LPWithParams(newProp, property);
    }

    public LPWithParams addScriptedChangeValueClassProp(LPWithParams to, LPWithParams from) {
        LP newProp = addChangeValueClassProp(from.getLP(), to.getLP());
        return new LPWithParams(newProp, from);
    }

    public LPWithParams addScriptedActiveProp(ComponentView tab, PropertyDrawEntity property) {
        Property<?> activeProp = tab != null ? tab.getActiveTab() : property.getActiveProperty();
        return new LPWithParams(new LP<>(activeProp));
    }

    public LPWithParams addScriptedRoundProp(LPWithParams expr, LPWithParams scaleExpr) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkRoundType(expr, scaleExpr);
        List<LPWithParams> propParams = new ArrayList<>();
        propParams.add(expr);
        boolean hasScale = scaleExpr != null;
        if (hasScale) {
            propParams.add(scaleExpr);
        }
        return addScriptedJProp(hasScale ? baseLM.roundScale : baseLM.round, propParams);
    }

    public void addScriptedFollows(NamedPropertyUsage mainPropUsage, List<TypedParameter> namedParams, List<PropertyFollowsDebug> resolveOptions, LPWithParams rightProp, Event event, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findLPByPropertyUsage(mainPropUsage, namedParams);
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        addScriptedFollows(mainProp, rightProp, resolveOptions, null, event,
                getConstraintData("{logics.property.violated.consequence.from}", rightProp, mainProp, debugPoint));
    }

    private void addScriptedFollows(LP mainProp, LPWithParams rightProp, List<PropertyFollowsDebug> resolveOptions, ImList<LP> optResConds, Event event, ConstraintData constraintData) {
        Integer[] params = new Integer[rightProp.usedParams.size()];
        for (int j = 0; j < params.length; j++) {
            params[j] = rightProp.usedParams.get(j) + 1;
        }
        addFollows(mainProp, ListFact.fromJavaList(resolveOptions), optResConds, event, rightProp.getLP(), constraintData, params);
    }

    public void addScriptedWriteWhen(NamedPropertyUsage mainPropUsage, List<TypedParameter> namedParams, LPWithParams valueProp, LPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        LP mainProp = findLPByPropertyUsage(mainPropUsage, namedParams);
        if (!(mainProp.property instanceof DataProperty)) {
            errLog.emitOnlyDataPropertyIsAllowedError(parser, mainPropUsage.name);
        }
        checks.checkParamCount(mainProp, namedParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(namedParams));

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        mainProp.setWhenChange(this, action && !Settings.get().isDisableWhenCalcDo() ? Event.SESSION : null, params.toArray());
    }

    public Set<Property> findPropsByPropertyUsages(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages==null)
            return null;

        Set<Property> props = new HashSet<>(); // функционально из-за exception'а не сделаешь
        for (NamedPropertyUsage usage : propUsages) {
            LP<?> lp = findLPByPropertyUsage(usage);
            props.add(lp.property);
        }
        return props;
    }

    public ImOrderSet<ActionOrProperty> findEventActionsOrPropsByUsages(List<ActionOrPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        if(propUsages==null)
            return null;

        MOrderSet<ActionOrProperty> mProps = SetFact.mOrderSetMax(propUsages.size());
        for (ActionOrPropertyUsage usage : propUsages) {
            LAP<?, ?> lp = findLAPByEventOrPropertyUsage(usage);
            mProps.add(lp.getActionOrProperty());
        }
        return mProps.immutableOrder();
    }

    public void addScriptedWhen(LPWithParams whenProp, List<TypedParameter> newContext, LAWithParams event, List<LPWithParams> orders, boolean descending, Event baseEvent, List<LPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint, LocalizedString debugCaption) throws ScriptingErrorLog.SemanticErrorException {
        whenProp = patchExtendParams(whenProp, newContext, 0, debugPoint);

        if(noInline==null) {
            noInline = new ArrayList<>();
            for(Integer usedParam : whenProp.usedParams)
                noInline.add(new LPWithParams(usedParam));
        }
        List<Object> params = getParamsPlainList(asList(event, whenProp), orders, noInline);
        addWhenAction(baseEvent, descending, false, noInline.size(), forceInline, debugPoint, debugCaption, params.toArray());
    }

    public void addScriptedGlobalEvent(LAWithParams event, Event baseEvent, boolean single) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkEventNoParameters(event.getLP());
        addBaseEvent((Action) event.getLP().action, baseEvent, single);
    }

    public void addScriptedAspect(NamedPropertyUsage mainPropUsage, List<TypedParameter> mainPropParams, LAWithParams action, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        LA mainAction = findLAByPropertyUsage(mainPropUsage, mainPropParams);
        checks.checkParamCount(mainAction, mainPropParams.size());
        checks.checkDistinctParameters(getParamNamesFromTypedParams(mainPropParams));

        LA<PropertyInterface> mainActionLA = (LA<PropertyInterface>) mainAction;

        List<Object> params = getParamsPlainList(Collections.singletonList(action));
        ImList<ActionMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(mainActionLA.listInterfaces, params.toArray());
        addAspectEvent(mainActionLA.action, actionImplements.get(0), before);
    }

    public void addScriptedTable(String name, String dbName, List<String> classIds, boolean isFull, boolean isExplicit) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateTable(name);

        // todo [dale]: Hack. Class CustomObjectClass is created after all in InitObjectClassTask 
        boolean isCustomObjectClassTable = isCustomObjectClassTable(name, classIds);

        ValueClass[] classes;
        if (!isCustomObjectClassTable) {
            classes = findClasses(classIds).toArray(new ValueClass[classIds.size()]);
        } else {
            classes = new ValueClass[classIds.size()];
        }
        
        tempTables.add(new TemporaryTableInfo(name, dbName, classes, isFull, isExplicit, isCustomObjectClassTable));
    }

    private boolean isCustomObjectClassTable(String name, List<String> classIds) {
        return classIds.size() == 1 && classIds.get(0).equals("CustomObjectClass");
    }
    
    private void addScriptedTables(DBNamingPolicy namingPolicy) {
        for (TemporaryTableInfo info : tempTables) {
            ValueClass[] classes = info.classes;
            if (info.isCustomObjectClassTable) {
                classes = new ValueClass[] {baseLM.baseClass.objectClass};
            }
            addTable(info.name, info.dbName, info.isFull, info.isExplicit, namingPolicy, classes);
        }
        tempTables.clear(); 
    } 
    
    private List<TemporaryTableInfo> tempTables = new ArrayList<>();
    
    private static class TemporaryTableInfo {
        public final String name;
        public final String dbName;
        public final ValueClass[] classes;
        public final boolean isFull, isExplicit;
        public final boolean isCustomObjectClassTable;
        
        public TemporaryTableInfo(String name, String dbName, ValueClass[] classes, boolean isFull, boolean isExplicit, boolean isCustomObjectClassTable) {
            this.name = name;
            this.dbName = dbName;
            this.classes = classes;
            this.isFull = isFull;
            this.isExplicit = isExplicit;
            this.isCustomObjectClassTable = isCustomObjectClassTable;  
        }
    }  

    private List<LP> indexedProperties = new ArrayList<>();
    private List<String> indexNames = new ArrayList<>();
    private List<IndexType> indexTypes = new ArrayList<>();
    private List<TemporaryIndexInfo> tempIndexes = new ArrayList<>();
            
    public void addScriptedIndex(LP lp, String dbName, IndexType indexType) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkMarkStoredProperty(lp);

        indexedProperties.add(lp);
        indexNames.add(dbName);
        indexTypes.add(indexType);

        ImSet<StoredDataProperty> fullAggrProps;
        if(lp.property instanceof AggregateGroupProperty && (fullAggrProps = ((AggregateGroupProperty) lp.property).getFullAggrProps()) != null) {
            for(StoredDataProperty fullAggrProp : fullAggrProps) {
                indexedProperties.add(new LP<>(fullAggrProp));
                indexNames.add(null);
                indexTypes.add(indexType);
            }
        }
    }

    public LPWithParams findIndexProp(NamedPropertyUsage toPropertyUsage, List<LPWithParams> toPropertyMapping, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        LP toPropertyLP = findLPByPropertyUsage(toPropertyUsage, toPropertyMapping, context);
        return new LPWithParams(toPropertyLP, getParamsAssertList(toPropertyMapping));
    }

    public void addScriptedIndex(String dbName, List<TypedParameter> params, List<LPWithParams> lps, IndexType indexType) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkIndexNecessaryProperty(lps);
        checks.checkMarkStoredProperties(lps);
        checks.checkDistinctParametersList(lps);
        checks.checkIndexNumberOfParameters(params.size(), lps);
        ImOrderSet<String> keyNames = ListFact.fromJavaList(params).toOrderExclSet().mapOrderSetValues(value -> value.paramName);
        tempIndexes.add(new TemporaryIndexInfo(dbName, keyNames, getParamsPlainList(lps).toArray(), indexType));
    }

    private static class TemporaryIndexInfo {
        public String dbName;
        public ImOrderSet<String> keyNames;
        public Object[] params;
        public IndexType indexType;

        public TemporaryIndexInfo(String dbName, ImOrderSet<String> keyNames, Object[] params, IndexType indexType) {
            this.dbName = dbName;
            this.keyNames = keyNames;
            this.params = params;
            this.indexType = indexType;
        }
    }

    public void addScriptedWindow(boolean isNative, String name, LocalizedString captionStr, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkDuplicateWindow(name);

        LocalizedString caption = (captionStr == null ? LocalizedString.create(name) : captionStr);
        AbstractWindow window = isNative ? createNativeWindow(name, caption, options) : createToolbarWindow(name, caption, options);

        window.drawScrollBars = nvl(options.getDrawScrollBars(), true);
        window.titleShown = nvl(options.getDrawTitle(), true);

        addWindow(window);
    }

    private NavigatorWindow createToolbarWindow(String name, LocalizedString caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        BorderPosition borderPosition = options.getBorderPosition();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            orientation = Orientation.VERTICAL;
        }

        if (borderPosition != null && dockPosition != null) {
            errLog.emitWindowPositionConflictError(parser, name);
        }

        NavigatorWindow window;
        if (borderPosition != null) {
            window = new NavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption, borderPosition.asLayoutConstraint());
        } else if (dockPosition != null) {
            window = new NavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption, dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        } else {
            window = new NavigatorWindow(orientation.asToolbarOrientation(), elementCanonicalName(name), caption);
        }

        FlexAlignment hAlign = options.getHAlign();
        FlexAlignment vAlign = options.getVAlign();
        FlexAlignment thAlign = options.getTextHAlign();
        FlexAlignment tvAlign = options.getTextVAlign();
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

        window.propertyElementClass = options.elementClassProperty != null ? options.elementClassProperty.getLP().property : null;
        window.elementClass = options.elementClass;

        return window;
    }

    private AbstractWindow createNativeWindow(String name, LocalizedString caption, NavigatorWindowOptions options) {
        AbstractWindow window = new AbstractWindow(elementCanonicalName(name), caption);
        DockPosition dp = options.getDockPosition();
        if (dp != null)
            window.setDockPosition(dp.x, dp.y, dp.width, dp.height);

        window.propertyElementClass = options.elementClassProperty != null ? options.elementClassProperty.getLP().property : null;
        window.elementClass = options.elementClass;
        return window;
    }

    public void hideWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        findWindow(name).visible = false;
    }

    public static class NavigatorElementOptions {
        ComplexLocation<NavigatorElement> location;
        public String windowName;
        public boolean parentWindow;
        public ImageOption imageOption;
        public LPWithParams headerProperty;
        public LPWithParams showIfProperty;

        public LPWithParams elementClassProperty;
        public String elementClass;

        public String changeKey;
        public boolean showChangeKey;
        public String changeMouse;
        public boolean showChangeMouse;

        public void setChangeKey(String changeKey, boolean showChangeKey) {
            this.changeKey = changeKey;
            this.showChangeKey = showChangeKey;
        }

        public void setChangeMouse(String changeMouse, boolean showChangeMouse) {
            this.changeMouse = changeMouse;
            this.showChangeMouse = showChangeMouse;
        }

    }

    public NavigatorElement createScriptedNavigatorElement(String name, LocalizedString caption, DebugInfo.DebugPoint point,
                                                           NamedPropertyUsage actionUsage, String formName, boolean isAction) throws ScriptingErrorLog.SemanticErrorException {
        LA<?> action = null;
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

        return createNavigatorElement(name, caption, point, action, form);
    }

    private String createDefaultNavigatorElementName(LA<?> action, FormEntity form) {
        if (action != null) {
            return action.action.getName();
        } else if (form != null) {
            return form.getName();
        }
        return null;
    }

    private NavigatorElement createNavigatorElement(String name, LocalizedString caption, DebugInfo.DebugPoint point, LA<?> action, FormEntity form) {
        String canonicalName = elementCanonicalName(name);

        Supplier<LocalizedString> defaultCaption;
        AppServerImage.Reader defaultImage;
        NavigatorElement newElement;
        if (form != null) {
            newElement = createNavigatorForm(form, canonicalName);
            defaultCaption = form::getCaption;
            defaultImage = form.getNFRichDesign(getVersion()).mainContainer.image;
        } else if (action != null) {
            newElement = createNavigatorAction(action, canonicalName);
            defaultCaption = () -> action.action.caption;
            defaultImage = action.action.image;
        } else {
            newElement = createNavigatorFolder(canonicalName);
            defaultCaption = () -> LocalizedString.create(name); // CanonicalNameUtils.getName(newElement.getCanonicalName()));
            defaultImage = null;
        }

        if(caption != null)
            defaultCaption = () -> caption;

        newElement.caption = defaultCaption;
        newElement.defaultImage = defaultImage;

        newElement.setDebugPoint(point);
        addNavigatorElement(newElement);

        return newElement;
    }

    private LA<?> findNavigatorAction(NamedPropertyUsage actionUsage) throws ScriptingErrorLog.SemanticErrorException {
        assert actionUsage != null;
        if (actionUsage.classNames == null) {
            actionUsage.classNames = Collections.emptyList();
        }
        LA<?> action = findLANoParamsByPropertyUsage(actionUsage);
        checks.checkNavigatorAction(action);
        return action;
    }

    public void setupNavigatorElement(NavigatorElement element, LocalizedString caption, NavigatorElement parentElement, NavigatorElementOptions options, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        if (caption != null) {
            element.caption = () -> caption;
        }

        applyNavigatorElementOptions(element, parentElement, options, isEditOperation);
    }

    public void applyNavigatorElementOptions(NavigatorElement element, NavigatorElement parent, NavigatorElementOptions options, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        setNavigatorElementWindow(element, options.windowName, options.parentWindow);
        setNavigatorElementImage(element, options.imageOption);
        setNavigatorElementClass(element, options.elementClassProperty != null ? options.elementClassProperty.getLP().property : null, options.elementClass);
        setNavigatorElementHeader(element, options.headerProperty != null ? options.headerProperty.getLP().property : null);
        setNavigatorElementShowIf(element, options.showIfProperty != null ? options.showIfProperty.getLP().property : null);
        setNavigatorElementChangeKey(element, options.changeKey, options.showChangeKey);
        setNavigatorElementChangeMouse(element, options.changeMouse, options.showChangeMouse);

        ComplexLocation<NavigatorElement> location = options.location;
        if (parent != null && !(isEditOperation && location == null))
            addOrMoveElement(element, parent, location != null ? location : ComplexLocation.DEFAULT(), isEditOperation);
    }

    private void addOrMoveElement(NavigatorElement element, NavigatorElement parentElement, ComplexLocation<NavigatorElement> location, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        Version version = getVersion();
        checks.checkNavigatorElementMoveOperation(element, parentElement, location, isEditOperation, version);

        parentElement.addOrMove(element, location, version);
    }

    public void setNavigatorElementWindow(NavigatorElement element, String windowName, boolean parentWindow) throws ScriptingErrorLog.SemanticErrorException {
        assert element != null;

        if (windowName != null) {
            AbstractWindow window = findWindow(windowName);

            if (window instanceof NavigatorWindow) {
                element.window = (NavigatorWindow) window;
                element.parentWindow = parentWindow;
            } else {
                errLog.emitAddToSystemWindowError(parser, element.getName(), windowName);
            }
        }
    }

    public void addNavigatorScheduler(LA action, NavigatorScheduler scheduler) {
        baseLM.navigatorSchedulers.put(scheduler, action);
    }

    public static class ImageOption {
        public String imagePath;
        public LPWithParams imageLP;
        
        public boolean hasImage;
        
        public ImageOption(String imagePath) {
            this.imagePath = imagePath;
            this.hasImage = true;
        }
        
        public ImageOption(LPWithParams imageLP) {
            this.imageLP = imageLP;
            this.hasImage = true;
        }
        
        public ImageOption(boolean hasImage) {
            this.hasImage = hasImage;
        }
    }
    
    public void setNavigatorElementImage(NavigatorElement element, ImageOption imageOption) {
        if (imageOption == null) return;
        assert imageOption.imageLP == null || imageOption.imagePath == null;
        
        element.setPropertyImage(null);
        element.setImage(AppServerImage.AUTO);
        
        if (!imageOption.hasImage) {
            element.setImage(AppServerImage.NULL);
        } else if (imageOption.imageLP != null) {
            element.setPropertyImage(imageOption.imageLP.getLP().property);
        } else if (imageOption.imagePath != null) {
            element.setImage(imageOption.imagePath);
        }
    }

    public void setNavigatorElementClass(NavigatorElement element, Property elementClassProperty, String elementClass) {
        if(elementClassProperty != null)
            element.setPropertyElementClass(elementClassProperty);
        if (elementClass != null)
            element.setElementClass(elementClass);
    }

    public void setNavigatorElementHeader(NavigatorElement element, Property headerProperty) {
        if (headerProperty != null)
            element.setHeaderProperty(headerProperty);
    }

    public void setNavigatorElementShowIf(NavigatorElement element, Property showIfProperty) {
        if (showIfProperty != null)
            element.setShowIfProperty(showIfProperty);
    }

    public void setNavigatorElementChangeKey(NavigatorElement element, String changeKey, boolean showChangeKey) {
        if (changeKey != null)
            element.setChangeKey(changeKey, showChangeKey);
    }

    public void setNavigatorElementChangeMouse(NavigatorElement element, String changeMouse, boolean showChangeMouse) {
        if (changeMouse != null)
            element.setChangeMouse(changeMouse, showChangeMouse);
    }

    public void propertyDefinitionCreated(LP<?> property, DebugInfo.DebugPoint point) {
        if(property != null) { // can be null if property is param
            Property prop = property.property;
            boolean needToCreateDelegate = debugger.isEnabled() && point.needToCreateDelegate() && prop instanceof DataProperty;
            if (prop.getDebugInfo() == null) { // при использовании в propertyExpression оптимизированных join свойств, не нужно им переустанавливать DebugInfo
                PropertyDebugInfo debugInfo = new PropertyDebugInfo(point, needToCreateDelegate);
                if (needToCreateDelegate) {
                    debugger.addDelegate(debugInfo);
                }
                prop.setDebugInfo(debugInfo);
            }
        }
    }

    public void actionDefinitionBodyCreated(LAWithParams lpWithParams, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate, List<ResolveClassSet> signature) {
        actionDefinitionBodyCreated((LA<PropertyInterface>) lpWithParams.getLP(), startPoint, endPoint, modifyContext, needToCreateDelegate, signature);
    }

    public void actionDefinitionBodyCreated(LA<?> lAction, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Boolean needToCreateDelegate, List<ResolveClassSet> signature) {
        if (lAction != null) {
            //noinspection unchecked
            Action property = lAction.action;
            setDebugInfo(needToCreateDelegate, startPoint, endPoint, modifyContext, property);
        }

        if(signature != null)
            lAction.setExplicitClasses(signature);
    }

    public static void setDebugInfo(Boolean needToCreateDelegate, DebugInfo.DebugPoint point, Action property) {
        setDebugInfo(needToCreateDelegate, point, point, false, property);
    }

    private static void setDebugInfo(Boolean needToCreateDelegate, DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint, boolean modifyContext, Action property) {
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

    public void topContextActionDefinitionBodyCreated(LAWithParams lpWithParams) {
        topContextActionDefinitionBodyCreated((LA<PropertyInterface>) lpWithParams.getLP());
    }

    public void topContextActionDefinitionBodyCreated(LA<PropertyInterface> lAction) {
        boolean isDebug = debugger.isEnabled();

        if(isDebug) {
            Action property = lAction.action;

            debugger.setNewDebugStack(property);
        }
    }

    public LAWithParams modifyContextFlowActionDefinitionBodyCreated(LAWithParams lpWithParams,
                                                                     List<TypedParameter> newContext, List<TypedParameter> oldContext) {
        return modifyContextFlowActionDefinitionBodyCreated(lpWithParams, newContext, oldContext, false, null);
    }

    public LAWithParams modifyContextFlowActionDefinitionBodyCreated(LAWithParams lpWithParams,
                                                                     List<TypedParameter> newContext, List<TypedParameter> oldContext,
                                                                     boolean needFullContext, Result<List<ResolveClassSet>> rSignature) {
        boolean isDebug = debugger.isEnabled();

        if(isDebug || needFullContext) {
            lpWithParams = patchExtendParams(lpWithParams, newContext, oldContext, needFullContext, rSignature);
        }

        if (isDebug) {
            //noinspection unchecked
            LA<PropertyInterface> lAction = (LA<PropertyInterface>) lpWithParams.getLP();

            Action property = lAction.action;

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

    // assert that newContext extends oldContext (at least there is such assertion in addScriptedForAProp)
    private LAWithParams patchExtendParams(LAWithParams lpWithParams, List<TypedParameter> newContext, List<TypedParameter> oldContext, boolean needFullContext, Result<List<ResolveClassSet>> rSignature) {
        List<Integer> resultInterfaces;
        List<ResolveClassSet> signature = null;
        if(needFullContext) {
            assert oldContext.isEmpty();

            resultInterfaces = getResultInterfaces(newContext.size(), lpWithParams);
//            signature = getUsedClasses(newContext, resultInterfaces);
            signature = getClassesFromTypedParams(newContext);
        } else {
            resultInterfaces = new ArrayList<>();
            for (int i = 0; i < lpWithParams.usedParams.size(); i++) {
                Integer usedParam = lpWithParams.usedParams.get(i);
                if(usedParam >= oldContext.size()) {
                    resultInterfaces.add(usedParam);
                }
            }
        }

        if(resultInterfaces.size() != (newContext.size() - oldContext.size())) {
            // по сути этот алгоритм эмулирует создание ListAction, с докидыванием в конец виртуального action'а который использует все extend параметры, однако само действие при этом не создает
            List<LAPWithParams> allCreationParams = new ArrayList<>();
            allCreationParams.add(lpWithParams);
            for (int i = oldContext.size(); i < newContext.size(); i++) { // докидываем
                allCreationParams.add(new LPWithParams(i));
            }

            List<Object> resultParams = getParamsPlainList(allCreationParams);

            LA wrappedLA = addListAProp(newContext.size() - oldContext.size(), resultParams.toArray());
            lpWithParams = new LAWithParams(wrappedLA, mergeAllParams(allCreationParams));
        }

        if(needFullContext)
            rSignature.set(signature);

        return lpWithParams;
    }

    private <T extends PropertyInterface> LP patchExtendParams(LP whereProp, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) {
        return patchExtendParams(new LPWithParams(whereProp, true), newContext, 0, debugPoint).getLP();
    }
    private <T extends PropertyInterface> LPWithParams patchExtendParams(LPWithParams whereProp, List<TypedParameter> newContext, int oldContextSize, DebugInfo.DebugPoint debugPoint) {
        return patchExtendParams(whereProp != null ? Collections.singletonList(whereProp) : Collections.emptyList(), newContext, oldContextSize, debugPoint);
    }
    private <T extends PropertyInterface> LPWithParams patchExtendParams(LPWithParams whereProp, List<LPWithParams> lpsOrders, boolean ordersNotNull, List<TypedParameter> newContext, int oldContextSize, DebugInfo.DebugPoint debugPoint) {
        return patchExtendParams(whereProp != null ? Collections.singletonList(whereProp) : Collections.emptyList(), lpsOrders, ordersNotNull, newContext, oldContextSize, debugPoint);
    }
    private <T extends PropertyInterface> LPWithParams patchExtendParams(LPWithParams whereProp, List<LPWithParams> whereProps, List<TypedParameter> newContext, int oldContextSize, DebugInfo.DebugPoint debugPoint) {
        return patchExtendParams(BaseUtils.mergeLists(whereProp != null ? Collections.singletonList(whereProp) : Collections.emptyList(), whereProps), newContext, oldContextSize, debugPoint);
    }
    private <T extends PropertyInterface> LPWithParams patchExtendParams(List<LPWithParams> whereProps, List<TypedParameter> newContext, int oldContextSize, DebugInfo.DebugPoint debugPoint) {
        return patchExtendParams(whereProps, Collections.emptyList(), false, newContext, oldContextSize, debugPoint);
    }
    private <T extends PropertyInterface> LPWithParams patchExtendParams(List<LPWithParams> lpsWithParams, List<LPWithParams> lpsOrders, boolean ordersNotNull, List<TypedParameter> newContext, int oldContextSize, DebugInfo.DebugPoint debugPoint) {
        LPWithParams where = lpsWithParams.isEmpty() ? null : lpsWithParams.get(0);

        List<LPWithParams> allCreationParams = new ArrayList<>();
        if(where != null)
            allCreationParams.add(where);

        if(ordersNotNull)
            lpsWithParams = BaseUtils.mergeList(lpsWithParams, lpsOrders);

        LP<?> lpNotNull = lpsWithParams.isEmpty() ? null : (lpsWithParams.size() == 1 ? lpsWithParams.get(0).getLP() : addJProp(false, true, 0, and(lpsWithParams.size()), getParamsPlainList(lpsWithParams).toArray()));
        List<String> patchReasons = new ArrayList<>();
        for (int i = 0; i < newContext.size() - oldContextSize; i++) {
            int paramIndex = newContext.size() - 1 - i;
            ValueClass valueClass = newContext.get(paramIndex).cls;
            if(valueClass instanceof CustomClass && (lpNotNull == null || !lpNotNull.isFullAndContains(i, (CustomClass) valueClass, patchReasons)))
                allCreationParams.add(new LPWithParams(is(valueClass), paramIndex)); // in theory we can make one big IsClassProperty.getMapProperty, but it's not that frequent case to care about it
        }

        if(allCreationParams.size() <= 1) // optimization
            return allCreationParams.isEmpty() ? null : allCreationParams.get(0);

        System.out.println("WARNING !!! PATCHING PARAMS : " + debugPoint + " " + (lpNotNull == null ? "NO WHERE" : patchReasons.toString()));

        List<Object> resultParams = getParamsPlainList(allCreationParams);
        LP wrappedLCP = addJProp(and(allCreationParams.size()), resultParams.toArray());
        return new LPWithParams(wrappedLCP, mergeAllParams(allCreationParams));
    }

    public LPWithParams patchExtendParams(LPWithParams lpWithParams, List<TypedParameter> newContext, boolean dynamic, DebugInfo.DebugPoint debugPoint) {
        LP<?> lp = lpWithParams.getLP();
        if((!dynamic && lp.listInterfaces.size() != newContext.size()) || propertyNeedsToBeWrapped(lp)) { // all are used and we don't need to wrapProperty
            // по сути этот алгоритм эмулирует создание ListAction, с докидыванием в конец виртуального action'а который использует все extend параметры, однако само действие при этом не создает
            List<LPWithParams> allCreationParams = new ArrayList<>();
            allCreationParams.add(lpWithParams);
            int removeLast = 0;

            if(!dynamic) {
                for (int i = 0; i < newContext.size(); i++) {
                    allCreationParams.add(new LPWithParams(i));
                    removeLast++;
                }
            }

            List<Object> resultParams = getParamsPlainList(allCreationParams);
            LP wrappedLCP = addJProp(false, false, removeLast, (LP)resultParams.get(0), resultParams.subList(1, resultParams.size()).toArray());
            wrappedLCP.property.setDebugInfo(new PropertyDebugInfo(debugPoint, false));
            lpWithParams = new LPWithParams(wrappedLCP, mergeAllParams(allCreationParams));
        }

        return lpWithParams;
    }

    public void checkPropertyValue(LP property) {
        checks.checkPropertyValue(property, alwaysNullProperties);
    }

    public LPNotExpr checkNotExprInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNotCIDInExpr(ci);
        checks.checkNotCIInExpr(ci);
        checks.checkNotTLAInExpr(lp,ci);
        return null; // dropping notExpr
    }
    public LPTrivialLA checkTLAInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNotCIDInExpr(ci);
        checks.checkNotCIInExpr(ci);
        return ci instanceof LPTrivialLA ? (LPTrivialLA)ci : null;
    }
    public LPContextIndependent checkCIInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNotCIDInExpr(ci);
        checks.checkNotTLAInExpr(lp, ci);
        if(lp != null) // checking action
            return null;
        return (LPContextIndependent)ci;
    }
    public LPLiteral checkLiteralInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checkNotExprInExpr(lp, ci);
        return ci instanceof LPLiteral ? (LPLiteral)ci : null;
    }

    public String checkStringValueInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checkNotExprInExpr(lp, ci);
        return ci instanceof LPLiteral && ((LPLiteral) ci).value instanceof LocalizedString ? ((LocalizedString) ((LPLiteral) ci).value).getSourceString() : null;
    }

    public LPCompoundID checkCompoundIDInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNotCIInExpr(ci);
        checks.checkNotTLAInExpr(lp,ci);
        return ci instanceof LPCompoundID ? (LPCompoundID)ci : null;
    }

    public LPNotExpr checkNumericLiteralInExpr(LPWithParams lp, LPNotExpr ci) throws ScriptingErrorLog.SemanticErrorException {
        checkNotExprInExpr(lp, ci);
        if (ci instanceof LPLiteral && ((LPLiteral) ci).value instanceof Number) {
            Number value = (Number) ((LPLiteral) ci).value;
            if (value instanceof Integer && value.intValue() > 0) {
                return new LPLiteral(-value.intValue());
            } else if (value instanceof Long && value.longValue() > 0) {
                return new LPLiteral(-value.longValue());
            } else if (value instanceof Double && value.doubleValue() > 0) {
                return new LPLiteral(-value.doubleValue());
            } else if (value instanceof BigDecimal && ((BigDecimal) value).signum() > 0) {
                return new LPLiteral(((BigDecimal) value).negate());
            }
        }
        return null;
    }

    public void checkNoExtendContext(int oldContextSize, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoExtendContext(oldContextSize, newContext);
    }

    public void checkNoExtendContext(List<Integer> usedParams, int oldContextSize, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        checks.checkNoExtendContext(usedParams, oldContextSize, newContext);
    }

    public void initModulesAndNamespaces(List<String> requiredModules, List<String> namespacePriority) throws ScriptingErrorLog.SemanticErrorException {
        initNamespacesToModules(this, new HashSet<>());

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
            checks.checkModule(getSysModule(moduleName), moduleName);
        }

        Set<String> prioritySet = new HashSet<>();
        for (String namespaceName : namespacePriority) {
            if (prioritySet.contains(namespaceName)) {
                errLog.emitNonUniquePriorityListError(parser, namespaceName);
            }
            prioritySet.add(namespaceName);
        }
    }

    public void setPropertyScriptInfo(LAP property, String script, DebugInfo.DebugPoint point) {
        property.setCreationScript(script);
        property.setCreationPath(point.toString());
        property.setPath(point.path);
    }

    private void parseStep(ScriptParser.State state) throws RecognitionException {
        parser.initParseStep(this, createStream(), state);
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
            LogicsModule requiredModule = getSysModule(requiredModuleName);
            assert requiredModule != null;
            if (!visitedModules.contains(requiredModule)) {
                initNamespacesToModules(requiredModule, visitedModules);
            }
        }
    }

    protected LogicsModule getSysModule(String requiredModuleName) {
        return BL.getSysModule(requiredModuleName);
    }

    private void showWarnings() {
        for (String warningText : warningList) {
            systemLogger.warn("WARNING!" + warningText);
        }
    }

    public interface AbstractFormActionOrPropertyUsage {
    }

    public static abstract class BaseFormActionOrPropertyUsage implements AbstractFormActionOrPropertyUsage {
        public List<String> mapping;

        public BaseFormActionOrPropertyUsage(List<String> mapping) {
            this.mapping = mapping;
        }
        
        public void setMapping(List<String> mapping) { // need this because in formMappedProperty mapping is parsed after usage
            this.mapping = mapping;
        }
    }

    public interface AbstractFormPropertyUsage extends AbstractFormActionOrPropertyUsage { // lcp or calc
    }

    public interface AbstractFormActionUsage extends AbstractFormActionOrPropertyUsage { // LA or calc
    }

    public static abstract class FormLAPUsage<L extends LAP> implements AbstractFormActionOrPropertyUsage {
        public final L lp;
        public final List<ResolveClassSet> signature;
        public final ImOrderSet<String> mapping;

        public FormLAPUsage(L lp, ImOrderSet<String> mapping) {
            this(lp, mapping, null);            
        }
        public FormLAPUsage(L lp, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            this.lp = lp;
            this.signature = signature;
            this.mapping = mapping;
        }
    }

    public static class FormLPUsage extends FormLAPUsage<LP> implements AbstractFormPropertyUsage {
        public FormLPUsage(LP lp, ImOrderSet<String> mapping) {
            super(lp, mapping);
        }

        public FormLPUsage(LP lp, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            super(lp, mapping, signature);
        }
    }

    public static class FormLAUsage extends FormLAPUsage<LA> implements AbstractFormActionUsage {
        public FormLAUsage(LA la, ImOrderSet<String> mapping) {
            super(la, mapping);
        }

        public FormLAUsage(LA la, ImOrderSet<String> mapping, List<ResolveClassSet> signature) {
            super(la, mapping, signature);
        }
    }

    public static class NamedPropertyUsage {
        public String name;
        public List<String> classNames;

        public NamedPropertyUsage(String name) {
            this(name, null);
        }

        public NamedPropertyUsage(String name, List<String> classNames) {
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

    public static class FormPredefinedUsage extends BaseFormActionOrPropertyUsage {
        public final NamedPropertyUsage property;

        public FormPredefinedUsage(NamedPropertyUsage property, List<String> mapping) {
            super(mapping);
            this.property = property;
        }
    }
    
    public abstract static class FormActionOrPropertyUsage<U extends ActionOrPropertyUsage> extends BaseFormActionOrPropertyUsage {
        public final U usage;

        public FormActionOrPropertyUsage(U usage, List<String> mapping) {
            super(mapping);
            this.usage = usage;
        }
    }

    public static class FormPropertyUsage extends FormActionOrPropertyUsage implements AbstractFormPropertyUsage {
        public FormPropertyUsage(NamedPropertyUsage property, List<String> mapping) {
            this(new PropertyUsage(property), mapping);
        }
        public FormPropertyUsage(PropertyUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public static class FormPropertyElseActionUsage extends FormActionOrPropertyUsage {
        public FormPropertyElseActionUsage(PropertyElseActionUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public static class FormActionUsage extends FormActionOrPropertyUsage implements AbstractFormActionUsage {
        public FormActionUsage(NamedPropertyUsage property, List<String> mapping) {
            this(new ActionUsage(property), mapping);
        }
        public FormActionUsage(ActionUsage property, List<String> mapping) {
            super(property, mapping);
        }
    }

    public abstract static class ActionOrPropertyUsage {
        public final NamedPropertyUsage property;

        public ActionOrPropertyUsage(NamedPropertyUsage property) {
            this.property = property;
        }
        
        public abstract FormActionOrPropertyUsage createFormUsage(List<String> mapping);
    }

    public static class PropertyUsage extends ActionOrPropertyUsage {
        public PropertyUsage(NamedPropertyUsage property) {
            super(property);
        }

        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormPropertyUsage(this, mapping);
        }
    }

    public static class PropertyElseActionUsage extends ActionOrPropertyUsage {
        public PropertyElseActionUsage(NamedPropertyUsage property) {
            super(property);
        }

        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormPropertyElseActionUsage(this, mapping);
        }
    }

    public static class ActionUsage extends ActionOrPropertyUsage {
        public ActionUsage(NamedPropertyUsage property) {
            super(property);
        }

        @Override
        public FormActionOrPropertyUsage createFormUsage(List<String> mapping) {
            return new FormActionUsage(this, mapping);
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
        public final NamedPropertyUsage outProp;
        
        public final LPWithParams listProp;

        public final LPWithParams changeProp;
        public final boolean assign;
        public final DebugInfo.DebugPoint assignDebugPoint;
        public final boolean constraintFilter;

        public FormActionProps(LPWithParams in, Boolean inNull, boolean out, Integer outParamNum, Boolean outNull, NamedPropertyUsage outProp, boolean constraintFilter, boolean assign, LPWithParams listProp, LPWithParams changeProp, DebugInfo.DebugPoint changeDebugPoint) {
//            assert outProp == null;
            this.in = in;
            this.inNull = inNull;

            this.listProp = listProp;

            this.out = out;
            this.outParamNum = outParamNum;
            this.outNull = outNull;
            this.outProp = outProp;

            this.changeProp = changeProp;
            this.constraintFilter = constraintFilter;
            this.assign = assign;
            this.assignDebugPoint = changeDebugPoint;
        }
    }

}
