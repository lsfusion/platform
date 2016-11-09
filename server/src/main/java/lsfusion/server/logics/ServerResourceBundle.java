package lsfusion.server.logics;


import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ServerResourceBundle {
    private static ResourceBundle serverResourceBundle = ResourceBundle.getBundle("ServerResourceBundle");

    public static String getString(String key) {
        return serverResourceBundle.getString(key);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }

    public static void load(String locale) {
        if (locale.equals("")) {
            serverResourceBundle = ResourceBundle.getBundle("ServerResourceBundle");
        } else {
            serverResourceBundle = ResourceBundle.getBundle("ServerResourceBundle", new Locale(locale, ""));
        }
    }
}