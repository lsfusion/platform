package lsfusion.server.logics.scripted;

import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.PropertyCanonicalNameUtils;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.resolving.*;
import lsfusion.server.logics.scripted.ScriptingLogicsModule.GroupingType;
import lsfusion.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
import lsfusion.server.logics.table.ImplementTable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptingLogicsModuleChecks {
    private ScriptingLogicsModule LM;
    private ScriptingErrorLog errLog;
    private ScriptParser parser;
    
    public ScriptingLogicsModuleChecks(ScriptingLogicsModule LM) {
        this.LM = LM;
        this.errLog = LM.getErrLog();
        this.parser = LM.getParser();
    } 
    
    public void checkGroup(AbstractGroup group, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (group == null) {
            errLog.emitGroupNotFoundError(parser, name);
        }
    }

    public void checkClass(ValueClass cls, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (cls == null) {
            errLog.emitClassNotFoundError(parser, name);
        }
    }

    public void checkComponent(ComponentView component, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (component == null) {
            errLog.emitComponentNotFoundError(parser, name);
        }
    }

    public void checkProperty(LP lp, String name, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, PropertyCanonicalNameUtils.createName(null, name, signature));
        }
    }

    public void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
        }
    }

    public void checkNamespace(String namespaceName) throws ScriptingErrorLog.SemanticErrorException {
        if (!LM.getNamespaceToModules().containsKey(namespaceName)) {
            errLog.emitNamespaceNotFoundError(parser, namespaceName);
        }
    }

    public void checkWindow(AbstractWindow window, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, name);
        }
    }

    public void checkNavigatorElement(NavigatorElement element, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (element == null) {
            errLog.emitNavigatorElementNotFoundError(parser, name);
        }
    }

    public void checkTable(ImplementTable table, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (table == null) {
            errLog.emitTableNotFoundError(parser, name);
        }
    }

    public void checkForm(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (form == null) {
            errLog.emitFormNotFoundError(parser, name);
        }
    }

    public void checkMetaCodeFragment(MetaCodeFragment code, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (code == null) {
            errLog.emitMetaCodeFragmentNotFoundError(parser, name);
        }
    }

    public void checkParamCount(LP mainProp, int paramCount) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramCount) {
            errLog.emitParamCountError(parser, mainProp, paramCount);
        }
    }

    public void checkPropertyValue(LP property, Map<Property, String> alwaysNullProperties) {
        if (property.property instanceof CalcProperty && !((CalcProperty)property.property).checkAlwaysNull(false) && !alwaysNullProperties.containsKey(property.property)) {
            String path = parser.getCurrentScriptPath(LM.getName(), parser.getCurrentParserLineNumber(), "\n\t\t\t");
            String location = path + ":" + (parser.getCurrentParser().input.LT(1).getCharPositionInLine() + 1);
            alwaysNullProperties.put(property.property, location);
        }
    }

    public void checkDuplicateClass(String className, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), className, null, new ModuleClassFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "class", className, module.getName());
        }
    }

    public void checkDuplicateGroup(String groupName, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), groupName, null, new ModuleGroupFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "group", groupName, module.getName());
        }
    }

    public void checkDuplicateProperty(String propName, List<ResolveClassSet> signature, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), propName, signature, new ModuleEqualLPFinder(false));
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "property", propName, module.getName());
        }
        ModuleEqualLPFinder finder = new ModuleEqualLPFinder(true);
        if (!finder.resolveInModule(LM, propName, signature).isEmpty()) {
            errLog.emitAlreadyDefinedInModuleError(parser, "property", propName, LM.getName());
        }
    }

    public void checkDuplicateWindow(String windowName, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), windowName, null, new ModuleWindowFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "window", windowName, module.getName());
        }
    }

    public void checkDuplicateNavigatorElement(String name, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), name, null, new ModuleNavigatorElementFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "navigator", name, module.getName());
        }
    }

    public void checkDuplicateForm(String name, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), name, null, new ModuleFormFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "form", name, module.getName());
        }
    }

    public void checkDuplicateMetaCodeFragment(String name, int paramCnt, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), name, paramCnt, new ModuleMetaCodeFragmentFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "meta code", name, module.getName());
        }
    }

    public void checkDuplicateTable(String name, BusinessLogics<?> BL) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModuleContainingElement(LM.getNamespace(), name, null, new ModuleTableFinder());
        if (module != null) {
            errLog.emitAlreadyDefinedInModuleError(parser, "table", name, module.getName());
        }
    }

    public void checkPropertyTypes(List<LPWithParams> properties, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        LP lp1 = properties.get(0).property;
        if(lp1 == null)
            return;
        Property prop1 = lp1.property;
        for (int i = 1; i < properties.size(); i++) {
            LP lp2 = properties.get(i).property;
            if(lp2 == null)
                return;
            Property prop2 = lp2.property;
            if (prop1.getType() != null && prop2.getType() != null && prop1.getType().getCompatible(prop2.getType()) == null) {
                errLog.emitIncompatibleTypes(parser, errMsgPropType);
            }
        }
    }

    public void checkStaticClassConstraints(boolean isAbstract, List<String> instNames, List<LocalizedString> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isAbstract && !instNames.isEmpty()) {
            errLog.emitAbstractClassInstancesDefError(parser);
        }

        Set<String> names = new HashSet<>();
        for (String name : instNames) {
            if (names.contains(name)) {
                errLog.emitAlreadyDefinedError(parser, "instance", name);
            }
            names.add(name);
        }
    }

    public void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        Set<ValueClass> parentsSet = new HashSet<>();
        for (String parentName : parents) {
            ValueClass valueClass = LM.findClass(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }

            if (parentsSet.contains(valueClass)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            parentsSet.add(valueClass);
        }
    }


    public void checkCIInExpr(ScriptingLogicsModule.LCPContextIndependent lcp) throws ScriptingErrorLog.SemanticErrorException {
        if (lcp != null) {
            errLog.emitCIInExpr(parser);
        }
    }

    public void checkFormulaClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormulaReturnClassError(parser);
        }
    }

    public void checkFormDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormDataClassError(parser);
        }
    }

    public void checkInputDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitInputDataClassError(parser);
        }
    }

    public void checkChangeClassActionClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof ConcreteCustomClass)) {
            errLog.emitChangeClassActionClassError(parser);
        }
    }

    public void checkSingleImplementation(List<SQLSyntaxType> types) throws ScriptingErrorLog.SemanticErrorException {
        Set<SQLSyntaxType> foundTypes = new HashSet<>();
        for (SQLSyntaxType type : types) {
            if (!foundTypes.add(type)) {
                errLog.emitFormulaMultipleImplementationError(parser, type);
            }
        }
    }

    public void checkFormulaParameters(Set<Integer> params) throws ScriptingErrorLog.SemanticErrorException {
        for (int param : params) {
            if (param == 0 || param > params.size()) {
                errLog.emitFormulaParamIndexError(parser, param, params.size());
            }
        }
    }

    public void checkNamedParams(LP property, List<String> namedParams) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() != namedParams.size() && !namedParams.isEmpty()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    public <T> void checkDistinctParameters(List<T> params) throws ScriptingErrorLog.SemanticErrorException {
        Set<T> paramsSet = new HashSet<>(params);
        if (paramsSet.size() < params.size()) {
            errLog.emitDistinctParamNamesError(parser);
        }
    }

    public void checkDistinctParametersList(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        for (LPWithParams lp : lps) {
            checkDistinctParameters(lp.usedParams);
        }
    }

    public void checkMetaCodeParamCount(MetaCodeFragment code, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (code.parameters.size() != paramCnt) {
            errLog.emitParamCountError(parser, code.parameters.size(), paramCnt);
        }
    }

    public void checkGPropOrderConsistence(GroupingType type, int orderParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && type != GroupingType.LAST && orderParamsCnt > 0) {
            errLog.emitRedundantOrderGPropError(parser, type);
        }
    }

    public void checkGPropAggregateConsistence(GroupingType type, int aggrParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && aggrParamsCnt > 1) {
            errLog.emitMultipleAggrGPropError(parser, type);
        }
        if (type == GroupingType.CONCAT && aggrParamsCnt != 2) {
            errLog.emitConcatAggrGPropError(parser);
        }
    }

    public void checkGPropSumConstraints(GroupingType type, LPWithParams mainProp) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.SUM && mainProp.property != null) {
            if (!(mainProp.property.property.getValueClass(ClassType.valuePolicy).getType() instanceof IntegralClass)) {
                errLog.emitNonIntegralSumArgumentError(parser);
            }
        }
    }

    public void checkGPropAggrConstraints(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (mainProps.get(0).property != null) {
                errLog.emitNonObjectAggrGPropError(parser);
            }
        }
    }

    public void checkGPropWhereConsistence(GroupingType type, LPWithParams where) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.AGGR && type != GroupingType.NAGGR && type != GroupingType.LAST && where != null) {
            errLog.emitWhereGPropError(parser, type);
        }
    }

    public void checkActionProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LAP<?>)) {
            errLog.emitNotActionPropertyError(parser);
        }
    }

    public void checkNavigatorAction(LAP<?> property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.listInterfaces.size() > 0) {
            errLog.emitWrongNavigatorAction(parser);
        }
    }

    public void checkAddActionsClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitAddActionsClassError(parser);
        }
    }

    public void checkCustomClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitCustomClassExpectedError(parser);
        }
    }

    void checkCalculationProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LCP<?>)) {
            errLog.emitNotCalculationPropertyError(parser);
        }
    }

    public void checkSessionProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property.property instanceof SessionDataProperty)) {
            errLog.emitNotSessionOrLocalPropertyError(parser);
        }
    }

    public void checkForActionPropertyConstraints(boolean isRecursive, List<Integer> oldContext, List<Integer> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (!isRecursive && oldContext.size() == newContext.size()) {
            errLog.emitForActionSameContextError(parser);
        }
    }

    public void checkRecursionContext(List<String> context, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
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

    public void checkIndexNecessaryProperty(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        boolean hasProperty = false;
        for (LPWithParams lp : lps) {
            if (lp.property != null) {
                hasProperty = true;
                break;
            }
        }
        if (!hasProperty) {
            errLog.emitIndexWithoutPropertyError(parser);
        }
    }

    public void checkStoredProperties(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        ImplementTable table = null;
        String firstPropertyName = null;
        for (LPWithParams lp : lps) {
            if (lp.property != null) {
                checkCalculationProperty(lp.property);
                CalcProperty<?> calcProperty = (CalcProperty<?>) lp.property.property;
                if (!calcProperty.isStored()) {
                    errLog.emitShouldBeStoredError(parser, calcProperty.getName());
                }
                if (table == null) {
                    table = calcProperty.mapTable.table;
                    firstPropertyName = calcProperty.getName();
                } else if (table != calcProperty.mapTable.table) {
                    errLog.emitIndexPropertiesDifferentTablesError(parser, firstPropertyName, calcProperty.getName());
                }
            }
        }
    }

    public void checkIndexNumberOfParameters(int paramsCount, List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        int paramsInProp = -1;
        for (LPWithParams lp : lps) {
            if (lp.property != null) {
                if (paramsInProp == -1) {
                    paramsInProp = lp.usedParams.size();
                } else if (lp.usedParams.size() != paramsInProp){
                    errLog.emitIndexPropertiesNonEqualParamsCountError(parser);
                }
            }
        }
        if (paramsCount != paramsInProp) {
            errLog.emitIndexParametersError(parser);
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

    public void checkPartitionWindowConsistence(PartitionType partitionType, boolean useLast) throws ScriptingErrorLog.SemanticErrorException {
        if (!useLast && (partitionType != PartitionType.SUM && partitionType != PartitionType.PREVIOUS)) {
            errLog.emitIllegalWindowPartitionError(parser);
        }
    }

    public void checkPartitionUngroupConsistence(LP ungroupProp, int groupPropCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (ungroupProp != null && ungroupProp.property.interfaces.size() != groupPropCnt) {
            errLog.emitUngroupParamsCntPartitionError(parser, groupPropCnt);
        }
    }

//    public void checkClassWhere(LCP<?> property, String name) {
//        ClassWhere<Integer> classWhere = property.getClassWhere(ClassType.signaturePolicy);
//        boolean needWarning = false;
//        if (classWhere.wheres.length > 1) {
//            needWarning = true;
//        } else {
//            AbstractClassWhere.And<Integer> where = classWhere.wheres[0];
//            for (int i = 0; i < where.size(); ++i) {
//                ResolveClassSet acSet = where.getValue(i);
//                if (acSet instanceof UpClassSet && ((UpClassSet)acSet).wheres.length > 1 ||
//                    acSet instanceof OrObjectClassSet && ((OrObjectClassSet)acSet).up.wheres.length > 1) {
//
//                    needWarning = true;
//                    break;
//                }
//            }
//        }
//        if (needWarning) {
//            warningList.add(" Property " + name + " has class where " + classWhere);
//        }
//    }

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

    public void checkChangeClassWhere(boolean contextExtended, LPWithParams param, LPWithParams where, List<ScriptingLogicsModule.TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
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

    public void checkImplementIsNotMain(LP mainProp, LP implProp) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp == implProp) {
            errLog.emitRecursiveImplementError(parser);
        }
    }

    public void checkNavigatorElementName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (name == null) {
            errLog.emitNavigatorElementFolderNameError(parser);
        }
    }

    public void checkNavigatorElementMoveOperation(NavigatorElement element, NavigatorElement parentElement, 
                                                   NavigatorElement anchorElement, boolean isEditOperation, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (parentElement.isLeafElement()) {
            errLog.emitIllegalParentNavigatorElement(parser, parentElement.getCanonicalName());
        }

        // если редактирование существующего элемента, и происходит перемещение элемента, то оно должно происходить только внутри своего уровня 
        if (isEditOperation && !parentElement.equals(element.getNFParent(version))) {
            errLog.emitIllegalNavigatorElementMove(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }

        if (anchorElement != null && !parentElement.equals(anchorElement.getNFParent(version))) {
            errLog.emitIllegalInsertBeforeAfterElement(parser, element.getCanonicalName(), parentElement.getCanonicalName(), anchorElement.getCanonicalName());
        }

        if (element.isAncestorOf(parentElement, version)) {
            errLog.emitIllegalAddNavigatorToSubnavigator(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }
    } 
    
    public void checkAssignProperty(LPWithParams fromProperty, LPWithParams toProperty) throws ScriptingErrorLog.SemanticErrorException {
        if (!(toProperty.property.property instanceof DataProperty || toProperty.property.property instanceof CaseUnionProperty || toProperty.property.property instanceof JoinProperty)) { // joinproperty только с неповторяющимися параметрами
            errLog.emitOnlyDataCasePropertyIsAllowedError(parser, toProperty.property.property.getName());
        }

        if (fromProperty.property != null && fromProperty.property.property.getType() != null &&
                toProperty.property.property.getType().getCompatible(fromProperty.property.property.getType()) == null) {
            errLog.emitIncompatibleTypes(parser, "ASSIGN");
        }
    }

    public void checkImportFromFileExpression(LPWithParams params) throws ScriptingErrorLog.SemanticErrorException {
        if (params.property != null && !(params.property.property.getValueClass(ClassType.valuePolicy).getType() instanceof FileClass)) {
            errLog.emitImportFromWrongClassError(parser);    
        }
    }
}
