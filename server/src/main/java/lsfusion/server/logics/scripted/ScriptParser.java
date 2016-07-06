package lsfusion.server.logics.scripted;

import lsfusion.server.LsfLogicsLexer;
import lsfusion.server.LsfLogicsParser;
import lsfusion.server.logics.debug.DebugInfo;
import org.antlr.runtime.*;

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

    /** Количество строк в развернутых метакодах во всем файле выше. Используется для определения номера строки
     * в файле с развернутыми метакодами в случае, если на самом деле метакоды свернуты      */
    private int globalExpandedLines = 0;
    
    /** Количество строк в развернутых полностью метакодах выше по стеку. Например, при использовании нескольких 
     * метакодов на одном уровне. Считается внутри метакода, если мы сейчас не внутри метакода, то равно 0  */ 
    private int currentExpandedLines = 0;
    
    /** Суммируются относительные позиции развернутых метакодов выше по стеку. Определяется общий сдвиг парсящихся 
     * сейчас метакодов  */
    private int currentExpansionLine = 0;
    
    private ScriptingErrorLog errLog;

    /** Определяет парсится ли сейчас код "развернутого" в IDE метакода. Проверяется, чтобы задать специальный state парсера, 
     * чтобы ничего не выполнялось при этом парсинге. */
    private boolean insideGeneratedMeta = false;
    
    /** Определяет интерпретируется ли сейчас код сформированный "неразвернутым" в IDE метакодом. Если равен true, 
     * то не нужно создавать делегаты для отладчика */
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
        currentExpandedLines = 0;
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

        //lineNumber is 1-based
        currentExpansionLine += lineNumber - 1;
        
        ParserInfo lastParser = new ParserInfo(parser, metaCode, callString, lineNumber);
        
        if (!enabledMeta && parsers.size() == 1) {
            insideNonEnabledMeta = true;
        }
        
        parsers.push(lastParser);
        parser.metaCodeParsingStatement();
        parsers.pop();

        int codeLinesCnt = 0;
        if (!insideGeneratedMeta && parser.parseState == State.PROP) {
            codeLinesCnt = linesCount(code);
            globalExpandedLines += codeLinesCnt - 1;
        }
        
        if (parsers.size() == 1) {
            currentExpandedLines = 0;
        } else if (!insideGeneratedMeta && parser.parseState == State.PROP) {
            currentExpandedLines += codeLinesCnt - 1; 
        }

        if (!enabledMeta && parsers.size() == 1) {
            insideNonEnabledMeta = false;
        }

        currentExpansionLine -= lineNumber - 1;
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

    public List<String> grabJavaCode() throws ScriptingErrorLog.SemanticErrorException {
        List<String> code = new ArrayList<>();
        Parser curParser = getCurrentParser();
        while (!curParser.input.LT(1).getText().equals("}>")) {
            if (curParser.input.LT(1).getType() == LsfLogicsParser.EOF) {
                errLog.emitJavaCodeNotEndedError(this);
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

    public DebugInfo getGlobalDebugInfo(String moduleName, boolean previous) {
        return new DebugInfo(new DebugInfo.DebugPoint(moduleName, getGlobalCurrentLineNumber(previous), getGlobalPositionInLine(previous), isInsideNonEnabledMeta()));
    }
    
    //0-based
    public int getGlobalCurrentLineNumber(boolean previous) {
        if (isInsideNonEnabledMeta()) {
            return globalExpandedLines + currentExpansionLine + getCurrentParserLineNumber(previous) - 1;
        } else {
            return currentExpandedLines + currentExpansionLine + getCurrentParserLineNumber(previous) - 1;
        }
    }

    public int getCurrentParserLineNumber() {
        return getCurrentParserLineNumber(false);
    }
    
    private int getCurrentParserLineNumber(boolean previous) {
        return getLineNumber(parsers.lastElement().getParser(), previous);
    }

    private int getLineNumber(Parser parser, boolean previous) {
        Token token = getToken(parser, previous);
        return token.getLine();
    }


    public int getGlobalPositionInLine(boolean previous) {
        return getCurrentParserPositionInLine(previous);
    }

    public int getCurrentParserPositionInLine() {
        return getCurrentParserPositionInLine(false);
    }
    
    private int getCurrentParserPositionInLine(boolean previous) {
        return getPositionInLine(parsers.lastElement().getParser(), previous);
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
