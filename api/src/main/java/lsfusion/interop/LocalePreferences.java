package lsfusion.interop;

import java.io.Serializable;

import static lsfusion.base.BaseUtils.nvl;

public class LocalePreferences implements Serializable {
    public String language;
    public String country;
    public String timeZone;
    public Integer twoDigitYearStart;
    public boolean useClientLocale;
    
    public LocalePreferences(String language, String country, String timeZone, Integer twoDigitYearStart, boolean useClientLocale) {
        this.language = language;
        this.country = country;
        this.timeZone = timeZone;
        this.twoDigitYearStart = twoDigitYearStart;
        this.useClientLocale = useClientLocale;
    }
    
    public static LocalePreferences overrideDefaultWithUser(LocalePreferences defaultPreferences, LocalePreferences userPreferences) {
        assert defaultPreferences != null;
        if (userPreferences == null || !userPreferences.useClientLocale) {
            return defaultPreferences;
        }

        String language = nvl(userPreferences.language, defaultPreferences.language);
        // country и language идут вместе парой, поэтому проверка на language, как на основной параметр 
        String country = userPreferences.language == null ? defaultPreferences.country : userPreferences.country;
        String timeZone = nvl(userPreferences.timeZone, defaultPreferences.timeZone);
        Integer twoDigitYearStart = nvl(userPreferences.twoDigitYearStart, defaultPreferences.twoDigitYearStart);
        
        return new LocalePreferences(language, country, timeZone, twoDigitYearStart, true);    
    }
}
