package lsfusion.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizeUtils {
    private static ResourceBundle.Control modifiedControl = new ControlWithoutDefault();

    public static ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, Locale.getDefault());
    }
    
    public static ResourceBundle getBundle(String baseName, Locale locale) {
        return ResourceBundle.getBundle(baseName, locale, modifiedControl);
    }
    
    private static class ControlWithoutDefault extends ResourceBundle.Control {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }
}

