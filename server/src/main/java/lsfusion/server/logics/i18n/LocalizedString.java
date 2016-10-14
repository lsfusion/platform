package lsfusion.server.logics.i18n;

import lsfusion.server.context.ThreadLocalContext;

import java.util.Locale;

/**
 *  Интернационализируемая строка.
 *  <br><br>
 *  Описание формата:<br>
 *  Каждая подстрока, которую нужно интернационализировать, должна быть указана с помощью идентификатора, заключенного
 *  в фигурные скобки. Идентификатор может содержать любые символы кроме разделителей (пробелы, табуляции, переводы строк).
 *  При необходимости включить в строку фигурную скобку, ее нужно экранировать с помощью обратного слэша. При необходимости 
 *  включить в строку обратный слэш, он должен экранироваться с помощью еще одного обратного слэша.
 *  Если идентификатор не будет найден, то в качестве результата будет принят сам идентификатор.
 *  <br><br>
 *  Пример строки в правильном формате:<br>
 *  "{main.firstID} - \\{Not an ID\\} {main.secondID}"    
 */


public class LocalizedString {
    public interface Localizer {
        String localize(String key, Locale locale);
    }

    public static final char OPEN_CH = '{';
    public static final char CLOSE_CH = '}';
    
    private final String source;
    private boolean needToBeLocalized;
    
    protected LocalizedString(String source) {
        this(source, needToBeLocalized(source));
    }

    public static boolean needToBeLocalized(String source) {
        return source.indexOf('{') != -1;
    }

    protected LocalizedString(String source, boolean needToBeLocalized) {
        assert source != null;
        this.source = source;
        this.needToBeLocalized = needToBeLocalized;
    }

    public boolean isNeedToBeLocalized() {
        return needToBeLocalized;
    }

    /** Предполагается, что getString вызывается для строки, удовлетворяющей checkLocalizedStringFormat,
     *  поэтому не производятся проверки на соответствие формату
     */
    public String getString(Locale locale, Localizer localizer) {
        if (!needToBeLocalized) {
            return getSourceString();
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch == '\\') {
                assert i+1 < source.length();
                builder.append(source.charAt(i+1));
                ++i;
            } else if (ch == OPEN_CH) {
                int closePos = source.indexOf(CLOSE_CH, i+1);
                assert closePos > 0;
                String localizedSubstr;
                if (localizer != null) {
                    localizedSubstr = localizer.localize(source.substring(i + 1, closePos), locale);
                } else {
                    localizedSubstr = source.substring(i + 1, closePos);
                }
                builder.append(localizedSubstr);
                i = closePos;
            } else {
                builder.append(source.charAt(i));
            }
        }
        return builder.toString();
    } 
    
    public String getSourceString() {
        return source;
    }

    public static class FormatError extends Exception {
        public FormatError(String text) {
            super(text);
        }
    }

    public static void checkLocalizedStringFormat(String s) throws FormatError {
        assert s != null;
        boolean insideKey = false;
        boolean keyIsEmpty = true;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                if (i + 1 == s.length()) {
                    throw new FormatError("wrong escape sequence at the end of the string");
                }
                char nextCh = s.charAt(i + 1);
                if (nextCh != '\\' && nextCh != OPEN_CH && nextCh != CLOSE_CH) {
                    throw new FormatError(String.format("wrong escape sequence: '%s'", nextCh));
                }
                if (insideKey) {
                    keyIsEmpty = false;
                }
                ++i;
            } else if (ch == CLOSE_CH) {
                if (!insideKey) {
                    throw new FormatError(String.format("invalid character '%c', should be escaped with '\\'", CLOSE_CH));
                } else if (keyIsEmpty) {
                    throw new FormatError("empty key is forbidden");
                } else {
                    insideKey = false;
                }
            } else if (ch == OPEN_CH) {
                if (insideKey) {
                    throw new FormatError(String.format("invalid character '%c', should be escaped with '\\'", OPEN_CH));
                } else {
                    insideKey = true;
                    keyIsEmpty = true;
                }
            } else if (insideKey && Character.isWhitespace(s.charAt(i))) {
                throw new FormatError("any whitespace is forbidden inside key");
            } else if (insideKey) {
                keyIsEmpty = false;
            }
        }
        if (insideKey) {
            throw new FormatError(String.format("key was not closed with '%c'", CLOSE_CH));
        }
    }
    
    public int hashCode() {
        return source.hashCode();    
    }
    
    public boolean equals(Object obj) {
        assert !(obj instanceof String);
        if (this == obj) return true;
        if (!(obj instanceof LocalizedString)) return false;
        return source.equals(((LocalizedString)obj).source);
    }

    /**
     * Создает LocalizedString с проверкой на корректность (соответствие формату) 
     */
    public static LocalizedString createChecked(String source) throws FormatError {
        if (source == null) {
            return null;
        }
        checkLocalizedStringFormat(source);
        return create(source);
        
    }

    /**
     * Создает LocalizedString без проверки на корректность  
     */
    public static LocalizedString create(String source) {
        if (source == null) {
            return null;
        }
        return new LocalizedString(source);
    }
    
    public static LocalizedString create(String source, boolean needToBeLocalized) {
        if (source == null) {
            return null;
        }
        return new LocalizedString(source, needToBeLocalized);
        
    } 
    
    /**
     * Экранирует обычную строку без локализации таким образом, чтобы потом ее можно было передать в конструктор LocalizedString,
     * например, объединенную с какой-нибудь строкой с фигурными скобками.
     */
    public static String escapeForLocalization(String source) {
        if (source == null) {
            return null;
        }
        
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < source.length(); ++i) {
            switch (source.charAt(i)) {
                case '\\': buffer.append("\\\\"); break;
                case '{': buffer.append("\\{"); break;
                case '}': buffer.append("\\}"); break;
                default: buffer.append(source.charAt(i));
            }
        }
        return buffer.toString();
    }
    
    public boolean isEmpty() {
        return source == null || source.trim().equals("");
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(this);
    }
    
}
