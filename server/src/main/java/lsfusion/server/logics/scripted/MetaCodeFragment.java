package lsfusion.server.logics.scripted;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * User: DAle
 * Date: 29.08.12
 * Time: 18:42
 */

public class MetaCodeFragment {
    public List<String> parameters;
    public List<String> tokens;
    private String code;
    private String moduleName;
    private int lineNumber;

    private char QUOTE = '\'';

    public MetaCodeFragment(List<String> params, List<String> tokens, String code, String moduleName, int lineNumber) {
        this.parameters = params;
        this.tokens = tokens;
        this.code = code;
        this.moduleName = moduleName;
        this.lineNumber = lineNumber;
    }

    public String getCode(List<String> params) {
        assert params.size() == parameters.size();
        ArrayList<String> newTokens = new ArrayList<String>();
        ArrayList<Integer> oldTokensCnt = new ArrayList<Integer>();

        for (int i = 0; i < tokens.size(); i++) {
            String tokenStr = transformedToken(params, tokens.get(i));
            if (tokenStr.equals("##") || tokenStr.equals("###")) {
                if (!newTokens.isEmpty() && i+1 < tokens.size()) {
                    String lastToken = newTokens.get(newTokens.size()-1);
                    String nextToken = transformedToken(params, tokens.get(i+1));
                    newTokens.set(newTokens.size()-1, concatTokens(lastToken, nextToken, tokenStr.equals("###")));
                    oldTokensCnt.set(oldTokensCnt.size()-1, oldTokensCnt.get(oldTokensCnt.size()-1) + 2);
                    ++i;
                }
            } else {
                newTokens.add(tokenStr);
                oldTokensCnt.add(1);
            }
        }

        return getTransformedCode(newTokens, tokens, oldTokensCnt, code);
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

    private String getTransformedCode(ArrayList<String> newTokens, List<String> oldTokens, ArrayList<Integer> oldTokensCnt, String code) {
        String whitespaces = "";
        StringBuilder transformedCode = new StringBuilder();
        int codePos = getUncommentedIndexOf(code, ")", 0) + 1;
        transformedCode.append(code.substring(0, codePos));
        int oldTokenIndex = 0;

        for (int i = 0; i < newTokens.size(); i++) {
            for (int j = 0; j < oldTokensCnt.get(i); j++) {
                int tokenStartPos = getUncommentedIndexOf(code, oldTokens.get(oldTokenIndex + j), codePos);
                whitespaces = whitespaces + code.substring(codePos, tokenStartPos);
                if (j == 0) {
                    transformedCode.append(whitespaces);
                    whitespaces = "";
                }
                codePos = tokenStartPos + oldTokens.get(oldTokenIndex + j).length();
            }
            oldTokenIndex += oldTokensCnt.get(i);
            transformedCode.append(newTokens.get(i));
        }
        transformedCode.append(whitespaces);
        transformedCode.append(code.substring(codePos));
        return transformedCode.toString();
    }

    private String transformedToken(List<String> actualParams, String token) {
        int index = parameters.indexOf(token);
        return index >= 0 ? actualParams.get(index) : token;
    }

    private String concatTokens(String t1, String t2, boolean toCapitalize) {
        if (t1.isEmpty() || t2.isEmpty()) {
            return t1 + capitalize(t2, toCapitalize && !t1.isEmpty());
        } else if (t1.charAt(0) == QUOTE || t2.charAt(0) == QUOTE) {
            return QUOTE + unquote(t1) + capitalize(unquote(t2), toCapitalize) + QUOTE;
        } else {
            return t1 + capitalize(t2, toCapitalize);
        }
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
}
