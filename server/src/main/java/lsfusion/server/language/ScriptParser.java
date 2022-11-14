package lsfusion.server.language;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.antlr.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class ScriptParser {
    public enum State {PRE, META_CLASS_TABLE, MAIN, METADECL, GENMETA}

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
    
    private boolean insideMetaDecl = false;
    private int prevMetaToken;
    private List<Pair<String, Boolean>> metaTokens;

    /** Определяет интерпретируется ли сейчас код сформированный "неразвернутым" в IDE метакодом. Если равен true, 
     * то не нужно создавать делегаты для отладчика */
    private boolean insideNonEnabledMeta = false;

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
        parsers.push(new ParserInfo(parser, null, null,null, 0));
        try {
            if (state == State.PRE) {
                parser.moduleHeader();
            } else {
                parser.script();
            }
        } catch (Throwable t) {
            ExecutionStackAspect.setExceptionStackString("Error during parsing at : " + parser.getCurrentDebugPoint());
            throw ExceptionUtils.propagate(t, RecognitionException.class);
        }
        parsers.pop();
        currentState = null;
    }

    public void runMetaCode(ScriptingLogicsModule LM, String code, int metaLineNumber, String metaModuleName, String callString, 
                            int lineNumberBefore, int lineNumberAfter, boolean enabledMeta) throws RecognitionException {
        runCode(LM, code, metaLineNumber, metaModuleName, callString, lineNumberBefore, parser -> {
            try {
                assert !insideMetaDecl;

                //lineNumber is 1-based
                currentExpansionLine += lineNumberAfter - 1;

                boolean isTopParser = parsers.size() == 2; // for meta decl parsing it doesn't matter; 2 because we already pushed lastParser
                boolean needOffset = parser.inMainParseState(); // in theory we might also need offset in class step

                if (!enabledMeta && isTopParser) {
                    insideNonEnabledMeta = true;
                }

                parser.metaCodeParsingStatement();

                int codeLinesCnt = 0;
                if (needOffset) {
                    codeLinesCnt = linesCount(code);
                    globalExpandedLines += codeLinesCnt - 1;
                }

                if (isTopParser) {
                    currentExpandedLines = 0;
                } else if (needOffset) {
                    currentExpandedLines += codeLinesCnt - 1;
                }

                if (!enabledMeta && isTopParser) {
                    insideNonEnabledMeta = false;
                }

                currentExpansionLine -= lineNumberAfter - 1;

            } catch (RecognitionException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    public ScriptingLogicsModule.LPWithParams runStringInterpolateCode(ScriptingLogicsModule LM, String code, String moduleName, int lineNumber, List<ScriptingLogicsModule.TypedParameter> context, boolean dynamic) {
        Result<ScriptingLogicsModule.LPWithParams> result = new Result();

        runCode(LM, code, 0, moduleName, "", lineNumber, parser -> {
            try {
                result.set(parser.propertyExpression(context, dynamic));
            } catch (RecognitionException e) {
                throw Throwables.propagate(e);
            }
        });

        return result.result;
    }

    public void runCode(ScriptingLogicsModule LM ,String code, int metaLineNumber, String metaModuleName, String callString,
                            int lineNumberBefore, Consumer<LsfLogicsParser> consumer) {
        LsfLogicsLexer lexer = new LsfLogicsLexer(new ANTLRStringStream(code));
        LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));

        lexer.self = LM;
        lexer.parseState = currentState;

        parser.self = LM;
        parser.parseState = currentState;

        ParserInfo lastParser = new ParserInfo(parser, metaLineNumber, metaModuleName, callString, lineNumberBefore);

        parsers.push(lastParser);
        consumer.accept(parser);
        parsers.pop();
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

    public boolean isInsideMetaDecl() {
        return insideMetaDecl;
    }

    public void enterMetaDeclState() {
        insideMetaDecl = true;
        metaTokens = new ArrayList<>();

        markMetaDeclCode(")");
    }
    public void grabMetaDeclCode() {
        if(!insideMetaDecl)
            return;

        Parser curParser = getCurrentParser();
        int newToken = curParser.input.index();
        for(int i = prevMetaToken; i < newToken; i++) {
            Token lt = curParser.input.get(i);
            int type = lt.getType();
            metaTokens.add(new Pair<>(lt.getText(), type == LsfLogicsParser.STRING_LITERAL || type == LsfLogicsParser.ID));
        }
        prevMetaToken = newToken;
    }
    public void skipMetaDeclCode() {
        if(!insideMetaDecl)
            return;

        markMetaDeclCode("}");
    }
    private void markMetaDeclCode(String prevToken) {
        Token braceToken = getCurrentParser().input.LT(-1);
        assert braceToken.getText().equals(prevToken);
        prevMetaToken = braceToken.getTokenIndex() + 1; // we want to include spaces / comments after prevToken
    }

    public List<Pair<String, Boolean>> leaveMetaDeclState() {
        assert insideMetaDecl;

        grabMetaDeclCode();
        
        List<Pair<String, Boolean>> result = metaTokens;

        metaTokens = null;
        insideMetaDecl = false;

        return result;
    }

    public boolean isInsideMetacode() {
        return parsers.size() > 1;
    }

    public LsfLogicsParser getCurrentParser() {
        return getCurrentParserInfo().getParser();
    }

    public ParserInfo getCurrentParserInfo() {
        return parsers.lastElement();
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

    public DebugInfo.DebugPoint getGlobalDebugPoint(String moduleName, String path, boolean previous) {
        return getGlobalDebugPoint(moduleName, path, previous, null, null);
    }

    public DebugInfo.DebugPoint getGlobalDebugPoint(String moduleName, String path, boolean previous, String topName, LocalizedString topCaption) {
        Token token = getToken(getCurrentParserInfo().getParser(), previous);

        boolean insideNonEnabledMeta = isInsideNonEnabledMeta();

        int globalCurrentLineNumber = (insideNonEnabledMeta ? globalExpandedLines : currentExpandedLines) + currentExpansionLine + token.getLine() - 1;
        int globalPositionInLine = token.getCharPositionInLine();

        int globalExpandedCurrentLineNumber = globalExpandedLines + currentExpansionLine + token.getLine() - 1;

        return new DebugInfo.DebugPoint(moduleName, path, globalCurrentLineNumber, globalPositionInLine, insideNonEnabledMeta, globalExpandedCurrentLineNumber, topName, topCaption);
    }

    public int getCurrentParserLineNumber() {
        Token token = getToken(getCurrentParserInfo().getParser(), false);

        return token.getLine();
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
