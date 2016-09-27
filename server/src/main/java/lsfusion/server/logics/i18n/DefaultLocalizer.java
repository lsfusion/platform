package lsfusion.server.logics.i18n;

import lsfusion.base.ResourceList;

import java.util.*;
import java.util.regex.Pattern;

public class DefaultLocalizer implements LocalizedString.Localizer {
    private Collection<String> resourceBundleNames; 
    
    public DefaultLocalizer() {
        resourceBundleNames = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^/]*ResourceBundle\\.properties"); // todo [dale]: возможно нужен другой regexp
        Collection<String> filenames = ResourceList.getResources(pattern);
        for (String filename : filenames) {
            resourceBundleNames.add(filename.substring(0, filename.lastIndexOf('.')));
        }
    }
    
    @Override
    public String localize(String key, Locale locale) {
        for (String bundleName : resourceBundleNames) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
                return bundle.getString(key);
            } catch (MissingResourceException | ClassCastException ignored) {}
        }
        return key;
    }
}
