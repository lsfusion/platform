package lsfusion.server.physics.dev.i18n;

import lsfusion.base.LocalizeUtils;
import lsfusion.server.base.ResourceUtils;

import java.util.*;
import java.util.regex.Pattern;

public class DefaultLocalizer extends AbstractLocalizer {
    private final Collection<String> resourceBundleNames; 
    
    public DefaultLocalizer() {
        resourceBundleNames = getBundlesNames();
    }

    public static List<String> getBundlesNames() {
        Pattern pattern = Pattern.compile("/([^/]*ResourceBundle)\\.properties"); // () will be returned, i.e. without extension
        return ResourceUtils.getResources(pattern);
    }

    @Override
    public String localizeKey(String key, Locale locale) {
        for (String bundleName : resourceBundleNames) {
            try {
                ResourceBundle bundle = LocalizeUtils.getBundle(bundleName, locale);
                if (bundle.containsKey(key)) { // asking instead of catching : with a bundle per module a key that lives in the last one, or in none,
                    Object value = bundle.getObject(key); // used to cost one thrown exception per bundle before it - thousands of them over a startup
                    if (value instanceof String)
                        return (String) value;
                }
            } catch (MissingResourceException ignored) { // the bundle itself may be missing for this locale
            }
        }
        return key;
    }
}
