package lsfusion.server.physics.dev.i18n;

import lsfusion.base.LocalizeUtils;
import lsfusion.base.ResourceUtils;

import java.util.*;
import java.util.regex.Pattern;

public class ReversedI18NDictionary {
    public ReversedI18NDictionary(String language, String country) {
        if (language != null) {
            Locale locale = (country == null ? new Locale(language) : new Locale(language, country));
            
            List<String> bundlesNames = getBundlesNames();
            this.literalsMap = new HashMap<>();
            for (String bundleName : bundlesNames) {
                ResourceBundle bundle = LocalizeUtils.getBundle(bundleName, locale);
                if (isSuitableBundle(bundle, locale)) {
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
    
    private List<String> getBundlesNames() {
        Pattern pattern = Pattern.compile("/([^/]*ResourceBundle)\\.properties");
        return ResourceUtils.getResources(pattern);
    }
     
    private boolean isSuitableBundle(ResourceBundle bundle, Locale locale) {
        if (bundle == null) return false;
        if (locale.getLanguage().equals("en") && bundle.getLocale().getLanguage().isEmpty()) return true; // absence of "_en" resource bundles
        return bundle.getLocale().getLanguage().equals(locale.getLanguage());
    }
    
    private Map<String, String> literalsMap = null;
}
