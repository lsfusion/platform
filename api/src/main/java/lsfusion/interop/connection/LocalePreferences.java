package lsfusion.interop.connection;

import java.io.Serializable;
import java.util.Locale;

public class LocalePreferences implements Serializable {
    public Locale locale;
    public String timeZone;
    public Integer twoDigitYearStart;
    public String dateFormat;
    public String timeFormat;

    public LocalePreferences(Locale locale, String timeZone, Integer twoDigitYearStart, String dateFormat, String timeFormat) {
        this.locale = locale;
        this.timeZone = timeZone;
        this.twoDigitYearStart = twoDigitYearStart;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }

    public static Locale getLocale(String language, String country) {
        if (language != null) {
            return new Locale(language, country == null ? "" : country);
        }
        return null; // default
    }

    //    public static LocalePreferences overrideDefaultWithUser(LocalePreferences defaultPreferences, LocalePreferences userPreferences) {
//        assert defaultPreferences != null;
//        if (userPreferences == null) {
//            return defaultPreferences;
//        }
//
//        String language = nvl(userPreferences.language, defaultPreferences.language);
//        // country и language идут вместе парой, поэтому проверка на language, как на основной параметр 
//        String country = userPreferences.language == null ? defaultPreferences.country : userPreferences.country;
//        String timeZone = nvl(userPreferences.timeZone, defaultPreferences.timeZone);
//        Integer twoDigitYearStart = nvl(userPreferences.twoDigitYearStart, defaultPreferences.twoDigitYearStart);
//        
//        return new LocalePreferences(language, country, timeZone, twoDigitYearStart);    
//    }
}
