package platform.server.logics;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import platform.server.LsfLogicsParser;
import platform.server.logics.linear.LP;

import java.io.StringWriter;

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
    private final String moduleName;

    public ScriptingErrorLog(String moduleName) {
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

    public void emitClassIsNotCustomClassError(LsfLogicsParser parser, String className) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("сlass '" + className + "' should not be a built-in class\n", parser, e);
        emitSemanticError(msg, e);
    }

    public void emitParamCountError(LsfLogicsParser parser, LP<?> property, int paramCount) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        int interfacesCount = property.property.interfaces.size();
        String msg = getSemanticRecognitionErrorText(String.valueOf(interfacesCount) + " parameter(s) expected, " +
                String.valueOf(paramCount) + " provided\n", parser, e);
        emitSemanticError(msg, e);
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

    public void emitUnionPropParamsError(LsfLogicsParser parser) throws SemanticErrorException {
        SemanticErrorException e = new SemanticErrorException(parser.input);
        String msg = getSemanticRecognitionErrorText("parameters of union property should all have same number of arguments\n", parser, e);
        emitSemanticError(msg, e);
    }
}
