package lsfusion.interop;

import java.io.Serializable;

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
}
