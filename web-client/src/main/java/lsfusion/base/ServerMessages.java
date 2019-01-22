package lsfusion.base;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ServerMessages {
    public static String getString(String key) {
        return ResourceBundle.getBundle("ServerMessages", ServerUtils.getLocale()).getString(key);
    }

    public static String getString(HttpServletRequest request, String key) {
        if (request != null) {
            Locale locale = ServerUtils.getLocale(request);
            if (locale != null) {
                return ResourceBundle.getBundle("ServerMessages", locale).getString(key);
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
}
