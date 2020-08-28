package lsfusion.server.language.metacode;

import lsfusion.base.Pair;
import lsfusion.server.language.ScriptedStringUtils;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static lsfusion.server.language.ScriptedStringUtils.*;
import static lsfusion.server.physics.dev.i18n.LocalizedString.CLOSE_CH;
import static lsfusion.server.physics.dev.i18n.LocalizedString.OPEN_CH;

public class MetaCodeFragment {
    public List<String> parameters;
    public List<String> tokens;
    public List<Pair<Integer,Boolean>> metaTokens;
    private String canonicalName;
    private String code;
    private String moduleName;
    private int lineNumber;

    public MetaCodeFragment(String canonicalName, List<String> params, List<String> tokens, List<Pair<Integer,Boolean>> metaTokens, String code, String moduleName, int lineNumber) {
        this.parameters = params;
        this.tokens = tokens;
        this.metaTokens = metaTokens;
        this.code = code;
        this.moduleName = moduleName;
        this.lineNumber = lineNumber;
        this.canonicalName = canonicalName;
    }

    public String getCode(List<String> params, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) {
        assert params.size() == parameters.size();
        ArrayList<String> newTokens = new ArrayList<>(tokens);
        for (Pair<Integer, Boolean> metaToken : metaTokens) {
            Integer metaIndex = metaToken.first;
            newTokens.set(metaIndex, transformToken(params, tokens.get(metaIndex), metaToken.second, getIdFromReversedI18NDictionary, appendEntry));
        }

        return getTransformedCode(newTokens, tokens, code);
    }

    private int getUncommentedIndexOf(String code, String token, int startPos) {
        while (true) {
            int tokenPos = code.indexOf(token, startPos);
            int nearestSlashesPos = code.lastIndexOf("//", tokenPos);
            if (nearestSlashesPos == -1 || nearestSlashesPos < startPos || code.lastIndexOf('\n', tokenPos) > nearestSlashesPos) {
                return tokenPos;
            }
            startPos = tokenPos + 1;
        }
    }

    private String getTransformedCode(ArrayList<String> newTokens, List<String> oldTokens, String code) {
        StringBuilder transformedCode = new StringBuilder();
        int codePos = getUncommentedIndexOf(code, ")", 0) + 1;
        transformedCode.append(code, 0, codePos);

        for (int i = 0; i < newTokens.size(); i++) {
            int tokenStartPos = getUncommentedIndexOf(code, oldTokens.get(i), codePos);
            transformedCode.append(code, codePos, tokenStartPos);
            codePos = tokenStartPos + oldTokens.get(i).length();
            transformedCode.append(newTokens.get(i));
        }
        transformedCode.append(code.substring(codePos));
        return transformedCode.toString();
    }

    private String transformParamToken(List<String> actualParams, String token) {
        int index = parameters.indexOf(token);
        return index >= 0 ? actualParams.get(index) : token;
    }
    
    private String transformToken(List<String> actualParams, String token, boolean isMeta, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) {
        if (!isMeta) { // optimization;
            assert !token.contains("##");
            return transformParamToken(actualParams, token);
        }
        
        String[] parts = token.split("##");
        boolean isStringLiteral = false;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            boolean capitalize = false;
            if (part.startsWith("#")) {
                assert i > 0;
                capitalize = (result.length() > 0) || (i == 1 && parts[0].isEmpty()); // when there is ###param, forcing its capitalization
                part = part.substring(1);
            }

            part = transformParamToken(actualParams, part);

            if (!part.isEmpty() && part.charAt(0) == QUOTE) {
                isStringLiteral = true;
                if (getIdFromReversedI18NDictionary != null && ScriptedStringUtils.hasNoLocalizationFeatures(part, true)) {
                    part = transformLiteralForReverseI18N(part, capitalize, getIdFromReversedI18NDictionary, appendEntry);
                } 
                part = unquote(part);
            }
            
            result.append(ScriptedStringUtils.capitalizeIfNeeded(part, capitalize));
        }
        
        if (isStringLiteral)
            result = new StringBuilder(quote(result.toString()));
        
        return result.toString();
    }

    // We don't know if the literal is localized or not at the moment. Thus, we transform the literal into the 
    // intermediate format if it can be found in the reverse dictionary.
    // This format is {{ {id}  }literal}, where { {id}  } part is the dictionary id with possible leading and trailing spaces
    // Without spaces the fromat is {{{id}}literal}
    private String transformLiteralForReverseI18N(String part, boolean capitalize, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) {
        try {
            String propertyFileValue = ScriptedStringUtils.transformAnyStringLiteralToPropertyFileValue(part);
            propertyFileValue = ScriptedStringUtils.capitalizeIfNeeded(propertyFileValue, capitalize);
            String translateId = null;
            if (System.getProperty("generateBundleFile") != null) {
                ScriptedStringUtils.addToResourceBundle(propertyFileValue, appendEntry);
                // For the purpose of bundle generation we need to transform literal into the intermediate form even if
                // it was not found in the dictionary (because we need to generate the bundle before)
                translateId = "id";
            }
        
            Pair<Integer, Integer> spaces = ScriptedStringUtils.getSpaces(propertyFileValue);
            if (spaces.first + spaces.second < propertyFileValue.length()) {
                String dictionaryId = getIdFromReversedI18NDictionary.apply(propertyFileValue.trim());
                if (dictionaryId != null) {
                    translateId = dictionaryId;
                }
                if (translateId != null) {
                    String i18nPart = ScriptedStringUtils.setSpaces(OPEN_CH + translateId + CLOSE_CH, spaces);
                    return quote(OPEN_CH + "" + OPEN_CH + i18nPart + CLOSE_CH + ScriptedStringUtils.capitalizeIfNeeded(unquote(part), capitalize) + CLOSE_CH);  
                }
            }
        } catch (ScriptedStringUtils.TransformationError transformationError) {
            transformationError.printStackTrace();
        }
        return part;
    }

    public String getModuleName() {
        return moduleName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<String> getParameters() {
        return parameters;
    }

    static public String metaCodeCallString(String name, MetaCodeFragment metaCode, List<String> actualParams) {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(name);
        builder.append("(");
        for (int i = 0; i < actualParams.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(metaCode.getParameters().get(i));
            builder.append("=");
            builder.append(actualParams.get(i));
        }
        builder.append(")");
        return builder.toString();
    }

    public String getCanonicalName() {
        return canonicalName;
    }
    
    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }
}
