package lsfusion.base;

import java.text.MessageFormat;

public class ApiResourceBundle {
    public static String getString(String key) {
        return LocalizeUtils.getBundle("ApiResourceBundle").getString(key);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }
}
