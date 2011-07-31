package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.server.LsfLogicsLexer;
import platform.server.LsfLogicsParser;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.logics.linear.LP;
import platform.server.logics.property.group.AbstractGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {
    private final static Logger scriptLogger = Logger.getLogger(ScriptingLogicsModule.class);
    private String scriptName;
    private String code = null;
    private String filename = null;
    private final BusinessLogics<?> BL;

    private final Set<String> importedModules = new HashSet<String>();

    public enum State {GROUP, CLASS, PROP, NAVIGATOR}
    public enum ConstType { INT, REAL, STRING, LOGICAL }

    private Map<String, ValueClass> primitiveTypeAliases = BaseUtils.buildMap(
            Arrays.<String>asList("INTEGER", "DOUBLE", "LONG", "DATE", "BOOLEAN"),
            Arrays.<ValueClass>asList(IntegerClass.instance, DoubleClass.instance, LongClass.instance, DateClass.instance, LogicalClass.instance)
    );

    private ScriptingLogicsModule(String scriptName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        super(scriptName);
        setBaseLogicsModule(baseModule);
        this.scriptName = scriptName;
        this.BL = BL;
    }

    public static ScriptingLogicsModule createFromString(String scriptName, String code, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.code = code;
        return module;
    }

    public static ScriptingLogicsModule createFromFile(String scriptName, String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.filename = filename;
        return module;
    }

    private CharStream createStream() throws IOException {
        if (code != null) {
            return new ANTLRStringStream(code);
        } else {
            return new ANTLRFileStream(filename);
        }
    }

    public void addImportedModule(String moduleName) {
        scriptLogger.info("import " + moduleName + ";");
        importedModules.add(moduleName);
    }

    protected LogicsModule getModule(String sid) {
        List<LogicsModule> modules = BL.getLogicModules();
        for (LogicsModule module : modules) {
            if (module.getSID().equals(sid)) {
                return module;
            }
        }
        return null;
    }

    private String transformCaptionStr(String captionStr) {
        String caption = captionStr.replace("\'", "'");
        return caption.substring(1, captionStr.length()-1);
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

    public ValueClass getClassByName(String name) {
            ValueClass valueClass = getPredefinedClass(name);
            if (valueClass == null) {
                int dotPosition = name.indexOf('.');
                if (dotPosition > 0) {
                    LogicsModule module = getModule(name.substring(0, dotPosition));
                    valueClass = module.getClass(module.transformNameToSID(name.substring(dotPosition + 1)));
                } else {
                    valueClass = getClass(transformNameToSID(name));
                    if (valueClass == null) {
                        for (String importModuleName : importedModules) {
                            LogicsModule module = getModule(importModuleName);
                            if ((valueClass = module.getClass(module.transformNameToSID(name))) != null) {
                                break;
                            }
                        }
                    }
                }
            }
            return valueClass;
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract, List<String> parentNames) {
        scriptLogger.info("addScriptedClass(" + className + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + parentNames + ");");
        String caption = (captionStr == null ? className : transformCaptionStr(captionStr));
        CustomClass[] parents = new CustomClass[parentNames.size()];
        for (int i = 0; i < parentNames.size(); i++) {
            String parentName = parentNames.get(i);
            ValueClass valueClass = getClassByName(parentName);
            assert valueClass instanceof CustomClass;
            parents[i] = (CustomClass) valueClass;
        }
        if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, parents);
        }
    }

    private AbstractGroup getGroupByName(String name) {
        AbstractGroup group;
        int dotPosition = name.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = getModule(name.substring(0, dotPosition));
            group = module.getGroup(module.transformNameToSID(name.substring(dotPosition + 1)));
        } else {
            group = getGroup(transformNameToSID(name));
            if (group == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = getModule(importModuleName);
                    if ((group = module.getGroup(module.transformNameToSID(name))) != null) {
                        break;
                    }
                }
            }
        }
        return group;
    }

    public LP<?> getLPByName(String name) {
        LP<?> property;
        int dotPosition = name.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = getModule(name.substring(0, dotPosition));
            property = module.getLP(module.transformNameToSID(name.substring(dotPosition + 1)));
        } else {
            property = getLP(transformNameToSID(name));
            if (property == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = getModule(importModuleName);
                    if ((property = module.getLP(module.transformNameToSID(name))) != null) {
                        break;
                    }
                }
            }
        }
        return property;
    }

    private LP<?> getLPByObj(Object obj) {
        if (obj instanceof LP) {
            return (LP<?>) obj;
        } else {
            return getLPByName((String) obj);
        }
    }

    private List<String> getNamedParamsList(String propertyName) {
        List<String> paramList;
        int dotPosition = propertyName.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = getModule(propertyName.substring(0, dotPosition));
            paramList = module.getNamedParams(module.transformNameToSID(propertyName.substring(dotPosition + 1)));
        } else {
            paramList = getNamedParams(transformNameToSID(propertyName));
            if (paramList == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = getModule(importModuleName);
                    if ((paramList = module.getNamedParams(module.transformNameToSID(propertyName))) != null) {
                        break;
                    }
                }
            }
        }
        return paramList;
    }

    private List<String> getNamedParamsList(Object obj) {
        if (obj instanceof LP) {
            return getNamedParams(((LP)obj).property.getSID());
        } else {
            return getNamedParamsList((String) obj);
        }
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName) {
        scriptLogger.info("addScriptedGroup(" + groupName + ", " + (captionStr==null ? "" : captionStr) + ", " + (parentName == null ? "null" : parentName) + ");");
        String caption = (captionStr == null ? groupName : transformCaptionStr(captionStr));
        AbstractGroup parentGroup = (parentName == null ? null : getGroupByName(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, String caption) {
        scriptLogger.info("createScriptedForm(" + formName + ", " + caption + ");");
        return new ScriptingFormEntity(baseLM.baseElement, this, formName, caption);
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedFrom(" + form+ ");");
        addFormEntity(form);
    }

    private String toLog(Object obj) {
        return BaseUtils.toCaption(obj);
    }

    public LP<?> addScriptedDProp(String propName, String caption, String parentGroup, String returnClass, List<String> paramClasses, boolean isPersistent, List<String> namedParams) {
        scriptLogger.info("addScriptedDProp(" + toLog(propName) + ", " + toLog(parentGroup) + ", " + returnClass + ", " +
                paramClasses + ", " + isPersistent + ", " + toLog(namedParams) + ");");

        AbstractGroup group = (parentGroup == null ? privateGroup : getGroupByName(parentGroup));
        ValueClass value = getClassByName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = getClassByName(paramClasses.get(i));
        }
        LP<?> prop;
        if (propName == null) {
            prop = addDProp(group, isPersistent, caption, value, params);
        } else {
            prop = addDProp(group, propName, isPersistent, caption, value, params);
        }
        addNamedParams(prop.property.getSID(), namedParams);
        return prop;
    }

    private int getParamIndex(String param, List<String> namedParams) {
        int index = -1;
        if (namedParams != null) {
            index = namedParams.indexOf(param);
        }
        if (index < 0 && param.startsWith("$")) {
            index = Integer.parseInt(param.substring(1)) - 1;
        }
        return index;
    }

    public LP<?> addScriptedJProp(String propName, String caption, String parentGroup, Object mainPropObj, boolean isPersistent, List<String> namedParams, List<Object> params, List<List<String>> mappings) {
        scriptLogger.info("addScriptedJProp(" + toLog(propName) + ", " + toLog(parentGroup) + ", " + mainPropObj + ", " + isPersistent + ", " + toLog(namedParams) + ", " + params + ", " + mappings + ");");

        AbstractGroup group = (parentGroup == null ? privateGroup : getGroupByName(parentGroup));
        LP<?> mainProp = getLPByObj(mainPropObj);
        List<Object> resultParams = getParamsPlainList(namedParams, params, mappings);
        LP<?> prop;
        if (propName == null) {
            prop = addJProp(group, false, isPersistent, caption, mainProp, resultParams.toArray());
        } else {
            prop = addJProp(group, false, propName, isPersistent, caption, mainProp, resultParams.toArray());
        }
        addNamedParams(prop.property.getSID(), namedParams);
        return prop;
    }

    private List<Object> getParamsPlainList(List<String> namedParams, List<Object> params, List<List<String>> mappings) {
        List<Object> resultParams = new ArrayList<Object>();
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i) instanceof LP || mappings.get(i) != null) {
                LP<?> paramProp = getLPByObj(params.get(i));
                resultParams.add(paramProp);
                for (String namedParam : mappings.get(i)) {
                    int paramIndex = getParamIndex(namedParam, namedParams);
                    assert paramIndex >= 0;
                    resultParams.add(paramIndex + 1);
                }
            } else {
                String paramStr = (String) params.get(i);
                if (namedParams != null) {
                    resultParams.add(namedParams.indexOf(paramStr) + 1);
                } else {
                    assert paramStr.startsWith("$");
                    resultParams.add(Integer.parseInt(paramStr.substring(1)));
                }
            }
        }
        return resultParams;
    }

    public LP<?> addScriptedGProp(String propName, String caption, String parentGroup, Object groupPropObj, boolean isPersistent, boolean isSGProp, List<String> namedParams, List<Object> params, List<List<String>> mappings) {
        scriptLogger.info("addScriptedGProp(" + toLog(propName) + ", " + toLog(parentGroup) + ", " + groupPropObj + ", " + isPersistent + ", " + isSGProp + ", " + toLog(namedParams) + ", " + params + ", " + mappings + ");");

        AbstractGroup group = (parentGroup == null ? privateGroup : getGroupByName(parentGroup));
        LP<?> groupProp = getLPByObj(groupPropObj);
        List<Object> resultParams = getParamsPlainList(getNamedParamsList(groupPropObj), params, mappings);
        LP<?> resultProp;
        if (isSGProp) {
            resultProp = (propName == null ? addSGProp(group, isPersistent, caption, groupProp, resultParams.toArray()) :
                                             addSGProp(group, propName, isPersistent, caption, groupProp, resultParams.toArray()));
        } else {
            resultProp = (propName == null ? addMGProp(group, isPersistent, caption, groupProp, resultParams.toArray()) :
                                             addMGProp(group, propName, isPersistent, caption, groupProp, resultParams.toArray()));
        }
        addNamedParams(resultProp.property.getSID(), namedParams);
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

    public LP<?> addScriptedUProp(String propName, String caption, String parentGroup, boolean isPersistent, Union unionType, List<String> namedParams, List<Object> params, List<List<String>> mappings) {
        scriptLogger.info("addScriptedUProp(" + toLog(propName) + ", " + toLog(parentGroup) + ", " + isPersistent + ", " + unionType + ", " + toLog(namedParams) + ", " + params + ", " + mappings + ");");

        AbstractGroup group = (parentGroup == null ? privateGroup : getGroupByName(parentGroup));
        List<Object> resultParams = getParamsPlainList(namedParams, params, mappings);
        if (unionType == Union.SUM) {
            resultParams = transformSumUnionParams(resultParams);
        }
        LP<?> resultProp = (propName == null ? addUProp(group, isPersistent, caption, unionType, resultParams.toArray()) :
                                               addUProp(group, propName, isPersistent, caption, unionType, resultParams.toArray()));
        addNamedParams(resultProp.property.getSID(), namedParams);
        return resultProp;
    }

    public LP<?> addConstantProp(ConstType type, String text) {
        scriptLogger.info("addConstantProp(" + type + ", " + text + ");");

        switch (type) {
            case INT: return addCProp(IntegerClass.instance, Integer.parseInt(text));
            case REAL: return addCProp(DoubleClass.instance, Double.parseDouble(text));
            case STRING: return addCProp(StringClass.get(text.length()), text);
            case LOGICAL: return addCProp(LogicalClass.instance, text.equals("TRUE"));
        }
        return null;
    }

    public LP<?> addScriptedTypeProp(String className, boolean bIs) {
        scriptLogger.info("addTypeProp(" + className + ", " + (bIs ? "IS" : "IF") + ");");
        if (bIs) {
            return is(getClassByName(className));
        } else {
            return object(getClassByName(className));
        }
    }

    private void parseStep(State state) {
        try {
            LsfLogicsLexer lexer = new LsfLogicsLexer(createStream());
            LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));
            parser.self = this;
            parser.parseState = state;
            parser.script();
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
        //To change body of implemented methods use File | Settings | File Templates.
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {
        parseStep(ScriptingLogicsModule.State.NAVIGATOR);
    }

    @Override
    public String getNamePrefix() {
        return scriptName;
    }
}
