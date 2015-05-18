package lsfusion.server.logics.scripted;

import org.antlr.runtime.*;
import lsfusion.server.LsfLogicsLexer;
import lsfusion.server.LsfLogicsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * User: DAle
 * Date: 04.09.12
 * Time: 15:19
 */

public class ScriptParser {
    public enum State {PRE, INIT, GROUP, CLASS, PROP, TABLE, INDEX, GENMETA}

    private State currentState = null;
    private Stack<ParserInfo> parsers = new Stack<>();
    
    private int globalExpandedLines = 0;
    private int globalExpansionLine = 0;
    
    private ScriptingErrorLog errLog;

    // Определяет парсится ли сейчас код "развернутого" в IDE метакода. Проверяется, чтобы задать специальный state парсера, чтобы ничего не выполнялось при этом парсинге. 
    private boolean insideGeneratedMeta = false;
    
    // Определяет интерпретируется ли сейчас код сформированный "неразвернутым" в IDE метакодом. Если равен true, то не нужно создавать делегаты для отладчика
    private boolean insideNonEnabledMeta = false;
    
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

        globalExpandedLines = 0;
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

    public void runMetaCode(ScriptingLogicsModule LM, String code, MetaCodeFragment metaCode, String callString, int lineNumber, boolean enabledMeta) throws RecognitionException {
        LsfLogicsLexer lexer = new LsfLogicsLexer(new ANTLRStringStream(code));
        LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));

        lexer.self = LM;
        lexer.parseState = currentState;

        parser.self = LM;
        parser.parseState = insideGeneratedMeta ? State.GENMETA : currentState;

        if (!insideNonEnabledMeta) {
            //lineNumber is 1-based
            globalExpansionLine += lineNumber - 1;
        }
        
        ParserInfo lastParser = new ParserInfo(parser, metaCode, callString, lineNumber);
        
        if (!enabledMeta && parsers.size() == 1) {
            insideNonEnabledMeta = true;
        }
        
        parsers.push(lastParser);
        parser.metaCodeParsingStatement();
        parsers.pop();

        if (parsers.size() == 1) {
            globalExpandedLines = 0;
        } else if (!insideGeneratedMeta && !insideNonEnabledMeta && parser.parseState == State.PROP) {
            globalExpandedLines += linesCount(code) - 1; 
        }

        if (!enabledMeta && parsers.size() == 1) {
            insideNonEnabledMeta = false;
        }

        if (!insideNonEnabledMeta) {
            globalExpansionLine -= lineNumber - 1;
        }
    }
    
    private int linesCount(String code) {
        int count = 1;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        if (isInsideMetacode()) {
            errLog.emitMetacodeInsideMetacodeError(this);
        }

        List<String> code = new ArrayList<>();
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

    public boolean enterGeneratedMetaState() {
        if (!insideGeneratedMeta && currentState != State.INIT) {
            insideGeneratedMeta = true;
            return true;
        }
        return false;
    }

    public void leaveGeneratedMetaState() {
        insideGeneratedMeta = false;
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

    public boolean isInsideNonEnabledMeta() {
        return insideNonEnabledMeta;
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

    //0-based
    public int getGlobalCurrentLineNumber() {
        return getGlobalCurrentLineNumber(false);
    }
    public int getGlobalCurrentLineNumber(boolean previous) {
        return globalExpandedLines + globalExpansionLine + getCurrentParserLineNumber(previous) - 1;
    }
    public int getGlobalPositionInLine() {
        return getGlobalPositionInLine(false);
    }
    public int getGlobalPositionInLine(boolean previous) {
        return getCurrentParserPositionInLine(previous);
    }

    public int getCurrentParserLineNumber() {
        return getCurrentParserLineNumber(false);
    }
    public int getCurrentParserLineNumber(boolean previous) {
        return getLineNumber(parsers.lastElement().getParser(), previous);
    }

    public int getCurrentParserPositionInLine() {
        return getCurrentParserPositionInLine(false);
    }
    public int getCurrentParserPositionInLine(boolean previous) {
        return getPositionInLine(parsers.lastElement().getParser(), previous);
    }

    private int getLineNumber(Parser parser, boolean previous) {
        Token token = getToken(parser, previous);
        return token.getLine();
    }

    private int getPositionInLine(Parser parser, boolean previous) {
        Token token = getToken(parser, previous);
        return token.getCharPositionInLine();
    }

    private Token getToken(Parser parser, boolean previous) {
        if (previous) {
            return parser.input.LT(-1);
        }
            
        Token token = parser.input.LT(1);
        if (token.getType() == Token.EOF) {
            token = parser.input.LT(-1);
        }
        return token;
    }
}
