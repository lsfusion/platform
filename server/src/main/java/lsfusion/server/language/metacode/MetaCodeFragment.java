package lsfusion.server.language.metacode;

import lsfusion.base.Pair;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MetaCodeFragment {
    public List<String> parameters;
    public List<String> tokens;
    public List<Pair<Integer,Boolean>> metaTokens;
    private String canonicalName;
    private String code;
    private String moduleName;
    private int lineNumber;

    private final char QUOTE = '\'';

    public MetaCodeFragment(String canonicalName, List<String> params, List<String> tokens, List<Pair<Integer,Boolean>> metaTokens, String code, String moduleName, int lineNumber) {
        this.parameters = params;
        this.tokens = tokens;
        this.metaTokens = metaTokens;
        this.code = code;
        this.moduleName = moduleName;
        this.lineNumber = lineNumber;
        this.canonicalName = canonicalName;
    }

    public String getCode(List<String> params) {
        assert params.size() == parameters.size();
        ArrayList<String> newTokens = new ArrayList<>(tokens);
        for (Pair<Integer, Boolean> metaToken : metaTokens) {
            Integer metaIndex = metaToken.first;
            newTokens.set(metaIndex, transformToken(params, tokens.get(metaIndex), metaToken.second));
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
    private String transformToken(List<String> actualParams, String token, boolean isMeta) {
        if(!isMeta) { // optimization;
            assert !token.contains("##");
            return transformParamToken(actualParams, token);
        }
        String[] parts = token.split("##");
        boolean isStringLiteral = false;
        String result = "";
        boolean firstPartIsNotEmpty = false;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            boolean capitalize = false;
            if (part.startsWith("#")) {
                assert i > 0;
                capitalize = !result.isEmpty() || (i == 1 && parts[0].isEmpty()); // when there is ###param, forcing its capitalization
                part = part.substring(1);
            }

            part = transformParamToken(actualParams, part);

            if (!part.isEmpty() && part.charAt(0) == QUOTE) {
                isStringLiteral = true;
                part = unquote(part);
            }
            
            result += capitalize(part, capitalize);
        }
        
        if(isStringLiteral)
            result = QUOTE + result + QUOTE;
        
        return result;
    }

    private String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == QUOTE && s.charAt(s.length()-1) == QUOTE) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

    private String capitalize(String s, boolean toCapitalize) {
        if (toCapitalize && s.length() > 0) {
            s = StringUtils.capitalize(s);
        }
        return s;
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
