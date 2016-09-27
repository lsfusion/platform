package lsfusion.server.logics.scripted;

import lsfusion.server.logics.i18n.LocalizedString;

public class ScriptedStringUtils {
    public static class TransformationError extends Exception {
        public TransformationError(String text) {
            super(text);
        }
    }
    
    private static final String wrongEscapeSeqFormat = "wrong escape sequence: '\\%s'";
    
    public static String transformStringLiteral(String s) throws TransformationError {
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                assert i+2 < s.length();
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

    public static LocalizedString transformLocalizedStringLiteral(String s) throws TransformationError, LocalizedString.FormatError {
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                assert i+2 < s.length();
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
        return LocalizedString.createChecked(b.toString());
    }
}
