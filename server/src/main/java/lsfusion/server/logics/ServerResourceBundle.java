package lsfusion.server.logics;

import lsfusion.base.LocalizeUtils;

import java.text.MessageFormat;

public class ServerResourceBundle {
    public static String getString(String key) {
        return LocalizeUtils.getBundle("ServerResourceBundle").getString(key);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }
}