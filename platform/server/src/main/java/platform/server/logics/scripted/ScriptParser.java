package platform.server.logics.scripted;

import org.antlr.runtime.*;
import platform.server.LsfLogicsLexer;
import platform.server.LsfLogicsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * User: DAle
 * Date: 04.09.12
 * Time: 15:19
 */

public class ScriptParser {
    public enum State {PRE, INIT, GROUP, CLASS, PROP, TABLE, INDEX}

    private State currentState = null;
    private Stack<ParserInfo> parsers = new Stack<ParserInfo>();
    private ScriptingErrorLog errLog;

    public ScriptParser(ScriptingErrorLog errLog) {
        this.errLog = errLog;
    }

    public void initParseStep(ScriptingLogicsModule LM, CharStream stream, State state) throws RecognitionException {
        LsfLogicsLexer lexer = new LsfLogicsLexer(stream);
        LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));

        parser.self = LM;
        parser.parseState = state;

        lexer.self = LM;
        lexer.parseState = state;

        currentState = state;
        parsers.push(new ParserInfo(parser, null, null, 0));
        if (state == State.PRE) {
            parser.moduleHeader();
        } else {
            parser.script();
        }
        parsers.pop();
        currentState = null;
    }

    public void runMetaCode(ScriptingLogicsModule LM, String code, MetaCodeFragment metaCode, String callString, int lineNumber) throws RecognitionException {
        LsfLogicsLexer lexer = new LsfLogicsLexer(new ANTLRStringStream(code));
        LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));

        lexer.self = LM;
        lexer.parseState = currentState;

        parser.self = LM;
        parser.parseState = currentState;

        parsers.push(new ParserInfo(parser, metaCode, callString, lineNumber));
        parser.metaCodeParsingStatement();
        parsers.pop();
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        if (isInsideMetacode()) {
            errLog.emitMetacodeInsideMetacodeError(this);
        }

        List<String> code = new ArrayList<String>();
        Parser curParser = getCurrentParser();
        while (!curParser.input.LT(1).getText().equals("END")) {
            if (curParser.input.LT(1).getType() == LsfLogicsParser.EOF) {
                errLog.emitMetaCodeNotEndedError(this, metaCodeName);
            }
            String token = curParser.input.LT(1).getText();
            code.add(token);
            curParser.input.consume();
        }
        return code;
    }

    public boolean isInsideMetacode() {
        return parsers.size() > 1;
    }

    public Parser getCurrentParser() {
        if (parsers.empty()) {
            return null;
        } else {
            return parsers.peek().getParser();
        }
    }

    public boolean semicolonNeeded() {
        return !("}".equals(getCurrentParser().input.LT(-1).getText()));
    }

    public String getCurrentScriptPath(String moduleName, int lineNumber, String separator) {
        StringBuilder path = new StringBuilder();

        for (int i = 0; i < parsers.size(); i++) {
            path.append((i == 0 ? moduleName : parsers.get(i).getMetacodeDefinitionModuleName()));
            path.append(":");

            int curLineNumber = lineNumber;
            if (i+1 < parsers.size()) {
                curLineNumber = parsers.get(i+1).getLineNumber();
            }
            if (i > 0) {
                curLineNumber += parsers.get(i).getMetacodeDefinitionLineNumber() - 1;
            }

            path.append(curLineNumber);
            if (i+1 < parsers.size()) {
                path.append(":");
                path.append("\t");
                path.append(parsers.get(i+1).getMetacodeCallStr());
                path.append(separator);
            }
        }
        return path.toString();
    }

    public int getCurrentParserLineNumber() {
        return getLineNumber(parsers.lastElement().getParser());
    }

    private int getLineNumber(Parser parser) {
        Token token = getToken(parser);
        return token.getLine();
    }

    private int getPositionInLine(Parser parser) {
        Token token = getToken(parser);
        return token.getCharPositionInLine();
    }

    private Token getToken(Parser parser) {
        Token token = parser.input.LT(1);
        if (token.getType() == Token.EOF) {
            token = parser.input.LT(-1);
        }
        return token;
    }
}
