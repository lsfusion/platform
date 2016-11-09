package lsfusion.base;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;


public class ApiResourceBundle {

    private static ResourceBundle apiResourceBundle = ResourceBundle.getBundle("ApiResourceBundle");

    public static String getString(String key) {
        return apiResourceBundle.getString(key);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }

    public static void load(String locale) {
        if (locale.equals("")) {
            apiResourceBundle = ResourceBundle.getBundle("ApiResourceBundle");
        } else {
            apiResourceBundle = ResourceBundle.getBundle("ApiResourceBundle", new Locale(locale, ""));
        }
    }
}
