package lsfusion.server.physics.dev.i18n;

import lsfusion.base.LocalizeUtils;

import java.util.*;

public class ReversedI18NDictionary {
    public ReversedI18NDictionary(String language, String country) {
        if (language != null) {
            Locale locale = (country == null ? new Locale(language) : new Locale(language, country));
            
            List<String> bundlesNames = DefaultLocalizer.getBundlesNames();
            this.literalsMap = new HashMap<>();
            for (String bundleName : bundlesNames) {
                ResourceBundle bundle = LocalizeUtils.getBundle(bundleName, locale);
                if (isSuitableBundle(bundle, language)) {
                    for (String key : bundle.keySet()) {
                        String value = bundle.getString(key);
                        literalsMap.put(value, key);
                    }
                }
            }
        } 
    }
    
    public ReversedI18NDictionary() {
        
    }
    
    public String getValue(String key) {
        if (literalsMap == null) return null;
        return literalsMap.get(key);
    }

    private boolean isSuitableBundle(ResourceBundle bundle, String language) {
        if (bundle == null) return false;
        if ((language.equals("en") || language.equals("default")) &&
                bundle.getLocale().getLanguage().isEmpty()) return true; // absence of "_en" resource bundles
        return bundle.getLocale().getLanguage().equals(language);
    }
    
    private Map<String, String> literalsMap = null;
}
