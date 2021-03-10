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
    public List<Pair<String, Boolean>> tokens;
    private String canonicalName;
    private String moduleName;
    private int lineNumber;

    public MetaCodeFragment(String canonicalName, List<String> params, List<Pair<String, Boolean>> tokens, String moduleName, int lineNumber) {
        this.parameters = params;
        this.tokens = tokens;
        this.moduleName = moduleName;
        this.lineNumber = lineNumber;
        this.canonicalName = canonicalName;
    }

    public String getCode(List<String> params, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) {
        assert params.size() == parameters.size();

        StringBuilder transformedCode = new StringBuilder();
        for (Pair<String, Boolean> token : tokens) {
            String resultToken = token.first;
            if(token.second) // it is id or string literal
                resultToken = transformToken(params, resultToken, getIdFromReversedI18NDictionary, appendEntry);
            transformedCode.append(resultToken);
        }
        return transformedCode.toString();
    }

    private String transformParamToken(List<String> actualParams, String token) {
        int index = parameters.indexOf(token);
        return index >= 0 ? actualParams.get(index) : token;
    }
    
    private String transformToken(List<String> actualParams, String token, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) {
        if (!token.contains("##")) // optimization;
            return transformParamToken(actualParams, token);
        
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
