package lsfusion.server.physics.dev.i18n;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

import static lsfusion.base.BaseUtils.nvl;

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


public final class LocalizedString {
    public interface Localizer {
        String localize(String source, Locale locale);
    }

    public static final char OPEN_CH = '{';
    public static final char CLOSE_CH = '}';
    
    private final String source;
    private final boolean needToBeLocalized;
    
    private boolean isFormatted;
    private final Object[] params;
    private static final Object[] NOPARAMS = new Object[]{};
    
    private LocalizedString(String source) {
        if (canBeOptimized(source)) {
            this.source = removeEscapeSymbols(source);
            this.needToBeLocalized = false;
        } else {
            this.source = source;
            this.needToBeLocalized = true;
        }
        this.params = NOPARAMS;
    }

    private LocalizedString(String source, boolean needToBeLocalized) {
        this(source, needToBeLocalized, false);
    }

    private LocalizedString(String source, boolean needToBeLocalized, boolean isFormatted, Object... params) {
        assert source != null;
        
        this.source = source;
        this.needToBeLocalized = needToBeLocalized;
        this.isFormatted = isFormatted;
        this.params = params;
    }
    
    public boolean needToBeLocalized() {
        return needToBeLocalized;
    }

    private final static Localizer emptyLocalizer = new AbstractLocalizer() {
        protected String localizeKey(String key, Locale locale) {
            return key;
        }
    };
    
    public String getString(Locale locale, Localizer localizer) {
        if (!needToBeLocalized()) {
            return getSourceString();
        }
        
        if (localizer == null) {
            localizer = emptyLocalizer;
        }
        
        String result = localizer.localize(source, locale);
        if (isFormatted) {
            // Необходимо преобразовать строку, чтобы ее можно было передать в MessageFormat
            // Для этого мы заменяем одиночные кавычки двумя кавычками (этим мы лишаемся функциональности, позволяющей экранировать скобки кавычками)
            // Затем экранируем кавычками все фигурные строки, которые не выглядят как {5} (число в фигурных скобках)
            result = result.replace("'", "''").replace("{", "'{'").replace("}", "'}'").replaceAll("'\\{'(\\d+)'\\}'", "{$1}");
            return MessageFormat.format(result, params);
        } else {
            return result;
        }

    }

    public String getSourceString() {
        return source;
    }

    public static class FormatError extends Exception {
        public FormatError(String text) {
            super("localized string: " + text);
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
        if (!(obj instanceof LocalizedString)) {
            return false;
        } else {
            LocalizedString other = (LocalizedString)obj;
            return  source.equals(other.source) && 
                    needToBeLocalized() == other.needToBeLocalized() && 
                    isFormatted == other.isFormatted &&
                    Arrays.equals(params, other.params);
        }
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

    private static class Instancer {
        private final static Instancer instance = new Instancer();
        
        @IdentityStartLazy
        public LocalizedString create(String source) {
            return new LocalizedString(source);
        }

        @IdentityStartLazy
        public LocalizedString create(String source, boolean needToBeLocalized) {
            return new LocalizedString(source, needToBeLocalized);
        }
        
        @IdentityStartLazy
        public LocalizedString createFormatted(String source, ImList<Object> params) { // we need ImList to get correct equals for IdentityLazy
            return new LocalizedString(source, true, true, params.toArray(new Object[params.size()]));
        }
    }
    
    public final static LocalizedString NONAME = LocalizedString.create(""); 

    /**
     * Создает LocalizedString без проверки на корректность
     * Если create вызывается без второго параметра, то происходит проверка (в конструкторе) на необходимость локализации
     * строки. Если в строке нет ни одной неэкранированной фигурной скобки, то экранирование убирается и  
     * needToBeLocalized устанавливается в false
     */
    public static LocalizedString create(String source) {
        if (source == null) {
            return null;
        }
        return Instancer.instance.create(source);
    }
    
    public static LocalizedString create(String source, boolean needToBeLocalized) {
        if (source == null) {
            return null;
        }
        return Instancer.instance.create(source, needToBeLocalized);
    } 
    
    public static LocalizedString createFormatted(String source, Object... params) {
        if (source == null) {
            return null;
        }
        return Instancer.instance.createFormatted(source, ListFact.toList(params));
    }
    
    public static boolean canBeOptimized(String s) {
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
        if (leftString == null || rightString == null) return nvl(leftString, rightString);
        
        boolean leftNTBL = leftString.needToBeLocalized();
        boolean rightNTBL = rightString.needToBeLocalized();
        
        if (!leftNTBL && !rightNTBL) {
            return create(leftString.getSourceString() + rightString.getSourceString(), false);
        }
        
        String left = leftNTBL ? leftString.getSourceString() : escapeForLocalization(leftString.getSourceString()); 
        String right = rightNTBL ? rightString.getSourceString() : escapeForLocalization(rightString.getSourceString());
        
        assert !leftString.isFormatted || !rightString.isFormatted : "two formatted LocalizedStrings concatenated";
        boolean isFormatted = leftString.isFormatted || rightString.isFormatted;
        if (isFormatted) {
            Object[] params = (leftString.isFormatted ? leftString.params : rightString.params);
            return createFormatted(left + right, params);
        } else {
            return create(left + right, true);
        }
    }
    
    public static LocalizedString concat(LocalizedString leftString, String rightString) {
        return concat(leftString, create(rightString, false));
    }

    public static LocalizedString concat(String leftString, LocalizedString rightString) {
        return concat(create(leftString, false), rightString);
    }
    
    public static LocalizedString concatList(Object... strings) {
        LocalizedString result = null;
        for (Object nextString : strings) {
            LocalizedString next = null;
            if (nextString instanceof LocalizedString) {
                next = (LocalizedString) nextString;
            } else if (nextString instanceof String) {
                next = create((String) nextString, false);
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
