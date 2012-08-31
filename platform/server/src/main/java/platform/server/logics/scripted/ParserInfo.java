package platform.server.logics.scripted;

import org.antlr.runtime.Parser;

/**
 * User: DAle
 * Date: 29.08.12
 * Time: 15:21
 */

public class ParserInfo {
    private Parser parser;
    private String metacodeDefinitionModuleName = null;
    private int metacodeDefinitionLineNumber;
    private String metacodeCallStr;
    private int lineNumber;

    public ParserInfo(Parser parser, MetaCodeFragment metaCode, String metacodeCallStr, int lineNumber) {
        this.parser = parser;
        if (metaCode != null) {
            this.metacodeDefinitionLineNumber = metaCode.getLineNumber();
            this.metacodeDefinitionModuleName = metaCode.getModuleName();
        }
        this.metacodeCallStr = metacodeCallStr;
        this.lineNumber = lineNumber;
    }

    public Parser getParser() {
        return parser;
    }

    public String getMetacodeCallStr() {
        return metacodeCallStr;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMetacodeDefinitionModuleName() {
        return metacodeDefinitionModuleName;
    }

    public int getMetacodeDefinitionLineNumber() {
        return metacodeDefinitionLineNumber;
    }
}

