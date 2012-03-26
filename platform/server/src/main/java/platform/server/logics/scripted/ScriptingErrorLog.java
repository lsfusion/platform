package platform.server.logics.scripted;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import platform.server.LsfLogicsParser;
import platform.server.logics.linear.LP;

import java.io.StringWriter;

import static java.lang.String.format;

/**
 * User: DAle
 * Date: 17.10.11
 * Time: 17:43
 */

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
    private String moduleName;

    public ScriptingErrorLog(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void write(String s) {
        errWriter.write(s);
    }

    public String toString() {
        return errWriter.toString();
    }

    public static String getErrorMessage(BaseRecognizer parser, String oldMsg, RecognitionException e) {
        return /*BaseRecognizer.getRuleInvocationStack(e, parser.getClass().getName()) + " " + */ oldMsg;
    }

    public String getRecognitionErrorText(String errorType, String msg, RecognitionException e) {
        String hdr = moduleName + ":" + e.line + ":" + e.charPositionInLine;
        return "[" + errorType + "]:\t" + hdr + " " + msg;
    }

    public String getSemanticRecognitionErrorText(String msg, LsfLogicsParser parser, RecognitionException e) {
        return "\n" + getRecognitionErrorText("error", getErrorMessage(parser, msg, e), e) + "Subsequent errors (if any) could not be found.";
    }

    public void displayRecognitionError(BaseRecognizer parser, String errorType, String[] tokenNames, RecognitionException e) {
        String msg = parser.getErrorMessage(e, tokenNames);
        parser.emitErrorMessage(getRecognitionErrorText(errorType, msg, e));
    }

    public void emitSemanticError(String msg, SemanticErrorException e) throws SemanticErrorException {
        e.setMessage(msg);
        throw e;
    }

    public void emitNotFoundError(LsfLogicsParser parser, String objectName, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("%s '%s' not found", objectName, name));
    }

    public void emitClassNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "class", name);
    }

    public void emitGroupNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "group", name);
    }

    public void emitPropertyNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "property", name);
    }

    public void emitModuleNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "module", name);
    }

    public void emitParamNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "parameter", name);
    }

    public void emitFormNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "form", name);
    }

    public void emitMetaCodeFragmentNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "meta code", name);
    }

    public void emitObjectNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "object", name);
    }

    public void emitComponentNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "component", name);
    }

    public void emitNavigatorElementNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "navigator element", name);
    }

    public void emitWindowNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "window", name);
    }

    public void emitIllegalInsertBeforeAfterNavigatorElement(LsfLogicsParser parser, String element) throws SemanticErrorException {
        emitSimpleError(parser, "can't insert component after or before '" + element + "'");
    }

    public void emitIllegalMoveNavigatorToSubnavigator(LsfLogicsParser parser, String movingElement, String movedToElement) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move navigator element '%s' to it's subelement '%s'", movingElement, movedToElement));
    }

    public void emitComponentIsNullError(LsfLogicsParser parser, String mainMsg) throws SemanticErrorException {
        emitSimpleError(parser, mainMsg + " component is null");
    }

    public void emitComponentMustBeAContainerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "component must be a container");
    }

    public void emitInsertBeforeAfterMainContainerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't insert before or after main container");
    }

    public void emitIllegalMoveComponentToSubcomponent(LsfLogicsParser parser, String movingComponent, String movedToComponent) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move component '%s' to it's subcomponent '%s'", movingComponent, movedToComponent));
    }

    public void emitRemoveMainContainerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't remove main container");
    }

    public void emitIntersectionInDifferentContainersError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "forbidden to create the intersection of objects in different containers");
    }

    public void emitUnableToSetPropertyError(LsfLogicsParser parser, String propertyName, String cause) throws SemanticErrorException {
        emitSimpleError(parser, "unable to set property '" + propertyName + "'. Cause: " + cause);
    }

    public void emitWrongKeyStrokeFormat(LsfLogicsParser parser, String ksLiteral) throws SemanticErrorException {
        emitSimpleError(parser, "can't create keystroke from string '" + ksLiteral + "'");
    }

    public void emitWindowOrientationNotSpecified(LsfLogicsParser parser, String sid) throws SemanticErrorException {
        emitSimpleError(parser, "orientation (VERTICAL or HORIZONTAL) isn't specified for window '" + sid + "'");
    }

    public void emitWindowPositionNotSpecified(LsfLogicsParser parser, String sid) throws SemanticErrorException {
        emitSimpleError(parser, "position ( POSITION(x, y, width, height) ) isn't specified for window '" + sid + "'");
    }

    public void emitWindowPositionConflict(LsfLogicsParser parser, String sid) throws SemanticErrorException {
        emitSimpleError(parser, "both border position (LEFT, RIGHT, TOP or BOTTOM) and dock position (POSITION(x, y, widht, height)) are specified for window '" + sid + "', " +
                                "only one of those should be used");
    }

    public void emitAddToSystemWindowError(LsfLogicsParser parser, String sid) throws SemanticErrorException {
        emitSimpleError(parser, "it's illegal to add navigator element to system window '" + sid + "'");
    }

    public void emitParamIndexError(LsfLogicsParser parser, int paramIndex, int paramCount) throws SemanticErrorException {
        String errText = "wrong parameter index $" + String.valueOf(paramIndex);
        if (paramIndex < 1) {
            errText += ", first parameter is $1";
        } else {
            errText += ", last parameter is $" + String.valueOf(paramCount);
        }
        emitSimpleError(parser, errText);
    }

    public void emitBuiltInClassAsParentError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("built-in class '%s' cannot be inherited", className));
    }

    public void emitStaticClassAsParentError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("static class '%s' cannot be inherited", className));
    }

    public void emitBuiltInClassFormSetupError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, "can't set custom form for built-in class '" + className + "'");
    }

    public void emitAbstractStaticClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "static сlass cannot be abstract");
    }

    public void emitNonStaticHasInstancesError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("сlass '%s' must be static to have instances", className));
    }

    public void emitStaticHasNoInstancesError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, format("static сlass '%s' should have instances", className));
    }

    public void emitParamCountError(LsfLogicsParser parser, LP<?> property, int paramCount) throws SemanticErrorException {
        int interfacesCount = property.property.interfaces.size();
        emitParamCountError(parser, interfacesCount, paramCount);
    }

    public void emitParamCountError(LsfLogicsParser parser, int interfacesCount, int paramCount) throws SemanticErrorException {
        emitSimpleError(parser, format("%d parameter(s) expected, %d provided", interfacesCount, paramCount));
    }

    public void emitConstraintPropertyAlwaysNullError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "constrained property is always NULL");
    }

    public void emitLeftSideMustBeAProperty(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "left side of set property action must be a property");
    }

    public void emitMustBeAnActionCall(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't use parameter here, must be an action call");
    }

    public void emitPropertyAlwaysNullError(LsfLogicsParser parser, String propertyName) throws SemanticErrorException {
        emitSimpleError(parser, format("property '%s' is always NULL", propertyName));
    }

    public void emitAlreadyDefinedError(LsfLogicsParser parser, String type, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("%s '%s' was already defined", type, name));
    }

    public void emitNamedParamsError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "number of named parameters should be equal to actual number of parameters");
    }

    public void emitUnionArgumentsEqualParamsCountError(LsfLogicsParser parser, String errMsgPropType) throws SemanticErrorException {
        emitSimpleError(parser, format("arguments of %s property should all have same number of parameters that equals to the number of parameters of the result property", errMsgPropType));
    }

    public void emitCasePropWhenParamMissingInThenParams(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "WHEN expressions should only have parameters used in THEN expressions");
    }

    public void emitCasePropDiffThenParamsCountError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "all THEN expressions in case property should have same number of arguments");
    }

    public void emitFormulaReturnClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "formula return class must be a built-in class");
    }

    public void emitFormDataClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "form class must be a built-in class");
    }

    public void emitCaptionNotSpecifiedError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("caption wasn't specified for '%s' element", name));
    }

    public void emitMetaCodeNotEndedError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitSimpleError(parser, format("meta code '%s' does not end with END keyword", name));
    }

    public void emitDistinctParamNamesError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "names of parameters should be distinct");
    }

    public void emitRedundantOrderGPropError(LsfLogicsParser parser, ScriptingLogicsModule.GroupingType groupType) throws SemanticErrorException {
        emitSimpleError(parser, format("ORDER properties are forbidden with '%s' grouping type", groupType));
    }

    public void emitMultipleAggrGPropError(LsfLogicsParser parser, ScriptingLogicsModule.GroupingType groupType) throws SemanticErrorException {
        emitSimpleError(parser, format("multiple aggregate properties are forbidden with '%s' grouping type", groupType));
    }

    public void emitConcatAggrGPropError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "GROUP CONCAT property should have two aggregate properties exactly (second is a separator)");
    }

    public void emitNonObjectAggrUniqueGPropError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "GROUP UNIQUE should have simple parameter as aggregate function");
    }

    public void emitDifferentObjsNPropsQuantity(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "number of properties specified after PARENT should be equal to number of objects");
    }

    public void emitCreatingClassInstanceError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        emitSimpleError(parser, "error occurred during creation of " + className + " instance");
    }

    public void emitNotActionExecutedPropertyError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "executed property should be an action");
    }

    public void emitExtendActionContextError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "action parameters must be defined explicitly");
    }

    public void emitForActionSameContextError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "FOR action statement must introduce new parameters, use IF or WHILE instead");
    }

    public void emitNestedRecursionError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "RECURSION property inside another recursive step is forbidden");
    }

    public void emitRecursiveParamsOutideRecursionError(LsfLogicsParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("recursive parameter '%s' outside recursive step is forbidden", paramName));
    }

    public void emitParameterNotUsedInRecursionError(LsfLogicsParser parser, String paramName) throws SemanticErrorException {
        emitSimpleError(parser, format("there is no '%s' inside RECURSION", paramName));
    }

    public void emitAddObjClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "built-in class cannot be used in ADDOBJ action");
    }

    public void emitNecessaryPropertyError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "single parameter is forbidden in this context");
    }

    public void emitDeconcatIndexError(LsfLogicsParser parser, int index, int size) throws SemanticErrorException {
        if (index == 0) {
            emitSimpleError(parser, "indices are one-based");
        } else if (index > size) {
            emitSimpleError(parser, format("wrong index, should be at most %d", size));
        }
    }

    public void emitDeconcatError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "expression is not a list");
    }

    public void emitIllegalWindowPartitionError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "WINDOW allowed only in SUM and PREV types of PARTITION");
    }

    public void emitUngroupParamsCntPartitionError(LsfLogicsParser parser, int groupPropCnt) throws SemanticErrorException {
        emitSimpleError(parser, format("UNGROUP property should have %d parameter(s)", groupPropCnt));
    }

    public void emitFormActionObjectsMappingError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "all objects should have mapping");
    }

    private void emitSimpleError(LsfLogicsParser parser, String message) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText(message + "\n", parser, e);
        emitSemanticError(msg, e);
    }
}
