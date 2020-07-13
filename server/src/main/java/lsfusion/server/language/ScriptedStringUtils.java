package lsfusion.server.language;

import lsfusion.base.Pair;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ScriptedStringUtils {
    public static class TransformationError extends Exception {
        public TransformationError(String text) {
            super(text);
        }
    }
    
    private static final String wrongEscapeSeqFormat = "wrong escape sequence: '\\%s'";
    
    public static String transformStringLiteral(String s) throws TransformationError {
        if (s == null) {
            return null;
        }
        
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                if (i+2 == s.length()) {
                    throw new TransformationError("wrong escape sequence at the end of the string");
                }
                
                char nextCh = s.charAt(i+1);
                switch (nextCh) {
                    case '\\': b.append('\\'); break;
                    case '\'': b.append('\''); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case '{': case '}': {
                        throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh) + ", literal has no localization");    
                    }
                    default: {
                        throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh));
                    }
                }
                ++i;
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();
    }

    public static String transformLocalizedStringLiteralToSourceString(String s) throws TransformationError {
        if (s == null) {
            return null;
        }
        
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                if (i+2 == s.length()) {
                    throw new TransformationError("wrong escape sequence at the end of the string");
                }

                char nextCh = s.charAt(i+1);
                switch (nextCh) {
                    case '\\': b.append("\\\\"); break;
                    case '\'': b.append('\''); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case '{': b.append("\\{"); break;
                    case '}': b.append("\\}"); break;
                    default: {
                        throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh));
                    }
                }
                ++i;
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();        
    }
    
    public static LocalizedString transformLocalizedStringLiteral(String s, BusinessLogics BL) throws TransformationError, LocalizedString.FormatError {
        String sourceString = transformLocalizedStringLiteralToSourceString(s);
        
        if (sourceString != null) {
            LocalizedString.checkLocalizedStringFormat(sourceString);
            if (LocalizedString.canBeOptimized(sourceString)) {
                if (System.getProperty("generateBundleFile") != null) {
                    addToResourceBundle(sourceString, BL);
                }
                
                Pair<Integer, Integer> spaces = getSpaces(sourceString);
                if (spaces.first + spaces.second < sourceString.length()) {
                    String translateId = BL.getReversedI18nDictionary().getValue(sourceString.trim());
                    if (translateId != null) {
                        sourceString = setSpaces(LocalizedString.OPEN_CH + translateId + LocalizedString.CLOSE_CH, spaces);
                        return LocalizedString.create(sourceString, true);
                    }
                }
            }
        }
        
        return LocalizedString.create(sourceString);
    }

    private static Pair<Integer, Integer> getSpaces(String s) {
        int leadingSpaces = 0;
        while (leadingSpaces < s.length() && s.charAt(leadingSpaces) == ' ') {
            ++leadingSpaces;
        }

        int trailingSpaces = 0;
        while (trailingSpaces < s.length() && s.charAt(s.length() - trailingSpaces - 1) == ' ') {
            ++trailingSpaces;
        }
        return new Pair<>(leadingSpaces, trailingSpaces);
    }
    
    private static String setSpaces(String s, Pair<Integer, Integer> spaces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces.first; ++i) {
            builder.append(' ');
        }
        builder.append(s);
        for (int i = 0; i < spaces.second; ++i) {
            builder.append(' ');
        }
        return builder.toString();
    }
    
    private static void addToResourceBundle(String s, BusinessLogics BL) {
        if (hasCyrillicChars(s)) {
            BL.getResourceBundleGenerator().appendEntry(s.trim());
        }
    }
    
    private static boolean hasCyrillicChars(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (Character.UnicodeBlock.of(s.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
                return true;
            }
        }
        return false;
    }
}
