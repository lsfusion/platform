package platform.server.logics;


import platform.server.Utf8ResourceBundle;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ServerResourceBundle {

    private static ResourceBundle serverResourceBundle = Utf8ResourceBundle.getBundle("ServerResourceBundle", new Locale("ru", ""));

    public static String getString(String key) {
        return serverResourceBundle.getString(key);
    }

    public static String getString(String key, Object ... params) {
        String returnString = serverResourceBundle.getString(key);
        return MessageFormat.format(returnString, params);
    }

    public static void load(String locale) {
        if (locale.equals("")) {
            serverResourceBundle = Utf8ResourceBundle.getBundle("ServerResourceBundle", Locale.getDefault());
        } else {
            serverResourceBundle = Utf8ResourceBundle.getBundle("ServerResourceBundle", new Locale(locale, ""));
            //serverResourceBundle = Utf8ResourceBundle.getBundle("ServerResourceBundle", Locale.US);
        }
    }

}