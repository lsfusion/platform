package lsfusion.base;

import lsfusion.utils.LocalizeUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;

public class ServerMessages {
    public static String getString(String key) {
        return getString(key, ServerUtils.getLocale());
    }

    public static String getString(HttpServletRequest request, String key) {
        if (request != null) {
            Locale locale = ServerUtils.getLocale(request);
            if (locale != null) {
                return getString(key, locale);
            }
        }
        return getString(key);
    }

    public static String getString(HttpServletRequest request, String key, Object... params) {
        if (request != null) {
            return MessageFormat.format(getString(request, key), params);
        }
        return MessageFormat.format(getString(key), params);
    }
    
    private static String getString(String key, Locale locale) {
        return LocalizeUtils.getBundle("ServerMessages", locale).getString(key);
    }
}
