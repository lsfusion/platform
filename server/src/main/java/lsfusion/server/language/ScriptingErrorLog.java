package lsfusion.server.language;

import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.id.resolve.NamespaceElementFinder.FoundItem;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;

import java.io.StringWriter;
import java.util.List;

import static java.lang.String.format;

public class ScriptingErrorLog {
    public static class SemanticErrorException extends RecognitionException {
        private String msg;

        public SemanticErrorException(IntStream input) {
            super(input);
        }

        public void setMessage(String msg) {
            this.msg = msg;
        }

        @Override
        public String getMessage() { return msg; }
    }

    private final StringWriter errWriter = new StringWriter();
    private String moduleId = "";
    private int lineNumberShift = 0;
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public void setLineNumberShift(int lineShift) {
        this.lineNumberShift = lineShift;
    } 
    
    public void write(String s) {
        errWriter.write(s);
    }

    public String toString() {
        return errWriter.toString();
    }

    private String getSemanticRecognitionErrorText(String msg, ScriptParser parser, RecognitionException e) {
        return getRecognitionErrorText(parser, "error", getErrorMessage(parser.getCurrentParser(), msg, e), e) + "Subsequent errors (if any) could not be found.";
    }

    private String getRecognitionErrorText(ScriptParser parser, String errorType, String msg, RecognitionException e) {
        String path = parser.getCurrentScriptPath(moduleId, e.line - lineNumberShift, "\n\t\t\t");
        String hdr = path + ":" + (e.charPositionInLine + 1);
        return "[" + errorType + "]:\t" + hdr + " " + msg;
    }

    public static String getErrorMessage(BaseRecognizer parser, String oldMsg, RecognitionException e) {
        return /*BaseRecognizer.getRuleInvocationStack(e, parser.findClass().getName()) + " " + */ oldMsg;
    }

    public void displayRecognitionError(BaseRecognizer parser, ScriptParser scriptParser, String errorType, String[] tokenNames, RecognitionException e) {
        String msg = parser.getErrorMessage(e, tokenNames);
        parser.emitErrorMessage(getRecognitionErrorText(scriptParser,  errorType, msg, e));
    }

    public static void emitSemanticError(String msg, SemanticErrorException e) throws SemanticErrorException {
        e.setMessage(msg);
        throw e;
    }

    public void emitNotFoundError(ScriptParser parser, String objectName, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("%s '%s' is not found", objectName, name));
    }

    public void emitClassNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "class", name);
    }

    public void emitGroupNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "group", name);
    }

    public void emitPropertyNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "property", name);
    }

    public void emitPropertyOrActionNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "property or action", name);
    }

    public void emitActionNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "action", name);
    }

    public void emitModuleNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "module", name);
    }

    public void emitParamNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "parameter", name);
    }

    public void emitFormNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "form", name);
    }

    public void emitMetaCodeFragmentNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "meta code", name);
    }

    public void emitGroupObjectNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "group object", name);
    }

    public void emitObjectNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "object", name);
    }

    public void emitComponentNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "component", name);
    }

    public void emitNavigatorElementNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "navigator element", name);
    }

    public void emitTableNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "table", name);
    }

    public void emitWindowNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "window", name);
    }

    public void emitFilterGroupNotFoundError(ScriptParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "filter group", name);
    }

    public void emitInternalClientActionHasParamsOnFileCallingError(ScriptParser parser, String argument) throws SemanticErrorException {
        emitSimpleError(parser, "Calling .js file: INTERNAL CLIENT '" + argument + "' - Should not have arguments. Use arguments only with js function() calling");
    }

    public void emitInternalClientActionHasTooMuchToPropertiesError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "Calling .js file: INTERNAL CLIENT Should have max 1 TO property");
    }

    public void emitIllegalAddNavigatorToSubnavigatorError(ScriptParser parser, String addedElement, String addedToElement) throws SemanticErrorException {
        emitSimpleError(parser, format("can't add navigator element '%s' to it's subelement '%s'", addedElement, addedToElement));
    }

    public void emitWrongNavigatorActionError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "navigator action should not have arguments");
    }
    
    public void emitIllegalInsertBeforeAfterElementError(ScriptParser parser, String element, String parentElement, String anchorElement) throws SemanticErrorException {
        emitSimpleError(parser, format("can't insert '%s' after or before '%s' in '%s'", element, anchorElement, parentElement));
    }

    public void emitIllegalGridPropertyMoveError(ScriptParser parser, String element) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move '%s' because it should be in panel instead of grid", element));
    }

    public void emitIllegalNavigatorElementMoveError(ScriptParser parser, String element, String parentElement) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move '%s' because it's not a direct child of '%s'", element, parentElement));
    }

    public void emitIllegalParentNavigatorElementError(ScriptParser parser, String parentElement) throws SemanticErrorException {
        emitSimpleError(parser, format("element '%s' can't be a parent element because it's not a navigator folder", parentElement));
    }
    
    public void emitGroupObjectInTreeAfterNotLastError(ScriptParser parser, String groupObjectName) throws SemanticErrorException {
        emitSimpleError(parser, format("'%s' is not last in tree group - can't use it in AFTER", groupObjectName));
    }

    public void emitGroupObjectInTreeBeforeNotFirstError(ScriptParser parser, String groupObjectName) throws SemanticErrorException {
        emitSimpleError(parser, format("'%s' is not first in tree group - can't use it in BEFORE", groupObjectName));
    }

    public void emitComponentParentError(ScriptParser parser, String compName) throws SemanticErrorException {
        emitSimpleError(parser, format("component '%s' has no parent", compName));
    }
    
    public void emitComponentMustBeAContainerError(ScriptParser parser, String componentName) throws SemanticErrorException {
        emitSimpleError(parser, format("component '%s' must be a container", componentName));
    }

    public void emitIllegalMoveComponentToSubcomponentError(ScriptParser parser, String movingComponent, String movedToComponent) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move component '%s' to it's subcomponent '%s'", movingComponent, movedToComponent));
    }

    public void emitRemoveMainContainerError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't remove main container");
    }

    public void emitUnableToSetPropertyError(ScriptParser parser, String propertyName, String cause) throws SemanticErrorException {
        emitSimpleError(parser, format("unable to set property '%s'. Cause: %s", propertyName, cause));
    }

    public void emitWrongKeyStrokeFormatError(ScriptParser parser, String ksLiteral) throws SemanticErrorException {
        emitSimpleError(parser, format("can't create keystroke from string '%s'", ksLiteral));
    }

    public void emitWindowPositionNotSpecifiedError(ScriptParser parser, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("position ( POSITION(x, y, width, height) ) isn't specified for window '%s'", name));
    }

    public void emitWindowPositionConflictError(ScriptParser parser, String name) throws SemanticErrorException {
        emitSimpleError(parser, "both border position (LEFT, RIGHT, TOP or BOTTOM) and dock position (POSITION(x, y, width, height))" +
                format("are specified for window '%s', only one of those should be used", name));
    }

    public void emitAddToSystemWindowError(ScriptParser parser, String neName, String windowName) throws SemanticErrorException {
        emitSimpleError(parser, format("it's illegal to add navigator element '%s' to system window '%s'", neName, windowName));
    }

    public void emitFormulaParamIndexError(ScriptParser parser, int paramIndex, int paramCount) throws SemanticErrorException {
        String errText = "wrong parameter index $" + String.valueOf(paramIndex);
        if (paramIndex < 1) {
            errText += ", first parameter is $1";
        } else {
            errText += ", last parameter is $" + String.valueOf(paramCount);
        }
        emitSimpleError(parser, errText);
    }

    public void emitFormulaMultipleImplementationError(ScriptParser parser, SQLSyntaxType type) throws SemanticErrorException {
        emitSimpleError(parser, format("two implementations for syntax %s", (type == null ? "DEFAULT" : type)));
    }

    public void emitFormulaDifferentParamCountError(ScriptParser parser, String implementation1, String implementation2) throws SemanticErrorException {
        String errText = "formula property implementations:";
        errText += format("\n\t%s", implementation1);
        errText += format("\n\t%s", implementation2);
        errText += "\nhave different number of parameters";
        emitSimpleError(parser, errText);
    }
    
    public void emitParamClassRedefinitionError(ScriptParser parser, String paramName, String oldClassName) throws SemanticErrorException {
        emitSimpleError(parser, format("class of parameter '%s' was already defined as '%s'", paramName, oldClassName));
    }

    public void emitParamClassNonDeclarationError(ScriptParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("class of parameter '%s' should be defined at first usage", paramName));
    }

    public void emitBuiltInClassAsParentError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("built-in class '%s' cannot be inherited", className));
    }

    public void emitBuiltInClassFormSetupError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("can't set custom form for built-in class '%s'", className));
    }

    public void emitCustomClassExpectedError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("custom class parameter expected for property '%s'", propertyName));
    }

    public void emitTimeSeriesExpectedError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("time-related class parameter expected for property '%s'", propertyName));
    }

    public void emitEqualParamClassesExpectedError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("class parameters should be equal for property '%s'", propertyName));
    }

    public void emitAbstractClassInstancesDefError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "abstract class cannot be instantiated");
    }

    public void emitAbstractClassInstancesUseError(ScriptParser parser, String className, String objectName) throws SemanticErrorException {
        emitSimpleError(parser, format("static object '%s' not found (class '%s' is abstract)", objectName, className));
    }

    public void emitParamCountError(ScriptParser parser, LAP property, int paramCount) throws SemanticErrorException {
        int interfacesCount = property.getActionOrProperty().interfaces.size();
        emitParamCountError(parser, interfacesCount, paramCount);
    }

    public void emitParamCountError(ScriptParser parser, int interfacesCount, int paramCount) throws SemanticErrorException {
        emitElementCountError(parser, "parameter(s)", interfacesCount, paramCount);
    }
    
    public void emitElementCountError(ScriptParser parser, String elementName, int expected, int provided) throws SemanticErrorException {
        emitSimpleError(parser, format("%d %s expected, %d provided", expected, elementName, provided));    
    }

    public void emitConstraintPropertyAlwaysNullError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "constrained property is always NULL");
    }

    public void emitAlreadyDefinedError(ScriptParser parser, String type, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("%s '%s' was already defined", type, name));
    }

    public <T> void emitAlreadyDefinedError(ScriptParser parser, String type, String name, List<FoundItem<T>> items) throws SemanticErrorException {
        assert !items.isEmpty();
        StringBuilder formatStringBuilder = new StringBuilder(format("%s '%s' was already defined in modules:", type, name));
        for (FoundItem<T> item : items) {
            formatStringBuilder.append("\n\t\t");
            formatStringBuilder.append(item.toString());
        }
        emitSimpleError(parser, formatStringBuilder.toString());
    }

    public void emitAlreadyDefinedPropertyDrawError(ScriptParser parser, String formName, String propertyDrawName, String oldPosition) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' in form '%s' was already defined at %s", propertyDrawName, formName, oldPosition));
    }
    
    public void emitCustomPropertyViewFunctionError(ScriptParser parser, String propertyDrawName, String customRenderFunction, boolean render) throws SemanticErrorException {
        String type = render ? "render" : "editor";
        emitSimpleError(parser,
                format("Incorrect custom " + type + " function definition for %s:\n\texpected format: '<custom_" + type + "_function>',\n\tprovided: '%s'",
                        propertyDrawName,
                        customRenderFunction));
    }

    public void emitCustomPropertyWrongEditType(ScriptParser parser, String editType) throws SemanticErrorException {
        emitSimpleError(parser, format("Incorrect CUSTOM EDIT type definition. \n\texpected type: TEXT or REPLACE or none,\n\tprovided: '%s'", editType));
    }

    public void emitNamedParamsError(ScriptParser parser, List<String> paramNames, int actualParameters) throws SemanticErrorException {
        emitSimpleError(parser, format("number of actual property parameters (%d) differs from number of named parameters (%d: %s)",
                actualParameters, paramNames.size(), paramNames.toString()));
    }

    public void emitFormulaReturnClassError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("formula return class must be a built-in class, '%s' provided", className));
    }

    public void emitCIInExprError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "BY clause in GROUP operator cannot be used in expressions");
    }

    public void emitLAInExprError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "actions cannot be used in expressions");
    }

    public void emitInputDataClassError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("input class must be a built-in class, '%s' class is given", className));    
    }

    public void emitIncompatibleTypesError(ScriptParser parser, String propType) throws SemanticErrorException {
        emitSimpleError(parser, format("%s's arguments' types don't match", propType));
    }

    public void emitMetaCodeNotEndedError(ScriptParser parser, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("meta code '%s' does not end with END keyword", name));
    }

    public void emitJavaCodeNotEndedError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "java code does not end with '}>' sequence");
    }

    public void emitDistinctParamNamesError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "names of parameters should be distinct");
    }

    public void emitRedundantOrderGPropError(ScriptParser parser, ScriptingLogicsModule.GroupingType groupType) throws SemanticErrorException {
        emitSimpleError(parser, format("ORDER clause is forbidden with '%s' grouping type", groupType));
    }

    public void emitMultipleAggrGPropError(ScriptParser parser, ScriptingLogicsModule.GroupingType groupType) throws SemanticErrorException {
        emitSimpleError(parser, format("multiple aggregate properties are forbidden with '%s' grouping type", groupType));
    }

    public void emitConcatAggrGPropError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "GROUP CONCAT property should have single aggregate property (JSON type) OR two aggregate properties (second is a separator)");
    }

    public void emitNonIntegralSumArgumentError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "GROUP SUM main property should have integral class as return value");
    }
    
    public void emitNonObjectAggrGPropError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "GROUP AGGR should have simple parameter as aggregate function");
    }

    public void emitWhereGPropError(ScriptParser parser, ScriptingLogicsModule.GroupingType groupType) throws SemanticErrorException {
        emitSimpleError(parser, format("WHERE clause is forbidden with '%s' grouping type", groupType));
    }

    public void emitDifferentObjsNPropsQuantityError(ScriptParser parser, int numberOfObjects) throws SemanticErrorException {
        emitSimpleError(parser, format("number of properties specified after PARENT should be equal to number of objects (%d)", numberOfObjects));
    }

    public void emitCreatingClassInstanceError(ScriptParser parser, String exceptionMessage, String className) throws SemanticErrorException {
        emitSimpleError(parser, String.format("error '%s' occurred during creation of %s instance", exceptionMessage, className));
    }

    public void emitNotSessionOrLocalPropertyError(ScriptParser parser, String creationString) throws SemanticErrorException {
        emitSimpleError(parser, format("should be a session or local property instead of '%s'", creationString));
    }

    public void emitExtendActionContextError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "action parameters must be defined explicitly");
    }

    public void emitForActionSameContextError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "FOR action statement must introduce new parameters, use IF or WHILE instead");
    }

    public void emitNoExtendContextError(ScriptParser parser, List<String> newParameters) throws SemanticErrorException {
        emitSimpleError(parser, "introducing new parameters (" + newParameters + ") is not allowed in this context");
    }

    public void emitNestedRecursionError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "RECURSION property inside another recursive step is forbidden");
    }

    public void emitRecursiveParamsOutideRecursionError(ScriptParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("recursive parameter '%s' outside recursive step is forbidden", paramName));
    }

    public void emitParameterNotUsedInRecursionError(ScriptParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("there is no '%s' inside RECURSION", paramName));
    }

    public void emitAddActionsClassError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("built-in class '%s' cannot be used in NEW actions", className));
    }

    public void emitAggrClassError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("built-in class '%s' cannot be used in AGGR operator", className));
    }

    public void emitDeconcatIndexError(ScriptParser parser, int index, int size) throws SemanticErrorException {
        if (index == 0) {
            emitSimpleError(parser, format("wrong index '%d', indices are one-based", index));
        } else if (index > size) {
            emitSimpleError(parser, format("wrong index '%d', should be at most %d", index, size));
        }
    }

    public void emitConcatError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "CONCAT first param should be string literal OR all params should be JSON");
    }

    public void emitDeconcatError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "expression does not return a list");
    }

    public void emitIllegalWindowPartitionError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "WINDOW is allowed only with SUM and PREV types in PARTITION");
    }

    public void emitUngroupParamsCntPartitionError(ScriptParser parser, int groupPropCnt) throws SemanticErrorException {
        emitSimpleError(parser, format("UNGROUP property should have %d parameter(s)", groupPropCnt));
    }

    public void emitChangeClassActionClassError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("cannot change class to built-in or abstract class ('%s' class in given)", className));
    }

    public void emitNoInlineError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "DATA or AGGR operators cannot be used inside [ ]");
    }

    public void emitWrongClassesForTableError(ScriptParser parser, String property, String table) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' can't be included into table '%s': wrong classes", property, table));
    }

    public void emitNotAbstractPropertyError(ScriptParser parser, String propName) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' is not ABSTRACT", propName));
    }

    public void emitNotAbstractActionError(ScriptParser parser, String propName) throws SemanticErrorException {
        emitSimpleError(parser, format("action '%s' is not ABSTRACT", propName));
    }

    public void emitOwnNamespacePriorityError(ScriptParser parser, String namespaceName) throws SemanticErrorException {
        emitSimpleError(parser, format("namespace '%s' has maximum priority level and should be deleted from the PRIORITY list", namespaceName));
    }

    public void emitNamespaceNotFoundError(ScriptParser parser, String namespaceName) throws SemanticErrorException {
        emitSimpleError(parser, format("namespace '%s' was not found in required modules", namespaceName));
    }

    public void emitNonUniquePriorityListError(ScriptParser parser, String namespaceName) throws SemanticErrorException {
        emitSimpleError(parser, format("priority list contains namespace '%s' more than once", namespaceName));
    }

    public void emitEventNoParametersError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "event should have no parameters");
    }

    public void emitAmbiguousNameError(ScriptParser parser, List<LogicsModule> modules, String name) throws SemanticErrorException {
        String msg = String.format("ambiguous name '%s', list of modules:", name);
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) {
                msg = msg + ", ";
            }
            msg = msg + " " + modules.get(i).getName() + " (namespace " + modules.get(i).getNamespace() + ")";
        }
        emitSimpleError(parser, msg);
    }

    public void emitAmbiguousPropertyNameError(ScriptParser parser, List<FoundItem<LAP<?, ?>>> foundItems, String name) throws SemanticErrorException {
        StringBuilder msg = new StringBuilder(String.format("ambiguous name '%s', was found in modules:", name));
        for (FoundItem<LAP<?, ?>> item : foundItems) {
            msg.append("\n\t").append(item.toString());                
        }
        emitSimpleError(parser, msg.toString());
    }

    public void emitNeighbourPropertyError(ScriptParser parser, String name1, String name2) throws SemanticErrorException {
        emitSimpleError(parser, format("properties '%s' and '%s' should be in one group", name1, name2));
    }

    public void emitMetacodeInsideMetacodeError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "metacode cannot be defined inside another metacode");
    }

    public void emitNamespaceNameError(ScriptParser parser, String namespaceName) throws SemanticErrorException {
        emitSimpleError(parser, format("namespace name '%s' contains underscore character", namespaceName));
    }

    public void emitDuplicateClassParentError(ScriptParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("class '%s' is a parent already", className));
    }

    public void emitEvalExpressionError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "ACTION EVAL expression should be a string");
    }

    public void emitChangeClassWhereError(ScriptParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("local param '%s' must be used in WHERE clause", paramName));
    }

    public void emitAddObjToPropertyError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "TO clause should use only local parameters introduced in WHERE clause");
    }

    public void emitWrongPropertyParametersError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("wrong parameters are passed to the property '%s'", propertyName));
    }

    public void emitWrongPropertyParameterError(ScriptParser parser, String paramName, String paramClass, String actualParamClass) throws SemanticErrorException {
        emitSimpleError(parser, format("parameter '%s' of class '%s' has actual class '%s'", paramName, paramClass, actualParamClass));
    }
    
    public void emitOnlyDataOrCasePropertyIsAllowedError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("'%s' is only allowed to be DATA/MULTI/CASE property", propertyName));
    }

    public void emitOnlyDataPropertyIsAllowedError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("'%s' is only allowed to be DATA property", propertyName));
    }

    public void emitColorComponentValueError(ScriptParser parser) throws SemanticErrorException {
        emitOutOfRangeError(parser, "color component", 0, 255);
    }

    public void emitOutOfRangeError(ScriptParser parser, String valueType, int lbound, int rbound) throws SemanticErrorException {
        emitSimpleError(parser, format("%s is out of range (%d-%d)", valueType, lbound, rbound));
    }

    public void emitIntegerValueError(ScriptParser parser, String literalText) throws SemanticErrorException {
        emitSimpleError(parser, format("absolute value of INTEGER constant '%s' should be less than 2147483648 (2^31), use LONG or NUMERIC instead", literalText));
    }

    public void emitLongValueError(ScriptParser parser, String literalText) throws SemanticErrorException {
        emitSimpleError(parser, format("absolute value of LONG constant '%s' should be less than 2^63, use NUMERIC instead", literalText));
    }

    public void emitDoubleValueError(ScriptParser parser, String literalText) throws SemanticErrorException {
        emitSimpleError(parser, format("double constant '%s' is out of range", literalText));
    }

    public void emitNumericValueError(ScriptParser parser, String literalText) throws SemanticErrorException {
        emitSimpleError(parser, format("numeric constant '%s' is out of range", literalText));
    }

    public void emitDateDayError(ScriptParser parser, int y, int m, int d) throws SemanticErrorException {
        emitSimpleError(parser, format("wrong date %04d-%02d-%02d", y, m, d));
    }

    public void emitAbstractCaseImplError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "abstract CASE implementation needs WHEN ... THEN block");
    }

    public void emitAbstractNonCaseImplError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "WHEN ... THEN block should be used only with CASE abstract");
    }
    
    public void emitIndexWithoutPropertyError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "index should contain at least one property");
    }
    
    public void emitIndexPropertiesNonEqualParamsCountError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "properties in INDEX statement should have the same number of parameters");    
    }

    public void emitIndexParametersError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "all parameters that can be found in INDEX statement should be passed to every property in INDEX statement");
    }
     
    public void emitShouldBeStoredError(ScriptParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' should be materialized", propertyName));
    }

    public void emitIndexPropertiesDifferentTablesError(ScriptParser parser, String firstPropName, String secondPropName) throws SemanticErrorException {
        emitSimpleError(parser, format("properties '%s' and '%s' should be in one table to create index", firstPropName, secondPropName));
    }
    
    public void emitObjectOfGroupObjectError(ScriptParser parser, String objName, String groupObjName) throws SemanticErrorException {
        emitSimpleError(parser, format("group object '%s' does not contain object '%s'", groupObjName, objName));
    }
    
    public void emitImportNonIntegralSheetError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "Sheet index should have INTEGER or LONG value");
    }

    public void emitNavigatorElementFolderNameError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "navigator folder name should be defined");
    }

    public void emitImportFromWrongClassError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "FROM expression should return FILE value");
    }

    public void emitPropertyWithParamsExpectedError(ScriptParser parser, String propertyName, String paramClasses) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' is expected to have %s signature", propertyName, paramClasses));
    }

    public void emitRecursiveImplementError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "recursive implement");
    }

    public void emitSessionOperatorParameterError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "single parameter could not be a parameter of a session operator");
    }

    public void emitRelationalOperatorClassCommpatibilityError(ScriptParser parser, String leftClassName, String rightClassName) throws SemanticErrorException {
        emitSimpleError(parser, format("value of class '%s' is not comparable with value of class '%s'", leftClassName, rightClassName));
    }

    public void emitUseNullInsteadOfFalseError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "use NULL instead of FALSE");
    }

    public void emitEvalModuleError(ScriptParser parser, String prefix) throws SemanticErrorException {
        emitSimpleError(parser, prefix + " cannot be used in EVAL module");
    }

    public void emitSignatureParamError(ScriptParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "all params should have explicit classes defined");
    }

    public void emitSimpleError(ScriptParser parser, String message) throws SemanticErrorException {
        if (parser.getCurrentParser() != null) {
            SemanticErrorException e = new SemanticErrorException(parser.getCurrentParser().input);
            String msg = getSemanticRecognitionErrorText(message + "\n", parser, e);
            emitSemanticError(msg, e);
        } else {
            throw new ScriptErrorException(message);
        }
    }
}
