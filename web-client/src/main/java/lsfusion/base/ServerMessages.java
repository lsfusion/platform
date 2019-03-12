package lsfusion.base;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;

public class ServerMessages {
    
    public static String getString(HttpServletRequest request, String key, Object... params) {
        String string = getString(key, ServerUtils.getLocale(request));
        if(params.length == 0) // optimization
            return string;
        return MessageFormat.format(string, params);
    }
    
    private static String getString(String key, Locale locale) {
        return LocalizeUtils.getBundle("ServerMessages", locale).getString(key);
    }
}
