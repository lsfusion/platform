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
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText(objectName + " '" + name + "' not found\n", parser, e);
        emitSemanticError(msg, e);
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

    public void emitComponentNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "component", name);
    }

    public void emitNavigatorElementNotFoundError(LsfLogicsParser parser, String name) throws SemanticErrorException {
        emitNotFoundError(parser, "navigator element", name);
    }

    public void emitIllegalInsertBeforeAfterNavigatorElement(LsfLogicsParser parser, String element) throws SemanticErrorException {
        emitSimpleError(parser, "can't insert component after or before '" + element + "'");
    }

    public void emitIllegalMoveNavigatorToSubnavigator(LsfLogicsParser parser, String movingElement, String movedToElement) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move navigator element '%s' to it's subelement '%s'.", movingElement, movedToElement));
    }

    public void emitComponentIsNullError(LsfLogicsParser parser, String mainMsg) throws SemanticErrorException {
        emitSimpleError(parser, mainMsg + " component is null");
    }

    public void emitComponentMustBeAContainerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "component must be a container.");
    }

    public void emitInsertBeforeAfterMainContainerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't insert before or after main container.");
    }

    public void emitIllegalMoveComponentToSubcomponent(LsfLogicsParser parser, String movingComponent, String movedToComponent) throws SemanticErrorException {
        emitSimpleError(parser, format("can't move component '%s' to it's subcomponent '%s'.", movingComponent, movedToComponent));
    }

    public void emitRemoveMainContinaerError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "can't remove main container.");
    }

    public void emitIntersectionInDifferentContainersError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "forbidden to create the intersection of objects in different containers.");
    }

    public void emitUnableToSetPropertyError(LsfLogicsParser parser, String propertyName, String cause) throws SemanticErrorException {
        emitSimpleError(parser, "unable to set property '" + propertyName + "'. Cause: " + cause);
    }

    public void emitWrongKeyStrokeFormat(LsfLogicsParser parser, String ksLiteral) throws SemanticErrorException {
        emitSimpleError(parser, "can't create keystroke from string '" + ksLiteral + "'");
    }

    public void emitParamIndexError(LsfLogicsParser parser, int paramIndex, int paramCount) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String errText = "wrong parameter index $" + String.valueOf(paramIndex);
        if (paramIndex < 1) {
            errText += ", first parameter is $1";
        } else {
            errText += ", last parameter is $" + String.valueOf(paramCount);
        }
        String msg = getSemanticRecognitionErrorText(errText + "\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitBuiltInClassAsParentError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("built-in class '" + className + "' cannot be inherited\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitStaticClassAsParentError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("static class '" + className + "' cannot be inherited\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitAbstractStaticClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "static сlass cannot be abstract");
    }

    public void emitNonStaticHasInstancesError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("сlass '" + className + "' must be static to have instances\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitStaticHasNoInstancesError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("static сlass '" + className + "' should have instances\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitParamCountError(LsfLogicsParser parser, LP<?> property, int paramCount) throws SemanticErrorException {
        int interfacesCount = property.property.interfaces.size();
        emitParamCountError(parser, interfacesCount, paramCount);
    }

    public void emitParamCountError(LsfLogicsParser parser, int interfacesCount, int paramCount) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText(String.valueOf(interfacesCount) + " parameter(s) expected, " +
                                                     String.valueOf(paramCount) + " provided\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitConstraintPropertyAlwaysNullError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "constrained property is always NULL");
    }

    public void emitPropertyAlwaysNullError(LsfLogicsParser parser, String propertyName) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("property '" + propertyName +  "' is always NULL\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitAlreadyDefinedError(LsfLogicsParser parser, String type, String name) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText(type + " '" + name + "' was already defined\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitNamedParamsError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "number of named parameters should be equal to actual number of parameters");
    }

    public void emitUnionPropParamsError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "parameters of union property should all have same number of arguments");
    }

    public void emitFormulaReturnClassError(LsfLogicsParser parser) throws SemanticErrorException {
        emitSimpleError(parser, "formula return class must be a built-in class");
    }

    private void emitSimpleError(LsfLogicsParser parser, String message) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText(message + "\n", parser, e);
        emitSemanticError(msg, e);
    }
}
