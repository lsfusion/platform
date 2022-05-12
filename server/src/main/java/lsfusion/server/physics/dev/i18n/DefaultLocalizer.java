package lsfusion.server.physics.dev.i18n;

import lsfusion.base.LocalizeUtils;
import lsfusion.base.ResourceUtils;

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
                return bundle.getString(key);
            } catch (MissingResourceException | ClassCastException ignored) {}
        }
        return key;
    }
}
