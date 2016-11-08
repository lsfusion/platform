package lsfusion.server.logics.i18n;

import lsfusion.base.BaseUtils;
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
        if (canBeOptimized(source)) {
            this.source = removeEscapeSymbols(source);
            this.needToBeLocalized = false;
        } else {
            this.source = source;
            this.needToBeLocalized = true;
        }
    }

    protected LocalizedString(String source, boolean needToBeLocalized) {
        assert source != null;
        this.source = source;
        this.needToBeLocalized = needToBeLocalized;
    }

    public boolean needToBeLocalized() {
        return needToBeLocalized;
    }

    public String getString(Locale locale, Localizer localizer) {
        if (!needToBeLocalized()) {
            return getSourceString();
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch == '\\' && i+1 < source.length()) {
                builder.append(source.charAt(i+1));
                ++i;
            } else if (ch == OPEN_CH) {
                // не учитывается вариант с экранированной закрывающей скобкой, но для этого должен быть ключ с фигурной скобкой
                int closePos = source.indexOf(CLOSE_CH, i+1);
                if (closePos == -1) {
                    builder.append(source.substring(i));
                    break;
                }
                
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
     * Если create вызывается без второго параметра, то происходит проверка (в кострукторе) на необходимость локализации
     * строки. Если в строке нет ни одной неэкранированной фигурной скобки, то экранирование убирается и строка 
     * needToBeLocalized устанавливается в false
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
    
    private static boolean canBeOptimized(String s) {
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == OPEN_CH || ch == CLOSE_CH) {
                return false;
            } else if (ch == '\\') {
                ++i;
            }
        }
        return true;
    }
    
    private static String removeEscapeSymbols(String s) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char cur = s.charAt(i);
            if (cur == '\\' && i+1 < s.length()) {
                char next = s.charAt(i+1);
                if (next == '\\' || next == OPEN_CH || next == CLOSE_CH) {
                    builder.append(next);
                    ++i;
                    continue;
                }
            } 
            builder.append(cur);
        }
        return builder.toString();
    }
    
    public static LocalizedString concat(LocalizedString leftString, LocalizedString rightString) {
        if (leftString == null || rightString == null) return BaseUtils.nvl(leftString, rightString);
        
        boolean leftNTBL = leftString.needToBeLocalized();
        boolean rightNTBL = rightString.needToBeLocalized();
        
        if (!leftNTBL && !rightNTBL) {
            return create(leftString.getSourceString() + rightString.getSourceString(), false);
        }
        
        String left = leftNTBL ? leftString.getSourceString() : escapeForLocalization(leftString.getSourceString()); 
        String right = rightNTBL ? rightString.getSourceString() : escapeForLocalization(rightString.getSourceString());        
        return create(left + right, true);
    }
    
    public static LocalizedString concat(LocalizedString leftString, String rightString) {
        return concat(leftString, new LocalizedString(rightString, false));
    }

    public static LocalizedString concat(String leftString, LocalizedString rightString) {
        return concat(new LocalizedString(leftString, false), rightString);
    }
    
    public static LocalizedString concatList(Object... strings) {
        LocalizedString result = null;
        for (Object nextString : strings) {
            LocalizedString next = null;
            if (nextString instanceof LocalizedString) {
                next = (LocalizedString) nextString;
            } else if (nextString instanceof String) {
                next = new LocalizedString((String) nextString, false);
            } else {
                assert false;
            }
            result = concat(result, next);
        }
        return result;
    }
    
    /**
     * Экранирует обычную строку без локализации таким образом, чтобы потом ее можно было передать в конструктор LocalizedString,
     * например, объединенную с какой-нибудь строкой с фигурными скобками.
     */
    public static String escapeForLocalization(String source) {
        if (source == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); ++i) {
            switch (source.charAt(i)) {
                case '\\': builder.append("\\\\"); break;
                case OPEN_CH: builder.append("\\" + OPEN_CH); break;
                case CLOSE_CH: builder.append("\\" + CLOSE_CH); break;
                default: builder.append(source.charAt(i));
            }
        }
        return builder.toString();
    }
    
    public boolean isEmpty() {
        return source == null || source.trim().equals("");
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(this);
    }
    
}
