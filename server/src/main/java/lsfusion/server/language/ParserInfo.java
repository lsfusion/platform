package lsfusion.server.language;

import lsfusion.server.language.metacode.MetaCodeFragment;
import org.antlr.runtime.Parser;

public class ParserInfo {
    private LsfLogicsParser parser;
    // Информация об объявлении метакода (META metaCode), который мы сейчас парсим   
    private String metacodeDefinitionModuleName;
    private Integer metacodeDefinitionLineNumber;
    
    // Информация об использовании метакода (@metaCode), который мы парсим
    private String metacodeCallStr;
    private int lineNumber;

    public ParserInfo(LsfLogicsParser parser, Integer metacodeDefinitionLineNumber, String metacodeDefinitionModuleName, String metacodeCallStr, int lineNumber) {
        this.parser = parser;

        this.metacodeDefinitionLineNumber = metacodeDefinitionLineNumber;
        this.metacodeDefinitionModuleName = metacodeDefinitionModuleName;

        this.metacodeCallStr = metacodeCallStr;
        this.lineNumber = lineNumber;
    }

    public LsfLogicsParser getParser() {
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

