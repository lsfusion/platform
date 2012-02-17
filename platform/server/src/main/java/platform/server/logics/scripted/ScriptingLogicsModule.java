package platform.server.logics.scripted;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.server.LsfLogicsLexer;
import platform.server.LsfLogicsParser;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.window.*;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.panellocation.ToolbarPanelLocation;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.StoredDataProperty;
import platform.server.logics.property.group.AbstractGroup;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static platform.base.BaseUtils.*;
import static platform.base.BaseUtils.mergeLists;
import static platform.server.logics.scripted.ScriptingLogicsModule.InsertPosition.IN;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {

    private final static Logger scriptLogger = Logger.getLogger(ScriptingLogicsModule.class);

    private final CompoundNameResolver<LP<?>> lpResolver = new LPNameResolver();
    private final CompoundNameResolver<AbstractGroup> groupResolver = new AbstractGroupNameResolver();
    private final CompoundNameResolver<NavigatorElement> navigatorResolver = new NavigatorElementNameResolver();
    private final CompoundNameResolver<AbstractWindow> windowResolver = new WindowNameResolver();
    private final CompoundNameResolver<MetaCodeFragment> metaCodeFragmentResolver = new MetaCodeFragmentNameResolver();

    private String code = null;
    private String filename = null;
    private final BusinessLogics<?> BL;
    private final Set<String> importedModules = new HashSet<String>();
    private final ScriptingErrorLog errLog;
    private LsfLogicsParser parser;

    public enum State {GROUP, CLASS, PROP, TABLE}
    public enum ConstType { INT, REAL, STRING, LOGICAL, ENUM }
    public enum InsertPosition {IN, BEFORE, AFTER}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, UNIQUE}

    private State currentState = null;

    private Map<String, ValueClass> primitiveTypeAliases = BaseUtils.buildMap(
            Arrays.<String>asList("INTEGER", "DOUBLE", "LONG", "DATE", "BOOLEAN", "DATETIME", "TEXT"),
            Arrays.<ValueClass>asList(IntegerClass.instance, DoubleClass.instance, LongClass.instance, DateClass.instance, LogicalClass.instance, DateTimeClass.instance, TextClass.instance)
    );

    private ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        setBaseLogicsModule(baseModule);
        this.BL = BL;
        errLog = new ScriptingErrorLog("");
    }

    public ScriptingLogicsModule(String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        this(baseModule, BL);
        this.filename = filename;
    }

    public ScriptingLogicsModule(InputStream stream, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(baseModule, BL);
        this.code = IOUtils.readStreamToString(stream, "utf-8");
    }

    public void setModuleName(String moduleName) {
        setSID(moduleName);
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

    public LsfLogicsParser getParser() {
        return parser;
    }

    public void addImportedModule(String moduleName) {
        scriptLogger.info("import " + moduleName + ";");
        importedModules.add(moduleName);
    }

    protected LogicsModule findModule(String sid) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.findModule(sid);
        checkModule(module, sid);
        return module;
    }

    public String transformStringLiteral(String captionStr) {
        String caption = captionStr.replace("\\'", "'");
        caption = caption.replace("\\n", "\n");
        caption = caption.replace("\\r", "\r");
        caption = caption.replace("\\t", "\t");
        return caption.substring(1, caption.length()-1);
    }

    private ValueClass getPredefinedClass(String name) {
        if (primitiveTypeAliases.containsKey(name)) {
            return primitiveTypeAliases.get(name);
        } else if (name.startsWith("STRING[")) {
            name = name.substring("STRING[".length(), name.length() - 1);
            return StringClass.get(Integer.parseInt(name));
        } else if (name.startsWith("ISTRING[")) {
            name = name.substring("ISTRING[".length(), name.length() - 1);
            return InsensitiveStringClass.get(Integer.parseInt(name));
        }
        return null;
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
            getErrLog().emitObjectNotFoundError(getParser(), name);
        }
        return obj;
    }

    public MappedProperty getPropertyWithMapping(FormEntity form, String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> property = findLPByCompoundName(name);
        if (property.property.interfaces.size() != mapping.size()) {
            getErrLog().emitParamCountError(getParser(), property, mapping.size());
        }
        return new MappedProperty(property, getMappingObjectsArray(form, mapping));
    }

    public ValueClass findClassByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
            ValueClass valueClass = getPredefinedClass(name);
            if (valueClass == null) {
                int dotPosition = name.indexOf('.');
                if (dotPosition > 0) {
                    LogicsModule module = findModule(name.substring(0, dotPosition));
                    valueClass = module.getClassByName(name.substring(dotPosition + 1));
                } else {
                    valueClass = getClassByName(name);
                    if (valueClass == null) {
                        for (String importModuleName : importedModules) {
                            LogicsModule module = findModule(importModuleName);
                            if ((valueClass = module.getClassByName(name)) != null) {
                                break;
                            }
                        }
                    }
                }
            }
            checkClass(valueClass, name);
            return valueClass;
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract, boolean isStatic,
                                 List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedClass(" + className + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + isStatic + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        checkDuplicateClass(className);
        checkStaticClassConstraints(className, isStatic, isAbstract, instNames, instCaptions);
        checkClassParents(parentNames);

        String caption = (captionStr == null ? className : transformStringLiteral(captionStr));

        CustomClass[] parents;
        if (!isStatic && parentNames.isEmpty()) {
            parents = new CustomClass[] {baseLM.baseClass};
        } else {
            parents = new CustomClass[parentNames.size()];
            for (int i = 0; i < parentNames.size(); i++) {
                String parentName = parentNames.get(i);
                parents[i] = (CustomClass) findClassByCompoundName(parentName);
            }
        }

        assert !(isStatic && isAbstract);
        if (isStatic) {
            String[] captions = new String[instCaptions.size()];
            for (int i = 0; i < instCaptions.size(); i++) {
                captions[i] = (instCaptions.get(i) == null ? null : transformStringLiteral(instCaptions.get(i)));
            }
            addStaticClass(className, caption, instNames.toArray(new String[instNames.size()]), captions, parents);
        } else if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, parents);
        }
    }

    private AbstractGroup findGroupByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractGroup group = groupResolver.resolve(name);
        checkGroup(group, name);
        return group;
    }

    public LP<?> findLPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> property = lpResolver.resolve(name);
        checkProperty(property, name);
        return property;
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

    public MetaCodeFragment findMetaCodeFragmentByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        MetaCodeFragment code = metaCodeFragmentResolver.resolve(name);
        checkMetaCodeFragment(code, name);
        return code;
    }

    public NavigatorElement findNavigatorElementByName(String name) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement element = navigatorResolver.resolve(name);
        checkNavigatorElement(element, name);
        return element;
    }

    public List<String> getNamedParamsList(String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        List<String> paramList;
        int dotPosition = propertyName.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = findModule(propertyName.substring(0, dotPosition));
            paramList = module.getNamedParams(module.transformNameToSID(propertyName.substring(dotPosition + 1)));
        } else {
            paramList = getNamedParams(transformNameToSID(propertyName));
            if (paramList == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = findModule(importModuleName);
                    if ((paramList = module.getNamedParams(module.transformNameToSID(propertyName))) != null) {
                        break;
                    }
                }
            }
        }
        return paramList;
    }

    private List<String> getNamedParamsList(Object obj) throws ScriptingErrorLog.SemanticErrorException {
        if (obj instanceof LP) {
            return getNamedParams(((LP)obj).property.getSID());
        } else {
            return getNamedParamsList((String) obj);
        }
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGroup(" + groupName + ", " + (captionStr==null ? "" : captionStr) + ", " + (parentName == null ? "null" : parentName) + ");");
        checkDuplicateGroup(groupName);
        String caption = (captionStr == null ? groupName : transformStringLiteral(captionStr));
        AbstractGroup parentGroup = (parentName == null ? null : findGroupByCompoundName(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, String caption) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedForm(" + formName + ", " + caption + ");");
        checkDuplicateNavigatorElement(formName);
        caption = (caption == null ? formName : transformStringLiteral(caption));
        return new ScriptingFormEntity(baseLM.baseElement, this, formName, caption);
    }

    public ScriptingFormView createScriptedFormView(String formName, String caption, boolean applyDefault) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedFormView(" + formName + ", " + applyDefault + ");");

        FormEntity form = findFormByCompoundName(formName);

        ScriptingFormView formView = new ScriptingFormView(form, applyDefault, this);
        if (caption != null) {
            formView.caption = caption;
        }

        form.richDesign = formView;

        return formView;
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedForm(" + form + ");");
        addFormEntity(form);
    }

    public LP<?> addScriptedDProp(String returnClass, List<String> paramClasses, boolean innerProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDProp(" + returnClass + ", " + paramClasses + ", " + innerProp + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }
        if (innerProp) {
            return addDProp(genSID(), "", value, params);
        } else {
            StoredDataProperty dataProperty = new StoredDataProperty(genSID(), "", params, value);
            return addProperty(null, new LP<ClassPropertyInterface>(dataProperty));
        }
    }

    public int getParamIndex(String param, List<String> namedParams, boolean dynamic) throws ScriptingErrorLog.SemanticErrorException {
        int index = -1;
        if (namedParams != null) {
            index = namedParams.indexOf(param);
        }
        if (index < 0 && param.startsWith("$")) {
            index = Integer.parseInt(param.substring(1)) - 1;
            if (index < 0 || !dynamic && namedParams != null && index >= namedParams.size()) {
                errLog.emitParamIndexError(parser, index + 1, namedParams == null ? 0 : namedParams.size());
            }
        }
        if (index < 0 && namedParams != null && dynamic) {
            index = namedParams.size();
            namedParams.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, param);
        }
        return index;
    }

    public static class LPWithParams {
        public LP<?> property;
        public List<Integer> usedParams;

        public LPWithParams(LP<?> property, List<Integer> usedParams) {
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

    public void addSettingsToProperty(LP<?> property, String name, String caption, List<String> namedParams, String groupName, boolean isPersistent, boolean isData) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addSettingsToProperty(" + property.property.getSID() + ", " + name + ", " + caption + ", " +
                           namedParams + ", " + groupName + ", " + isPersistent + ");");
        checkDuplicateProperty(name);
        checkDistinctParameters(namedParams);
        checkNamedParams(property, namedParams);
        changePropertyName(property, name);

        AbstractGroup group = (groupName == null ? null : findGroupByCompoundName(groupName));
        property.property.caption = (caption == null ? name : transformStringLiteral(caption));
        addPropertyToGroup(property.property, group);
        if (isData) {
            property.property.markStored(baseLM.tableFactory);
        } else if (isPersistent) {
            addPersistent(property);
        }
        checkPropertyValue(property, name);
        addNamedParams(property.property.getSID(), namedParams);
    }

    public void setPanelLocation(LP<?> property, boolean toolbar, String onlyPropertySID, boolean defaultProperty) throws ScriptingErrorLog.SemanticErrorException {
        PanelLocation panelLocation;
        if (toolbar) {
            panelLocation = new ToolbarPanelLocation();
        } else {
            panelLocation = new ShortcutPanelLocation(defaultProperty);
            if (onlyPropertySID != null)
                ((ShortcutPanelLocation) panelLocation).setOnlyProperty(findLPByCompoundName(onlyPropertySID));
        }
        property.setPanelLocation(panelLocation);
    }

    public void setFixedCharWidth(LP<?> property, Integer fixedCharWidth) {
        if (fixedCharWidth != null)
            property.setFixedCharWidth(fixedCharWidth);
    }

    public void setMinCharWidth(LP<?> property, Integer minCharWidth) {
        if (minCharWidth != null)
            property.setMinimumCharWidth(minCharWidth);
    }

    public void setMaxCharWidth(LP<?> property, Integer maxCharWidth) {
        if (maxCharWidth != null)
            property.setMaximumCharWidth(maxCharWidth);
    }

    public void setPrefCharWidth(LP<?> property, Integer prefCharWidth) {
        if (prefCharWidth != null)
            property.setPreferredCharWidth(prefCharWidth);
    }

    public void setImage(LP<?> property, String path) {
        property.setImage(path);
    }

    public void setEditKey(LP<?> property, String code, Boolean showEditKey) {
        property.setEditKey(KeyStroke.getKeyStroke(code));
        if (showEditKey != null)
            property.setShowEditKey(showEditKey);
    }

    public void setAutoset(LP<?> property, boolean autoset) {
        property.setAutoset(autoset);
    }

    private <T extends LP<?>> void changePropertyName(T lp, String name) {
        removeModuleLP(lp);
        lp.property.setSID(transformNameToSID(name));
        lp.property.freezeSID();
        addModuleLP(lp);
    }

    public LPWithParams addScriptedJProp(LP<?> mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LP<?> prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
        } else {
            scriptLogger.info("addScriptedJProp(" + mainProp.property.getSID() + ", " + resultParams + ");");
            prop = addJProp("", mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    private LP<?> getRelationProp(String op) {
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

    private LP<?> getArithProp(String op) {
        if (op.equals("+")) {
            return baseLM.sumDouble2;
        } else if (op.equals("-")) {
            return baseLM.subtractDouble2;
        } else if (op.equals("*")) {
            return baseLM.multiplyDouble2;
        } else if (op.equals("/")) {
            return baseLM.divideDouble2;
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

    public LPWithParams addScriptedAndProp(List<Boolean> nots, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert nots.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        if (nots.size() > 0) {
            boolean[] notsArray = new boolean[nots.size()];
            for (int i = 0; i < nots.size(); i++) {
                notsArray[i] = nots.get(i);
            }
            curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedIfElseUProp(LPWithParams ifProp, LPWithParams thenProp, LPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIfElseProp(" + ifProp + ", " + thenProp + ", " + elseProp + ");");
        return addScriptedUProp(Union.EXCLUSIVE,
                                asList(addScriptedJProp(and(false), asList(thenProp, ifProp)),
                                       addScriptedJProp(and(true), asList(elseProp, ifProp))));
    }

    public LP addScriptedActionProp(String javaClassName) throws ScriptingErrorLog.SemanticErrorException {
        try {
            return baseLM.addAProp(null, (ActionProperty) Class.forName(javaClassName).getConstructor(BL.getClass()).newInstance(BL));
        } catch (ClassNotFoundException e) {
            errLog.emitClassNotFoundError(parser, javaClassName);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, javaClassName);
        }
        return null;
    }

    public LPWithParams addScriptedAdditiveProp(List<String> operands, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(getArithProp(op), asList(curLP, properties.get(i)));
        }
        return curLP;
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
        return addScriptedJProp(baseLM.minusDouble, Arrays.<LPWithParams>asList(prop));
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


    public LP addScriptedFlowAProp(List<String> innerContext, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        int mapActions[][] = new int[properties.size()][];
        for (int i = 0; i < properties.size(); i++) {
            mapActions[i] = toPrimitive(properties.get(i).usedParams);
        }

        LP[] props = new LP[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            props[i] = properties.get(i).property;
        }
        return addListAProp(null, genSID(), "", innerContext.size(), props, mapActions);
    }

    public LP addScriptedAddObjProp(String className) throws ScriptingErrorLog.SemanticErrorException {
        return getSimpleAddObjectAction((CustomClass) findClassByCompoundName(className));
    }

    public LP addScriptedMessageProp(String message) throws ScriptingErrorLog.SemanticErrorException {
        return addMAProp(message, "");
    }

    public LPWithParams addScriptedSetPropertyAProp(LPWithParams toProperty, LPWithParams fromProperty, boolean isDefault, List<String> oldContext) throws ScriptingErrorLog.SemanticErrorException {
        if (toProperty == null) {
            if (fromProperty.usedParams != null || isDefault) {
                errLog.emitLeftSideMustBeAProperty(parser);
            } else {
                errLog.emitMustBeAnActionCall(parser);
            }
        }

        if (fromProperty == null) {
            //значит либо вызов action, либо запись дефолта.. в обоих случаях используем дефолт свойство для записи
            fromProperty = new LPWithParams(baseLM.vdefault((ConcreteValueClass) toProperty.property.property.getCommonClasses().value), new ArrayList<Integer>());
        }

        List<Integer> allParams = mergeAllParams(asList(toProperty, fromProperty));

        //все использованные параметры, которые были в старом контексте, идут на вход результирующего свойства
        ArrayList<Integer> mapThisInterfacesList = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex >= oldContext.size()) {
                break;
            }
            mapThisInterfacesList.add(paramIndex);
        }
        int mapThisInterfaces[] = relativeIndexes(allParams, mapThisInterfacesList);

        int mapToInterfaces[] = relativeIndexes(allParams, toProperty.usedParams);

        LP result;
        if (fromProperty.property != null) {
            int mapFromInterfaces[] = relativeIndexes(allParams, fromProperty.usedParams);
            result = addSetPropertyAProp(null, genSID(), "", toProperty.property, mapToInterfaces, fromProperty.property, mapFromInterfaces, mapThisInterfaces);
        } else {
            // значит для записи используется параметр
            assert fromProperty.usedParams != null;

            int mapResultInterace = single(fromProperty.usedParams);
            result = addSetPropertyAProp(null, genSID(), "", toProperty.property, mapToInterfaces, mapResultInterace, mapThisInterfaces);
        }

        return new LPWithParams(result, mapThisInterfacesList);
    }

    private List<Object> getParamsPlainList(List<LPWithParams>... mappedPropLists) throws ScriptingErrorLog.SemanticErrorException {
        List<LP<?>> props = new ArrayList<LP<?>>();
        List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
        for (List<LPWithParams> mappedPropList : mappedPropLists) {
            for (LPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.property);
                usedParams.add(mappedProp.usedParams);
            }
        }
        return getParamsPlainList(props, usedParams);
    }

    private List<Object> getParamsPlainList(List<LP<?>> paramProps, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        List<Integer> allUsedParams = mergeIntLists(usedParams);
        List<Object> resultParams = new ArrayList<Object>();

        for (int i = 0; i < paramProps.size(); i++) {
            LP<?> property = paramProps.get(i);
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

    public LP<?> addScriptedGProp(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps, List<LPWithParams> orderProps,
                                  boolean ascending, LPWithParams whereProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGProp(" + type + ", " + mainProps + ", " + groupProps + ", " + orderProps + ", " +
                                            ascending + ", " + whereProp + ");");

        checkGPropOrderConsistence(type, orderProps.size());
        checkGPropAggregateConsistence(type, mainProps.size());
        checkGPropUniqueConstraints(type, mainProps, groupProps);

        List<LPWithParams> whereProps = new ArrayList<LPWithParams>();
        if (type == GroupingType.UNIQUE) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(null, asList(mainProps.get(0).usedParams.get(0))));
            }
        }
        List<Object> resultParams = getParamsPlainList(mainProps, whereProps, orderProps, groupProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps)).size();
        LP resultProp = null;
        if (type == GroupingType.SUM) {
            resultProp = addSGProp(null, genSID(), false, false, "", groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.MAX || type == GroupingType.MIN) {
            resultProp = addMGProp(null, genSID(), false, "", type == GroupingType.MIN, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.CONCAT) {
            resultProp = addOGProp(null, genSID(), false, "", GroupType.STRING_AGG, orderProps.size(), !ascending /* todo [dale]: wtf? */, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.UNIQUE) {
            resultProp = addAGProp(null, false, getSID(), false, "", false, groupPropParamCount, resultParams.toArray());
        }
        return resultProp;
    }

    private List<Object> transformSumUnionParams(List<Object> params) {
        List<Object> newList = new ArrayList<Object>();
        for (Object obj : params) {
            if (obj instanceof LP) {
                newList.add(1);
            }
            newList.add(obj);
        }
        return newList;
    }

    public LPWithParams addScriptedUProp(Union unionType, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedUProp(" + unionType + ", " + paramProps + ");");
        checkUnionPropertyParams(paramProps);
        List<Object> resultParams = getParamsPlainList(paramProps);
        if (unionType == Union.SUM) {
            resultParams = transformSumUnionParams(resultParams);
        }
        LP<?> prop = addUProp(null, "", unionType, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    public LPWithParams addScriptedOProp(PartitionType partitionType, boolean isAscending, boolean useLast, int groupPropsCnt,
                                         List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedOProp(" + partitionType + ", " + isAscending + ", " + useLast + ", " + groupPropsCnt + ", " + paramProps + ");");

        List<Object> resultParams = getParamsPlainList(paramProps);
        LP prop = addOProp(null, genSID(), false, "", partitionType, isAscending, useLast, groupPropsCnt, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    public LP<?> addScriptedSFProp(String typeName, String formulaLiteral) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSFProp(" + typeName + ", " + formulaLiteral + ");");
        ValueClass cls = findClassByCompoundName(typeName);
        checkFormulaClass(cls);
        String formulaText = transformStringLiteral(formulaLiteral);
        Set<Integer> params = findFormulaParameters(formulaText);
        checkFormulaParameters(params);
        return addSFProp(transformFormulaText(formulaText), (DataClass) cls, params.size());
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

    public LP<?> addConstantProp(ConstType type, String text) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addConstantProp(" + type + ", " + text + ");");

        switch (type) {
            case INT: return addCProp(IntegerClass.instance, Integer.parseInt(text));
            case REAL: return addCProp(DoubleClass.instance, Double.parseDouble(text));
            case STRING: text = transformStringLiteral(text); return addCProp(StringClass.get(text.length()), text);
            case LOGICAL: return addCProp(LogicalClass.instance, text.equals("TRUE"));
            case ENUM: return addStaticClassConst(text);
        }
        return null;
    }

    public LP<?> addScriptedFAProp(String formName, List<String> objectNames, List<LPWithParams> props, String className, boolean newSession, boolean isModal) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFAProp(" + formName + ", " + objectNames + ", " + props + ", " + className + ", " + newSession + ", " + isModal + ");");

        FormEntity form = findFormByCompoundName(formName);

        DataClass cls = null;
        if (className != null) {
            ValueClass valueClass = findClassByCompoundName(className);
            checkFormDataClass(valueClass);
            cls = (DataClass) valueClass;
        }

        ObjectEntity[] objects = new ObjectEntity[objectNames.size()];
        for (int i = 0; i < objectNames.size(); i++) {
            objects[i] = form.getObject(objectNames.get(i));
            if (objects[i] == null) {
                errLog.emitObjectNotFoundError(parser, objectNames.get(i));
            }
        }

        PropertyObjectEntity[] propObjects = new PropertyObjectEntity[props == null ? 0 : props.size()];
        if (props != null) {
            for (int i = 0; i < props.size(); i++) {
                PropertyObjectInterfaceEntity[] params = new PropertyObjectInterfaceEntity[props.get(i).usedParams.size()];
                for (int j = 0; j < props.get(i).usedParams.size(); j++) {
                    params[j] = objects[props.get(i).usedParams.get(j)];
                }
                propObjects[i] = form.addPropertyObject(props.get(i).property, params);
            }
        }
        return addFAProp(null, genSID(), "", form, objects, propObjects, new OrderEntity[propObjects.length], cls, newSession, isModal);
    }

    public void addScriptedMetaCodeFragment(String name, List<String> params, List<String> metaCode) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedMetaCodeFragment(" + name + ", " + params + ", " + metaCode + ");");

        checkDuplicateMetaCodeFragment(name);
        checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(params, metaCode);
        addMetaCodeFragment(name, fragment);
    }

    public void runMetaCode(String name, List<String> params) throws ScriptingErrorLog.SemanticErrorException {
        MetaCodeFragment metaCode = findMetaCodeFragmentByCompoundName(name);
        checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params);
        try {
            LsfLogicsLexer lexer = new LsfLogicsLexer(new ANTLRStringStream(code));
            LsfLogicsParser subParser = new LsfLogicsParser(new CommonTokenStream(lexer));

            lexer.self = this;
            lexer.parseState = currentState;

            subParser.self = this;
            subParser.parseState = currentState;

            subParser.statements();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        List<String> code = new ArrayList<String>();
        while (!parser.input.LT(1).getText().equals("END")) {
            if (parser.input.LT(1).getType() == LsfLogicsParser.EOF) {
                errLog.emitMetaCodeNotEndedError(parser, metaCodeName);
            }
            code.add(parser.input.LT(1).getText());
            parser.input.consume();
        }
        return code;
    }

    private LP<?> addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.indexOf('.');
        assert pointPos > 0;
        assert name.indexOf('.') == name.lastIndexOf('.');

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos+1);
        LP<?> resultProp = null;

        ValueClass cls = findClassByCompoundName(className);
        if (cls instanceof StaticCustomClass) {
            StaticCustomClass staticClass = (StaticCustomClass) cls;
            if (staticClass.hasSID(instanceName)) {
                resultProp = addCProp(staticClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static class instance", instanceName);
            }
        } else {
            errLog.emitNonStaticHasInstancesError(parser, className);
        }
        return resultProp;
    }

    public LP<?> addScriptedTypeProp(String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addTypeProp(" + className + ", " + (bIs ? "IS" : "AS") + ");");
        if (bIs) {
            return is(findClassByCompoundName(className));
        } else {
            return object(findClassByCompoundName(className));
        }
    }

    public LP<?> addScriptedTypeExprProp(LP<?> mainProp, LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(mainProp, Arrays.<LPWithParams>asList(property)).property;
    }

    public void addScriptedConstraint(LP<?> property, boolean checked, String message) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConstraint(" + property + ", " + checked + ", " + message + ");");
        if (!property.property.check()) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        property.property.caption = transformStringLiteral(message);
        addConstraint(property, checked);
    }

    public void addScriptedFollows(String mainPropName, List<String> namedParams, List<Integer> options, List<LPWithParams> props) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFollows(" + mainPropName + ", " + namedParams + ", " + options + ", " + props + ");");
        LP<?> mainProp = findLPByCompoundName(mainPropName);
        checkProperty(mainProp, mainPropName);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);

        for (int i = 0; i < props.size(); i++) {
            int[] params = new int[props.get(i).usedParams.size()];
            for (int j = 0; j < params.length; j++) {
                params[j] = props.get(i).usedParams.get(j) + 1;
            }
            follows(mainProp, options.get(i), props.get(i).property, params);
        }
    }

    public void addScriptedWriteOnChange(String mainPropName, List<String> namedParams, boolean useOld, boolean anyChange,
                                         LPWithParams valueProp, LPWithParams changeProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedWriteOnChange(" + mainPropName + ", " + namedParams + ", " + useOld + ", " + anyChange + ", " + valueProp + ", " + changeProp + ");");
        LP<?> mainProp = findLPByCompoundName(mainPropName);
        checkProperty(mainProp, mainPropName);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);

        List<Object> params = getParamsPlainList(asList(valueProp, changeProp));

        mainProp.setDerivedChange(!useOld, !anyChange, valueProp.property, BL, params.subList(1, params.size()).toArray());
    }

    public void addScriptedTable(String name, List<String> classIds) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedTable(" + name + ", " + classIds + ");");

        ValueClass[] classes = new ValueClass[classIds.size()];
        for (int i = 0; i < classIds.size(); i++) {
            classes[i] = findClassByCompoundName(classIds.get(i));
        }
        baseLM.tableFactory.include(name, classes);
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

    public NavigatorElement createScriptedNavigatorElement(String name, String caption, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedNavigatorElement(" + name + ", " + caption + ");");

        assert name != null && caption != null && anchorElement != null;

        checkDuplicateNavigatorElement(name);

        NavigatorElement newElement = addNavigatorElement(name, caption);

        setupNavigatorElement(newElement, caption, pos, anchorElement, windowName);

        return newElement;
    }

    public void setupNavigatorElement(NavigatorElement<?> element, String caption, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("setupNavigatorElement(" + element.getSID() + ", " + caption + ", " + pos + ", " + anchorElement + ", " + windowName + ");");

        assert element != null;

        if (caption != null) {
            element.caption = caption;
        }

        if (windowName != null) {
            setNavigatorElementWindow(element, windowName);
        }

        if (pos != null && anchorElement != null) {
            moveElement(element, pos, anchorElement);
        }
    }

    private void moveElement(NavigatorElement element, InsertPosition pos, NavigatorElement anchorElement) throws ScriptingErrorLog.SemanticErrorException {
        assert anchorElement != null && pos != null;
        NavigatorElement parent = null;
        if (pos == IN) {
            parent = anchorElement;
        } else {
            parent = anchorElement.getParent();
            if (parent == null) {
                errLog.emitIllegalInsertBeforeAfterNavigatorElement(parser, anchorElement.getSID());
            }
        }

        if (element.isAncestorOf(parent)) {
            errLog.emitIllegalMoveNavigatorToSubnavigator(parser, element.getSID(), parent.getSID());
        }

        switch (pos) {
            case IN:
                parent.add(element);
                break;
            case BEFORE:
                parent.addBefore(element, anchorElement);
                break;
            case AFTER:
                parent.addAfter(element, anchorElement);
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

    private void checkProperty(LP<?> lp, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, name);
        }
    }

    private void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
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

    private void checkParamCount(LP<?> mainProp, int paramCount) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramCount) {
            errLog.emitParamCountError(parser, mainProp, paramCount);
        }
    }

    private void checkPropertyValue(LP<?> property, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (!property.property.check()) {
            errLog.emitPropertyAlwaysNullError(parser, name);
        }
    }

    private void checkDuplicateClass(String className) throws ScriptingErrorLog.SemanticErrorException {
        if (getClassByName(className) != null) {
            errLog.emitAlreadyDefinedError(parser, "class", className);
        }
    }

    private void checkDuplicateGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        if (getGroupByName(groupName) != null) {
            errLog.emitAlreadyDefinedError(parser, "group", groupName);
        }
    }

    private void checkDuplicateProperty(String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (getLPByName(propName) != null) {
            errLog.emitAlreadyDefinedError(parser, "property", propName);
        }
    }

    private void checkDuplicateWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (getWindowByName(name) != null) {
            errLog.emitAlreadyDefinedError(parser, "window", name);
        }
    }

    private void checkDuplicateNavigatorElement(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (getNavigatorElementByName(name) != null) {
            errLog.emitAlreadyDefinedError(parser, "form or navigator", name);
        }
    }

    private void checkDuplicateMetaCodeFragment(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (getMetaCodeFragmentByName(name) != null) {
            errLog.emitAlreadyDefinedError(parser, "meta code", name);
        }
    }

    private void checkUnionPropertyParams(List<LPWithParams> uPropParams) throws ScriptingErrorLog.SemanticErrorException {
        int paramCnt = uPropParams.get(0).property.property.interfaces.size();
        for (LPWithParams lp : uPropParams) {
            if (lp.property.property.interfaces.size() != paramCnt) {
                errLog.emitUnionPropParamsError(parser);
            }
        }
    }

    private void checkStaticClassConstraints(String className, boolean isStatic, boolean isAbstract, List<String> instNames, List<String> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isStatic && isAbstract) {
            errLog.emitAbstractStaticClassError(parser);
        } else if (!isStatic && instNames.size() > 0) {
            errLog.emitNonStaticHasInstancesError(parser, className);
        } else if (isStatic && instNames.size() == 0) {
            errLog.emitStaticHasNoInstancesError(parser, className);
        } else if (isStatic) {
            Set<String> names = new HashSet<String>();
            for (String name : instNames) {
                if (names.contains(name)) {
                    errLog.emitAlreadyDefinedError(parser, "instance", name);
                }
                names.add(name);
            }
        }
    }

    private void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        for (String parentName : parents) {
            ValueClass valueClass = findClassByCompoundName(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }
            if (valueClass instanceof StaticCustomClass) {
                errLog.emitStaticClassAsParentError(parser, parentName);
            }
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

    private void checkFormulaParameters(Set<Integer> params) throws ScriptingErrorLog.SemanticErrorException {
        for (int param : params) {
            if (param == 0 || param > params.size()) {
                errLog.emitParamIndexError(parser, param, params.size());
            }
        }
    }

    private void checkNamedParams(LP<?> property, List<String> namedParams) throws ScriptingErrorLog.SemanticErrorException {
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

    private void checkGPropUniqueConstraints(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.UNIQUE) {
            if (mainProps.get(0).property != null) {
                errLog.emitNonObjectAggrUniqueGPropError(parser);
            }
            //todo [dale]: добавить ошибку для группировочных свойств
        }
    }

    private void parseStep(State state) {
        try {
            LsfLogicsLexer lexer = new LsfLogicsLexer(createStream());
            parser = new LsfLogicsParser(new CommonTokenStream(lexer));

            parser.self = this;
            parser.parseState = state;

            lexer.self = this;
            lexer.parseState = state;

            currentState = state;
            parser.script();
            currentState = null;

//            arithLexer lexer = new arithLexer(createStream());
//            arithParser parser = new arithParser(new CommonTokenStream(lexer));
//            parser.program();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initClasses() {
        parseStep(ScriptingLogicsModule.State.CLASS);
    }

    @Override
    public void initTables() {
        parseStep(ScriptingLogicsModule.State.TABLE);
    }

    @Override
    public void initGroups() {
        parseStep(ScriptingLogicsModule.State.GROUP);
    }

    @Override
    public void initProperties()  {
        parseStep(ScriptingLogicsModule.State.PROP);
    }

    @Override
    public void initIndexes() {
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
    }

    @Override
    public String getNamePrefix() {
        return getSID();
    }

    public abstract class CompoundNameResolver<T> {
        public final T resolve(String name) throws ScriptingErrorLog.SemanticErrorException {
            T result;
            int dotPosition = name.indexOf('.');
            if (dotPosition > 0) {
                LogicsModule module = findModule(name.substring(0, dotPosition));
                result = resolveInModule(module, name.substring(dotPosition + 1));
            } else {
                result = resolveInModule(ScriptingLogicsModule.this, name);
                if (result == null) {
                    for (String importModuleName : importedModules) {
                        LogicsModule module = findModule(importModuleName);
                        if ((result = resolveInModule(module, name)) != null) {
                            break;
                        }
                    }
                }
            }
            return result;
        }

        public abstract T resolveInModule(LogicsModule module, String simpleName);
    }

    private class LPNameResolver extends CompoundNameResolver<LP<?>> {
        @Override
        public LP<?> resolveInModule(LogicsModule module, String simpleName) {
            return module.getLPByName(simpleName);
        }
    }

    private class AbstractGroupNameResolver extends CompoundNameResolver<AbstractGroup> {
        @Override
        public AbstractGroup resolveInModule(LogicsModule module, String simpleName) {
            return module.getGroupByName(simpleName);
        }
    }

    private class NavigatorElementNameResolver extends CompoundNameResolver<NavigatorElement> {
        @Override
        public NavigatorElement resolveInModule(LogicsModule module, String simpleName) {
            return module.getNavigatorElementByName(simpleName);
        }
    }

    private class WindowNameResolver extends CompoundNameResolver<AbstractWindow> {
        @Override
        public AbstractWindow resolveInModule(LogicsModule module, String simpleName) {
            return module.getWindowByName(simpleName);
        }
    }

    private class MetaCodeFragmentNameResolver extends CompoundNameResolver<MetaCodeFragment> {
        @Override
        public MetaCodeFragment resolveInModule(LogicsModule module, String simpleName) {
            return module.getMetaCodeFragmentByName(simpleName);
        }
    }
}
