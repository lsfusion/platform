package lsfusion.client;


import lsfusion.base.LocalizeUtils;

import java.text.MessageFormat;

public class ClientResourceBundle {
    public static String getString(String key) {
        return LocalizeUtils.getBundle("ClientResourceBundle").getString(key);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }
}
